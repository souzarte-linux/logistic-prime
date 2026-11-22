


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE EXTENSION IF NOT EXISTS "pg_stat_statements" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA "extensions";






CREATE EXTENSION IF NOT EXISTS "supabase_vault" WITH SCHEMA "vault";






CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";






CREATE TYPE "public"."expense_category" AS ENUM (
    'combustivel',
    'manutencao',
    'alimentacao',
    'equipe'
);


ALTER TYPE "public"."expense_category" OWNER TO "postgres";


CREATE TYPE "public"."payment_cycle" AS ENUM (
    'semanal',
    'quinzenal',
    'mensal',
    'misto',
    'variavel'
);


ALTER TYPE "public"."payment_cycle" OWNER TO "postgres";


CREATE TYPE "public"."payment_method" AS ENUM (
    'dinheiro',
    'pix',
    'cartao',
    'carteira'
);


ALTER TYPE "public"."payment_method" OWNER TO "postgres";


CREATE TYPE "public"."product_type" AS ENUM (
    'alimento',
    'pacote',
    'documento',
    'outro',
    'farmacia',
    'mercado',
    'logistico'
);


ALTER TYPE "public"."product_type" OWNER TO "postgres";


CREATE TYPE "public"."vehicle_type" AS ENUM (
    'moto',
    'carro',
    'bike',
    'patinete'
);


ALTER TYPE "public"."vehicle_type" OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."debug_auth_context"() RETURNS "jsonb"
    LANGUAGE "sql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
  SELECT jsonb_build_object(
    'current_role', current_setting('role', true),
    'session_user', session_user,
    'auth_user_id', auth.user_id(),
    'jwt_claims', current_setting('request.jwt.claims', true)
  );
$$;


ALTER FUNCTION "public"."debug_auth_context"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_expired_photos_to_clean"("p_batch_size" integer DEFAULT 100) RETURNS TABLE("stop_id" "uuid", "photo_url" "text", "route_id" "uuid", "route_status" "text", "photo_expires_at" timestamp with time zone)
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
BEGIN
    RETURN QUERY
    SELECT 
        mrs.id AS stop_id,
        mrs.photo_url,
        mrs.route_id,
        mdr.status AS route_status,
        mrs.photo_expires_at
    FROM public.master_route_stops mrs
    INNER JOIN public.master_delivery_routes mdr ON mrs.route_id = mdr.id
    WHERE mrs.photo_url IS NOT NULL
      AND mrs.photo_expires_at IS NOT NULL
      AND mrs.photo_expires_at <= timezone('utc', now())
      AND mdr.status <> 'em_andamento'
    ORDER BY mrs.photo_expires_at ASC
    LIMIT p_batch_size;
END;
$$;


ALTER FUNCTION "public"."get_expired_photos_to_clean"("p_batch_size" integer) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_vault_balances"() RETURNS TABLE("vault_id" "uuid", "user_id" "uuid", "account_id" "uuid", "balance" numeric)
    LANGUAGE "sql"
    SET "search_path" TO 'public'
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


ALTER FUNCTION "public"."get_vault_balances"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."handle_master_updated_at"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
BEGIN
    NEW.updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."handle_master_updated_at"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."handle_new_user"() RETURNS "trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
BEGIN
  INSERT INTO public.profiles (id, email, full_name)
  VALUES (
    NEW.id,
    NEW.email,
    COALESCE(NEW.raw_user_meta_data->>'full_name', '')
  );
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."handle_new_user"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."handle_updated_at"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
BEGIN
    NEW.updated_at = timezone('utc', now());
    RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."handle_updated_at"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."mark_photos_as_cleaned"("p_stop_ids" "uuid"[], "p_status" "text" DEFAULT 'sucesso'::"text", "p_details" "jsonb" DEFAULT '{}'::"jsonb") RETURNS integer
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'public'
    AS $$
DECLARe
    v_updated_count INT;
BEGIN
    IF p_stop_ids IS NULL OR array_length(p_stop_ids, 1) = 0 THEN
        RETURN 0;
    END IF;

    UPDATE public.master_route_stops
    SET photo_url = NULL,
        photo_expires_at = NULL,
        updated_at = timezone('utc', now())
    WHERE id = ANY(p_stop_ids);

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;

    INSERT INTO public.storage_cleanup_logs (photos_count, status, details)
    VALUES (
        v_updated_count,
        p_status,
        jsonb_build_object(
            'cleaned_stop_ids', p_stop_ids,
            'extra', p_details
        )
    );

    RETURN v_updated_count;
END;
$$;


ALTER FUNCTION "public"."mark_photos_as_cleaned"("p_stop_ids" "uuid"[], "p_status" "text", "p_details" "jsonb") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."prevent_vault_ledger_mutation"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    SET "search_path" TO 'public'
    AS $$
BEGIN
    -- Única exceção: DELETE em cascata quando o usuário dono já foi removido de auth.users
    IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = OLD.user_id) THEN
        RETURN OLD;
    END IF;
    RAISE EXCEPTION 'vault_ledger é imutável: atualizações e exclusões são proibidas';
END;
$$;


ALTER FUNCTION "public"."prevent_vault_ledger_mutation"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."rls_auto_enable"() RETURNS "event_trigger"
    LANGUAGE "plpgsql" SECURITY DEFINER
    SET "search_path" TO 'pg_catalog'
    AS $$
DECLARE
  cmd record;
BEGIN
  FOR cmd IN
    SELECT *
    FROM pg_event_trigger_ddl_commands()
    WHERE command_tag IN ('CREATE TABLE', 'CREATE TABLE AS', 'SELECT INTO')
      AND object_type IN ('table','partitioned table')
  LOOP
     IF cmd.schema_name IS NOT NULL AND cmd.schema_name IN ('public') AND cmd.schema_name NOT IN ('pg_catalog','information_schema') AND cmd.schema_name NOT LIKE 'pg_toast%' AND cmd.schema_name NOT LIKE 'pg_temp%' THEN
      BEGIN
        EXECUTE format('alter table if exists %s enable row level security', cmd.object_identity);
        RAISE LOG 'rls_auto_enable: enabled RLS on %', cmd.object_identity;
      EXCEPTION
        WHEN OTHERS THEN
          RAISE LOG 'rls_auto_enable: failed to enable RLS on %', cmd.object_identity;
      END;
     ELSE
        RAISE LOG 'rls_auto_enable: skip % (either system schema or not in enforced list: %.)', cmd.object_identity, cmd.schema_name;
     END IF;
  END LOOP;
END;
$$;


ALTER FUNCTION "public"."rls_auto_enable"() OWNER TO "postgres";

SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."bank_accounts" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "institution" "text",
    "agency" "text",
    "account_last_digits" "text",
    "account_type" "text",
    "informed_balance" numeric(12,2),
    "informed_balance_at" timestamp with time zone,
    "active" boolean DEFAULT true NOT NULL,
    "sort_order" integer DEFAULT 0 NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "bank_accounts_account_last_digits_check" CHECK ((("account_last_digits" IS NULL) OR ("length"("account_last_digits") <= 6))),
    CONSTRAINT "bank_accounts_account_type_check" CHECK ((("account_type" IS NULL) OR ("account_type" = ANY (ARRAY['corrente'::"text", 'poupanca'::"text", 'pagamento'::"text", 'outra'::"text"]))))
);


ALTER TABLE "public"."bank_accounts" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."billing_cycles" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "platform_id" "uuid" NOT NULL,
    "period_start" "date" NOT NULL,
    "period_end" "date" NOT NULL,
    "expected_payment_date" "date" NOT NULL,
    "status" "text" DEFAULT 'pending'::"text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "include_end_date" boolean DEFAULT true,
    "payment_received_date" "date"
);


ALTER TABLE "public"."billing_cycles" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."card_account_links" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "card_id" "uuid" NOT NULL,
    "account_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone
);


ALTER TABLE "public"."card_account_links" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."card_brands" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."card_brands" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."card_invoice_payments" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "card_id" "uuid" NOT NULL,
    "reference_month" "date" NOT NULL,
    "amount" numeric(12,2) NOT NULL,
    "paid_at" timestamp with time zone NOT NULL,
    "source_account_id" "uuid",
    "source_vault_id" "uuid",
    "vault_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "account_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "operation_id" "uuid" NOT NULL,
    "notes" "text",
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "card_invoice_payments_account_amount_check" CHECK (("account_amount" >= (0)::numeric)),
    CONSTRAINT "card_invoice_payments_amount_check" CHECK (("amount" > (0)::numeric)),
    CONSTRAINT "card_invoice_payments_reference_month_check" CHECK ((EXTRACT("day" FROM "reference_month") = (1)::numeric)),
    CONSTRAINT "card_invoice_payments_vault_amount_check" CHECK (("vault_amount" >= (0)::numeric)),
    CONSTRAINT "chk_card_invoice_payments_account" CHECK ((("account_amount" = (0)::numeric) OR ("source_account_id" IS NOT NULL))),
    CONSTRAINT "chk_card_invoice_payments_sum" CHECK ((("vault_amount" + "account_amount") = "amount")),
    CONSTRAINT "chk_card_invoice_payments_vault" CHECK ((("vault_amount" = (0)::numeric) OR ("source_vault_id" IS NOT NULL)))
);


ALTER TABLE "public"."card_invoice_payments" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."card_operators" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."card_operators" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."companies" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "cep" "text",
    "street" "text",
    "number" "text",
    "complement" "text",
    "cnpj" "text",
    "phone" "text",
    "is_whatsapp" boolean DEFAULT false NOT NULL,
    "social_media" "text",
    "website" "text",
    "neighborhood" "text",
    "city" "text",
    "state" "text"
);


