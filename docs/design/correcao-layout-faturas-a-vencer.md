# Especificação Técnica de Correção de UI/UX — Contas a Receber (Faturas)

**Documento:** Análise e Especificação de Correção de Layout da Tela "Contas a Receber" — Aba "Ciclo a Vencer" (Alinhamento e Padronização de Botões e Textos)  
**Código da Tarefa:** TASK-DES-03  
**Agente Responsável:** Agente Designer — Time Pocket (Central do Motorista)  
**Arquivo Alvo:** [`app/src/main/java/com/fernando/centraldomotorista/ui/screens/faturas/FaturasScreen.kt`](file:///d:/Dev/logistic-prime/logistic-prime/app/src/main/java/com/fernando/centraldomotorista/ui/screens/faturas/FaturasScreen.kt)  
**Componente Alvo:** `FaturaCardItem` (Linhas 792 a 862 e seções correlatas)  
**Data:** 25 de Setembro de 2026  
**Status:** Pronto para Implementação (Ready for Dev)  

---

## 1. Diagnóstico do Problema e Causa Raiz

### 1.1. O Problema Reportado
Na tela **"Contas a Receber"** (`FaturasScreen.kt`), especialmente na aba **"Ciclo a Vencer"** (`FaturasTab.A_VENCER`), os cartões de fatura (`FaturaCardItem`) apresentam inconsistências visuais críticas na barra de ações:
1. Os 3 botões secundários (**"Detalhes"**, **"Editar"** e **"Ajustes"**) apresentam **alturas variáveis e assimétricas entre si** no mesmo cartão.
2. Os textos internos dos botões sofrem **quebra de linha involuntária** (ex: *"De-talhes"* ou *"Ajus-tes"* ocupando 2 linhas, enquanto *"Editar"* ocupa 1 linha), gerando um aspecto visual truncado, desalinhado e anti-profissional.
3. O botão de exclusão (`IconButton` com ícone de lixeira) possui tamanho arbitrário (`38.dp`), não possui borda ou outline e fica **completamente flutuando e desalinhado** em relação aos outros três botões da linha.
4. O botão primário *"Liquidar / Baixar Repasse"* possui alturas divergentes entre abas (48dp em `isAVencer` vs 46dp em `isEmAberto`).

---

### 1.2. Análise Técnica da Causa Raiz no Jetpack Compose

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANÁLISE DE LARGURA DO CARD                      │
├────────────────────────────────────────────────────────────────────────┤
│ Largura do viewport típico mobile: 360.dp                              │
│ Padding horizontal da tela: 16.dp + 16.dp = 32.dp                      │
│ Largura total do Card: 328.dp                                          │
│ Padding interno do Card: 16.dp + 16.dp = 32.dp                         │
│ Largura útil para a linha de botões: 296.dp                            │
├────────────────────────────────────────────────────────────────────────┤
│ Estrutura atual:                                                       │
│ [ OutlinedButton (1f) ] [ OutlinedButton (1f) ] [ OutlinedButton (1f) ] [ IconButton 38dp ]
│ Espaçamento: 3 gaps de 8.dp = 24.dp                                    │
│ Largura restante para os 3 botões: 296.dp - 38.dp - 24.dp = 234.dp     │
│ Largura alocada para cada OutlinedButton: ~78.dp                        │
├────────────────────────────────────────────────────────────────────────┤
│ O GARGALO CRÍTICO DO MATERIAL 3:                                       │
│ O OutlinedButton do Material 3 usa por padrão:                         │
│ contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)    │
│                                                                        │
│ Espaço consumido apenas pelo padding lateral: 24.dp + 24.dp = 48.dp!   │
│ Espaço restante para o conteúdo interno: 78.dp - 48.dp = 30.dp!        │
│                                                                        │
│ Dentro de 30.dp precisam caber:                                        │
│ [Ícone 14dp] + [Spacer 4dp] + [Texto 11sp negrito ("Detalhes" = ~44dp)]│
│ 14 + 4 + 44 = 62.dp necessários  >  30.dp disponíveis!                │
└────────────────────────────────────────────────────────────────────────┘
```

**Consequências:**
1. Como faltam mais de 30dp de espaço interno, o texto do botão é **forçado a quebrar em duas linhas**.
2. Ao quebrar a linha, o `OutlinedButton` **expande verticalmente de forma automática** para acomodar o texto duplo (~52dp a 56dp).
3. Palavras mais curtas como *"Editar"* (6 letras) podem conseguir caber em 1 linha em algumas densidades de tela, mantendo altura menor (~40dp), enquanto *"Detalhes"* e *"Ajustes"* pulam para 2 linhas (~54dp).
4. Como nenhum botão possui `.height()` fixo, **a linha inteira fica em "dentes de serra"**, destruindo o alinhamento visual.
5. O `IconButton` de exclusão, com `.size(38.dp)` fixo sem outline, fica perdido ao lado de botões de 54dp de altura.

---

## 2. Matriz Comparativa: Estado Atual vs. Estado Corrigido

| Propriedade Visual | Estado Atual (Buggy) | Estado Corrigido (Projetado) | Impacto de UX |
| :--- | :--- | :--- | :--- |
| **Altura da Linha Secundária** | Variável / Automática (40dp a 56dp) | **Fixa e Uniforme em 38.dp** para todos |  Alinhamento horizontal linear e perfeito |
| **ContentPadding dos Botões** | Padrão M3 (24dp horizontal) | **Compacto: `PaddingValues(horizontal = 4.dp, vertical = 0.dp)`** |  Libera 40dp de largura interna útil por botão |
| **Quebra de Texto** | Múltiplas linhas involuntárias | **`maxLines = 1, overflow = TextOverflow.Ellipsis`** |  Zero quebra de linha em qualquer dispositivo |
| **Espaçamento entre Ícone e Texto** | `Spacer(width = 4.dp)` | **`Spacer(width = 3.dp)`** |  Mais 2dp preservados para o rótulo de texto |
| **Botão de Exclusão (Lixeira)** | `IconButton(size = 38.dp)` sem borda | **`OutlinedButton` compacto (38x38dp) com shape 10dp e borda sutil** |  Harmonização total com os botões adjacentes |
| **Botão Primário (Liquidar)** | 48dp em A Vencer / 46dp em Aberto | **Padronizado em 46.dp**, `RoundedCornerShape(12.dp)` |  Identidade visual estável e ergonômica |
| **Espaçamento entre Linhas** | Espaçamento default inconsistente | **`Spacer(height = 8.dp)`** entre primário e secundários |  Hierarquia visual clara e espaçamento simétrico |

---

## 3. Especificações Técnicas de Layout

### 3.1. Linha Secundária de Ações (Ajustes, Detalhes, Editar, Excluir)
- **Container:** `Row` com `Modifier.fillMaxWidth().height(38.dp)`, `horizontalArrangement = Arrangement.spacedBy(6.dp)` e `verticalAlignment = Alignment.CenterVertically`.
- **Botões com Rótulo ("Detalhes", "Editar", "Ajustes"):**
  - Componente: `OutlinedButton`
  - Modificador: `Modifier.weight(1f).height(38.dp)`
  - Shape: `RoundedCornerShape(10.dp)`
  - Borda: `BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))`
  - ContentPadding: `PaddingValues(horizontal = 4.dp, vertical = 0.dp)`
  - Ícone: Tamanho `14.dp`, tint semântico (Cinza suave para Detalhes, `OrangeNeon` para Editar, `BlueInfo` para Ajustes).
  - Espaçamento interno: `Spacer(modifier = Modifier.width(3.dp))`
  - Texto: `fontSize = 11.sp`, `fontWeight = FontWeight.Bold`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`.
