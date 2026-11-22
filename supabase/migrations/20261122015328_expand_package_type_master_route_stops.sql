-- Amplia os tipos de carga (ADR-005). Mantém 'pacotinho' por compatibilidade e aceita 'pacote'.
ALTER TABLE public.master_route_stops
    DROP CONSTRAINT IF EXISTS master_route_stops_package_type_check;

ALTER TABLE public.master_route_stops
    ADD CONSTRAINT master_route_stops_package_type_check
    CHECK (package_type IN ('pacotinho', 'pacote', 'volumoso', 'documento', 'comida', 'farmacia'));

COMMENT ON COLUMN public.master_route_stops.package_type IS
    'Tipo de carga: pacotinho/pacote, volumoso, documento, comida, farmacia';
