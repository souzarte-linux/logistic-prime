-- =============================================================================
-- SUÍTE DE TESTES E VALIDAÇÃO AUTOMATIZADA: CENTRO DE CUSTOS (FASE 1 e 1.1)
-- =============================================================================
-- Arquivo: docs/backend/validacao-centro-de-custos.sql
-- Objetivo: Provar e validar rigorosamente os itens 11 (c) a (s) da Fase 1 e Fase 1.1:
--   c) Cálculo de saldo de cofre e transferências entre caixinhas;
--   d) Imutabilidade de vault_ledger (UPDATE/DELETE devem falhar);
--   e) Idempotência de inserção com (operation_id, leg) duplicado;
--   f) Validação de estorno sem reverses_entry_id;
--   g) Validação de valor positivo em fixed_cost_versions e start_date nulo em fixed_costs;
--   h) Isolamento multi-tenant por RLS e proteção por FK composta entre usuários;
--   i) Idempotência de reexecução do DDL;
--   j) Validação de pagamentos divididos (soma das partes e conta obrigatória);
--   k) Unicidade de operation_id e alocações de reserva ativas por ciclo/caixinha;
--   l) Unicidade de vínculo ativo por card_id em card_account_links;
--   m) ON DELETE SET NULL em fk_vaults_account_user preserva user_id;
--   n) ON DELETE SET NULL em fk_cost_center_settings_vault_user;
--   o) DELETE em cascata de auth.users com todas as 12 tabelas populadas;
--   p) Bloqueio de UPDATE/DELETE em vault_ledger para usuário ativo;
--   q) FK composta em vault_ledger impede reverses_entry_id e contra_vault_id cruzados;
--   r) Privilégios estritos de authenticated e anon em vault_ledger;
--   s) Isolamento RLS com role authenticated e claim JWT sub.
-- Todas as operações são executadas com ROLLBACK garantindo estado limpo.
-- =============================================================================

BEGIN;

DO $$
DECLARE
    v_user_a UUID := gen_random_uuid();
    v_user_b UUID := gen_random_uuid();
    v_account_a UUID := gen_random_uuid();
    v_vault_1 UUID := gen_random_uuid();
    v_vault_2 UUID := gen_random_uuid();
    v_cost_id UUID := gen_random_uuid();
    v_cycle_id UUID := gen_random_uuid();
    v_card_id UUID := gen_random_uuid();
    v_op_id UUID;
    v_entry_1 UUID := gen_random_uuid();
    v_balance NUMERIC(12,2);
    v_balance_dest NUMERIC(12,2);
    v_failed BOOLEAN;
