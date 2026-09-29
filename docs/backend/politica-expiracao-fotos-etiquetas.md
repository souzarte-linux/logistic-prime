# 🗑️ Política e Rotina de Limpeza de Fotos de Etiquetas Expiradas

> **Projeto:** Central do Motorista (logistic-prime)  
> **Módulo:** Bipagem e Rota Master (Extensões ADR-003 / Prompt 4)  
> **Bucket Supabase:** `master-route-photos`  
> **Data:** 28 de Setembro de 2026  
> **Status:** **Implementado e Aprovado** ✅  

---

## 1. Motivação e Controle de Custos

O plano gratuito do **Supabase** oferece uma cota de **1 GB de armazenamento no Supabase Storage**.

Na rotina operacional da Rota Master:
- O motorista bipará entre 30 e 100 pacotes por dia.
- O aplicativo permite capturar fotos de backup opcionais de etiquetas quando o OCR estiver incompleto ou para fins de conferência.
- Mesmo com compressão otimizada (~150 KB a 300 KB por foto), um fluxo contínuo de 80 fotos/dia geraria:
  $$80 \text{ fotos/dia} \times 250 \text{ KB} = 20 \text{ MB/dia} \implies \approx 600 \text{ MB/mês}$$
- Em menos de 60 dias, a cota de 1 GB do plano gratuito seria totalmente exaurida.

---

## 2. A Regra de Retenção Configurável (Padrão: 3 Dias)

Para garantir sustentabilidade operacional sem custos adicionais de armazenamento:

1. **Prazo Padrão e Customização pelo Usuário:**
   - O prazo padrão de retenção de fotos de etiquetas foi reduzido para **3 dias** (substituindo a retenção fixa anterior de 15 dias).
   - O prazo é **totalmente configurável pelo usuário** através das preferências do app (`RoutePreferences` via Jetpack DataStore e modal `RouteSettingsDialog` acessível na barra de ações da aba Rota).
   - Opções disponíveis para o motorista: **1 dia**, **3 dias (Padrão)**, **7 dias**, **15 dias** e **30 dias**.

2. **Momento do Cálculo da Expiração:**
   - Durante a rota em andamento, as fotos ficam ativas e protegidas.
   - No momento em que o motorista conclui a rota física via tela do Cockpit (`finishRoute` no `MasterRouteRepository.kt`), o sistema calcula e grava automaticamente:
     $$\text{photo\_expires\_at} = \text{finished\_at} + \text{photoRetentionDays}\text{ dias}$$
     (onde `photoRetentionDays` é obtido das preferências ativas do motorista, assumindo 3 dias por padrão).

3. **Salvaguarda Absoluta de Rotas Ativas:**
   - Rotas com status `'em_andamento'` **NUNCA têm suas fotos apagadas**, independentemente de qualquer valor em carimbos temporais. A função SQL e a Edge Function filtram expressamente `mdr.status <> 'em_andamento'`.

4. **Pós-Expiração:**
   - Transcorrido o prazo configurado desde o encerramento da rota, a foto de backup cumpriu seu objetivo operacional (conferência de entrega, resolução de divergências no galpão e fechamento de ganhos).
   - O arquivo físico é deletado do bucket do Storage e as colunas `photo_url` e `photo_expires_at` são atualizadas para `NULL` no registro `master_route_stops`. Os dados textuais da parada (código de rastreio, endereço, destinatário, status) permanecem permanentemente salvos no banco.

---

## 3. Componentes Implementados

