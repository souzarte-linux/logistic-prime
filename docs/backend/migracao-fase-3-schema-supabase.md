# Relatório de Execução de Migração — Saneamento do Schema no Supabase (Fase 3)

**Projeto:** Central do Motorista (Pocket Logistics)  
**Tarefa:** TASK-BCK-02  
**Executor:** Agente Backend  
**Ambiente:** Supabase Produção Ativo (`https://koocvhlprwtdympjwbco.supabase.co`)  
**Data da Execução:** 25 de Setembro de 2026  
**Status:** **SUCESSO (100% Executado e Validado)**  
**Documento:** `docs/backend/migracao-fase-3-schema-supabase.md`  

---

## 1. Contexto e Objetivos

Durante a auditoria arquitetural (TASK-BCK-01), foram identificadas lacunas estruturais críticas entre os DTOs do aplicativo Android nativo e o schema real do banco de dados PostgreSQL hospedado no Supabase. Essas divergências causavam falhas com códigos HTTP 400 (Bad Request) e impediam a vinculação de sessões e ajustes financeiros às faturas de repasse:

1. **Enum `payment_cycle`:** Suportava apenas `('semanal', 'quinzenal', 'mensal', 'misto')`. O cadastro ou atualização de plataformas com ciclo `'variavel'` falhava com erro de violação de tipo enum (`22P02 invalid input value for enum`).
2. **Tabela `delivery_partner_sessions`:** Declarava o campo `billing_cycle_id` no DTO Kotlin (`DeliveryPartnerSessionDto.kt`), porém a coluna inexistia na tabela do Supabase. Sessões não podiam ser conciliadas em ciclos de faturamento.
3. **Tabela `financial_adjustments`:** O DTO Kotlin (`FinancialAdjustmentDto.kt`) e os modelos de domínio utilizavam `subtype` e `notes` para classificação detalhada de créditos/débitos (`produto_extraviado`, `inss`, `multas`, etc.), mas a tabela do Supabase continha apenas `description`.
4. **Tabela `billing_cycles`:** O aplicativo esperava controlar a inclusão da data final (`include_end_date`) e registrar a data efetiva de recebimento (`payment_received_date`), campos ausentes na tabela.
5. **Índices de Performance:** Falta de índices nas chaves estrangeiras de ciclo, gerando gargalo de full table scans nas consultas analíticas de faturas.

---

## 2. Script SQL de Migração Executado

O script abaixo foi executado com sucesso no ambiente Supabase via MCP tool `execute_sql`:

```sql
-- =============================================================================
-- CENTRAL DO MOTORISTA - SANEAMENTO DE SCHEMA SUPABASE (FASE 3)
-- =============================================================================

-- 1. Inclusão de 'variavel' no enum payment_cycle
ALTER TYPE payment_cycle ADD VALUE IF NOT EXISTS 'variavel';

-- 2. Adição de billing_cycle_id em delivery_partner_sessions
ALTER TABLE delivery_partner_sessions 
ADD COLUMN IF NOT EXISTS billing_cycle_id UUID REFERENCES billing_cycles(id) ON DELETE SET NULL;

-- 3. Adição de subtype e notes em financial_adjustments
ALTER TABLE financial_adjustments 
ADD COLUMN IF NOT EXISTS subtype TEXT;

ALTER TABLE financial_adjustments 
ADD COLUMN IF NOT EXISTS notes TEXT;

-- 4. Adição de include_end_date e payment_received_date em billing_cycles
ALTER TABLE billing_cycles 
ADD COLUMN IF NOT EXISTS include_end_date BOOLEAN DEFAULT TRUE;

ALTER TABLE billing_cycles 
ADD COLUMN IF NOT EXISTS payment_received_date DATE;

-- 5. Criação de índices de alta performance
CREATE INDEX IF NOT EXISTS idx_delivery_partner_sessions_billing_cycle 
ON delivery_partner_sessions(billing_cycle_id);

CREATE INDEX IF NOT EXISTS idx_financial_adjustments_billing_cycle 
ON financial_adjustments(billing_cycle_id);

CREATE INDEX IF NOT EXISTS idx_billing_cycles_lookup 
ON billing_cycles(user_id, platform_id, status);
```

---

## 3. Evidências de Validação Técnica

Após a execução, foram realizadas consultas de validação no catálogo do PostgreSQL (`pg_enum`, `information_schema.columns`, `information_schema.table_constraints` e `pg_indexes`):

