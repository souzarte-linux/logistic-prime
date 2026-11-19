# Diagnóstico e Linha de Base (Baseline) — Fase 0

- **Data da execução:** 17/11/2026
- **Commit base:** `37e918cca58c3d262b1898311a5b71fe6710f396` (HEAD do branch `main`)
- **Branch de trabalho:** `feature/cc-f0-fundacao`
- **Executor:** Engenheiro Android Sênior

---

## 1. Versões do Toolchain

| Componente | Versão |
|---|---|
| Gradle | 8.10.2 |
| Android Gradle Plugin (AGP) | 8.8.2 |
| Kotlin | 2.1.0 |
| Java / JVM Target | 17 (Eclipse Adoptium 17.0.20.1+1) |
| compileSdk | 35 |
| minSdk | 26 |
| targetSdk | 35 |
| Jetpack Compose BOM | 2024.12.01 |
| Material 3 | Via BOM 2024.12.01 |
| Navigation Compose | 2.8.8 |
| Retrofit / OkHttp | 2.11.0 / 4.12.0 |
| Supabase Kt | 3.1.1 (apenas módulo `auth-kt`) |
| DataStore Preferences | 1.1.2 |
| Coroutines | 1.10.1 |

---

## 2. Saída Resumida dos Testes (Baseline Antes das Alterações)

Comando executado:
```bash
./gradlew testDebugUnitTest testReleaseUnitTest assembleDebug
```

### Resultado:
- **`testDebugUnitTest`:** 301 testes executados, 301 aprovados, 0 falhas, 0 erros, 0 ignorados.
- **`testReleaseUnitTest`:** 301 testes executados, 301 aprovados, 0 falhas, 0 erros, 0 ignorados.
- **`assembleDebug`:** Compilação concluída com sucesso (`BUILD SUCCESSFUL in 4m 8s`).
- **Testes com falha pré-existente:** Nenhum teste falhando antes da mudança.
- **Status da Baseline:** ✅ Íntegra e verde.

---

## 3. Mapa Arquivo → Função → Tabela (Telas de Manutenção, Histórico e Cartões)

### 3.1 Tela de Manutenção (`LancarManutencaoScreen`, `PartMaintenanceScreen`)
- **Arquivos:**
  - `ui/screens/pecas/LancarManutencaoScreen.kt`
  - `ui/screens/pecas/PartMaintenanceScreen.kt`
  - `ui/screens/pecas/PartMaintenanceViewModel.kt`
  - `data/repository/PartMaintenanceRepository.kt`
  - `data/repository/ExpenseRepository.kt`
  - `data/remote/api/PartMaintenanceApi.kt`
  - `data/remote/api/ExpenseApi.kt`
- **Funções principais:**
  - `PartMaintenanceViewModel.savePartMaintenance()`: Cria primeiro um registro em `expenses` com categoria `manutencao` via `ExpenseRepository.createExpense()` e, subsequentemente, cria o registro em `part_maintenance` via `PartMaintenanceRepository.createPartMaintenance()`.
  - `PartMaintenanceViewModel.loadParts()`: Consulta histórico de manutenções via `PartMaintenanceRepository.getPartMaintenances()`.
  - `PartMaintenanceViewModel.deleteMaintenance()`: Exclui a peça e remove a despesa vinculada via `ExpenseRepository.deleteExpense()`.
- **Tabelas Supabase:**
  - `expenses` (despesa financeira gerada)
  - `part_maintenance` (registro da peça substituída, km e vida útil)

### 3.2 Tela de Histórico (`HistoricoScreen`)
- **Arquivos:**
  - `ui/screens/historico/HistoricoScreen.kt`
  - `ui/screens/historico/HistoricoViewModel.kt`
  - `data/repository/HistoricoRepository.kt`
  - `data/repository/DailyTotalRepository.kt`
  - `data/repository/ExpenseRepository.kt`
  - `data/remote/api/DailyTotalApi.kt`
  - `data/remote/api/ExpenseApi.kt`
- **Funções principais:**
  - `HistoricoRepository.loadHistoricoData()`: Carrega entradas diárias (`daily_totals`) e saídas (`expenses`), mesclando em itens ordenados cronologicamente.
  - `HistoricoViewModel.calculatePeriodMetrics()`: Calcula métricas agregadas do período (entradas brutas, despesas, saldo de caixa). Nota: despesas em cartão de crédito são contabilizadas integralmente na data da compra (regime de caixa puro).
  - `HistoricoViewModel.deleteExpense()`: Exclusão de lançamentos financeiros via `ExpenseRepository.deleteExpense()`.
