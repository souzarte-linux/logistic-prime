# Especificação Técnica de UI/UX — Botão Editar Pacote, Scanner de Busca e Correções Ergonômicas

**Documento:** Especificações de Interface e Microinterações — Edição Rápida de Parada, Busca por Bipagem, Desmembramento da Barra de Bipagem, Seletor de Parceiros e Correção de Quebra de Linha  
**Código da Tarefa:** TASK-DES-09  
**Agente Responsável:** Agente Designer UX/UI — Time Pocket (Central do Motorista)  
**Destinatário:** Subagente Android/Kotlin & Desenvolvedor Master (Fernando)  
**Data:** 30 de Setembro de 2026  
**Status:** Pronto para Implementação (Ready for Dev)  

---

## 1. Visão Geral e Contexto Operacional

Durante os testes de campo com o Cockpit de Bordo e o Scanner Fracionado (TASK-DES-08), foram identificadas oportunidades de refinamento ergonômico essenciais para o fluxo de trabalho acelerado do motorista:

1. **Correção de Dados de Paradas em Rota:** O motorista precisa ajustar rapidamente um endereço lido incorretamente pelo OCR, corrigir o nome do destinatário ou alterar o tipo de pacote sem ter que reiniciar a rota ou recorrer a telas secundárias.
2. **Localização Instantânea de Pacote na Rota:** Ao procurar um pacote físico na caçamba/porta-malas entre 40+ encomendas, o motorista precisa bipar o código de barras com a câmera e ter o card correspondente filtrado e focado instantaneamente na tela, sem precisar digitar números longos.
3. **Ergonomia e Anti-Truncamento em Telas Compactas (360dp a 390dp):**
   - No Cockpit, a linha com *"X paradas listadas"*, *"Otimizar Rota"*, *"Expandir"* e *"Contrair"* sofria quebra de linha indesejada em telas estreitas, quebrando o layout visual.
   - No Scanner (`RouteScannerScreen`), a fusão da Plataforma Ativa e dos botões de Tipo de Pacote em uma única linha causava corte de texto no botão *"Trocar"*.
   - A atribuição de pacotes a parceiros cadastrados ficava restrita a alternar apenas entre o Master e o primeiro parceiro da lista, impedindo a seleção entre múltiplos parceiros.

---

## 2. Especificação 1 — Botão "Editar Pacote" no `StopDeliveryCard`

### 2.1 Diagnóstico e Posicionamento
Atualmente, o `StopDeliveryCard` expandido termina com o botão *"Remover Pacote da Rota"* em largura total. Para acomodar a edição cadastral sem aumentar verticalmente o card, os dois botões utilitários são organizados em uma **`Row` simétrica balanceada** (`weight(1f)` cada), com linguagem visual Dark Neon harmoniosa.

