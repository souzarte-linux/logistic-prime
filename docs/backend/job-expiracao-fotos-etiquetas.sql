-- =============================================================================
-- CENTRAL DO MOTORISTA (POCKET) - ROTINA DE LIMPEZA DE FOTOS EXPIRADAS
-- =============================================================================
-- Arquivo: docs/backend/job-expiracao-fotos-etiquetas.sql
-- Descrição:
--   1. Tabela de auditoria storage_cleanup_logs para métricas de limpeza e consumo.
--   2. Índice parcial idx_master_route_stops_expired_photos para busca de fotos vencidas.
--   3. Funções RPC no Supabase para seleção segura e baixa em lote das fotos.
--   4. Salvaguardas ativas: NUNCA seleciona ou limpa fotos de rotas 'em_andamento'.
--   5. Script de agendamento via pg_cron / pg_net (opcional no Supabase Pro/Self-hosted).
-- Referência: ADR-003 e Prompt 4 (Plano de Implementação Final)
-- Data: 28/09/2026
-- =============================================================================

-- 1. Tabela de Auditoria de Limpeza de Storage
CREATE TABLE IF NOT EXISTS public.storage_cleanup_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    executed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    photos_count INT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'sucesso' CHECK (status IN ('sucesso', 'parcial', 'erro')),
    details JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_storage_cleanup_logs_date 
    ON public.storage_cleanup_logs(executed_at DESC);

-- Habilitar RLS na tabela de auditoria (somente authenticated pode consultar seus logs ou service_role)
ALTER TABLE public.storage_cleanup_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "storage_cleanup_logs_select" ON public.storage_cleanup_logs;
CREATE POLICY "storage_cleanup_logs_select"
    ON public.storage_cleanup_logs
    FOR SELECT
    TO authenticated
    USING (true);

-- 2. Índice Parcial Crítico para Fotos Vencidas
-- Evita full scan na tabela master_route_stops durante a rotina diária
CREATE INDEX IF NOT EXISTS idx_master_route_stops_expired_photos
    ON public.master_route_stops(photo_expires_at)
    WHERE photo_url IS NOT NULL;

-- 3. Função RPC para Buscar Fotos Vencidas (Respeitando a Regra de Rotas Concluídas)
CREATE OR REPLACE FUNCTION public.get_expired_photos_to_clean(p_batch_size INT DEFAULT 100)
RETURNS TABLE (
    stop_id UUID,
    photo_url TEXT,
    route_id UUID,
    route_status TEXT,
    photo_expires_at TIMESTAMPTZ
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        mrs.id AS stop_id,
        mrs.photo_url,
        mrs.route_id,
        mdr.status AS route_status,
        mrs.photo_expires_at
    FROM public.master_route_stops mrs
    INNER JOIN public.master_delivery_routes mdr ON mrs.route_id = mdr.id
    WHERE mrs.photo_url IS NOT NULL
      AND mrs.photo_expires_at IS NOT NULL
      AND mrs.photo_expires_at <= timezone('utc', now())
      -- REGRA CRÍTICA DE PROTEÇÃO:
      -- Não apagar fotos de rotas ainda em andamento! Somente rotas finalizadas ('concluida' ou 'cancelada').
      AND mdr.status <> 'em_andamento'
    ORDER BY mrs.photo_expires_at ASC
    LIMIT p_batch_size;
END;
$$;

-- 4. Função RPC para Marcar Fotos como Limpas após Exclusão no Storage
CREATE OR REPLACE FUNCTION public.mark_photos_as_cleaned(
    p_stop_ids UUID[],
    p_status TEXT DEFAULT 'sucesso',
    p_details JSONB DEFAULT '{}'::jsonb
)
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_updated_count INT;
BEGIN
    IF p_stop_ids IS NULL OR array_length(p_stop_ids, 1) = 0 THEN
        RETURN 0;
    END IF;

    -- Atualiza as paradas removendo photo_url e photo_expires_at
    UPDATE public.master_route_stops
    SET photo_url = NULL,
        photo_expires_at = NULL,
        updated_at = timezone('utc', now())
    WHERE id = ANY(p_stop_ids);

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    -- Registra na tabela de auditoria
    INSERT INTO public.storage_cleanup_logs (photos_count, status, details)
    VALUES (
        v_updated_count,
        p_status,
        jsonb_build_object(
            'cleaned_stop_ids', p_stop_ids,
            'extra', p_details
        )
    );

    RETURN v_updated_count;
END;
$$;

-- 5. Exemplo de Agendamento Nativo via pg_cron (se a extensão pg_cron estiver habilitada no Supabase)
-- Descomente abaixo caso o projeto utilize pg_cron diretamente no banco:
/*
SELECT cron.schedule(
    'clean-expired-master-photos-daily',
    '0 3 * * *', -- Diariamente às 03:00 UTC
    $$
    -- Chamada via pg_net disparando a Edge Function de limpeza
    SELECT net.http_post(
        url := 'https://<PROJECT-REF>.supabase.co/functions/v1/clean-expired-photos',
        headers := '{"Content-Type": "application/json", "Authorization": "Bearer <SERVICE_ROLE_KEY>"}'::jsonb,
        body := '{"batch_size": 100}'::jsonb
    );
    $$
);
*/