ALTER TABLE "public"."companies" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."cost_center_settings" (
    "user_id" "uuid" NOT NULL,
    "maintenance_vault_id" "uuid",
    "wear_provision_enabled" boolean DEFAULT true NOT NULL,
    "fixed_cost_provision_enabled" boolean DEFAULT true NOT NULL,
    "reserve_mode" "text" DEFAULT 'CALENDAR'::"text" NOT NULL,
    "planned_weekdays" smallint DEFAULT 127 NOT NULL,
    "days_off" "jsonb" DEFAULT '[]'::"jsonb" NOT NULL,
    "seeded_catalog_version" integer DEFAULT 0 NOT NULL,
    "dismissed_seed_keys" "jsonb" DEFAULT '[]'::"jsonb" NOT NULL,
    "reminder_enabled" boolean DEFAULT true NOT NULL,
    "reminder_offsets" "jsonb" DEFAULT '[3, 0]'::"jsonb" NOT NULL,
    "cost_center_started_on" "date",
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "cost_center_settings_planned_weekdays_check" CHECK ((("planned_weekdays" >= 0) AND ("planned_weekdays" <= 127))),
    CONSTRAINT "cost_center_settings_reserve_mode_check" CHECK (("reserve_mode" = ANY (ARRAY['CALENDAR'::"text", 'WORKING_DAYS'::"text"])))
);


ALTER TABLE "public"."cost_center_settings" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."credit_cards" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "holder_name" "text" NOT NULL,
    "nickname" "text" NOT NULL,
    "first_four" "text",
    "last_four" "text" NOT NULL,
    "brand_id" "uuid",
    "issuer_id" "uuid",
    "due_day" integer NOT NULL,
    "closing_day" integer NOT NULL,
    "card_type" "text" NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "credit_cards_card_type_check" CHECK (("card_type" = ANY (ARRAY['credito'::"text", 'debito'::"text", 'multiplo'::"text", 'voucher'::"text"]))),
    CONSTRAINT "credit_cards_closing_day_check" CHECK ((("closing_day" >= 1) AND ("closing_day" <= 31))),
    CONSTRAINT "credit_cards_due_day_check" CHECK ((("due_day" >= 1) AND ("due_day" <= 31)))
);


ALTER TABLE "public"."credit_cards" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."daily_totals" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "platform_id" "uuid",
    "amount" numeric(10,2) NOT NULL,
    "distance_km" numeric(8,2) DEFAULT 0,
    "product_type" "public"."product_type" DEFAULT 'alimento'::"public"."product_type",
    "subtract_routes" boolean DEFAULT false,
    "notes" "text",
    "occurred_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "billing_cycle_id" "uuid"
);


ALTER TABLE "public"."daily_totals" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."delivery_partner_sessions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "partner_id" "uuid" NOT NULL,
    "route_id" "uuid",
    "expected_package_count" integer DEFAULT 0 NOT NULL,
    "scanned_barcodes" "jsonb" DEFAULT '[]'::"jsonb" NOT NULL,
    "scanned_count" integer DEFAULT 0 NOT NULL,
    "delivered_count" integer DEFAULT 0 NOT NULL,
    "returned_count" integer DEFAULT 0 NOT NULL,
    "start_time" timestamp with time zone,
    "end_time" timestamp with time zone,
    "amount_paid" numeric DEFAULT 0 NOT NULL,
    "expense_id" "uuid",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "package_rate" numeric DEFAULT 0,
    "default_bonus" numeric DEFAULT 0,
    "platform_id" "uuid",
    "returned_barcodes" "jsonb" DEFAULT '[]'::"jsonb",
    "billing_cycle_id" "uuid"
);


ALTER TABLE "public"."delivery_partner_sessions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."delivery_partners" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "full_name" "text" NOT NULL,
    "cep" "text",
    "street" "text",
    "number" "text",
    "neighborhood" "text",
    "city" "text",
    "state" "text",
    "phone" "text",
    "is_whatsapp" boolean DEFAULT false NOT NULL,
    "social_media" "text",
    "pix_key" "text",
    "pix_bank" "text",
    "cpf" "text",
    "preferred_route_id" "uuid",
    "package_rate" numeric DEFAULT 0 NOT NULL,
    "default_bonus" numeric DEFAULT 0 NOT NULL,
    "delivery_type" "text" DEFAULT 'moto'::"text" NOT NULL,
    "rating" smallint DEFAULT 3 NOT NULL,
    "payment_cycle_type" "text" DEFAULT 'fixed'::"text" NOT NULL,
    "payment_cycle_fixed" "text",
    "payment_cycle_variable_days" integer[],
    "active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "photo_url" "text",
    "cycle_start_date" "date",
    "cycle_end_date" "date",
    "include_end_date" boolean DEFAULT true,
    "payment_delay_days" integer DEFAULT 7,
    "payment_date" "date",
    "variable_cycles" "jsonb" DEFAULT '[]'::"jsonb",
    CONSTRAINT "delivery_partners_delivery_type_check" CHECK (("delivery_type" = ANY (ARRAY['a_pe'::"text", 'bike'::"text", 'moto'::"text", 'carro'::"text", 'utilitario'::"text"]))),
    CONSTRAINT "delivery_partners_payment_cycle_fixed_check" CHECK (("payment_cycle_fixed" = ANY (ARRAY['semanal'::"text", 'quinzenal'::"text", 'mensal'::"text"]))),
    CONSTRAINT "delivery_partners_payment_cycle_type_check" CHECK (("payment_cycle_type" = ANY (ARRAY['fixed'::"text", 'variable'::"text"]))),
    CONSTRAINT "delivery_partners_rating_check" CHECK ((("rating" >= 1) AND ("rating" <= 5)))
);


ALTER TABLE "public"."delivery_partners" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."delivery_routes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."delivery_routes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."expense_settlements" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "expense_id" "uuid" NOT NULL,
    "kind" "text" DEFAULT 'pix'::"text" NOT NULL,
    "account_id" "uuid",
    "vault_id" "uuid",
    "amount" numeric(12,2) NOT NULL,
    "vault_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "account_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "status" "text" DEFAULT 'PENDENTE_DESPESA'::"text" NOT NULL,
    "operation_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "chk_expense_settlements_account" CHECK ((("account_amount" = (0)::numeric) OR ("account_id" IS NOT NULL))),
    CONSTRAINT "chk_expense_settlements_sum" CHECK ((("vault_amount" + "account_amount") = "amount")),
    CONSTRAINT "chk_expense_settlements_vault" CHECK ((("vault_amount" = (0)::numeric) OR ("vault_id" IS NOT NULL))),
    CONSTRAINT "expense_settlements_account_amount_check" CHECK (("account_amount" >= (0)::numeric)),
    CONSTRAINT "expense_settlements_amount_check" CHECK (("amount" >= (0)::numeric)),
    CONSTRAINT "expense_settlements_kind_check" CHECK (("kind" = ANY (ARRAY['pix'::"text", 'debito'::"text"]))),
    CONSTRAINT "expense_settlements_status_check" CHECK (("status" = ANY (ARRAY['PENDENTE_DESPESA'::"text", 'CONFIRMADA'::"text", 'ABORTADA'::"text"]))),
    CONSTRAINT "expense_settlements_vault_amount_check" CHECK (("vault_amount" >= (0)::numeric))
);


ALTER TABLE "public"."expense_settlements" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."expenses" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "category" "public"."expense_category" NOT NULL,
    "title" "text" NOT NULL,
    "vendor" "text",
    "amount" numeric(10,2) NOT NULL,
    "liters" numeric(8,2),
    "fuel_type" "text",
    "price_per_liter" numeric(8,3),
    "odometer_km" numeric(10,1),
    "description" "text",
    "payment_method" "public"."payment_method" DEFAULT 'pix'::"public"."payment_method",
    "occurred_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "receipt_number" "text",
    "is_full_tank" boolean DEFAULT true NOT NULL,
    "invoice_number" "text",
    "part_brand" "text",
    "part_model" "text",
    "card_brand" "text",
    "card_operator" "text",
    "installment_group_id" "uuid",
    "installment_number" integer,
    "installment_total" integer,
    "card_due_day" integer,
    "gas_station_id" "uuid",
    "card_id" "uuid",
    "company_id" "uuid",
    "meal_type" "text"
);


ALTER TABLE "public"."expenses" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."financial_adjustments" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "platform_id" "uuid" NOT NULL,
    "billing_cycle_id" "uuid",
    "type" "text" NOT NULL,
    "amount" numeric NOT NULL,
    "description" "text",
    "occurred_at" "date" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "subtype" "text",
    "notes" "text"
);


ALTER TABLE "public"."financial_adjustments" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."fixed_cost_payments" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "fixed_cost_id" "uuid" NOT NULL,
    "competence_date" "date" NOT NULL,
    "paid_at" timestamp with time zone NOT NULL,
    "amount" numeric(12,2) NOT NULL,
    "source_account_id" "uuid",
    "source_vault_id" "uuid",
    "vault_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "account_amount" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "operation_id" "uuid" NOT NULL,
    "notes" "text",
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "chk_fixed_cost_payments_account" CHECK ((("account_amount" = (0)::numeric) OR ("source_account_id" IS NOT NULL))),
    CONSTRAINT "chk_fixed_cost_payments_sum" CHECK ((("vault_amount" + "account_amount") = "amount")),
    CONSTRAINT "chk_fixed_cost_payments_vault" CHECK ((("vault_amount" = (0)::numeric) OR ("source_vault_id" IS NOT NULL))),
    CONSTRAINT "fixed_cost_payments_account_amount_check" CHECK (("account_amount" >= (0)::numeric)),
    CONSTRAINT "fixed_cost_payments_amount_check" CHECK (("amount" > (0)::numeric)),
    CONSTRAINT "fixed_cost_payments_vault_amount_check" CHECK (("vault_amount" >= (0)::numeric))
);


