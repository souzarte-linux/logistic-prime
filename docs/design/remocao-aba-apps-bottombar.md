# Especificação Técnica de UI/UX — Barra de Navegação Inferior (4 Abas Consolidadas)

**Documento:** Remoção Exclusiva da Aba "Apps" da Barra Inferior, Manutenção da Aba "Painel" e Gestor de Plataformas no Menu Lateral  
**Código da Tarefa:** TASK-DES-05  
**Agente Responsável:** Agente Designer — Time Pocket (Central do Motorista)  
**Arquivo Alvo de Navegação:** [`app/src/main/java/com/fernando/centraldomotorista/navigation/NavGraph.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/navigation/NavGraph.kt)  
**Telas Relacionadas:** [`HomeScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/home/HomeScreen.kt), [`PainelScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/painel/PainelScreen.kt) e [`PlatformsScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/apps/PlatformsScreen.kt)  
**Data:** 27 de Setembro de 2026  
**Status:** Pronto para Implementação (Ready for Dev)  

---

## 1. Diretriz de Produto e Sumário Executivo

### 1.1. Retificação Oficial de Diretrizes
Em conformidade com a validação definitiva de produto do usuário:
1. **A aba "PAINEL" DEVE CONTINUAR EXISTINDO** e compor a barra inferior como a **2ª aba fixa**, fornecendo visão executiva diária, semanal, meta mensal e gráfico de tendência de 7D/30D com layout responsivo 2x1.
2. **A remoção da barra inferior é EXCLUSIVA da aba "Apps"**: A funcionalidade de parametrização cadastral *"App & Plataforma"* (`PlatformsScreen`) pertence ao fluxo de configurações e é acessada estritamente através do **Menu Lateral (Drawer)** em `CADASTRO > App & Plataforma`.
3. **A Bottom Navigation Bar passa a ter exatamente 4 abas fundamentais consolidadas**:
   - 1. 🏠 **Início** (`Screen.Inicio`)
   - 2. 📊 **Painel** (`Screen.Painel`)
   - 3. 📈 **Relatórios** (`Screen.Relatorios`)
   - 4. 🕒 **Histórico** (`Screen.Historico`)

---

## 2. Estrutura da Bottom Navigation Bar (4 Abas Ativas)

```
┌────────────────────────────────────────────────────────────────────────┐
│                   BARRA INFERIOR DE 4 ABAS CONSOLIDADAS                │
├────────────────────┬────────────────────┬────────────────────┬─────────┤
│       INÍCIO       │       PAINEL       │     RELATÓRIOS     │HISTÓRICO│
│      [ Ícone ]     │      [ Ícone ]     │      [ Ícone ]     │[ Ícone ]│
│       Início       │       Painel       │     Relatórios     │Histórico│
│      (~90.dp)      │      (~90.dp)      │      (~90.dp)      │ (~90.dp)│
└────────────────────┴────────────────────┴────────────────────┴─────────┘
```

### 2.1. Métricas de Ergonomia, Grid e Proporções
- **Distribuição Simétrica de Largura:**
  - Em telas de 360.dp: cada aba dispõe de exatamente **90.dp** (`360 / 4 = 90.dp`).
  - Em telas de 390.dp: cada aba dispõe de **97.5.dp**.
  - Em telas de 412.dp: cada aba dispõe de **103.dp**.
- **Acomodação de Texto (Zero Truncamento):**
  - Rótulos: *"Início"* (~35dp), *"Painel"* (~36dp), *"Relatórios"* (~56dp), *"Histórico"* (~52dp).
  - Como o maior texto (*"Relatórios"*) consome ~56dp, a largura útil de 90dp acomoda o texto com folga de mais de 30dp, **garantindo exibição limpa em 1 linha sem quebra ou reticências**.
- **Área de Toque (Touch Target):** `90.dp x 64.dp` (excede em 150% o padrão mínimo de acessibilidade WCAG de 48x48dp).

---

### 2.2. Tokens Visuais do Tema Dark Mode Neon

| Elemento Visual | Token Compose / Hex | Descrição de Uso |
| :--- | :--- | :--- |
| **Container da Barra** | `BottomNavDark` (`#111111`) | Preto sólido / cinza escuro profundo, criando elevação natural contra o `#121212` de fundo |
| **Ícone Ativo** | `OrangeNeon` (`#FF5500`) | Laranja Neon vibrante da marca |
| **Texto Ativo** | `OrangeNeon` (`#FF5500`) | Rótulo da aba ativa em negrito (`FontWeight.Bold`, 10.5.sp a 11.sp) |
| **Pílula de Seleção** | `OrangeNeon.copy(alpha = 0.15f)` | Indicador em pílula elíptica contornando o ícone ativo |
| **Ícone Inativo** | `MaterialTheme.colorScheme.onSurfaceVariant` (`#A0A0A0`) | Cinza neutro com contraste superior a 5.8:1 |
| **Texto Inativo** | `MaterialTheme.colorScheme.onSurfaceVariant` (`#A0A0A0`) | Rótulo em peso normal (`FontWeight.Normal`, 10.sp a 10.5.sp) |
| **Linha Divisória Superior** | `MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)` | Borda sutil de 0.5dp |

