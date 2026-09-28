-- =============================================================================
-- CENTRAL DO MOTORISTA (POCKET) - MIGRAÇÃO DE ROTAS E PARADAS MASTER
-- =============================================================================
-- Arquivo: docs/backend/migracao-rotas-e-paradas-master.sql
-- Descrição: Criação das tabelas relacionais master_delivery_routes e master_route_stops,
--            índices de performance, triggers para updated_at e políticas de RLS.
-- Referência: ADR-002 (Bipagem com OCR, Rota Master e Deep Links de Navegação)
-- Data: 28/09/2026
-- =============================================================================

-- 1. Criação da Tabela de Cabeçalho da Rota Master (master_delivery_routes)
CREATE TABLE IF NOT EXISTS public.master_delivery_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    platform_id UUID REFERENCES public.platforms(id) ON DELETE SET NULL,
    route_date DATE NOT NULL DEFAULT CURRENT_DATE,
    start_location TEXT NOT NULL,
    start_latitude NUMERIC(10, 7),
    start_longitude NUMERIC(10, 7),
    status TEXT NOT NULL CHECK (status IN ('em_andamento', 'concluida', 'cancelada')) DEFAULT 'em_andamento',
    total_packages INTEGER NOT NULL DEFAULT 0,
    delivered_packages INTEGER NOT NULL DEFAULT 0,
    returned_packages INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    finished_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now())
);

-- 2. Criação da Tabela de Paradas / Pacotes da Rota Master (master_route_stops)
CREATE TABLE IF NOT EXISTS public.master_route_stops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES public.master_delivery_routes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    barcode TEXT NOT NULL,
    recipient_name TEXT,
    full_address TEXT NOT NULL,
    street TEXT,
    number TEXT,
    neighborhood TEXT,
    city TEXT,
    state TEXT,
    cep TEXT,
    stop_order INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL CHECK (status IN ('pendente', 'entregue', 'ausente', 'devolvido')) DEFAULT 'pendente',
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    notes TEXT,
    scanned_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    delivered_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT uq_master_route_stop_barcode UNIQUE (route_id, barcode)
);

-- 3. Índices de Alta Performance
CREATE INDEX IF NOT EXISTS idx_master_routes_user_status 
    ON public.master_delivery_routes(user_id, status);

CREATE INDEX IF NOT EXISTS idx_master_routes_date 
    ON public.master_delivery_routes(user_id, route_date DESC);

CREATE INDEX IF NOT EXISTS idx_master_stops_route_order 
    ON public.master_route_stops(route_id, stop_order ASC);

CREATE INDEX IF NOT EXISTS idx_master_stops_barcode 
    ON public.master_route_stops(route_id, barcode);

CREATE INDEX IF NOT EXISTS idx_master_stops_user 
    ON public.master_route_stops(user_id);

-- 4. Função e Triggers para Atualização Automática de updated_at
CREATE OR REPLACE FUNCTION public.handle_master_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_master_delivery_routes_updated_at ON public.master_delivery_routes;
CREATE TRIGGER trg_master_delivery_routes_updated_at
    BEFORE UPDATE ON public.master_delivery_routes
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_master_updated_at();

DROP TRIGGER IF EXISTS trg_master_route_stops_updated_at ON public.master_route_stops;
CREATE TRIGGER trg_master_route_stops_updated_at
    BEFORE UPDATE ON public.master_route_stops
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_master_updated_at();

-- 5. Habilitação de RLS (Row Level Security) e Políticas Isoladas por Usuário
ALTER TABLE public.master_delivery_routes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.master_route_stops ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "master_delivery_routes_all" ON public.master_delivery_routes;
CREATE POLICY "master_delivery_routes_all"
    ON public.master_delivery_routes
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "master_route_stops_all" ON public.master_route_stops;
CREATE POLICY "master_route_stops_all"
    ON public.master_route_stops
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);