ALTER TABLE "public"."fixed_cost_payments" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."fixed_cost_versions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "fixed_cost_id" "uuid" NOT NULL,
    "amount" numeric(12,2) NOT NULL,
    "effective_from" "date" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "fixed_cost_versions_amount_check" CHECK (("amount" > (0)::numeric))
);


ALTER TABLE "public"."fixed_cost_versions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."fixed_costs" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "category" "text" NOT NULL,
    "catalog_key" "text",
    "periodicity" "text" NOT NULL,
    "due_day" smallint,
    "due_month" smallint,
    "start_date" "date",
    "end_date" "date",
    "vault_id" "uuid",
    "is_vehicle_related" boolean DEFAULT false NOT NULL,
    "analytical" boolean DEFAULT false NOT NULL,
    "seeded" boolean DEFAULT false NOT NULL,
    "notes" "text",
    "active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "chk_fixed_costs_dates" CHECK ((("end_date" IS NULL) OR ("start_date" IS NULL) OR ("end_date" >= "start_date"))),
    CONSTRAINT "fixed_costs_category_check" CHECK (("category" = ANY (ARRAY['veiculo_tributo'::"text", 'veiculo_protecao'::"text", 'garagem'::"text", 'financiamento'::"text", 'comunicacao'::"text", 'trabalho_fiscal'::"text", 'documentos'::"text", 'protecao_pessoal'::"text", 'equipamento'::"text", 'outro'::"text"]))),
    CONSTRAINT "fixed_costs_due_day_check" CHECK ((("due_day" >= 1) AND ("due_day" <= 31))),
    CONSTRAINT "fixed_costs_due_month_check" CHECK ((("due_month" >= 1) AND ("due_month" <= 12))),
    CONSTRAINT "fixed_costs_periodicity_check" CHECK (("periodicity" = ANY (ARRAY['semanal'::"text", 'mensal'::"text", 'bimestral'::"text", 'trimestral'::"text", 'semestral'::"text", 'anual'::"text"])))
);


ALTER TABLE "public"."fixed_costs" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."gas_station_brands" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."gas_station_brands" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."gas_stations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "name" "text" NOT NULL,
    "address" "text",
    "brand" "text" NOT NULL,
    "fuel_types" "jsonb" DEFAULT '[]'::"jsonb",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "nickname" "text",
    "cep" "text",
    "street" "text",
    "number" "text",
    "neighborhood" "text",
    "city" "text",
    "state" "text",
    "latitude" numeric,
    "longitude" numeric
);


ALTER TABLE "public"."gas_stations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."marketplaces" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid",
    "name" "text" NOT NULL,
    "active" boolean DEFAULT true,
    "created_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."marketplaces" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."master_delivery_routes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "platform_id" "uuid",
    "route_date" "date" DEFAULT CURRENT_DATE NOT NULL,
    "start_location" "text" NOT NULL,
    "start_latitude" numeric(10,7),
    "start_longitude" numeric(10,7),
    "status" "text" DEFAULT 'em_andamento'::"text" NOT NULL,
    "total_packages" integer DEFAULT 0 NOT NULL,
    "delivered_packages" integer DEFAULT 0 NOT NULL,
    "returned_packages" integer DEFAULT 0 NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "finished_at" timestamp with time zone,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "master_delivery_routes_status_check" CHECK (("status" = ANY (ARRAY['em_andamento'::"text", 'concluida'::"text", 'cancelada'::"text"])))
);


ALTER TABLE "public"."master_delivery_routes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."master_route_stops" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "route_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "barcode" "text" NOT NULL,
    "recipient_name" "text",
    "full_address" "text" NOT NULL,
    "street" "text",
    "number" "text",
    "neighborhood" "text",
    "city" "text",
    "state" "text",
    "cep" "text",
    "stop_order" integer DEFAULT 1 NOT NULL,
    "status" "text" DEFAULT 'pendente'::"text" NOT NULL,
    "latitude" numeric(10,7),
    "longitude" numeric(10,7),
    "notes" "text",
    "scanned_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "delivered_at" timestamp with time zone,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "platform_id" "uuid",
    "package_type" "text" DEFAULT 'pacotinho'::"text" NOT NULL,
    "photo_url" "text",
    "photo_expires_at" timestamp with time zone,
    "assigned_partner_id" "uuid",
    "transfer_status" "text",
    "transferred_via" "text",
    "transferred_at" timestamp with time zone,
    "marketplace_name" "text",
    CONSTRAINT "master_route_stops_package_type_check" CHECK (("package_type" = ANY (ARRAY['pacotinho'::"text", 'pacote'::"text", 'volumoso'::"text", 'documento'::"text", 'comida'::"text", 'farmacia'::"text"]))),
    CONSTRAINT "master_route_stops_status_check" CHECK (("status" = ANY (ARRAY['pendente'::"text", 'entregue'::"text", 'ausente'::"text", 'devolvido'::"text"]))),
    CONSTRAINT "master_route_stops_transfer_status_check" CHECK (("transfer_status" = ANY (ARRAY['atribuido_pendente'::"text", 'confirmado'::"text"]))),
    CONSTRAINT "master_route_stops_transferred_via_check" CHECK (("transferred_via" = ANY (ARRAY['manual_master'::"text", 'scan_parceiro'::"text"])))
);


ALTER TABLE "public"."master_route_stops" OWNER TO "postgres";


COMMENT ON COLUMN "public"."master_route_stops"."package_type" IS 'Tipo de carga: pacotinho/pacote, volumoso, documento, comida, farmacia';



CREATE TABLE IF NOT EXISTS "public"."notifications" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "type" "text" NOT NULL,
    "billing_cycle_id" "uuid",
    "read" boolean DEFAULT false NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."notifications" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."oil_changes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "changed_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "km_at_change" numeric DEFAULT 0 NOT NULL,
    "notes" "text",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."oil_changes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."part_maintenance" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "part_name" "text" NOT NULL,
    "life_km" numeric DEFAULT 0 NOT NULL,
    "last_change_km" numeric DEFAULT 0 NOT NULL,
    "last_change_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "company_id" "uuid",
    "part_product_id" "uuid",
    "expense_id" "uuid"
);


ALTER TABLE "public"."part_maintenance" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."part_products" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "part_type_id" "uuid" NOT NULL,
    "brand" "text" NOT NULL,
    "model" "text",
    "default_life_km" numeric DEFAULT 0 NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."part_products" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."part_types" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."part_types" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."partner_session_packages" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "barcode" "text" NOT NULL,
    "origin" "text" DEFAULT 'novo'::"text" NOT NULL,
    "master_stop_id" "uuid",
    "status" "text" DEFAULT 'bipado'::"text" NOT NULL,
    "scanned_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "partner_session_packages_origin_check" CHECK (("origin" = ANY (ARRAY['novo'::"text", 'importado_master'::"text"]))),
    CONSTRAINT "partner_session_packages_status_check" CHECK (("status" = ANY (ARRAY['bipado'::"text", 'entregue'::"text", 'devolvido'::"text"])))
);


ALTER TABLE "public"."partner_session_packages" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."parts_catalog" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "name" "text" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."parts_catalog" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."platforms" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "name" "text" NOT NULL,
    "cycle" "public"."payment_cycle" DEFAULT 'semanal'::"public"."payment_cycle" NOT NULL,
    "payment_day" "text",
    "bank_name" "text",
    "bank_agency" "text",
    "bank_account" "text",
    "pix_key_type" "text",
    "pix_key" "text",
    "pix_bank" "text",
    "active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "segment" "text" DEFAULT 'logistica'::"text" NOT NULL,
    "payment_model" "text" DEFAULT 'producao'::"text" NOT NULL,
    "rules" "jsonb" DEFAULT '{}'::"jsonb" NOT NULL,
    "partner_id" "uuid"
);


ALTER TABLE "public"."platforms" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."profiles" (
    "id" "text" NOT NULL,
    "full_name" "text",
    "email" "text",
    "phone" "text",
    "social_handle" "text",
    "vehicle" "public"."vehicle_type" DEFAULT 'moto'::"public"."vehicle_type",
    "plate" "text",
    "avatar_url" "text",
    "daily_goal" numeric(10,2) DEFAULT 200,
    "monthly_goal" numeric(10,2) DEFAULT 3450,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "gender" "text",
    "weekly_goal" numeric DEFAULT 1000,
    "vehicle_brand" "text",
    "vehicle_model" "text",
    "vehicle_year" integer,
    "tank_size_l" numeric,
    "avg_consumption_kml" numeric,
    "oil_change_km" numeric,
    "tire_size_front" "text",
    "tire_size_rear" "text",
    "has_bag" boolean DEFAULT false,
    "last_oil_change_at" timestamp with time zone
);


