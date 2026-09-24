-- ============================================================================
-- MIGRATION V2: GESTÃO INTEGRADA DE FATURAS, CICLOS DE REPASSE E AJUSTES FINANCEIROS
-- PROJETO: Central do Motorista / Logística Prime / Pocket
-- BANCO DE DADOS: PostgreSQL (Neon.tech)
-- DIRETRIZ MANDATÓRIA: Afeta registros futuros e existentes/históricos.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. EXTENSÕES DO POSTGRESQL
-- ----------------------------------------------------------------------------
-- btree_gist é obrigatória para suportar operadores de igualdade (=) com tipos escalares 
-- (ex: UUID, TEXT) juntos com tipos de intervalo (DATERANGE) em restrições de exclusão (EXCLUDE).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ----------------------------------------------------------------------------
-- 2. EVOLUÇÃO DE DDL: PLATFORMS
-- ----------------------------------------------------------------------------
ALTER TABLE platforms ADD COLUMN IF NOT EXISTS rules JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE platforms ADD COLUMN IF NOT EXISTS segment VARCHAR(50) DEFAULT 'logistica';
ALTER TABLE platforms ADD COLUMN IF NOT EXISTS payment_model VARCHAR(50) DEFAULT 'producao';

-- Validar integridade dos ciclos de plataforma
ALTER TABLE platforms DROP CONSTRAINT IF EXISTS check_platform_cycle;
ALTER TABLE platforms ADD CONSTRAINT check_platform_cycle 
    CHECK (LOWER(cycle) IN ('semanal', 'quinzenal', 'mensal', 'misto', 'variavel', 'diario'));

ALTER TABLE platforms DROP CONSTRAINT IF EXISTS check_platform_payment_model;
ALTER TABLE platforms ADD CONSTRAINT check_platform_payment_model 
    CHECK (LOWER(payment_model) IN ('producao', 'diaria', 'fixo', 'misto'));

-- ----------------------------------------------------------------------------
-- 3. EVOLUÇÃO DE DDL: FINANCIAL_ADJUSTMENTS
-- ----------------------------------------------------------------------------
ALTER TABLE financial_adjustments ADD COLUMN IF NOT EXISTS subtype VARCHAR(50);
ALTER TABLE financial_adjustments ADD COLUMN IF NOT EXISTS notes TEXT;

-- Saneamento de subtipos existentes
UPDATE financial_adjustments
SET subtype = CASE 
    WHEN LOWER(type) IN ('bonus', 'credito', 'acrescimo') THEN 'bonificacao'
    WHEN LOWER(type) IN ('desconto', 'debito') THEN 'desconto_geral'
    ELSE 'ajuste_avulso'
END
WHERE subtype IS NULL;

-- Constraints em financial_adjustments
ALTER TABLE financial_adjustments DROP CONSTRAINT IF EXISTS check_financial_adjustments_type;
ALTER TABLE financial_adjustments ADD CONSTRAINT check_financial_adjustments_type 
    CHECK (LOWER(type) IN ('credito', 'debito', 'bonus', 'desconto', 'acrescimo'));

ALTER TABLE financial_adjustments DROP CONSTRAINT IF EXISTS check_financial_adjustments_amount_positive;
ALTER TABLE financial_adjustments ADD CONSTRAINT check_financial_adjustments_amount_positive 
    CHECK (amount >= 0.00);

-- ----------------------------------------------------------------------------
-- 4. EVOLUÇÃO DE DDL: BILLING_CYCLES
-- ----------------------------------------------------------------------------
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS include_end_date BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS payment_received_date DATE;

-- Colunas de agregação com precisão monetária estrita NUMERIC(12,2)
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS gross_routes_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS total_tips_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS total_bonus_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS gross_daily_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS total_adjustments_credit NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS total_adjustments_debit NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS net_total_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS routes_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS packages_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS daily_totals_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE billing_cycles ADD COLUMN IF NOT EXISTS last_recalculated_at TIMESTAMPTZ DEFAULT clock_timestamp();

-- Saneamento preventivo: datas invertidas no histórico
UPDATE billing_cycles 
SET period_end = period_start 
WHERE period_end < period_start;

-- ----------------------------------------------------------------------------
-- 5. SANEAMENTO E CONVERSÃO DE STATUS DOS CICLOS EXISTENTES
-- ----------------------------------------------------------------------------
-- Regra de negócio mandatória:
-- 'open', 'pending' -> 'em_aberto' (se CURRENT_DATE <= period_end) ou 'a_vencer' (se CURRENT_DATE > period_end);
-- 'paid' -> 'pago'.
UPDATE billing_cycles
SET status = CASE 
    WHEN LOWER(status) IN ('paid', 'pago') THEN 'pago'
    WHEN CURRENT_DATE <= period_end THEN 'em_aberto'
    ELSE 'a_vencer'
END;

