package com.fernando.centraldomotorista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fernando.centraldomotorista.navigation.CentralDoMotoristaApp
import com.fernando.centraldomotorista.ui.theme.CentralDoMotoristaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CentralDoMotoristaTheme(darkTheme = true) {
                CentralDoMotoristaApp()
            }
        }
    }
}
