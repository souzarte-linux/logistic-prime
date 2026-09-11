# Plano de Implementação - Filtro Multinível em Cascata + Seleção de Período na Tela 'Rotas dos Parceiros'

Refatorar a exibição de sessões de entrega em `PartnerRoutesScreen.kt` e `PartnerRoutesViewModel.kt`, transformando a atual listagem plana em uma **Cascata Hierárquica Multinível (Mês ➔ Semana ➔ Dia ➔ Sessão)** com comportamento retrátil/expansível (Accordion), idêntico à arquitetura adotada na tela `HistoricoScreen.kt`, aplicando estritamente as regras de **`BigDecimal`** para qualquer cálculo e formatação monetária — **e incluindo um seletor de período específico**, que o plano original não contemplava.

---

## Diagnóstico do código atual (verificado nos repositórios)

**No `logistic-prime` (Kotlin/Compose, projeto alvo):**
- `PartnerRoutesViewModel.kt` hoje carrega **todas** as sessões do parceiro e calcula apenas o total do **mês corrente** (`monthDeliveredCount` / `monthTotalAmountPaid`, linhas 79-96). Não existe nenhum estado de filtro por período.
- `PartnerRoutesScreen.kt` não possui nenhum ícone, botão ou diálogo de filtro/período.
- `HistoricoScreen.kt` tem um ícone `Icons.Default.FilterAlt` (linha ~366) que é **puramente decorativo** — sem `onClick`, sem diálogo, sem lógica.
- O projeto já tem um padrão reutilizável de seleção de data única: `showDatePicker(context, selectedDate) { newDate -> ... }` (definido em `ui/screens/routes/NewRouteScreen.kt`), que envolve o `android.app.DatePickerDialog` nativo. Não existe seletor de **intervalo** de datas no app.

**No `pocket-pwa-builder` (React/TS, referência de UX indicada):**
- A tela `src/pages/Relatorios.tsx` já implementa exatamente esse recurso, e é essa lógica que deve ser replicada (adaptada para Kotlin/Compose):
  - Um tipo `Period = 'dia' | 'semana' | 'quinzena' | 'mes' | 'ano' | 'custom'`, com uma lista de presets rotulados: **Dia, Semana, Quinzena, Mês, Ano, Intervalo**.
  - Funções puras `startOf(period)` / `endOf(period)` que calculam o início/fim de cada preset (semana começando na segunda-feira; quinzena = últimos 14 dias; mês = dia 1 até o fim do mês; ano = 1º de janeiro até o fim do ano).
  - Um dropdown mostrando o rótulo do período ativo, que ao abrir lista os presets como botões (o selecionado fica destacado).
  - Quando o preset **"Intervalo"** (`custom`) é escolhido, aparecem **dois campos de data inline** ("De" / "Até") logo abaixo do dropdown, com validação cruzada: a data final não pode ser anterior à inicial (`min`/`max` nos próprios campos) nem posterior a hoje.
  - O intervalo resultante (`range.since` / `range.until`) é recalculado via `useMemo` sempre que `period`, `customStart` ou `customEnd` mudam, com fallback seguro (volta para "semana") caso as datas digitadas sejam inválidas.
  - Todas as listas de dados (rotas, diários, despesas, ajustes) são então filtradas por esse intervalo antes de alimentar os KPIs e gráficos.

**Conclusão:** o plano original não contemplava seleção de período — a versão revisada abaixo reproduz fielmente o modelo de presets do `pocket-pwa-builder` (Dia / Semana / Quinzena / Mês / Ano / Intervalo personalizado), adaptado à stack Kotlin/Compose e reaproveitando o `DatePickerDialog` nativo já usado no projeto para os dois campos de data do modo "Intervalo".

---

## Diretrizes e Blindagem com BigDecimal
*(inalterado em relação ao plano original)*

1. **Proibição Absoluta de `Double` / `Float`:** Nenhuma variável, estado, cálculo ou formatação de moeda utilizará tipos de ponto flutuante primitivos.
2. **Inicialização:** Todos os totais iniciam com `BigDecimal.ZERO`.
3. **Agregação:** Todas as somas utilizam `.add()` ou `.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amountPaid) }`.
4. **Formatação Monetária:**
   ```kotlin
   private fun BigDecimal.formatCurrency(): String {
       val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
       return formatter.format(this)
   }
   ```
5. **Parsers:** Conversão textual direta para `BigDecimal` sem passar por `Double`.

---

## Proposta de Mudanças

### 1. Modelos de Dados da Hierarquia
**Arquivo:** `PartnerSessionHierarchyModels.kt` (novo, pacote `ui.screens.deliverypartners`)

*(inalterado — `PartnerSessionDayGroup`, `PartnerSessionWeekGroup`, `PartnerSessionMonthGroup`, como no plano original)*