ALTER TABLE "public"."profiles" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."reserve_allocation_days" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "allocation_id" "uuid" NOT NULL,
    "day" "date" NOT NULL,
    "platform_id" "text",
    "suggested_amount" numeric(14,6) NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "reserve_allocation_days_suggested_amount_check" CHECK (("suggested_amount" >= (0)::numeric))
);


ALTER TABLE "public"."reserve_allocation_days" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."reserve_allocations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "billing_cycle_id" "uuid" NOT NULL,
    "vault_id" "uuid" NOT NULL,
    "suggested_amount" numeric(12,2) NOT NULL,
    "amount" numeric(12,2) NOT NULL,
    "net_cap_applied" boolean DEFAULT false NOT NULL,
    "status" "text" DEFAULT 'ATIVA'::"text" NOT NULL,
    "operation_id" "uuid" NOT NULL,
    "undone_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "reserve_allocations_amount_check" CHECK (("amount" >= (0)::numeric)),
    CONSTRAINT "reserve_allocations_status_check" CHECK (("status" = ANY (ARRAY['ATIVA'::"text", 'DESFEITA'::"text", 'DISPENSADA'::"text"]))),
    CONSTRAINT "reserve_allocations_suggested_amount_check" CHECK (("suggested_amount" >= (0)::numeric))
);


ALTER TABLE "public"."reserve_allocations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."routes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "text" NOT NULL,
    "platform_id" "uuid",
    "origin" "text",
    "destination" "text",
    "distance_km" numeric(8,2) DEFAULT 0 NOT NULL,
    "amount" numeric(10,2) DEFAULT 0 NOT NULL,
    "tip" numeric(10,2) DEFAULT 0 NOT NULL,
    "product_type" "public"."product_type" DEFAULT 'alimento'::"public"."product_type" NOT NULL,
    "notes" "text",
    "occurred_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "package_count" integer DEFAULT 1 NOT NULL,
    "package_unit_price" numeric DEFAULT 0 NOT NULL,
    "started_at" timestamp with time zone,
    "ended_at" timestamp with time zone,
    "break_minutes" integer DEFAULT 0 NOT NULL,
    "start_km" numeric DEFAULT 0 NOT NULL,
    "end_km" numeric DEFAULT 0 NOT NULL,
    "billing_cycle_id" "uuid",
    "small_packages_count" integer DEFAULT 0,
    "large_packages_count" integer DEFAULT 0,
    "large_packages_prices" "jsonb" DEFAULT '[]'::"jsonb",
    "bonus" numeric DEFAULT 0 NOT NULL
);


ALTER TABLE "public"."routes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."storage_cleanup_logs" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "executed_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "photos_count" integer DEFAULT 0 NOT NULL,
    "status" "text" DEFAULT 'sucesso'::"text" NOT NULL,
    "details" "jsonb" DEFAULT '{}'::"jsonb",
    CONSTRAINT "storage_cleanup_logs_status_check" CHECK (("status" = ANY (ARRAY['sucesso'::"text", 'parcial'::"text", 'erro'::"text"])))
);


ALTER TABLE "public"."storage_cleanup_logs" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."vault_ledger" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "vault_id" "uuid" NOT NULL,
    "operation_id" "uuid" NOT NULL,
    "leg" smallint DEFAULT 0 NOT NULL,
    "entry_type" "text" NOT NULL,
    "amount" numeric(12,2) NOT NULL,
    "occurred_at" timestamp with time zone NOT NULL,
    "ref_type" "text",
    "ref_id" "text",
    "contra_vault_id" "uuid",
    "reverses_entry_id" "uuid",
    "note" "text",
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    CONSTRAINT "chk_vault_ledger_estorno" CHECK ((("entry_type" <> 'estorno'::"text") OR ("reverses_entry_id" IS NOT NULL))),
    CONSTRAINT "chk_vault_ledger_transferencia" CHECK ((("entry_type" <> 'transferencia'::"text") OR ("contra_vault_id" IS NOT NULL))),
    CONSTRAINT "vault_ledger_amount_check" CHECK (("amount" <> (0)::numeric)),
    CONSTRAINT "vault_ledger_entry_type_check" CHECK (("entry_type" = ANY (ARRAY['aporte'::"text", 'retirada'::"text", 'transferencia'::"text", 'ajuste'::"text", 'pagamento_despesa'::"text", 'pagamento_custo_fixo'::"text", 'pagamento_fatura'::"text", 'estorno'::"text"]))),
    CONSTRAINT "vault_ledger_leg_check" CHECK (("leg" = ANY (ARRAY[0, 1]))),
    CONSTRAINT "vault_ledger_ref_type_check" CHECK ((("ref_type" IS NULL) OR ("ref_type" = ANY (ARRAY['despesa'::"text", 'custo_fixo'::"text", 'fatura'::"text", 'manual'::"text", 'baixa_fatura'::"text"]))))
);


ALTER TABLE "public"."vault_ledger" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."vaults" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "account_id" "uuid",
    "name" "text" NOT NULL,
    "purpose" "text" DEFAULT 'outro'::"text" NOT NULL,
    "goal_amount" numeric(12,2),
    "initial_balance" numeric(12,2) DEFAULT 0.00 NOT NULL,
    "tracking_start_date" "date" DEFAULT CURRENT_DATE NOT NULL,
    "active" boolean DEFAULT true NOT NULL,
    "sort_order" integer DEFAULT 0 NOT NULL,
    "created_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "timezone"('utc'::"text", "now"()) NOT NULL,
    "deleted_at" timestamp with time zone,
    CONSTRAINT "vaults_goal_amount_check" CHECK ((("goal_amount" IS NULL) OR ("goal_amount" > (0)::numeric))),
    CONSTRAINT "vaults_purpose_check" CHECK (("purpose" = ANY (ARRAY['manutencao'::"text", 'impostos'::"text", 'garagem'::"text", 'seguro'::"text", 'custos_fixos'::"text", 'viagem'::"text", 'emergencia'::"text", 'outro'::"text"])))
);


ALTER TABLE "public"."vaults" OWNER TO "postgres";


CREATE OR REPLACE VIEW "public"."vault_balances" WITH ("security_invoker"='true') AS
 SELECT "v"."id" AS "vault_id",
    "v"."user_id",
    "v"."account_id",
    (("v"."initial_balance" + COALESCE("sum"("vl"."amount"), 0.00)))::numeric(12,2) AS "balance"
   FROM ("public"."vaults" "v"
     LEFT JOIN "public"."vault_ledger" "vl" ON (("vl"."vault_id" = "v"."id")))
  GROUP BY "v"."id", "v"."user_id", "v"."account_id", "v"."initial_balance";


ALTER VIEW "public"."vault_balances" OWNER TO "postgres";


ALTER TABLE ONLY "public"."bank_accounts"
    ADD CONSTRAINT "bank_accounts_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."billing_cycles"
    ADD CONSTRAINT "billing_cycles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."card_account_links"
    ADD CONSTRAINT "card_account_links_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."card_brands"
    ADD CONSTRAINT "card_brands_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."card_invoice_payments"
    ADD CONSTRAINT "card_invoice_payments_operation_id_key" UNIQUE ("operation_id");



ALTER TABLE ONLY "public"."card_invoice_payments"
    ADD CONSTRAINT "card_invoice_payments_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."card_operators"
    ADD CONSTRAINT "card_operators_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."card_operators"
    ADD CONSTRAINT "card_operators_user_id_name_key" UNIQUE ("user_id", "name");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."companies"
    ADD CONSTRAINT "companies_user_id_name_key" UNIQUE ("user_id", "name");



ALTER TABLE ONLY "public"."cost_center_settings"
    ADD CONSTRAINT "cost_center_settings_pkey" PRIMARY KEY ("user_id");



ALTER TABLE ONLY "public"."credit_cards"
    ADD CONSTRAINT "credit_cards_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."daily_totals"
    ADD CONSTRAINT "daily_totals_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."delivery_partners"
    ADD CONSTRAINT "delivery_partners_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."delivery_routes"
    ADD CONSTRAINT "delivery_routes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."delivery_routes"
    ADD CONSTRAINT "delivery_routes_user_id_name_key" UNIQUE ("user_id", "name");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "expense_settlements_expense_id_key" UNIQUE ("expense_id");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "expense_settlements_operation_id_key" UNIQUE ("operation_id");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "expense_settlements_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."expenses"
    ADD CONSTRAINT "expenses_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."financial_adjustments"
    ADD CONSTRAINT "financial_adjustments_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fixed_cost_payments_operation_id_key" UNIQUE ("operation_id");



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fixed_cost_payments_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."fixed_cost_versions"
    ADD CONSTRAINT "fixed_cost_versions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."fixed_costs"
    ADD CONSTRAINT "fixed_costs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."gas_station_brands"
    ADD CONSTRAINT "gas_station_brands_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."gas_stations"
    ADD CONSTRAINT "gas_stations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."marketplaces"
    ADD CONSTRAINT "marketplaces_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."master_delivery_routes"
    ADD CONSTRAINT "master_delivery_routes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "master_route_stops_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."notifications"
    ADD CONSTRAINT "notifications_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."oil_changes"
    ADD CONSTRAINT "oil_changes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."part_maintenance"
    ADD CONSTRAINT "part_maintenance_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."part_maintenance"
    ADD CONSTRAINT "part_maintenance_user_id_part_name_key" UNIQUE ("user_id", "part_name");



ALTER TABLE ONLY "public"."part_products"
    ADD CONSTRAINT "part_products_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."part_types"
    ADD CONSTRAINT "part_types_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."part_types"
    ADD CONSTRAINT "part_types_user_id_name_key" UNIQUE ("user_id", "name");



