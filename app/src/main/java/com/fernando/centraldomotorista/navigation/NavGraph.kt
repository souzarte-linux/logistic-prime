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
import com.fernando.centraldomotorista.data.remote.supabase
import com.fernando.centraldomotorista.ui.screens.home.HomeScreen
import com.fernando.centraldomotorista.ui.screens.home.HomeViewModel
import com.fernando.centraldomotorista.ui.screens.login.LoginScreen
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.google.firebase.auth.FirebaseAuth
import io.github.jan.supabase.auth.auth

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Inicio : Screen("inicio", "Início", Icons.Default.Home)
    object Painel : Screen("painel", "Painel", Icons.Default.BarChart)
    object Relatorios : Screen("relatorios", "Relatórios", Icons.Default.Assessment)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Historico : Screen("historico", "Histórico", Icons.Default.History)
    
    // Actions & Forms
    object LancarRota : Screen("lancar_rota", "Lançar Rota", Icons.Default.Navigation)
    object LancarTotalDia : Screen("lancar_total_dia", "Total do Dia", Icons.Default.CalendarToday)
    object FuelExpense : Screen("fuel_expense", "Novo Abastecimento", Icons.Default.LocalGasStation)
    object GasStations : Screen("gas_stations", "Postos de Gasolina", Icons.Default.EvStation)
    object CreditCards : Screen("credit_cards", "Cartões de Crédito", Icons.Default.CreditCard)
}

val bottomNavItems = listOf(
    Screen.Inicio,
    Screen.Painel,
    Screen.Relatorios,
    Screen.Apps,
    Screen.Historico,
)

@Composable
fun CentralDoMotoristaApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }

    val isUserLoggedIn = supabase.auth.currentUserOrNull() != null || FirebaseAuth.getInstance().currentUser != null

    val startDestination = remember {
        if (isUserLoggedIn) Screen.Inicio.route else Screen.Login.route
    }

    val showBottomBar = currentRoute != null &&
            currentRoute != Screen.Login.route &&
            currentRoute != Screen.LancarRota.route &&
            currentRoute != Screen.LancarTotalDia.route &&
            currentRoute != Screen.FuelExpense.route &&
            currentRoute != Screen.GasStations.route &&
            currentRoute != Screen.CreditCards.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    contentColor = Color.White
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
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
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
                        navController.navigate(Screen.Inicio.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Inicio.route) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
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
                    onNavigateToGasStations = {
                        navController.navigate(Screen.GasStations.route)
                    },
                    onNavigateToCreditCards = {
                        navController.navigate(Screen.CreditCards.route)
                    },
                    onSignOut = {
                        authViewModel.signOut(googleAuthClient)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.FuelExpense.route) {
                val fuelViewModel: com.fernando.centraldomotorista.ui.screens.expenses.FuelExpenseViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.expenses.FuelExpenseScreen(
                    viewModel = fuelViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGasStations = { navController.navigate(Screen.GasStations.route) },
                    onNavigateToManageCards = { navController.navigate(Screen.CreditCards.route) }
                )
            }

            composable(Screen.GasStations.route) {
                val gasStationViewModel: com.fernando.centraldomotorista.ui.screens.gasstations.GasStationViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.gasstations.GasStationScreen(
                    viewModel = gasStationViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.CreditCards.route) {
                val creditCardsViewModel: com.fernando.centraldomotorista.ui.screens.cards.CreditCardsViewModel = viewModel()
                com.fernando.centraldomotorista.ui.screens.cards.CreditCardsScreen(
                    viewModel = creditCardsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LancarRota.route) {
                PlaceholderActionScreen(
                    title = "Lançar Ganhos por Rota",
                    description = "Formulário detalhado de corrida com cálculo automático de km e comissões.",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.LancarTotalDia.route) {
                PlaceholderActionScreen(
                    title = "Lançar Total do Dia",
                    description = "Registro consolidado de faturamento diário por aplicativo parceiro.",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Painel.route) {
                GenericScreenPlaceholder(title = "Painel de Corridas e Ganhos")
            }

            composable(Screen.Relatorios.route) {
                GenericScreenPlaceholder(title = "Relatórios e Faturamento")
            }

            composable(Screen.Apps.route) {
                GenericScreenPlaceholder(title = "Plataformas e Aplicativos")
            }

            composable(Screen.Historico.route) {
                GenericScreenPlaceholder(title = "Histórico de Atividades")
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
                title = { Text(title, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
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
                        color = Color.White
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
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
            .background(BackgroundDark),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
    }
}