**Adição:** incluir um modelo de estado de filtro, espelhando os presets de `Relatorios.tsx` do `pocket-pwa-builder`:

```kotlin
enum class PartnerPeriodPreset(val label: String) {
    DIA("Dia"),
    SEMANA("Semana"),
    QUINZENA("Quinzena"),
    MES("Mês"),
    ANO("Ano"),
    PERSONALIZADO("Intervalo")
}

data class PartnerPeriodFilter(
    val preset: PartnerPeriodPreset = PartnerPeriodPreset.SEMANA,
    val customStart: LocalDate = LocalDate.now().minusDays(7),
    val customEnd: LocalDate = LocalDate.now()
) {
    /** Espelha startOf()/endOf() do Relatorios.tsx, mas em LocalDate (sem hora). */
    fun resolveRange(today: LocalDate = LocalDate.now()): Pair<LocalDate, LocalDate> = when (preset) {
        PartnerPeriodPreset.DIA -> today to today
        PartnerPeriodPreset.SEMANA -> {
            // Semana começa na segunda-feira, como no startOf('semana') original
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            start to start.plusDays(6)
        }
        PartnerPeriodPreset.QUINZENA -> today.minusDays(14) to today
        PartnerPeriodPreset.MES -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        PartnerPeriodPreset.ANO -> LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
        PartnerPeriodPreset.PERSONALIZADO -> customStart to customEnd
    }
}
```

Observação: no `Relatorios.tsx`, `endOf()` inclui `23:59:59.999` do dia final (limite exclusivo em milissegundos); como as sessões aqui são agrupadas por `LocalDate`, o equivalente correto é usar comparação **inclusiva** de datas (`!date.isBefore(start) && !date.isAfter(end)`) em vez de recriar timestamps de milissegundo.

---

### 2. ViewModel
**Arquivo:** `PartnerRoutesViewModel.kt`

- **Atualizar `PartnerRoutesUiState`** (além dos campos já previstos):
  - `monthGroups: List<PartnerSessionMonthGroup> = emptyList()` (resultado já filtrado pelo período ativo)
  - `expandedMonths: Set<String> = emptySet()`
  - `expandedWeeks: Set<String> = emptySet()`
  - `periodFilter: PartnerPeriodFilter = PartnerPeriodFilter()` (default `SEMANA`, como no `Relatorios.tsx` original: `useState<Period>('semana')`)
  - `isPeriodDropdownExpanded: Boolean = false`
- **Lógica de Agrupamento Hierárquico (`buildHierarchy`)** — inalterada em relação ao plano original, mas passa a operar sobre `sessions` já recortadas por `periodFilter.resolveRange()`.
- **Nova função `applyPeriodPreset(preset: PartnerPeriodPreset)`**:
  - Atualiza `periodFilter.preset`.
  - Se `preset != PERSONALIZADO`, resolve o range via `resolveRange()` e filtra as sessões cuja data (mesma extração já usada no `buildHierarchy`: `(startTime ?: createdAt)?.toLocalDate()`) esteja dentro do intervalo (inclusive).
  - Recalcula `monthGroups` e os totais consolidados sobre o resultado filtrado — reproduzindo o `useMemo(() => { ... }, [period, customStart, customEnd])` do original, porém como recomputação síncrona dentro do `StateFlow`.
  - **Auto-expansão ajustada ao filtro**: expande automaticamente o(s) mês(es)/semana(s) que contêm dados no intervalo resultante, em vez de depender apenas de "mês/semana corrente".
- **Nova função `applyCustomPeriod(start: LocalDate, end: LocalDate)`**:
  - Equivale a mudar `customStart`/`customEnd` e forçar `preset = PERSONALIZADO` no original.
  - Validação: se `end.isBefore(start)` ou `end.isAfter(LocalDate.now())`, não aplica e retorna o motivo (reproduzindo as restrições `min`/`max` dos `<input type="date">` do `Relatorios.tsx`) — a tela usa isso para mostrar um erro inline.
  - Caso as datas sejam inválidas/em branco, aplicar o **fallback do original**: cair de volta no preset `SEMANA` em vez de travar a tela.
- **Funções de UI do dropdown**: `fun togglePeriodDropdown()` (equivalente ao `showTimeDropdown` do original).
- **Funções de Alternância (accordion)**: `toggleMonth(monthKey: String)`, `toggleWeek(weekKey: String)` — inalteradas.

---

### 3. Tela Compose
**Arquivo:** `PartnerRoutesScreen.kt`

- **Elementos Preservados Intactos**: TopAppBar, Card de Dados do Motorista, Cards de Métricas do Mês, botão "Iniciar Sessão de Entrega", diálogos existentes — inalterado em relação ao plano original.