ALTER TABLE ONLY "public"."partner_session_packages"
    ADD CONSTRAINT "partner_session_packages_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."parts_catalog"
    ADD CONSTRAINT "parts_catalog_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."platforms"
    ADD CONSTRAINT "platforms_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."profiles"
    ADD CONSTRAINT "profiles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."reserve_allocation_days"
    ADD CONSTRAINT "reserve_allocation_days_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."reserve_allocations"
    ADD CONSTRAINT "reserve_allocations_operation_id_key" UNIQUE ("operation_id");



ALTER TABLE ONLY "public"."reserve_allocations"
    ADD CONSTRAINT "reserve_allocations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."routes"
    ADD CONSTRAINT "routes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."storage_cleanup_logs"
    ADD CONSTRAINT "storage_cleanup_logs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."bank_accounts"
    ADD CONSTRAINT "uq_bank_accounts_id_user" UNIQUE ("id", "user_id");



ALTER TABLE ONLY "public"."fixed_cost_versions"
    ADD CONSTRAINT "uq_fixed_cost_versions_cost_effective" UNIQUE ("fixed_cost_id", "effective_from");



ALTER TABLE ONLY "public"."fixed_costs"
    ADD CONSTRAINT "uq_fixed_costs_id_user" UNIQUE ("id", "user_id");



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "uq_master_route_stop_barcode" UNIQUE ("route_id", "barcode");



ALTER TABLE ONLY "public"."partner_session_packages"
    ADD CONSTRAINT "uq_partner_session_barcode" UNIQUE ("session_id", "barcode");