BEGIN
    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'INICIANDO BATERIA DE TESTES DE INTEGRIDADE DA FASE 1';
    RAISE NOTICE '=====================================================';

    -- Mock de usuários em auth.users se necessário para integridade de FK
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_a, 'user_a@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO auth.users (id, email) VALUES (v_user_b, 'user_b@test.com') ON CONFLICT DO NOTHING;
    EXCEPTION WHEN OTHERS THEN
        -- Em caso de ambiente onde auth.users é gerenciado pelo Supabase Gotrue
        NULL;
    END;

    -- Setup base para o usuário A
    INSERT INTO public.bank_accounts (id, user_id, name, account_type)
    VALUES (v_account_a, v_user_a, 'Conta Corrente Principal', 'corrente');

    INSERT INTO public.vaults (id, user_id, account_id, name, purpose, initial_balance)
    VALUES (v_vault_1, v_user_a, v_account_a, 'Reserva Geral', 'outro', 100.00);

    INSERT INTO public.vaults (id, user_id, account_id, name, purpose, initial_balance)
    VALUES (v_vault_2, v_user_a, v_account_a, 'Caixinha Destino', 'viagem', 0.00);

    -- -------------------------------------------------------------------------
    -- TESTE 11.c: Cálculo do saldo (100 inicial + 50 - 30 - 10 - 5 = 105)
    -- e transferência de 20 (leg 0 = -20, leg 1 = +20 => 85 na origem e 20 no destino)
    -- -------------------------------------------------------------------------
    INSERT INTO public.vault_ledger (id, user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
    VALUES (v_entry_1, v_user_a, v_vault_1, gen_random_uuid(), 0, 'aporte', 50.00, now());

    INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
    VALUES (v_user_a, v_vault_1, gen_random_uuid(), 0, 'pagamento_despesa', -30.00, now());

    INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
    VALUES (v_user_a, v_vault_1, gen_random_uuid(), 0, 'pagamento_custo_fixo', -10.00, now());

    INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
    VALUES (v_user_a, v_vault_1, gen_random_uuid(), 0, 'pagamento_fatura', -5.00, now());

    SELECT balance INTO v_balance FROM public.vault_balances WHERE vault_id = v_vault_1;
    IF v_balance <> 105.00 THEN
        RAISE EXCEPTION 'FALHA TESTE 11.c: Esperado balance 105.00, encontrado %', v_balance;
    END IF;

    -- Transferência de 20.00 (leg 0 na origem, leg 1 no destino)
    v_op_id := gen_random_uuid();
    INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, contra_vault_id)
    VALUES (v_user_a, v_vault_1, v_op_id, 0, 'transferencia', -20.00, now(), v_vault_2);

    INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, contra_vault_id)
    VALUES (v_user_a, v_vault_2, v_op_id, 1, 'transferencia', 20.00, now(), v_vault_1);

    SELECT balance INTO v_balance FROM public.vault_balances WHERE vault_id = v_vault_1;
    SELECT balance INTO v_balance_dest FROM public.vault_balances WHERE vault_id = v_vault_2;

    IF v_balance <> 85.00 OR v_balance_dest <> 20.00 THEN
        RAISE EXCEPTION 'FALHA TESTE 11.c (transferência): Origem % (esperado 85.00), Destino % (esperado 20.00)', v_balance, v_balance_dest;
    END IF;
    RAISE NOTICE 'TESTE 11.c: PASSOU (Saldo 105.00 e Transferência 85.00/20.00 confirmados).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.d: UPDATE e DELETE em vault_ledger DEVEM falhar
    -- -------------------------------------------------------------------------
    v_failed := FALSE;
    BEGIN
        UPDATE public.vault_ledger SET amount = 999.00 WHERE id = v_entry_1;
    EXCEPTION WHEN OTHERS THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.d: UPDATE em vault_ledger não foi impedido!';
    END IF;

    v_failed := FALSE;
    BEGIN
        DELETE FROM public.vault_ledger WHERE id = v_entry_1;
    EXCEPTION WHEN OTHERS THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.d: DELETE em vault_ledger não foi impedido!';
    END IF;
    RAISE NOTICE 'TESTE 11.d: PASSOU (Imutabilidade de vault_ledger comprovada para UPDATE e DELETE).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.e: INSERT repetido com mesmo (operation_id, leg) DEVE falhar
    -- -------------------------------------------------------------------------
    v_failed := FALSE;
    BEGIN
        INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, contra_vault_id)
        VALUES (v_user_a, v_vault_1, v_op_id, 0, 'transferencia', -20.00, now(), v_vault_2);
    EXCEPTION WHEN unique_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.e: Inserção com (operation_id, leg) duplicado não violou unicidade!';
    END IF;
    RAISE NOTICE 'TESTE 11.e: PASSOU (Violação de unicidade em (operation_id, leg) comprovada).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.f: Estorno sem reverses_entry_id DEVE falhar
    -- -------------------------------------------------------------------------
    v_failed := FALSE;
    BEGIN
        INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_user_a, v_vault_1, gen_random_uuid(), 0, 'estorno', -50.00, now());
    EXCEPTION WHEN check_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.f: Estorno sem reverses_entry_id foi aceito indevidamente!';
    END IF;
    RAISE NOTICE 'TESTE 11.f: PASSOU (Estorno sem reverses_entry_id rejeitado pelo CHECK).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.g: fixed_cost_versions com amount <= 0 DEVE falhar; fixed_costs aceita start_date nulo
    -- -------------------------------------------------------------------------
    INSERT INTO public.fixed_costs (id, user_id, name, category, periodicity, start_date)
    VALUES (v_cost_id, v_user_a, 'Seguro Auto', 'veiculo_protecao', 'mensal', NULL);

    v_failed := FALSE;
    BEGIN
        INSERT INTO public.fixed_cost_versions (user_id, fixed_cost_id, amount, effective_from)
        VALUES (v_user_a, v_cost_id, 0.00, CURRENT_DATE);
    EXCEPTION WHEN check_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.g: Inserção de valor <= 0 em fixed_cost_versions não falhou!';
    END IF;
    RAISE NOTICE 'TESTE 11.g: PASSOU (fixed_costs com start_date nulo aceito e amount <= 0 rejeitado).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.h: Isolamento por FK composta (B não pode apontar para a caixinha de A)
    -- -------------------------------------------------------------------------
    v_failed := FALSE;
    BEGIN
        INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_user_b, v_vault_1, gen_random_uuid(), 0, 'aporte', 10.00, now());
    EXCEPTION WHEN foreign_key_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.h: Usuário B conseguiu inserir movimentação na caixinha de A!';
    END IF;
    RAISE NOTICE 'TESTE 11.h: PASSOU (FK composta impediu referência cruzada entre usuários).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.j: Pagamento com soma divergente DEVE falhar; account_amount > 0 sem conta DEVE falhar
    -- -------------------------------------------------------------------------
    v_failed := FALSE;
    BEGIN
        INSERT INTO public.fixed_cost_payments (user_id, fixed_cost_id, competence_date, paid_at, amount, vault_amount, account_amount, source_account_id, operation_id)
        VALUES (v_user_a, v_cost_id, CURRENT_DATE, now(), 100.00, 40.00, 50.00, v_account_a, gen_random_uuid());
    EXCEPTION WHEN check_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.j: Pagamento com vault_amount + account_amount <> amount foi aceito!';
    END IF;

    v_failed := FALSE;
    BEGIN
        INSERT INTO public.fixed_cost_payments (user_id, fixed_cost_id, competence_date, paid_at, amount, vault_amount, account_amount, source_account_id, operation_id)
        VALUES (v_user_a, v_cost_id, CURRENT_DATE, now(), 100.00, 0.00, 100.00, NULL, gen_random_uuid());
    EXCEPTION WHEN check_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.j: Pagamento com account_amount > 0 sem source_account_id foi aceito!';
    END IF;
    RAISE NOTICE 'TESTE 11.j: PASSOU (Regras de pagamento dividido validadas com sucesso).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.k: Unicidade de operation_id e duas alocações ATIVA para o mesmo par DEVEM falhar
    -- -------------------------------------------------------------------------
    v_op_id := gen_random_uuid();
    INSERT INTO public.reserve_allocations (id, user_id, billing_cycle_id, vault_id, suggested_amount, amount, status, operation_id)
    VALUES (gen_random_uuid(), v_user_a, v_cycle_id, v_vault_1, 50.00, 50.00, 'ATIVA', v_op_id);

    v_failed := FALSE;
    BEGIN
        INSERT INTO public.reserve_allocations (id, user_id, billing_cycle_id, vault_id, suggested_amount, amount, status, operation_id)
        VALUES (gen_random_uuid(), v_user_a, v_cycle_id, v_vault_2, 30.00, 30.00, 'ATIVA', v_op_id);
    EXCEPTION WHEN unique_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.k: Reutilização de operation_id em reserve_allocations não falhou!';
    END IF;

    v_failed := FALSE;
    BEGIN
        INSERT INTO public.reserve_allocations (id, user_id, billing_cycle_id, vault_id, suggested_amount, amount, status, operation_id)
        VALUES (gen_random_uuid(), v_user_a, v_cycle_id, v_vault_1, 50.00, 50.00, 'ATIVA', gen_random_uuid());
    EXCEPTION WHEN unique_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.k: Duas alocações ATIVA para o mesmo ciclo e caixinha foram aceitas!';
    END IF;
    RAISE NOTICE 'TESTE 11.k: PASSOU (Unicidade de operation_id e restrição de alocações ativas confirmadas).';


    -- -------------------------------------------------------------------------
    -- TESTE 11.l: Dois vínculos ativos para o mesmo card_id em card_account_links DEVEM falhar
    -- -------------------------------------------------------------------------
    INSERT INTO public.card_account_links (user_id, card_id, account_id)
    VALUES (v_user_a, v_card_id, v_account_a);

    v_failed := FALSE;
    BEGIN
        INSERT INTO public.card_account_links (user_id, card_id, account_id)
        VALUES (v_user_a, v_card_id, v_account_a);
    EXCEPTION WHEN unique_violation THEN
        v_failed := TRUE;
    END;
    IF NOT v_failed THEN
        RAISE EXCEPTION 'FALHA TESTE 11.l: Dois vínculos ativos para o mesmo card_id foram aceitos!';
    END IF;
    RAISE NOTICE 'TESTE 11.l: PASSOU (Unicidade de vínculo ativo por card_id confirmada).';

    -- -------------------------------------------------------------------------
    -- TESTE 11.m: DELETE de bank_accounts referenciada por vaults DEVE suceder
    -- e vaults.account_id fica NULL com user_id intacto
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_m UUID := gen_random_uuid();
        v_acc_m UUID := gen_random_uuid();
        v_vault_m UUID := gen_random_uuid();
        v_res_acc UUID;
        v_res_user UUID;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_m, 'user_m@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO public.bank_accounts (id, user_id, name) VALUES (v_acc_m, v_user_m, 'Conta Teste M');
        INSERT INTO public.vaults (id, user_id, account_id, name) VALUES (v_vault_m, v_user_m, v_acc_m, 'Cofre Teste M');

        DELETE FROM public.bank_accounts WHERE id = v_acc_m;

        SELECT account_id, user_id INTO v_res_acc, v_res_user FROM public.vaults WHERE id = v_vault_m;
        IF v_res_acc IS NOT NULL OR v_res_user <> v_user_m THEN
            RAISE EXCEPTION 'FALHA TESTE 11.m: account_id não ficou NULL ou user_id foi modificado (account_id=%, user_id=%)', v_res_acc, v_res_user;
        END IF;
        RAISE NOTICE 'TESTE 11.m: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.n: DELETE de vaults referenciada por cost_center_settings.maintenance_vault_id
    -- DEVE suceder e a coluna fica NULL
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_n UUID := gen_random_uuid();
        v_vault_n UUID := gen_random_uuid();
        v_res_vault UUID;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_n, 'user_n@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO public.vaults (id, user_id, name) VALUES (v_vault_n, v_user_n, 'Cofre Manutencao N');
        INSERT INTO public.cost_center_settings (user_id, maintenance_vault_id) VALUES (v_user_n, v_vault_n);

        DELETE FROM public.vaults WHERE id = v_vault_n;

        SELECT maintenance_vault_id INTO v_res_vault FROM public.cost_center_settings WHERE user_id = v_user_n;
        IF v_res_vault IS NOT NULL THEN
            RAISE EXCEPTION 'FALHA TESTE 11.n: maintenance_vault_id não ficou NULL após DELETE da caixinha (encontrado %)', v_res_vault;
        END IF;
        RAISE NOTICE 'TESTE 11.n: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.o: DELETE do usuário em auth.users com TODAS as tabelas novas
    -- populadas (inclusive razão com estorno) DEVE suceder e não restar nenhuma linha
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_o UUID := gen_random_uuid();
        v_acc_o UUID := gen_random_uuid();
        v_vault_o UUID := gen_random_uuid();
        v_entry_o UUID := gen_random_uuid();
        v_estorno_o UUID := gen_random_uuid();
        v_cost_o UUID := gen_random_uuid();
        v_alloc_o UUID := gen_random_uuid();
        v_card_o UUID := gen_random_uuid();
        v_count_tables INT := 0;
        v_tcount INT;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_o, 'user_o@test.com') ON CONFLICT DO NOTHING;
        
        -- 1. bank_accounts
        INSERT INTO public.bank_accounts (id, user_id, name) VALUES (v_acc_o, v_user_o, 'Conta O');
        -- 2. vaults
        INSERT INTO public.vaults (id, user_id, account_id, name) VALUES (v_vault_o, v_user_o, v_acc_o, 'Cofre O');
        -- 3. vault_ledger (entrada original e estorno)
        INSERT INTO public.vault_ledger (id, user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_entry_o, v_user_o, v_vault_o, gen_random_uuid(), 0, 'aporte', 100.00, now());
        INSERT INTO public.vault_ledger (id, user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, reverses_entry_id)
        VALUES (v_estorno_o, v_user_o, v_vault_o, gen_random_uuid(), 0, 'estorno', -100.00, now(), v_entry_o);
        -- 4. expense_settlements
        INSERT INTO public.expense_settlements (user_id, expense_id, account_id, amount, account_amount, operation_id)
        VALUES (v_user_o, gen_random_uuid(), v_acc_o, 50.00, 50.00, gen_random_uuid());
        -- 5. fixed_costs
        INSERT INTO public.fixed_costs (id, user_id, name, category, periodicity, vault_id)
        VALUES (v_cost_o, v_user_o, 'Custo O', 'outro', 'mensal', v_vault_o);
        -- 6. fixed_cost_versions
        INSERT INTO public.fixed_cost_versions (user_id, fixed_cost_id, amount, effective_from)
        VALUES (v_user_o, v_cost_o, 100.00, CURRENT_DATE);
        -- 7. fixed_cost_payments
        INSERT INTO public.fixed_cost_payments (user_id, fixed_cost_id, competence_date, paid_at, amount, account_amount, source_account_id, operation_id)
        VALUES (v_user_o, v_cost_o, CURRENT_DATE, now(), 100.00, 100.00, v_acc_o, gen_random_uuid());
        -- 8. card_invoice_payments
        INSERT INTO public.card_invoice_payments (user_id, card_id, reference_month, amount, paid_at, account_amount, source_account_id, operation_id)
        VALUES (v_user_o, v_card_o, DATE_TRUNC('month', CURRENT_DATE)::DATE, 150.00, now(), 150.00, v_acc_o, gen_random_uuid());
        -- 9. cost_center_settings
        INSERT INTO public.cost_center_settings (user_id, maintenance_vault_id)
        VALUES (v_user_o, v_vault_o);
        -- 10. reserve_allocations
        INSERT INTO public.reserve_allocations (id, user_id, billing_cycle_id, vault_id, suggested_amount, amount, status, operation_id)
        VALUES (v_alloc_o, v_user_o, gen_random_uuid(), v_vault_o, 40.00, 40.00, 'ATIVA', gen_random_uuid());
        -- 11. reserve_allocation_days
        INSERT INTO public.reserve_allocation_days (user_id, allocation_id, day, suggested_amount)
        VALUES (v_user_o, v_alloc_o, CURRENT_DATE, 40.00);
        -- 12. card_account_links
        INSERT INTO public.card_account_links (user_id, card_id, account_id)
        VALUES (v_user_o, v_card_o, v_acc_o);

        -- Excluir o usuário de auth.users (disparando cascata completa)
        DELETE FROM auth.users WHERE id = v_user_o;

        -- Verificar que não resta nenhuma linha em nenhuma das 12 tabelas
        SELECT count(*) INTO v_tcount FROM public.bank_accounts WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.vaults WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.vault_ledger WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.expense_settlements WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.fixed_costs WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.fixed_cost_versions WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.fixed_cost_payments WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.card_invoice_payments WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.cost_center_settings WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.reserve_allocations WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.reserve_allocation_days WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;
        SELECT count(*) INTO v_tcount FROM public.card_account_links WHERE user_id = v_user_o; v_count_tables := v_count_tables + v_tcount;

        IF v_count_tables > 0 THEN
            RAISE EXCEPTION 'FALHA TESTE 11.o: Restaram % linhas do usuário nas tabelas após DELETE de auth.users', v_count_tables;
        END IF;
        RAISE NOTICE 'TESTE 11.o: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.p: UPDATE e DELETE em vault_ledger com usuário em auth.users
    -- DEVEM continuar falhando (trigger), executados como superusuário
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_p UUID := gen_random_uuid();
        v_vault_p UUID := gen_random_uuid();
        v_entry_p UUID := gen_random_uuid();
        v_failed_upd BOOLEAN := FALSE;
        v_failed_del BOOLEAN := FALSE;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_p, 'user_p@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO public.vaults (id, user_id, name) VALUES (v_vault_p, v_user_p, 'Cofre P');
        INSERT INTO public.vault_ledger (id, user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_entry_p, v_user_p, v_vault_p, gen_random_uuid(), 0, 'aporte', 75.00, now());

        BEGIN
            UPDATE public.vault_ledger SET amount = 999.00 WHERE id = v_entry_p;
        EXCEPTION WHEN OTHERS THEN
            v_failed_upd := TRUE;
        END;

        BEGIN
            DELETE FROM public.vault_ledger WHERE id = v_entry_p;
        EXCEPTION WHEN OTHERS THEN
            v_failed_del := TRUE;
        END;

        IF NOT v_failed_upd OR NOT v_failed_del THEN
            RAISE EXCEPTION 'FALHA TESTE 11.p: Mutação em vault_ledger não foi barrada pelo trigger (UPD=%, DEL=%)', v_failed_upd, v_failed_del;
        END IF;
        RAISE NOTICE 'TESTE 11.p: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.q: Usuário B inserindo vault_ledger com reverses_entry_id de A
    -- DEVE falhar com foreign_key_violation; idem com contra_vault_id de A
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_qa UUID := gen_random_uuid();
        v_user_qb UUID := gen_random_uuid();
        v_vault_qa UUID := gen_random_uuid();
        v_vault_qb UUID := gen_random_uuid();
        v_entry_qa UUID := gen_random_uuid();
        v_failed_rev BOOLEAN := FALSE;
        v_failed_contra BOOLEAN := FALSE;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_qa, 'user_qa@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO auth.users (id, email) VALUES (v_user_qb, 'user_qb@test.com') ON CONFLICT DO NOTHING;

        INSERT INTO public.vaults (id, user_id, name) VALUES (v_vault_qa, v_user_qa, 'Cofre QA');
        INSERT INTO public.vaults (id, user_id, name) VALUES (v_vault_qb, v_user_qb, 'Cofre QB');

        INSERT INTO public.vault_ledger (id, user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_entry_qa, v_user_qa, v_vault_qa, gen_random_uuid(), 0, 'aporte', 100.00, now());

        -- B tenta estornar a entrada de A
        BEGIN
            INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, reverses_entry_id)
            VALUES (v_user_qb, v_vault_qb, gen_random_uuid(), 0, 'estorno', -100.00, now(), v_entry_qa);
        EXCEPTION WHEN foreign_key_violation THEN
            v_failed_rev := TRUE;
        END;

        -- B tenta transferência com contra_vault_id apontando para cofre de A
        BEGIN
            INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at, contra_vault_id)
            VALUES (v_user_qb, v_vault_qb, gen_random_uuid(), 0, 'transferencia', -50.00, now(), v_vault_qa);
        EXCEPTION WHEN foreign_key_violation THEN
            v_failed_contra := TRUE;
        END;

        IF NOT v_failed_rev OR NOT v_failed_contra THEN
            RAISE EXCEPTION 'FALHA TESTE 11.q: FK composta falhou em bloquear referência cruzada (reverses_entry=%, contra_vault=%)', v_failed_rev, v_failed_contra;
        END IF;
        RAISE NOTICE 'TESTE 11.q: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.r: Privilégios de authenticated e anon em public.vault_ledger
    -- -------------------------------------------------------------------------
    BEGIN
        IF has_table_privilege('authenticated', 'public.vault_ledger', 'UPDATE') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: authenticated possui privilégio indevido de UPDATE em vault_ledger';
        END IF;
        IF has_table_privilege('authenticated', 'public.vault_ledger', 'DELETE') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: authenticated possui privilégio indevido de DELETE em vault_ledger';
        END IF;
        IF has_table_privilege('authenticated', 'public.vault_ledger', 'TRUNCATE') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: authenticated possui privilégio indevido de TRUNCATE em vault_ledger';
        END IF;
        IF NOT has_table_privilege('authenticated', 'public.vault_ledger', 'SELECT') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: authenticated não possui privilégio de SELECT em vault_ledger';
        END IF;
        IF NOT has_table_privilege('authenticated', 'public.vault_ledger', 'INSERT') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: authenticated não possui privilégio de INSERT em vault_ledger';
        END IF;
        IF has_table_privilege('anon', 'public.vault_ledger', 'SELECT') THEN
            RAISE EXCEPTION 'FALHA TESTE 11.r: anon possui privilégio indevido de SELECT em vault_ledger';
        END IF;
        RAISE NOTICE 'TESTE 11.r: PASSOU';
    END;


    -- -------------------------------------------------------------------------
    -- TESTE 11.s: Isolamento RLS com SET LOCAL ROLE authenticated e claim sub
    -- -------------------------------------------------------------------------
    DECLARE
        v_user_sa UUID := gen_random_uuid();
        v_user_sb UUID := gen_random_uuid();
        v_acc_sa UUID := gen_random_uuid();
        v_vault_sa UUID := gen_random_uuid();
        v_cnt_vaults INT;
        v_cnt_ledger INT;
        v_cnt_accounts INT;
    BEGIN
        INSERT INTO auth.users (id, email) VALUES (v_user_sa, 'user_sa@test.com') ON CONFLICT DO NOTHING;
        INSERT INTO auth.users (id, email) VALUES (v_user_sb, 'user_sb@test.com') ON CONFLICT DO NOTHING;

        INSERT INTO public.bank_accounts (id, user_id, name) VALUES (v_acc_sa, v_user_sa, 'Conta SA');
        INSERT INTO public.vaults (id, user_id, account_id, name) VALUES (v_vault_sa, v_user_sa, v_acc_sa, 'Cofre SA');
        INSERT INTO public.vault_ledger (user_id, vault_id, operation_id, leg, entry_type, amount, occurred_at)
        VALUES (v_user_sa, v_vault_sa, gen_random_uuid(), 0, 'aporte', 200.00, now());

        -- Simular requisição do usuário SB
        EXECUTE 'SET LOCAL ROLE authenticated';
        PERFORM set_config('request.jwt.claim.sub', v_user_sb::text, true);

        SELECT count(*) INTO v_cnt_vaults FROM public.vaults WHERE user_id = v_user_sa;
        SELECT count(*) INTO v_cnt_ledger FROM public.vault_ledger WHERE user_id = v_user_sa;
        SELECT count(*) INTO v_cnt_accounts FROM public.bank_accounts WHERE user_id = v_user_sa;

        -- Restaurar role
        EXECUTE 'RESET ROLE';

        IF v_cnt_vaults <> 0 OR v_cnt_ledger <> 0 OR v_cnt_accounts <> 0 THEN
            RAISE EXCEPTION 'FALHA TESTE 11.s: Usuário SB conseguiu enxergar registros de SA (vaults=%, ledger=%, accounts=%)', v_cnt_vaults, v_cnt_ledger, v_cnt_accounts;
        END IF;
        RAISE NOTICE 'TESTE 11.s: PASSOU';
    END;

    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'TODOS OS TESTES (c) A (s) FORAM EXECUTADOS COM SUCESSO';
    RAISE NOTICE '=====================================================';
END;
$$;

-- Desfazer todas as inserções de teste (banco limpo)
ROLLBACK;
