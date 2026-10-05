# Especificação Técnica de UI/UX — Releitura OCR, Combobox de Cargas e Parceiros, Cards com Drag & Drop e Hierarquia Tipográfica

**Documento:** Especificação de Interface, Ergonomia e Interação — Releitura Direcionada de OCR, Combobox M3 de Cargas e Atribuição, Card Contraído com Endereço em Maiúsculas na Linha 1 e Reordenação Gestual por Drag Handle  
**Código da Tarefa:** TASK-DES-10 (Revisão v2 — Inversão Endereço / Cliente)  
**Agente Responsável:** Agente Designer UX/UI — Time Pocket (Central do Motorista)  
**Destinatário:** Subagente Android/Kotlin & Desenvolvedor Master (Fernando)  
**Data:** 04 de Outubro de 2026  
**Status:** Pronto para Implementação (Ready for Dev)  

---

## 1. Visão Geral e Contexto Operacional

Esta especificação aprimora a experiência de bipagem no galpão e o gerenciamento de paradas no Cockpit veicular, abordando 7 necessidades práticas levantadas em operações reais:

1. **Releitura OCR sem Perda do Código:** Permitir que o motorista refaça a leitura do texto da etiqueta sem descartar o código de barras já capturado.
2. **Combobox de Tipos de Carga:** Expandir o suporte para além de *Pacote* e *Volumoso*, suportando *Documento*, *Comida* e *Farmácia* via `ExposedDropdownMenuBox`.
3. **Combobox de Atribuição com Memória:** Selecionar entregadores parceiros em ordem alfabética ou manter o *Motorista Master (Você)*, com retenção do último parceiro selecionado para bipagem em lote.
4. **Reatribuição ao Master no Cockpit:** Permitir devolver pacotes transferidos de volta ao Master selecionando *Motorista Master (Você)* no topo do menu de transferência.
5. **Card Contraído com Prioridade Geográfica (Endereço na Linha 1 em Maiúsculas):** Ocultar o código de barras no estado contraído e exibir o **Endereço Completo em letras MAIÚSCULAS na Linha 1** (para identificação geográfica imediata da rua/local ao bater o olho) e o **Nome do(a) Cliente na Linha 2** (logo abaixo do endereço).
6. **Alça de Arrasto (Drag Handle) no Card:** Elemento visual Dark Neon para reordenação gestual manual da ordem das entregas no Cockpit.
7. **Preservação Estrutural:** Manutenção rigorosa do layout consolidado do `OcrConfirmationCard`, alterando estritamente os pontos solicitados.

---

## 2. Especificação 1 — Botão "Ler Etiqueta Novamente" no Bloco de OCR

### 2.1 Diagnóstico do Fluxo de Leitura
Na Etapa 2 do scanner (`OCR_CONFIRMATION`), o código de barras já foi lido com sucesso. Caso a etiqueta esteja amassada, mal iluminada ou o primeiro frame do OCR tenha capturado o texto parcialmente, o motorista **não deve ser obrigado a re-bipar o código de barras**.