---

## 3. Manutenção da Aba "Painel" com Layout 2x1 Responsivo

A aba **Painel** (`PainelScreen.kt`) permanece como o cockpit gerencial do motorista. Conforme especificado na Fase 3, seu layout é otimizado através de uma **grade compacta 2x1**:

### 3.1. Arquitetura da Seção Superior de Métricas
- **Linha 1 — Duas Colunas Lado a Lado (`weight(1f)` cada):**
  - **LUCRO DIÁRIO:** Valor grande em Laranja Neon (20sp negrito), indicador verde de tendência (↑ 0%), subtítulo *"X pacotes hoje"*, altura contida em ~96dp (`isCompact = true`).
  - **LUCRO SEMANAL:** Valor grande em Laranja Neon (20sp negrito), indicador verde de tendência (↑ 0%), subtítulo *"X pacotes esta semana"*, altura contida em ~96dp (`isCompact = true`).
- **Linha 2 — Banner Full-Width com Destaque Neon:**
  - **META MENSAL:** Borda sólida de 1.5dp em Laranja Neon (`BorderStroke(1.5.dp, OrangeNeon)`), ícone de alta no topo direito, valor principal da meta em branco puro (24sp negrito), barra de progresso horizontal em degradê laranja e subtítulo completo (*"Progresso X% • R$ Y faturados • Z pacotes este mês"*).
- **Economia Visual:** Ocupa apenas **~235.dp** de altura (redução de 43% de rolagem vertical), trazendo a seção *"Ganhos por Plataforma"* e o gráfico *"Tendência de Desempenho"* (7D/30D) diretamente para a área visível da tela.

---

## 4. Gestor de Plataformas Exclusivo no Menu Lateral (Drawer)

### 4.1. Localização no Menu Lateral
O acesso ao cadastro e gerenciamento de plataformas continua concentrado no Drawer da `HomeScreen.kt`:
- **Seção Expansível:** `CADASTRO`
- **Item de Menu:** `App & Plataforma`
- **Ícone:** `Icons.Default.Apps` (ou `Icons.Default.Smartphone`)
- **Subtítulo:** *"Plataformas de entrega e repasse"*
- **Rota Invocada:** `"plataformas?fromDrawer=true"`