- **Novo: Seletor de Período (`PartnerPeriodSelector`)** — porta direta do padrão de `Relatorios.tsx` (`pocket-pwa-builder`) para Compose:
  - Posicionado logo acima da cascata de sessões (abaixo dos cards de métricas do mês), como um dropdown expansível (`ExposedDropdownMenuBox` ou `Surface` + `AnimatedVisibility`, equivalente ao `showTimeDropdown` do original).
  - Botão fechado mostra o rótulo do preset ativo (`Dia`, `Semana`, `Quinzena`, `Mês`, `Ano` ou `Intervalo`) com ícone de chevron que gira ao abrir/fechar — igual ao `ChevronDown`/`ChevronUp` do original.
  - Ao expandir, lista os 6 presets como itens clicáveis, destacando o selecionado com fundo `OrangeNeon` e texto preto (equivalente ao `bg-primary text-primary-foreground` do original) — chamando `applyPeriodPreset(preset)` e fechando o dropdown.
  - **Quando o preset `PERSONALIZADO` ("Intervalo") está ativo**, exibir logo abaixo — fora do dropdown, sempre visível, como no `grid grid-cols-2 gap-3` do original — dois campos lado a lado, "De" e "Até":
    - Cada campo é um `OutlinedTextField` somente leitura que, ao ser tocado, abre o `DatePickerDialog` nativo já usado no projeto (`showDatePicker`, de `ui/screens/routes/NewRouteScreen.kt`) — reaproveitando o padrão existente em vez de introduzir o `DateRangePicker` do Material3.
    - Restrições equivalentes aos `min`/`max` dos `<input type="date">` originais: o `DatePickerDialog` de "Até" não permite data anterior à selecionada em "De", nem posterior a hoje; o de "De" não permite data posterior à selecionada em "Até".
    - Ao confirmar qualquer uma das duas datas, chama `applyCustomPeriod(start, end)`.
  - Os totais consolidados exibidos (cards de métricas e fechamentos de mês/semana/dia) devem refletir sempre `uiState.monthGroups` já filtrado pelo `periodFilter` ativo — nunca a lista completa não filtrada.

- **Cascata Multinível** (Mês ➔ Semana ➔ Dia ➔ Sessão) — inalterada em relação ao plano original (`PartnerMonthAccordionItem`, `PartnerWeekAccordionItem`, `PartnerDaySection`, com bordas, linha conectora, cards de fechamento, animações `AnimatedVisibility`/`animateFloatAsState`).

- **Estado vazio do período**: se `filteredMonthGroups` estiver vazio após aplicar um período personalizado, exibir mensagem clara (ex.: `"Nenhuma sessão encontrada no período selecionado."`) com botão para limpar o filtro — evita a tela parecer quebrada quando o intervalo escolhido não tem dados.

- **Padronização Monetária** — inalterada.

---

### 4. Testes Automatizados
**Arquivo:** `PartnerRoutesViewModelTest.kt` (novo)

- Validar o agrupamento hierárquico (Mês ➔ Semana ➔ Dia ➔ Sessão).
- Validar soma de `deliveredCount` e `amountPaid` (com `BigDecimal`) em todos os níveis.
- Validar auto-expansão inicial de mês e semana corrente.
- Validar `toggleMonth` e `toggleWeek`.
- **Novo:** validar `resolveRange()` para cada um dos 6 presets (`DIA`, `SEMANA`, `QUINZENA`, `MES`, `ANO`, `PERSONALIZADO`), comparando com a lógica de `startOf()`/`endOf()` do `Relatorios.tsx` original (ex.: semana começando na segunda-feira).
- **Novo:** validar `applyPeriodPreset`/`applyCustomPeriod`, incluindo:
  - Sessões nas bordas exatas do intervalo (data inicial e final inclusive).
  - Intervalo sem nenhuma sessão (lista vazia resultante, sem exceção).
  - `applyCustomPeriod` rejeitando `end < start` e `end > hoje`, com fallback para o preset `SEMANA` em caso de datas inválidas (espelhando o fallback do original).
- Garantir que nenhum cálculo gere imprecisão de ponto flutuante.

---

## Plano de Verificação

### Testes Automatizados
```powershell
./gradlew testDebugUnitTest --tests "com.fernando.centraldomotorista.delivery.*"
./gradlew compileDebugKotlin
```

### Verificação Manual
- Revisar a integridade visual da hierarquia na tela `PartnerRoutesScreen`.
- Conferir expansão e retração de meses e semanas com as animações.
- Validar que a linha conectora da semana se ajusta perfeitamente à lista de dias.
- **Novo:** testar o dropdown de período com os 6 presets (Dia, Semana, Quinzena, Mês, Ano, Intervalo), incluindo intervalo personalizado sem resultados e intervalo cruzando múltiplos meses.
- **Novo:** confirmar que os totais dos cards e dos fechamentos de mês/semana/dia mudam corretamente ao trocar o período.
- Validar diálogos de edição, exclusão e detalhes de sessão finalizada.
- Confirmar que nenhum dado ou fluxo pré-existente foi quebrado.
