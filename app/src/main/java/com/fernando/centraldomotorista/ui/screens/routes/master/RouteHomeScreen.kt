package com.fernando.centraldomotorista.ui.screens.routes.master

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.MasterDeliveryRoute
import com.fernando.centraldomotorista.data.model.RouteStatus
import com.fernando.centraldomotorista.ui.screens.routes.master.components.StartRouteDestination
import com.fernando.centraldomotorista.ui.screens.routes.master.components.StartRouteDialog
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import java.time.format.DateTimeFormatter

/**
 * Hub principal da aba "Rota" (5ª aba da barra de navegação).
 * Gerencia a alternância entre rota ativa em andamento e estado vazio de nova rota diária.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteHomeScreen(
    viewModel: RouteHomeViewModel,
    onNavigateToScanner: (String) -> Unit,
    onNavigateToCockpit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showStartDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            snackbarHostState.showSnackbar(message = err)
        }
    }

    if (showStartDialog) {
        StartRouteDialog(
            platforms = uiState.platforms,
            isCreating = uiState.isCreatingRoute,
            errorMessage = uiState.error,
            onDismiss = {
                viewModel.clearError()
                showStartDialog = false
            },
            onConfirmStart = { platformId, startLocation, lat, lng, destination ->
                viewModel.createNewRoute(
                    platformId = platformId,
                    startLocation = startLocation,
                    startLat = lat,
                    startLng = lng,
                    onSuccess = { newRouteId ->
                        showStartDialog = false
                        when (destination) {
                            StartRouteDestination.SCANNER -> onNavigateToScanner(newRouteId)
                            StartRouteDestination.COCKPIT -> onNavigateToCockpit(newRouteId)
                        }
                    }
                )
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Central do Motorista",
                            style = MaterialTheme.typography.bodySmall,
                            color = OrangeNeon,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Rota de Entregas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar",
                            tint = TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.activeRoute == null && uiState.recentRoutes.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = OrangeNeon
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- SEÇÃO 1: ROTA ATIVA (se houver) ---
                    val activeRoute = uiState.activeRoute
                    if (activeRoute != null && activeRoute.status == RouteStatus.EM_ANDAMENTO) {
                        item {
                            ActiveRouteHeroCard(
                                route = activeRoute,
                                platformName = uiState.platforms.firstOrNull { it.id == activeRoute.platformId }?.name,
                                onOpenCockpit = { onNavigateToCockpit(activeRoute.id) },
                                onOpenScanner = { onNavigateToScanner(activeRoute.id) }
                            )
                        }
                    } else {
                        // --- SEÇÃO 2: ESTADO VAZIO (Nenhuma rota ativa hoje) ---
                        item {
                            EmptyRouteHeroCard(
                                isCreating = uiState.isCreatingRoute,
                                onStartNewRoute = {
                                    viewModel.clearError()
                                    showStartDialog = true
                                }
                            )
                        }
                    }

                    // --- SEÇÃO 3: HISTÓRICO DE ROTAS ANTERIORES ---
                    item {
                        Text(
                            text = "ROTAS ANTERIORES",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    val pastRoutes = uiState.recentRoutes.filter { it.id != activeRoute?.id }
                    if (pastRoutes.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nenhuma rota anterior encontrada.",
                                    fontSize = 13.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.padding(20.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(pastRoutes, key = { it.id }) { pastRoute ->
                            PastRouteCard(
                                route = pastRoute,
                                platformName = uiState.platforms.firstOrNull { it.id == pastRoute.platformId }?.name,
                                onClick = { onNavigateToCockpit(pastRoute.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card Hero de destaque para Rota Ativa em Andamento.
 */
@Composable
private fun ActiveRouteHeroCard(
    route: MasterDeliveryRoute,
    platformName: String?,
    onOpenCockpit: () -> Unit,
    onOpenScanner: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.5.dp, OrangeNeon),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = OrangeNeon.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🚚 ROTA EM ANDAMENTO",
                        color = OrangeNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = route.routeDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = platformName ?: "Plataforma Geral",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )

            Text(
                text = "🏁 Partida: ${route.startLocation}",
                fontSize = 13.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = SurfaceDarkAlt,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${route.totalPackages}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Total Pacotes",
                            fontSize = 10.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                Surface(
                    color = GreenNeon.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${route.deliveredPackages}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                        Text(
                            text = "Entregues",
                            fontSize = 10.sp,
                            color = GreenNeon
                        )
                    }
                }

                Surface(
                    color = RedAlert.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${route.returnedPackages}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert
                        )
                        Text(
                            text = "Devoluções",
                            fontSize = 10.sp,
                            color = RedAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Botão Principal: Continuar no Cockpit
            Button(
                onClick = onOpenCockpit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IR PARA O COCKPIT DE BORDO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botão Secundário: Bipar Mais
            OutlinedButton(
                onClick = onOpenScanner,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TextSecondaryDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Abrir Câmera de Bipagem",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Card Hero para Estado Vazio (Sem Rota Ativa Hoje).
 */
@Composable
private fun EmptyRouteHeroCard(
    isCreating: Boolean,
    onStartNewRoute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = OrangeNeon.copy(alpha = 0.15f),
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Nenhuma Rota Ativa Hoje",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Inicie sua rota no galpão, bipe os pacotes com a câmera ML Kit e acompanhe cada entrega com despacho para Waze e Google Maps.",
                fontSize = 13.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartNewRoute,
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CRIAR NOVA ROTA DO DIA",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Card compacto de rota anterior finalizada.
 */
@Composable
private fun PastRouteCard(
    route: MasterDeliveryRoute,
    platformName: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = route.routeDate.format(DateTimeFormatter.ofPattern("dd/MM")),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon
                    )
                    Text(
                        text = " • ${platformName ?: "Geral"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "📦 ${route.totalPackages} pacotes (${route.deliveredPackages} entregues • ${route.returnedPackages} devoluções)",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )

                Text(
                    text = "🏁 ${route.startLocation}",
                    fontSize = 11.sp,
                    color = TextSecondaryDark.copy(alpha = 0.7f)
                )
            }

            Surface(
                color = when (route.status) {
                    RouteStatus.CONCLUIDA -> GreenNeon.copy(alpha = 0.15f)
                    RouteStatus.CANCELADA -> RedAlert.copy(alpha = 0.15f)
                    RouteStatus.EM_ANDAMENTO -> OrangeNeon.copy(alpha = 0.15f)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = when (route.status) {
                        RouteStatus.CONCLUIDA -> "Concluída"
                        RouteStatus.CANCELADA -> "Cancelada"
                        RouteStatus.EM_ANDAMENTO -> "Em Andamento"
                    },
                    color = when (route.status) {
                        RouteStatus.CONCLUIDA -> GreenNeon
                        RouteStatus.CANCELADA -> RedAlert
                        RouteStatus.EM_ANDAMENTO -> OrangeNeon
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
