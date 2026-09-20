package com.fernando.centraldomotorista.ui.screens.deliverypartners.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.screens.apps.PlatformSegmentFilter
import com.fernando.centraldomotorista.ui.screens.apps.PlatformSortBy
import com.fernando.centraldomotorista.ui.screens.apps.PlatformStatusFilter
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

/**
 * Sessão App & Plataforma para o Entregador Parceiro.
 * Replicando fielmente a tela "Gestão de Plataformas" do Drawer (Menu Lateral),
 * com busca, filtros de status, filtros de categoria, ordenação e cards com switch ativo/inativo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPlatformsSection(
    platforms: List<Platform>,
    earningsMap: Map<String, BigDecimal> = emptyMap(),
    selectedPlatformId: String? = null,
    onSelectPlatform: ((Platform) -> Unit)? = null,
    onEditPlatform: (String) -> Unit,
    onCreatePlatform: () -> Unit,
    onToggleActive: (Platform) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf(PlatformStatusFilter.ALL) }
    var segmentFilter by remember { mutableStateOf(PlatformSegmentFilter.ALL) }
    var sortBy by remember { mutableStateOf(PlatformSortBy.ALPHABETICAL) }
    var isFilterModalOpen by remember { mutableStateOf(false) }

    val activeFilterCount = (if (statusFilter != PlatformStatusFilter.ALL) 1 else 0) +
            (if (segmentFilter != PlatformSegmentFilter.ALL) 1 else 0) +
            (if (sortBy != PlatformSortBy.ALPHABETICAL) 1 else 0)

    val filteredPlatforms = remember(platforms, searchQuery, statusFilter, segmentFilter, sortBy, earningsMap) {
        var list = platforms

        // Filtro de status
        list = when (statusFilter) {
            PlatformStatusFilter.ALL -> list
            PlatformStatusFilter.ACTIVE -> list.filter { it.active }
            PlatformStatusFilter.INACTIVE -> list.filter { !it.active }
        }

        // Filtro de segmento
        if (segmentFilter != PlatformSegmentFilter.ALL && segmentFilter.rawValue != null) {
            list = list.filter { it.segment.equals(segmentFilter.rawValue, ignoreCase = true) }
        }

        // Busca textual
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                        it.segment.lowercase().contains(q) ||
                        it.cycle.lowercase().contains(q) ||
                        (it.paymentDay?.lowercase()?.contains(q) == true)
            }
        }

        // Ordenação
        when (sortBy) {
            PlatformSortBy.ALPHABETICAL -> list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            PlatformSortBy.HIGHEST_EARNING -> list.sortedByDescending { earningsMap[it.id] ?: BigDecimal.ZERO }
            PlatformSortBy.LOWEST_EARNING -> list.sortedBy { earningsMap[it.id] ?: BigDecimal.ZERO }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Subtítulo descritivo idêntico à Gestão de Plataformas
        Text(
            text = "Plataformas vinculadas à operação do entregador. Configure parâmetros, repasses e ganhos.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )

        // 1. Campo de Busca
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Buscar por nome, segmento ou ciclo...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Buscar", tint = OrangeNeon)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Limpar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OrangeNeon,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Barra de Contagem & Botão de Filtros com Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredPlatforms.size} ${if (filteredPlatforms.size == 1) "PLATAFORMA" else "PLATAFORMAS"}",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            AssistChip(
                onClick = { isFilterModalOpen = true },
                label = {
                    Text(
                        text = if (activeFilterCount > 0)
                            "Filtros & Ordenação ($activeFilterCount)"
                        else
                            "Filtros & Ordenação",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = OrangeNeon
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    labelColor = OrangeNeon
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (activeFilterCount > 0) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        // 3. Lista de Cards de Plataformas
        if (filteredPlatforms.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Nenhuma plataforma encontrada",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tente ajustar os filtros ou cadastre uma nova plataforma para o entregador.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredPlatforms.forEach { platform ->
                    val isSelected = platform.id == selectedPlatformId
                    PartnerPlatformCardItem(
                        platform = platform,
                        monthEarnings = earningsMap[platform.id] ?: BigDecimal.ZERO,
                        isSelected = isSelected,
                        onCardClick = { onSelectPlatform?.invoke(platform) },
                        onEditClick = { onEditPlatform(platform.id) },
                        onToggleActive = { onToggleActive(platform) }
                    )
                }
            }
        }

        // 4. Botão / Card de Ação: "ADICIONAR NOVA PLATAFORMA"
        Surface(
            onClick = onCreatePlatform,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = OrangeNeon.copy(alpha = 0.08f),
            border = BorderStroke(1.5.dp, OrangeNeon.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADICIONAR NOVA PLATAFORMA",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = OrangeNeon,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    // Modal BottomSheet de Filtros & Ordenação
    if (isFilterModalOpen) {
        PartnerFilterSortModal(
            statusFilter = statusFilter,
            segmentFilter = segmentFilter,
            sortBy = sortBy,
            onDismiss = { isFilterModalOpen = false },
            onStatusChange = { statusFilter = it },
            onSegmentChange = { segmentFilter = it },
            onSortChange = { sortBy = it },
            onClear = {
                statusFilter = PlatformStatusFilter.ALL
                segmentFilter = PlatformSegmentFilter.ALL
                sortBy = PlatformSortBy.ALPHABETICAL
            }
        )
    }
}

/**
 * Card individual de plataforma no padrão visual do "Gestor de Plataformas".
 */
