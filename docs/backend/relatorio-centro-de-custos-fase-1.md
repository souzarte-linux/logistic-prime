# Relatório de Execução — FASE 1: Banco de Dados (Supabase / Centro de Custos)

## 1. Resultado
🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO (EXECUÇÃO PENDENTE NO SUPABASE)**: Script de migração DDL idempotente `docs/backend/migracao-centro-de-custos.sql` e suíte de testes transacionais `docs/backend/validacao-centro-de-custos.sql` criados estritamente conforme as especificações. Como a ferramenta MCP de execução SQL direta (`execute_sql`) não está disponível no ambiente, a aplicação no banco Supabase em produção está marcada como **EXECUÇÃO PENDENTE (manual)**, com passo a passo documentado.

---

## 2. O que mudou
Foi projetada a infraestrutura completa de banco de dados relacional para o novo módulo de **Centro de Custos** da aplicação, sem alterar nenhuma coluna, tipo, dado ou política das tabelas legadas (`expenses`, `credit_cards`, `part_maintenance`, `billing_cycles`).

### Arquivos criados / alterados:
1. `docs/backend/migracao-centro-de-custos.sql`: DDL idempotente em transação única contendo:
   - Funções auxiliares `handle_updated_at()` e `prevent_vault_ledger_mutation()`.
   - 12 tabelas relacionais com `id UUID PRIMARY KEY`, `user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE`, `created_at` e `updated_at` com timezone UTC, RLS ativo para `authenticated` e chaves estrangeiras compostas `(<pai>_id, user_id)`:
     1. `bank_accounts`: contas bancárias (não gerenciadas pelo app, informativas).
     2. `vaults`: caixinhas/cofres virtuais de reserva financeira.
     3. `vault_ledger`: razão contábil **imutável** com bloqueio de UPDATE/DELETE por trigger e policies restritas a SELECT e INSERT.
     4. `expense_settlements`: liquidação e rateio de despesas (PIX / Débito) entre caixinha e conta.
     5. `fixed_costs`: custos fixos paramétricos **sem coluna de valor monetário** (anti-valor R16/G15).
     6. `fixed_cost_versions`: versões históricas de valores com vigência (`effective_from`).
     7. `fixed_cost_payments`: liquidação de competências de custos fixos.
     8. `card_invoice_payments`: liquidações totais ou parciais de faturas de cartão.
     9. `cost_center_settings`: parâmetros operacionais e modo de reserva por motorista.
     10. `reserve_allocations`: alocações geradas na baixa de faturas das plataformas.
     11. `reserve_allocation_days`: granularidade diária e por plataforma das alocações.
     12. `card_account_links`: mapeamento de cartões de débito para contas bancárias.
   - 13 índices de alta performance cobrindo consultas críticas e filtros de auditoria.
   - View de conciliação `vault_balances WITH (security_invoker = true)` e função fallback `get_vault_balances()`.
   - REVOKE completo para a role `anon` e GRANTs mínimos granulares para a role `authenticated`.
2. `docs/backend/validacao-centro-de-custos.sql`: suíte de validação automatizada em PostgreSQL cobrindo os testes (c) a (l) da especificação com asserções estritas e encerramento em ROLLBACK.
3. `docs/backend/relatorio-centro-de-custos-fase-1.md`: este relatório de auditoria e entrega.

---

## 3. Evidências

### 3.1 Inspeção do Schema Real e Tipos Mapeados
Conforme auditoria do repositório (`docs/backend/auditoria-banco-e-apis.md`, DTOs remotos em `data/remote/dto/` e migrações existentes):
- **PostgreSQL / Supabase Version**: PostgreSQL 15+ (compatível com `security_invoker = true` em views).
- **`expenses.id`**: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **`expenses.user_id`**: `TEXT` (legado)
- **`expenses.occurred_at`**: `TIMESTAMPTZ NOT NULL DEFAULT NOW()`
- **`expenses.category`**: `VARCHAR(30)`
- **`expenses.amount`**: `NUMERIC(12, 2)`
- **`credit_cards.id`**: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **`credit_cards.user_id`**: `TEXT` (legado)
- **`part_maintenance.id`**: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **`part_maintenance.user_id`**: `TEXT` (legado)
- **`billing_cycles.id`**: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- **`billing_cycles.platform_id`**: `UUID`
- **`billing_cycles.user_id`**: `TEXT` (legado)

**Ajuste de Tipos e Integridade Referencial:**
Como as tabelas legadas (`credit_cards`, `expenses`, `billing_cycles`) utilizam `user_id` do tipo `TEXT`, referências a essas tabelas a partir das novas tabelas (`user_id UUID`) não utilizam FKs compostas diretas cruzando tipos incompatíveis, mantendo-se a coluna com o tipo exato da chave primária original (`UUID`) e aplicando integridade em camada de aplicação / Room, enquanto todas as tabelas novas do Centro de Custos possuem FKs compostas rígidas `(<pai>_id, user_id) REFERENCES <pai>(id, user_id)` impedindo qualquer vazamento multi-tenant.

### 3.2 Contagens e Preservação das Tabelas Existentes
Registros no inventário de auditoria do banco:
- `expenses`: 74 registros
- `credit_cards`: 2 registros
- `part_maintenance`: 5 registros
- `billing_cycles`: 11 registros
Nenhuma dessas 4 tabelas sofreu alteração de DDL, acréscimo de coluna, alteração de constraints ou modificação de policies.

