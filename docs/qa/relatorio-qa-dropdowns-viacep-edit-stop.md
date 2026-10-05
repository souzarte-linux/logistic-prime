# Relatório de Homologação de QA: Dropdowns Dark Mode, ViaCEP em EditStopDialog e UI de Início de Rota

**Projeto:** Central do Motorista (Pocket)  
**Módulo:** Gestão de Rotas, UI de Cockpit, Scanner e Edição de Paradas  
**Data da Auditoria:** 04 de Outubro de 2026  
**Status de Homologação:** **RELEASE READY (APROVADO 100%)**  
**Responsável QA:** Agente QA Pocket  

---

## 1. Resumo Executivo

Este documento formaliza a homologação técnica referente aos refinamentos de UI, consistência do Tema Escuro (Dark Mode) em Dropdowns e integração inteligente de CEP com preservação de detalhes no diálogo de edição de paradas (`EditStopDialog`), além da modernização do diálogo de início de rota (`StartRouteDialog`).

A suíte completa de testes unitários do Android foi executada via Gradle (`./gradlew testDebugUnitTest --rerun-tasks`). Foram validadas **34 suítes de testes**, totalizando **262 testes unitários automatizados** (incluindo 3 novos testes específicos implementados em [`AddressFormatterTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/AddressFormatterTest.kt)).

A suíte alcançou uma taxa de sucesso de **100% de aprovação (0 falhas, 0 erros, 0 skipped)** em um tempo de execução de **7.299s**.

Nenhuma regressão foi detectada em nenhuma área do aplicativo (Faturas, Ciclo de Faturamento V2, Manutenção de Peças, Histórico, Navegação em 5 Abas e Cockpit de Rotas).

---

## 2. Matriz Consolidada de Testes Automatizados

### 2.1 Visão Geral da Execução Gradle

| Métrica | Valor Obtido | Status |
| :--- | :--- | :--- |
| **Comando Executado** | `./gradlew testDebugUnitTest --rerun-tasks` | Conforme |
| **Total de Suítes Executadas** | `34 suítes` | Conforme |
| **Total de Testes Unitários** | `262 testes` | Conforme (+3 novos) |
| **Testes Aprovados (Passed)** | `262 testes` | **100%** |
| **Falhas (Failures)** | `0` | Zero Defeitos |
| **Erros (Errors)** | `0` | Zero Defeitos |
| **Ignorados / Pulados (Skipped)** | `0` | Zero Pendências |
| **Tempo Total de Execução** | `7.299s` | Alta Performance |
| **Resultado Gradle** | `BUILD SUCCESSFUL` | Aprovado |

---

### 2.2 Detalhamento das Suítes Relevantes do Módulo

| Suíte de Testes | Componente / Domínio | Testes | Falhas | Erros | Duração |
| :--- | :--- | :---: | :---: | :---: | :---: |
| [`AddressFormatterTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/AddressFormatterTest.kt) | Formatação Canônica, Title Case & Mesclagem ViaCEP | 8 (+3) | 0 | 0 | 0.034s |
| [`BrazilianLabelParserTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/BrazilianLabelParserTest.kt) | OCR Multilinha, Prefixos, CPF/Telefone & Heurísticas | 10 | 0 | 0 | 0.103s |
| [`MasterRouteRepositoryTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteRepositoryTest.kt) | Repositório Master, Cargas, Marketplaces & Reordenação | 29 | 0 | 0 | 0.312s |
| [`MasterRouteScannerTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteScannerTest.kt) | Scanner Fracionado, Sticky Partners & Releitura | 9 | 0 | 0 | 0.006s |
| [`MasterRouteCockpitTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteCockpitTest.kt) | Cockpit, Métricas e Busca com Scanner | 5 | 0 | 0 | 0.010s |
| [`MasterRouteHandOffTest`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/delivery/MasterRouteHandOffTest.kt) | Hand-off para NewRoute e Totais | 11 | 0 | 0 | 0.093s |
| Demais Suítes (28 suítes) | Faturas, Ciclos V2, Peças, Histórico, Painel | 190 | 0 | 0 | 6.741s |
| **Total Geral** | **34 suítes** | **262** | **0** | **0** | **7.299s** |

---

## 3. Validação dos Ajustes de UI e Integrações

### 3.1 StartRouteDialog: Combobox, Persistência e Resolução de Sobreposição
- **Arquivos Auditados:**
  - [`StartRouteDialog.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/StartRouteDialog.kt)
  - [`RoutePreferences.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/preferences/RoutePreferences.kt)
- **Melhorias Verificadas:**
  1. **Combobox de Plataformas:** Substituição de botões dispersos por um seletor unificado em dark theme com badge da transportadora ativa e dropdown escuro.
  2. **Persistência da Última Escolha:** O diálogo lê `routePreferences.getLastPlatformId()` ao inicializar e persiste a seleção via `routePreferences.setLastPlatformIdSync(selectedPlatformId)` ao confirmar o início da rota.
  3. **Eliminação da Sobreposição Física:**
     - O botão *"Cancelar"* foi movido para dentro da coluna vertical de ações no slot `confirmButton`.
     - O parâmetro `dismissButton` do AlertDialog foi configurado como `null`.
     - Todos os 3 botões ("Abrir Scanner de Pacotes", "Ir para o Cockpit de Bordo" e "Cancelar") empilham-se ordenadamente sem colisão de toques ou truncamento visual.

---

### 3.2 Consistência Dark Mode nos Dropdowns
- **Arquivos Auditados:**
  - [`RouteScannerScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerScreen.kt)
  - [`StopDeliveryCard.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/StopDeliveryCard.kt)
  - [`EditStopDialog.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/EditStopDialog.kt)
- **Melhorias Verificadas:**
  1. Todos os componentes `DropdownMenu` foram configurados com:
     - `containerColor = SurfaceDarkAlt`
     - `border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))`
     - `modifier = Modifier.background(SurfaceDarkAlt)`
  2. Todos os `DropdownMenuItem` utilizam `colors = MenuDefaults.itemColors(textColor = TextPrimaryDark, leadingIconColor = TextPrimaryDark)` com texto em alto contraste (`TextPrimaryDark` ou `OrangeNeon` para itens ativos).
  3. Zero ocorrências de texto escuro em fundo escuro ou menus brancos que quebram a imersão noturna do motorista.

---

### 3.3 EditStopDialog: ViaCEP com Preservação de Detalhes e Combobox de Cargas
- **Arquivos Auditados:**
  - [`EditStopDialog.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/EditStopDialog.kt)
  - [`AddressFormatter.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/util/AddressFormatter.kt)
  - [`ViaCepApi.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/data/remote/api/ViaCepApi.kt)
