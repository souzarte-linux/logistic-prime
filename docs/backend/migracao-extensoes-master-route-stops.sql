-- =============================================================================
-- CENTRAL DO MOTORISTA (POCKET) - EXTENSÕES DE PARADAS MASTER E PACOTES DE PARCEIRO
-- =============================================================================
-- Arquivo: docs/backend/migracao-extensoes-master-route-stops.sql
-- Descrição:
--   1. Adiciona platform_id, package_type, foto/expiração e campos de transferência
--      (assigned_partner_id, transfer_status, transferred_via, transferred_at)
--      na tabela master_route_stops.
--   2. Cria índice parcial de alta performance idx_master_route_stops_barcode_active.
--   3. Cria a tabela relacional partner_session_packages para rastreabilidade granular
--      de pacotes de entregadores parceiros com suporte à importação do Master.
-- Referência: ADR-003 e Plano de Implementação Final (Prompt 1)
-- Data: 28/09/2026
-- =============================================================================

-- 1. Novas colunas em master_route_stops
ALTER TABLE public.master_route_stops
    ADD COLUMN IF NOT EXISTS platform_id UUID REFERENCES public.platforms(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS package_type TEXT NOT NULL DEFAULT 'pacotinho' CHECK (package_type IN ('pacotinho', 'volumoso')),
    ADD COLUMN IF NOT EXISTS photo_url TEXT,
    ADD COLUMN IF NOT EXISTS photo_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS assigned_partner_id UUID REFERENCES public.delivery_partners(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS transfer_status TEXT CHECK (transfer_status IN ('atribuido_pendente', 'confirmado')),
    ADD COLUMN IF NOT EXISTS transferred_via TEXT CHECK (transferred_via IN ('manual_master', 'scan_parceiro')),
    ADD COLUMN IF NOT EXISTS transferred_at TIMESTAMPTZ;

-- 2. Índice Parcial Crítico para Busca Ativa por Barcode (sem full table scan)
CREATE INDEX IF NOT EXISTS idx_master_route_stops_barcode_active
    ON public.master_route_stops (barcode)
    WHERE transfer_status IS DISTINCT FROM 'confirmado' AND status <> 'devolvido';

-- 3. Índices complementares em master_route_stops
CREATE INDEX IF NOT EXISTS idx_master_route_stops_platform 
    ON public.master_route_stops(platform_id);

CREATE INDEX IF NOT EXISTS idx_master_route_stops_assigned_partner 
    ON public.master_route_stops(assigned_partner_id);

-- 4. Criação da Tabela Granular partner_session_packages
CREATE TABLE IF NOT EXISTS public.partner_session_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES public.delivery_partner_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    origin TEXT NOT NULL DEFAULT 'novo' CHECK (origin IN ('novo', 'importado_master')),
    master_stop_id UUID REFERENCES public.master_route_stops(id) ON DELETE SET NULL,
    status TEXT NOT NULL DEFAULT 'bipado' CHECK (status IN ('bipado', 'entregue', 'devolvido')),
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT uq_partner_session_barcode UNIQUE (session_id, barcode)
);

-- 5. Índices em partner_session_packages
CREATE INDEX IF NOT EXISTS idx_partner_session_packages_session 
    ON public.partner_session_packages(session_id);

CREATE INDEX IF NOT EXISTS idx_partner_session_packages_barcode 
    ON public.partner_session_packages(barcode);

CREATE INDEX IF NOT EXISTS idx_partner_session_packages_stop 
    ON public.partner_session_packages(master_stop_id);

CREATE INDEX IF NOT EXISTS idx_partner_session_packages_user 
    ON public.partner_session_packages(user_id);

-- 6. Habilitação de RLS e Políticas Isoladas por Usuário
ALTER TABLE public.partner_session_packages ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "partner_session_packages_all" ON public.partner_session_packages;
CREATE POLICY "partner_session_packages_all"
    ON public.partner_session_packages
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
