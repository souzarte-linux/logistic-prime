package com.fernando.centraldomotorista.ui.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.ui.theme.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCreateRoute: () -> Unit,
    onNavigateToCreateDailyTotal: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToFuelExpense: () -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var isCadastroExpanded by remember { mutableStateOf(true) }
    var selectedQuickExpenseCategory by remember { mutableStateOf<String?>(null) }
    var showExpenseBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearActionMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Cabeçalho do Usuário
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkAlt)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = uiState.profile?.fullName ?: "Central do Motorista",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = uiState.profile?.email ?: "Logística & Entregas",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Início
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null, tint = OrangeNeon) },
                        label = { Text("Início", fontWeight = FontWeight.Bold, color = Color.White) },
                        selected = true,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = OrangeNeon.copy(alpha = 0.15f)),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color.DarkGray.copy(alpha = 0.6f))

                    // Seção CADASTRO (Expansível / Colapsável, expandida por padrão)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isCadastroExpanded = !isCadastroExpanded },
                        color = OrangeNeon.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Ícone laranja de pasta
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(OrangeNeon.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Cadastro",
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Título em destaque + subtítulo
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "CADASTRO",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.5.sp,
                                    color = OrangeNeon
                                )
                                Text(
                                    text = "Empresas, Postos, Operadoras...",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Seta indicando expansível/colapsável
                            Icon(
                                imageVector = if (isCadastroExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isCadastroExpanded) "Recolher" else "Expandir",
                                tint = OrangeNeon,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Itens da Seção Cadastro
                    AnimatedVisibility(
                        visible = isCadastroExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // 🏢 Empresas
                            DrawerCadastroItem(
                                icon = Icons.Default.Business,
                                title = "Empresas",
                                subtitle = "Prestadoras de Serviços",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("empresas")
                                    }
                                }
                            )

                            // ⛽ Postos de Gasolina
                            DrawerCadastroItem(
                                icon = Icons.Default.LocalGasStation,
                                title = "Postos de Gasolina",
                                subtitle = "Postos e abastecimento",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("postos")
                                    }
                                }
                            )

                            // 🧾 Emissores
                            DrawerCadastroItem(
                                icon = Icons.Default.ReceiptLong,
                                title = "Emissores",
                                subtitle = "Instituições Emissoras dos Cartões",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("emissores")
                                    }
                                }
                            )

                            // 📱 Apps & Plataformas
                            DrawerCadastroItem(
                                icon = Icons.Default.Smartphone,
                                title = "Apps & Plataformas",
                                subtitle = "Plataformas de entrega e repasse",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("plataformas")
                                    }
                                }
                            )

                            // 💳 Bandeiras
                            DrawerCadastroItem(
                                icon = Icons.Default.CreditCard,
                                title = "Bandeiras",
                                subtitle = "Cartões e formas de pagamento",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("bandeiras")
                                    }
                                }
                            )

                            // 🔧 Monitoramento Peças
                            DrawerCadastroItem(
                                icon = Icons.Default.Build,
                                title = "Monitoramento Peças",
                                subtitle = "Controle de trocas e manutenção",
                                onClick = {
                                    coroutineScope.launch {
                                        drawerState.close()
                                        onNavigateToRoute("monitoramento-pecas")
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = Color.DarkGray.copy(alpha = 0.6f))

                    // Relatórios & Extrato
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.LightGray) },
                        label = { Text("Relatórios & Extrato", color = Color.White) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                onNavigateToReports()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Sair da Conta
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RedAlert) },
                        label = { Text("Sair da Conta", color = RedAlert, fontWeight = FontWeight.Bold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch {
                                drawerState.close()
                                onSignOut()
                            }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "CENTRAL DO MOTORISTA",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        // Sino com badge de notificações não lidas
                        Box(modifier = Modifier.padding(end = 4.dp)) {
                            IconButton(onClick = {
                                Toast.makeText(
                                    context,
                                    "Você tem ${uiState.notificacoesNaoLidas} notificações não lidas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificações",
                                    tint = Color.White
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

                        // Botão Sair
                        IconButton(onClick = onSignOut) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Sair",
                                tint = OrangeNeon
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToFuelExpense() },
                    containerColor = OrangeNeon,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.LocalGasStation, contentDescription = "Novo Abastecimento")
                }
            },
            containerColor = BackgroundDark
        ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        trackColor = SurfaceDark
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
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
                                text = "LUCRO LÍQUIDO HOJE",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            if (uiState.sessaoAtiva) {
                                Surface(
                                    color = GreenNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GreenNeon.copy(alpha = 0.5f))
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
                            color = OrangeNeon,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black
                        )

                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

                        // Rodapé Metas
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "META DIÁRIA",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = uiState.metaDiaria.formatCurrency(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "FALTAM",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (uiState.faltamParaMeta <= BigDecimal.ZERO) "Meta Atingida! 🎉" else uiState.faltamParaMeta.formatCurrency(),
                                    color = if (uiState.faltamParaMeta <= BigDecimal.ZERO) GreenNeon else OrangeNeon.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // C. Banner de Alerta de Manutenção
            if (uiState.alertaManutencao != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(RedAlert.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerta",
                                    tint = RedAlert
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ALERTA: ${uiState.alertaManutencao?.partName?.uppercase() ?: "PEÇA"}",
                                    color = RedAlert,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Você ultrapassou em ${uiState.kmUltrapassado} KM a vida útil de ${uiState.alertaManutencao?.lifeKm} KM.",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // D. Grid de 3 Ações
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 1. Card Grande Laranja - Lançar Ganhos por Rota
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCreateRoute() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = OrangeNeon)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "LANÇAR GANHOS POR ROTA",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "Registre corrida por km, tempo e valor",
                                        fontSize = 12.sp,
                                        color = Color.Black.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.Black
                            )
                        }
                    }

                    // 2 e 3 em linha: Lançar Total do Dia e Contas a Receber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Lançar Total do Dia
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToCreateDailyTotal() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "TOTAL DO DIA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Lançar valor bruto",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Contas a Receber
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToReports() },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = GreenNeon,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "A RECEBER",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = uiState.contasAReceber.formatCurrency(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = GreenNeon
                                )
                            }
                        }
                    }
                }
            }

            // E. Lançamento Rápido de Despesa
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "LANÇAMENTO RÁPIDO DE DESPESA",
                        color = Color.Gray,
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
                                selectedQuickExpenseCategory = "manutencao"
                                showExpenseBottomSheet = true
                            }
                        )
                        QuickExpenseButton(
                            title = "Alimentação",
                            icon = Icons.Default.Restaurant,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selectedQuickExpenseCategory = "alimentacao"
                                showExpenseBottomSheet = true
                            }
                        )
                    }
                }
            }

            // F. Rotas Recentes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROTAS RECENTES",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Recarregar",
                        color = OrangeNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.refresh() }
                    )
                }
            }

            if (uiState.rotasRecentes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToCreateRoute() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
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
                                tint = Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Nenhuma rota registrada ainda.",
                                color = Color.White,
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
                    RouteRecentItem(route)
                }
            }
        }
    }
}

    // Modal Bottom Sheet de Despesa Rápida
    if (showExpenseBottomSheet && selectedQuickExpenseCategory != null) {
        val category = selectedQuickExpenseCategory!!
        val categoryLabel = when (category) {
            "combustivel" -> "Combustível"
            "manutencao" -> "Manutenção"
            "alimentacao" -> "Alimentação"
            else -> "Despesa"
        }

        var amountText by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = {
                if (!isSubmitting) showExpenseBottomSheet = false
            },
            containerColor = SurfaceDark,
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Lançar Despesa: $categoryLabel",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeNeon
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.') },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amount = amountText.toBigDecimalOrNull()
                        if (amount != null && amount > BigDecimal.ZERO) {
                            isSubmitting = true
                            viewModel.createQuickExpense(category, amount) {
                                isSubmitting = false
                                showExpenseBottomSheet = false
                            }
                        } else {
                            Toast.makeText(context, "Digite um valor válido", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSubmitting && amountText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Confirmar Lançamento",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
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
        color = SurfaceDark,
        shape = RoundedCornerShape(12.dp)
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RouteRecentItem(route: Route) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val origin = route.origin?.ifBlank { "Origem não informada" } ?: "Rota rápida"
                val destination = route.destination?.ifBlank { "Destino" } ?: "Concluída"
                Text(
                    text = "$origin ➔ $destination",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = route.occurredAt.format(DateTimeFormatter.ofPattern("HH:mm - dd/MM")),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    if (route.distanceKm > BigDecimal.ZERO) {
                        Text(
                            text = "• ${route.distanceKm} km",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                    if (route.packageCount > 1) {
                        Text(
                            text = "• ${route.packageCount} pacotes",
                            fontSize = 11.sp,
                            color = Color.LightGray
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
fun DrawerCadastroItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone à esquerda dentro de um quadrado com cantos arredondados
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = SurfaceDarkAlt,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Título em branco/negrito, subtítulo em cinza claro abaixo
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Text(
                    text = subtitle,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Seta ">" à direita
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

