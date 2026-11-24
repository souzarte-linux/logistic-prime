-- Funções de limpeza de fotos: somente service_role (Edge Function)
REVOKE EXECUTE ON FUNCTION public.get_expired_photos_to_clean(integer) FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.mark_photos_as_cleaned(uuid[], text, jsonb) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.get_expired_photos_to_clean(integer) TO service_role;
GRANT EXECUTE ON FUNCTION public.mark_photos_as_cleaned(uuid[], text, jsonb) TO service_role;

ALTER FUNCTION public.get_expired_photos_to_clean(integer) SET search_path = public;
ALTER FUNCTION public.mark_photos_as_cleaned(uuid[], text, jsonb) SET search_path = public;
ALTER FUNCTION public.handle_master_updated_at() SET search_path = public;

-- Funções de gatilho/depuração: não devem ser chamáveis via /rest/v1/rpc
REVOKE EXECUTE ON FUNCTION public.handle_new_user() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.rls_auto_enable() FROM PUBLIC, anon, authenticated;
REVOKE EXECUTE ON FUNCTION public.debug_auth_context() FROM PUBLIC, anon, authenticated;
ALTER FUNCTION public.debug_auth_context() SET search_path = public;
