package com.fernando.centraldomotorista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.navigation.CentralDoMotoristaApp
import com.fernando.centraldomotorista.ui.theme.CentralDoMotoristaTheme
import com.fernando.centraldomotorista.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
            CentralDoMotoristaTheme(darkTheme = isDarkMode) {
                CentralDoMotoristaApp(
                    isDarkMode = isDarkMode,
                    onThemeToggle = { themeViewModel.toggleTheme(it) }
                )
            }
        }
    }
}
