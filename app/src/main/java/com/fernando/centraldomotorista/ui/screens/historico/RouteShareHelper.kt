package com.fernando.centraldomotorista.ui.screens.historico

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fernando.centraldomotorista.data.model.Route
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object RouteShareHelper {

    fun BigDecimal.formatCurrency(): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        return formatter.format(this)
    }

    fun buildShareText(
        route: Route,
        platformName: String? = null
    ): String {
        val origin = route.origin?.ifBlank { "Origem" } ?: "Origem"
        val destination = route.destination?.ifBlank { "Destino" } ?: "Destino"
        val company = (platformName ?: "ROTA").uppercase()

        return """
            📋 *Central do Motorista — Comprovante de Rota*
            🏢 *Empresa:* $company
            🛣️ *Rota:* $origin ➔ $destination
            💰 *Valor Total:* ${route.amount.formatCurrency()}
            📦 *Pacotes:* ${route.packageCount}
            🚗 *Distância:* ${route.distanceKm} KM
        """.trimIndent()
    }

    fun calculateWorkedTime(
        startedAt: java.time.OffsetDateTime?,
        endedAt: java.time.OffsetDateTime?,
        breakMinutes: Long
    ): String {
        return if (startedAt != null && endedAt != null) {
            val ms = endedAt.toInstant().toEpochMilli() - startedAt.toInstant().toEpochMilli()
            val totalMins = maxOf(0L, (ms / 60000) - breakMinutes)
            val h = totalMins / 60
            val m = totalMins % 60
            String.format(Locale.getDefault(), "%02dh %02dmin", h, m)
        } else {
            "—"
        }
    }

    fun calculateTotalPackages(
        smallCount: Int,
        largeCount: Int,
        fallbackPackageCount: Int
    ): Int {
        val total = smallCount + largeCount
        return if (total > 0) total else fallbackPackageCount
    }

    /**
     * Compartilha a rota no formato Imagem (PNG) gerando um comprovante visual escuro de 800px de largura.
     */
    fun shareRouteImage(
        context: Context,
        route: Route,
        platformName: String? = null
    ) {
        try {
            val bitmap = generateRouteBitmap(route, platformName)
            if (bitmap == null) {
                Log.e("RouteShareHelper", "Falha ao gerar bitmap da rota")
                Toast.makeText(context, "Erro ao gerar imagem da rota para compartilhamento", Toast.LENGTH_SHORT).show()
                return
            }

            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, "comprovante_rota_${route.id.take(8)}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val company = (platformName ?: "ROTA").uppercase()
            val shareText = buildShareText(route, platformName)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Comprovante de Rota - $company")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Comprovante de Rota").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("RouteShareHelper", "Erro ao compartilhar imagem da rota: ${e.message}", e)
            Toast.makeText(context, "Erro ao gerar imagem da rota para compartilhamento", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Compartilha a rota no formato Documento (PDF) gerando uma página A4 (595x842 pt).
     */
    fun shareRoutePdf(
        context: Context,
        route: Route,
        platformName: String? = null
    ) {
        try {
            val docsDir = File(context.cacheDir, "docs")
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val file = File(docsDir, "comprovante_rota_${route.id.take(8)}_${System.currentTimeMillis()}.pdf")
            val document = generateRoutePdfDocument(route, platformName)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val company = (platformName ?: "ROTA").uppercase()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Comprovante de Rota (PDF) - $company")
                putExtra(Intent.EXTRA_TEXT, "Comprovante da rota - $company em anexo.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Documento PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("RouteShareHelper", "Erro ao gerar ou compartilhar PDF da rota: ${e.message}", e)
            Toast.makeText(context, "Erro ao gerar PDF da rota para compartilhamento", Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // GERAÇÃO DE BITMAP (PNG)
    // =========================================================================
    internal fun generateRouteBitmap(
        route: Route,
        platformName: String? = null
    ): Bitmap? {
        val width = 800
        val baseHeight = 1040
        val extraNotesHeight = if (!route.notes.isNullOrBlank()) 100 else 0
        val height = baseHeight + extraNotesHeight

        val bitmap: Bitmap? = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (_: Throwable) {
            null
        }

        val canvas = bitmap?.let { try { Canvas(it) } catch (_: Throwable) { null } } ?: Canvas()
        drawRouteBitmapContent(canvas, route, platformName, width.toFloat(), height.toFloat())

        return bitmap
    }

    internal fun drawRouteBitmapContent(
        canvas: Canvas,
        route: Route,
        platformName: String?,
        width: Float = 800f,
        height: Float = 1040f
    ) {

        // Fundo principal escuro
        canvas.drawColor(Color.parseColor("#0F0F12"))

        // Card central escuro
        val cardRect = RectF(20f, 20f, width - 20f, height - 20f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181C")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        // Borda Laranja Neon
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        val leftX = 50f
        val rightX = width - 50f
        var currentY = 70f

        // Cabeçalho: Título do App
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("GIROCUSTO • CENTRAL DO MOTORISTA", leftX, currentY, titlePaint)

        currentY += 28f
        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9E9E")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("COMPROVANTE DETALHADO DE ROTA", leftX, currentY, subTitlePaint)

        currentY += 35f

        // Divisor superior
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C2C32")
            strokeWidth = 1.5f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        currentY += 35f

        // Linha 1: Empresa centralizada envolta em retângulo
        val companyText = (platformName ?: "ROTA").uppercase()
        val companyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = companyTextPaint.measureText(companyText)
        val pillWidth = textWidth + 40f
        val pillHeight = 36f
        val pillCenterX = width / 2f
        val pillRect = RectF(
            pillCenterX - (pillWidth / 2f),
            currentY - 24f,
            pillCenterX + (pillWidth / 2f),
            currentY - 24f + pillHeight
        )
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#261D14") // OrangeNeon copy alpha 0.15
            style = Paint.Style.FILL
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(pillRect, 18f, 18f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 18f, 18f, pillBorderPaint)
        canvas.drawText(companyText, pillCenterX - (textWidth / 2f), currentY, companyTextPaint)

        currentY += 42f

        // Linha 2: Origem ➔ Destino
        val origin = route.origin?.ifBlank { "Origem" } ?: "Origem"
        val destination = route.destination?.ifBlank { "Destino" } ?: "Destino"
        val routeStr = "$origin ➔ $destination"
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(routeStr, width / 2f, currentY, routePaint)

        currentY += 45f

        // =========================================================================
        // CARD: VALOR TOTAL DA ROTA
        // =========================================================================
        val totalCardRect = RectF(leftX, currentY, rightX, currentY + 115f)
        val sectionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F1F24")
            style = Paint.Style.FILL
        }
        val sectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33333A")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(totalCardRect, 16f, 16f, sectionBgPaint)
        canvas.drawRoundRect(totalCardRect, 16f, 16f, sectionBorderPaint)

        val totalHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText("VALOR TOTAL DA ROTA", leftX + 20f, currentY + 28f, totalHeaderPaint)

        // Linha interna do card
        canvas.drawLine(leftX + 20f, currentY + 38f, rightX - 20f, currentY + 38f, dividerPaint)

        // Valor total grande
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(route.amount.formatCurrency(), leftX + 20f, currentY + 80f, valuePaint)

        // Adicionais (Gorjeta/Bônus) se houver
        if (route.tip > BigDecimal.ZERO || route.bonus > BigDecimal.ZERO) {
            val bonusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#34C759")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            var extraStr = ""
            if (route.tip > BigDecimal.ZERO) extraStr += "+ Gorjeta: ${route.tip.formatCurrency()}   "
            if (route.bonus > BigDecimal.ZERO) extraStr += "+ Bônus: ${route.bonus.formatCurrency()}"
            canvas.drawText(extraStr.trim(), leftX + 20f, currentY + 104f, bonusPaint)
        }

        currentY += 135f

        // =========================================================================
        // SEÇÃO: DATA & HORÁRIOS
        // =========================================================================
        val zone = ZoneId.systemDefault()
        val localOccurred = route.occurredAt.atZoneSameInstant(zone)
        val localStarted = route.startedAt?.atZoneSameInstant(zone)
        val localEnded = route.endedAt?.atZoneSameInstant(zone)
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR"))
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))

        val workedTimeStr = if (localStarted != null && localEnded != null) {
            val ms = route.endedAt!!.toInstant().toEpochMilli() - route.startedAt!!.toInstant().toEpochMilli()
            val totalMins = maxOf(0L, (ms / 60000) - route.breakMinutes)
            val h = totalMins / 60
            val m = totalMins % 60
            String.format(Locale.getDefault(), "%02dh %02dmin", h, m)
        } else {
            "—"
        }

        val horariosCardHeight = if (route.breakMinutes > 0) 140f else 115f
        drawSectionBox(canvas, leftX, currentY, rightX, currentY + horariosCardHeight, "DATA & HORÁRIOS")
        var rowY = currentY + 54f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Data da Rota", localOccurred.format(dateFormatter))
        rowY += 26f
        val timeIntervalStr = if (localStarted != null && localEnded != null) {
            "${localStarted.format(timeFormatter)} às ${localEnded.format(timeFormatter)}"
        } else {
            "—"
        }
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Horário", timeIntervalStr)
        rowY += 26f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Tempo Trabalhado", workedTimeStr)
        if (route.breakMinutes > 0) {
            rowY += 26f
            drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Tempo de Pausa", "${route.breakMinutes} minutos")
        }

        currentY += horariosCardHeight + 20f

        // =========================================================================
        // SEÇÃO: PACOTES & VOLUMES
        // =========================================================================
        val totalPkgs = (route.smallPackagesCount + route.largePackagesCount).let {
            if (it > 0) it else route.packageCount
        }
        var pkgsCardHeight = 90f
        if (route.smallPackagesCount > 0) pkgsCardHeight += 26f
        if (route.largePackagesCount > 0) pkgsCardHeight += 26f

        drawSectionBox(canvas, leftX, currentY, rightX, currentY + pkgsCardHeight, "PACOTES & VOLUMES")
        rowY = currentY + 54f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Total de Pacotes", "$totalPkgs pacotes")
        if (route.smallPackagesCount > 0) {
            rowY += 26f
            val smallSubtotal = BigDecimal(route.smallPackagesCount).multiply(route.packageUnitPrice)
            drawRow(
                canvas, leftX + 20f, rightX - 20f, rowY,
                "Pacotes Pequenos",
                "${route.smallPackagesCount}x (${route.packageUnitPrice.formatCurrency()}) = ${smallSubtotal.formatCurrency()}"
            )
        }
        if (route.largePackagesCount > 0) {
            rowY += 26f
            val largeTotal = route.largePackagesPrices.fold(BigDecimal.ZERO, BigDecimal::add)
            drawRow(
                canvas, leftX + 20f, rightX - 20f, rowY,
                "Pacotes Grandes / Volumosos",
                "${route.largePackagesCount} volumes = ${largeTotal.formatCurrency()}"
            )
        }
        rowY += 26f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Tipo de Produto", route.productType.replaceFirstChar { it.uppercase() })

        currentY += pkgsCardHeight + 20f

        // =========================================================================
        // SEÇÃO: DISTÂNCIA PERCORRIDA (RIGOROSAMENTE NA ORDEM ESPECIFICADA)
        // Linha 1: Odômetro Inicial: XX.XXX "KM"
        // Linha 2: Odômetro Final: XX.XXX "KM"
        // Linha 3: Dist. Percorrido: XX.XXX "KM"
        // =========================================================================
        val distCardHeight = 115f
        drawSectionBox(canvas, leftX, currentY, rightX, currentY + distCardHeight, "Distância Percorrida")
        rowY = currentY + 54f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Odômetro Inicial", "${route.startKm} KM")
        rowY += 26f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Odômetro Final", "${route.endKm} KM")
        rowY += 26f
        drawRow(canvas, leftX + 20f, rightX - 20f, rowY, "Dist. Percorrido", "${route.distanceKm} KM")

        currentY += distCardHeight + 20f

        // Observações (se houver)
        if (!route.notes.isNullOrBlank()) {
            drawSectionBox(canvas, leftX, currentY, rightX, currentY + 80f, "OBSERVAÇÕES")
            val notesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 13f
            }
            canvas.drawText(route.notes.take(80), leftX + 20f, currentY + 54f, notesPaint)
            currentY += 100f
        }

        // Rodapé
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#616161")
            textSize = 11f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Comprovante gerado pelo aplicativo GiroCusto", width / 2f, height - 35f, footerPaint)
    }

    private fun drawSectionBox(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        title: String
    ) {
        val rect = RectF(left, top, right, bottom)
        val sectionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F1F24")
            style = Paint.Style.FILL
        }
        val sectionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33333A")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, 14f, 14f, sectionBgPaint)
        canvas.drawRoundRect(rect, 14f, 14f, sectionBorderPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        canvas.drawText(title, left + 18f, top + 26f, headerPaint)

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C2C32")
            strokeWidth = 1f
        }
        canvas.drawLine(left + 18f, top + 34f, right - 18f, top + 34f, dividerPaint)
    }

    private fun drawRow(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        label: String,
        value: String
    ) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A0A0A8")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(label, left, y, labelPaint)
        canvas.drawText(value, right, y, valuePaint)
    }

    // =========================================================================
    // GERAÇÃO DE PDF (A4: 595 x 842 pt) - DELEGADO PARA RoutePdfDrawer
    // =========================================================================
    internal fun generateRoutePdfDocument(
        route: Route,
        platformName: String? = null
    ): PdfDocument = RoutePdfDrawer.generateRoutePdfDocument(route, platformName)

    internal fun drawRoutePdfPage(
        canvas: Canvas,
        route: Route,
        platformName: String?
    ) = RoutePdfDrawer.drawRoutePdfPage(canvas, route, platformName)
}