### 4.2. Comportamento Visual em `PlatformsScreen.kt`
1. **Ocultação da BottomBar:** A barra inferior fica oculta quando em `PlatformsScreen` (pois `Screen.Plataformas` não integra `bottomNavItems`), garantindo foco total nos cartões de ciclo, faturamento e modal de filtros.
2. **TopAppBar com Retorno Obrigatório:**
   - Exibe a seta de retorno ([`Icons.AutoMirrored.Filled.ArrowBack`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/apps/PlatformsScreen.kt#L98)).
   - Ao tocar em voltar, executa `navController.popBackStack()`, conduzindo o usuário com segurança de volta à tela inicial `Início`.

---

## 5. Blocos JSON de Especificação (Formato Padrão do Designer)

### 5.1. Bloco JSON — Bottom Navigation Bar (4 Abas)

```json
{
  "navigation_component_id": "MAIN_BOTTOM_NAVIGATION_BAR_4_TABS",
  "component_type": "NavigationBar",
  "active_tabs_count": 4,
  "container_properties": {
    "background_color_dark": "#111111",
    "background_color_light": "#FFFFFF",
    "tonal_elevation_dp": 8,
    "border_top": {
      "width_dp": 0.5,
      "color": "#222222"
    }
  },
  "items": [
    {
      "index": 0,
      "route": "inicio",
      "title": "Início",
      "icon": "Icons.Default.Home",
      "content_description": "Tela inicial operacional",
      "target_screen": "Screen.Inicio"
    },
    {
      "index": 1,
      "route": "painel",
      "title": "Painel",
      "icon": "Icons.Default.BarChart",
      "content_description": "Painel gerencial de metas e tendências",
      "target_screen": "Screen.Painel"
    },
    {
      "index": 2,
      "route": "relatorios",
      "title": "Relatórios",
      "icon": "Icons.Default.Assessment",
      "content_description": "Painel analítico e relatórios de faturamento",
      "target_screen": "Screen.Relatorios"
    },
    {
      "index": 3,
      "route": "historico",
      "title": "Histórico",
      "icon": "Icons.Default.History",
      "content_description": "Extrato de transações e histórico de rotas",
      "target_screen": "Screen.Historico"
    }
  ],
  "visual_tokens": {
    "selected_icon_color": "#FF5500",
    "selected_text_color": "#FF5500",
    "indicator_pill_color": "#FF550026",
    "unselected_icon_color": "#A0A0A0",
    "unselected_text_color": "#A0A0A0",
    "column_width_dp_at_360w": 90.0,
    "font_size_sp": 10.5,
    "font_weight_selected": "Bold",
    "font_weight_unselected": "Normal"
  }
}
```

---

### 5.2. Bloco JSON — Métricas 2x1 da Aba Painel

```json
{
  "screen_id": "SCREEN_PAINEL",
  "screen_name": "Painel Gerencial (PainelScreen)",
  "component": {
    "component_id": "PAINEL_METRICS_GRID_2X1",
    "component_type": "ResponsiveMetricsGrid",
    "layout_structure": {
      "row_1": {
        "type": "Row",
        "arrangement": "spacedBy(10.dp)",
        "items": [
          {
            "id": "STAT_LUCRO_DIARIO",
            "label": "LUCRO DIÁRIO",
            "weight": 1.0,
            "is_compact": true,
            "value_color": "#FF5500",
            "font_size_sp": 20
          },
          {
            "id": "STAT_LUCRO_SEMANAL",
            "label": "LUCRO SEMANAL",
            "weight": 1.0,
            "is_compact": true,
            "value_color": "#FF5500",
            "font_size_sp": 20
          }
        ]
      },
      "row_2": {
        "type": "FullWidthCard",
        "id": "STAT_META_MENSAL",
        "label": "META MENSAL",
        "highlight": true,
        "border": {
          "width_dp": 1.5,
          "color": "#FF5500"
        },
        "value_color": "#FFFFFF",
        "font_size_sp": 24,
        "has_progress_bar": true,
        "right_icon": "Icons.AutoMirrored.Filled.TrendingUp"
      }
    }
  }
}
```

---

### 5.3. Bloco JSON — Gestor de Plataformas no Drawer

```json
{
  "drawer_entry_id": "DRAWER_ITEM_PLATAFORMAS",
  "section": "CADASTRO",
  "label": "App & Plataforma",
  "subtitle": "Plataformas de entrega e repasse",
  "icon": "Icons.Default.Apps",
  "destination_route": "plataformas?fromDrawer=true",
  "target_screen": "Screen.Plataformas",
  "navigation_rules": {
    "show_in_bottom_bar": false,
    "top_app_bar_back_button": true,
    "on_back_destination": "Screen.Inicio"
  }
}
```

---

## 6. Código Kotlin Compose Drop-in para `NavGraph.kt`

Abaixo estão os trechos exatos para aplicação no arquivo [`NavGraph.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/navigation/NavGraph.kt):

### 6.1. Lista `bottomNavItems` Oficial com 4 Abas (Linhas 78 a 84)

```kotlin
// Em NavGraph.kt: Definição oficial das 4 abas fixas consolidadas
val bottomNavItems = listOf(
    Screen.Inicio,
    Screen.Painel,
    Screen.Relatorios,
    Screen.Historico,
)
```

---

### 6.2. Scaffold e Renderização da `NavigationBar` com 4 Abas

```kotlin
    val isCurrentDestination: (String) -> Boolean = { route ->
        currentRoute == route || currentRoute?.startsWith("$route?") == true
    }
    // A barra inferior aparece exclusivamente nas 4 abas fundamentais
    val showBottomBar = bottomNavItems.any { isCurrentDestination(it.route) }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                val bottomNavContainerColor = if (isDarkMode) BottomNavDark else BottomNavLight
                NavigationBar(
                    containerColor = bottomNavContainerColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = isCurrentDestination(screen.route)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (!isCurrentDestination(screen.route)) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangeNeon,
                                selectedTextColor = OrangeNeon,
                                indicatorColor = OrangeNeon.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // NavHost com modifier.padding(innerPadding)
    }
```

---

## 7. Checklist de Aceite para Engenharia

- [ ] **4 Abas na Barra Inferior:** `bottomNavItems` contém exatamente `[Screen.Inicio, Screen.Painel, Screen.Relatorios, Screen.Historico]`.
- [ ] **Aba Apps Fora da BottomBar:** A aba `Screen.Plataformas` / `Apps` NÃO consta em `bottomNavItems`.
- [ ] **Aba Painel Funcional:** A aba `Screen.Painel` continua acessível como a 2ª aba, renderizando as métricas diária/semanal em 2 colunas e meta mensal em destaque.
- [ ] **Acesso ao Gestor de Plataformas:** Acessível via Menu Lateral (Drawer) em `CADASTRO > App & Plataforma`.
- [ ] **Comportamento da Tela de Plataformas:** Ao abrir pelo Drawer, a barra inferior fica oculta e o TopAppBar exibe a seta de retorno para a tela `Início`.
- [ ] **Estilo Dark Mode Neon:** Barra inferior com fundo `#111111` (`BottomNavDark`) e aba ativa em `#FF5500` (`OrangeNeon`).
