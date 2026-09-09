package com.fernando.centraldomotorista.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fernando.centraldomotorista.auth.AuthViewModel
import com.fernando.centraldomotorista.auth.GoogleAuthClient
import com.fernando.centraldomotorista.data.model.PartMaintenance
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.ui.screens.home.HomeScreen
import com.fernando.centraldomotorista.ui.screens.home.HomeViewModel
import com.fernando.centraldomotorista.ui.screens.login.LoginScreen
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.fernando.centraldomotorista.auth.BiometricAuthHelper
import io.github.jan.supabase.auth.auth

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Inicio : Screen("inicio", "Início", Icons.Default.Home)
    object Painel : Screen("painel", "Painel", Icons.Default.BarChart)
    object Relatorios : Screen("relatorios", "Relatórios", Icons.Default.Assessment)
    object Historico : Screen("historico", "Histórico", Icons.Default.History)
    
    // Actions & Forms
    object LancarRota : Screen("lancar_rota", "Lançar Rota", Icons.Default.Navigation)
    object LancarTotalDia : Screen("lancar_total_dia", "Total do Dia", Icons.Default.CalendarToday)
    object FuelExpense : Screen("fuel_expense", "Novo Abastecimento", Icons.Default.LocalGasStation)
    object MealExpense : Screen("meal_expense", "Lançamento de Alimentação", Icons.Default.Restaurant)
    object LancarManutencao : Screen("lancar-manutencao", "Lançar Manutenção", Icons.Default.Build)
    
    // Menu Lateral - Cadastro
    object Empresas : Screen("empresas", "Empresas", Icons.Default.Business)
    object GasStations : Screen("postos", "Postos de Gasolina", Icons.Default.LocalGasStation)
    object Emissores : Screen("emissores", "Emissores", Icons.Default.ReceiptLong)
    object Plataformas : Screen("plataformas", "Apps & Plataformas", Icons.Default.Smartphone)
    object Bandeiras : Screen("bandeiras", "Bandeiras", Icons.Default.CreditCard)
    object MonitoramentoPecas : Screen("monitoramento-pecas", "Monitoramento Peças", Icons.Default.Build)
    object DeliveryRoutes : Screen("delivery_routes", "Rotas", Icons.Default.AltRoute)
    object DeliveryPartners : Screen("delivery_partners", "Entregadores Parceiros", Icons.Default.TwoWheeler)
    
    // Suporte a telas auxiliares existentes
    object CreditCards : Screen("credit_cards", "Gerenciamento de Cartões", Icons.Default.CreditCard)
    object PartProducts : Screen("part_products", "Produtos & Marcas", Icons.Default.Category)
}

val bottomNavItems = listOf(
    Screen.Inicio,
    Screen.Painel,
    Screen.Relatorios,
    Screen.Historico,
)

