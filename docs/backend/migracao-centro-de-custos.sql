-- =============================================================================
-- CENTRAL DO MOTORISTA / LOGÍSTICA PRIME - MÓDULO CENTRO DE CUSTOS
-- =============================================================================
-- Arquivo: docs/backend/migracao-centro-de-custos.sql
-- Fase: FASE 1 — Banco de Dados (Supabase / PostgreSQL)
-- Descrição: Criação das 12 tabelas relacionais do Centro de Custos, razão imutável
--            (vault_ledger), view de conciliação de saldos, índices de performance,
--            triggers de updated_at, proteções de imutabilidade, restrições de integridade
--            e políticas de RLS estritas com isolamento por usuário e FKs compostas.
-- Diretriz: Script 100% IDEMPOTENTE executado em transação única (BEGIN ... COMMIT).
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. FUNÇÕES AUXILIARES DE SISTEMA E TRIGGERS
-- -----------------------------------------------------------------------------

-- Função para atualização automática da coluna updated_at
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SET search_path = public;

-- Função de bloqueio para garantia de imutabilidade estrita do razão (vault_ledger)
CREATE OR REPLACE FUNCTION public.prevent_vault_ledger_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'vault_ledger é imutável: atualizações e exclusões são proibidas';
END;
$$ LANGUAGE plpgsql SET search_path = public;


-- -----------------------------------------------------------------------------
-- 1. TABELA: bank_accounts (Contas Bancárias / Instituições)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    institution TEXT,
    agency TEXT,
    account_last_digits TEXT CHECK (account_last_digits IS NULL OR length(account_last_digits) <= 6),
    account_type TEXT CHECK (account_type IS NULL OR account_type IN ('corrente', 'poupanca', 'pagamento', 'outra')),
    informed_balance NUMERIC(12,2),
    informed_balance_at TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_bank_accounts_id_user UNIQUE (id, user_id)
);

DROP TRIGGER IF EXISTS trg_bank_accounts_updated_at ON public.bank_accounts;
CREATE TRIGGER trg_bank_accounts_updated_at
    BEFORE UPDATE ON public.bank_accounts
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.bank_accounts ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bank_accounts_authenticated_all" ON public.bank_accounts;
CREATE POLICY "bank_accounts_authenticated_all"
    ON public.bank_accounts
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);


