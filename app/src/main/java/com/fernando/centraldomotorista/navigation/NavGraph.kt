package com.fernando.centraldomotorista.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fernando.centraldomotorista.auth.AuthViewModel
import com.fernando.centraldomotorista.auth.GoogleAuthClient
import com.fernando.centraldomotorista.ui.screens.home.HomeViewModel
import com.fernando.centraldomotorista.ui.screens.home.NeonTestState
import com.fernando.centraldomotorista.ui.screens.login.LoginScreen
import com.fernando.centraldomotorista.ui.theme.BackgroundDark
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Inicio : Screen("inicio", "Início", Icons.Default.Home)
    object Painel : Screen("painel", "Painel", Icons.Default.BarChart)
    object Relatorios : Screen("relatorios", "Relatórios", Icons.Default.Assessment)
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Historico : Screen("historico", "Histórico", Icons.Default.History)
}

val bottomNavItems = listOf(
    Screen.Inicio,
    Screen.Painel,
    Screen.Relatorios,
    Screen.Apps,
    Screen.Historico,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralDoMotoristaApp(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

    val startDestination = remember {
        if (isUserLoggedIn) Screen.Inicio.route else Screen.Login.route
    }

    val showBottomBar = currentRoute != null && currentRoute != Screen.Login.route

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
                HomeScreenContent(
                    authViewModel = authViewModel,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
fun HomeScreenContent(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel = viewModel(),
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val user = FirebaseAuth.getInstance().currentUser
    val testState by homeViewModel.testState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Central do Motorista",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        authViewModel.signOut(googleAuthClient)
                        onSignOut()
                    }) {
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
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Usuário Autenticado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👤 Usuário Autenticado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon
                    )
                    Text(
                        text = "Nome: " + (user?.displayName ?: "Motorista"),
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Email: " + (user?.email ?: "Não informado"),
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "UID: " + (user?.uid ?: "-"),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            // Card Neon Data API - Teste de Sanidade
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Neon Data API (PostgREST)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                    }

                    when (val state = testState) {
                        is NeonTestState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = OrangeNeon,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Consultando /profiles no Neon...",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        is NeonTestState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "✅ Conexão e Autenticação OK!",
                                    color = GreenNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                if (state.profile != null) {
                                    Text(
                                        text = "Perfil retornado: " + (state.profile.fullName ?: state.profile.email ?: state.profile.id),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Meta Diária: R$ " + state.profile.dailyGoal + " | Meta Semanal: R$ " + state.profile.weeklyGoal,
                                        color = Color.LightGray,
                                        fontSize = 13.sp
                                    )
                                } else {
                                    Text(
                                        text = "Nenhum profile retornado na busca (eq." + (user?.uid ?: "") + "). Registros retornados: " + state.rawJsonCount,
                                        color = Color.Yellow,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        is NeonTestState.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "❌ Erro na Chamada da API",
                                    color = RedAlert,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = state.message,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        NeonTestState.Idle -> {
                            Text(
                                text = "Pronto para testar.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Button(
                        onClick = { homeViewModel.testFetchProfile() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Testar Consulta Novamente (Logcat)",
                            fontWeight = FontWeight.Bold
                        )
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
