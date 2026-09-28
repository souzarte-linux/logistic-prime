package com.fernando.centraldomotorista.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utilitário responsável pelo despacho de navegação veicular nativa via Deep Linking
 * para Google Maps e Waze, eliminando custos de APIs externas de mapas.
 */
object NavigationIntentHelper {

    enum class NavAppPreference {
        GOOGLE_MAPS,
        WAZE,
        ALWAYS_ASK
    }

    /**
     * Inicia a navegação para o endereço especificado no app de GPS do motorista.
     */
    fun launchNavigation(
        context: Context,
        address: String,
        preference: NavAppPreference = NavAppPreference.ALWAYS_ASK
    ) {
        if (address.isBlank()) {
            Toast.makeText(context, "Endereço inválido para navegação", Toast.LENGTH_SHORT).show()
            return
        }

        val encodedAddress = Uri.encode(address.trim())

        when (preference) {
            NavAppPreference.WAZE -> {
                if (launchWaze(context, encodedAddress)) return
                if (launchGoogleMaps(context, encodedAddress)) return
            }
            NavAppPreference.GOOGLE_MAPS -> {
                if (launchGoogleMaps(context, encodedAddress)) return
                if (launchWaze(context, encodedAddress)) return
            }
            NavAppPreference.ALWAYS_ASK -> {
                // Tenta seletor genérico do sistema
            }
        }

        // Tenta seletor padrão do Android via geo intent
        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encodedAddress")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val chooser = Intent.createChooser(genericIntent, "Navegar com:").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            // Fallback para navegador web caso nenhum aplicativo responda
            openBrowserFallback(context, encodedAddress)
        } catch (e: Exception) {
            // Em caso de falha geral, tenta Maps direto ou Browser
            if (!launchGoogleMaps(context, encodedAddress)) {
                openBrowserFallback(context, encodedAddress)
            }
        }
    }

    /**
     * Dispara navegação direta no Google Maps em modo Turn-by-Turn.
     */
    fun launchGoogleMaps(context: Context, encodedAddress: String): Boolean {
        return try {
            val gmmIntentUri = Uri.parse("google.navigation:q=$encodedAddress&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispara navegação direta no Waze.
     */
    fun launchWaze(context: Context, encodedAddress: String): Boolean {
        return try {
            val wazeUri = Uri.parse("waze://?q=$encodedAddress&navigate=yes")
            val wazeIntent = Intent(Intent.ACTION_VIEW, wazeUri).apply {
                setPackage("com.waze")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(wazeIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun openBrowserFallback(context: Context, encodedAddress: String) {
        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedAddress")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o mapa", Toast.LENGTH_SHORT).show()
        }
    }
}
