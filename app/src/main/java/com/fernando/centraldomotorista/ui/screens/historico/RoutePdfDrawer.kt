package com.fernando.centraldomotorista.ui.screens.historico

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fernando.centraldomotorista.data.model.Route
import com.fernando.centraldomotorista.ui.screens.historico.RouteShareHelper.formatCurrency
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object RoutePdfDrawer {

    // =========================================================================
    // GERAÇÃO DE PDF (A4: 595 x 842 pt)
    // =========================================================================
    fun generateRoutePdfDocument(
        route: Route,
        platformName: String? = null
    ): PdfDocument {
        val document = PdfDocument()
        val pageInfo = try {
            PdfDocument.PageInfo.Builder(595, 842, 1).create()
        } catch (_: Throwable) {
            null
        }
        val page = if (pageInfo != null) {
            try { document.startPage(pageInfo) } catch (_: Throwable) { null }
        } else null

        val canvas = page?.canvas ?: Canvas()
        drawRoutePdfPage(canvas, route, platformName)

        if (page != null) {
            try { document.finishPage(page) } catch (_: Throwable) {}
        }
        return document
    }

    fun drawRoutePdfPage(
        canvas: Canvas,
        route: Route,
        platformName: String?
    ) {
        val width = 595f
        val height = 842f
        val margin = 36f
        val contentWidth = width - (margin * 2)

        // Fundo da página (branco elegante para impressão e leitura)
        canvas.drawColor(Color.WHITE)

        // Faixa de Cabeçalho Superior escura
        val headerRect = RectF(0f, 0f, width, 84f)
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181C")
            style = Paint.Style.FILL
        }
        canvas.drawRect(headerRect, headerBgPaint)

        // Linha laranja inferior do cabeçalho
        val orangeStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            strokeWidth = 3f
        }
        canvas.drawLine(0f, 84f, width, 84f, orangeStripePaint)

        // Textos do cabeçalho
        val appTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        canvas.drawText("GIROCUSTO • CENTRAL DO MOTORISTA", margin, 38f, appTitlePaint)

        val docSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("COMPROVANTE OFICIAL DE EXECUÇÃO DE ROTA", margin, 58f, docSubtitlePaint)

        var currentY = 115f

        // Linha 1: Empresa centralizada envolta em retângulo
        val companyText = (platformName ?: "ROTA").uppercase()
        val companyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = companyTextPaint.measureText(companyText)
        val pillWidth = textWidth + 30f
        val pillHeight = 24f
        val pillCenterX = width / 2f
        val pillRect = RectF(
            pillCenterX - (pillWidth / 2f),
            currentY - 16f,
            pillCenterX + (pillWidth / 2f),
            currentY - 16f + pillHeight
        )
        val pillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF3E0")
            style = Paint.Style.FILL
        }
        val pillBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(pillRect, 12f, 12f, pillBgPaint)
        canvas.drawRoundRect(pillRect, 12f, 12f, pillBorderPaint)
        canvas.drawText(companyText, pillCenterX - (textWidth / 2f), currentY, companyTextPaint)

        currentY += 32f

        // Linha 2: Origem ➔ Destino
        val origin = route.origin?.ifBlank { "Origem" } ?: "Origem"
        val destination = route.destination?.ifBlank { "Destino" } ?: "Destino"
        val routeStr = "$origin ➔ $destination"
        val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(routeStr, width / 2f, currentY, routePaint)

        currentY += 30f

        // Card Valor Total da Rota
        val totalCardRect = RectF(margin, currentY, margin + contentWidth, currentY + 80f)
        val totalCardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8E1")
            style = Paint.Style.FILL
        }
        val totalCardBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFA000")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(totalCardRect, 10f, 10f, totalCardBg)
        canvas.drawRoundRect(totalCardRect, 10f, 10f, totalCardBorder)

        val totalHeaderPdf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        canvas.drawText("VALOR TOTAL DA ROTA", margin + 14f, currentY + 20f, totalHeaderPdf)

        val dividerPdf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE082")
            strokeWidth = 1f
        }
        canvas.drawLine(margin + 14f, currentY + 26f, margin + contentWidth - 14f, currentY + 26f, dividerPdf)

        val valAmountPdf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#BF360C")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(route.amount.formatCurrency(), margin + 14f, currentY + 56f, valAmountPdf)

        if (route.tip > BigDecimal.ZERO || route.bonus > BigDecimal.ZERO) {
            val bonusPdf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#2E7D32")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            var extraStr = ""
            if (route.tip > BigDecimal.ZERO) extraStr += "+ Gorjeta: ${route.tip.formatCurrency()}   "
            if (route.bonus > BigDecimal.ZERO) extraStr += "+ Bônus: ${route.bonus.formatCurrency()}"
            canvas.drawText(extraStr.trim(), margin + 14f, currentY + 72f, bonusPdf)
        }

        currentY += 98f

        // Seção: DATA & HORÁRIOS
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

        val horariosHeight = if (route.breakMinutes > 0) 105f else 85f
        drawPdfSection(canvas, margin, currentY, contentWidth, horariosHeight, "DATA & HORÁRIOS")
        var rowPdfY = currentY + 40f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Data da Rota", localOccurred.format(dateFormatter))
        rowPdfY += 18f
        val timeIntervalStr = if (localStarted != null && localEnded != null) {
            "${localStarted.format(timeFormatter)} às ${localEnded.format(timeFormatter)}"
        } else {
            "—"
        }
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Horário", timeIntervalStr)
        rowPdfY += 18f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Tempo Trabalhado", workedTimeStr)
        if (route.breakMinutes > 0) {
            rowPdfY += 18f
            drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Tempo de Pausa", "${route.breakMinutes} minutos")
        }

        currentY += horariosHeight + 16f

        // Seção: PACOTES & VOLUMES
        val totalPkgs = (route.smallPackagesCount + route.largePackagesCount).let {
            if (it > 0) it else route.packageCount
        }
        var pkgsHeight = 70f
        if (route.smallPackagesCount > 0) pkgsHeight += 18f
        if (route.largePackagesCount > 0) pkgsHeight += 18f

        drawPdfSection(canvas, margin, currentY, contentWidth, pkgsHeight, "PACOTES & VOLUMES")
        rowPdfY = currentY + 40f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Total de Pacotes", "$totalPkgs pacotes")
        if (route.smallPackagesCount > 0) {
            rowPdfY += 18f
            val smallSubtotal = BigDecimal(route.smallPackagesCount).multiply(route.packageUnitPrice)
            drawPdfRow(
                canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY,
                "Pacotes Pequenos",
                "${route.smallPackagesCount}x (${route.packageUnitPrice.formatCurrency()}) = ${smallSubtotal.formatCurrency()}"
            )
        }
        if (route.largePackagesCount > 0) {
            rowPdfY += 18f
            val largeTotal = route.largePackagesPrices.fold(BigDecimal.ZERO, BigDecimal::add)
            drawPdfRow(
                canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY,
                "Pacotes Grandes / Volumosos",
                "${route.largePackagesCount} volumes = ${largeTotal.formatCurrency()}"
            )
        }
        rowPdfY += 18f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Tipo de Produto", route.productType.replaceFirstChar { it.uppercase() })

        currentY += pkgsHeight + 16f

        // Seção: DISTÂNCIA PERCORRIDA (RIGOROSAMENTE NA ORDEM ESPECIFICADA)
        // Linha 1: Odômetro Inicial: XX.XXX "KM"
        // Linha 2: Odômetro Final: XX.XXX "KM"
        // Linha 3: Dist. Percorrido: XX.XXX "KM"
        val distHeight = 85f
        drawPdfSection(canvas, margin, currentY, contentWidth, distHeight, "Distância Percorrida")
        rowPdfY = currentY + 40f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Odômetro Inicial", "${route.startKm} KM")
        rowPdfY += 18f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Odômetro Final", "${route.endKm} KM")
        rowPdfY += 18f
        drawPdfRow(canvas, margin + 14f, margin + contentWidth - 14f, rowPdfY, "Dist. Percorrido", "${route.distanceKm} KM")

        currentY += distHeight + 16f

        // Observações (se houver)
        if (!route.notes.isNullOrBlank()) {
            drawPdfSection(canvas, margin, currentY, contentWidth, 55f, "OBSERVAÇÕES")
            val notesPdf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#424242")
                textSize = 9.5f
            }
            canvas.drawText(route.notes.take(90), margin + 14f, currentY + 40f, notesPdf)
            currentY += 70f
        }

        // Rodapé do PDF
        val footerPdfLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        canvas.drawLine(margin, height - 42f, margin + contentWidth, height - 42f, footerPdfLine)

        val footerPdfText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#757575")
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Comprovante digital emitido pelo GiroCusto Central do Motorista", width / 2f, height - 24f, footerPdfText)
    }

    internal fun drawPdfSection(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String
    ) {
        val rect = RectF(x, y, x + width, y + height)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F9F9FA")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E65100")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.05f
        }
        canvas.drawText(title, x + 14f, y + 20f, titlePaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EEEEEE")
            strokeWidth = 1f
        }
        canvas.drawLine(x + 14f, y + 26f, x + width - 14f, y + 26f, linePaint)
    }

    internal fun drawPdfRow(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        label: String,
        value: String
    ) {
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#616161")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#212121")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(label, left, y, labelPaint)
        canvas.drawText(value, right, y, valuePaint)
    }
}
