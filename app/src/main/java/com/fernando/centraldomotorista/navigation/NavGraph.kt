package com.fernando.centraldomotorista.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fernando.centraldomotorista.auth.AuthState
import com.fernando.centraldomotorista.auth.AuthViewModel
import com.fernando.centraldomotorista.ui.screens.login.LoginScreen
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.bottomNavColor
import com.fernando.centraldomotorista.ui.theme.secondaryTextColor
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    object Login : Screen("login", "Login", null)
    object Inicio : Screen("inicio", "Início", Icons.Filled.Home)
    object Painel : Screen("painel", "Painel", Icons.Filled.BarChart)
    object Relatorios : Screen("relatorios", "Relatórios", Icons.Filled.Assessment)
    object Apps : Screen("apps", "Apps", Icons.Filled.Apps)
    object Historico : Screen("historico", "Histórico", Icons.Filled.History)
}

private val bottomNavItems = listOf(
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
    val authState by authViewModel.authState.collectAsState()
    val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null

    val startDestination = if (isUserLoggedIn) Screen.Inicio.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != Screen.Login.route

    // Reagir a mudanças de logout para voltar à tela de login
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedOut && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CentralBottomNavBar(navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Tela de Login
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

            // Tela Início (com cabeçalho e botão de Logout temporário)
            composable(Screen.Inicio.route) {
                HomeScreenPlaceholder(
                    authState = authState,
                    onSignOut = { authViewModel.signOut() }
                )
            }

            composable(Screen.Painel.route) { PlaceholderScreen("Tela PAINEL (Prompt 2)") }
            composable(Screen.Relatorios.route) { PlaceholderScreen("Tela RELATÓRIOS & INSIGHTS (Prompt 3)") }
            composable(Screen.Apps.route) { PlaceholderScreen("Tela GESTOR DE PLATAFORMAS (Prompt 4)") }
            composable(Screen.Historico.route) { PlaceholderScreen("Tela HISTÓRICO (Prompt 5)") }
        }
    }
}

@Composable
private fun HomeScreenPlaceholder(
    authState: AuthState,
    onSignOut: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Barra superior com boas-vindas e botão de Sair
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Olá,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val userName = if (authState is AuthState.LoggedIn) {
                        authState.profile.fullName ?: authState.user.displayName ?: "Motorista"
                    } else {
                        "Motorista"
                    }
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RedAlert
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sair",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.padding(2.dp))
                    Text(text = "Sair", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Conteúdo central da tela Início
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tela INÍCIO (Prompt 1)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Usuário autenticado com sucesso via Firebase Auth",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrangeNeon
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun CentralBottomNavBar(navController: androidx.navigation.NavHostController) {
    val barColor = bottomNavColor()
    val unselectedColor = secondaryTextColor()

    NavigationBar(containerColor = barColor) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    screen.icon?.let {
                        Icon(it, contentDescription = screen.label)
                    }
                },
                label = { Text(screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = OrangeNeon,
                    selectedTextColor = OrangeNeon,
                    unselectedIconColor = unselectedColor,
                    unselectedTextColor = unselectedColor,
                    indicatorColor = barColor,
                ),
            )
        }
    }
}