### 2.2 Wireframe Textual — Ações do Card Expandido
```
┌─────────────────────────────────────────────────────────────┐
│ #14 • CARLOS EDUARDO SILVA                    📦 BR42091823 │
│ 📍 Rua das Palmeiras, 120 - Centro • CEP 01001-000          │
│ ─────────────────────────────────────────────────────────── │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🚗 NAVEGAR NO GPS (Waze / Maps)                         │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ [ ✅ Entregue ]     [ 👤 Ausente ]     [ ↩️ Devolver ]      │
│ ─────────────────────────────────────────────────────────── │
│                                                             │
│ ┌───────────────────────────┐ ┌───────────────────────────┐ │ ◄── Row Balanceada
│ │  ✏️ Editar Pacote         │ │  🗑️ Remover da Rota       │ │     50% / 50%
│ │  Borda OrangeNeon (0.4f)  │ │  Borda RedAlert (0.4f)    │ │     Altura 40.dp
│ └───────────────────────────┘ └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 Especificação Compose dos Botões Utilitários

```kotlin
// Em StopDeliveryCard.kt — Rodapé do conteúdo expandido
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    // 1. Botão Editar Pacote (Dark Neon Laranja)
    OutlinedButton(
        onClick = { onEditStop(stop) },
        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = OrangeNeon.copy(alpha = 0.08f),
            contentColor = OrangeNeon
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .weight(1f)
            .height(40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Editar Pacote",
            tint = OrangeNeon,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Editar Pacote",
            color = OrangeNeon,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }

    // 2. Botão Remover Pacote da Rota (Dark Neon Vermelho)
    OutlinedButton(
        onClick = { onDeleteStop(stop) },
        border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = RedAlert.copy(alpha = 0.08f),
            contentColor = RedAlert
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .weight(1f)
            .height(40.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Remover Pacote da Rota",
            tint = RedAlert,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Remover",
            color = RedAlert,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
```

---

## 3. Especificação 2 — Diálogo de Edição Rápida: `EditStopDialog`

### 3.1 Racional de UX
O diálogo deve permitir correções pontuais sem sobrecarregar a memória do motorista, apresentando os dados atuais em campos claros, com validação de CEP e seleção simples de marketplace e tipo de volume.

### 3.2 Wireframe Textual — `EditStopDialog`
```
┌─────────────────────────────────────────────────────────────┐
│ ✏️  EDITAR DADOS DO PACOTE                                  │
│     Código: BR420918237BR (Fixo)                            │
│ ─────────────────────────────────────────────────────────── │
│                                                             │
│ Destinatário / Cliente:                                     │
│ [ CARLOS EDUARDO SILVA                            ]         │
│                                                             │
│ Endereço Completo:                                          │
│ [ Rua das Palmeiras, 120 - Apto 42                          │
│   Bairro Centro - São Paulo / SP                  ]         │
│                                                             │
│ CEP:                                                        │
│ [ 01001-000                                       ]         │
│                                                             │
│ Tipo de Pacote:                                             │
│ ┌───────────────────────────┐ ┌───────────────────────────┐ │
│ │  📦 Pacotinho             │ │  🏋️ Volumoso             │ │
│ └───────────────────────────┘ └───────────────────────────┘ │
│                                                             │
│ Plataforma / Marketplace:                                   │
│ [ Mercado Livre                                   ] [▼]     │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │              💾 SALVAR ALTERAÇÕES                       │ │ ◄── Primary CTA #FF5500
│ └─────────────────────────────────────────────────────────┘ │
│                      [ Cancelar ]                           │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Especificação dos Componentes do Diálogo

- **Contêiner:** `AlertDialog` ou `Dialog` com superfície `SurfaceDark` (`#1E1E1E`), borda de 1.5dp em `OrangeNeon.copy(alpha = 0.5f)` e cantos arredondados de `18.dp`.
- **Cabeçalho:** Ícone `Icons.Default.Edit` em badge circular Laranja Neon com subtítulo contendo o código de barras imutável (`#BR...`).
- **Campos de Entrada (`OutlinedTextField`):**
  - **Destinatário:** Texto simples, `maxLines = 1`, caixa alta automática recomendada.
  - **Endereço Completo:** `maxLines = 3`, `minLines = 2`, suporte a múltiplas linhas para número e complemento.
  - **CEP:** Teclado numérico (`KeyboardType.Number`), máscara de formatação automática `#####-###`.
- **Seletor de Tipo:** Segmented Chips com `[ 📦 Pacotinho ]` e `[ 🏋️ Volumoso ]`. O ativo recebe fundo sólido `OrangeNeon` ou `YellowGold`.
- **Seletor de Plataforma:** Dropdown menu estilizado populado com a lista de plataformas cadastradas no Supabase.
- **Ações:** Botão primário *"SALVAR ALTERAÇÕES"* em `OrangeNeon` e botão neutro *"Cancelar"*.

---

## 4. Especificação 3 — Scanner de Busca Instantânea no Cockpit

### 4.1 Diagnóstico de Campo
Ao carregar ou descarregar, o motorista muitas vezes está com um pacote na mão e precisa saber qual é a ordem da parada ou confirmar se ele já foi conferido. Digitar o código de barras no campo de busca com luvas ou sob o sol é demorado e propenso a erros.

### 4.2 Wireframe Textual — Barra de Busca com Scanner
```
┌─────────────────────────────────────────────────────────────┐
│ ┌───────────────────────────────────────────────┐ ┌───────┐ │
│ │ 🔍 Buscar endereço, cliente ou código...  [X] │ │  📷   │ │ ◄── Botão Scanner
│ │ Fundo SurfaceDark • Borda sutil 1dp           │ │ 48x48 │ │     Dark Neon
│ └───────────────────────────────────────────────┘ └───────┘ │     Laranja Neon
└─────────────────────────────────────────────────────────────┘
```

### 4.3 Diálogo de Câmera de Leitura Instantânea (`SearchBarcodeScanDialog`)

Ao clicar no botão de scanner, abre-se um diálogo modal compacto ou tela de leitura rápida:
- O CameraX inicializa exclusivamente em modo **Barcode Scanning**.
- Retículo central com laser animado e legenda: *"Aponte para o código de barras do pacote"*.
- Ao bipar (beep sonoro + vibração de 40ms):
  - O código lido é injetado diretamente em `searchQuery`.
  - O diálogo fecha automaticamente.
  - A lista filtra instantaneamente o pacote correspondente e o card resultante é **automaticamente expandido e destacado**.

```kotlin
// Em RouteCockpitScreen.kt — Campo de Busca com Botão de Scanner
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.onSearchQueryChanged(it) },
        placeholder = { Text("Buscar endereço, cliente ou código...", fontSize = 12.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (uiState.searchQuery.isNotBlank()) {
                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpar busca", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OrangeNeon,
            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
            focusedContainerColor = SurfaceDark,
            unfocusedContainerColor = SurfaceDark,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        modifier = Modifier.weight(1f)
    )

    // Botão de Leitura Rápida por Câmera / Barcode
    IconButton(
        onClick = { showSearchBarcodeScanner = true },
        modifier = Modifier
            .size(48.dp)
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, OrangeNeon.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Bipar código para buscar",
            tint = OrangeNeon,
            modifier = Modifier.size(22.dp)
        )
    }
}
```

---

## 5. Especificação 4 — Ajuste Ergonômico da Linha de Ações da Lista (Zero Quebra de Linha)

### 5.1 O Problema em Telas Compactas (360dp)
Na linha superior das paradas:
`"18 paradas listadas"  |  [Otimizar Rota]  [Expandir]  [Contrair]`
Em aparelhos como o Galaxy A03 / Moto G / Pixel compactos (360dp de largura útil), o texto *"Otimizar Rota"* quebrava em duas linhas (*"Otimizar\nRota"*), aumentando desnecessariamente a altura da linha e desestabilizando o alinhamento vertical.

### 5.2 Solução Ergonômica com Anti-Truncamento
1. **Regra de Tipografia:**
   - Todos os botões recebem `softWrap = false` e `maxLines = 1`.
   - Tamanho de fonte ajustado de `11.sp` para `10.5.sp`.
2. **Padding Otimizado:**
   - `contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)`.
3. **Contador Compacto:**
   - O contador passa a exibir `18 paradas` em vez de `18 paradas listadas`, liberando mais de 24dp de largura útil na linha.
4. **Layout Visual Resultante:**

```
┌─────────────────────────────────────────────────────────────┐
│ 18 paradas         [⚡ Otimizar]   [⤢ Expandir]  [⤡ Contrair]│ ◄── Perfeito em 360dp
└─────────────────────────────────────────────────────────────┘
```

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = "$totalDisplayCount paradas",
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondaryDark,
        maxLines = 1,
        softWrap = false
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = { viewModel.optimizeStopsOrder() },
            enabled = !uiState.isOptimizing && totalDisplayCount > 1,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.size(3.dp))
            Text("Otimizar", fontSize = 10.5.sp, color = OrangeNeon, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        TextButton(
            onClick = { viewModel.expandAllStops() },
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("Expandir", fontSize = 10.5.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }

        TextButton(
            onClick = { viewModel.collapseAllStops() },
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("Contrair", fontSize = 10.5.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}
```

---

## 6. Especificação 5 — Desmembramento da Barra de Parametrização no Scanner (`RouteScannerScreen`)

### 6.1 Diagnóstico do Corte de Texto
No `OcrConfirmationCard`, a Plataforma Ativa e os seletores de `Pacote` vs `Volumoso` estavam espremidos lado a lado em uma única `Row`. O texto `"Trocar"` e o nome da plataforma ficavam truncados em quase todos os dispositivos.

### 6.2 Solução em 2 Linhas Independentes
Desmembrar a parametrização em **duas seções visuais empilhadas**:
- **Linha A (Marketplace / Plataforma):** Ocupa 100% da largura, com badge de ícone, nome completo da plataforma em caixa alta e botão estilizado *"Trocar Plataforma"* que nunca sofre corte.
- **Linha B (Tipo de Volume):** Dois botões em largura 50%/50% para `[ 📦 Pacotinho ]` e `[ 🏋️ Volumoso ]`.

### 6.3 Wireframe Textual — Parametrização Desmembrada
```
┌─────────────────────────────────────────────────────────────┐
│ PLATAFORMA ATIVA:                                           │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🏢 MERCADO LIVRE                      [ 🔄 Trocar ]     │ │ ◄── Linha 1 (100% Width)
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ TIPO DE PACOTE:                                             │
│ ┌───────────────────────────┐ ┌───────────────────────────┐ │
│ │ 📦 Pacotinho (Ativo)      │ │ 🏋️ Volumoso              │ │ ◄── Linha 2 (50% / 50%)
│ └───────────────────────────┘ └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

```kotlin
// Em OcrConfirmationCard (RouteScannerScreen.kt)
Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    // 1. Linha de Plataforma Ativa (100% Largura - Zero Corte)
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onChangePlatform)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activePlatform?.name?.uppercase() ?: "PLATAFORMA GERAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = OrangeNeon.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Trocar Plataforma",
                    fontSize = 11.sp,
                    color = OrangeNeon,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }

    // 2. Linha de Tipo de Volume (50% / 50%)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isPacotinho = currentPackageType == PackageType.PACOTINHO
        Surface(
            color = if (isPacotinho) OrangeNeon else SurfaceDarkAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isPacotinho) OrangeNeon else Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectPackageType(PackageType.PACOTINHO) }
        ) {
            Text(
                text = "📦 Pacotinho",
                color = if (isPacotinho) Color.White else TextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 9.dp)
            )
        }

        val isVolumoso = currentPackageType == PackageType.VOLUMOSO
        Surface(
            color = if (isVolumoso) YellowGold else SurfaceDarkAlt,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (isVolumoso) YellowGold else Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .weight(1f)
                .clickable { onSelectPackageType(PackageType.VOLUMOSO) }
        ) {
            Text(
                text = "🏋️ Volumoso",
                color = if (isVolumoso) Color.Black else TextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 9.dp)
            )
        }
    }
}
```

---

## 7. Especificação 6 — Seletor de Destinatário (Master vs Parceiros com Ciclo Completo)

### 7.1 Diagnóstico
No código anterior, o clique apenas alternava entre `null` (Master) e o primeiro parceiro (`partners.firstOrNull()`). Se houvesse 3 ou mais parceiros cadastrados, os demais nunca podiam ser selecionados.

### 7.2 Comportamento de Ciclo Completo e Menu Suspenso
A nova interface oferece dois modos complementares de interação:
1. **Toque Curto (Ciclo Sequencial):** Ao tocar no card, o seletor avança ciclicamente por todos os parceiros:
   `Master (Você)` ➔ `Parceiro A` ➔ `Parceiro B` ➔ `Parceiro C` ➔ `Master (Você)`...
2. **Menu Suspenso (Dropdown Direto):** O ícone de seta para baixo abre instantaneamente a lista completa dos parceiros com indicação visual de quem está selecionado.

### 7.3 Wireframe Textual — Seletor de Parceiro
```
┌─────────────────────────────────────────────────────────────┐
│ DESTINATÁRIO DO PACOTE:                                     │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 👤 Destinado a: Master (Você)             [Ciclar ➔] [▼]│ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Menu Aberto (Ao tocar no [▼]):                              │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ ✓ 👤 Master (Você)                                      │ │
│ │   🛵 Carlos Silva (Moto)                                │ │
│ │   🚗 Marcos Oliveira (Carro)                            │ │
│ │   🛵 Rafael Mendes (Moto)                               │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

```kotlin
// Em OcrConfirmationCard — Seletor de Parceiro com Suporte Multi-Parceiro
if (partners.isNotEmpty()) {
    var showPartnerDropdown by remember { mutableStateOf(false) }
    val assignedPartner = partners.firstOrNull { it.id == selectedPartnerId }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = if (selectedPartnerId != null) OrangeNeon.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (selectedPartnerId != null) OrangeNeon else Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Cicla sequencialmente entre Master (null) e todos os parceiros
                    val partnerIds = listOf<String?>(null) + partners.map { it.id }
                    val currentIndex = partnerIds.indexOf(selectedPartnerId)
                    val nextIndex = (currentIndex + 1) % partnerIds.size
                    onSelectPartner(partnerIds[nextIndex])
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (selectedPartnerId != null) Icons.Default.TwoWheeler else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedPartnerId != null) OrangeNeon else TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (assignedPartner != null) "Atribuir a: ${assignedPartner.fullName}" else "Destinado a: Master (Você)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPartnerId != null) OrangeNeon else TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ciclar",
                        fontSize = 10.sp,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    IconButton(
                        onClick = { showPartnerDropdown = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Ver lista", tint = OrangeNeon)
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showPartnerDropdown,
            onDismissRequest = { showPartnerDropdown = false },
            modifier = Modifier.background(SurfaceDark)
        ) {
            DropdownMenuItem(
                text = { Text("👤 Master (Você)", color = if (selectedPartnerId == null) OrangeNeon else TextPrimaryDark) },
                onClick = {
                    onSelectPartner(null)
                    showPartnerDropdown = false
                }
            )
            partners.forEach { partner ->
                DropdownMenuItem(
                    text = { Text("🛵 ${partner.fullName}", color = if (selectedPartnerId == partner.id) OrangeNeon else TextPrimaryDark) },
                    onClick = {
                        onSelectPartner(partner.id)
                        showPartnerDropdown = false
                    }
                )
            }
        }
    }
}
```

---

## 8. Mapeamento de Tokens Material 3

| Elemento Visual | Token Compose | Hexadecimal | Descrição de Uso |
| :--- | :--- | :--- | :--- |
| **Botão Editar Pacote** | `OrangeNeon.copy(0.08f)` / `OrangeNeon` | `#14FF5500` / `#FF5500` | Fundo translúcido e borda em Laranja Neon |
| **Botão Remover Pacote** | `RedAlert.copy(0.08f)` / `RedAlert` | `#14D32F2F` / `#D32F2F` | Fundo translúcido e borda em Vermelho Alerta |
| **Botão Scanner de Busca** | `SurfaceDark` / `OrangeNeon` | `#1E1E1E` / `#FF5500` | Ícone `QrCodeScanner` destacado ao lado do input |
| **Plataforma Ativa Full** | `Color.Black.copy(0.55f)` | `#8C000000` | Card largo que elimina truncamento |
| **Seletor de Volume Ativo**| `OrangeNeon` ou `YellowGold` | `#FF5500` / `#B8860B` | Pacotinho (Laranja) ou Volumoso (Dourado) |

---

## 9. Blocos JSON de Especificação (Contratos para Devs)

### 9.1 Bloco JSON: Ações de Rodapé no `StopDeliveryCard`
```json
{
  "component_id": "STOP_CARD_FOOTER_ACTIONS",
  "screen_target": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/components/StopDeliveryCard.kt",
  "layout": "Row",
  "height_dp": 40,
  "buttons": [
    {
      "id": "BTN_EDIT_STOP",
      "label": "Editar Pacote",
      "icon": "Icons.Default.Edit",
      "theme": "DARK_NEON_ORANGE",
      "border_color": "#FF5500",
      "weight": 1.0,
      "action": "onEditStop(stop)"
    },
    {
      "id": "BTN_REMOVE_STOP",
      "label": "Remover",
      "icon": "Icons.Default.DeleteOutline",
      "theme": "DARK_NEON_RED",
      "border_color": "#D32F2F",
      "weight": 1.0,
      "action": "onDeleteStop(stop)"
    }
  ]
}
```

### 9.2 Bloco JSON: Busca com Scanner Integrado
```json
{
  "component_id": "SEARCH_BAR_WITH_CAMERA_SCANNER",
  "screen_target": "app/src/main/java/com/fernando/centraldomotorista/ui/screens/routes/master/RouteCockpitScreen.kt",
  "elements": {
    "text_field": {
      "placeholder": "Buscar endereço, cliente ou código...",
      "weight": 1.0
    },
    "scanner_button": {
      "icon": "Icons.Default.QrCodeScanner",
      "size_dp": 48,
      "border_color": "#FF5500",
      "action": "openSearchBarcodeScannerDialog"
    }
  }
}
```

---

## 10. Checklist de Implementação para o Agente Android/Kotlin

- [ ] **Em `StopDeliveryCard.kt`:**
  - Substituir o botão solitário de exclusão pela `Row` contendo *"Editar Pacote"* (`onEditStop`) e *"Remover"* (`onDeleteStop`).
- [ ] **Criar `EditStopDialog.kt` em `components/`:**
  - Diálogo de edição com suporte a destinatário, endereço, CEP, `PackageType` e plataforma.
- [ ] **Em `RouteCockpitScreen.kt`:**
  - Adicionar o botão `IconButton` de scanner ao lado do `OutlinedTextField` de busca.
  - Implementar o diálogo de leitura com CameraX para busca instantânea.
  - Aplicar `softWrap = false`, `maxLines = 1` e texto enxuto (`$total paradas`) nos botões de cabeçalho da lista.
  - Integrar o estado de `stopToEdit: MasterRouteStop?` acionando o `EditStopDialog`.
- [ ] **Em `RouteScannerScreen.kt`:**
  - Desmembrar a linha de plataforma e tipo de volume em duas linhas independentes no `OcrConfirmationCard`.
  - Implementar o seletor de parceiro com ciclo completo e menu suspenso para todos os parceiros da lista.
