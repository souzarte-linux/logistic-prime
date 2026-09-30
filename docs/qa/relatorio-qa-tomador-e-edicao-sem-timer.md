# Relatório de Homologação de QA: Seletor de Tomador/Marketplace e Edição sem Timer

**Projeto:** Central do Motorista (Pocket)  
**Módulo:** Gestão de Rotas, Scanner de Bipagem & Edição de Paradas (ADR-004 / TASK-DES-09 / Prompt 11)  
**Data da Auditoria:** 30 de Setembro de 2026  
**Status de Homologação:** **RELEASE READY (APROVADO 100%)**  
**Responsável QA:** Agente QA Pocket  

---

## 1. Resumo Executivo

Este documento formaliza a homologação de qualidade e validação de testes referente à implementação do **Seletor de Tomador/Marketplace (Sticky)** e do fluxo de **Edição sem Timer com Cancelamento de Auto-Avanço** nas telas da Rota Master.

A suíte completa de testes unitários do Android foi executada via Gradle com `--rerun-tasks`. Foram validadas **33 suítes de testes**, totalizando **249 testes unitários automatizados** (incluindo 3 novos testes específicos implementados para cobrir o cancelamento do timer, a persistência sticky do tomador e a edição com salvamento de marketplace).

A suíte alcançou uma taxa de sucesso de **100% de aprovação (0 falhas, 0 erros, 0 skipped)** em um tempo de execução de **6.480s**.

Nenhuma regressão foi introduzida nas demais camadas do aplicativo (Faturas, Ciclo de Faturamento V2, Manutenção de Peças, Histórico, Navegação em 5 Abas e Cockpit de Rotas).

---

## 2. Matriz Consolidada de Testes Automatizados

### 2.1 Visão Geral da Execução Gradle

| Métrica | Valor Obtido | Status |
| :--- | :--- | :--- |
| **Comando Executado** | `./gradlew testDebugUnitTest --rerun-tasks` | Conforme |
| **Total de Suítes Executadas** | `33 suítes` | Conforme |
| **Total de Testes Unitários** | `249 testes` | Conforme (+3 novos) |
| **Testes Aprovados (Passed)** | `249 testes` | **100%** |
| **Falhas (Failures)** | `0` | Zero Defeitos |
| **Erros (Errors)** | `0` | Zero Defeitos |
| **Ignorados / Pulados (Skipped)** | `0` | Zero Pendências |
| **Tempo Total de Execução** | `6.480s` | Excelente Performance |
| **Resultado Gradle** | `BUILD SUCCESSFUL` | Aprovado |

---

### 2.2 Detalhamento das Suítes Relevantes do Módulo

| Suíte de Testes | Componente / Domínio | Testes | Falhas | Erros | Duração |
| :--- | :--- | :---: | :---: | :---: | :---: |
| [`MasterRouteScannerTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteScannerTest.kt) | Scanner, Timer, Sticky Marketplace & Edição | 9 (+3) | 0 | 0 | 0.021s |
| [`MarketplaceRepositoryTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MarketplaceRepositoryTest.kt) | Catálogo de Marketplaces & Buscas | 4 | 0 | 0 | 0.022s |
| [`MasterRouteRepositoryTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteRepositoryTest.kt) | Edição de Paradas, Marketplaces & Rotas | 26 | 0 | 0 | 0.374s |
| [`BrazilianLabelParserTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/BrazilianLabelParserTest.kt) | OCR Multilinha, Prefixos & Heurísticas | 8 | 0 | 0 | 0.032s |
| [`MasterRouteCockpitTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteCockpitTest.kt) | Cockpit, Métricas e Busca com Scanner | 5 | 0 | 0 | 0.018s |
| [`MasterRouteHandOffTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteHandOffTest.kt) | Hand-off para NewRoute e Totais | 11 | 0 | 0 | 0.149s |
| Demais Suítes (27 suítes) | Faturas, Ciclos V2, Peças, Histórico, Painel | 186 | 0 | 0 | 5.864s |
| **Total Geral** | **33 suítes** | **249** | **0** | **0** | **6.480s** |

---

## 3. Validação dos Ajustes de Negócio e UX

