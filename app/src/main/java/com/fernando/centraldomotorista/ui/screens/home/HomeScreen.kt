package com.fernando.centraldomotorista.ui.screens.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.data.repository.PartMaintenanceAlertItem
import com.fernando.centraldomotorista.data.repository.ReceivableItem
import com.fernando.centraldomotorista.data.repository.TipoAlertaManutencao
import com.fernando.centraldomotorista.navigation.Screen
import com.fernando.centraldomotorista.ui.components.RouteDetailsDialog
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.net.URL
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkMode: Boolean = true,
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToCreateRoute: () -> Unit,
    onNavigateToCreateDailyTotal: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToFuelExpense: () -> Unit,
    onNavigateToMealExpense: () -> Unit = {},
    onNavigateToMaintenanceExpense: () -> Unit,
    onNavigateToCreateMaintenanceFromAlert: (PartMaintenance) -> Unit = {},
    onNavigateToEditMaintenance: (PartMaintenance) -> Unit = onNavigateToCreateMaintenanceFromAlert,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToHistorico: () -> Unit = { onNavigateToRoute(Screen.Historico.route) },
    onNavigateToDeliveryPartners: () -> Unit = { onNavigateToRoute(Screen.DeliveryPartners.route) },
    onNavigateToFaturas: () -> Unit = { onNavigateToRoute("faturas") },
    onSignOut: () -> Unit,
    onOpenDrawer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvents.collect {
            listState.scrollToItem(0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                coroutineScope.launch {
                    listState.scrollToItem(0)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var showReceivablesModal by remember { mutableStateOf(false) }
    var showEditDailyGoalModal by remember { mutableStateOf(false) }
    var selectedRouteForDetails by remember { mutableStateOf<Route?>(null) }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CENTRAL DO MOTORISTA",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                    actions = {
                        // Sino com badge de notificações não lidas
                        Box(modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(onClick = { showNotificationsModal = true }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificações",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (uiState.notificacoesNaoLidas > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(16.dp)
                                        .background(RedAlert, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = uiState.notificacoesNaoLidas.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Botão Sair com confirmação
                        IconButton(onClick = { showLogoutConfirmation = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sair",
                                tint = OrangeNeon
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToCreateRoute() },
                    containerColor = OrangeNeon,
                    contentColor = Color.Black,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "Ganhos",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    },
                    shape = CircleShape
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.loading && (uiState.rotasRecentes.isNotEmpty() || uiState.profile != null),
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState.loading && uiState.rotasRecentes.isEmpty() && uiState.profile == null) {
                        HomeSkeletonLoading()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                        ) {
                            // A. Indicador de Carregamento / Erro
                            if (uiState.loading) {
                                item {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = OrangeNeon,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }

                            if (uiState.error != null) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.15f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RedAlert)
                                            Text(
                                                text = uiState.error ?: "",
                                                color = RedAlert,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            TextButton(onClick = { viewModel.refresh() }) {
                                                Text("Recarregar", color = OrangeNeon, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // B. Card de Lucro Líquido Hoje
                            item {
                                val isLucroNegativo = uiState.lucroHoje < BigDecimal.ZERO
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isLucroNegativo) "SALDO DO DIA (A RECUPERAR)" else "LUCRO LÍQUIDO HOJE",
                                                color = if (isLucroNegativo) RedAlert else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                            if (uiState.sessaoAtiva) {
                                                Surface(
                                                    color = GreenNeon.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(100.dp),
                                                    border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(GreenNeon, CircleShape)
                                                        )
                                                        Text(
                                                            text = "SESSÃO ATIVA",
                                                            color = GreenNeon,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Valor Grande em Destaque
                                        Text(
                                            text = uiState.lucroHoje.formatCurrency(),
                                            color = if (isLucroNegativo) RedAlert else OrangeNeon,
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Black
                                        )

                                        // Detalhamento de Entradas vs Saídas do Dia (Item 2.1)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Entradas (Ganhos)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .background(GreenNeon.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                        contentDescription = "Entradas",
                                                        tint = GreenNeon,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "ENTRADAS",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "+ ${uiState.ganhosHoje.formatCurrency()}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = GreenNeon
                                                    )
                                                }
                                            }

                                            VerticalDivider(
                                                modifier = Modifier.height(28.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            )

                                            // Saídas (Despesas)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .background(RedAlert.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                                        contentDescription = "Saídas",
                                                        tint = RedAlert,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = "SAÍDAS",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "- ${uiState.despesasHoje.formatCurrency()}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = RedAlert
                                                    )
                                                }
                                            }
                                        }

                                        // Termômetro de Meta do Dia
                                        val metaVal = uiState.metaDiaria
                                        val lucroVal = uiState.lucroHoje
                                        val rawPercent = if (metaVal > BigDecimal.ZERO && !isLucroNegativo) (lucroVal.toFloat() / metaVal.toFloat()) else 0f
                                        val clampedProgress = rawPercent.coerceIn(0f, 1f)
                                        val animatedProgress by animateFloatAsState(
                                            targetValue = clampedProgress,
                                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                                            label = "thermometerProgress"
                                        )
                                        val percentText = if (isLucroNegativo) "0" else (rawPercent * 100).toInt().coerceAtLeast(0).toString()

                                        val thermometerColor = when {
                                            isLucroNegativo -> RedAlert
                                            clampedProgress >= 0.85f -> GreenNeon                     // Verde
                                            clampedProgress >= 0.55f -> Color(0xFFFFD600)            // Amarelo
                                            clampedProgress >= 0.25f -> Color(0xFFFF8A00)            // Amarelo-Alaranjado
                                            else -> RedAlert                                         // Vermelho
                                        }

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "TERMÔMETRO DA META",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = if (isLucroNegativo) "Custos superam ganhos" else "$percentText% atingido",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = thermometerColor
                                                )
                                            }

                                            // Linha / Barra do Termômetro
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(100.dp))
                                                    .clip(RoundedCornerShape(100.dp))
                                            ) {
                                                val gradientColors = listOf(
                                                    Color(0xFFE53935), // Vermelho
                                                    Color(0xFFFF8A00), // Amarelo-alaranjado
                                                    Color(0xFFFFD600), // Amarelo
                                                    Color(0xFF00E676)  // Verde
                                                )

                                                if (animatedProgress > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .fillMaxWidth(animatedProgress)
                                                            .background(
                                                                Brush.horizontalGradient(colors = gradientColors),
                                                                RoundedCornerShape(100.dp)
                                                            )
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

                                        // Rodapé Metas com Edição Rápida (Item 2.2)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "META DIÁRIA",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { showEditDailyGoalModal = true }
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = uiState.metaDiaria.formatCurrency(),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Editar meta diária",
                                                        tint = OrangeNeon,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "FALTAM",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = if (uiState.faltamParaMeta <= BigDecimal.ZERO && !isLucroNegativo) "Meta Atingida! 🎉" else uiState.faltamParaMeta.formatCurrency(),
                                                    color = if (uiState.faltamParaMeta <= BigDecimal.ZERO && !isLucroNegativo) GreenNeon else if (isLucroNegativo) RedAlert else OrangeNeon.copy(alpha = 0.9f),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // C. Banners de Alerta de Manutenção Proativo (75% a 94% Amarelo, 95%+ Vermelho)
                            val alertsToDisplay = if (uiState.alertasManutencao.isNotEmpty()) {
                                uiState.alertasManutencao
                            } else if (uiState.alertaManutencao != null && uiState.tipoAlertaManutencao != TipoAlertaManutencao.NENHUM) {
                                listOf(
                                    PartMaintenanceAlertItem(
                                        part = uiState.alertaManutencao!!,
                                        tipoAlerta = uiState.tipoAlertaManutencao,
                                        percentage = 0,
                                        kmRemaining = uiState.kmManutencao,
                                        kmOverdue = uiState.kmManutencao,
                                        isOverdue = uiState.tipoAlertaManutencao == TipoAlertaManutencao.CRITICO
                                    )
                                )
                            } else {
                                emptyList()
                            }

                            if (alertsToDisplay.isNotEmpty()) {
                                items(alertsToDisplay, key = { "alerta_manutencao_${it.part.id}" }) { itemAlerta ->
                                    val alerta = itemAlerta.part
                                    val isCritico = itemAlerta.tipoAlerta == TipoAlertaManutencao.CRITICO
                                    val alertColor = if (isCritico) RedAlert else Color(0xFFFFB300)
                                    val alertBgColor = if (isCritico) RedAlert.copy(alpha = 0.15f) else Color(0xFFFFD600).copy(alpha = 0.12f)
                                    val alertBorderColor = if (isCritico) RedAlert.copy(alpha = 0.5f) else Color(0xFFFFB300).copy(alpha = 0.5f)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { onNavigateToCreateMaintenanceFromAlert(alerta) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = alertBgColor),
                                        border = BorderStroke(1.dp, alertBorderColor)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(alertColor.copy(alpha = 0.2f), CircleShape),
                                                 contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isCritico) Icons.Default.Warning else Icons.Default.WarningAmber,
                                                    contentDescription = "Alerta",
                                                    tint = alertColor
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isCritico) "ALERTA: ${alerta.partName.uppercase()}" else "AVISO PREVENTIVO: ${alerta.partName.uppercase()}",
                                                        color = alertColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (itemAlerta.percentage > 0) {
                                                        Surface(
                                                             color = alertColor.copy(alpha = 0.2f),
                                                             shape = RoundedCornerShape(4.dp)
                                                         ) {
                                                             Text(
                                                                 text = "${itemAlerta.percentage}%",
                                                                 color = alertColor,
                                                                 fontWeight = FontWeight.Black,
                                                                 fontSize = 11.sp,
                                                                 modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                             )
                                                         }
                                                     }
                                                 }
                                                 Text(
                                                     text = when {
                                                         itemAlerta.isOverdue ->
                                                             "Você ultrapassou em ${itemAlerta.kmOverdue.toPlainString()} KM a vida útil de ${alerta.lifeKm.toPlainString()} KM."
                                                         isCritico ->
                                                             "Faltam apenas ${itemAlerta.kmRemaining.toPlainString()} KM para a troca (vida útil de ${alerta.lifeKm.toPlainString()} KM)."
                                                         else ->
                                                             "Faltam ${itemAlerta.kmRemaining.toPlainString()} KM para a troca preventiva (vida útil de ${alerta.lifeKm.toPlainString()} KM)."
                                                     },
                                                     color = MaterialTheme.colorScheme.onSurface,
                                                     fontSize = 12.sp
                                                 )
                                                 Text(
                                                     text = "Toque para lançar nova troca / manutenção ➔",
                                                     color = OrangeNeon,
                                                     fontSize = 11.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     modifier = Modifier.padding(top = 4.dp)
                                                 )
                                             }
                                             Icon(
                                                 imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                 contentDescription = "Lançar Nova Troca / Manutenção",
                                                 tint = OrangeNeon,
                                                 modifier = Modifier.size(24.dp)
                                             )
                                         }
                                     }
                                }
                            }

                            // D. Grid de 4 Ações (Grade 2x2)
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Linha 1: Lançar Ganhos por Rota e Total do Dia
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Lançar Ganhos por Rota (Hero Primary Action)
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

                                        // 2. Total do Dia
                                        HomeActionCard(
                                            title = "TOTAL DO DIA",
                                            subtitle = "Lançar valor bruto",
                                            icon = Icons.Default.CalendarToday,
                                            iconTint = OrangeNeon,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onClick = { onNavigateToCreateDailyTotal() }
                                        )
                                    }

                                    // Linha 2: Contas a Receber e Entregadores Parceiros
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 3. Contas a Receber (Faturas)
                                        HomeActionCard(
                                            title = "A RECEBER",
                                            subtitle = uiState.contasAReceber.formatCurrency(),
                                            subtitleColor = GreenNeon,
                                            subtitleFontWeight = FontWeight.Bold,
                                            icon = Icons.Default.AccountBalanceWallet,
                                            iconTint = GreenNeon,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onClick = { onNavigateToFaturas() }
                                        )

                                        // 4. Entregadores Parceiros
                                        HomeActionCard(
                                            title = "ENTREGADORES PARCEIROS",
                                            subtitle = "Gerenciar equipe e sessões de entrega",
                                            icon = Icons.Default.Groups,
                                            iconTint = OrangeNeon,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            onClick = { onNavigateToDeliveryPartners() }
                                        )
                                    }
                                }
                            }

                            // E. Lançamento Rápido de Despesa
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "LANÇAMENTO RÁPIDO DE DESPESA",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        QuickExpenseButton(
                                            title = "Combustível",
                                            icon = Icons.Default.LocalGasStation,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                onNavigateToFuelExpense()
                                            }
                                        )
                                        QuickExpenseButton(
                                            title = "Manutenção",
                                            icon = Icons.Default.Build,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                onNavigateToMaintenanceExpense()
                                            }
                                        )
                                        QuickExpenseButton(
                                            title = "Alimentação",
                                            icon = Icons.Default.Restaurant,
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                onNavigateToMealExpense()
                                            }
                                        )
                                    }
                                }
                            }

                            // F. Rotas Recentes (Item 2.6 - Com Identificação da Plataforma)
                            item {
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
                                            .clickable { onNavigateToHistorico() }
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
                            }

                            if (uiState.rotasRecentes.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToCreateRoute() },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Route,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Text(
                                                text = "Nenhuma rota registrada ainda.",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "Toque em 'Lançar Ganhos por Rota' para começar.",
                                                color = OrangeNeon,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(uiState.rotasRecentes) { route ->
                                    RouteRecentItem(
                                        route = route,
                                        platformName = route.platformId?.let { uiState.plataformasMap[it] },
                                        onClick = { selectedRouteForDetails = route }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    // Diálogo de Confirmação para Logout Seguro
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = {
                Text(
                    text = "Sair da Conta",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Tem certeza de que deseja sair da sua conta? Será necessário fazer login novamente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmation = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedAlert,
                        contentColor = Color.White
                    )
                ) {
                    Text("Sair", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Bottom Sheet de Notificações
    if (showNotificationsModal) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Notificações",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (uiState.notificacoes.isNotEmpty()) {
                        Surface(
                            color = RedAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = "${uiState.notificacoes.size} pendentes",
                                color = RedAlert,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (uiState.notificacoes.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenNeon,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tudo em dia!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Você não possui notificações pendentes no momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.notificacoes.forEach { notif ->
                            val notifTitle = when (notif.type) {
                                "billing_cycle_closed" -> "Ciclo de faturamento concluído"
                                "maintenance_due" -> "Alerta de manutenção preventiva"
                                "payment_received" -> "Pagamento recebido"
                                else -> "Aviso do sistema"
                            }
                            val notifDescription = when (notif.type) {
                                "billing_cycle_closed" -> "Um ciclo de faturamento foi fechado e está aguardando repasse."
                                "maintenance_due" -> "Uma ou mais peças do seu veículo atingiram o limite de km."
                                "payment_received" -> "O repasse de faturamento foi processado com sucesso."
                                else -> "Notificação referente à sua conta de motorista."
                            }

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (notif.type) {
                                                "maintenance_due" -> Icons.Default.Build
                                                "billing_cycle_closed", "payment_received" -> Icons.Default.AttachMoney
                                                else -> Icons.Default.Notifications
                                            },
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notifTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = notifDescription,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Ícone de Check verde para marcar como lida
                                    IconButton(
                                        onClick = { viewModel.markNotificationAsRead(notif.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Marcar como lida",
                                            tint = GreenNeon,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Edição Rápida da Meta Diária (Item 2.2)
    if (showEditDailyGoalModal) {
        var goalText by remember { mutableStateOf(uiState.metaDiaria.toPlainString()) }
        AlertDialog(
            onDismissRequest = { showEditDailyGoalModal = false },
            title = {
                Text(
                    text = "Editar Meta Diária",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Defina seu objetivo diário de faturamento para calcular o termômetro de desempenho.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it },
                        label = { Text("Meta Diária (R$)") },
                        prefix = { Text("R$ ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            cursorColor = OrangeNeon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = goalText.replace(",", ".").trim().toBigDecimalOrNull()
                        if (parsed != null && parsed >= BigDecimal.ZERO) {
                            viewModel.updateDailyGoal(parsed)
                            showEditDailyGoalModal = false
                        } else {
                            Toast.makeText(context, "Digite um valor válido.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDailyGoalModal = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Informativo no Card "A RECEBER" (Item 2.4)
    if (showReceivablesModal) {
        ModalBottomSheet(
            onDismissRequest = { showReceivablesModal = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabeçalho
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
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GreenNeon,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Contas a Receber",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = GreenNeon.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(
                            text = uiState.contasAReceber.formatCurrency(),
                            color = GreenNeon,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = "Lançamentos com repasse pendente vinculados aos ciclos de faturamento das plataformas:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                if (uiState.itensAReceber.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GreenNeon,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "Tudo em dia!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nenhum repasse pendente registrado no momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.itensAReceber) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (item.platformName != null) {
                                                Surface(
                                                    color = OrangeNeon.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = item.platformName,
                                                        color = OrangeNeon,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val formattedDate = item.date
                                            .atZoneSameInstant(java.time.ZoneId.systemDefault())
                                            .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                                        Text(
                                            text = formattedDate,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = item.amount.formatCurrency(),
                                        color = GreenNeon,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        showReceivablesModal = false
                        onNavigateToReports()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Relatórios & Extratos Completos", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal de Detalhes da Rota com Botão Editar no Topo Direito
    selectedRouteForDetails?.let { route ->
        RouteDetailsDialog(
            route = route,
            platformName = route.platformId?.let { uiState.plataformasMap[it] },
            onDismiss = { selectedRouteForDetails = null },
            onEdit = { r ->
                selectedRouteForDetails = null
                onNavigateToRoute("lancar_rota?itemId=${r.id}")
            }
        )
    }
}

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

@Composable
fun QuickExpenseButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = OrangeNeon,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RouteRecentItem(
    route: Route,
    platformName: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (platformName != null) {
                        Surface(
                            color = OrangeNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = platformName,
                                color = OrangeNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val origin = route.origin?.ifBlank { "Origem não informada" } ?: "Rota rápida"
                    val destination = route.destination?.ifBlank { "Destino" } ?: "Concluída"
                    Text(
                        text = "$origin ➔ $destination",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formattedDateTime = route.occurredAt
                        .atZoneSameInstant(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("HH:mm - dd/MM"))
                    Text(
                        text = formattedDateTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (route.distanceKm > BigDecimal.ZERO) {
                        Text(
                            text = "• ${route.distanceKm} km",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (route.packageCount > 1) {
                        Text(
                            text = "• ${route.packageCount} pacotes",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = route.amount.formatCurrency(),
                color = GreenNeon,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun HomeSkeletonLoading() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Lucro Skeleton
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
            )
        ) {}

        // Action Cards Skeleton (Grade 2x2)
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                    )
                ) {}
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(115.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                    )
                ) {}
            }
        }

        // Quick Expenses Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                    )
                ) {}
            }
        }

        // Recent Routes Skeleton
        repeat(2) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
                )
            ) {}
        }
    }
}

