-- =============================================================================
-- CENTRAL DO MOTORISTA (POCKET) - EXPANSÃO DE PACKAGE_TYPE EM MASTER_ROUTE_STOPS
-- =============================================================================
-- Arquivo: docs/backend/migracao-expansao-package-type.sql
-- Descrição:
--   Atualiza o CHECK constraint da coluna package_type na tabela master_route_stops
--   para suportar os 5 tipos de pacotes da Central de Entregas Master:
--   - 'pacotinho' (Pacote padrão de e-commerce)
--   - 'volumoso'  (Cargas volumosas / caixas grandes)
--   - 'documento' (Envelopes, contratos, malotes)
--   - 'comida'    (Marmitas, alimentos perecíveis, delivery rápido)
--   - 'farmacia'  (Medicamentos, itens farmacêuticos)
--
-- Referência: ADR-002, ADR-003, ADR-004 e Expansão de Tipos de Carga
-- Data: 04/10/2026
-- =============================================================================

-- 1. Remove constraint anterior se existir
ALTER TABLE public.master_route_stops
    DROP CONSTRAINT IF EXISTS master_route_stops_package_type_check;

-- 2. Recria constraint com os 5 tipos permitidos
ALTER TABLE public.master_route_stops
    ADD CONSTRAINT master_route_stops_package_type_check
    CHECK (package_type IN ('pacotinho', 'volumoso', 'documento', 'comida', 'farmacia'));

-- 3. Atualiza comentário da coluna para documentação
COMMENT ON COLUMN public.master_route_stops.package_type IS
    'Tipo de carga/pacote: pacotinho, volumoso, documento, comida, farmacia';