- **Botão de Exclusão (Lixeira):**
  - Componente: `Surface` interativo ou `OutlinedButton` quadrado.
  - Modificador: `Modifier.size(38.dp)` (altura e largura exatamente 38.dp).
  - Shape: `RoundedCornerShape(10.dp)` (idêntico aos botões adjacentes).
  - Borda: `BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f))`.
  - Cor de Fundo: `RedAlert.copy(alpha = 0.08f)` ou transparente.
  - Ícone: `Icons.Default.Delete`, tamanho `16.dp`, tint `RedAlert`.

---

### 3.2. Botão Primário ("Liquidar / Baixar Repasse")
- **Componente:** `Button` (Material 3).
- **Modificador:** `Modifier.fillMaxWidth().height(46.dp)`.
- **Shape:** `RoundedCornerShape(12.dp)`.
- **Cores:**
  - Aba *"Ciclo a Vencer"*: `containerColor = GreenNeon` (#00A152), `contentColor = Color.Black`.
  - Aba *"Em Aberto"*: `containerColor = OrangeNeon` (#FF5500), `contentColor = Color.Black`.
- **Ícone:** `Icons.Default.PriceCheck`, tamanho `18.dp`, `tint = Color.Black`.
- **Tipografia:** `fontSize = 13.sp`, `fontWeight = FontWeight.Black` ou `Bold`, `color = Color.Black`, centralizado.

---

## 4. Blocos JSON de Especificação (Formato Padrão do Designer)

### 4.1. Bloco JSON — Linha Secundária de Ações

```json
{
  "screen_id": "SCREEN_FATURAS",
  "screen_name": "Contas a Receber (FaturasScreen)",
  "tab_context": "FaturasTab.A_VENCER",
  "component": {
    "component_id": "FATURA_CARD_SECONDARY_ACTIONS_ROW",
    "component_type": "ActionsToolbar",
    "layout_properties": {
      "container_type": "Row",
      "width": "fillMaxWidth",
      "height_dp": 38,
      "spacing_between_items_dp": 6,
      "vertical_alignment": "CenterVertically"
    },
    "children_buttons": [
      {
        "button_id": "BTN_DETALHES",
        "type": "OutlinedButton",
        "weight": 1.0,
        "height_dp": 38,
        "shape": "RoundedCornerShape(10.dp)",
        "border": {
          "width_dp": 1,
          "color_token": "MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)"
        },
        "content_padding_dp": {
          "horizontal": 4,
          "vertical": 0
        },
        "icon": {
          "vector": "Icons.Default.Visibility",
          "size_dp": 14,
          "tint_token": "MaterialTheme.colorScheme.onSurfaceVariant"
        },
        "spacer_dp": 3,
        "label": {
          "text": "Detalhes",
          "font_size_sp": 11,
          "font_weight": "Bold",
          "max_lines": 1,
          "overflow": "Ellipsis",
          "color_token": "MaterialTheme.colorScheme.onSurface"
        }
      },
      {
        "button_id": "BTN_EDITAR",
        "type": "OutlinedButton",
        "weight": 1.0,
        "height_dp": 38,
        "shape": "RoundedCornerShape(10.dp)",
        "border": {
          "width_dp": 1,
          "color_token": "MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)"
        },
        "content_padding_dp": {
          "horizontal": 4,
          "vertical": 0
        },
        "icon": {
          "vector": "Icons.Default.Edit",
          "size_dp": 14,
          "tint_token": "OrangeNeon"
        },
        "spacer_dp": 3,
        "label": {
          "text": "Editar",
          "font_size_sp": 11,
          "font_weight": "Bold",
          "max_lines": 1,
          "overflow": "Ellipsis",
          "color_token": "MaterialTheme.colorScheme.onSurface"
        }
      },
      {
        "button_id": "BTN_AJUSTES",
        "type": "OutlinedButton",
        "weight": 1.0,
        "height_dp": 38,
        "shape": "RoundedCornerShape(10.dp)",
        "border": {
          "width_dp": 1,
          "color_token": "MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)"
        },
        "content_padding_dp": {
          "horizontal": 4,
          "vertical": 0
        },
        "icon": {
          "vector": "Icons.Default.Tune",
          "size_dp": 14,
          "tint_token": "BlueInfo"
        },
        "spacer_dp": 3,
        "label": {
          "text": "Ajustes",
          "font_size_sp": 11,
          "font_weight": "Bold",
          "max_lines": 1,
          "overflow": "Ellipsis",
          "color_token": "MaterialTheme.colorScheme.onSurface"
        }
      },
      {
        "button_id": "BTN_EXCLUIR",
        "type": "OutlinedIconButtonContainer",
        "width_dp": 38,
        "height_dp": 38,
        "shape": "RoundedCornerShape(10.dp)",
        "background_color_token": "RedAlert.copy(alpha = 0.08f)",
        "border": {
          "width_dp": 1,
          "color_token": "RedAlert.copy(alpha = 0.35f)"
        },
        "icon": {
          "vector": "Icons.Default.Delete",
          "size_dp": 16,
          "tint_token": "RedAlert"
        },
        "accessibility_description": "Excluir fatura"
      }
    ]
  }
}
```

---

### 4.2. Bloco JSON — Botão Primário de Liquidação

```json
{
  "screen_id": "SCREEN_FATURAS",
  "screen_name": "Contas a Receber (FaturasScreen)",
  "component": {
    "component_id": "FATURA_PRIMARY_ACTION_BUTTON",
    "component_type": "PrimaryButton",
    "layout_properties": {
      "width": "fillMaxWidth",
      "height_dp": 46,
      "shape": "RoundedCornerShape(12.dp)",
      "margin_bottom_dp": 8
    },
    "visual_tokens": {
      "tab_a_vencer": {
        "container_color": "#00A152",
        "content_color": "#000000",
        "label_text": "Liquidar / Baixar Repasse"
      },
      "tab_em_aberto": {
        "container_color": "#FF5500",
        "content_color": "#000000",
        "label_text": "Liquidar / Baixar Ciclo"
      }
    },
    "icon": {
      "vector": "Icons.Default.PriceCheck",
      "size_dp": 18,
      "tint": "#000000",
      "spacer_end_dp": 8
    },
    "typography": {
      "font_size_sp": 13,
      "font_weight": "Bold",
      "color": "#000000"
    }
  }
}
```

---

## 5. Código Kotlin Compose Drop-in

### 5.1. Código Refatorado para `FaturasScreen.kt` (Linhas 792 a 862)

Substituir o bloco de ações em `FaturaCardItem` pelo código padronizado abaixo:

```kotlin
            // Linha 4: Botões de Ação Estruturados e Padronizados (Design System)
            if (isAVencer) {
                // Aba "A Vencer": Botão primário full-width [ Liquidar / Baixar Repasse ] em GreenNeon
                Button(
                    onClick = onPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Liquidar / Baixar Repasse",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linha secundária de ações com altura fixa de 38.dp e padding compacto
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Detalhes
                    OutlinedButton(
                        onClick = onViewDetails,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Detalhes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Editar
                    OutlinedButton(
                        onClick = onEditItems,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = OrangeNeon
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Editar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Ajustes
                    OutlinedButton(
                        onClick = onAdjustments,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = BlueInfo
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Ajustes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 4. Excluir (OutlinedIconButton Harmonizado)
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showDeleteConfirm = true },
                        shape = RoundedCornerShape(10.dp),
                        color = RedAlert.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir fatura",
                                tint = RedAlert,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
```

> [!TIP]
> **Recomendação de Reutilização de Código:**  
> A mesma `Row` secundária padronizada deve ser aplicada nos blocos de `isEmAberto` (linhas 881-931) e `else` (linhas 934-984) de `FaturaCardItem`, garantindo que **todas as 3 abas** compartilhem a mesma harmonia visual e zero quebras de layout.

---

## 6. Prova Matemática de Responsividade por Resolução

Abaixo está o teste de estresse da largura útil para os botões secundários em diferentes larguras de smartphones Android:

| Dispositivo / Largura da Tela | Largura do Card | Largura Útil do Container | Botão Excluir (38dp) + Gaps (18dp) | Largura Útil por Botão de Texto | Espaço Restante para Texto (após ícone e spacer) | Status Visual |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Android Compacto (320 dp)** | 288 dp | 256 dp | 56 dp | **66.6 dp** | **42.6 dp** |  Cabe perfeitamente com 11sp |
| **Padrão Android (360 dp)** | 328 dp | 296 dp | 56 dp | **80.0 dp** | **56.0 dp** |  Folga confortável sem truncar |
| **Intermediário (390 dp)** | 358 dp | 326 dp | 56 dp | **90.0 dp** | **66.0 dp** |  Layout espaçoso e folgado |
| **Flagship / Plus (412 dp)** | 380 dp | 348 dp | 56 dp | **97.3 dp** | **73.3 dp** |  Excelente respiro |

---

## 7. Checklist de Aceite para Engenharia

- [ ] **Uniformidade de Altura:** Todos os 4 botões secundários possuem exatamente `38.dp` de altura na tela.
- [ ] **Zero Quebras de Linha:** Nenhum dos textos ("Detalhes", "Editar", "Ajustes") salta para uma segunda linha em telas compactas (360dp ou 320dp).
- [ ] **Alinhamento do Botão de Excluir:** O botão da lixeira possui `38x38 dp`, cantos de `10.dp`, borda de `1.dp` em tom avermelhado sutil e fica perfeitamente alinhado vertical e horizontalmente na mesma linha dos demais botões.
- [ ] **Botão Primário:** Altura fixada em `46.dp`, com cantos arredondados de `12.dp` e ícone `PriceCheck` em preto.
- [ ] **Consistência Cross-Tab:** O comportamento visual foi replicado igualmente nas abas *"Ciclo a Vencer"*, *"Em Aberto"* e *"Pago"*.