### 2.2 Wireframe Textual — Bloco de OCR com Releitura
```
┌─────────────────────────────────────────────────────────────┐
│ 🏷️ CÓDIGO BIPADO: BR420918237BR             [🔄 Re-bipar]│
│ ─────────────────────────────────────────────────────────── │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 👤 CARLOS EDUARDO SILVA                                 │ │
│ │ 📍 Rua das Palmeiras, 120 - Apto 42                     │ │
│ │    Centro • CEP: 01001-000 • São Paulo / SP             │ │
│ │                                                         │ │
│ │ ┌─────────────────────────────────────────────────────┐ │ │
│ │ │ 📷 LER ETIQUETA NOVAMENTE (OCR)                     │ │ │ ◄── Botão Releitura
│ │ │ Altura 34dp • Borda sutil OrangeNeon                │ │ │     Mantém o Barcode!
│ │ └─────────────────────────────────────────────────────┘ │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Especificação Compose do Botão de Releitura

```kotlin
// Inserido dentro da Surface do bloco de OCR em OcrConfirmationCard
OutlinedButton(
    onClick = onRescanOcrOnly,
    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f)),
    colors = ButtonDefaults.outlinedButtonColors(
        containerColor = OrangeNeon.copy(alpha = 0.08f),
        contentColor = OrangeNeon
    ),
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
        .fillMaxWidth()
        .height(34.dp),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
) {
    Icon(
        imageVector = Icons.Default.DocumentScanner,
        contentDescription = "Ler Etiqueta Novamente",
        tint = OrangeNeon,
        modifier = Modifier.size(15.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = "LER ETIQUETA NOVAMENTE (OCR)",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = OrangeNeon,
        maxLines = 1,
        softWrap = false
    )
}
```

- **Comportamento no ViewModel:** Ao clicar, dispara `viewModel.rescanOcrOnly()`, que limpa `pendingParsedAddress`, reativa `isOcrScanning = true` e processa novos frames do `textRecognizer` sem alterar o `pendingBarcode`.

---

## 3. Especificação 2 — Combobox para Tipo de Carga (`ExposedDropdownMenuBox`)

### 3.1 Substituição dos Botões Rígidos
Substitui os dois botões anteriores (*Pacotinho* e *Volumoso*) por um seletor suspenso elegante que acomoda 5 modalidades de entrega com seus respectivos ícones e tokens de cor:

| Tipo de Carga | Chave / Enum | Ícone Material | Cor do Ícone | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| **Pacote** | `PACOTE` | `Icons.Default.Inventory2` | `OrangeNeon` | Encomenda padrão / caixa média |
| **Volumoso** | `VOLUMOSO` | `Icons.Default.FitnessCenter` | `YellowGold` | Pacotes pesados ou grandes dimensões |
| **Documento** | `DOCUMENTO` | `Icons.Default.Description` | `BlueInfo` | Envelopes, contratos, notificações |
| **Comida** | `COMIDA` | `Icons.Default.Fastfood` | `GreenNeon` | Refeições e entregas rápidas |
| **Farmácia** | `FARMACIA` | `Icons.Default.LocalPharmacy` | `RedAlert` | Remédios e itens de saúde |

### 3.2 Wireframe Textual — Combobox Tipo de Carga
```
┌─────────────────────────────────────────────────────────────┐
│ TIPO DE CARGA:                                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 📦 Pacote                                           [▼] │ │ ◄── Campo Fechado
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Menu Aberto (Ao tocar):                                     │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✓ 📦 Pacote (Padrão)                                    │ │
│ │   🏋️ Volumoso (Pesado)                                  │ │
│ │   📄 Documento (Envelope)                               │ │
│ │   🍔 Comida (Alimentação)                               │ │
│ │   💊 Farmácia (Medicamento)                             │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Especificação Compose do `ExposedDropdownMenuBox`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageTypeCombobox(
    selectedType: PackageType,
    onTypeSelected: (PackageType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tipo de Carga", fontSize = 11.sp) },
            leadingIcon = {
                Icon(
                    imageVector = selectedType.icon,
                    contentDescription = null,
                    tint = selectedType.accentColor,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeNeon,
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedContainerColor = SurfaceDarkAlt,
                unfocusedContainerColor = SurfaceDarkAlt,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            PackageType.entries.forEach { type ->
                val isSelected = type == selectedType
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = type.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) OrangeNeon else TextPrimaryDark
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = type.icon,
                            contentDescription = null,
                            tint = type.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                        }
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
```

---

## 4. Especificação 3 — Combobox "Atribuir a:" com Memória e Ordenação Alfabética

### 4.1 Racional Operacional
Quando o motorista está no galpão bipando uma gaiola com 15 pacotes repassados a um motoboy parceiro (ex: *"Carlos Silva"*), ele não quer ter que selecionar o parceiro 15 vezes seguidas. A combobox memoriza o último parceiro selecionado e já o sugere para as próximas bipagens.

### 4.2 Regras de Composição e Ordenação
1. **Opção Fixa Superior:** `👤 Motorista Master (Você)` (valor `null`).
2. **Parceiros Ordenados Alfabeticamente:** Lista de parceiros cadastrados ordenada por `fullName` (`partners.sortedBy { it.fullName }`).
3. **Memória de Seleção (Sticky Memory):** O `RouteScannerViewModel` preserva `lastSelectedPartnerId`. Ao bipar um novo pacote, ele inicializa com esse ID por padrão.

### 4.3 Wireframe Textual — Combobox de Atribuição
```
┌─────────────────────────────────────────────────────────────┐
│ ATRIBUIR ENTREGA A:                                         │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 👤 Motorista Master (Você)                          [▼] │ │ ◄── Campo Fechado
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Menu Aberto (Ordenado A-Z):                                 │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✓ 👤 Motorista Master (Você)                            │ │
│ ├─────────────────────────────────────────────────────────┤ │
│ │   🛵 Carlos Eduardo Silva (Moto)                        │ │
│ │   🚗 Marcos Oliveira Santos (Carro)                     │ │
│ │   🛵 Rafael Mendes Pereira (Moto)                       │ │
│ │   🛵 Tiago Barbosa (Moto)                               │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Especificação 4 — Reatribuição no Cockpit com Opção "Motorista Master (Você)"

### 5.1 O Problema no Menu do Cockpit
No `StopDeliveryCard` expandido, o menu suspenso de transferência listava apenas entregadores parceiros terceiros. Se um pacote havia sido atribuído por engano a um parceiro, não havia como trazê-lo de volta para o Master.

### 5.2 Estrutura do Menu de Transferência no Cockpit
```kotlin
// Em StopDeliveryCard.kt — Dropdown de Reatribuição
DropdownMenu(
    expanded = showPartnerMenu,
    onDismissRequest = { showPartnerMenu = false },
    modifier = Modifier.background(SurfaceDark)
) {
    // 1. PRIMEIRA OPÇÃO: Motorista Master (Você) para reaver o pacote
    DropdownMenuItem(
        text = {
            Text(
                text = "👤 Motorista Master (Você)",
                fontSize = 12.sp,
                fontWeight = if (!isTransferred) FontWeight.Bold else FontWeight.Normal,
                color = if (!isTransferred) OrangeNeon else TextPrimaryDark
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
        },
        onClick = {
            showPartnerMenu = false
            onAssignToPartner(null) // null = Retorna posse ao Master
        }
    )

    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

    // 2. Parceiros Ordenados Alfabeticamente
    val sortedPartners = partners.sortedBy { it.fullName }
    sortedPartners.forEach { partner ->
        val isCurrent = stop.assignedPartnerId == partner.id
        DropdownMenuItem(
            text = {
                Text(
                    text = "🛵 ${partner.fullName}",
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) YellowGold else TextPrimaryDark
                )
            },
            leadingIcon = {
                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = YellowGold, modifier = Modifier.size(16.dp))
            },
            onClick = {
                showPartnerMenu = false
                onAssignToPartner(partner.id)
            }
        )
    }
}
```

---

## 6. Especificação 5 — Card de Parada Contraído (`StopDeliveryCard`): Endereço na Linha 1 em Maiúsculas e Cliente na Linha 2

### 6.1 Nova Arquitetura de Informações do Card Contraído (Prioridade Geográfica)
No trânsito veicular e na execução da rota diária, a informação mais importante para o motorista bater o olho e se localizar instantaneamente é o **endereço de destino (rua, número e bairro)**. Por isso, a hierarquia textual contraída prioriza o logradouro geográfico com destaque visual máximo em caixa alta.

**Diretrizes Rigorosas:**
1. **Linha 1:** `#Número da Parada` em negrito (`OrangeNeon` se próxima parada, `TextPrimaryDark` se demais) + `ENDEREÇO COMPLETO EM MAIÚSCULAS` (`13.5.sp`, `FontWeight.Bold`, `TextPrimaryDark`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`).
2. **Linha 2 (Logo abaixo do endereço):** `Nome do(a) Cliente` (`11.5.sp`, `TextSecondaryDark`, `maxLines = 1`, `overflow = TextOverflow.Ellipsis`).
3. **Código de Barras:** **Oculto no estado contraído.** Aparece exclusivamente quando o card for expandido pelo usuário.
4. **Alça de Arrasto (Drag Handle):** Ícone no canto direito da linha superior para permitir reordenação manual gestual.

### 6.2 Wireframe Textual — Card Contraído vs Expandido

```
── ESTADO CONTRAÍDO (Prioridade Geográfica Imediata: Rua em Destaque) ────
┌────────────────────────────────────────────────────────────────────────┐
│ #14  RUA DAS PALMEIRAS, 120 - CENTRO, SÃO PAULO        [Entregue]  [⋮⋮]│ ◄── Linha 1: # + ENDEREÇO EM MAIÚSCULAS
│      Carlos Eduardo Silva                                              │ ◄── Linha 2: Nome do(a) cliente
└────────────────────────────────────────────────────────────────────────┘

── ESTADO EXPANDIDO (Revela Barcode, Marketplace e Ações) ───────────────
┌────────────────────────────────────────────────────────────────────────┐
│ #14  RUA DAS PALMEIRAS, 120 - CENTRO, SÃO PAULO        [Entregue]  [⤡] │
│      Carlos Eduardo Silva                                              │
│ ────────────────────────────────────────────────────────────────────── │
│ 📦 CÓDIGO DO PACOTE: BR420918237BR             🏬 MARKETPLACE: Shopee  │ ◄── Barcode aparece AQUI!
│ 🏋️ Carga: Volumoso                           CEP: 01001-000           │
│                                                                        │
│ ┌────────────────────────────────────────────────────────────────────┐ │
│ │ 🚗 NAVEGAR NO GPS (Waze / Google Maps)                             │ │
│ └────────────────────────────────────────────────────────────────────┘ │
│                                                                        │
│ [ ✅ Entregue ]           [ 👤 Ausente ]          [ ↩️ Devolver ]      │
│                                                                        │
│ [ ✏️ Editar Pacote ]                               [ 🗑️ Remover ]       │
└────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Snippet Compose do Card Contraído com Nova Hierarquia

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    // Bloco da Esquerda: Número + Endereço na Linha 1 + Nome na Linha 2
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = "#${stop.stopOrder}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isNext && isPending) OrangeNeon else TextPrimaryDark,
            modifier = Modifier.padding(top = 1.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Linha 1: ENDEREÇO COMPLETO EM MAIÚSCULAS (Identificação Geográfica Imediata)
            Text(
                text = stop.fullAddress.ifBlank { "ENDEREÇO NÃO IDENTIFICADO" }.uppercase(),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Linha 2: Nome do(a) Cliente abaixo do endereço
            val recipient = stop.recipientName?.ifBlank { null }
            Text(
                text = recipient ?: "Destinatário não informado",
                fontSize = 11.5.sp,
                color = TextSecondaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    // Bloco da Direita: Badge de Status + Drag Handle
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Status Badge
        StatusBadge(status = stop.status, isNext = isNext)

        // Alça de Arrasto (Drag Handle)
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Arrastar para reordenar",
            tint = TextSecondaryDark.copy(alpha = 0.6f),
            modifier = Modifier
                .size(22.dp)
                .clickable { /* Aciona reorder listener */ }
        )
    }
}
```

---

## 7. Especificação 6 — Alça de Arrasto (`DragHandle`) para Reordenação Manual

### 7.1 Visual e Interação no Dark Neon
- **Ícone:** `Icons.Default.DragHandle` (ou `Icons.Default.Reorder`) com 2 ou 3 traços horizontais paralelos.
- **Cor:** `TextSecondaryDark.copy(alpha = 0.5f)` em repouso; `OrangeNeon` quando pressionado.
- **Área de Toque:** `32.dp x 32.dp` para fácil captura tátil com uma das mãos.
- **Efeito Visual ao Arrastar:**
  - O card selecionado ganha elevação `8.dp`.
  - Contorno sutil em `OrangeNeon` (`BorderStroke(1.5.dp, OrangeNeon)`).
  - Feedback tátil com vibração curta (`HapticFeedbackType.LongPress`).

---

## 8. Mapeamento de Tokens Material 3

| Elemento Visual | Token Compose | Hexadecimal | Descrição de Uso |
| :--- | :--- | :--- | :--- |
| **Linha Laser / Botão Releitura** | `OrangeNeon` | `#FF5500` | Destaque do botão "Ler Etiqueta Novamente" |
| **Carga: Pacote** | `OrangeNeon` | `#FF5500` | Ícone `Inventory2` para encomendas padrão |
| **Carga: Volumoso** | `YellowGold` | `#B8860B` | Ícone `FitnessCenter` para pacotes pesados |
| **Carga: Documento** | `BlueInfo` | `#1565C0` | Ícone `Description` para envelopes |
| **Carga: Comida** | `GreenNeon` | `#00A152` | Ícone `Fastfood` para entregas de refeições |
| **Carga: Farmácia** | `RedAlert` | `#D32F2F` | Ícone `LocalPharmacy` para saúde/medicamentos |
| **Alça de Arrasto (Repouso)** | `TextSecondaryDark.copy(0.6f)` | `#99A0A0A0` | Traços neutros de drag handle |
| **Alça de Arrasto (Ativo)** | `OrangeNeon` | `#FF5500` | Card elevado ao ser arrastado |

---

## 9. Blocos JSON de Especificação (Contratos para Devs)

### 9.1 Bloco JSON: Tipos de Carga Mapeados
```json
{
  "component_id": "PACKAGE_TYPE_COMBOBOX",
  "screen_target": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteScannerScreen.kt",
  "package_types": [
    { "key": "PACOTE", "label": "Pacote", "icon": "Inventory2", "color": "#FF5500" },
    { "key": "VOLUMOSO", "label": "Volumoso", "icon": "FitnessCenter", "color": "#B8860B" },
    { "key": "DOCUMENTO", "label": "Documento", "icon": "Description", "color": "#1565C0" },
    { "key": "COMIDA", "label": "Comida", "icon": "Fastfood", "color": "#00A152" },
    { "key": "FARMACIA", "label": "Farmácia", "icon": "LocalPharmacy", "color": "#D32F2F" }
  ]
}
```

### 9.2 Bloco JSON: Card Contraído Hierárquico
```json
{
  "component_id": "STOP_CARD_COLLAPSED_HIERARCHY",
  "screen_target": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/StopDeliveryCard.kt",
  "rules": {
    "line_1": "#order + FULL_ADDRESS_UPPERCASE",
    "line_2": "recipient_name",
    "barcode_visible_collapsed": false,
    "barcode_visible_expanded": true,
    "drag_handle_visible": true
  }
}
```

---

## 10. Checklist de Implementação para o Agente Android/Kotlin

- [ ] **Em `MasterRouteModels.kt`:**
  - Atualizar o enum `PackageType` com os 5 valores: `PACOTE`, `VOLUMOSO`, `DOCUMENTO`, `COMIDA`, `FARMACIA`.
- [ ] **Em `RouteScannerScreen.kt`:**
  - Inserir o botão *"LER ETIQUETA NOVAMENTE (OCR)"* no bloco de OCR do `OcrConfirmationCard`.
  - Substituir os botões rígidos de tipo de pacote pela combobox `PackageTypeCombobox`.
  - Substituir o seletor de parceiro pela combobox ordenada com memória do último selecionado.
- [ ] **Em `StopDeliveryCard.kt`:**
  - No estado contraído: **Linha 1 com endereço completo em maiúsculas**, **Linha 2 com nome do cliente abaixo do endereço** e **ocultar barcode**.
  - No estado expandido: exibir barcode e marketplace com destaque.
  - Inserir ícone `DragHandle` no canto superior direito.
  - No menu de reatribuição: incluir `👤 Motorista Master (Você)` como primeira opção com ação `onAssignToPartner(null)`.
- [ ] **Em `RouteCockpitScreen.kt`:**
  - Habilitar suporte a reordenação gestual na lista de paradas com atualização instantânea de ordem no banco.
