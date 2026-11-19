# Relatório de Execução — Fase 1.1: Correções do SQL do Centro de Custos

**Papel:** Engenheiro de Banco de Dados Sênior (PostgreSQL / Supabase)  
**Data:** 19/11/2026  
**Branch:** `feature/cc-f1-1-correcoes` (base: `origin/feature/cc-f1-banco`)  
**Commit do Código SQL:** `e53cadd` (`e53caddf1b5ae52838bb2dd007d465714fa55627`)  
**Status da Execução:** `EXECUÇÃO PENDENTE (manual)`

---

## 1. Resumo e Decisões Técnicas

A migração original da Fase 1 (`docs/backend/migracao-centro-de-custos.sql`) não foi executada em nenhum banco de produção ou homologação. Assim, todas as correções estruturais foram aplicadas **no lugar**, garantindo integridade referencial nativa, compatibilidade estrita com PostgreSQL >= 15 e conformidade total com os requisitos de isolamento multi-tenant e imutabilidade do razão contábil (`vault_ledger`).

### Edições Aplicadas (E0 a E5)

1. **E0 — Verificação de Versão Mínima do PostgreSQL (>= 15):**
   - Inserida a instrução `-- Requer PostgreSQL >= 15` no cabeçalho.
   - Inserido bloco de guarda logo após `BEGIN;`:
     ```sql
     DO $$ BEGIN IF current_setting('server_version_num')::int < 150000 THEN RAISE EXCEPTION 'Requer PostgreSQL >= 15'; END IF; END $$;
     ```
   - *Justificativa:* O suporte a `ON DELETE SET NULL (column_list)` em foreign keys compostas exige o PostgreSQL 15+.

2. **E1 — Exceção para DELETE em Cascata na Função de Imutabilidade:**
   - Substituída a função `public.prevent_vault_ledger_mutation()` para permitir `DELETE` apenas quando o usuário dono (`user_id`) já tiver sido removido de `auth.users`:
     ```sql
     CREATE OR REPLACE FUNCTION public.prevent_vault_ledger_mutation()
     RETURNS TRIGGER AS $$
     BEGIN
         -- Única exceção: DELETE em cascata quando o usuário dono já foi removido de auth.users
         IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = OLD.user_id) THEN
             RETURN OLD;
         END IF;
         RAISE EXCEPTION 'vault_ledger é imutável: atualizações e exclusões são proibidas';
     END;
     $$ LANGUAGE plpgsql SET search_path = public;
     ```
   - *Justificativa:* Garante cumprimento da LGPD (direito de exclusão da conta pelo usuário via cascata de `auth.users`) sem abrir brecha para que o usuário ou superusuário apague lançamentos individuais enquanto a conta estiver ativa.

3. **E2 — Sintaxe de ON DELETE SET NULL Específica de Coluna em FKs Compostas:**
   - Atualizadas 3 constraints compostas:
     - `fk_vaults_account_user` → `ON DELETE SET NULL (account_id)`
     - `fk_fixed_costs_vault_user` → `ON DELETE SET NULL (vault_id)`
     - `fk_cost_center_settings_vault_user` → `ON DELETE SET NULL (maintenance_vault_id)`
   - *Justificativa:* Em chaves estrangeiras compostas `(fk_id, user_id)`, a sintaxe padrão `ON DELETE SET NULL` tentaria anular também `user_id`, que é `NOT NULL` (violando constraint). O PostgreSQL 15 permite especificar explicitamente qual coluna deve ser anulada.

4. **E3 — Substituição de ON DELETE RESTRICT por ON DELETE NO ACTION:**
   - Substituídas todas as 10 ocorrências de `ON DELETE RESTRICT` nas FKs compostas por `ON DELETE NO ACTION` nas tabelas:
     - `vault_ledger` (`fk_vault_ledger_vault_user`)
     - `expense_settlements` (`fk_expense_settlements_account_user`, `fk_expense_settlements_vault_user`)
     - `fixed_cost_payments` (`fk_fixed_cost_payments_cost_user`, `fk_fixed_cost_payments_account_user`, `fk_fixed_cost_payments_vault_user`)
     - `card_invoice_payments` (`fk_card_invoice_payments_account_user`, `fk_card_invoice_payments_vault_user`)
     - `reserve_allocations` (`fk_reserve_allocations_vault_user`)
     - `card_account_links` (`fk_card_account_links_account_user`)
   - *Justificativa:* `NO ACTION` permite a avaliação de integridade diferida / fim de comando, essencial quando uma deleção de usuário em `auth.users` cascadeia para todas as tabelas filhas sem sofrer bloqueio prévio imediato.