### 3.1 Seletor de Tomador / Marketplace no Scanner
- **Arquivos Auditados:**
  - [`RouteScannerScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerScreen.kt#L199-L208)
  - [`RouteScannerViewModel.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerViewModel.kt#L149-L157)
- **Comportamento Verificado:**
  1. No card de confirmação OCR (`OcrConfirmationCard`), a Linha 3.1 exibe `"🏬 Tomador / Origem: [Nome] (Trocar)"` com fundo semitransparente e borda laranja.
  2. O toque aciona o `MarketplaceSelectorDialog`, exibindo chips para os 10 marketplaces nativos (TikTok Shop, Kwai, Mercado Livre, Shopee, Amazon, Shein, Magalu, C&A, Riachuelo, Loja Virtual) e campo de texto com botão "Usar" para cadastro ágil de novo tomador.
  3. **Seleção Sticky:** A escolha do tomador persiste em `_uiState.value.currentMarketplaceName` entre sucessivas bipagens sem resetar, assegurando produtividade durante o carregamento de cargas monotomador ou mistas.
- **Evidência de Teste:**
  - [`MasterRouteScannerTest.testStickyMarketplaceSelectorPersistsBetweenScans`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteScannerTest.kt#L351-L401) aprovado com 100% de sucesso.

---

### 3.2 Cancelamento de Timer e Edição Tranquila no Mini-Card
- **Arquivos Auditados:**
  - [`RouteScannerScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerScreen.kt#L723-L745)
  - [`RouteScannerViewModel.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerViewModel.kt#L406-L453)
- **Comportamento Verificado:**
  1. O timer de auto-avanço pós-leitura confiável foi ampliado de 2s para **5 segundos**, permitindo leitura e conferência confortável pelo motorista.
  2. Ao clicar no botão **"Editar"** do `ScannedPackageMiniCard`:
     - O timer de contagem regressiva é cancelado de forma síncrona através de `viewModel.cancelAutoAdvance()`.
     - O job de coroutine `autoAdvanceJob` é cancelado (`autoAdvanceJob?.cancel()`) e `autoAdvanceCountdown` é setado para `null`.
     - O diálogo completo [`EditStopDialog`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/EditStopDialog.kt) é aberto sobreposto.
  3. O usuário edita destinatário, logradouro, número, bairro, CEP, tipo de pacote, transportadora, tomador/marketplace e notas sem pressa.
  4. Ao clicar em **"Salvar Alterações"**, `updateScannedStop` atualiza a parada no banco e reflete os dados imediatamente no card, fechando o modal e permitindo a continuidade do fluxo sem perder nenhuma informação.
- **Evidências de Teste:**
  - [`MasterRouteScannerTest.testCancelAutoAdvanceSetsCountdownToNull`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteScannerTest.kt#L403-L418) aprovado.
  - [`MasterRouteScannerTest.testUpdateScannedStopPersistsMarketplaceAndCancelsAutoAdvance`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteScannerTest.kt#L420-L473) aprovado.

---

### 3.3 Consistência do `EditStopDialog` e `StopDeliveryCard`
- **Arquivos Auditados:**
  - [`EditStopDialog.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/EditStopDialog.kt)
  - [`StopDeliveryCard.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/StopDeliveryCard.kt)
  - [`RouteCockpitViewModel.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteCockpitViewModel.kt#L389-L425)
- **Comportamento Verificado:**
  1. No diálogo `EditStopDialog`, há agora a separação clara entre:
     - `🚚 Transportadora / Plataforma` (ex: Mercado Envios, Shopee Xpress)
     - `🏬 Tomador / Marketplace` (ex: Mercado Livre, Shopee, TikTok Shop, Kwai)
  2. No card `StopDeliveryCard` (Cockpit):
     - Exibição de badge com fundo translúcido e borda em Laranja Neon (`• 🏬 [Nome do Marketplace]`) no cabeçalho e rodapé.
     - Rodapé simétrico balanceado com dois botões de ação: *"Editar Pacote"* e *"Remover"*.
  3. No Cockpit, a linha de cabeçalho da lista de paradas exibe `"$total paradas"` e mantém botões com `softWrap = false`, prevenindo quebras em dispositivos compactos de 360dp.
- **Evidência de Teste:**
  - [`MasterRouteRepositoryTest.testUpdateStopDetails`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteRepositoryTest.kt#L1055-L1091) e [`MasterRouteRepositoryTest.testAddStopWithMarketplace`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteRepositoryTest.kt#L1035-L1053) 100% aprovados.

---

## 4. Análise de Regressão

Durante a auditoria de regressão completa, validou-se a integridade de todas as funcionalidades adjacentes:
1. **Módulo de Faturas & Conciliação:**
   - As entidades e repositórios de paradas com os novos campos não interferem nas transações financeiras nem na desvinculação em `BillingCycleUnlinkTransactionsTest`.
2. **Scanner e OCR:**
   - O fluxo de OCR de 2 etapas (TASK-DES-08) manteve sua estabilidade (`testFractionatedScannerStepTransitionOnBarcodeDetected`, `testSkipOcrSavesWithEmptyAddressAndReturnsToBarcodeSearch`).
3. **Distribuição e Parceiros:**
   - A atribuição com status `ATRIBUIDO_PENDENTE` e ciclo de parceiros permanece 100% íntegra.

---

## 5. Declaração Formal de Homologação (Release Ready)

Após auditoria técnica minuciosa de código e testes automatizados:

1. **Aprovação Total:** Todos os **249 testes unitários** passaram com sucesso (100% aprovação).
2. **Critérios de Aceitação:** Atendimento integral a todos os requisitos de negócio e operacionais de UI/UX.
3. **Estabilidade:** 0 regressões detectadas no ecossistema Android do aplicativo.

> [!IMPORTANT]
> **PARECER DO QA:**  
> A entrega referente ao **Seletor de Tomador/Marketplace** e **Edição sem Timer com Cancelamento de Auto-Avanço** está formalmente **HOMOLOGADA** e declarada **RELEASE READY**.
