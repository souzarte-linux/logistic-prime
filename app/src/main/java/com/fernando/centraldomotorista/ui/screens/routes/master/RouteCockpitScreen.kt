package com.fernando.centraldomotorista.ui.screens.routes.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.data.model.StopStatus
import android.widget.Toast
import com.fernando.centraldomotorista.ui.screens.routes.master.components.DiscardEmptyRouteDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.EditStopDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.FinishRouteDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.RemoveStopConfirmDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.RouteProgressHero
import com.fernando.centraldomotorista.ui.screens.routes.master.components.RouteMapView
import com.fernando.centraldomotorista.ui.screens.routes.master.components.SearchBarcodeScannerDialog
import com.fernando.centraldomotorista.ui.screens.routes.master.components.StopDeliveryCard
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold
import com.fernando.centraldomotorista.util.NavigationIntentHelper

/**
 * Tela do Cockpit de Bordo veicular para o motorista Master.
 * Exibe painel com métricas de entrega, despacho rápido para Waze/Google Maps,
 * cards expansíveis/contraíveis (Prompt 5), atribuição a parceiros (Prompt 6)
 * e encerramento com rateio proporcional multi-plataforma (Prompt 8).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCockpitScreen(
    routeId: String,
    viewModel: RouteCockpitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToNewRouteWithHandOff: (handoffs: List<MasterRouteHandOffItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showDiscardEmptyDialog by remember { mutableStateOf(false) }
    var stopToRemove by remember { mutableStateOf<MasterRouteStop?>(null) }
    var stopToEdit by remember { mutableStateOf<MasterRouteStop?>(null) }
    var showSearchScannerDialog by remember { mutableStateOf(false) }
    var draggedStopId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(routeId) {
        viewModel.loadCockpit(routeId)
    }

    if (stopToEdit != null) {
        val stop = stopToEdit!!
        EditStopDialog(
            stop = stop,
            platforms = uiState.platforms,
            marketplaces = uiState.marketplaces,
            onDismiss = { stopToEdit = null },
            onSave = { name, addr, cep, pkgType, platId, mktName, notes ->
                viewModel.updateStop(
                    stop = stop,
                    recipientName = name,
                    fullAddress = addr,
                    cep = cep,
                    packageType = pkgType,
                    platformId = platId,
                    marketplaceName = mktName,
                    notes = notes,
                    onSuccess = {
                        stopToEdit = null
                        Toast.makeText(context, "Pacote #${stop.stopOrder} atualizado!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showSearchScannerDialog) {
        SearchBarcodeScannerDialog(
            onDismiss = { showSearchScannerDialog = false },
            onBarcodeScanned = { barcode ->
                viewModel.onSearchQueryChanged(barcode)
                val foundStop = uiState.stops.firstOrNull { it.barcode.equals(barcode, ignoreCase = true) }
                if (foundStop != null) {
                    viewModel.expandStop(foundStop.id)
                    Toast.makeText(context, "Pacote #${foundStop.stopOrder} localizado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Pacote $barcode não encontrado nesta rota!", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (stopToRemove != null) {
        val stop = stopToRemove!!
        RemoveStopConfirmDialog(
            stop = stop,
            onDismiss = { stopToRemove = null },
            onConfirmRemove = {
                stopToRemove = null
                viewModel.deleteStop(stop)
            }
        )
    }

    if (showDiscardEmptyDialog) {
        DiscardEmptyRouteDialog(
            isDiscarding = uiState.isFinishing,
            onDismiss = { showDiscardEmptyDialog = false },
            onConfirmDiscard = {
                viewModel.discardEmptyRoute {
                    showDiscardEmptyDialog = false
                    onNavigateBack()
                }
            }
        )
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
                viewModel.finishRoute(endKmInput = kmEnd) { handoffs ->
                    onNavigateToNewRouteWithHandOff(handoffs)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cockpit de Bordo",
                                fontSize = 12.sp,
                                color = OrangeNeon,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "• ${uiState.totalPackages} pacotes bipados",
                                fontSize = 11.sp,
                                color = TextSecondaryDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
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
                    IconButton(onClick = { viewModel.toggleMapVisibility() }) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = if (uiState.isMapVisible) "Ocultar Mapa" else "Ver Mapa",
                            tint = if (uiState.isMapVisible) OrangeNeon else TextSecondaryDark
                        )
                    }
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
                            onClick = {
                                if (uiState.totalPackages == 0 || uiState.stops.isEmpty()) {
                                    showDiscardEmptyDialog = true
                                } else {
                                    showFinishDialog = true
                                }
                            },
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
                val nextPendingStopId = uiState.activeStops.firstOrNull { it.status == StopStatus.PENDENTE }?.id

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

                    // --- 2. Barra de Busca de Paradas com Leitor de Código de Barras ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceDark,
                                border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(52.dp)
                                    .clickable { showSearchScannerDialog = true }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Escanear pacote para buscar",
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // --- 2.1 Mapa osmdroid Embutido (Prompt 9) ---
                    if (uiState.isMapVisible) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                            ) {
                                RouteMapView(
                                    stops = uiState.stops,
                                    startLat = uiState.route?.startLatitude?.toDouble(),
                                    startLng = uiState.route?.startLongitude?.toDouble(),
                                    onNavigateGps = { address ->
                                        NavigationIntentHelper.launchNavigation(
                                            context = context,
                                            address = address,
                                            preference = NavigationIntentHelper.NavAppPreference.ALWAYS_ASK
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // --- 3. Cabeçalho de Ações da Lista: Contador, Otimização e Expandir/Contrair (Prompts 5 e 9) ---
                    val activeStops = uiState.filteredActiveStops
                    val awaitingStops = uiState.filteredAwaitingConfirmationStops
                    val totalDisplayCount = activeStops.size + awaitingStops.size

                    if (totalDisplayCount > 0) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Status discreto de geocodificação em background
                                if (uiState.isGeocoding) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = OrangeNeon,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Obtendo coordenadas GPS das paradas...",
                                            fontSize = 11.sp,
                                            color = OrangeNeon,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$totalDisplayCount paradas listadas",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondaryDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Botão Otimizar Ordem (GPS) via Nearest Neighbor offline
                                        TextButton(
                                            onClick = { viewModel.optimizeStopsOrder() },
                                            enabled = !uiState.isOptimizing && totalDisplayCount > 1,
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            if (uiState.isOptimizing) {
                                                CircularProgressIndicator(
                                                    color = OrangeNeon,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text("Otimizando...", fontSize = 11.sp, color = OrangeNeon, maxLines = 1, softWrap = false)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                                    contentDescription = null,
                                                    tint = OrangeNeon,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.size(4.dp))
                                                Text("Otimizar", fontSize = 11.sp, color = OrangeNeon, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                            }
                                        }

                                        TextButton(
                                            onClick = { viewModel.expandAllStops() },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Expandir", fontSize = 11.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }

                                        TextButton(
                                            onClick = { viewModel.collapseAllStops() },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text("Contrair", fontSize = 11.sp, color = TextSecondaryDark, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 4. Lista de Paradas / Pacotes Ativos (Posse direta do Master) ---
                    if (totalDisplayCount == 0) {
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
                        items(activeStops, key = { it.id }) { stop ->
                            val isDraggingThis = draggedStopId == stop.id
                            StopDeliveryCard(
                                stop = stop,
                                isNext = (stop.id == nextPendingStopId),
                                isExpanded = uiState.expandedStopIds.contains(stop.id),
                                onToggleExpand = { viewModel.toggleStopExpanded(stop.id) },
                                onNavigateGps = { address ->
                                    NavigationIntentHelper.launchNavigation(
                                        context = context,
                                        address = address,
                                        preference = NavigationIntentHelper.NavAppPreference.ALWAYS_ASK
                                    )
                                },
                                onUpdateStatus = { stopId, newStatus ->
                                    viewModel.updateStopStatus(stopId, newStatus)
                                },
                                onAssignToPartner = { partnerId ->
                                    viewModel.assignStopToPartner(stop.id, partnerId)
                                },
                                onEditStop = { stopToEdit = it },
                                onDeleteStop = { stopToRemove = it },
                                partners = uiState.deliveryPartners,
                                dragHandle = {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Arrastar para reordenar parada",
                                        tint = if (isDraggingThis) OrangeNeon else TextSecondaryDark.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .pointerInput(stop.id) {
                                                detectVerticalDragGestures(
                                                    onDragStart = {
                                                        draggedStopId = stop.id
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedStopId = null
                                                        dragOffsetY = 0f
                                                        viewModel.saveReorderedStops()
                                                    },
                                                    onDragCancel = {
                                                        draggedStopId = null
                                                        dragOffsetY = 0f
                                                    },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetY += dragAmount
                                                        val activeIndex = activeStops.indexOfFirst { it.id == stop.id }
                                                        val thresholdPx = 130f
                                                        if (dragOffsetY > thresholdPx && activeIndex in 0 until activeStops.size - 1) {
                                                            val currentFullIndex = uiState.stops.indexOfFirst { it.id == stop.id }
                                                            val targetStop = activeStops[activeIndex + 1]
                                                            val targetFullIndex = uiState.stops.indexOfFirst { it.id == targetStop.id }
                                                            if (currentFullIndex >= 0 && targetFullIndex >= 0) {
                                                                viewModel.moveStop(currentFullIndex, targetFullIndex)
                                                                dragOffsetY -= thresholdPx
                                                            }
                                                        } else if (dragOffsetY < -thresholdPx && activeIndex > 0) {
                                                            val currentFullIndex = uiState.stops.indexOfFirst { it.id == stop.id }
                                                            val targetStop = activeStops[activeIndex - 1]
                                                            val targetFullIndex = uiState.stops.indexOfFirst { it.id == targetStop.id }
                                                            if (currentFullIndex >= 0 && targetFullIndex >= 0) {
                                                                viewModel.moveStop(currentFullIndex, targetFullIndex)
                                                                dragOffsetY += thresholdPx
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                                    .then(
                                        if (isDraggingThis) {
                                            Modifier
                                                .zIndex(2f)
                                                .graphicsLayer { translationY = dragOffsetY }
                                        } else {
                                            Modifier.zIndex(1f)
                                        }
                                    )
                            )
                        }

                        // --- 5. Seção "Aguardando Confirmação do Parceiro" (Prompt 6) ---
                        if (awaitingStops.isNotEmpty()) {
                            item {
                                Surface(
                                    color = SurfaceDarkAlt,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, YellowGold.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = YellowGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Aguardando Confirmação do Parceiro (${awaitingStops.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = YellowGold
                                        )
                                    }
                                }
                            }

                            items(awaitingStops, key = { it.id }) { stop ->
                                StopDeliveryCard(
                                    stop = stop,
                                    isNext = false,
                                    isExpanded = uiState.expandedStopIds.contains(stop.id),
                                    onToggleExpand = { viewModel.toggleStopExpanded(stop.id) },
                                    onNavigateGps = { address ->
                                        NavigationIntentHelper.launchNavigation(
                                            context = context,
                                            address = address,
                                            preference = NavigationIntentHelper.NavAppPreference.ALWAYS_ASK
                                        )
                                    },
                                    onUpdateStatus = { stopId, newStatus ->
                                        viewModel.updateStopStatus(stopId, newStatus)
                                    },
                                    onAssignToPartner = { partnerId ->
                                        viewModel.assignStopToPartner(stop.id, partnerId)
                                    },
                                    onEditStop = { stopToEdit = it },
                                    onDeleteStop = { stopToRemove = it },
                                    partners = uiState.deliveryPartners
                                )
                            }
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
