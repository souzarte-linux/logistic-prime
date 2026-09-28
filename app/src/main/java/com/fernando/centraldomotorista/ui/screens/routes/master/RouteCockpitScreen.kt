package com.fernando.centraldomotorista.ui.screens.routes.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.ui.screens.routes.master.components.FinishRouteDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.RouteProgressHero
import com.fernando.centraldomotorista.ui.screens.routes.master.components.StopDeliveryCard
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.util.NavigationIntentHelper

/**
 * Tela do Cockpit de Bordo veicular para o motorista Master.
 * Exibe painel com métricas de entrega, despacho rápido para Waze/Google Maps
 * e atualização instantânea do status de cada entrega.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCockpitScreen(
    routeId: String,
    viewModel: RouteCockpitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToNewRouteWithHandOff: (platformId: String?, deliveredPackages: Int, origin: String?, masterRouteId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        viewModel.loadCockpit(routeId)
    }

    if (showFinishDialog) {
        FinishRouteDialog(
            platformName = uiState.platformName,
            totalPackages = uiState.totalPackages,
            deliveredCount = uiState.deliveredCount,
            returnedCount = uiState.returnedCount,
            onDismiss = { showFinishDialog = false },
            onFinishAndLaunchEarnings = { kmEnd ->
                showFinishDialog = false
                viewModel.finishRoute {
                    onNavigateToNewRouteWithHandOff(
                        uiState.route?.platformId,
                        uiState.deliveredCount,
                        uiState.route?.startLocation,
                        routeId
                    )
                }
            },
            onFinishOnly = {
                showFinishDialog = false
                viewModel.finishRoute {
                    onNavigateBack()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cockpit de Bordo",
                            fontSize = 12.sp,
                            color = OrangeNeon,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.platformName ?: "Rota Operacional",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToScanner(routeId) }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Bipar Mais",
                            tint = OrangeNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        bottomBar = {
            val isRouteActive = uiState.route?.status == RouteStatus.EM_ANDAMENTO
            if (isRouteActive) {
                Surface(
                    color = BackgroundDark,
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { showFinishDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeNeon,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "CONCLUIR ROTA DO DIA (FECHAMENTO)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = OrangeNeon
                )
            } else {
                val nextPendingStopId = uiState.stops.firstOrNull { it.status == StopStatus.PENDENTE }?.id

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- 1. Hero Card de Progresso ---
                    item {
                        RouteProgressHero(
                            platformName = uiState.platformName,
                            startLocation = uiState.route?.startLocation ?: "Galpão Base",
                            totalPackages = uiState.totalPackages,
                            deliveredCount = uiState.deliveredCount,
                            pendingCount = uiState.pendingCount,
                            returnedCount = uiState.returnedCount,
                            onAddMoreStops = { onNavigateToScanner(routeId) }
                        )
                    }

                    // --- 2. Barra de Busca de Paradas ---
                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Buscar por endereço, cliente ou código...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Limpar busca",
                                            tint = TextSecondaryDark,
                                            modifier = Modifier.size(18.dp)
                                        )
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // --- 3. Lista de Paradas / Pacotes ---
                    val displayStops = uiState.filteredStops
                    if (displayStops.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (uiState.searchQuery.isNotBlank()) "Nenhuma parada corresponde à busca." else "Nenhum pacote bipado ainda nesta rota.",
                                        fontSize = 13.sp,
                                        color = TextSecondaryDark,
                                        textAlign = TextAlign.Center
                                    )
                                    if (uiState.searchQuery.isBlank()) {
                                        Button(
                                            onClick = { onNavigateToScanner(routeId) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = OrangeNeon,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCodeScanner,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text("Abrir Scanner e Bipar Pacotes", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(displayStops, key = { it.id }) { stop ->
                            StopDeliveryCard(
                                stop = stop,
                                isNext = (stop.id == nextPendingStopId),
                                onNavigateGps = { address ->
                                    NavigationIntentHelper.launchNavigation(
                                        context = context,
                                        address = address,
                                        preference = NavigationIntentHelper.NavAppPreference.ALWAYS_ASK
                                    )
                                },
                                onUpdateStatus = { stopId, newStatus ->
                                    viewModel.updateStopStatus(stopId, newStatus)
                                }
                            )
                        }
                    }

                    // Espaço extra no final para não cobrir pelo botão fixo
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}