-- -----------------------------------------------------------------------------
-- 2. TABELA: vaults (Caixinhas / Cofres Virtuais)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.vaults (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    account_id UUID,
    name TEXT NOT NULL,
    purpose TEXT NOT NULL DEFAULT 'outro' CHECK (purpose IN ('manutencao', 'impostos', 'garagem', 'seguro', 'custos_fixos', 'viagem', 'emergencia', 'outro')),
    goal_amount NUMERIC(12,2) CHECK (goal_amount IS NULL OR goal_amount > 0),
    initial_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    tracking_start_date DATE NOT NULL DEFAULT CURRENT_DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_vaults_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_vaults_account_user FOREIGN KEY (account_id, user_id) REFERENCES public.bank_accounts(id, user_id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_vaults_updated_at ON public.vaults;
CREATE TRIGGER trg_vaults_updated_at
    BEFORE UPDATE ON public.vaults
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.vaults ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "vaults_authenticated_all" ON public.vaults;
CREATE POLICY "vaults_authenticated_all"
    ON public.vaults
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_vaults_account_lower_name 
    ON public.vaults (account_id, lower(name)) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_vaults_user_account 
    ON public.vaults (user_id, account_id);


-- -----------------------------------------------------------------------------
-- 3. TABELA: vault_ledger (Razão Imutável de Movimentações das Caixinhas)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.vault_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    vault_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    leg SMALLINT NOT NULL DEFAULT 0 CHECK (leg IN (0, 1)),
    entry_type TEXT NOT NULL CHECK (entry_type IN ('aporte', 'retirada', 'transferencia', 'ajuste', 'pagamento_despesa', 'pagamento_custo_fixo', 'pagamento_fatura', 'estorno')),
    amount NUMERIC(12,2) NOT NULL CHECK (amount <> 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    ref_type TEXT CHECK (ref_type IS NULL OR ref_type IN ('despesa', 'custo_fixo', 'fatura', 'manual', 'baixa_fatura')),
    ref_id TEXT,
    contra_vault_id UUID,
    reverses_entry_id UUID REFERENCES public.vault_ledger(id),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT uq_vault_ledger_operation_leg UNIQUE (operation_id, leg),
    CONSTRAINT chk_vault_ledger_estorno CHECK (entry_type <> 'estorno' OR reverses_entry_id IS NOT NULL),
    CONSTRAINT chk_vault_ledger_transferencia CHECK (entry_type <> 'transferencia' OR contra_vault_id IS NOT NULL),
    CONSTRAINT fk_vault_ledger_vault_user FOREIGN KEY (vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE RESTRICT
);

-- Bloqueio estrito de UPDATE e DELETE via trigger
DROP TRIGGER IF EXISTS trg_vault_ledger_prevent_mutation ON public.vault_ledger;
CREATE TRIGGER trg_vault_ledger_prevent_mutation
    BEFORE UPDATE OR DELETE ON public.vault_ledger
    FOR EACH ROW
    EXECUTE FUNCTION public.prevent_vault_ledger_mutation();

ALTER TABLE public.vault_ledger ENABLE ROW LEVEL SECURITY;

-- Somente políticas de SELECT e INSERT para o razão imutável (sem UPDATE nem DELETE)
DROP POLICY IF EXISTS "vault_ledger_select" ON public.vault_ledger;
CREATE POLICY "vault_ledger_select"
    ON public.vault_ledger
    FOR SELECT
    TO authenticated
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "vault_ledger_insert" ON public.vault_ledger;
CREATE POLICY "vault_ledger_insert"
    ON public.vault_ledger
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_vault_ledger_user_occurred 
    ON public.vault_ledger (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_vault_ledger_vault_occurred 
    ON public.vault_ledger (vault_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_vault_ledger_ref 
    ON public.vault_ledger (ref_type, ref_id);


-- -----------------------------------------------------------------------------
-- 4. TABELA: expense_settlements (Liquidação e Origem de Despesas PIX / Débito)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.expense_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    expense_id UUID NOT NULL UNIQUE,
    kind TEXT NOT NULL DEFAULT 'pix' CHECK (kind IN ('pix', 'debito')),
    account_id UUID,
    vault_id UUID,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    vault_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (vault_amount >= 0),
    account_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (account_amount >= 0),
    status TEXT NOT NULL DEFAULT 'PENDENTE_DESPESA' CHECK (status IN ('PENDENTE_DESPESA', 'CONFIRMADA', 'ABORTADA')),
    operation_id UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_expense_settlements_sum CHECK (vault_amount + account_amount = amount),
    CONSTRAINT chk_expense_settlements_account CHECK (account_amount = 0 OR account_id IS NOT NULL),
    CONSTRAINT chk_expense_settlements_vault CHECK (vault_amount = 0 OR vault_id IS NOT NULL),
    CONSTRAINT fk_expense_settlements_account_user FOREIGN KEY (account_id, user_id) REFERENCES public.bank_accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_settlements_vault_user FOREIGN KEY (vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE RESTRICT
);

DROP TRIGGER IF EXISTS trg_expense_settlements_updated_at ON public.expense_settlements;
CREATE TRIGGER trg_expense_settlements_updated_at
    BEFORE UPDATE ON public.expense_settlements
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.expense_settlements ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "expense_settlements_authenticated_all" ON public.expense_settlements;
CREATE POLICY "expense_settlements_authenticated_all"
    ON public.expense_settlements
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_expense_settlements_vault_id 
    ON public.expense_settlements (vault_id);


-- -----------------------------------------------------------------------------
-- 5. TABELA: fixed_costs (Custos Fixos Paramétricos — SEM COLUNA DE VALOR)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.fixed_costs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('veiculo_tributo', 'veiculo_protecao', 'garagem', 'financiamento', 'comunicacao', 'trabalho_fiscal', 'documentos', 'protecao_pessoal', 'equipamento', 'outro')),
    catalog_key TEXT,
    periodicity TEXT NOT NULL CHECK (periodicity IN ('semanal', 'mensal', 'bimestral', 'trimestral', 'semestral', 'anual')),
    due_day SMALLINT CHECK (due_day BETWEEN 1 AND 31),
    due_month SMALLINT CHECK (due_month BETWEEN 1 AND 12),
    start_date DATE,
    end_date DATE,
    vault_id UUID,
    is_vehicle_related BOOLEAN NOT NULL DEFAULT FALSE,
    analytical BOOLEAN NOT NULL DEFAULT FALSE,
    seeded BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_fixed_costs_dates CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT uq_fixed_costs_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_fixed_costs_vault_user FOREIGN KEY (vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_fixed_costs_updated_at ON public.fixed_costs;
CREATE TRIGGER trg_fixed_costs_updated_at
    BEFORE UPDATE ON public.fixed_costs
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.fixed_costs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "fixed_costs_authenticated_all" ON public.fixed_costs;
CREATE POLICY "fixed_costs_authenticated_all"
    ON public.fixed_costs
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_fixed_costs_user_catalog_key 
    ON public.fixed_costs (user_id, catalog_key) 
    WHERE catalog_key IS NOT NULL AND catalog_key <> 'outro' AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_fixed_costs_user_active 
    ON public.fixed_costs (user_id, active);


-- -----------------------------------------------------------------------------
-- 6. TABELA: fixed_cost_versions (Histórico de Valores Vigentes dos Custos Fixos)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.fixed_cost_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fixed_cost_id UUID NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    effective_from DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT uq_fixed_cost_versions_cost_effective UNIQUE (fixed_cost_id, effective_from),
    CONSTRAINT fk_fixed_cost_versions_cost_user FOREIGN KEY (fixed_cost_id, user_id) REFERENCES public.fixed_costs(id, user_id) ON DELETE CASCADE
);

DROP TRIGGER IF EXISTS trg_fixed_cost_versions_updated_at ON public.fixed_cost_versions;
CREATE TRIGGER trg_fixed_cost_versions_updated_at
    BEFORE UPDATE ON public.fixed_cost_versions
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.fixed_cost_versions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "fixed_cost_versions_authenticated_all" ON public.fixed_cost_versions;
CREATE POLICY "fixed_cost_versions_authenticated_all"
    ON public.fixed_cost_versions
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_fixed_cost_versions_cost_effective 
    ON public.fixed_cost_versions (fixed_cost_id, effective_from DESC);


-- -----------------------------------------------------------------------------
-- 7. TABELA: fixed_cost_payments (Pagamentos / Liquidações de Custos Fixos)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.fixed_cost_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fixed_cost_id UUID NOT NULL,
    competence_date DATE NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    source_account_id UUID,
    source_vault_id UUID,
    vault_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (vault_amount >= 0),
    account_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (account_amount >= 0),
    operation_id UUID NOT NULL UNIQUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_fixed_cost_payments_sum CHECK (vault_amount + account_amount = amount),
    CONSTRAINT chk_fixed_cost_payments_account CHECK (account_amount = 0 OR source_account_id IS NOT NULL),
    CONSTRAINT chk_fixed_cost_payments_vault CHECK (vault_amount = 0 OR source_vault_id IS NOT NULL),
    CONSTRAINT fk_fixed_cost_payments_cost_user FOREIGN KEY (fixed_cost_id, user_id) REFERENCES public.fixed_costs(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_fixed_cost_payments_account_user FOREIGN KEY (source_account_id, user_id) REFERENCES public.bank_accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_fixed_cost_payments_vault_user FOREIGN KEY (source_vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE RESTRICT
);

DROP TRIGGER IF EXISTS trg_fixed_cost_payments_updated_at ON public.fixed_cost_payments;
CREATE TRIGGER trg_fixed_cost_payments_updated_at
    BEFORE UPDATE ON public.fixed_cost_payments
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.fixed_cost_payments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "fixed_cost_payments_authenticated_all" ON public.fixed_cost_payments;
CREATE POLICY "fixed_cost_payments_authenticated_all"
    ON public.fixed_cost_payments
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_fixed_cost_payments_cost_competence 
    ON public.fixed_cost_payments (fixed_cost_id, competence_date) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_fixed_cost_payments_vault 
    ON public.fixed_cost_payments (source_vault_id);


-- -----------------------------------------------------------------------------
-- 8. TABELA: card_invoice_payments (Pagamentos de Faturas de Cartão de Crédito)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.card_invoice_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    card_id UUID NOT NULL,
    reference_month DATE NOT NULL CHECK (EXTRACT(DAY FROM reference_month) = 1),
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    paid_at TIMESTAMPTZ NOT NULL,
    source_account_id UUID,
    source_vault_id UUID,
    vault_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (vault_amount >= 0),
    account_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (account_amount >= 0),
    operation_id UUID NOT NULL UNIQUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_card_invoice_payments_sum CHECK (vault_amount + account_amount = amount),
    CONSTRAINT chk_card_invoice_payments_account CHECK (account_amount = 0 OR source_account_id IS NOT NULL),
    CONSTRAINT chk_card_invoice_payments_vault CHECK (vault_amount = 0 OR source_vault_id IS NOT NULL),
    CONSTRAINT fk_card_invoice_payments_account_user FOREIGN KEY (source_account_id, user_id) REFERENCES public.bank_accounts(id, user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_card_invoice_payments_vault_user FOREIGN KEY (source_vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE RESTRICT
);

DROP TRIGGER IF EXISTS trg_card_invoice_payments_updated_at ON public.card_invoice_payments;
CREATE TRIGGER trg_card_invoice_payments_updated_at
    BEFORE UPDATE ON public.card_invoice_payments
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.card_invoice_payments ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "card_invoice_payments_authenticated_all" ON public.card_invoice_payments;
CREATE POLICY "card_invoice_payments_authenticated_all"
    ON public.card_invoice_payments
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE INDEX IF NOT EXISTS idx_card_invoice_payments_card_month 
    ON public.card_invoice_payments (card_id, reference_month);


-- -----------------------------------------------------------------------------
-- 9. TABELA: cost_center_settings (Configurações do Centro de Custos por Usuário)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.cost_center_settings (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    maintenance_vault_id UUID,
    wear_provision_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fixed_cost_provision_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reserve_mode TEXT NOT NULL DEFAULT 'CALENDAR' CHECK (reserve_mode IN ('CALENDAR', 'WORKING_DAYS')),
    planned_weekdays SMALLINT NOT NULL DEFAULT 127 CHECK (planned_weekdays BETWEEN 0 AND 127),
    days_off JSONB NOT NULL DEFAULT '[]'::jsonb,
    seeded_catalog_version INT NOT NULL DEFAULT 0,
    dismissed_seed_keys JSONB NOT NULL DEFAULT '[]'::jsonb,
    reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_offsets JSONB NOT NULL DEFAULT '[3,0]'::jsonb,
    cost_center_started_on DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT fk_cost_center_settings_vault_user FOREIGN KEY (maintenance_vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE SET NULL
);

DROP TRIGGER IF EXISTS trg_cost_center_settings_updated_at ON public.cost_center_settings;
CREATE TRIGGER trg_cost_center_settings_updated_at
    BEFORE UPDATE ON public.cost_center_settings
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.cost_center_settings ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "cost_center_settings_authenticated_all" ON public.cost_center_settings;
CREATE POLICY "cost_center_settings_authenticated_all"
    ON public.cost_center_settings
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);


-- -----------------------------------------------------------------------------
-- 10. TABELA: reserve_allocations (Alocações Automáticas de Reserva na Baixa)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reserve_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    billing_cycle_id UUID NOT NULL,
    vault_id UUID NOT NULL,
    suggested_amount NUMERIC(12,2) NOT NULL CHECK (suggested_amount >= 0),
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    net_cap_applied BOOLEAN NOT NULL DEFAULT FALSE,
    status TEXT NOT NULL DEFAULT 'ATIVA' CHECK (status IN ('ATIVA', 'DESFEITA', 'DISPENSADA')),
    operation_id UUID NOT NULL UNIQUE,
    undone_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_reserve_allocations_id_user UNIQUE (id, user_id),
    CONSTRAINT fk_reserve_allocations_vault_user FOREIGN KEY (vault_id, user_id) REFERENCES public.vaults(id, user_id) ON DELETE RESTRICT
);

DROP TRIGGER IF EXISTS trg_reserve_allocations_updated_at ON public.reserve_allocations;
CREATE TRIGGER trg_reserve_allocations_updated_at
    BEFORE UPDATE ON public.reserve_allocations
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.reserve_allocations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "reserve_allocations_authenticated_all" ON public.reserve_allocations;
CREATE POLICY "reserve_allocations_authenticated_all"
    ON public.reserve_allocations
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reserve_allocations_cycle_vault 
    ON public.reserve_allocations (billing_cycle_id, vault_id) 
    WHERE status IN ('ATIVA', 'DISPENSADA');

CREATE INDEX IF NOT EXISTS idx_reserve_allocations_cycle 
    ON public.reserve_allocations (billing_cycle_id);


-- -----------------------------------------------------------------------------
-- 11. TABELA: reserve_allocation_days (Detalhamento Diário das Alocações)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.reserve_allocation_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    allocation_id UUID NOT NULL,
    day DATE NOT NULL,
    platform_id TEXT,
    suggested_amount NUMERIC(14,6) NOT NULL CHECK (suggested_amount >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    CONSTRAINT fk_reserve_allocation_days_alloc_user FOREIGN KEY (allocation_id, user_id) REFERENCES public.reserve_allocations(id, user_id) ON DELETE CASCADE
);

DROP TRIGGER IF EXISTS trg_reserve_allocation_days_updated_at ON public.reserve_allocation_days;
CREATE TRIGGER trg_reserve_allocation_days_updated_at
    BEFORE UPDATE ON public.reserve_allocation_days
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.reserve_allocation_days ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "reserve_allocation_days_authenticated_all" ON public.reserve_allocation_days;
CREATE POLICY "reserve_allocation_days_authenticated_all"
    ON public.reserve_allocation_days
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reserve_allocation_days_alloc_day_plat 
    ON public.reserve_allocation_days (allocation_id, day, COALESCE(platform_id, ''));

CREATE INDEX IF NOT EXISTS idx_reserve_allocation_days_alloc_day 
    ON public.reserve_allocation_days (allocation_id, day);


-- -----------------------------------------------------------------------------
-- 12. TABELA: card_account_links (Vínculo de Cartões de Débito a Contas Bancárias)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.card_account_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    card_id UUID NOT NULL,
    account_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
    deleted_at TIMESTAMPTZ,
    CONSTRAINT fk_card_account_links_account_user FOREIGN KEY (account_id, user_id) REFERENCES public.bank_accounts(id, user_id) ON DELETE RESTRICT
);

DROP TRIGGER IF EXISTS trg_card_account_links_updated_at ON public.card_account_links;
CREATE TRIGGER trg_card_account_links_updated_at
    BEFORE UPDATE ON public.card_account_links
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

ALTER TABLE public.card_account_links ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "card_account_links_authenticated_all" ON public.card_account_links;
CREATE POLICY "card_account_links_authenticated_all"
    ON public.card_account_links
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_card_account_links_card_id 
    ON public.card_account_links (card_id) 
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_card_account_links_account 
    ON public.card_account_links (account_id);


-- -----------------------------------------------------------------------------
-- 13. VIEW E FUNÇÃO DE SALDO DAS CAIXINHAS (CONCILIAÇÃO E AUDITORIA)
-- -----------------------------------------------------------------------------
-- View com security_invoker = true para respeitar o RLS das tabelas base
CREATE OR REPLACE VIEW public.vault_balances WITH (security_invoker = true) AS
SELECT
    v.id AS vault_id,
    v.user_id,
    v.account_id,
    (v.initial_balance + COALESCE(SUM(vl.amount), 0.00))::NUMERIC(12,2) AS balance
FROM public.vaults v
LEFT JOIN public.vault_ledger vl ON vl.vault_id = v.id
GROUP BY v.id, v.user_id, v.account_id, v.initial_balance;

-- Função fallback para compatibilidade abrangente
CREATE OR REPLACE FUNCTION public.get_vault_balances()
RETURNS TABLE (
    vault_id UUID,
    user_id UUID,
    account_id UUID,
    balance NUMERIC(12,2)
)
LANGUAGE sql
SECURITY INVOKER
SET search_path = public
AS $$
    SELECT
        v.id AS vault_id,
        v.user_id,
        v.account_id,
        (v.initial_balance + COALESCE(SUM(vl.amount), 0.00))::NUMERIC(12,2) AS balance
    FROM public.vaults v
    LEFT JOIN public.vault_ledger vl ON vl.vault_id = v.id
    GROUP BY v.id, v.user_id, v.account_id, v.initial_balance;
$$;


-- -----------------------------------------------------------------------------
-- 14. PERMISSÕES E PRIVILÉGIOS (GRANTS E REVOKES)
-- -----------------------------------------------------------------------------
-- Revogar explicitamente qualquer acesso do papel anon nas novas estruturas
REVOKE ALL ON TABLE public.bank_accounts FROM anon;
REVOKE ALL ON TABLE public.vaults FROM anon;
REVOKE ALL ON TABLE public.vault_ledger FROM anon;
REVOKE ALL ON TABLE public.expense_settlements FROM anon;
REVOKE ALL ON TABLE public.fixed_costs FROM anon;
REVOKE ALL ON TABLE public.fixed_cost_versions FROM anon;
REVOKE ALL ON TABLE public.fixed_cost_payments FROM anon;
REVOKE ALL ON TABLE public.card_invoice_payments FROM anon;
REVOKE ALL ON TABLE public.cost_center_settings FROM anon;
REVOKE ALL ON TABLE public.reserve_allocations FROM anon;
REVOKE ALL ON TABLE public.reserve_allocation_days FROM anon;
REVOKE ALL ON TABLE public.card_account_links FROM anon;
REVOKE ALL ON TABLE public.vault_balances FROM anon;
REVOKE ALL ON FUNCTION public.get_vault_balances() FROM anon;

-- Conceder permissões para o papel authenticated nas tabelas mutáveis
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.bank_accounts TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.vaults TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.expense_settlements TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.fixed_costs TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.fixed_cost_versions TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.fixed_cost_payments TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.card_invoice_payments TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.cost_center_settings TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.reserve_allocations TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.reserve_allocation_days TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.card_account_links TO authenticated;

-- Razão imutável (vault_ledger): SOMENTE SELECT E INSERT
GRANT SELECT, INSERT ON TABLE public.vault_ledger TO authenticated;

-- View e função de conciliação
GRANT SELECT ON TABLE public.vault_balances TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_vault_balances() TO authenticated;

COMMIT;
