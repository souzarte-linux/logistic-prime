# Evidência F1 / F1.1: Centro de Custos (Supabase `koocvhlprwtdympjwbco`)

Data da verificação: 22/11/2026. Registrado em 24/11/2026.
Executado pelo Claude.ai via Supabase MCP.

## Como foi executado

A suíte `docs/backend/validacao-centro-de-custos.sql` (branch `feature/cc-f1-1-correcoes`, SHA `7e13e8e`) foi rodada em blocos `DO`.

Alterações em relação ao arquivo original, feitas só para a execução via API:
- removidos `BEGIN`/`ROLLBACK` e os `RAISE NOTICE`;
- adicionada uma exceção-sentinela no final, para a transação abortar (rollback automático) e a mensagem voltar pela API.

Nenhum dado de teste persistiu no banco.

## Resultado final

| Teste | Resultado |
|---|---|
| 11.c saldo e transferência | PASSOU |
| 11.d imutabilidade UPDATE/DELETE (trigger) | PASSOU |
| 11.e unicidade (operation_id, leg) | PASSOU |
| 11.f estorno sem reverses_entry_id | PASSOU |
| 11.g amount <= 0 / start_date nulo | PASSOU |
| 11.h FK composta entre usuários | PASSOU |
| 11.i reexecução idempotente do DDL | **NÃO EXECUTADO** |
| 11.j pagamento dividido | PASSOU |
| 11.k unicidade de alocações | PASSOU |
| 11.l vínculo único por card_id | PASSOU |
| 11.m ON DELETE SET NULL (vaults) | PASSOU |
| 11.n ON DELETE SET NULL (settings) | PASSOU |
| 11.o cascata de auth.users nas 12 tabelas | PASSOU |
| 11.p trigger bloqueia mutação | PASSOU |
| 11.q FK composta no razão | PASSOU |
| 11.r privilégios de `authenticated`/`anon` no razão | PASSOU (após a correção abaixo; falhou antes) |
| 11.s isolamento RLS | PASSOU (executado após a correção) |

Contagem: 17 testes listados, 16 passaram, 1 não foi executado (11.i), 0 falharam depois da correção.

## Achado e correção (teste 11.r)

**Antes:** no banco real, o papel `authenticated` possuía UPDATE, DELETE, TRUNCATE, REFERENCES e TRIGGER em `public.vault_ledger`. A seção 14 do script de migração prevê apenas SELECT e INSERT.

O RLS (só SELECT e INSERT) e o trigger de imutabilidade já bloqueavam UPDATE e DELETE via API, mas a defesa em profundidade não estava aplicada.

**Correção aplicada em 22/11/2026**, com autorização expressa do dono, como a migração `cc_f1_revoke_vault_ledger_mutation_privileges`:

```sql
REVOKE UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON TABLE public.vault_ledger FROM authenticated;
REVOKE ALL ON TABLE public.vault_ledger FROM anon;
GRANT SELECT, INSERT ON TABLE public.vault_ledger TO authenticated;
```

**Verificação posterior:** `authenticated` tem somente INSERT e SELECT; `anon` não tem nenhum privilégio; os testes 11.r e 11.s passaram.

## Pendências

- **Teste 11.i** (reexecução idempotente do DDL) segue não executado.
- **Causa da divergência:** as tabelas foram criadas por SQL direto no banco, sem passar pelo script `migracao-centro-de-custos.sql`, e por isso os privilégios padrão do Supabase prevaleceram. O script já prevê o `REVOKE` correto (seção 14).
- **Registro em `supabase/migrations`:** concluído em 24/11/2026. O arquivo `20261122020000_centro_de_custos_12_tabelas.sql` foi adicionado à `main` (commit `8e51a0e`). O registro interno de migrações do Supabase ainda não lista as 12 tabelas, apenas a migração do `REVOKE`.