### 3.1. Scripts de Banco de Dados (SQL)
- **Arquivo:** [`job-expiracao-fotos-etiquetas.sql`](file:///d:/Dev/logistic-prime/logistic-prime/docs/backend/job-expiracao-fotos-etiquetas.sql)
  - **Tabela `storage_cleanup_logs`:** Registra histórico de execuções, quantidade de fotos removidas, data/hora e metadados JSON.
  - **Índice `idx_master_route_stops_expired_photos`:** Índice parcial sobre `photo_expires_at` onde `photo_url IS NOT NULL`, eliminando full table scans.
  - **Função `get_expired_photos_to_clean(p_batch_size)`:** Seleciona paradas vencidas garantindo que a rota associada não esteja em andamento.
  - **Função `mark_photos_as_cleaned(p_stop_ids, p_status, p_details)`:** Realiza o update atômico para `NULL` e insere o log de auditoria.

### 3.2. Edge Function (TypeScript / Deno)
- **Arquivo:** [`edge-function-cleanup-photos.ts`](file:///d:/Dev/logistic-prime/logistic-prime/docs/backend/edge-function-cleanup-photos.ts)
  - Caminho sugerido no Supabase CLI: `supabase/functions/clean-expired-photos/index.ts`
  - Remove os arquivos binários do bucket `master-route-photos` via Supabase Storage API (`supabase.storage.from(...).remove(...)`).
  - Dispara a baixa no banco de dados.
  - Suporta modo simulação (`dry_run: true`).

---

## 4. Como Executar e Testar Manualmente

### 4.1. Teste em Modo Simulação (Dry Run)
Executa a consulta e mapeia quais arquivos seriam deletados, sem remover nada fisicamente nem alterar o banco:

```bash
curl -X POST "https://<PROJECT-REF>.supabase.co/functions/v1/clean-expired-photos" \
  -H "Authorization: Bearer <SUPABASE_SERVICE_ROLE_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"batch_size": 50, "dry_run": true}'
```

**Exemplo de Resposta:**
```json
{
  "success": true,
  "mode": "dry_run",
  "message": "Modo Simulação: 8 fotos seriam excluídas.",
  "photos_found": 8,
  "file_paths": [
    "routes/123/stop_abc.jpg",
    "routes/123/stop_def.jpg"
  ],
  "stop_ids": [
    "e9c40242-ff52-475f-b51c-4ce1f31f9d45",
    "a85fd021-36ba-4a49-92db-0f89d59298cc"
  ]
}
```

### 4.2. Execução Efetiva da Limpeza
```bash
curl -X POST "https://<PROJECT-REF>.supabase.co/functions/v1/clean-expired-photos" \
  -H "Authorization: Bearer <SUPABASE_SERVICE_ROLE_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"batch_size": 100, "dry_run": false}'
```

**Exemplo de Resposta:**
```json
{
  "success": true,
  "message": "Rotina executada com sucesso. 8 fotos limpas.",
  "photos_found": 8,
  "photos_deleted": 8,
  "stops_updated": 8,
  "executed_at": "2026-09-28T03:00:15.123Z"
}
```

---

## 5. Estratégias de Agendamento (Automação Diária)

### Opção A: GitHub Actions (Sem Custo Adicional)
Criar workflow `.github/workflows/cleanup-photos.yml`:

```yaml
name: Cleanup Expired Master Route Photos
on:
  schedule:
    - cron: '0 3 * * *' # Executa todo dia às 03:00 UTC
  workflow_dispatch:

jobs:
  cleanup:
    runs-on: ubuntu-latest
    steps:
      - name: Trigger Supabase Cleanup Function
        run: |
          curl -f -X POST "${{ secrets.SUPABASE_URL }}/functions/v1/clean-expired-photos" \
            -H "Authorization: Bearer ${{ secrets.SUPABASE_SERVICE_ROLE_KEY }}" \
            -H "Content-Type: application/json" \
            -d '{"batch_size": 200}'
```

### Opção B: pg_cron Nativo no Supabase
Caso o banco tenha a extensão `pg_cron` e `pg_net` ativas:

```sql
SELECT cron.schedule(
    'clean-expired-master-photos-daily',
    '0 3 * * *',
    $$
    SELECT net.http_post(
        url := 'https://koocvhlprwtdympjwbco.supabase.co/functions/v1/clean-expired-photos',
        headers := '{"Content-Type": "application/json", "Authorization": "Bearer <SERVICE_ROLE_KEY>"}'::jsonb,
        body := '{"batch_size": 100}'::jsonb
    );
    $$
);
```

---

## 6. Consulta de Auditoria no Supabase SQL Editor

Para acompanhar o volume de fotos limpas e garantir que a cota permaneça abaixo de 1 GB:

```sql
SELECT 
    date_trunc('day', executed_at) AS dia,
    COUNT(*) AS total_execucoes,
    SUM(photos_count) AS total_fotos_limpas,
    status
FROM public.storage_cleanup_logs
GROUP BY 1, 4
ORDER BY 1 DESC
LIMIT 30;
```