5. **E4 — Restrições e FKs Compostas em vault_ledger:**
   - Removida a FK inline simples de `reverses_entry_id REFERENCES public.vault_ledger(id)`.
   - Adicionada constraint de unicidade: `CONSTRAINT uq_vault_ledger_id_user UNIQUE (id, user_id)`.
   - Adicionadas constraints compostas:
     - `CONSTRAINT fk_vault_ledger_reverses_user FOREIGN KEY (reverses_entry_id, user_id) REFERENCES public.vault_ledger(id, user_id) ON DELETE NO ACTION`
     - `CONSTRAINT fk_vault_ledger_contra_user FOREIGN KEY (contra_vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE NO ACTION`
   - *Justificativa:* Impede que um usuário mal-intencionado ou erro de aplicação faça estorno (`reverses_entry_id`) ou transferência (`contra_vault_id`) referenciando lançamentos ou caixinhas de outro usuário.

6. **E5 — Revogação Prévia Explícita para Papel authenticated:**
   - Inserido antes dos `GRANTs` da seção 14:
     ```sql
     REVOKE ALL ON TABLE public.bank_accounts, public.vaults, public.vault_ledger, public.expense_settlements,
         public.fixed_costs, public.fixed_cost_versions, public.fixed_cost_payments, public.card_invoice_payments,
         public.cost_center_settings, public.reserve_allocations, public.reserve_allocation_days,
         public.card_account_links, public.vault_balances FROM authenticated;
     ```
   - *Justificativa:* Higienização de privilégios para garantir que nenhum default do PostgreSQL (como permissão de `UPDATE`, `DELETE` ou `TRUNCATE` herdada de defaults de schema) permaneça ativo em tabelas imutáveis antes de conceder os privilégios explícitos mínimos.

---

## 2. Novos Testes Automatizados (11.m a 11.s)

Foram implementados no arquivo `docs/backend/validacao-centro-de-custos.sql` os seguintes testes automatizados (executados em blocos `DO` com tratamento de exceção dentro de transação com `ROLLBACK`):

| Teste | Objetivo | Cenário de Teste | Saída Esperada |
|---|---|---|---|
| **11.m** | `ON DELETE SET NULL (account_id)` | Cria conta e caixinha. Deleta conta. | `account_id` vira `NULL`, `user_id` intacto. `TESTE 11.m: PASSOU` |
| **11.n** | `ON DELETE SET NULL (maintenance_vault_id)` | Cria caixinha e associa a `cost_center_settings`. Deleta caixinha. | `maintenance_vault_id` vira `NULL`. `TESTE 11.n: PASSOU` |
| **11.o** | Deleção de usuário em `auth.users` com cascata completa | Popula todas as 12 tabelas para o usuário (inclusive razão com estorno). Deleta usuário de `auth.users`. | 0 linhas restantes nas 12 tabelas. Trigger do razão permite remoção. `TESTE 11.o: PASSOU` |
| **11.p** | Bloqueio de mutação para usuário existente | Cria usuário ativo e lançamento no razão. Tenta `UPDATE` e `DELETE`. | Ambas as operações disparam exceção do trigger `prevent_vault_ledger_mutation`. `TESTE 11.p: PASSOU` |
| **11.q** | FK composta impede referência cruzada de estorno e contra-caixinha | Usuário B tenta inserir no razão com `reverses_entry_id` de A e depois com `contra_vault_id` de A. | Ambas as tentativas falham com `foreign_key_violation`. `TESTE 11.q: PASSOU` |
| **11.r** | Checagem estrita de privilégios SQL | Inspeciona `has_table_privilege` para `authenticated` e `anon` em `vault_ledger`. | `UPDATE`, `DELETE`, `TRUNCATE` de `authenticated` = FALSE. `SELECT`, `INSERT` = TRUE. `SELECT` de `anon` = FALSE. `TESTE 11.r: PASSOU` |
| **11.s** | Isolamento de leitura por RLS via JWT | Configura role local `authenticated` e claim `sub` do usuário B. Consulta tabelas com dados de A. | Retorna 0 linhas para caixinhas, razão e contas bancárias de A. `TESTE 11.s: PASSOU` |