ALTER TABLE ONLY "public"."reserve_allocations"
    ADD CONSTRAINT "uq_reserve_allocations_id_user" UNIQUE ("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "uq_vault_ledger_id_user" UNIQUE ("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "uq_vault_ledger_operation_leg" UNIQUE ("operation_id", "leg");



ALTER TABLE ONLY "public"."vaults"
    ADD CONSTRAINT "uq_vaults_id_user" UNIQUE ("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "vault_ledger_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."vaults"
    ADD CONSTRAINT "vaults_pkey" PRIMARY KEY ("id");



CREATE INDEX "idx_billing_cycles_lookup" ON "public"."billing_cycles" USING "btree" ("user_id", "platform_id", "status");



CREATE INDEX "idx_card_account_links_account" ON "public"."card_account_links" USING "btree" ("account_id");



CREATE INDEX "idx_card_invoice_payments_card_month" ON "public"."card_invoice_payments" USING "btree" ("card_id", "reference_month");



CREATE INDEX "idx_daily_totals_user_date" ON "public"."daily_totals" USING "btree" ("user_id", "occurred_at" DESC);



CREATE INDEX "idx_delivery_partner_sessions_billing_cycle" ON "public"."delivery_partner_sessions" USING "btree" ("billing_cycle_id");



CREATE INDEX "idx_expense_settlements_vault_id" ON "public"."expense_settlements" USING "btree" ("vault_id");



CREATE INDEX "idx_expenses_user_date" ON "public"."expenses" USING "btree" ("user_id", "occurred_at" DESC);



CREATE INDEX "idx_financial_adjustments_billing_cycle" ON "public"."financial_adjustments" USING "btree" ("billing_cycle_id");



CREATE INDEX "idx_fixed_cost_payments_vault" ON "public"."fixed_cost_payments" USING "btree" ("source_vault_id");



CREATE INDEX "idx_fixed_cost_versions_cost_effective" ON "public"."fixed_cost_versions" USING "btree" ("fixed_cost_id", "effective_from" DESC);



CREATE INDEX "idx_fixed_costs_user_active" ON "public"."fixed_costs" USING "btree" ("user_id", "active");



CREATE UNIQUE INDEX "idx_fixed_costs_user_catalog_key" ON "public"."fixed_costs" USING "btree" ("user_id", "catalog_key") WHERE (("catalog_key" IS NOT NULL) AND ("catalog_key" <> 'outro'::"text") AND ("deleted_at" IS NULL));



CREATE INDEX "idx_master_route_stops_assigned_partner" ON "public"."master_route_stops" USING "btree" ("assigned_partner_id");



CREATE INDEX "idx_master_route_stops_barcode_active" ON "public"."master_route_stops" USING "btree" ("barcode") WHERE (("transfer_status" IS DISTINCT FROM 'confirmado'::"text") AND ("status" <> 'devolvido'::"text"));



CREATE INDEX "idx_master_route_stops_expired_photos" ON "public"."master_route_stops" USING "btree" ("photo_expires_at") WHERE ("photo_url" IS NOT NULL);



CREATE INDEX "idx_master_route_stops_platform" ON "public"."master_route_stops" USING "btree" ("platform_id");



CREATE INDEX "idx_master_routes_date" ON "public"."master_delivery_routes" USING "btree" ("user_id", "route_date" DESC);



CREATE INDEX "idx_master_routes_user_status" ON "public"."master_delivery_routes" USING "btree" ("user_id", "status");



CREATE INDEX "idx_master_stops_barcode" ON "public"."master_route_stops" USING "btree" ("route_id", "barcode");



CREATE INDEX "idx_master_stops_route_order" ON "public"."master_route_stops" USING "btree" ("route_id", "stop_order");



CREATE INDEX "idx_master_stops_user" ON "public"."master_route_stops" USING "btree" ("user_id");



CREATE INDEX "idx_oil_changes_user_date" ON "public"."oil_changes" USING "btree" ("user_id", "changed_at" DESC);



CREATE INDEX "idx_partner_session_packages_barcode" ON "public"."partner_session_packages" USING "btree" ("barcode");



CREATE INDEX "idx_partner_session_packages_session" ON "public"."partner_session_packages" USING "btree" ("session_id");



CREATE INDEX "idx_partner_session_packages_stop" ON "public"."partner_session_packages" USING "btree" ("master_stop_id");



CREATE INDEX "idx_partner_session_packages_user" ON "public"."partner_session_packages" USING "btree" ("user_id");



CREATE INDEX "idx_platforms_user" ON "public"."platforms" USING "btree" ("user_id");



CREATE INDEX "idx_reserve_allocation_days_alloc_day" ON "public"."reserve_allocation_days" USING "btree" ("allocation_id", "day");



CREATE INDEX "idx_reserve_allocations_cycle" ON "public"."reserve_allocations" USING "btree" ("billing_cycle_id");



CREATE INDEX "idx_routes_user_date" ON "public"."routes" USING "btree" ("user_id", "occurred_at" DESC);



CREATE INDEX "idx_storage_cleanup_logs_date" ON "public"."storage_cleanup_logs" USING "btree" ("executed_at" DESC);



CREATE INDEX "idx_vault_ledger_ref" ON "public"."vault_ledger" USING "btree" ("ref_type", "ref_id");



CREATE INDEX "idx_vault_ledger_user_occurred" ON "public"."vault_ledger" USING "btree" ("user_id", "occurred_at" DESC);



CREATE INDEX "idx_vault_ledger_vault_occurred" ON "public"."vault_ledger" USING "btree" ("vault_id", "occurred_at");



CREATE UNIQUE INDEX "idx_vaults_account_lower_name" ON "public"."vaults" USING "btree" ("account_id", "lower"("name")) WHERE ("deleted_at" IS NULL);



CREATE INDEX "idx_vaults_user_account" ON "public"."vaults" USING "btree" ("user_id", "account_id");



CREATE UNIQUE INDEX "uq_card_account_links_card_id" ON "public"."card_account_links" USING "btree" ("card_id") WHERE ("deleted_at" IS NULL);



CREATE UNIQUE INDEX "uq_fixed_cost_payments_cost_competence" ON "public"."fixed_cost_payments" USING "btree" ("fixed_cost_id", "competence_date") WHERE ("deleted_at" IS NULL);



CREATE UNIQUE INDEX "uq_reserve_allocation_days_alloc_day_plat" ON "public"."reserve_allocation_days" USING "btree" ("allocation_id", "day", COALESCE("platform_id", ''::"text"));



CREATE UNIQUE INDEX "uq_reserve_allocations_cycle_vault" ON "public"."reserve_allocations" USING "btree" ("billing_cycle_id", "vault_id") WHERE ("status" = ANY (ARRAY['ATIVA'::"text", 'DISPENSADA'::"text"]));



CREATE OR REPLACE TRIGGER "trg_bank_accounts_updated_at" BEFORE UPDATE ON "public"."bank_accounts" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_card_account_links_updated_at" BEFORE UPDATE ON "public"."card_account_links" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_card_invoice_payments_updated_at" BEFORE UPDATE ON "public"."card_invoice_payments" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_cost_center_settings_updated_at" BEFORE UPDATE ON "public"."cost_center_settings" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_expense_settlements_updated_at" BEFORE UPDATE ON "public"."expense_settlements" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_fixed_cost_payments_updated_at" BEFORE UPDATE ON "public"."fixed_cost_payments" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_fixed_cost_versions_updated_at" BEFORE UPDATE ON "public"."fixed_cost_versions" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_fixed_costs_updated_at" BEFORE UPDATE ON "public"."fixed_costs" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_master_delivery_routes_updated_at" BEFORE UPDATE ON "public"."master_delivery_routes" FOR EACH ROW EXECUTE FUNCTION "public"."handle_master_updated_at"();



CREATE OR REPLACE TRIGGER "trg_master_route_stops_updated_at" BEFORE UPDATE ON "public"."master_route_stops" FOR EACH ROW EXECUTE FUNCTION "public"."handle_master_updated_at"();



CREATE OR REPLACE TRIGGER "trg_reserve_allocation_days_updated_at" BEFORE UPDATE ON "public"."reserve_allocation_days" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_reserve_allocations_updated_at" BEFORE UPDATE ON "public"."reserve_allocations" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



CREATE OR REPLACE TRIGGER "trg_vault_ledger_prevent_mutation" BEFORE DELETE OR UPDATE ON "public"."vault_ledger" FOR EACH ROW EXECUTE FUNCTION "public"."prevent_vault_ledger_mutation"();



CREATE OR REPLACE TRIGGER "trg_vaults_updated_at" BEFORE UPDATE ON "public"."vaults" FOR EACH ROW EXECUTE FUNCTION "public"."handle_updated_at"();



ALTER TABLE ONLY "public"."bank_accounts"
    ADD CONSTRAINT "bank_accounts_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."billing_cycles"
    ADD CONSTRAINT "billing_cycles_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."card_account_links"
    ADD CONSTRAINT "card_account_links_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."card_invoice_payments"
    ADD CONSTRAINT "card_invoice_payments_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."cost_center_settings"
    ADD CONSTRAINT "cost_center_settings_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."credit_cards"
    ADD CONSTRAINT "credit_cards_brand_id_fkey" FOREIGN KEY ("brand_id") REFERENCES "public"."card_brands"("id");



ALTER TABLE ONLY "public"."credit_cards"
    ADD CONSTRAINT "credit_cards_issuer_id_fkey" FOREIGN KEY ("issuer_id") REFERENCES "public"."card_operators"("id");



ALTER TABLE ONLY "public"."daily_totals"
    ADD CONSTRAINT "daily_totals_billing_cycle_id_fkey" FOREIGN KEY ("billing_cycle_id") REFERENCES "public"."billing_cycles"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."daily_totals"
    ADD CONSTRAINT "daily_totals_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_billing_cycle_id_fkey" FOREIGN KEY ("billing_cycle_id") REFERENCES "public"."billing_cycles"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_expense_id_fkey" FOREIGN KEY ("expense_id") REFERENCES "public"."expenses"("id");



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_partner_id_fkey" FOREIGN KEY ("partner_id") REFERENCES "public"."delivery_partners"("id");



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."delivery_partner_sessions"
    ADD CONSTRAINT "delivery_partner_sessions_route_id_fkey" FOREIGN KEY ("route_id") REFERENCES "public"."delivery_routes"("id");



ALTER TABLE ONLY "public"."delivery_partners"
    ADD CONSTRAINT "delivery_partners_preferred_route_id_fkey" FOREIGN KEY ("preferred_route_id") REFERENCES "public"."delivery_routes"("id");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "expense_settlements_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."expenses"
    ADD CONSTRAINT "expenses_card_id_fkey" FOREIGN KEY ("card_id") REFERENCES "public"."credit_cards"("id");



ALTER TABLE ONLY "public"."expenses"
    ADD CONSTRAINT "expenses_company_id_fkey" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."expenses"
    ADD CONSTRAINT "expenses_gas_station_id_fkey" FOREIGN KEY ("gas_station_id") REFERENCES "public"."gas_stations"("id");



ALTER TABLE ONLY "public"."financial_adjustments"
    ADD CONSTRAINT "financial_adjustments_billing_cycle_id_fkey" FOREIGN KEY ("billing_cycle_id") REFERENCES "public"."billing_cycles"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."financial_adjustments"
    ADD CONSTRAINT "financial_adjustments_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fixed_cost_payments_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."fixed_cost_versions"
    ADD CONSTRAINT "fixed_cost_versions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."fixed_costs"
    ADD CONSTRAINT "fixed_costs_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."card_account_links"
    ADD CONSTRAINT "fk_card_account_links_account_user" FOREIGN KEY ("account_id", "user_id") REFERENCES "public"."bank_accounts"("id", "user_id");



ALTER TABLE ONLY "public"."card_invoice_payments"
    ADD CONSTRAINT "fk_card_invoice_payments_account_user" FOREIGN KEY ("source_account_id", "user_id") REFERENCES "public"."bank_accounts"("id", "user_id");



ALTER TABLE ONLY "public"."card_invoice_payments"
    ADD CONSTRAINT "fk_card_invoice_payments_vault_user" FOREIGN KEY ("source_vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."cost_center_settings"
    ADD CONSTRAINT "fk_cost_center_settings_vault_user" FOREIGN KEY ("maintenance_vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id") ON DELETE SET NULL ("maintenance_vault_id");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "fk_expense_settlements_account_user" FOREIGN KEY ("account_id", "user_id") REFERENCES "public"."bank_accounts"("id", "user_id");



ALTER TABLE ONLY "public"."expense_settlements"
    ADD CONSTRAINT "fk_expense_settlements_vault_user" FOREIGN KEY ("vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fk_fixed_cost_payments_account_user" FOREIGN KEY ("source_account_id", "user_id") REFERENCES "public"."bank_accounts"("id", "user_id");



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fk_fixed_cost_payments_cost_user" FOREIGN KEY ("fixed_cost_id", "user_id") REFERENCES "public"."fixed_costs"("id", "user_id");



ALTER TABLE ONLY "public"."fixed_cost_payments"
    ADD CONSTRAINT "fk_fixed_cost_payments_vault_user" FOREIGN KEY ("source_vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."fixed_cost_versions"
    ADD CONSTRAINT "fk_fixed_cost_versions_cost_user" FOREIGN KEY ("fixed_cost_id", "user_id") REFERENCES "public"."fixed_costs"("id", "user_id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."fixed_costs"
    ADD CONSTRAINT "fk_fixed_costs_vault_user" FOREIGN KEY ("vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id") ON DELETE SET NULL ("vault_id");



ALTER TABLE ONLY "public"."reserve_allocation_days"
    ADD CONSTRAINT "fk_reserve_allocation_days_alloc_user" FOREIGN KEY ("allocation_id", "user_id") REFERENCES "public"."reserve_allocations"("id", "user_id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reserve_allocations"
    ADD CONSTRAINT "fk_reserve_allocations_vault_user" FOREIGN KEY ("vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "fk_vault_ledger_contra_user" FOREIGN KEY ("contra_vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "fk_vault_ledger_reverses_user" FOREIGN KEY ("reverses_entry_id", "user_id") REFERENCES "public"."vault_ledger"("id", "user_id");



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "fk_vault_ledger_vault_user" FOREIGN KEY ("vault_id", "user_id") REFERENCES "public"."vaults"("id", "user_id");



ALTER TABLE ONLY "public"."vaults"
    ADD CONSTRAINT "fk_vaults_account_user" FOREIGN KEY ("account_id", "user_id") REFERENCES "public"."bank_accounts"("id", "user_id") ON DELETE SET NULL ("account_id");



ALTER TABLE ONLY "public"."master_delivery_routes"
    ADD CONSTRAINT "master_delivery_routes_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."master_delivery_routes"
    ADD CONSTRAINT "master_delivery_routes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "master_route_stops_assigned_partner_id_fkey" FOREIGN KEY ("assigned_partner_id") REFERENCES "public"."delivery_partners"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "master_route_stops_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "master_route_stops_route_id_fkey" FOREIGN KEY ("route_id") REFERENCES "public"."master_delivery_routes"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."master_route_stops"
    ADD CONSTRAINT "master_route_stops_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."notifications"
    ADD CONSTRAINT "notifications_billing_cycle_id_fkey" FOREIGN KEY ("billing_cycle_id") REFERENCES "public"."billing_cycles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."part_maintenance"
    ADD CONSTRAINT "part_maintenance_company_id_fkey" FOREIGN KEY ("company_id") REFERENCES "public"."companies"("id");



ALTER TABLE ONLY "public"."part_maintenance"
    ADD CONSTRAINT "part_maintenance_expense_id_fkey" FOREIGN KEY ("expense_id") REFERENCES "public"."expenses"("id");



ALTER TABLE ONLY "public"."part_maintenance"
    ADD CONSTRAINT "part_maintenance_part_product_id_fkey" FOREIGN KEY ("part_product_id") REFERENCES "public"."part_products"("id");



ALTER TABLE ONLY "public"."part_products"
    ADD CONSTRAINT "part_products_part_type_id_fkey" FOREIGN KEY ("part_type_id") REFERENCES "public"."part_types"("id");



ALTER TABLE ONLY "public"."partner_session_packages"
    ADD CONSTRAINT "partner_session_packages_master_stop_id_fkey" FOREIGN KEY ("master_stop_id") REFERENCES "public"."master_route_stops"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."partner_session_packages"
    ADD CONSTRAINT "partner_session_packages_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."delivery_partner_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."partner_session_packages"
    ADD CONSTRAINT "partner_session_packages_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."platforms"
    ADD CONSTRAINT "platforms_partner_id_fkey" FOREIGN KEY ("partner_id") REFERENCES "public"."delivery_partners"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reserve_allocation_days"
    ADD CONSTRAINT "reserve_allocation_days_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."reserve_allocations"
    ADD CONSTRAINT "reserve_allocations_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."routes"
    ADD CONSTRAINT "routes_billing_cycle_id_fkey" FOREIGN KEY ("billing_cycle_id") REFERENCES "public"."billing_cycles"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."routes"
    ADD CONSTRAINT "routes_platform_id_fkey" FOREIGN KEY ("platform_id") REFERENCES "public"."platforms"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."vault_ledger"
    ADD CONSTRAINT "vault_ledger_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."vaults"
    ADD CONSTRAINT "vaults_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "auth"."users"("id") ON DELETE CASCADE;



CREATE POLICY "Marketplaces read access" ON "public"."marketplaces" FOR SELECT USING (true);



CREATE POLICY "Marketplaces write access" ON "public"."marketplaces" USING ((("auth"."uid"() = "user_id") OR ("user_id" IS NULL))) WITH CHECK ((("auth"."uid"() = "user_id") OR ("user_id" IS NULL)));



ALTER TABLE "public"."bank_accounts" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "bank_accounts_authenticated_all" ON "public"."bank_accounts" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."billing_cycles" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "billing_cycles_delete_own" ON "public"."billing_cycles" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "billing_cycles_insert_own" ON "public"."billing_cycles" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "billing_cycles_select_own" ON "public"."billing_cycles" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "billing_cycles_update_own" ON "public"."billing_cycles" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."card_account_links" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "card_account_links_authenticated_all" ON "public"."card_account_links" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."card_brands" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "card_brands_delete_own" ON "public"."card_brands" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_brands_insert_own" ON "public"."card_brands" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_brands_select_own" ON "public"."card_brands" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_brands_update_own" ON "public"."card_brands" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."card_invoice_payments" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "card_invoice_payments_authenticated_all" ON "public"."card_invoice_payments" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."card_operators" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "card_operators_delete_own" ON "public"."card_operators" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_operators_insert_own" ON "public"."card_operators" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_operators_select_own" ON "public"."card_operators" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "card_operators_update_own" ON "public"."card_operators" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."companies" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "companies_delete_own" ON "public"."companies" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "companies_insert_own" ON "public"."companies" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "companies_select_own" ON "public"."companies" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "companies_update_own" ON "public"."companies" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."cost_center_settings" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "cost_center_settings_authenticated_all" ON "public"."cost_center_settings" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."credit_cards" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "credit_cards_delete_own" ON "public"."credit_cards" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "credit_cards_insert_own" ON "public"."credit_cards" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "credit_cards_select_own" ON "public"."credit_cards" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "credit_cards_update_own" ON "public"."credit_cards" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."daily_totals" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "daily_totals_delete_own" ON "public"."daily_totals" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "daily_totals_insert_own" ON "public"."daily_totals" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "daily_totals_select_own" ON "public"."daily_totals" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "daily_totals_update_own" ON "public"."daily_totals" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."delivery_partner_sessions" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "delivery_partner_sessions_delete_own" ON "public"."delivery_partner_sessions" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partner_sessions_insert_own" ON "public"."delivery_partner_sessions" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partner_sessions_select_own" ON "public"."delivery_partner_sessions" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partner_sessions_update_own" ON "public"."delivery_partner_sessions" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."delivery_partners" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "delivery_partners_delete_own" ON "public"."delivery_partners" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partners_insert_own" ON "public"."delivery_partners" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partners_select_own" ON "public"."delivery_partners" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_partners_update_own" ON "public"."delivery_partners" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."delivery_routes" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "delivery_routes_delete_own" ON "public"."delivery_routes" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_routes_insert_own" ON "public"."delivery_routes" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_routes_select_own" ON "public"."delivery_routes" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "delivery_routes_update_own" ON "public"."delivery_routes" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."expense_settlements" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "expense_settlements_authenticated_all" ON "public"."expense_settlements" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."expenses" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "expenses_delete_own" ON "public"."expenses" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "expenses_insert_own" ON "public"."expenses" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "expenses_select_own" ON "public"."expenses" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "expenses_update_own" ON "public"."expenses" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."financial_adjustments" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "financial_adjustments_delete_own" ON "public"."financial_adjustments" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "financial_adjustments_insert_own" ON "public"."financial_adjustments" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "financial_adjustments_select_own" ON "public"."financial_adjustments" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "financial_adjustments_update_own" ON "public"."financial_adjustments" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."fixed_cost_payments" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "fixed_cost_payments_authenticated_all" ON "public"."fixed_cost_payments" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."fixed_cost_versions" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "fixed_cost_versions_authenticated_all" ON "public"."fixed_cost_versions" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."fixed_costs" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "fixed_costs_authenticated_all" ON "public"."fixed_costs" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."gas_station_brands" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "gas_station_brands_delete_own" ON "public"."gas_station_brands" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_station_brands_insert_own" ON "public"."gas_station_brands" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_station_brands_select_own" ON "public"."gas_station_brands" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_station_brands_update_own" ON "public"."gas_station_brands" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."gas_stations" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "gas_stations_delete_own" ON "public"."gas_stations" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_stations_insert_own" ON "public"."gas_stations" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_stations_select_own" ON "public"."gas_stations" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "gas_stations_update_own" ON "public"."gas_stations" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."marketplaces" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."master_delivery_routes" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "master_delivery_routes_all" ON "public"."master_delivery_routes" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."master_route_stops" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "master_route_stops_all" ON "public"."master_route_stops" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."notifications" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "notifications_delete_own" ON "public"."notifications" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "notifications_insert_own" ON "public"."notifications" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "notifications_select_own" ON "public"."notifications" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "notifications_update_own" ON "public"."notifications" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."oil_changes" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "oil_changes_delete_own" ON "public"."oil_changes" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "oil_changes_insert_own" ON "public"."oil_changes" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "oil_changes_select_own" ON "public"."oil_changes" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "oil_changes_update_own" ON "public"."oil_changes" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."part_maintenance" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "part_maintenance_delete_own" ON "public"."part_maintenance" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "part_maintenance_insert_own" ON "public"."part_maintenance" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "part_maintenance_select_own" ON "public"."part_maintenance" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "part_maintenance_update_own" ON "public"."part_maintenance" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."part_products" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "part_products_delete_own" ON "public"."part_products" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "part_products_insert_own" ON "public"."part_products" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "part_products_select_own" ON "public"."part_products" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "part_products_update_own" ON "public"."part_products" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."part_types" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "part_types_delete_own" ON "public"."part_types" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "part_types_insert_own" ON "public"."part_types" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "part_types_select_own" ON "public"."part_types" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "part_types_update_own" ON "public"."part_types" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."partner_session_packages" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "partner_session_packages_all" ON "public"."partner_session_packages" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."parts_catalog" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "parts_catalog_delete_own" ON "public"."parts_catalog" FOR DELETE TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "parts_catalog_insert_own" ON "public"."parts_catalog" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = "auth"."uid"()));



CREATE POLICY "parts_catalog_select_own" ON "public"."parts_catalog" FOR SELECT TO "authenticated" USING (("user_id" = "auth"."uid"()));



CREATE POLICY "parts_catalog_update_own" ON "public"."parts_catalog" FOR UPDATE TO "authenticated" USING (("user_id" = "auth"."uid"()));



ALTER TABLE "public"."platforms" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "platforms_delete_own" ON "public"."platforms" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "platforms_insert_own" ON "public"."platforms" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "platforms_select_own" ON "public"."platforms" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "platforms_update_own" ON "public"."platforms" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."profiles" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "profiles_delete_own" ON "public"."profiles" FOR DELETE TO "authenticated" USING (("id" = ("auth"."uid"())::"text"));



CREATE POLICY "profiles_insert_own" ON "public"."profiles" FOR INSERT TO "authenticated" WITH CHECK (("id" = ("auth"."uid"())::"text"));



CREATE POLICY "profiles_select_own" ON "public"."profiles" FOR SELECT TO "authenticated" USING (("id" = ("auth"."uid"())::"text"));



CREATE POLICY "profiles_update_own" ON "public"."profiles" FOR UPDATE TO "authenticated" USING (("id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."reserve_allocation_days" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "reserve_allocation_days_authenticated_all" ON "public"."reserve_allocation_days" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."reserve_allocations" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "reserve_allocations_authenticated_all" ON "public"."reserve_allocations" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."routes" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "routes_delete_own" ON "public"."routes" FOR DELETE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "routes_insert_own" ON "public"."routes" FOR INSERT TO "authenticated" WITH CHECK (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "routes_select_own" ON "public"."routes" FOR SELECT TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



CREATE POLICY "routes_update_own" ON "public"."routes" FOR UPDATE TO "authenticated" USING (("user_id" = ("auth"."uid"())::"text"));



ALTER TABLE "public"."storage_cleanup_logs" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "storage_cleanup_logs_select" ON "public"."storage_cleanup_logs" FOR SELECT TO "authenticated" USING (true);



ALTER TABLE "public"."vault_ledger" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "vault_ledger_insert" ON "public"."vault_ledger" FOR INSERT TO "authenticated" WITH CHECK (("auth"."uid"() = "user_id"));



CREATE POLICY "vault_ledger_select" ON "public"."vault_ledger" FOR SELECT TO "authenticated" USING (("auth"."uid"() = "user_id"));



ALTER TABLE "public"."vaults" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "vaults_authenticated_all" ON "public"."vaults" TO "authenticated" USING (("auth"."uid"() = "user_id")) WITH CHECK (("auth"."uid"() = "user_id"));





ALTER PUBLICATION "supabase_realtime" OWNER TO "postgres";


GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";






















































































































































REVOKE ALL ON FUNCTION "public"."debug_auth_context"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."debug_auth_context"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."get_expired_photos_to_clean"("p_batch_size" integer) FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."get_expired_photos_to_clean"("p_batch_size" integer) TO "service_role";



GRANT ALL ON FUNCTION "public"."get_vault_balances"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_vault_balances"() TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_master_updated_at"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_master_updated_at"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_master_updated_at"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."handle_new_user"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."handle_new_user"() TO "service_role";



GRANT ALL ON FUNCTION "public"."handle_updated_at"() TO "anon";
GRANT ALL ON FUNCTION "public"."handle_updated_at"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."handle_updated_at"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."mark_photos_as_cleaned"("p_stop_ids" "uuid"[], "p_status" "text", "p_details" "jsonb") FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."mark_photos_as_cleaned"("p_stop_ids" "uuid"[], "p_status" "text", "p_details" "jsonb") TO "service_role";



GRANT ALL ON FUNCTION "public"."prevent_vault_ledger_mutation"() TO "anon";
GRANT ALL ON FUNCTION "public"."prevent_vault_ledger_mutation"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."prevent_vault_ledger_mutation"() TO "service_role";



REVOKE ALL ON FUNCTION "public"."rls_auto_enable"() FROM PUBLIC;
GRANT ALL ON FUNCTION "public"."rls_auto_enable"() TO "service_role";


















GRANT ALL ON TABLE "public"."bank_accounts" TO "authenticated";
GRANT ALL ON TABLE "public"."bank_accounts" TO "service_role";



GRANT ALL ON TABLE "public"."billing_cycles" TO "anon";
GRANT ALL ON TABLE "public"."billing_cycles" TO "authenticated";
GRANT ALL ON TABLE "public"."billing_cycles" TO "service_role";



GRANT ALL ON TABLE "public"."card_account_links" TO "authenticated";
GRANT ALL ON TABLE "public"."card_account_links" TO "service_role";



GRANT ALL ON TABLE "public"."card_brands" TO "anon";
GRANT ALL ON TABLE "public"."card_brands" TO "authenticated";
GRANT ALL ON TABLE "public"."card_brands" TO "service_role";



GRANT ALL ON TABLE "public"."card_invoice_payments" TO "authenticated";
GRANT ALL ON TABLE "public"."card_invoice_payments" TO "service_role";



GRANT ALL ON TABLE "public"."card_operators" TO "anon";
GRANT ALL ON TABLE "public"."card_operators" TO "authenticated";
GRANT ALL ON TABLE "public"."card_operators" TO "service_role";



GRANT ALL ON TABLE "public"."companies" TO "anon";
GRANT ALL ON TABLE "public"."companies" TO "authenticated";
GRANT ALL ON TABLE "public"."companies" TO "service_role";



GRANT ALL ON TABLE "public"."cost_center_settings" TO "authenticated";
GRANT ALL ON TABLE "public"."cost_center_settings" TO "service_role";



GRANT ALL ON TABLE "public"."credit_cards" TO "anon";
GRANT ALL ON TABLE "public"."credit_cards" TO "authenticated";
GRANT ALL ON TABLE "public"."credit_cards" TO "service_role";



GRANT ALL ON TABLE "public"."daily_totals" TO "anon";
GRANT ALL ON TABLE "public"."daily_totals" TO "authenticated";
GRANT ALL ON TABLE "public"."daily_totals" TO "service_role";



GRANT ALL ON TABLE "public"."delivery_partner_sessions" TO "anon";
GRANT ALL ON TABLE "public"."delivery_partner_sessions" TO "authenticated";
GRANT ALL ON TABLE "public"."delivery_partner_sessions" TO "service_role";



GRANT ALL ON TABLE "public"."delivery_partners" TO "anon";
GRANT ALL ON TABLE "public"."delivery_partners" TO "authenticated";
GRANT ALL ON TABLE "public"."delivery_partners" TO "service_role";



GRANT ALL ON TABLE "public"."delivery_routes" TO "anon";
GRANT ALL ON TABLE "public"."delivery_routes" TO "authenticated";
GRANT ALL ON TABLE "public"."delivery_routes" TO "service_role";



GRANT ALL ON TABLE "public"."expense_settlements" TO "authenticated";
GRANT ALL ON TABLE "public"."expense_settlements" TO "service_role";



GRANT ALL ON TABLE "public"."expenses" TO "anon";
GRANT ALL ON TABLE "public"."expenses" TO "authenticated";
GRANT ALL ON TABLE "public"."expenses" TO "service_role";



GRANT ALL ON TABLE "public"."financial_adjustments" TO "anon";
GRANT ALL ON TABLE "public"."financial_adjustments" TO "authenticated";
GRANT ALL ON TABLE "public"."financial_adjustments" TO "service_role";



GRANT ALL ON TABLE "public"."fixed_cost_payments" TO "authenticated";
GRANT ALL ON TABLE "public"."fixed_cost_payments" TO "service_role";



GRANT ALL ON TABLE "public"."fixed_cost_versions" TO "authenticated";
GRANT ALL ON TABLE "public"."fixed_cost_versions" TO "service_role";



GRANT ALL ON TABLE "public"."fixed_costs" TO "authenticated";
GRANT ALL ON TABLE "public"."fixed_costs" TO "service_role";



GRANT ALL ON TABLE "public"."gas_station_brands" TO "anon";
GRANT ALL ON TABLE "public"."gas_station_brands" TO "authenticated";
GRANT ALL ON TABLE "public"."gas_station_brands" TO "service_role";



GRANT ALL ON TABLE "public"."gas_stations" TO "anon";
GRANT ALL ON TABLE "public"."gas_stations" TO "authenticated";
GRANT ALL ON TABLE "public"."gas_stations" TO "service_role";



GRANT ALL ON TABLE "public"."marketplaces" TO "anon";
GRANT ALL ON TABLE "public"."marketplaces" TO "authenticated";
GRANT ALL ON TABLE "public"."marketplaces" TO "service_role";



GRANT ALL ON TABLE "public"."master_delivery_routes" TO "anon";
GRANT ALL ON TABLE "public"."master_delivery_routes" TO "authenticated";
GRANT ALL ON TABLE "public"."master_delivery_routes" TO "service_role";



GRANT ALL ON TABLE "public"."master_route_stops" TO "anon";
GRANT ALL ON TABLE "public"."master_route_stops" TO "authenticated";
GRANT ALL ON TABLE "public"."master_route_stops" TO "service_role";



GRANT ALL ON TABLE "public"."notifications" TO "anon";
GRANT ALL ON TABLE "public"."notifications" TO "authenticated";
GRANT ALL ON TABLE "public"."notifications" TO "service_role";



GRANT ALL ON TABLE "public"."oil_changes" TO "anon";
GRANT ALL ON TABLE "public"."oil_changes" TO "authenticated";
GRANT ALL ON TABLE "public"."oil_changes" TO "service_role";



GRANT ALL ON TABLE "public"."part_maintenance" TO "anon";
GRANT ALL ON TABLE "public"."part_maintenance" TO "authenticated";
GRANT ALL ON TABLE "public"."part_maintenance" TO "service_role";



GRANT ALL ON TABLE "public"."part_products" TO "anon";
GRANT ALL ON TABLE "public"."part_products" TO "authenticated";
GRANT ALL ON TABLE "public"."part_products" TO "service_role";



GRANT ALL ON TABLE "public"."part_types" TO "anon";
GRANT ALL ON TABLE "public"."part_types" TO "authenticated";
GRANT ALL ON TABLE "public"."part_types" TO "service_role";



GRANT ALL ON TABLE "public"."partner_session_packages" TO "anon";
GRANT ALL ON TABLE "public"."partner_session_packages" TO "authenticated";
GRANT ALL ON TABLE "public"."partner_session_packages" TO "service_role";



GRANT ALL ON TABLE "public"."parts_catalog" TO "anon";
GRANT ALL ON TABLE "public"."parts_catalog" TO "authenticated";
GRANT ALL ON TABLE "public"."parts_catalog" TO "service_role";



GRANT ALL ON TABLE "public"."platforms" TO "anon";
GRANT ALL ON TABLE "public"."platforms" TO "authenticated";
GRANT ALL ON TABLE "public"."platforms" TO "service_role";



GRANT ALL ON TABLE "public"."profiles" TO "anon";
GRANT ALL ON TABLE "public"."profiles" TO "authenticated";
GRANT ALL ON TABLE "public"."profiles" TO "service_role";



GRANT ALL ON TABLE "public"."reserve_allocation_days" TO "authenticated";
GRANT ALL ON TABLE "public"."reserve_allocation_days" TO "service_role";



GRANT ALL ON TABLE "public"."reserve_allocations" TO "authenticated";
GRANT ALL ON TABLE "public"."reserve_allocations" TO "service_role";



GRANT ALL ON TABLE "public"."routes" TO "anon";
GRANT ALL ON TABLE "public"."routes" TO "authenticated";
GRANT ALL ON TABLE "public"."routes" TO "service_role";



GRANT ALL ON TABLE "public"."storage_cleanup_logs" TO "anon";
GRANT ALL ON TABLE "public"."storage_cleanup_logs" TO "authenticated";
GRANT ALL ON TABLE "public"."storage_cleanup_logs" TO "service_role";



GRANT ALL ON TABLE "public"."vault_ledger" TO "authenticated";
GRANT ALL ON TABLE "public"."vault_ledger" TO "service_role";



GRANT ALL ON TABLE "public"."vaults" TO "authenticated";
GRANT ALL ON TABLE "public"."vaults" TO "service_role";



GRANT ALL ON TABLE "public"."vault_balances" TO "authenticated";
GRANT ALL ON TABLE "public"."vault_balances" TO "service_role";









ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "service_role";



































