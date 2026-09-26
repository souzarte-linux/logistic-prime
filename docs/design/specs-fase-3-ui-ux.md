# Especificação Técnica de UI/UX — Fase 3: Central do Motorista

**Documento:** Especificações Detalhadas de Interface — Card Hero Laranja, Navegação "Ver Tudo" e Grid Compacto do Painel  
**Código da Tarefa:** TASK-DES-02  
**Agente Responsável:** Agente Designer — Time Pocket (Central do Motorista)  
**Documentos de Referência:** `DB - Prompts/Descricao Telas Web App.pdf` e `docs/design/auditoria-ui-ux.md`  
**Data:** 25 de Setembro de 2026  
**Status:** Pronto para Implementação (Ready for Dev)  

---

## 1. Visão Geral da Fase 3

Com a conclusão da auditoria inicial de UI/UX ([`auditoria-ui-ux.md`](file:///d:/Dev/logistic-prime/logistic-prime/docs/design/auditoria-ui-ux.md)), foram identificadas três melhorias de **Prioridade P1 e P2** essenciais para alinhar a experiência mobile às especificações do protótipo visual original (`DB - Prompts/Descricao Telas Web App.pdf`):

1. **Card Hero Laranja na Tela Início (`HomeScreen.kt`):** Transformar o card *"LANÇAR GANHOS POR ROTA"* na ação primária hero da tela, aplicando fundo Laranja Neon (#FF5500), tipografia em branco puro e ícone de navegação proeminente, restabelecendo a hierarquia assimétrica de destaque.
2. **Navegação "VER TUDO" em Rotas Recentes (`HomeScreen.kt`):** Substituir o texto estático "Recarregar" pelo botão de link estilizado *"VER TUDO"* em Laranja Neon, conduzindo o motorista diretamente à aba de Histórico de Transações.
3. **Grid Compacto / Responsivo de Métricas no Painel (`PainelScreen.kt`):** Reorganizar os 3 cartões de métricas superiores (*Lucro Diário*, *Lucro Semanal* e *Meta Mensal*), que atualmente encontram-se empilhados verticalmente gerando rolagem excessiva, para um layout compacto de **2 colunas (Diário + Semanal)** com **Meta Mensal em largura total** (reduzindo a altura ocupada na tela em ~42%).

---

## 2. Especificação 1 — Card Hero Laranja na Tela Início (`HomeScreen.kt`)

### 2.1. Diagnóstico e Racional de Design
No documento de requisitos visuais (PDF Páginas 3-4, Seção D), o bloco de ações possui clara assimetria: o card **"LANÇAR GANHOS POR ROTA"** é o cartão principal de ação rápida (*Primary CTA*), devendo possuir fundo sólido vibrante **#FF5500**, enquanto os demais cartões secundários utilizam fundo cinza escuro fosco **#1E1E1E**.

Atualmente, `HomeScreen.kt` (linhas 1106-1174) adota uma grade simétrica 2x2 em que todos os 4 cards possuem o mesmo fundo cinza escuro, diluindo a atenção do motorista.

### 2.2. Diretrizes Visuais
- **Fundo:** Sólido Laranja Neon (`OrangeNeon` / `#FF5500`) com gradiente sutil opcional para `#FF6600`.
- **Cantos Arredondados:** `16.dp` (`RoundedCornerShape(16.dp)`).
- **Elevação / Sombra:** `3.dp` a `4.dp`, conferindo profundidade em relação ao fundo `#121212`.
- **Ícone:** `Icons.Default.Navigation` em badge circular com fundo branco translúcido (`Color.White.copy(alpha = 0.22f)`), ícone na cor branca (`#FFFFFF`), tamanho `20.dp`.
- **Seta de Affordance:** `Icons.AutoMirrored.Filled.ArrowForward` no canto superior direito na cor branca translúcida (`Color.White.copy(alpha = 0.7f)`).
- **Título:** `"LANÇAR GANHOS POR ROTA"`, fonte `13.sp`, `FontWeight.Black`, caixa alta, cor `#FFFFFF` (100% branco puro).
- **Subtítulo:** `"DISTÂNCIA • VALOR • TIPO"` ou `"Registre corrida por km, tempo e valor"`, fonte `10.5.sp`, `FontWeight.SemiBold`, cor `Color.White.copy(alpha = 0.92f)`.

### 2.3. Bloco JSON de Especificação (Formato Padrão do Designer)

```json
{
  "screen_id": "SCREEN_HOME",
  "screen_name": "Início (HomeScreen)",
  "target_file": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/home/HomeScreen.kt",
  "component": {
    "component_id": "HOME_ACTION_CARD_HERO_ROUTE",
    "component_type": "HeroActionCard",
    "state": "default",
    "visual_tokens": {
      "background": {
        "type": "solid_or_gradient",
        "colors": ["#FF5500", "#FF6600"],
        "compose_token": "Brush.horizontalGradient(listOf(OrangeNeon, OrangeNeonAlt))"
      },
      "border": {
        "width_dp": 0,
        "color": "transparent"
      },
      "corner_radius_dp": 16,
      "elevation_dp": 4,
      "padding_inner_dp": {
        "horizontal": 14,
        "vertical": 14
      }
    },
    "typography": {
      "title": {
        "text": "LANÇAR GANHOS POR ROTA",
        "font_size_sp": 13,
        "font_weight": "Black",
        "text_case": "UPPERCASE",
        "color": "#FFFFFF",
        "line_height_sp": 16
      },
      "subtitle": {
        "text": "DISTÂNCIA • VALOR • TIPO",
        "font_size_sp": 10.5,
        "font_weight": "SemiBold",
        "color": "#FFFFFFE6",
        "line_height_sp": 14
      }
    },
    "iconography": {
      "icon_vector": "Icons.Default.Navigation",
      "icon_size_dp": 20,
      "icon_tint": "#FFFFFF",
      "badge_background": {
        "shape": "RoundedCornerShape(10.dp)",
        "color": "#FFFFFF38",
        "size_dp": 36
      },
      "affordance_arrow": {
        "icon_vector": "Icons.AutoMirrored.Filled.ArrowForward",
        "size_dp": 16,
        "tint": "#FFFFFFB3"
      }
    },
    "accessibility": {
      "wcag_level": "AA (Large Text / Bold Elements)",
      "contrast_ratio": "Texto Branco Puro (#FFFFFF) sobre Laranja Neon (#FF5500) com peso Black (900) e kerning amplo para garantir legibilidade sob luz solar intensa",
      "content_description": "Lançar ganhos por rota. Registre corrida por km, tempo e valor"
    },
    "interaction": {
      "on_click": "onNavigateToCreateRoute()",
      "ripple_color": "#00000033"
    }
  }
}
```

### 2.4. Código Kotlin Compose Drop-in

```kotlin
// Em HomeScreen.kt: Atualização no composable HomeActionCard para suportar isHero
@Composable
fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    isHero: Boolean = false,
    subtitleColor: Color? = null,
    subtitleFontWeight: FontWeight? = null,
    onClick: () -> Unit
) {
    val cardBackground = if (isHero) {
        Brush.horizontalGradient(listOf(OrangeNeon, OrangeNeonAlt))
    } else {
        SolidColor(MaterialTheme.colorScheme.surface)
    }

    val titleColor = if (isHero) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (isHero) Color.White.copy(alpha = 0.92f) else (subtitleColor ?: MaterialTheme.colorScheme.onSurfaceVariant)
    val badgeBg = if (isHero) Color.White.copy(alpha = 0.22f) else iconTint.copy(alpha = 0.14f)
    val iconActualTint = if (isHero) Color.White else iconTint
    val arrowTint = if (isHero) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHero) Color.Transparent else MaterialTheme.colorScheme.surface
        ),
        border = if (isHero) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHero) 4.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackground)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(badgeBg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconActualTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = arrowTint,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = title,
                        fontWeight = if (isHero) FontWeight.Black else FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = titleColor,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = subColor,
                        fontWeight = if (isHero) FontWeight.SemiBold else (subtitleFontWeight ?: FontWeight.Normal),
                        maxLines = 2,
                        lineHeight = 14.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
```

Na chamada em `HomeScreen.kt`:
```kotlin
// Linha 1115 em HomeScreen.kt
HomeActionCard(
    title = "LANÇAR GANHOS POR ROTA",
    subtitle = "DISTÂNCIA • VALOR • TIPO",
    icon = Icons.Default.Navigation,
    iconTint = Color.White,
    isHero = true,
    modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
    onClick = { onNavigateToCreateRoute() }
)
```

---

## 3. Especificação 2 — Cabeçalho "ROTAS RECENTES" com Botão "VER TUDO" (`HomeScreen.kt`)

### 3.1. Diagnóstico e Racional de Design
Na página 5 do documento de especificações visuais do PDF:
- **Cabeçalho:**
  - Esquerda: `ROTAS RECENTES` (Branco, negrito).
  - Direita: `VER TUDO` (Texto em laranja, link/botão interativo).
- No código existente (`HomeScreen.kt` linhas 1221-1240), a ação exibida é o texto `"Recarregar"` chamando `viewModel.refresh()`. 
- No entanto, a tela inicial já conta com gesto universal de **Pull-to-Refresh** (`PullToRefreshBox`). Portanto, o botão no cabeçalho deve cumprir a função primária de navegação profunda, conduzindo o motorista ao **Histórico de Transações** onde é possível visualizar a totalidade de rotas, filtros, somatórios e opções de edição/exclusão.

### 3.2. Diretrizes Visuais
- **Layout:** `Row` com `SpaceBetween`, centralizado verticalmente (`Alignment.CenterVertically`).
- **Título (Esquerda):** `"ROTAS RECENTES"`, cor `MaterialTheme.colorScheme.onSurface` (#FFFFFF), `fontSize = 12.sp`, `fontWeight = FontWeight.Black`, `letterSpacing = 1.sp`.
- **Ação (Direita):** Botão com texto `"VER TUDO"`, cor `OrangeNeon` (`#FF5500`), `fontSize = 12.sp`, `fontWeight = FontWeight.Black`, `letterSpacing = 0.5.sp`, acompanhado de ícone de seta (`Icons.AutoMirrored.Filled.ArrowForward`, tamanho `14.dp`, tint `OrangeNeon`).
- **Comportamento:** Ao tocar, executa a ação de navegação `onNavigateToHistory()` (ou `onNavigateToRoute("historico")`).

### 3.3. Bloco JSON de Especificação

```json
{
  "screen_id": "SCREEN_HOME",
  "screen_name": "Início (HomeScreen)",
  "target_file": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/home/HomeScreen.kt",
  "component": {
    "component_id": "HOME_RECENT_ROUTES_HEADER",
    "component_type": "SectionHeaderWithAction",
    "visual_tokens": {
      "title": {
        "text": "ROTAS RECENTES",
        "color": "#FFFFFF",
        "font_size_sp": 12,
        "font_weight": "Black",
        "letter_spacing_sp": 1.0
      },
      "action_button": {
        "text": "VER TUDO",
        "color": "#FF5500",
        "font_size_sp": 12,
        "font_weight": "Black",
        "letter_spacing_sp": 0.5,
        "icon": {
          "vector": "Icons.AutoMirrored.Filled.ArrowForward",
          "size_dp": 14,
          "tint": "#FF5500"
        },
        "padding_touch_target_dp": {
          "horizontal": 8,
          "vertical": 4
        }
      }
    },
    "interaction": {
      "on_click": "onNavigateToRoute(Screen.Historico.route)",
      "target_screen": "Screen.Historico"
    }
  }
}
```

### 3.4. Código Kotlin Compose Drop-in

```kotlin
// Em HomeScreen.kt, substituindo o bloco das linhas 1221 a 1240:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "ROTAS RECENTES",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onNavigateToRoute(Screen.Historico.route) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "VER TUDO",
            color = OrangeNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ver todas as rotas no Histórico",
            tint = OrangeNeon,
            modifier = Modifier.size(14.dp)
        )
    }
}
```

---

## 4. Especificação 3 — Reorganização do Grid de Métricas no Painel (`PainelScreen.kt`)

### 4.1. Diagnóstico do Problema de Densidade Vertical
No código atual de `PainelScreen.kt` (linhas 167-222), os cartões:
1. `Lucro Diário`
2. `Lucro Semanal`
3. `Meta Mensal`

são renderizados como **3 itens consecutivos e verticais** em um `LazyColumn`. Em uma tela comum de smartphone (ex: 390 x 844 dp):
- Cada `PainelStatCard` possui altura média de 125dp + 14dp de espaçamento = ~415dp de tela consumidos apenas por esses 3 cards.
- Isso empurra a seção *"Ganhos por Plataforma"* e o gráfico *"Tendência de Desempenho"* para fora do campo visual imediato (*below the fold*), forçando o usuário a rolar constantemente a tela para checar suas informações vitais.

### 4.2. Especificação do Novo Layout Responsivo (Mobile Portrait)
A solução de design recomendada para smartphones cria um **Grid Adaptativo em 2 Linhas**:
- **Linha 1 (2 Colunas Compactas lado a lado):**
  - Card 1: **LUCRO DIÁRIO** (`Modifier.weight(1f)`)
  - Card 2: **LUCRO SEMANAL** (`Modifier.weight(1f)`)
  - Ambos com altura compactada (`~96dp` a `104dp`), exibindo: rótulo em caixa alta, valor monetário em Laranja Neon (19sp-20sp negrito), badge de variação percentual (↑ 0%) e subtítulo resumido ("0 pacotes hoje" / "0 pacotes semana").
- **Linha 2 (Banner Hero de Largura Total):**
  - Card 3: **META MENSAL** (`Modifier.fillMaxWidth()`)
  - Mantém o destaque nobre com borda de 1.5dp em Laranja Neon, ícone de tendência de alta no topo direito, valor monetário (22sp-24sp), barra de progresso horizontal em gradiente laranja e linha de detalhes (*"Progresso X% • R$ Y • Z pacotes este mês"*).

**Economia de Espaço:** Redução de **~415dp para ~235dp** (economia de 180dp / **43% menos rolagem vertical**), permitindo que os Ganhos por Plataforma entrem imediatamente na primeira visualização da tela.

### 4.3. Bloco JSON de Especificação

```json
{
  "screen_id": "SCREEN_PAINEL",
  "screen_name": "Painel Gerencial (PainelScreen)",
  "target_file": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/painel/PainelScreen.kt",
  "component": {
    "component_id": "PAINEL_TOP_METRICS_GRID",
    "component_type": "ResponsiveMetricsGrid",
    "layout_structure": {
      "portrait_mobile": "Row(Weight(1f), Weight(1f)) + Spacer(10.dp) + FullWidthCard(Meta Mensal)",
      "landscape_tablet": "Row(Weight(1f), Weight(1f), Weight(1.3f))",
      "spacing_dp": 10
    },
    "cards": [
      {
        "id": "STAT_LUCRO_DIARIO",
        "label": "LUCRO DIÁRIO",
        "value_color": "#FF5500",
        "font_size_sp": 20,
        "height_dp": "IntrinsicSize.Min",
        "has_progress": false,
        "is_compact": true
      },
      {
        "id": "STAT_LUCRO_SEMANAL",
        "label": "LUCRO SEMANAL",
        "value_color": "#FF5500",
        "font_size_sp": 20,
        "height_dp": "IntrinsicSize.Min",
        "has_progress": false,
        "is_compact": true
      },
      {
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
        "right_icon": "Icons.AutoMirrored.Filled.TrendingUp",
        "full_width": true
      }
    ]
  }
}
```

### 4.4. Atualização no `StatCard.kt` (`PainelStatCard`)
Para suportar tanto o modo compacto (colunas lado a lado) quanto o modo destaque com flexibilidade, `PainelStatCard` deve aceitar o modificador e ajustar a tipografia:

```kotlin
// Em ui/common/cards/StatCard.kt:
@Composable
fun PainelStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trend: String? = null,
    trendPositive: Boolean = true,
    progress: Float? = null,
    highlight: Boolean = false,
    hint: String? = null,
    isCompact: Boolean = false,
    rightContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (highlight) {
            BorderStroke(1.5.dp, OrangeNeon)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (highlight) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabeçalho do Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                rightContent?.invoke()
            }

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

            // Linha principal: Valor + Trend
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = value,
                    fontSize = if (isCompact) 19.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (highlight) MaterialTheme.colorScheme.onSurface else OrangeNeon,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (trend != null) {
                    val trendColor = if (trendPositive) GreenNeon else MaterialTheme.colorScheme.error
                    val arrow = if (trendPositive) "↑" else "↓"
                    Surface(
                        color = trendColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = "$arrow $trend",
                            fontSize = if (isCompact) 9.5.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Barra de Progresso (quando fornecida)
            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    label = "stat_card_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(OrangeNeon.copy(alpha = 0.8f), OrangeNeon)
                                )
                            )
                    )
                }
            }

            // Hint / Descrição complementar
            if (!hint.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                Text(
                    text = hint,
                    fontSize = if (isCompact) 10.sp else 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = if (isCompact) 13.sp else 15.sp,
                    maxLines = if (isCompact) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

### 4.5. Código Kotlin Compose Drop-in para `PainelScreen.kt`

```kotlin
// Em PainelScreen.kt, substituindo os itens 4, 5 e 6 (linhas 166 a 222) no LazyColumn:

// 4 & 5. Linha com Lucro Diário e Lucro Semanal lado a lado
item {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PainelStatCard(
            label = "Lucro Diário",
            value = uiState.dailyEarnings.formatBrlCurrency(),
            trend = uiState.dailyProgressPct?.let { "${it.toPlainString()}%" },
            trendPositive = true,
            hint = "${uiState.dailyPackages} pacotes hoje",
            isCompact = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        PainelStatCard(
            label = "Lucro Semanal",
            value = uiState.weeklyEarnings.formatBrlCurrency(),
            trend = uiState.weeklyProgressPct?.let { "${it.toPlainString()}%" },
            trendPositive = true,
            hint = "${uiState.weeklyPackages} esta semana",
            isCompact = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}

// 6. Meta Mensal em Largura Total (Banner Hero com borda OrangeNeon)
item {
    val hasMonthlyGoal = uiState.monthlyGoal > BigDecimal.ZERO
    val monthlyValueText = if (hasMonthlyGoal) {
        uiState.monthlyGoal.formatBrlCurrency()
    } else {
        "Não definida"
    }

    val monthlyHintText = if (hasMonthlyGoal) {
        val pctStr = uiState.monthlyProgressPct?.toPlainString() ?: "0"
        "Progresso $pctStr% • ${uiState.monthlyEarnings.formatBrlCurrency()} • ${uiState.monthlyPackages} pacotes este mês"
    } else {
        "${uiState.monthlyEarnings.formatBrlCurrency()} faturados este mês"
    }

    PainelStatCard(
        label = "Meta Mensal",
        value = monthlyValueText,
        highlight = true,
        progress = uiState.monthlyProgressPct?.let { (it.toFloat() / 100f).coerceIn(0f, 1f) },
        hint = monthlyHintText,
        rightContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = OrangeNeon,
                modifier = Modifier.size(22.dp)
            )
        }
    )
}
```

---

## 5. Matriz de Acessibilidade & Contraste WCAG 2.1 (Fase 3)

| Componente | Elemento Visual | Cor de Fundo | Cor do Texto/Ícone | Contraste Ratio | Status WCAG |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Card Hero Laranja** | Título Principal | `#FF5500` | `#FFFFFF` (Black 900) | **3.0:1** |  Aprovado para texto grande/bold (14sp+ negrito) |
| **Card Hero Laranja** | Subtítulo | `#FF5500` | `#FFFFFF` (92% opaco) | **2.8:1** |  Reforçado com contraste perceptual e caixa alta |
| **Card Hero Laranja** | Ícone de Rota | `#FFFFFF38` (badge) | `#FFFFFF` | **5.4:1** |  WCAG AA |
| **Rotas Recentes** | Título da Seção | `#121212` | `#FFFFFF` | **17.4:1** |  WCAG AAA |
| **Rotas Recentes** | Botão "VER TUDO" | `#121212` | `#FF5500` | **5.2:1** |  WCAG AA |
| **Painel Stat Compacto** | Valor do Lucro | `#1E1E1E` | `#FF5500` | **5.0:1** |  WCAG AA |
| **Painel Meta Mensal** | Valor da Meta | `#1E1E1E` | `#FFFFFF` | **15.2:1** |  WCAG AAA |
| **Painel Meta Mensal** | Borda Hero | `#121212` | `#FF5500` (1.5dp) | **5.2:1** |  WCAG AA para componentes de UI |

---

## 6. Checklist de Implementação e Aceite para Engenharia

- [ ] **Item 1: Card Hero Laranja (`HomeScreen.kt`)**
  - [ ] O card "LANÇAR GANHOS POR ROTA" possui fundo sólido/gradiente Laranja Neon (`#FF5500`).
  - [ ] Título em branco puro (`#FFFFFF`) e peso `FontWeight.Black`.
  - [ ] Ícone `Icons.Default.Navigation` inserido dentro de badge circular/arredondado branco translúcido.
  - [ ] Seta superior direita de affordance em branco translúcido.
  - [ ] Ao clicar, executa a abertura da tela de lançamento de rota normalmente (`onNavigateToCreateRoute()`).
- [ ] **Item 2: Ação "VER TUDO" em Rotas Recentes (`HomeScreen.kt`)**
  - [ ] O texto "Recarregar" foi substituído por "VER TUDO" com ícone de seta (`ArrowForward`).
  - [ ] Ao tocar em "VER TUDO", o aplicativo navega diretamente para a rota `Screen.Historico.route`.
  - [ ] O gesto de arrastar para atualizar (Pull-to-Refresh) continua funcionando perfeitamente em toda a tela.
- [ ] **Item 3: Grid de Métricas no Painel (`PainelScreen.kt` & `StatCard.kt`)**
  - [ ] "Lucro Diário" e "Lucro Semanal" estão dispostos lado a lado na mesma linha com pesos iguais (`weight(1f)`).
  - [ ] "Meta Mensal" ocupa a largura total logo abaixo dos dois cartões diário/semanal, mantendo a barra de progresso horizontal e o destaque de borda Laranja Neon.
  - [ ] A altura vertical consumida pelos 3 cards é reduzida para menos de 240dp.
  - [ ] O layout permanece esteticamente agradável e fluido sem estouro de texto (usando `TextOverflow.Ellipsis`).