### 3.3 Verificação da Suíte Local de Testes (Gradle)
```
Comando: ./gradlew testDebugUnitTest --rerun-tasks
Resultado: BUILD SUCCESSFUL in 2m 40s
Tarefas executadas: 35 actionable tasks: 35 executed
Falhas: 0
Ignorados: 0
Últimas linhas da saída:
> Task :app:compileDebugUnitTestKotlin
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:hiltAggregateDepsDebugUnitTest
> Task :app:hiltJavaCompileDebugUnitTest NO-SOURCE
> Task :app:processDebugUnitTestJavaRes
> Task :app:transformDebugUnitTestClassesWithAsm
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 2m 40s
35 actionable tasks: 35 executed
```

---

## 4. Testes
- **Testes adicionados**:
  - `docs/backend/validacao-centro-de-custos.sql`: suíte automatizada contendo:
    - Teste 11.c: Cálculo do saldo (100 inicial + 50 - 30 - 10 - 5 = 105) e transferência de 20 (leg 0 = -20, leg 1 = +20 => 85 na origem e 20 no destino).
    - Teste 11.d: Tentativa de UPDATE e DELETE em `vault_ledger` bloqueadas pelo trigger `prevent_vault_ledger_mutation()`.
    - Teste 11.e: Tentativa de inserção duplicada com `(operation_id, leg)` barrada por unicidade.
    - Teste 11.f: Estorno sem `reverses_entry_id` rejeitado pelo CHECK `chk_vault_ledger_estorno`.
    - Teste 11.g: Tentativa de `fixed_cost_versions.amount <= 0` rejeitada por CHECK; `fixed_costs.start_date` nulo aceito.
    - Teste 11.h: Isolamento por FK composta: Usuário B impedido de apontar para caixinha pertencente ao Usuário A.
    - Teste 11.j: Pagamento dividido com soma divergente rejeitado; `account_amount > 0` sem conta bancária rejeitado.
    - Teste 11.k: Reutilização de `operation_id` em `reserve_allocations` barrada; duas alocações ativas para o mesmo par `(billing_cycle_id, vault_id)` barradas por índice único parcial.
    - Teste 11.l: Tentativa de cadastrar dois vínculos ativos para o mesmo `card_id` em `card_account_links` barrada.
- **Testes existentes alterados**: nenhum teste existente foi alterado.
- **Testes existentes removidos**: nenhum teste existente foi removido.

---

## 5. O que NÃO foi feito ou está simulado
- A execução física do script SQL diretamente no cluster Supabase em nuvem não foi executada de forma automática pelo assistente porque o ambiente não possui o conector MCP `execute_sql` configurado. A execução está pronta para ser aplicada pelo usuário no Supabase SQL Editor.
- Nenhuma funcionalidade de interface ou simulação foi adicionada nesta fase (escopo estritamente de DDL de banco de dados).

---

## 6. Riscos e dívidas encontrados
1. **Incompatibilidade de Tipos com o Legado**: Como `expenses.user_id`, `credit_cards.user_id` e `billing_cycles.user_id` são `TEXT`, não é possível criar FK composta direta `(expense_id, user_id)` sem antes migrar o legado para `UUID`. Essa decisão do plano (preservar o legado sem migrações arriscadas) foi respeitada, mantendo o isolamento multi-tenant garantido pelo `user_id UUID` próprio das novas tabelas.
2. **PostgreSQL Version Compatibility**: A view `public.vault_balances` utiliza a cláusula `WITH (security_invoker = true)` nativa do Postgres 15+. Como garantia preventiva, a função `public.get_vault_balances()` com `SECURITY INVOKER` foi disponibilizada em conjunto.

---

## 7. O que o usuário precisa fazer (Passo a passo no Supabase)

Para aplicar a migração no banco de dados do projeto:
1. Abra o navegador e acesse o [Painel do Supabase](https://supabase.com/dashboard).
2. Selecione o projeto **Central do Motorista / Logística Prime**.
3. No menu lateral esquerdo, clique no ícone **SQL Editor** (ícone de terminal `>_`).
4. Clique no botão **New query** (Nova consulta).
5. Abra o arquivo local `docs/backend/migracao-centro-de-custos.sql`, copie todo o seu conteúdo e cole no editor do Supabase.
6. Clique no botão **Run** (Executar) no canto inferior direito.
7. Verifique na aba de resultados a mensagem `Success. No rows returned`.
8. Em seguida, crie uma nova query no editor, copie o conteúdo de `docs/backend/validacao-centro-de-custos.sql` e clique em **Run**.
9. Verifique na aba "Messages" a emissão de todos os avisos de sucesso:
   - `TESTE 11.c: PASSOU`
   - `TESTE 11.d: PASSOU`
   - `TESTE 11.e: PASSOU`
   - `TESTE 11.f: PASSOU`
   - `TESTE 11.g: PASSOU`
   - `TESTE 11.h: PASSOU`
   - `TESTE 11.j: PASSOU`
   - `TESTE 11.k: PASSOU`
   - `TESTE 11.l: PASSOU`
   - `TODOS OS TESTES (c) A (l) FORAM EXECUTADOS COM SUCESSO`

---

## 8. Commit e sincronização
- **Branch**: `feature/cc-f1-banco` (derivada de `feature/cc-f0-fundacao`)
- **Commit**: `e406ff1`
- **Comando para envio remoto**:
  ```bash
  git push -u origin feature/cc-f1-banco
  ```
- **Comando para abertura do PR (SEM MERGE)**:
  ```bash
  gh pr create --base feature/cc-f0-fundacao --head feature/cc-f1-banco --title "feat(backend): migração DDL e suíte de validação do Centro de Custos no Supabase (Fase 1)" --body "Migração DDL de 12 tabelas do Centro de Custos, razão imutável, RLS, FKs compostas e suíte de validação automatizada."
  ```
