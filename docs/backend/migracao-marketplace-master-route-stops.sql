-- =============================================================================
-- CENTRAL DO MOTORISTA (POCKET) - SUPORTE A MARKETPLACE NAS PARADAS MASTER
-- =============================================================================
-- Arquivo: docs/backend/migracao-marketplace-master-route-stops.sql
-- Descrição:
--   1. Adiciona a coluna marketplace_name na tabela master_route_stops para 
--      identificação do e-commerce gerador do pacote (Mercado Livre, Shopee, 
--      Shein, TikTok Shop, Kwai, Amazon, Magalu, etc.).
--   2. Cria índice em marketplace_name para filtros e relatórios analíticos.
--   3. As políticas RLS existentes continuam garantindo que o motorista só acesse
--      e atualize suas próprias paradas (auth.uid() = user_id).
-- Referência: ADR-002, ADR-003 e Central de Entregas Master
-- Data: 29/09/2026
-- =============================================================================

-- 1. Adição da coluna marketplace_name
ALTER TABLE public.master_route_stops
    ADD COLUMN IF NOT EXISTS marketplace_name TEXT;

-- 2. Índice para consultas e agrupamentos por Marketplace
CREATE INDEX IF NOT EXISTS idx_master_route_stops_marketplace 
    ON public.master_route_stops(marketplace_name);

-- 3. Comentário descritivo na coluna
COMMENT ON COLUMN public.master_route_stops.marketplace_name IS 
    'Nome do Marketplace / E-commerce de origem do pacote (ex: TikTok Shop, Shopee, Mercado Livre, etc.)';