@Composable
private fun PartnerPlatformCardItem(
    platform: Platform,
    monthEarnings: BigDecimal,
    isSelected: Boolean = false,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleActive: () -> Unit
) {
    val isLogistica = platform.segment.equals("logistica", ignoreCase = true)
    val segmentBadgeColor = if (isLogistica) BlueInfo else GreenNeon
    val segmentLabel = if (isLogistica) "Logística" else "Delivery"
    val cycleLabel = BillingCycleCalculator.getCycleDisplayLabel(platform.cycle, platform.paymentDay)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha Superior: Nome + Toggle Ativa/Inativa + Botão Editar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = platform.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Switch de Ativo/Inativo
                    Surface(
                        onClick = onToggleActive,
                        color = if (platform.active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (platform.active) GreenNeon.copy(alpha = 0.4f) else RedAlert.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (platform.active) "🟢 ATIVA" else "🔴 INATIVA",
                            color = if (platform.active) GreenNeon else RedAlert,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Botão de Configuração / Edição
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Editar ${platform.name}",
                            tint = OrangeNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Linha Intermediária: Badge Categoria + Frequência de Repasse
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = segmentBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, segmentBadgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = segmentLabel,
                        color = segmentBadgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = cycleLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Linha Inferior: Estimativa de Pagamento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Est. de pagamento:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = monthEarnings.formatCurrency(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = OrangeNeon
                )
            }
        }
    }
}

/**
 * Modal BottomSheet de Filtros e Ordenação idêntico ao da tela "Gestão de Plataformas".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartnerFilterSortModal(
    statusFilter: PlatformStatusFilter,
    segmentFilter: PlatformSegmentFilter,
    sortBy: PlatformSortBy,
    onDismiss: () -> Unit,
    onStatusChange: (PlatformStatusFilter) -> Unit,
    onSegmentChange: (PlatformSegmentFilter) -> Unit,
    onSortChange: (PlatformSortBy) -> Unit,
    onClear: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Cabeçalho do modal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FILTRAR & ORDENAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Personalize a exibição das plataformas do parceiro",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            // 1. Status da Plataforma
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "STATUS DA PLATAFORMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformStatusFilter.entries.forEach { status ->
                        val isSelected = statusFilter == status
                        OutlinedButton(
                            onClick = { onStatusChange(status) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.Transparent
                            )
                        ) {
                            Text(status.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Categoria (Segmento)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TIPO DE CATEGORIA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformSegmentFilter.entries.forEach { segment ->
                        val isSelected = segmentFilter == segment
                        OutlinedButton(
                            onClick = { onSegmentChange(segment) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.Transparent
                            )
                        ) {
                            Text(segment.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Ordenação
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ORDENAÇÃO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PlatformSortBy.entries.forEach { sort ->
                    val isSelected = sortBy == sort
                    Surface(
                        onClick = { onSortChange(sort) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) OrangeNeon else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sort.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Ações: Limpar e Aplicar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Limpar Filtros", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}