- **Tabelas Supabase:**
  - `daily_totals` (ganhos diários por plataforma)
  - `expenses` (despesas operacionais, combustível, manutenção, etc.)

### 3.3 Tela de Cartões (`CreditCardsScreen`, `CardManagementScreen`)
- **Arquivos:**
  - `ui/screens/cards/CreditCardsScreen.kt`
  - `ui/screens/cards/CardManagementScreen.kt`
  - `ui/screens/cards/CreditCardsViewModel.kt`
  - `data/repository/CreditCardRepository.kt`
  - `data/repository/CardBrandRepository.kt`
  - `data/repository/CardOperatorRepository.kt`
  - `data/remote/api/CreditCardApi.kt`
  - `data/remote/api/CardBrandApi.kt`
  - `data/remote/api/CardOperatorApi.kt`
- **Funções principais:**
  - `CreditCardsViewModel.loadCards()`: Lista os cartões cadastrados pelo usuário via `CreditCardRepository.getCreditCards()`.
  - `CreditCardsViewModel.saveCard()`: Salva ou edita cartão de crédito/débito via `CreditCardRepository.createCreditCard()` / `updateCreditCard()`.
  - `CreditCardsViewModel.deleteCard()`: Remove cartão via `CreditCardRepository.deleteCreditCard()`.
- **Tabelas Supabase:**
  - `credit_cards` (cadastro de cartões, tipo, limite, dia de fechamento/vencimento)
  - `card_brands` (bandeiras cadastradas)
  - `card_operators` (emissor/banco/operadora)

---

## 4. Achados Classificados (CRÍTICO / ALTO / MÉDIO)

### [CRÍTICO] Exposição Direta ao PostgREST com Chave Anon sem Camada Intermediária
- **Descrição:** O app realiza todas as chamadas CRUD diretamente aos endpoints REST do Supabase (`/rest/v1/...`) usando a chave pública anônima (`SUPABASE_ANON_KEY`).
- **Impacto:** A integridade, privacidade e isolamento dos dados dependem 100% de políticas RLS (*Row Level Security*) estritamente configuradas no Supabase. Qualquer tabela nova sem RLS ou com política frouxa permitirá leitura e escrita não autorizada.

### [ALTO] Falta de Atomicidade na Criação e Exclusão de Manutenções
- **Descrição:** `PartMaintenanceViewModel.savePartMaintenance()` efetua dois disparos HTTP sequenciais assíncronos (`createExpense` e depois `createPartMaintenance`).
- **Impacto:** Se a rede falhar entre as duas chamadas ou o segundo POST falhar, cria-se uma despesa financeira órfã sem vínculo com a peça substituída. O mesmo risco ocorre na exclusão.

### [ALTO] Ausência de Persistência Local e Operação Offline
- **Descrição:** O aplicativo não possuía nenhum banco de dados local (Room ou SQLite), nem fila de sincronização (Outbox).
- **Impacto:** Se o motorista estiver em área com sinal instável ou sem conexão à internet (comum em rodovias ou subsolos), qualquer tentativa de registro falha imediatamente na interface com erro de rede.

### [MÉDIO] Falta de Injeção de Dependências e Inicializador de Aplicação
- **Descrição:** O projeto não continha classe `Application` personalizada (`android:name` ausente no `AndroidManifest.xml`), e os repositórios eram instanciados usando parâmetros default para objetos globais `RetrofitClient.*`.
- **Impacto:** Acoplamento direto entre camadas e dificuldade de configurar serviços que necessitam de ciclo de vida global (como o WorkManager). A introdução do Hilt na Fase 0 resolve este gargalo sem quebrar o código legado.

### [MÉDIO] Regime Financeiro Misto sem Segregação de Lucro Real vs. Caixa
- **Descrição:** A despesa no cartão de crédito é subtraída do caixa no exato momento da compra, distorcendo o saldo financeiro disponível até o efetivo pagamento da fatura.
- **Impacto:** Dificulta a previsibilidade do fluxo de caixa e o provisionamento para despesas futuras, necessidade que motiva a introdução do Centro de Custos.
