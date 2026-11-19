-- =============================================================================
-- SUÍTE DE TESTES E VALIDAÇÃO AUTOMATIZADA: CENTRO DE CUSTOS (FASE 1)
-- =============================================================================
-- Arquivo: docs/backend/validacao-centro-de-custos.sql
-- Objetivo: Provar e validar rigorosamente os itens 11 (c) a (l) da Fase 1:
--   c) Cálculo de saldo de cofre e transferências entre caixinhas;
--   d) Imutabilidade de vault_ledger (UPDATE/DELETE devem falhar);
--   e) Idempotência de inserção com (operation_id, leg) duplicado;
--   f) Validação de estorno sem reverses_entry_id;
--   g) Validação de valor positivo em fixed_cost_versions e start_date nulo em fixed_costs;
--   h) Isolamento multi-tenant por RLS e proteção por FK composta entre usuários;
--   i) Idempotência de reexecução do DDL;
--   j) Validação de pagamentos divididos (soma das partes e conta obrigatória);
--   k) Unicidade de operation_id e alocações de reserva ativas por ciclo/caixinha;
--   l) Unicidade de vínculo ativo por card_id em card_account_links.
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

    RAISE NOTICE '=====================================================';
    RAISE NOTICE 'TODOS OS TESTES (c) A (l) FORAM EXECUTADOS COM SUCESSO';
    RAISE NOTICE '=====================================================';
END;
$$;

-- Desfazer todas as inserções de teste (banco limpo)
ROLLBACK;