@Composable
fun CentralDoMotoristaApp(
    authViewModel: AuthViewModel = viewModel(),
    isDarkMode: Boolean = true,
    onThemeToggle: (Boolean) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }

    val isUserLoggedIn = supabase.auth.currentUserOrNull() != null
    val homeViewModel: HomeViewModel = viewModel()
    val partMaintenanceViewModel: com.fernando.centraldomotorista.ui.screens.pecas.PartMaintenanceViewModel = viewModel()

    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }
    var isBiometricAuthenticated by remember { mutableStateOf(false) }

    val requireBiometricOnStart = isUserLoggedIn && isBiometricEnabled && isBiometricAvailable

    val startDestination = remember {
        if (isUserLoggedIn && (!isBiometricEnabled || !isBiometricAvailable)) {
            Screen.Inicio.route
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(requireBiometricOnStart) {
        if (requireBiometricOnStart && !isBiometricAuthenticated) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricAuthHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = {
                        isBiometricAuthenticated = true
                        homeViewModel.refresh()
                        navController.navigate(Screen.Inicio.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onError = {
                        // Permanece na tela de login
                    }
                )
            }
        }
    }

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OrangeNeon,
                                selectedTextColor = OrangeNeon,
                                indicatorColor = OrangeNeon.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        homeViewModel.refresh()
                        navController.navigate(Screen.Inicio.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Inicio.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onNavigateToCreateRoute = {
                        navController.navigate(Screen.LancarRota.route)
                    },
                    onNavigateToCreateDailyTotal = {
                        navController.navigate(Screen.LancarTotalDia.route)
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Relatorios.route)
                    },
                    onNavigateToFuelExpense = {
                        navController.navigate(Screen.FuelExpense.route)
                    },
                    onNavigateToMealExpense = {
                        navController.navigate(Screen.MealExpense.route)
                    },
                    onNavigateToMaintenanceExpense = {
                        partMaintenanceViewModel.openAddDialog()
                        navController.navigate(Screen.LancarManutencao.route)
                    },
                    onNavigateToEditMaintenance = { part ->
                        partMaintenanceViewModel.startEditing(part)
                        navController.navigate(Screen.LancarManutencao.route)
                    },
                    onNavigateToRoute = { route ->
                        navController.navigate(route)
                    },
                    onSignOut = {
                        authViewModel.signOut(googleAuthClient)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = "${Screen.FuelExpense.route}?itemId={itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val fuelViewModel: com.fernando.centraldomotorista.ui.screens.expenses.FuelExpenseViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.expenses.FuelExpenseScreen(
                    itemId = itemId,
                    viewModel = fuelViewModel,
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    },
                    onNavigateToGasStations = { navController.navigate(Screen.GasStations.route) },
                    onNavigateToManageCards = { navController.navigate(Screen.CreditCards.route) }
                )
            }

            composable(
                route = "${Screen.MealExpense.route}?itemId={itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val mealViewModel: com.fernando.centraldomotorista.ui.screens.expenses.MealExpenseViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.expenses.MealExpenseScreen(
                    itemId = itemId,
                    viewModel = mealViewModel,
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    },
                    onNavigateToCompanies = { navController.navigate(Screen.Empresas.route) },
                    onNavigateToManageCards = { navController.navigate(Screen.CreditCards.route) }
                )
            }

            // Cadastro - Postos de Gasolina
            composable(Screen.GasStations.route) {
                val gasStationViewModel: com.fernando.centraldomotorista.ui.screens.gasstations.GasStationViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.gasstations.GasStationScreen(
                    viewModel = gasStationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // Alias legado
            composable("gas_stations") {
                val gasStationViewModel: com.fernando.centraldomotorista.ui.screens.gasstations.GasStationViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.gasstations.GasStationScreen(
                    viewModel = gasStationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Empresas
            composable(Screen.Empresas.route) {
                val empresasViewModel: com.fernando.centraldomotorista.ui.screens.empresas.EmpresasViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.empresas.EmpresasScreen(
                    viewModel = empresasViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Emissores
            composable(Screen.Emissores.route) {
                val emissoresViewModel: com.fernando.centraldomotorista.ui.screens.emissores.EmissoresViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.emissores.EmissoresScreen(
                    viewModel = emissoresViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Apps & Plataformas
            composable(Screen.Plataformas.route) {
                val platformsViewModel: com.fernando.centraldomotorista.ui.screens.apps.PlatformsViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.apps.PlatformsScreen(
                    viewModel = platformsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("apps") {
                val platformsViewModel: com.fernando.centraldomotorista.ui.screens.apps.PlatformsViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.apps.PlatformsScreen(
                    viewModel = platformsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Bandeiras
            composable(Screen.Bandeiras.route) {
                val bandeirasViewModel: com.fernando.centraldomotorista.ui.screens.bandeiras.BandeirasViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.bandeiras.BandeirasScreen(
                    viewModel = bandeirasViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Rotas de Entrega
            composable(Screen.DeliveryRoutes.route) {
                val deliveryRoutesViewModel: com.fernando.centraldomotorista.ui.screens.deliveryroutes.DeliveryRoutesViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliveryroutes.DeliveryRoutesScreen(
                    viewModel = deliveryRoutesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("rotas") {
                val deliveryRoutesViewModel: com.fernando.centraldomotorista.ui.screens.deliveryroutes.DeliveryRoutesViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliveryroutes.DeliveryRoutesScreen(
                    viewModel = deliveryRoutesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Cadastro - Entregadores Parceiros
            composable(Screen.DeliveryPartners.route) {
                val deliveryPartnersViewModel: com.fernando.centraldomotorista.ui.screens.deliverypartners.DeliveryPartnersViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliverypartners.DeliveryPartnersScreen(
                    viewModel = deliveryPartnersViewModel,
                    onNavigateToNewSession = { partnerId ->
                        navController.navigate("new_partner_session/$partnerId")
                    },
                    onNavigateToCloseSession = { sessionId ->
                        navController.navigate("close_partner_session/$sessionId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("entregadores") {
                val deliveryPartnersViewModel: com.fernando.centraldomotorista.ui.screens.deliverypartners.DeliveryPartnersViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliverypartners.DeliveryPartnersScreen(
                    viewModel = deliveryPartnersViewModel,
                    onNavigateToNewSession = { partnerId ->
                        navController.navigate("new_partner_session/$partnerId")
                    },
                    onNavigateToCloseSession = { sessionId ->
                        navController.navigate("close_partner_session/$sessionId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sessão de Trabalho - Iniciar Nova Sessão
            composable(
                route = "new_partner_session/{partnerId}",
                arguments = listOf(navArgument("partnerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
                val newSessionViewModel: com.fernando.centraldomotorista.ui.screens.deliverypartners.NewPartnerSessionViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliverypartners.NewPartnerSessionScreen(
                    partnerId = partnerId,
                    viewModel = newSessionViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSessionCreated = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            // Sessão de Trabalho - Fechar Sessão e Pagar
            composable(
                route = "close_partner_session/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                val closeSessionViewModel: com.fernando.centraldomotorista.ui.screens.deliverypartners.ClosePartnerSessionViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.deliverypartners.ClosePartnerSessionScreen(
                    sessionId = sessionId,
                    viewModel = closeSessionViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSessionClosed = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            // Cadastro - Monitoramento Peças
            composable(Screen.MonitoramentoPecas.route) {
                com.fernando.centraldomotorista.ui.screens.pecas.PartMaintenanceScreen(
                    viewModel = partMaintenanceViewModel,
                    onNavigateToLancarManutencao = { navController.navigate(Screen.LancarManutencao.route) },
                    onNavigateToPartProducts = { navController.navigate("part_products") },
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }
            composable("part_maintenance") {
                com.fernando.centraldomotorista.ui.screens.pecas.PartMaintenanceScreen(
                    viewModel = partMaintenanceViewModel,
                    onNavigateToLancarManutencao = { navController.navigate(Screen.LancarManutencao.route) },
                    onNavigateToPartProducts = { navController.navigate("part_products") },
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            // Formulário Dedicado - Lançar / Editar Manutenção
            composable(
                route = "${Screen.LancarManutencao.route}?itemId={itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                com.fernando.centraldomotorista.ui.screens.pecas.LancarManutencaoScreen(
                    itemId = itemId,
                    viewModel = partMaintenanceViewModel,
                    onNavigateToManageCards = { navController.navigate(Screen.CreditCards.route) },
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            // Produtos & Marcas de Peças (acessada a partir do Monitoramento de Peças)
            composable("part_products") {
                val partProductsViewModel: com.fernando.centraldomotorista.ui.screens.partproducts.PartProductsViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.partproducts.PartProductsScreen(
                    viewModel = partProductsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Gerenciamento de Cartões (Cartões, Emissores e Bandeiras em abas)
            composable(Screen.CreditCards.route) {
                com.fernando.centraldomotorista.ui.screens.cards.CardManagementScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screen.LancarRota.route}?itemId={itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val routeViewModel: com.fernando.centraldomotorista.ui.screens.routes.NewRouteViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.routes.NewRouteScreen(
                    itemId = itemId,
                    viewModel = routeViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRouteSaved = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "${Screen.LancarTotalDia.route}?itemId={itemId}",
                arguments = listOf(navArgument("itemId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val totalDiaViewModel: com.fernando.centraldomotorista.ui.screens.dailytotal.LancarTotalDiaViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.dailytotal.LancarTotalDiaScreen(
                    itemId = itemId,
                    viewModel = totalDiaViewModel,
                    onNavigateBack = {
                        homeViewModel.refresh()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Painel.route) {
                GenericScreenPlaceholder(title = "Painel de Corridas e Ganhos")
            }

            composable(Screen.Relatorios.route) {
                GenericScreenPlaceholder(title = "Relatórios e Faturamento")
            }

            composable(Screen.Historico.route) {
                val historicoViewModel: com.fernando.centraldomotorista.ui.screens.historico.HistoricoViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.historico.HistoricoScreen(
                    viewModel = historicoViewModel,
                    onNavigateToEdit = { editRoute ->
                        navController.navigate(editRoute)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderActionScreen(
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Em breve!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Voltar para o Início", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GenericScreenPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