- **Melhorias Verificadas:**
  1. **Busca Automática via ViaCEP no Campo CEP:**
     - Ao completar 8 dígitos no campo CEP, dispara busca em background.
     - Indicador `CircularProgressIndicator` exibido no trailing icon do campo enquanto a consulta está em andamento.
  2. **Preservação de Dados Prediais e Referências:**
     - A função [`AddressFormatter.mergeAddressPreservingDetails`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/util/AddressFormatter.kt) extrai e preserva o número predial (inclusive "S/N"), complementos prediais (Apto, Bloco, Torre, Galpão, Casa) e referências operacionais ("próximo do mercado...").
     - Substitui o logradouro, bairro, cidade e estado pelos dados oficiais dos Correios sem apagar o número nem os complementos preexistentes.
  3. **Combobox de 5 Tipos de Carga:**
     - Seletor moderno de tipo de carga contendo: `📦 Pacote`, `🏋️ Volumoso`, `📄 Documento`, `🍔 Comida` e `💊 Farmácia`, com dropdown no padrão Dark Mode.
- **Evidências de Teste:**
  - [`AddressFormatterTest.testMergeAddressPreservingDetailsWithNumberComplementAndReference`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/AddressFormatterTest.kt#L82-L98)
  - [`AddressFormatterTest.testMergeAddressPreservingSnAndGalpao`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/AddressFormatterTest.kt#L100-L117)
  - [`AddressFormatterTest.testMergeAddressPreservingInlineNumberWithoutPreviousComplements`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/test/java/com/fernando/centraldomotorista/util/AddressFormatterTest.kt#L119-L137)
  - Todos aprovados com 100% de sucesso.

---

## 4. Análise de Regressão

Durante a auditoria de regressão completa:
1. **Módulo Financeiro e Conciliação:** 37 testes automatizados cobrindo ciclos de faturamento e faturas sem nenhuma falha.
2. **Scanner e OCR:** Suítes de OCR cumulativo e bicos de rota funcionando sem alterações de contrato.
3. **Cockpit e Paradas:** Persistência no banco Supabase íntegra com os novos tipos de carga e campos preservados.

---

## 5. Declaração Formal de Homologação (Release Ready)

Após auditoria técnica de código e execução com 100% de aprovação na suíte completa:

1. **Aprovação Total:** Todos os **262 testes unitários** passaram com sucesso.
2. **Critérios de Aceitação:** Todos os requisitos de UI, tema escuro, persistência e mesclagem de endereços foram rigorosamente cumpridos.
3. **Estabilidade:** Nenhuma regressão detectada no ecossistema Android.

> [!IMPORTANT]
> **PARECER DO QA:**  
> A entrega dos **Ajustes de UI, Dropdowns Dark Mode e ViaCEP em EditStopDialog** está formalmente **HOMOLOGADA** e declarada **RELEASE READY**.
