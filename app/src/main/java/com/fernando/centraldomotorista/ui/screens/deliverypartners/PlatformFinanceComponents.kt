package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun BigDecimal.formatBrl(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

/**
 * Card de Finanças de uma Plataforma Ativa.
 */
@Composable
fun PlatformFinanceCard(
    platform: Platform,
    sessions: List<DeliveryPartnerSession>,
    filter: PlatformFinanceFilter,
    availableMonths: List<YearMonth>,
    onFilterChange: (PlatformFinanceFilter) -> Unit,
    onSessionClick: (DeliveryPartnerSession) -> Unit,
    routesMap: Map<String, DeliveryRoute>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDropdownOpen by remember { mutableStateOf(false) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var expandedWeeks by remember { mutableStateOf<Set<String>>(emptySet()) }

    val today = remember { LocalDate.now() }
    val breakdown = remember(platform.id, sessions, filter) {
        PlatformFinanceHelper.calculatePlatformBreakdown(platform.id, sessions, filter, today)
    }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Cabeçalho do Card da Plataforma
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nome e Badge da Plataforma
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = OrangeNeon.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = platform.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GreenNeon.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ATIVA",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenNeon,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "• ${platform.segment.replaceFirstChar { it.uppercase() }}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Combo de Período em Pill elegante (no canto superior direito)
                Box {
                    Surface(
                        onClick = { isDropdownOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(32.dp)
                            .padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = filter.getDisplayLabel(today),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Selecionar período",
                                tint = OrangeNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Dropdown de Opções de Período
                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mês Atual", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OrangeNeon) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.MES_CORRENTE))
                                isDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Esta Semana") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.ESTA_SEMANA))
                                isDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Semana Passada") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.SEMANA_PASSADA))
                                isDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Esta Quinzena") },
                            leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.ESTA_QUINZENA))
                                isDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mês Passado") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.MES_PASSADO))
                                isDropdownOpen = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Últimos 3 Meses") },
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            onClick = {
                                onFilterChange(PlatformFinanceFilter(preset = PlatformFinancePeriodPreset.ULTIMOS_3_MESES))
                                isDropdownOpen = false
                            }
                        )

                        // Meses Específicos disponíveis
                        if (availableMonths.isNotEmpty()) {
                            HorizontalDivider()
                            val monthFmt = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR"))
                            availableMonths.take(4).forEach { ym ->
                                val label = ym.format(monthFmt).replaceFirstChar { it.uppercase() }
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        onFilterChange(
                                            PlatformFinanceFilter(
                                                preset = PlatformFinancePeriodPreset.PERSONALIZADO,
                                                specificYearMonth = ym
                                            )
                                        )
                                        isDropdownOpen = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Personalizado (Datas...)", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                            leadingIcon = { Icon(Icons.Default.EditCalendar, contentDescription = null, tint = OrangeNeon) },
                            onClick = {
                                isDropdownOpen = false
                                showCustomDateDialog = true
                            }
                        )
                    }
                }
            }

            // 2. Métricas do Período (Card Hero + Sub-cards 50%/50%)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // a) Card Hero (Full-Width): TOTAL REPASSADO
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = GreenNeon.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = GreenNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "TOTAL REPASSADO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenNeon,
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = breakdown.grandTotalAmount.formatBrl(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenNeon,
                            textAlign = TextAlign.End
                        )
                    }
                }

                // b) Row de 2 Sub-cards (50% / 50%): ENTREGUES e DEVOLVIDOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card ENTREGUES
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "ENTREGUES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${breakdown.grandTotalDelivered}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "pacotes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 1.5.dp)
                                )
                            }
                        }
                    }

                    // Card DEVOLVIDOS
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "DEVOLVIDOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (breakdown.grandTotalReturned > 0) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${breakdown.grandTotalReturned}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (breakdown.grandTotalReturned > 0) RedAlert else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "pacotes",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 1.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // 3. Subdivisão por Meses e Semanas
            val monthsWithSessions = breakdown.months.filter { it.sessionCount > 0 }
            if (monthsWithSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhuma sessão de entrega registrada nesta plataforma no período selecionado.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    monthsWithSessions.forEach { monthSub ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Título do Mês
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = monthSub.monthLabel,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = OrangeNeon
                                        )
                                    }
                                    Text(
                                        text = "${monthSub.sessionCount} rota(s)",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Subseções por Semana
                                monthSub.weeks.filter { it.sessions.isNotEmpty() }.forEach { weekSub ->
                                    val weekKey = "${platform.id}-${monthSub.yearMonth}-${weekSub.weekIndex}"
                                    val isWeekExpanded = expandedWeeks.contains(weekKey)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                                            // Cabeçalho da Semana
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedWeeks = if (isWeekExpanded) {
                                                            expandedWeeks - weekKey
                                                        } else {
                                                            expandedWeeks + weekKey
                                                        }
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = weekSub.label,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${weekSub.deliveredCount} entregues • ${weekSub.returnedCount} devoluções",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = weekSub.amountPaid.formatBrl(),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GreenNeon
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .rotate(if (isWeekExpanded) 180f else 0f)
                                                    )
                                                }
                                            }

                                            // Detalhamento das Sessões da Semana
                                            AnimatedVisibility(
                                                visible = isWeekExpanded,
                                                enter = fadeIn(),
                                                exit = fadeOut()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    weekSub.sessions.forEach { s ->
                                                        val sDate = (s.startTime ?: s.createdAt)?.atZoneSameInstant(ZoneId.systemDefault())
                                                        val routeName = s.routeId?.let { routesMap[it]?.name } ?: "Sem Rota"
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.surface,
                                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                                            onClick = { onSessionClick(s) },
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = sDate?.format(dateFormatter) ?: "Data desconhecida",
                                                                        fontSize = 11.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Text(
                                                                        text = "$routeName • ${s.deliveredCount} pct",
                                                                        fontSize = 10.5.sp,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }

                                                                Text(
                                                                    text = s.amountPaid.formatBrl(),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = GreenNeon
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // SUBTOTAL DO MÊS
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = OrangeNeon.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Subtotal ${monthSub.monthLabel}:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = OrangeNeon,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = monthSub.subtotalAmountPaid.formatBrl(),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = OrangeNeon,
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                text = "${monthSub.subtotalDelivered} pacotes",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.5.sp,
                                                color = OrangeNeon.copy(alpha = 0.85f),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. LINHA DE TOTAL DO PERÍODO TOTAL
                    if (breakdown.isMultiInterval || monthsWithSessions.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GreenNeon.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, GreenNeon.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TOTAL GERAL DO PERÍODO",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 0.8.sp,
                                        color = GreenNeon
                                    )
                                    Text(
                                        text = "${breakdown.grandTotalDelivered} entregues • ${breakdown.grandTotalReturned} devolvidos (${breakdown.totalSessions} sessões)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = breakdown.grandTotalAmount.formatBrl(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = GreenNeon
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Seleção de Intervalo Personalizado
    if (showCustomDateDialog) {
        var tempStart by remember { mutableStateOf(filter.resolveRange(today).first) }
        var tempEnd by remember { mutableStateOf(filter.resolveRange(today).second) }

        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            icon = { Icon(Icons.Default.DateRange, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(28.dp)) },
            title = { Text("Selecionar Intervalo de Datas", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Escolha a data de início e de término do período para a plataforma ${platform.name}:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Data Início
                    OutlinedCard(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> tempStart = LocalDate.of(y, m + 1, d) },
                                tempStart.year,
                                tempStart.monthValue - 1,
                                tempStart.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data Início:", fontSize = 12.sp)
                            Text(tempStart.format(dateFormatter), fontWeight = FontWeight.Bold, color = OrangeNeon, fontSize = 13.sp)
                        }
                    }

                    // Data Fim
                    OutlinedCard(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> tempEnd = LocalDate.of(y, m + 1, d) },
                                tempEnd.year,
                                tempEnd.monthValue - 1,
                                tempEnd.dayOfMonth
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Data Término:", fontSize = 12.sp)
                            Text(tempEnd.format(dateFormatter), fontWeight = FontWeight.Bold, color = OrangeNeon, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val s = if (tempStart.isAfter(tempEnd)) tempEnd else tempStart
                        val e = if (tempStart.isAfter(tempEnd)) tempStart else tempEnd
                        onFilterChange(
                            PlatformFinanceFilter(
                                preset = PlatformFinancePeriodPreset.PERSONALIZADO,
                                customStart = s,
                                customEnd = e
                            )
                        )
                        showCustomDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Aplicar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCustomDateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Gráfico de Linhas mostrando os ganhos no tempo, com filtros de plataforma e período.
 */
@Composable
fun PlatformEarningsLineChart(
    activePlatforms: List<Platform>,
    sessions: List<DeliveryPartnerSession>,
    modifier: Modifier = Modifier
) {
    var selectedPlatformId by remember { mutableStateOf<String?>(null) } // null = todas as ativas
    var selectedPeriodPreset by remember { mutableStateOf(PlatformFinancePeriodPreset.MES_CORRENTE) }
    var selectedPoint by remember { mutableStateOf<PlatformChartPoint?>(null) }

    val today = remember { LocalDate.now() }
    val activePlatformIds = remember(activePlatforms) { activePlatforms.map { it.id }.toSet() }

    val filter = remember(selectedPeriodPreset) {
        PlatformFinanceFilter(preset = selectedPeriodPreset)
    }

    val chartPoints = remember(selectedPlatformId, activePlatformIds, sessions, filter) {
        PlatformFinanceHelper.generateEarningsLineChartData(
            platformIdFilter = selectedPlatformId,
            activePlatformIds = activePlatformIds,
            sessions = sessions,
            filter = filter,
            today = today
        )
    }

    val maxAmount = remember(chartPoints) {
        val maxVal = chartPoints.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
        if (maxVal > BigDecimal.ZERO) maxVal else BigDecimal("100.00")
    }

    val totalAmount = remember(chartPoints) {
        chartPoints.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
    }

    val bestDay = remember(chartPoints) {
        chartPoints.maxByOrNull { it.amount }
    }

    val averageDaily = remember(chartPoints, totalAmount) {
        if (chartPoints.isNotEmpty()) {
            totalAmount.divide(BigDecimal(chartPoints.size), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Título
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "EVOLUÇÃO DOS GANHOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = totalAmount.formatBrl(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = GreenNeon
                )
            }

            // Filtros no Topo do Gráfico (Plataforma e Período)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Filtro de Plataforma (Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedPlatformId == null,
                        onClick = {
                            selectedPlatformId = null
                            selectedPoint = null
                        },
                        label = { Text("Todas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeNeon,
                            selectedLabelColor = Color.Black
                        )
                    )

                    activePlatforms.forEach { p ->
                        FilterChip(
                            selected = selectedPlatformId == p.id,
                            onClick = {
                                selectedPlatformId = p.id
                                selectedPoint = null
                            },
                            label = { Text(p.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangeNeon,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Filtro de Período
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        PlatformFinancePeriodPreset.ESTA_SEMANA to "Semana",
                        PlatformFinancePeriodPreset.MES_CORRENTE to "Mês",
                        PlatformFinancePeriodPreset.MES_PASSADO to "Mês Ant.",
                        PlatformFinancePeriodPreset.ULTIMOS_3_MESES to "3 Meses"
                    )
                    presets.forEach { (preset, label) ->
                        val isSelected = selectedPeriodPreset == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) OrangeNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.dp, OrangeNeon) else null,
                            onClick = {
                                selectedPeriodPreset = preset
                                selectedPoint = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Exibição do Ponto Selecionado
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenNeon.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dia ${selectedPoint!!.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedPoint!!.amount.formatBrl()} (${selectedPoint!!.deliveredCount} entregues)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenNeon
                        )
                    }
                }
            }

            // Gráfico de Linhas em Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(chartPoints) {
                            detectTapGestures { tapOffset ->
                                val count = chartPoints.size
                                if (count > 0) {
                                    val stepX = size.width / if (count > 1) (count - 1) else 1
                                    val tapped = chartPoints.minByOrNull {
                                        kotlin.math.abs((chartPoints.indexOf(it) * stepX) - tapOffset.x)
                                    }
                                    if (tapped != null) {
                                        selectedPoint = if (selectedPoint?.date == tapped.date) null else tapped
                                    }
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val count = chartPoints.size
                    if (count == 0) return@Canvas

                    val stepX = if (count > 1) w / (count - 1) else w / 2f
                    val maxD = maxAmount.toDouble().coerceAtLeast(1.0)

                    fun getY(amt: BigDecimal): Float {
                        val norm = (amt.toDouble() / maxD).coerceIn(0.0, 1.0)
                        return (h - (norm * h * 0.78f) - (h * 0.12f)).toFloat()
                    }

                    // Linhas de Grade Tracejadas (Topo, Meio, Base)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    val gridColor = Color.Gray.copy(alpha = 0.2f)

                    drawLine(gridColor, Offset(0f, getY(maxAmount)), Offset(w, getY(maxAmount)), strokeWidth = 1f, pathEffect = dashEffect)
                    drawLine(gridColor, Offset(0f, getY(maxAmount.divide(BigDecimal(2), 2, RoundingMode.HALF_UP))), Offset(w, getY(maxAmount.divide(BigDecimal(2), 2, RoundingMode.HALF_UP))), strokeWidth = 1f, pathEffect = dashEffect)
                    drawLine(gridColor, Offset(0f, h - 10f), Offset(w, h - 10f), strokeWidth = 1f)

                    // Path da Linha e Path do Gradiente
                    val pathLine = Path()
                    val pathFill = Path()

                    chartPoints.forEachIndexed { i, p ->
                        val x = if (count > 1) i * stepX else w / 2f
                        val y = getY(p.amount)

                        if (i == 0) {
                            pathLine.moveTo(x, y)
                            pathFill.moveTo(x, h - 10f)
                            pathFill.lineTo(x, y)
                        } else {
                            pathLine.lineTo(x, y)
                            pathFill.lineTo(x, y)
                        }
                    }

                    val lastX = if (count > 1) (count - 1) * stepX else w / 2f
                    pathFill.lineTo(lastX, h - 10f)
                    pathFill.close()

                    // Desenha o gradiente abaixo da linha
                    drawPath(
                        path = pathFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(OrangeNeon.copy(alpha = 0.35f), Color.Transparent),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Desenha a linha de tendência
                    drawPath(
                        path = pathLine,
                        color = OrangeNeon,
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Desenha os pontos
                    chartPoints.forEachIndexed { i, p ->
                        val x = if (count > 1) i * stepX else w / 2f
                        val y = getY(p.amount)
                        val isSelected = selectedPoint?.date == p.date

                        if (p.amount > BigDecimal.ZERO || isSelected) {
                            drawCircle(
                                color = if (isSelected) GreenNeon else OrangeNeon,
                                radius = if (isSelected) 6f else 3.5f,
                                center = Offset(x, y)
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5f,
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }

            // Linha X de Datas de Referência (Início, Meio e Fim)
            if (chartPoints.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(chartPoints.first().label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (chartPoints.size > 2) {
                        Text(chartPoints[chartPoints.size / 2].label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(chartPoints.last().label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Métricas de Rodapé
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Média Diária", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(averageDaily.formatBrl(), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (bestDay != null && bestDay.amount > BigDecimal.ZERO) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Maior Faturamento (${bestDay.label})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(bestDay.amount.formatBrl(), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GreenNeon)
                    }
                }
            }
        }
    }
}
