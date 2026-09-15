package com.fernando.centraldomotorista.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast

enum class WhatsAppVariant(val packageName: String, val title: String, val subtitle: String) {
    STANDARD("com.whatsapp", "WhatsApp Pessoal", "Aplicativo convencional"),
    BUSINESS("com.whatsapp.w4b", "WhatsApp Business", "Conta comercial")
}

object WhatsAppHelper {

    fun getInstalledWhatsAppVariants(context: Context): List<WhatsAppVariant> {
        val pm = context.packageManager
        return WhatsAppVariant.values().filter { variant ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(variant.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(variant.packageName, 0)
                }
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    fun openWhatsApp(context: Context, phone: String, packageName: String? = null) {
        val cleanDigits = phone.filter { it.isDigit() }
        val fullNumber = if (cleanDigits.startsWith("55")) cleanDigits else "55$cleanDigits"
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullNumber")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (!packageName.isNullOrBlank()) {
                setPackage(packageName)
            }
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$fullNumber"))
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Não foi possível abrir o WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
