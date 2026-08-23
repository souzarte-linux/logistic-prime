package com.fernando.centraldomotorista.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.bottomNavColor
import com.fernando.centraldomotorista.ui.theme.secondaryTextColor

// As 5 abas da bottom navigation, na mesma ordem do design (INÍCIO, PAINEL,
// RELATÓRIOS, APPS, HISTÓRICO). Cada rota vira uma tela cheia depois (Prompts 1-5).
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
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
fun CentralDoMotoristaApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { CentralBottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inicio.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Cada composable() abaixo é um placeholder — será substituído
            // pela tela real nos Prompts 1 a 5.
            composable(Screen.Inicio.route) { Text("Tela INÍCIO (Prompt 1)") }
            composable(Screen.Painel.route) { Text("Tela PAINEL (Prompt 2)") }
            composable(Screen.Relatorios.route) { Text("Tela RELATÓRIOS & INSIGHTS (Prompt 3)") }
            composable(Screen.Apps.route) { Text("Tela GESTOR DE PLATAFORMAS (Prompt 4)") }
            composable(Screen.Historico.route) { Text("Tela HISTÓRICO (Prompt 5)") }
        }
    }
}

@Composable
private fun CentralBottomNavBar(navController: androidx.navigation.NavHostController) {
    val barColor = bottomNavColor()      // resolve sozinho pra cor certa (claro ou escuro)
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
                icon = { Icon(screen.icon, contentDescription = screen.label) },
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
