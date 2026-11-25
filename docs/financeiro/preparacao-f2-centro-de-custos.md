# Preparação da Fase F2: Centro de Custos (atualizado em 24/11/2026)

## Estado verificado

**Git**
- `main` em `8e51a0e` no GitHub.
- PRs #1, #2, #3 e #4 mergeados ou fechados. Não há PR aberto.
- Build após os merges das fases 0, 1 e correções: `BUILD SUCCESSFUL in 6m 11s` (`./gradlew testDebugUnitTest assembleDebug`, executado pelo dono em Linux). A contagem de testes não foi impressa.
- Há 5 avisos de compilação (`w:`), todos de código antigo (ícones obsoletos e uma condição sempre verdadeira em `RouteRepository.kt:69`). Não são erros.

**Banco Supabase (`koocvhlprwtdympjwbco`)**
- As 12 tabelas do Centro de Custos existem.
- `vault_ledger`: `authenticated` só tem SELECT e INSERT; `anon` não tem acesso.
- Tipos reais: `expenses.id`, `credit_cards.id`, `billing_cycles.id`, `billing_cycles.platform_id`, `part_maintenance.id` e `part_maintenance.expense_id` são UUID.
- `expenses.user_id`, `credit_cards.user_id` e `billing_cycles.user_id` são TEXT (as tabelas novas usam UUID).
- A migração das 12 tabelas está registrada em `supabase/migrations/20261122020000_centro_de_custos_12_tabelas.sql`.

**Código já presente na `main`**
- `CostCenterDatabase` na versão 2, com esquema exportado em `app/schemas/.../2.json`.
- `LocalDatabaseProvider`, `PostgrestErrorClassifier`, `SyncEngine` com FIFO estrito e `PushResult.AlreadyApplied` (SQLSTATE 23505).

## Ajustes ao Prompt F2 do plano v3

1. **Branch:** a F2 parte da `main` atual (`8e51a0e`). Sugestão de nome: `feature/cc-f2-dados`.
2. **Tipos:** DTOs e entidades das tabelas novas usam `user_id` UUID. Referências a `expenses`, `credit_cards` e `billing_cycles` usam ids UUID (String no Kotlin). Nunca juntar por `user_id` com as tabelas antigas (TEXT).
3. **Room:** o `CostCenterDatabase` já está na versão 2. A F2 deve subir para a versão 3, com `Migration(2, 3)` testada e esquema exportado em `app/schemas`.
4. **Acesso ao banco local:** usar `LocalDatabaseProvider.current()` nos repositórios, e não DAOs singleton.
5. **Sincronização:** tratar `PushResult.AlreadyApplied` e respeitar o FIFO estrito já existentes no `SyncEngine`.
6. **Migrações do Supabase:** esta pendência (registrar a migração das 12 tabelas) está concluída.

## Pendências que continuam abertas

- Teste 11.i (reexecução idempotente do DDL) não executado.
- Avisos de ícones obsoletos e a condição em `RouteRepository.kt:69`: dívida técnica, a tratar em tarefa própria.