A mensagem final da suíte foi devidamente atualizada para:  
`TODOS OS TESTES (c) A (s) FORAM EXECUTADOS COM SUCESSO`

---

## 3. Status da Execução e Roteiro de Verificação Manual

### Status: `EXECUÇÃO PENDENTE (manual)`
*Motivo:* O ambiente de execução local não dispõe de instância ativa de banco de dados nem cliente `psql`/Docker/Supabase CLI configurado. Em estrita conformidade com o **Princípio 1 (Verdade antes de "verde")**, nenhuma saída fictícia foi gerada.

### Passo a Passo para Execução e Evidência no Supabase Studio:

1. Acesse o **SQL Editor** no painel do Supabase do projeto.
2. **Execução 1 (Aplicação do DDL):**
   - Cole o conteúdo integral de [docs/backend/migracao-centro-de-custos.sql](file:///home/nando/Documentos/logistic-prime/docs/backend/migracao-centro-de-custos.sql).
   - Clique em **Run**.
   - Verifique a mensagem: `Success. No rows returned` (ou `COMMIT`).
3. **Execução 2 (Idempotência):**
   - Execute novamente o mesmo script [docs/backend/migracao-centro-de-custos.sql](file:///home/nando/Documentos/logistic-prime/docs/backend/migracao-centro-de-custos.sql) sem nenhuma modificação.
   - Deve rodar sem nenhum erro ou conflito de constraint.
4. **Execução 3 (Suíte de Testes Automatizada 11.c a 11.s):**
   - Cole o conteúdo de [docs/backend/validacao-centro-de-custos.sql](file:///home/nando/Documentos/logistic-prime/docs/backend/validacao-centro-de-custos.sql).
   - Clique em **Run**.
   - Verifique a saída dos notices na aba de resultados:
     ```text
     NOTICE:  =====================================================
     NOTICE:  INICIANDO BATERIA DE TESTES DE INTEGRIDADE DA FASE 1
     NOTICE:  =====================================================
     NOTICE:  TESTE 11.c: PASSOU (Saldo 105.00 e Transferência 85.00/20.00 confirmados).
     NOTICE:  TESTE 11.d: PASSOU (Imutabilidade de vault_ledger comprovada para UPDATE e DELETE).
     NOTICE:  TESTE 11.e: PASSOU (Violação de unicidade em (operation_id, leg) comprovada).
     NOTICE:  TESTE 11.f: PASSOU (Estorno sem reverses_entry_id rejeitado pelo CHECK).
     NOTICE:  TESTE 11.g: PASSOU (fixed_costs com start_date nulo aceito e amount <= 0 rejeitado).
     NOTICE:  TESTE 11.h: PASSOU (FK composta impediu referência cruzada entre usuários).
     NOTICE:  TESTE 11.j: PASSOU (Regras de pagamento dividido validadas com sucesso).
     NOTICE:  TESTE 11.k: PASSOU (Unicidade de operation_id e restrição de alocações ativas confirmadas).
     NOTICE:  TESTE 11.l: PASSOU (Unicidade de vínculo ativo por card_id confirmada).
     NOTICE:  TESTE 11.m: PASSOU
     NOTICE:  TESTE 11.n: PASSOU
     NOTICE:  TESTE 11.o: PASSOU
     NOTICE:  TESTE 11.p: PASSOU
     NOTICE:  TESTE 11.q: PASSOU
     NOTICE:  TESTE 11.r: PASSOU
     NOTICE:  TESTE 11.s: PASSOU
     NOTICE:  =====================================================
     NOTICE:  TODOS OS TESTES (c) A (s) FORAM EXECUTADOS COM SUCESSO
     NOTICE:  =====================================================
     ```
   - O `ROLLBACK` final garante que nenhuma linha temporária de teste permaneça gravada no banco.

---

## 4. Conferência de Integridade e Regras Proibitivas

- [x] **DROP TABLE / DROP COLUMN:** Nenhuma instrução destrutiva utilizada.
- [x] **TRUNCATE:** Nenhum comando `TRUNCATE` utilizado.
- [x] **Tabelas Legadas:** Nenhuma tabela legada (`expenses`, `credit_cards`, `billing_cycles`, etc.) alterada.
- [x] **Valores de Custo / Service Role:** Nenhum uso de `service_role` ou alteração em regras financeiras de custo.
- [x] **Escopo Restrito:** Apenas `migracao-centro-de-custos.sql`, `validacao-centro-de-custos.sql` e este relatório foram manipulados.