-- Para ciclos pagos sem data de recebimento registrada, inicializar com a data de pagamento prevista
UPDATE billing_cycles
SET payment_received_date = COALESCE(payment_received_date, expected_payment_date)
WHERE status = 'pago' AND payment_received_date IS NULL;

-- Constraint de status em billing_cycles
ALTER TABLE billing_cycles DROP CONSTRAINT IF EXISTS check_billing_cycle_status;
ALTER TABLE billing_cycles ADD CONSTRAINT check_billing_cycle_status 
    CHECK (status IN ('em_aberto', 'a_vencer', 'pago', 'cancelado', 'atrasado'));

ALTER TABLE billing_cycles ALTER COLUMN status SET DEFAULT 'em_aberto';

-- ----------------------------------------------------------------------------
-- 6. DEDUPLICAÇÃO E SANEAMENTO DE FRONTEIRAS TEMPORAIS ANTES DO GIST
-- ----------------------------------------------------------------------------
-- Se houver múltiplos ciclos duplicados para a mesma plataforma e período no histórico,
-- unificamos as referências em routes, daily_totals e financial_adjustments e removemos os duplicados.
DO $$
DECLARE
    r_dup RECORD;
    v_survivor_id UUID;
    v_dup_ids UUID[];
BEGIN
    FOR r_dup IN (
        SELECT user_id, platform_id, period_start, period_end, 
               array_agg(id ORDER BY created_at ASC NULLS LAST, id ASC) AS id_list
        FROM billing_cycles
        GROUP BY user_id, platform_id, period_start, period_end
        HAVING COUNT(*) > 1
    ) LOOP
        v_survivor_id := r_dup.id_list[1];
        v_dup_ids := r_dup.id_list[2:];

        UPDATE routes 
        SET billing_cycle_id = v_survivor_id 
        WHERE billing_cycle_id = ANY(v_dup_ids);

        UPDATE daily_totals 
        SET billing_cycle_id = v_survivor_id 
        WHERE billing_cycle_id = ANY(v_dup_ids);

        UPDATE financial_adjustments 
        SET billing_cycle_id = v_survivor_id 
        WHERE billing_cycle_id = ANY(v_dup_ids);

        DELETE FROM billing_cycles 
        WHERE id = ANY(v_dup_ids);

        RAISE NOTICE 'Saneamento GiST: Unificados % ciclos duplicados para ID principal %', 
            array_length(v_dup_ids, 1), v_survivor_id;
    END LOOP;
END $$;

-- ----------------------------------------------------------------------------
-- 7. COLUNA GERADA DATE_RANGE E CONSTRAINT GIST ANTI-SOBREPOSIÇÃO
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'billing_cycles' AND column_name = 'date_range'
    ) THEN
        ALTER TABLE billing_cycles 
            ADD COLUMN date_range daterange GENERATED ALWAYS AS (
                daterange(period_start, period_end, CASE WHEN include_end_date THEN '[]' ELSE '[)' END)
            ) STORED;
    END IF;
END $$;

-- Aplicar a constraint de exclusão anti-sobreposição
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'billing_cycles_no_overlap'
    ) THEN
        ALTER TABLE billing_cycles 
            ADD CONSTRAINT billing_cycles_no_overlap 
            EXCLUDE USING gist (
                user_id WITH =, 
                platform_id WITH =, 
                date_range WITH &&
            );
    END IF;
EXCEPTION
    WHEN others THEN
        RAISE NOTICE 'Aviso na adicao da constraint GiST: %', SQLERRM;
END $$;

-- ----------------------------------------------------------------------------
-- 8. ÍNDICES DE ALTA PERFORMANCE
-- ----------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_billing_cycles_user_plat ON billing_cycles(user_id, platform_id);
CREATE INDEX IF NOT EXISTS idx_billing_cycles_status ON billing_cycles(user_id, status);
CREATE INDEX IF NOT EXISTS idx_billing_cycles_periods ON billing_cycles(period_start, period_end);
CREATE INDEX IF NOT EXISTS idx_billing_cycles_gist ON billing_cycles USING gist(user_id, platform_id, date_range);