### 3.1. Validação do Enum `payment_cycle`
```sql
SELECT enumlabel FROM pg_enum JOIN pg_type ON pg_enum.enumtypid = pg_type.oid WHERE pg_type.typname = 'payment_cycle';
```
**Resultado:**
```json
[
  {"enumlabel": "semanal"},
  {"enumlabel": "quinzenal"},
  {"enumlabel": "mensal"},
  {"enumlabel": "misto"},
  {"enumlabel": "variavel"}
]
```
✅ **Status:** Valor `'variavel'` integrado com sucesso.

---

### 3.2. Validação das Novas Colunas
```sql
SELECT 
    table_name, 
    column_name, 
    data_type, 
    is_nullable, 
    column_default 
FROM information_schema.columns 
WHERE table_name IN ('delivery_partner_sessions', 'financial_adjustments', 'billing_cycles')
  AND column_name IN ('billing_cycle_id', 'subtype', 'notes', 'include_end_date', 'payment_received_date')
ORDER BY table_name, column_name;
```
**Resultado:**
```json
[
  {
    "table_name": "billing_cycles",
    "column_name": "include_end_date",
    "data_type": "boolean",
    "is_nullable": "YES",
    "column_default": "true"
  },
  {
    "table_name": "billing_cycles",
    "column_name": "payment_received_date",
    "data_type": "date",
    "is_nullable": "YES",
    "column_default": null
  },
  {
    "table_name": "delivery_partner_sessions",
    "column_name": "billing_cycle_id",
    "data_type": "uuid",
    "is_nullable": "YES",
    "column_default": null
  },
  {
    "table_name": "financial_adjustments",
    "column_name": "billing_cycle_id",
    "data_type": "uuid",
    "is_nullable": "YES",
    "column_default": null
  },
  {
    "table_name": "financial_adjustments",
    "column_name": "notes",
    "data_type": "text",
    "is_nullable": "YES",
    "column_default": null
  },
  {
    "table_name": "financial_adjustments",
    "column_name": "subtype",
    "data_type": "text",
    "is_nullable": "YES",
    "column_default": null
  }
]
```
✅ **Status:** Todas as colunas criadas com os tipos e defaults especificados.

---

### 3.3. Validação da Chave Estrangeira
```sql
SELECT
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name,
    rc.delete_rule
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
  ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
  ON ccu.constraint_name = tc.constraint_name
JOIN information_schema.referential_constraints AS rc
  ON rc.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
  AND tc.table_name = 'delivery_partner_sessions'
  AND kcu.column_name = 'billing_cycle_id';
```
**Resultado:**
```json
[
  {
    "table_name": "delivery_partner_sessions",
    "column_name": "billing_cycle_id",
    "foreign_table_name": "billing_cycles",
    "foreign_column_name": "id",
    "delete_rule": "SET NULL"
  }
]
```
✅ **Status:** Chave estrangeira ativa com integridade referencial `ON DELETE SET NULL`.

---

### 3.4. Validação dos Índices
```sql
SELECT tablename, indexname, indexdef 
FROM pg_indexes 
WHERE indexname IN (
    'idx_delivery_partner_sessions_billing_cycle',
    'idx_financial_adjustments_billing_cycle',
    'idx_billing_cycles_lookup'
);
```
**Resultado:**
- `idx_delivery_partner_sessions_billing_cycle`: `USING btree (billing_cycle_id)` em `delivery_partner_sessions`.
- `idx_financial_adjustments_billing_cycle`: `USING btree (billing_cycle_id)` em `financial_adjustments`.
- `idx_billing_cycles_lookup`: `USING btree (user_id, platform_id, status)` em `billing_cycles`.

✅ **Status:** Índices operacionais e prontos para acelerar consultas do app.

---

## 4. Impacto e Benefícios Operacionais

1. **Eliminação de Erros HTTP 400 no Retrofit:** Chamadas da API do Retrofit ao Supabase REST que enviam `billing_cycle_id`, `subtype`, `notes`, `include_end_date` e plataformas com `cycle: "variavel"` agora são aceitas sem rejeição de esquema.
2. **Fidelidade com os DTOs do Android:** Perfeito alinhamento 1:1 entre:
   - [`PlatformDto.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/remote/dto/PlatformDto.kt)
   - [`DeliveryPartnerSessionDto.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/remote/dto/DeliveryPartnerSessionDto.kt)
   - [`FinancialAdjustmentDto.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/remote/dto/FinancialAdjustmentDto.kt)
   - [`BillingCycleDto.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/remote/dto/BillingCycleDto.kt)
3. **Consistência na Conciliação de Faturas:** Permite associar sessões bipadas de parceiros e deduções financeiras discriminadas aos ciclos de fechamento semanais, quinzenais e variáveis.
