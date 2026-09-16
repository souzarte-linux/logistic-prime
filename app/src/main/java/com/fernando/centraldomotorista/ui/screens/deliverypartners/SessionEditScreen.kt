package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import com.fernando.centraldomotorista.data.model.DeliveryRoute
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utilitário para cálculo automático e bilateral das sessões de entrega.
 */
object SessionCalculationHelper {
    fun parseAmount(text: String): BigDecimal {
        val clean = text.filter { it.isDigit() || it == ',' || it == '.' }.trim()
        if (clean.isBlank()) return BigDecimal.ZERO
        val normalized = if (clean.contains(',')) {
            clean.replace(".", "").replace(',', '.')
        } else {
            clean
        }
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    fun calculateAmount(deliveredCount: Int, packageRate: BigDecimal, defaultBonus: BigDecimal): BigDecimal {
        if (deliveredCount <= 0 && defaultBonus <= BigDecimal.ZERO) return BigDecimal.ZERO
        val base = BigDecimal(deliveredCount.coerceAtLeast(0)).multiply(packageRate.coerceAtLeast(BigDecimal.ZERO))
        return base.add(defaultBonus.coerceAtLeast(BigDecimal.ZERO)).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateDeliveredFromAmount(amount: BigDecimal, packageRate: BigDecimal, defaultBonus: BigDecimal): Int {
        if (packageRate <= BigDecimal.ZERO) return 0
        val net = amount.subtract(defaultBonus.coerceAtLeast(BigDecimal.ZERO)).coerceAtLeast(BigDecimal.ZERO)
        return net.divide(packageRate, 0, RoundingMode.HALF_UP).toInt()
    }

    fun calculateReturned(expected: Int, delivered: Int): Int {
        return (expected - delivered).coerceAtLeast(0)
    }

    fun calculateDelivered(expected: Int, returned: Int): Int {
        return (expected - returned).coerceAtLeast(0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionEditScreen(
    session: DeliveryPartnerSession,
    routes: List<DeliveryRoute>,
    partner: DeliveryPartner? = null,
    isReadOnly: Boolean = false,
    onToggleEditMode: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (DeliveryPartnerSession) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val zone = remember { ZoneId.systemDefault() }

    var readOnlyMode by remember(isReadOnly) { mutableStateOf(isReadOnly) }

    BackHandler(onBack = onDismiss)

    // Formatação de data e hora
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // Rota selecionada
    var selectedRouteId by remember { mutableStateOf(session.routeId) }
    var routeDropdownOpen by remember { mutableStateOf(false) }
    val selectedRouteName = remember(selectedRouteId, routes) {
        routes.firstOrNull { it.id == selectedRouteId }?.name ?: "Sem Rota Definida"
    }

    // Horários (Início e Término)
    var startDateTime by remember {
        mutableStateOf(session.startTime ?: OffsetDateTime.now())
    }
    var hasEndTime by remember { mutableStateOf(session.endTime != null) }
    var endDateTime by remember {
        mutableStateOf(session.endTime ?: OffsetDateTime.now())
    }

    // Taxa e Bônus com fallback para os padrões do parceiro se zerados na sessão
    val initialPackageRate = if (session.packageRate > BigDecimal.ZERO) {
        session.packageRate
    } else {
        partner?.packageRate ?: BigDecimal.ZERO
    }
    val initialDefaultBonus = if (session.defaultBonus > BigDecimal.ZERO) {
        session.defaultBonus
    } else {
        partner?.defaultBonus ?: BigDecimal.ZERO
    }

    // Quantidade de pacotes
    var expectedText by remember { mutableStateOf(session.expectedPackageCount.toString()) }
    var deliveredText by remember { mutableStateOf(session.deliveredCount.toString()) }
    var returnedText by remember { mutableStateOf(session.returnedCount.toString()) }

    // Valores Financeiros
    var packageRateText by remember {
        mutableStateOf(if (initialPackageRate > BigDecimal.ZERO) initialPackageRate.toPlainString() else "")
    }
    var defaultBonusText by remember {
        mutableStateOf(if (initialDefaultBonus > BigDecimal.ZERO) initialDefaultBonus.toPlainString() else "")
    }
    var amountPaidText by remember {
        val initialAmount = if (session.amountPaid > BigDecimal.ZERO) {
            session.amountPaid
        } else {
            SessionCalculationHelper.calculateAmount(session.deliveredCount, initialPackageRate, initialDefaultBonus)
        }
        mutableStateOf(initialAmount.toPlainString())
    }

    // Bipagens (Códigos Bipados)
    var barcodes by remember { mutableStateOf(session.scannedBarcodes) }
    var newBarcodeInput by remember { mutableStateOf("") }
    var barcodeToDelete by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    // Duração calculada em tempo real
    val calculatedDurationStr = remember(startDateTime, hasEndTime, endDateTime) {
        if (hasEndTime) {
            formatDuration(startDateTime, endDateTime)
        } else {
            "Em andamento"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (readOnlyMode) "SESSÃO DA ROTA" else "EDITAR SESSÃO",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (readOnlyMode) {
                        IconButton(
                            onClick = {
                                if (onToggleEditMode != null) onToggleEditMode() else readOnlyMode = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar Sessão",
                                tint = OrangeNeon
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val exp = expectedText.toIntOrNull() ?: session.expectedPackageCount
                                val del = deliveredText.toIntOrNull() ?: session.deliveredCount
                                val ret = returnedText.toIntOrNull() ?: session.returnedCount
                                val pkgRate = parseAmount(packageRateText)
                                val defBonus = parseAmount(defaultBonusText)
                                val amtPaid = parseAmount(amountPaidText)

                                val updated = session.copy(
                                    routeId = selectedRouteId,
                                    startTime = startDateTime,
                                    endTime = if (hasEndTime) endDateTime else null,
                                    expectedPackageCount = exp,
                                    deliveredCount = del,
                                    returnedCount = ret,
                                    packageRate = pkgRate,
                                    defaultBonus = defBonus,
                                    amountPaid = amtPaid,
                                    scannedBarcodes = barcodes,
                                    scannedCount = barcodes.size
                                )
                                onSave(updated)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Salvar",
                                tint = OrangeNeon
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (readOnlyMode) {
                        Button(
                            onClick = {
                                if (onToggleEditMode != null) onToggleEditMode() else readOnlyMode = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Editar Sessão",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                val exp = expectedText.toIntOrNull() ?: session.expectedPackageCount
                                val del = deliveredText.toIntOrNull() ?: session.deliveredCount
                                val ret = returnedText.toIntOrNull() ?: session.returnedCount
                                val pkgRate = parseAmount(packageRateText)
                                val defBonus = parseAmount(defaultBonusText)
                                val amtPaid = parseAmount(amountPaidText)

                                val updated = session.copy(
                                    routeId = selectedRouteId,
                                    startTime = startDateTime,
                                    endTime = if (hasEndTime) endDateTime else null,
                                    expectedPackageCount = exp,
                                    deliveredCount = del,
                                    returnedCount = ret,
                                    packageRate = pkgRate,
                                    defaultBonus = defBonus,
                                    amountPaid = amtPaid,
                                    scannedBarcodes = barcodes,
                                    scannedCount = barcodes.size
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeNeon,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Salvar Alterações",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // 0. SEÇÃO: DADOS DO ENTREGADOR PARCEIRO (SE DISPONÍVEL)
            if (partner != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = OrangeNeon.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = partner.fullName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!partner.phone.isNullOrBlank()) {
                                        Text(
                                            text = partner.phone,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!partner.pixKey.isNullOrBlank()) {
                                        Text(
                                            text = "PIX: ${partner.pixKey}",
                                            fontSize = 11.sp,
                                            color = GreenNeon,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            if (readOnlyMode) {
                                Surface(
                                    color = OrangeNeon.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "VISUALIZAÇÃO",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeNeon,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. SEÇÃO: SELEÇÃO DE ROTA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ROTA VINCULADA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !readOnlyMode) { routeDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon)
                                        Text(
                                            text = selectedRouteName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OrangeNeon)
                                }
                            }

                            DropdownMenu(
                                expanded = routeDropdownOpen,
                                onDismissRequest = { routeDropdownOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sem Rota Definida") },
                                    onClick = {
                                        selectedRouteId = null
                                        routeDropdownOpen = false
                                    }
                                )
                                routes.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r.name) },
                                        onClick = {
                                            selectedRouteId = r.id
                                            routeDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SEÇÃO: HORÁRIOS DA SESSÃO (Início e Término)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "HORÁRIOS DA ROTA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OrangeNeon
                            )

                            Surface(
                                color = OrangeNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Duração: $calculatedDurationStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangeNeon
                                    )
                                }
                            }
                        }

                        // INÍCIO DA SESSÃO
                        Text("Início:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Data de Início
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                OutlinedTextField(
                                    value = startDateTime.atZoneSameInstant(zone).format(dateFormatter),
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    maxLines = 1,
                                    label = { Text("Data Início", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = {
                                        IconButton(
                                            enabled = !readOnlyMode,
                                            onClick = {
                                                val currentLocal = startDateTime.atZoneSameInstant(zone).toLocalDate()
                                                showDatePicker(context, currentLocal) { newDate ->
                                                    val localTime = startDateTime.atZoneSameInstant(zone).toLocalTime()
                                                    val newZoned = newDate.atTime(localTime).atZone(zone)
                                                    startDateTime = newZoned.toOffsetDateTime()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.CalendarToday, contentDescription = "Data Início", tint = if (readOnlyMode) MaterialTheme.colorScheme.onSurfaceVariant else OrangeNeon)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !readOnlyMode) {
                                            val currentLocal = startDateTime.atZoneSameInstant(zone).toLocalDate()
                                            showDatePicker(context, currentLocal) { newDate ->
                                                val localTime = startDateTime.atZoneSameInstant(zone).toLocalTime()
                                                val newZoned = newDate.atTime(localTime).atZone(zone)
                                                startDateTime = newZoned.toOffsetDateTime()
                                            }
                                        }
                                )
                            }

                            // Hora de Início
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                OutlinedTextField(
                                    value = startDateTime.atZoneSameInstant(zone).format(timeFormatter),
                                    onValueChange = {},
                                    readOnly = true,
                                    singleLine = true,
                                    maxLines = 1,
                                    label = { Text("Hora Início", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    trailingIcon = {
                                        IconButton(
                                            enabled = !readOnlyMode,
                                            onClick = {
                                                val currentLocalTime = startDateTime.atZoneSameInstant(zone).toLocalTime()
                                                showTimePicker(context, currentLocalTime) { newTime ->
                                                    val localDate = startDateTime.atZoneSameInstant(zone).toLocalDate()
                                                    val newZoned = localDate.atTime(newTime).atZone(zone)
                                                    startDateTime = newZoned.toOffsetDateTime()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.AccessTime, contentDescription = "Hora Início", tint = if (readOnlyMode) MaterialTheme.colorScheme.onSurfaceVariant else OrangeNeon)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !readOnlyMode) {
                                            val currentLocalTime = startDateTime.atZoneSameInstant(zone).toLocalTime()
                                            showTimePicker(context, currentLocalTime) { newTime ->
                                                val localDate = startDateTime.atZoneSameInstant(zone).toLocalDate()
                                                val newZoned = localDate.atTime(newTime).atZone(zone)
                                                startDateTime = newZoned.toOffsetDateTime()
                                            }
                                        }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        // TÉRMINO DA SESSÃO
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Término:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable(enabled = !readOnlyMode) { hasEndTime = !hasEndTime }
                            ) {
                                Checkbox(
                                    checked = hasEndTime,
                                    enabled = !readOnlyMode,
                                    onCheckedChange = { hasEndTime = it },
                                    colors = CheckboxDefaults.colors(checkedColor = OrangeNeon)
                                )
                                Text("Sessão Concluída", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (hasEndTime) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Data de Término
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    OutlinedTextField(
                                        value = endDateTime.atZoneSameInstant(zone).format(dateFormatter),
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        maxLines = 1,
                                        label = { Text("Data Término", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        trailingIcon = {
                                            IconButton(
                                                enabled = !readOnlyMode,
                                                onClick = {
                                                    val currentLocal = endDateTime.atZoneSameInstant(zone).toLocalDate()
                                                    showDatePicker(context, currentLocal) { newDate ->
                                                        val localTime = endDateTime.atZoneSameInstant(zone).toLocalTime()
                                                        val newZoned = newDate.atTime(localTime).atZone(zone)
                                                        endDateTime = newZoned.toOffsetDateTime()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.CalendarToday, contentDescription = "Data Término", tint = if (readOnlyMode) MaterialTheme.colorScheme.onSurfaceVariant else OrangeNeon)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !readOnlyMode) {
                                                val currentLocal = endDateTime.atZoneSameInstant(zone).toLocalDate()
                                                showDatePicker(context, currentLocal) { newDate ->
                                                    val localTime = endDateTime.atZoneSameInstant(zone).toLocalTime()
                                                    val newZoned = newDate.atTime(localTime).atZone(zone)
                                                    endDateTime = newZoned.toOffsetDateTime()
                                                }
                                            }
                                    )
                                }

                                // Hora de Término
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    OutlinedTextField(
                                        value = endDateTime.atZoneSameInstant(zone).format(timeFormatter),
                                        onValueChange = {},
                                        readOnly = true,
                                        singleLine = true,
                                        maxLines = 1,
                                        label = { Text("Hora Término", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        trailingIcon = {
                                            IconButton(
                                                enabled = !readOnlyMode,
                                                onClick = {
                                                    val currentLocalTime = endDateTime.atZoneSameInstant(zone).toLocalTime()
                                                    showTimePicker(context, currentLocalTime) { newTime ->
                                                        val localDate = endDateTime.atZoneSameInstant(zone).toLocalDate()
                                                        val newZoned = localDate.atTime(newTime).atZone(zone)
                                                        endDateTime = newZoned.toOffsetDateTime()
                                                    }
                                                }
                                            ) {
                                                Icon(Icons.Default.AccessTime, contentDescription = "Hora Término", tint = if (readOnlyMode) MaterialTheme.colorScheme.onSurfaceVariant else OrangeNeon)
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !readOnlyMode) {
                                                val currentLocalTime = endDateTime.atZoneSameInstant(zone).toLocalTime()
                                                showTimePicker(context, currentLocalTime) { newTime ->
                                                    val localDate = endDateTime.atZoneSameInstant(zone).toLocalDate()
                                                    val newZoned = localDate.atTime(newTime).atZone(zone)
                                                    endDateTime = newZoned.toOffsetDateTime()
                                                }
                                            }
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "A sessão está marcada como em andamento (sem horário de finalização).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. SEÇÃO: CONTAGEM DE PACOTES
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "VOLUMETRIA DE PACOTES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = OrangeNeon
                        )

                        // 3 Campos de Volumetria na mesma linha com alturas e larguras padronizadas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = expectedText,
                                onValueChange = { input ->
                                    if (readOnlyMode) return@OutlinedTextField
                                    val clean = input.filter { c -> c.isDigit() }
                                    expectedText = clean
                                    val exp = clean.toIntOrNull() ?: 0
                                    val currentRet = returnedText.toIntOrNull() ?: 0
                                    val rate = SessionCalculationHelper.parseAmount(packageRateText)
                                    val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)

                                    val newDel = SessionCalculationHelper.calculateDelivered(exp, currentRet)
                                    deliveredText = newDel.toString()

                                    val total = SessionCalculationHelper.calculateAmount(newDel, rate, bonus)
                                    amountPaidText = total.toPlainString()
                                },
                                readOnly = readOnlyMode,
                                label = { Text("Expedidos", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )

                            OutlinedTextField(
                                value = deliveredText,
                                onValueChange = { input ->
                                    if (readOnlyMode) return@OutlinedTextField
                                    val clean = input.filter { c -> c.isDigit() }
                                    deliveredText = clean
                                    val del = clean.toIntOrNull() ?: 0
                                    val exp = expectedText.toIntOrNull() ?: 0
                                    val rate = SessionCalculationHelper.parseAmount(packageRateText)
                                    val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)

                                    if (exp > 0) {
                                        val ret = SessionCalculationHelper.calculateReturned(exp, del)
                                        returnedText = ret.toString()
                                    }

                                    val total = SessionCalculationHelper.calculateAmount(del, rate, bonus)
                                    amountPaidText = total.toPlainString()
                                },
                                readOnly = readOnlyMode,
                                label = { Text("Entregues", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = GreenNeon),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )

                            OutlinedTextField(
                                value = returnedText,
                                onValueChange = { input ->
                                    if (readOnlyMode) return@OutlinedTextField
                                    val clean = input.filter { c -> c.isDigit() }
                                    returnedText = clean
                                    val ret = clean.toIntOrNull() ?: 0
                                    val exp = expectedText.toIntOrNull() ?: 0
                                    val rate = SessionCalculationHelper.parseAmount(packageRateText)
                                    val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)

                                    val newDel = SessionCalculationHelper.calculateDelivered(exp, ret)
                                    deliveredText = newDel.toString()

                                    val total = SessionCalculationHelper.calculateAmount(newDel, rate, bonus)
                                    amountPaidText = total.toPlainString()
                                },
                                readOnly = readOnlyMode,
                                label = { Text("Devolvidos", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = RedAlert),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        // Alerta visual de coerência entre expedidos e soma (entregues + devolvidos)
                        val expVal = expectedText.toIntOrNull() ?: 0
                        val delVal = deliveredText.toIntOrNull() ?: 0
                        val retVal = returnedText.toIntOrNull() ?: 0
                        if (expVal > 0 && (delVal + retVal) != expVal) {
                            Surface(
                                color = RedAlert.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Aviso: Entregues ($delVal) + Devolvidos ($retVal) = ${delVal + retVal}, diferente do expedido ($expVal).",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = RedAlert,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. SEÇÃO: VALORES FINANCEIROS (BigDecimal)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "VALORES FINANCEIROS (R$)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = OrangeNeon
                            )

                            // Sugestão de sincronização rápida (entregues * taxa + bônus)
                            val delCount = deliveredText.toIntOrNull() ?: 0
                            val rate = SessionCalculationHelper.parseAmount(packageRateText)
                            val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)
                            if (!readOnlyMode && delCount > 0 && rate > BigDecimal.ZERO) {
                                val suggested = SessionCalculationHelper.calculateAmount(delCount, rate, bonus)
                                TextButton(
                                    onClick = { amountPaidText = suggested.toPlainString() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Sinc R$ ${suggested.toPlainString()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenNeon
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = packageRateText,
                                onValueChange = { input ->
                                    if (readOnlyMode) return@OutlinedTextField
                                    packageRateText = input
                                    val rate = SessionCalculationHelper.parseAmount(input)
                                    val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)
                                    val del = deliveredText.toIntOrNull() ?: 0

                                    val total = SessionCalculationHelper.calculateAmount(del, rate, bonus)
                                    amountPaidText = total.toPlainString()
                                },
                                readOnly = readOnlyMode,
                                label = { Text("Taxa / Pacote", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                prefix = { Text("R$ ", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )

                            OutlinedTextField(
                                value = defaultBonusText,
                                onValueChange = { input ->
                                    if (readOnlyMode) return@OutlinedTextField
                                    defaultBonusText = input
                                    val bonus = SessionCalculationHelper.parseAmount(input)
                                    val rate = SessionCalculationHelper.parseAmount(packageRateText)
                                    val del = deliveredText.toIntOrNull() ?: 0

                                    val total = SessionCalculationHelper.calculateAmount(del, rate, bonus)
                                    amountPaidText = total.toPlainString()
                                },
                                readOnly = readOnlyMode,
                                label = { Text("Bônus Fixo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                prefix = { Text("R$ ", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.Bold) },
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }

                        OutlinedTextField(
                            value = amountPaidText,
                            onValueChange = { input ->
                                if (readOnlyMode) return@OutlinedTextField
                                amountPaidText = input
                                val amt = SessionCalculationHelper.parseAmount(input)
                                val rate = SessionCalculationHelper.parseAmount(packageRateText)
                                val bonus = SessionCalculationHelper.parseAmount(defaultBonusText)
                                val exp = expectedText.toIntOrNull() ?: 0

                                // Cálculo bilateral reverso: ao alterar o valor total pago, atualiza a quantidade entregue calculada
                                if (rate > BigDecimal.ZERO) {
                                    val derivedDel = SessionCalculationHelper.calculateDeliveredFromAmount(amt, rate, bonus)
                                    deliveredText = derivedDel.toString()
                                    if (exp > 0) {
                                        val derivedRet = SessionCalculationHelper.calculateReturned(exp, derivedDel)
                                        returnedText = derivedRet.toString()
                                    }
                                }
                            },
                            readOnly = readOnlyMode,
                            label = { Text("Valor Total Pago ao Parceiro", maxLines = 1) },
                            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = GreenNeon) },
                            singleLine = true,
                            maxLines = 1,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Detalhamento visual da fórmula bilateral ativa
                        val curDel = deliveredText.toIntOrNull() ?: 0
                        val curRate = SessionCalculationHelper.parseAmount(packageRateText)
                        val curBonus = SessionCalculationHelper.parseAmount(defaultBonusText)
                        if (curRate > BigDecimal.ZERO) {
                            val formulaText = if (curBonus > BigDecimal.ZERO) {
                                "$curDel pct × R$ ${curRate.toPlainString()} + R$ ${curBonus.toPlainString()} = R$ ${SessionCalculationHelper.calculateAmount(curDel, curRate, curBonus).toPlainString()}"
                            } else {
                                "$curDel pct × R$ ${curRate.toPlainString()} = R$ ${SessionCalculationHelper.calculateAmount(curDel, curRate, curBonus).toPlainString()}"
                            }
                            Surface(
                                color = GreenNeon.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = "Cálculo bilateral ativo: $formulaText",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GreenNeon
                                    )
                                }
                            }
                        }

                        if (!session.expenseId.isNullOrBlank()) {
                            Text(
                                text = "Esta sessão possui despesa vinculada no histórico. Ao alterar o valor pago, a despesa correspondente será atualizada automaticamente.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. SEÇÃO: BIPAGEM DE PACOTES (scannedBarcodes)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "CÓDIGOS BIPADOS (${barcodes.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = OrangeNeon
                                )
                            }

                            if (barcodes.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            val allCodes = barcodes.joinToString("\n")
                                            clipboardManager.setText(AnnotatedString(allCodes))
                                            Toast.makeText(context, "${barcodes.size} códigos copiados!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = OrangeNeon.copy(alpha = 0.2f),
                                            contentColor = OrangeNeon
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiar Todos", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    if (!readOnlyMode) {
                                        OutlinedButton(
                                            onClick = { showClearAllConfirmation = true },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp),
                                            border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedAlert, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Limpar", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                                        }
                                    }
                                }
                            }
                        }

                        // Campo para adicionar novo código de barra manualmente
                        if (!readOnlyMode) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newBarcodeInput,
                                    onValueChange = { newBarcodeInput = it.trim() },
                                    label = { Text("Adicionar código manual", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    placeholder = { Text("Ex: 100827392817") },
                                    singleLine = true,
                                    maxLines = 1,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )

                                Button(
                                    onClick = {
                                        val code = newBarcodeInput.trim()
                                        if (code.isBlank()) {
                                            Toast.makeText(context, "Digite um código válido", Toast.LENGTH_SHORT).show()
                                        } else if (barcodes.contains(code)) {
                                            Toast.makeText(context, "Código já consta na lista!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            barcodes = barcodes + code
                                            newBarcodeInput = ""
                                            Toast.makeText(context, "Código adicionado!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxHeight().defaultMinSize(minWidth = 52.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Adicionar")
                                }
                            }
                        }

                        // Lista de códigos bipados vazia
                        if (barcodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum código de barras registrado nesta sessão.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Exibição dos itens individuais de códigos bipados
            itemsIndexed(barcodes, key = { index, code -> "$code-$index" }) { index, code ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = code,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
                                    Toast.makeText(context, "Código copiado: $code", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar Código",
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            if (!readOnlyMode) {
                                IconButton(
                                    onClick = {
                                        barcodeToDelete = code
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover Código",
                                        tint = RedAlert,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Confirmação de Exclusão Individual de Código
    if (barcodeToDelete != null) {
        val codeTarget = barcodeToDelete!!
        AlertDialog(
            onDismissRequest = { barcodeToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Excluir Código Bipado", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text("Deseja realmente remover o código \"$codeTarget\" desta sessão?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        barcodes = barcodes.filter { it != codeTarget }
                        barcodeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { barcodeToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Confirmação para Limpar Todos os Códigos
    if (showClearAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Limpar Todos os Códigos", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Text("Deseja realmente remover todos os ${barcodes.size} códigos bipados desta sessão?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        barcodes = emptyList()
                        showClearAllConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Limpar Todos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearAllConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun showDatePicker(
    context: Context,
    currentDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        currentDate.year,
        currentDate.monthValue - 1,
        currentDate.dayOfMonth
    ).show()
}

private fun showTimePicker(
    context: Context,
    currentTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(LocalTime.of(hourOfDay, minute))
        },
        currentTime.hour,
        currentTime.minute,
        true
    ).show()
}

private fun parseAmount(text: String): BigDecimal {
    val clean = text.filter { it.isDigit() || it == ',' || it == '.' }.trim()
    if (clean.isBlank()) return BigDecimal.ZERO
    val normalized = if (clean.contains(',')) {
        clean.replace(".", "").replace(',', '.')
    } else {
        clean
    }
    return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
}