CREATE INDEX IF NOT EXISTS idx_routes_billing_cycle_id ON routes(billing_cycle_id);
CREATE INDEX IF NOT EXISTS idx_routes_user_plat_occurred ON routes(user_id, platform_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_daily_totals_billing_cycle_id ON daily_totals(billing_cycle_id);
CREATE INDEX IF NOT EXISTS idx_daily_totals_user_plat_occurred ON daily_totals(user_id, platform_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_financial_adj_cycle_id ON financial_adjustments(billing_cycle_id);
CREATE INDEX IF NOT EXISTS idx_financial_adj_user_plat ON financial_adjustments(user_id, platform_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_financial_adj_type_subtype ON financial_adjustments(type, subtype);

-- ----------------------------------------------------------------------------
-- 9. FUNÇÕES DE NEGÓCIO
-- ----------------------------------------------------------------------------

-- Função 1: fn_calc_platform_cycle_range
-- Calcula as datas de início, término e previsão de pagamento com base nas regras da plataforma
CREATE OR REPLACE FUNCTION fn_calc_platform_cycle_range(
    p_platform_id UUID,
    p_ref_date DATE
)
RETURNS TABLE (
    period_start DATE,
    period_end DATE,
    expected_payment_date DATE
)
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_cycle TEXT;
    v_rules JSONB;
    v_fixed_pay_delay INT;
    v_day_of_month INT;
    v_month_start DATE;
    v_month_end DATE;
    v_start DATE;
    v_end DATE;
    v_pay_delay INT;
    v_entries JSONB;
    v_entry RECORD;
    v_cut_dates DATE[];
    v_delays INT[];
    v_m_offset INT;
    v_target_month_start DATE;
    v_days_in_m INT;
    v_cut_day INT;
    v_i INT;
    v_num_cuts INT;
BEGIN
    SELECT 
        LOWER(TRIM(COALESCE(p.cycle, 'semanal'))),
        COALESCE(p.rules, '{}'::jsonb)
    INTO v_cycle, v_rules
    FROM platforms p
    WHERE p.id = p_platform_id;

    IF NOT FOUND THEN
        v_cycle := 'semanal';
        v_rules := '{}'::jsonb;
    END IF;

    -- Obter atraso de pagamento (pay_delay), padrão de 7 dias se não informado
    v_fixed_pay_delay := GREATEST(1, COALESCE((v_rules->>'fixed_pay_delay')::INT, 7));

    IF v_cycle = 'semanal' THEN
        -- Segunda-feira como início da semana (ISO-8601 padrão no PostgreSQL com date_trunc('week'))
        v_start := (date_trunc('week', p_ref_date))::DATE;
        v_end := v_start + 6; -- Domingo
        v_pay_delay := v_fixed_pay_delay;

    ELSIF v_cycle = 'quinzenal' THEN
        v_month_start := (date_trunc('month', p_ref_date))::DATE;
        v_month_end := (date_trunc('month', p_ref_date) + INTERVAL '1 month - 1 day')::DATE;
        v_day_of_month := EXTRACT(DAY FROM p_ref_date)::INT;

        IF v_day_of_month <= 15 THEN
            v_start := v_month_start;
            v_end := v_month_start + 14; -- 15º dia
        ELSE
            v_start := v_month_start + 15; -- 16º dia
            v_end := v_month_end;
        END IF;
        v_pay_delay := v_fixed_pay_delay;

    ELSIF v_cycle = 'mensal' THEN
        v_start := (date_trunc('month', p_ref_date))::DATE;
        v_end := (date_trunc('month', p_ref_date) + INTERVAL '1 month - 1 day')::DATE;
        v_pay_delay := v_fixed_pay_delay;

    ELSIF v_cycle = 'diario' THEN
        v_start := p_ref_date;
        v_end := p_ref_date;
        v_pay_delay := v_fixed_pay_delay;

    ELSIF v_cycle IN ('misto', 'variavel') THEN
        -- Suporte a múltiplos cortes mensais configurados em cycle_entries ou cycle_days
        v_entries := v_rules->'cycle_entries';
        IF v_entries IS NULL OR jsonb_array_length(v_entries) = 0 THEN
            IF v_rules->'cycle_days' IS NOT NULL AND jsonb_array_length(v_rules->'cycle_days') > 0 THEN
                SELECT jsonb_agg(jsonb_build_object('cut', value::int, 'payDelay', v_fixed_pay_delay))
                INTO v_entries
                FROM jsonb_array_elements_text(v_rules->'cycle_days');
            ELSE
                v_entries := '[{"cut": 1, "payDelay": 7}, {"cut": 16, "payDelay": 7}]'::jsonb;
            END IF;
        END IF;

        -- Construir pontos de corte considerando mês anterior, atual e seguinte
        v_cut_dates := ARRAY[]::DATE[];
        v_delays := ARRAY[]::INT[];

        FOR v_m_offset IN -1..1 LOOP
            v_target_month_start := (date_trunc('month', p_ref_date) + (v_m_offset || ' months')::INTERVAL)::DATE;
            v_days_in_m := EXTRACT(DAY FROM (v_target_month_start + INTERVAL '1 month - 1 day')::DATE)::INT;

            FOR v_entry IN
                SELECT 
                    COALESCE((elem->>'cut')::INT, 1) AS cut,
                    GREATEST(1, COALESCE((elem->>'payDelay')::INT, (elem->>'pay_delay')::INT, v_fixed_pay_delay)) AS delay
                FROM jsonb_array_elements(v_entries) AS elem
                ORDER BY (elem->>'cut')::INT ASC
            LOOP
                v_cut_day := LEAST(GREATEST(v_entry.cut, 1), v_days_in_m);
                v_cut_dates := array_append(v_cut_dates, v_target_month_start + (v_cut_day - 1));
                v_delays := array_append(v_delays, v_entry.delay);
            END LOOP;
        END LOOP;

        v_num_cuts := array_length(v_cut_dates, 1);
        v_start := NULL;
        v_end := NULL;
        v_pay_delay := v_fixed_pay_delay;

        IF v_num_cuts >= 2 THEN
            FOR v_i IN 1..(v_num_cuts - 1) LOOP
                IF p_ref_date >= v_cut_dates[v_i] AND p_ref_date < v_cut_dates[v_i + 1] THEN
                    v_start := v_cut_dates[v_i];
                    v_end := v_cut_dates[v_i + 1] - 1;
                    v_pay_delay := v_delays[v_i];
                    EXIT;
                END IF;
            END LOOP;
        END IF;

        IF v_start IS NULL THEN
            -- Fallback quinzenal
            v_month_start := (date_trunc('month', p_ref_date))::DATE;
            v_month_end := (date_trunc('month', p_ref_date) + INTERVAL '1 month - 1 day')::DATE;
            IF EXTRACT(DAY FROM p_ref_date)::INT <= 15 THEN
                v_start := v_month_start;
                v_end := v_month_start + 14;
            ELSE
                v_start := v_month_start + 15;
                v_end := v_month_end;
            END IF;
            v_pay_delay := v_fixed_pay_delay;
        END IF;

    ELSE
        -- Padrão semanal
        v_start := (date_trunc('week', p_ref_date))::DATE;
        v_end := v_start + 6;
        v_pay_delay := v_fixed_pay_delay;
    END IF;

    RETURN QUERY
    SELECT 
        v_start,
        v_end,
        (v_end + v_pay_delay)::DATE;
END;
$$;

-- Sobrecarga para compatibilidade com chamadas passando TEXT
CREATE OR REPLACE FUNCTION fn_calc_platform_cycle_range(p_platform_id TEXT, p_ref_date DATE)
RETURNS TABLE (period_start DATE, period_end DATE, expected_payment_date DATE)
LANGUAGE sql STABLE AS $$
    SELECT * FROM fn_calc_platform_cycle_range(p_platform_id::UUID, p_ref_date);
$$;

-- Função 2: fn_refresh_billing_cycles_status
-- Atualiza dinamicamente o status dos ciclos com base no CURRENT_DATE
CREATE OR REPLACE FUNCTION fn_refresh_billing_cycles_status(p_user_id UUID DEFAULT NULL)
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_count INT;
BEGIN
    UPDATE billing_cycles
    SET status = CASE
        WHEN LOWER(status) IN ('pago', 'paid') THEN 'pago'
        WHEN CURRENT_DATE <= period_end THEN 'em_aberto'
        ELSE 'a_vencer'
    END
    WHERE LOWER(status) NOT IN ('pago', 'paid', 'cancelado')
      AND (p_user_id IS NULL OR user_id = p_user_id);

    GET DIAGNOSTICS v_count = ROW_COUNT;
    RETURN v_count;
END;
$$;

CREATE OR REPLACE FUNCTION fn_refresh_billing_cycles_status(p_user_id TEXT)
RETURNS INT LANGUAGE sql AS $$
    SELECT fn_refresh_billing_cycles_status(p_user_id::UUID);
$$;

-- Função 3: fn_recalculate_cycle_totals
-- Recalcula de forma atômica e precisa (NUMERIC 12,2) os totais financeiros de um ou todos os ciclos
CREATE OR REPLACE FUNCTION fn_recalculate_cycle_totals(p_cycle_id UUID DEFAULT NULL)
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    WITH agg_routes AS (
        SELECT 
            r.billing_cycle_id,
            COUNT(*)::INT AS routes_count,
            COALESCE(SUM(r.package_count), 0)::INT AS packages_count,
            COALESCE(SUM(r.amount), 0.00)::NUMERIC(12,2) AS gross_routes,
            COALESCE(SUM(r.tip), 0.00)::NUMERIC(12,2) AS tips,
            COALESCE(SUM(COALESCE(r.bonus, 0.00)), 0.00)::NUMERIC(12,2) AS bonus
        FROM routes r
        WHERE r.billing_cycle_id IS NOT NULL
          AND (p_cycle_id IS NULL OR r.billing_cycle_id = p_cycle_id)
        GROUP BY r.billing_cycle_id
    ),
    agg_daily AS (
        SELECT 
            dt.billing_cycle_id,
            COUNT(*)::INT AS daily_count,
            COALESCE(SUM(dt.amount), 0.00)::NUMERIC(12,2) AS gross_daily
        FROM daily_totals dt
        WHERE dt.billing_cycle_id IS NOT NULL
          AND (p_cycle_id IS NULL OR dt.billing_cycle_id = p_cycle_id)
        GROUP BY dt.billing_cycle_id
    ),
    agg_adj AS (
        SELECT 
            fa.billing_cycle_id,
            COALESCE(SUM(CASE WHEN LOWER(fa.type) IN ('credito', 'bonus', 'acrescimo') THEN fa.amount ELSE 0.00 END), 0.00)::NUMERIC(12,2) AS credits,
            COALESCE(SUM(CASE WHEN LOWER(fa.type) IN ('debito', 'desconto', 'avaria', 'extravio') THEN fa.amount ELSE 0.00 END), 0.00)::NUMERIC(12,2) AS debits
        FROM financial_adjustments fa
        WHERE fa.billing_cycle_id IS NOT NULL
          AND (p_cycle_id IS NULL OR fa.billing_cycle_id = p_cycle_id)
        GROUP BY fa.billing_cycle_id
    )
    UPDATE billing_cycles bc
    SET 
        routes_count = COALESCE(ar.routes_count, 0),
        packages_count = COALESCE(ar.packages_count, 0),
        gross_routes_amount = COALESCE(ar.gross_routes, 0.00),
        total_tips_amount = COALESCE(ar.tips, 0.00),
        total_bonus_amount = COALESCE(ar.bonus, 0.00),
        daily_totals_count = COALESCE(ad.daily_count, 0),
        gross_daily_amount = COALESCE(ad.gross_daily, 0.00),
        total_adjustments_credit = COALESCE(aa.credits, 0.00),
        total_adjustments_debit = COALESCE(aa.debits, 0.00),
        net_total_amount = (
            COALESCE(ar.gross_routes, 0.00) 
            + COALESCE(ar.tips, 0.00) 
            + COALESCE(ar.bonus, 0.00) 
            + COALESCE(ad.gross_daily, 0.00) 
            + COALESCE(aa.credits, 0.00) 
            - COALESCE(aa.debits, 0.00)
        ),
        last_recalculated_at = clock_timestamp()
    FROM (
        SELECT id FROM billing_cycles
        WHERE (p_cycle_id IS NULL OR id = p_cycle_id)
    ) targets
    LEFT JOIN agg_routes ar ON ar.billing_cycle_id = targets.id
    LEFT JOIN agg_daily ad ON ad.billing_cycle_id = targets.id
    LEFT JOIN agg_adj aa ON aa.billing_cycle_id = targets.id
    WHERE bc.id = targets.id;
END;
$$;

CREATE OR REPLACE FUNCTION fn_recalculate_cycle_totals(p_cycle_id TEXT)
RETURNS VOID LANGUAGE sql AS $$
    SELECT fn_recalculate_cycle_totals(p_cycle_id::UUID);
$$;

-- Função Trigger 4: fn_auto_assign_billing_cycle
-- Associa automaticamente novo lançamento (rota, diária ou ajuste) ao ciclo correto ou cria se inexistente
CREATE OR REPLACE FUNCTION fn_auto_assign_billing_cycle()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_ref_date DATE;
    v_cycle_id UUID;
    v_start DATE;
    v_end DATE;
    v_pay DATE;
    v_status VARCHAR(20);
BEGIN
    IF NEW.billing_cycle_id IS NOT NULL OR NEW.platform_id IS NULL THEN
        RETURN NEW;
    END IF;

    IF TG_TABLE_NAME = 'financial_adjustments' THEN
        v_ref_date := NEW.occurred_at;
    ELSE
        v_ref_date := NEW.occurred_at::DATE;
    END IF;

    IF v_ref_date IS NULL THEN
        RETURN NEW;
    END IF;

    -- Localizar ciclo compatível
    SELECT id INTO v_cycle_id
    FROM billing_cycles
    WHERE user_id = NEW.user_id
      AND platform_id = NEW.platform_id
      AND (
          (include_end_date = true AND v_ref_date BETWEEN period_start AND period_end)
          OR (include_end_date = false AND v_ref_date >= period_start AND v_ref_date < period_end)
      )
    ORDER BY period_start DESC
    LIMIT 1;

    -- Se não existir ciclo, calcular e criar automaticamente
    IF v_cycle_id IS NULL THEN
        SELECT period_start, period_end, expected_payment_date
        INTO v_start, v_end, v_pay
        FROM fn_calc_platform_cycle_range(NEW.platform_id, v_ref_date);

        IF v_start IS NOT NULL AND v_end IS NOT NULL THEN
            v_status := CASE 
                WHEN CURRENT_DATE <= v_end THEN 'em_aberto'
                ELSE 'a_vencer'
            END;

            INSERT INTO billing_cycles (
                user_id,
                platform_id,
                period_start,
                period_end,
                expected_payment_date,
                status,
                include_end_date
            ) VALUES (
                NEW.user_id,
                NEW.platform_id,
                v_start,
                v_end,
                v_pay,
                v_status,
                true
            )
            ON CONFLICT DO NOTHING
            RETURNING id INTO v_cycle_id;

            IF v_cycle_id IS NULL THEN
                SELECT id INTO v_cycle_id
                FROM billing_cycles
                WHERE user_id = NEW.user_id
                  AND platform_id = NEW.platform_id
                  AND (
                      (include_end_date = true AND v_ref_date BETWEEN period_start AND period_end)
                      OR (include_end_date = false AND v_ref_date >= period_start AND v_ref_date < period_end)
                  )
                LIMIT 1;
            END IF;
        END IF;
    END IF;

    NEW.billing_cycle_id := v_cycle_id;
    RETURN NEW;
END;
$$;

-- Função Trigger 5: trg_recalc_cycle_on_item_change
-- Dispara o recálculo dos totais do ciclo afetado sempre que um registro for inserido, alterado ou excluído
CREATE OR REPLACE FUNCTION trg_recalc_cycle_on_item_change()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.billing_cycle_id IS NOT NULL THEN
            PERFORM fn_recalculate_cycle_totals(NEW.billing_cycle_id);
        END IF;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.billing_cycle_id IS NOT NULL AND OLD.billing_cycle_id IS DISTINCT FROM NEW.billing_cycle_id THEN
            PERFORM fn_recalculate_cycle_totals(OLD.billing_cycle_id);
        END IF;
        IF NEW.billing_cycle_id IS NOT NULL THEN
            PERFORM fn_recalculate_cycle_totals(NEW.billing_cycle_id);
        END IF;
    ELSIF TG_OP = 'DELETE' THEN
        IF OLD.billing_cycle_id IS NOT NULL THEN
            PERFORM fn_recalculate_cycle_totals(OLD.billing_cycle_id);
        END IF;
    END IF;
    RETURN NULL;
END;
$$;

-- ----------------------------------------------------------------------------
-- 10. CRUCIAL: BACKFILL E SANEAMENTO DO PASSADO (REGISTROS HISTÓRICOS)
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    r_item RECORD;
    v_start DATE;
    v_end DATE;
    v_pay DATE;
    v_cycle_id UUID;
    v_created_cycles INT := 0;
    v_linked_routes INT := 0;
    v_linked_daily INT := 0;
    v_linked_adj INT := 0;
BEGIN
    RAISE NOTICE '==> INICIANDO BACKFILL DE REGISTROS HISTÓRICOS...';

    -- Passo A: Identificar todas as ocorrências históricas sem ciclo e garantir que o ciclo exista
    FOR r_item IN (
        SELECT DISTINCT user_id, platform_id, occurred_date
        FROM (
            SELECT user_id, platform_id, occurred_at::DATE AS occurred_date
            FROM routes
            WHERE billing_cycle_id IS NULL AND platform_id IS NOT NULL
            UNION
            SELECT user_id, platform_id, occurred_at::DATE AS occurred_date
            FROM daily_totals
            WHERE billing_cycle_id IS NULL AND platform_id IS NOT NULL
            UNION
            SELECT user_id, platform_id, occurred_at AS occurred_date
            FROM financial_adjustments
            WHERE billing_cycle_id IS NULL AND platform_id IS NOT NULL
        ) u
        ORDER BY user_id, platform_id, occurred_date ASC
    ) LOOP
        -- Checar se já existe ciclo contemplando a data
        SELECT id INTO v_cycle_id
        FROM billing_cycles
        WHERE user_id = r_item.user_id
          AND platform_id = r_item.platform_id
          AND (
              (include_end_date = true AND r_item.occurred_date BETWEEN period_start AND period_end)
              OR (include_end_date = false AND r_item.occurred_date >= period_start AND r_item.occurred_date < period_end)
          )
        ORDER BY period_start DESC
        LIMIT 1;

        -- Se não existe, criar ciclo retroativo de acordo com a regra da plataforma
        IF v_cycle_id IS NULL THEN
            SELECT period_start, period_end, expected_payment_date
            INTO v_start, v_end, v_pay
            FROM fn_calc_platform_cycle_range(r_item.platform_id, r_item.occurred_date);

            IF v_start IS NOT NULL AND v_end IS NOT NULL THEN
                INSERT INTO billing_cycles (
                    user_id,
                    platform_id,
                    period_start,
                    period_end,
                    expected_payment_date,
                    status,
                    include_end_date
                ) VALUES (
                    r_item.user_id,
                    r_item.platform_id,
                    v_start,
                    v_end,
                    v_pay,
                    CASE WHEN CURRENT_DATE <= v_end THEN 'em_aberto' ELSE 'a_vencer' END,
                    true
                )
                ON CONFLICT DO NOTHING
                RETURNING id INTO v_cycle_id;

                IF v_cycle_id IS NOT NULL THEN
                    v_created_cycles := v_created_cycles + 1;
                END IF;
            END IF;
        END IF;
    END LOOP;

    -- Passo B: Associar rotas históricas órfãs aos respectivos ciclos
    WITH updated_r AS (
        UPDATE routes r
        SET billing_cycle_id = bc.id
        FROM billing_cycles bc
        WHERE r.billing_cycle_id IS NULL
          AND r.platform_id IS NOT NULL
          AND r.user_id = bc.user_id
          AND r.platform_id = bc.platform_id
          AND (
              (bc.include_end_date = true AND r.occurred_at::DATE BETWEEN bc.period_start AND bc.period_end)
              OR (bc.include_end_date = false AND r.occurred_at::DATE >= bc.period_start AND r.occurred_at::DATE < bc.period_end)
          )
        RETURNING r.id
    )
    SELECT COUNT(*) INTO v_linked_routes FROM updated_r;

    -- Passo C: Associar diárias históricas órfãs aos respectivos ciclos
    WITH updated_dt AS (
        UPDATE daily_totals dt
        SET billing_cycle_id = bc.id
        FROM billing_cycles bc
        WHERE dt.billing_cycle_id IS NULL
          AND dt.platform_id IS NOT NULL
          AND dt.user_id = bc.user_id
          AND dt.platform_id = bc.platform_id
          AND (
              (bc.include_end_date = true AND dt.occurred_at::DATE BETWEEN bc.period_start AND bc.period_end)
              OR (bc.include_end_date = false AND dt.occurred_at::DATE >= bc.period_start AND dt.occurred_at::DATE < bc.period_end)
          )
        RETURNING dt.id
    )
    SELECT COUNT(*) INTO v_linked_daily FROM updated_dt;

    -- Passo D: Associar ajustes financeiros históricos órfãos aos respectivos ciclos
    WITH updated_fa AS (
        UPDATE financial_adjustments fa
        SET billing_cycle_id = bc.id
        FROM billing_cycles bc
        WHERE fa.billing_cycle_id IS NULL
          AND fa.platform_id IS NOT NULL
          AND fa.user_id = bc.user_id
          AND fa.platform_id = bc.platform_id
          AND (
              (bc.include_end_date = true AND fa.occurred_at BETWEEN bc.period_start AND bc.period_end)
              OR (bc.include_end_date = false AND fa.occurred_at >= bc.period_start AND fa.occurred_at < bc.period_end)
          )
        RETURNING fa.id
    )
    SELECT COUNT(*) INTO v_linked_adj FROM updated_fa;

    RAISE NOTICE '==> BACKFILL CONCLUÍDO COM SUCESSO:';
    RAISE NOTICE '    - Ciclos criados retroativamente: %', v_created_cycles;
    RAISE NOTICE '    - Rotas vinculadas a ciclos: %', v_linked_routes;
    RAISE NOTICE '    - Diárias vinculadas a ciclos: %', v_linked_daily;
    RAISE NOTICE '    - Ajustes financeiros vinculados: %', v_linked_adj;
END $$;

-- ----------------------------------------------------------------------------
-- 11. RECÁLCULO DOS TOTAIS AGREGADOS DE TODAS AS FATURAS HISTÓRICAS
-- ----------------------------------------------------------------------------
SELECT fn_recalculate_cycle_totals(NULL);

-- ----------------------------------------------------------------------------
-- 12. ATIVAÇÃO DOS TRIGGERS EM PRODUÇÃO (PARA REGISTROS FUTUROS)
-- ----------------------------------------------------------------------------

-- Triggers em routes
DROP TRIGGER IF EXISTS trg_routes_auto_cycle ON routes;
CREATE TRIGGER trg_routes_auto_cycle
    BEFORE INSERT OR UPDATE OF platform_id, occurred_at ON routes
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_assign_billing_cycle();

DROP TRIGGER IF EXISTS trg_routes_recalc_cycle ON routes;
CREATE TRIGGER trg_routes_recalc_cycle
    AFTER INSERT OR UPDATE OR DELETE ON routes
    FOR EACH ROW
    EXECUTE FUNCTION trg_recalc_cycle_on_item_change();

-- Triggers em daily_totals
DROP TRIGGER IF EXISTS trg_daily_totals_auto_cycle ON daily_totals;
CREATE TRIGGER trg_daily_totals_auto_cycle
    BEFORE INSERT OR UPDATE OF platform_id, occurred_at ON daily_totals
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_assign_billing_cycle();

DROP TRIGGER IF EXISTS trg_daily_totals_recalc_cycle ON daily_totals;
CREATE TRIGGER trg_daily_totals_recalc_cycle
    AFTER INSERT OR UPDATE OR DELETE ON daily_totals
    FOR EACH ROW
    EXECUTE FUNCTION trg_recalc_cycle_on_item_change();

-- Triggers em financial_adjustments
DROP TRIGGER IF EXISTS trg_financial_adj_auto_cycle ON financial_adjustments;
CREATE TRIGGER trg_financial_adj_auto_cycle
    BEFORE INSERT OR UPDATE OF platform_id, occurred_at ON financial_adjustments
    FOR EACH ROW
    EXECUTE FUNCTION fn_auto_assign_billing_cycle();

DROP TRIGGER IF EXISTS trg_financial_adj_recalc_cycle ON financial_adjustments;
CREATE TRIGGER trg_financial_adj_recalc_cycle
    AFTER INSERT OR UPDATE OR DELETE ON financial_adjustments
    FOR EACH ROW
    EXECUTE FUNCTION trg_recalc_cycle_on_item_change();

-- ----------------------------------------------------------------------------
-- 13. VIEWS DE ALTO DESEMPENHO E RELATÓRIOS
-- ----------------------------------------------------------------------------

-- View 1: Resumo completo de faturas/ciclos de repasse
CREATE OR REPLACE VIEW v_billing_cycles_summary AS
SELECT 
    bc.id AS billing_cycle_id,
    bc.user_id,
    bc.platform_id,
    p.name AS platform_name,
    p.cycle AS platform_cycle,
    p.payment_model,
    bc.period_start,
    bc.period_end,
    bc.expected_payment_date,
    bc.payment_received_date,
    bc.status,
    bc.include_end_date,
    bc.routes_count,
    bc.packages_count,
    bc.gross_routes_amount,
    bc.total_tips_amount,
    bc.total_bonus_amount,
    bc.daily_totals_count,
    bc.gross_daily_amount,
    bc.total_adjustments_credit,
    bc.total_adjustments_debit,
    (bc.total_adjustments_credit - bc.total_adjustments_debit)::NUMERIC(12,2) AS net_adjustments,
    (bc.gross_routes_amount + bc.total_tips_amount + bc.total_bonus_amount + bc.gross_daily_amount)::NUMERIC(12,2) AS gross_earnings,
    bc.net_total_amount AS net_total,
    bc.created_at,
    bc.last_recalculated_at
FROM billing_cycles bc
JOIN platforms p ON p.id = bc.platform_id;

-- View 2: Detalhamento analítico de bonificações e descontos
CREATE OR REPLACE VIEW v_billing_cycle_adjustments_breakdown AS
SELECT 
    fa.billing_cycle_id,
    fa.user_id,
    fa.platform_id,
    p.name AS platform_name,
    LOWER(fa.type) AS adjustment_type,
    COALESCE(fa.subtype, 'geral') AS adjustment_subtype,
    COUNT(*)::INT AS items_count,
    SUM(fa.amount)::NUMERIC(12,2) AS total_amount,
    MIN(fa.occurred_at) AS first_occurred_at,
    MAX(fa.occurred_at) AS last_occurred_at
FROM financial_adjustments fa
JOIN platforms p ON p.id = fa.platform_id
WHERE fa.billing_cycle_id IS NOT NULL
GROUP BY 
    fa.billing_cycle_id, 
    fa.user_id, 
    fa.platform_id, 
    p.name, 
    LOWER(fa.type), 
    COALESCE(fa.subtype, 'geral');

-- ----------------------------------------------------------------------------
-- 14. PERMISSÕES E ROLES (PostgREST / Supabase / Neon)
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    -- Conceder permissões para os roles da API REST se existirem
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON billing_cycles TO authenticated;
        GRANT SELECT, INSERT, UPDATE, DELETE ON financial_adjustments TO authenticated;
        GRANT SELECT, INSERT, UPDATE, DELETE ON routes TO authenticated;
        GRANT SELECT, INSERT, UPDATE, DELETE ON daily_totals TO authenticated;
        GRANT SELECT ON v_billing_cycles_summary TO authenticated;
        GRANT SELECT ON v_billing_cycle_adjustments_breakdown TO authenticated;
        GRANT EXECUTE ON FUNCTION fn_calc_platform_cycle_range(UUID, DATE) TO authenticated;
        GRANT EXECUTE ON FUNCTION fn_calc_platform_cycle_range(TEXT, DATE) TO authenticated;
        GRANT EXECUTE ON FUNCTION fn_refresh_billing_cycles_status(UUID) TO authenticated;
        GRANT EXECUTE ON FUNCTION fn_recalculate_cycle_totals(UUID) TO authenticated;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
        GRANT SELECT ON v_billing_cycles_summary TO anon;
        GRANT SELECT ON v_billing_cycle_adjustments_breakdown TO anon;
    END IF;

    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
        GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
        GRANT ALL ON ALL FUNCTIONS IN SCHEMA public TO service_role;
    END IF;
END $$;

COMMIT;
