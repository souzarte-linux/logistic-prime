package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.fernando.centraldomotorista.data.model.DeliveryPartnerSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SessionShareHelper {

    fun shareSessionImage(
        context: Context,
        session: DeliveryPartnerSession,
        partnerName: String,
        routeName: String,
        startTimeStr: String,
        endTimeStr: String,
        durationStr: String
    ) {
        try {
            val bitmap = generateSessionBitmap(
                session = session,
                partnerName = partnerName,
                routeName = routeName,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr,
                durationStr = durationStr
            )

            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, "sessao_${session.id.take(8)}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = """
                📋 *Central do Motorista — Detalhes da Sessão*
                👤 *Entregador:* $partnerName
                🛣️ *Rota:* $routeName
                ▶️ *Início:* $startTimeStr
                🏁 *Fim:* $endTimeStr
                ⏱️ *Duração:* $durationStr
                📦 *Entregues:* ${session.deliveredCount}
                ↩️ *Devolvidos:* ${session.returnedCount}
                💰 *Total Pago:* R$ ${String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Detalhes da Sessão - $partnerName")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Detalhes da Sessão").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("SessionShareHelper", "Erro ao compartilhar imagem da sessão: ${e.message}", e)
            Toast.makeText(context, "Erro ao gerar imagem para compartilhamento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateSessionBitmap(
        session: DeliveryPartnerSession,
        partnerName: String,
        routeName: String,
        startTimeStr: String,
        endTimeStr: String,
        durationStr: String
    ): Bitmap {
        val width = 800
        val height = 940
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fundo escuro externo
        canvas.drawColor(Color.parseColor("#0F0F12"))

        // Card com cantos arredondados
        val cardRect = RectF(24f, 24f, width - 24f, height - 24f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181C")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardPaint)

        // Borda sutil Neon
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        val leftX = 64f
        val rightX = width - 64f
        var currentY = 82f

        // Cabeçalho: Nome do App
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("CENTRAL DO MOTORISTA", leftX, currentY, brandPaint)

        // Título: Detalhes da Sessão
        currentY += 42f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Detalhes da Sessão", leftX, currentY, titlePaint)

        // Linha divisória
        currentY += 24f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33333E")
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Paints para linhas de dados
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9EA8")
            textSize = 23f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.LEFT
        }

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F0F0F5")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val orangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        fun drawRow(label: String, value: String, customValuePaint: Paint = valuePaint, maxWidthValue: Float = 420f) {
            currentY += 46f
            canvas.drawText(label, leftX, currentY, labelPaint)
            val truncatedVal = truncateText(value, customValuePaint, maxWidthValue)
            canvas.drawText(truncatedVal, rightX, currentY, customValuePaint)
        }

        // Seção 1: Informações da Rota
        drawRow("Entregador:", partnerName)
        drawRow("Rota:", routeName)
        drawRow("Início:", startTimeStr)
        drawRow("Término:", endTimeStr)
        drawRow("Duração Rota:", durationStr, orangePaint)

        // Linha divisória
        currentY += 24f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Seção 2: Pacotes
        drawRow("Pacotes Expedidos:", "${session.expectedPackageCount}")
        drawRow("Pacotes Bipados:", "${session.scannedCount}")
        drawRow("Pacotes Entregues:", "${session.deliveredCount}", greenPaint)
        drawRow("Pacotes Devolvidos:", "${session.returnedCount}")

        // Linha divisória
        currentY += 24f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Seção 3: Financeiro
        currentY += 50f
        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val amountValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Valor Total Pago:", leftX, currentY, amountLabelPaint)
        val formattedAmount = "R$ ${String.format(Locale("pt", "BR"), "%.2f", session.amountPaid)}"
        canvas.drawText(formattedAmount, rightX, currentY, amountValuePaint)

        // Linha divisória
        currentY += 28f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Rodapé
        currentY += 36f
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B6B76")
            textSize = 17f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Comprovante gerado em $nowStr", width / 2f, currentY, footerPaint)

        return bitmap
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return "$truncated..."
    }

    fun shareScannedBarcodesImage(
        context: Context,
        partnerName: String,
        routeName: String,
        sessionDateStr: String,
        items: List<ScannedItemExport>
    ) {
        try {
            val bitmap = generateScannedBarcodesBitmap(
                partnerName = partnerName,
                routeName = routeName,
                sessionDateStr = sessionDateStr,
                items = items
            )

            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, "itens_bipados_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val deliveredCount = items.count { !it.isReturned }
            val returnedCount = items.count { it.isReturned }

            val shareText = """
                📦 *Central do Motorista — Itens Bipados*
                👤 *Entregador:* $partnerName
                🛣️ *Rota:* $routeName
                📅 *Data:* $sessionDateStr
                ✅ *Entregues:* $deliveredCount
                ❌ *Devolvidos:* $returnedCount
                📊 *Total:* ${items.size}
            """.trimIndent()

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Itens Bipados - $partnerName")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Itens Bipados").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("SessionShareHelper", "Erro ao compartilhar imagem dos itens bipados: ${e.message}", e)
            Toast.makeText(context, "Erro ao gerar imagem para compartilhamento", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateScannedBarcodesBitmap(
        partnerName: String,
        routeName: String,
        sessionDateStr: String,
        items: List<ScannedItemExport>
    ): Bitmap {
        val width = 800
        val useTwoColumns = items.size > 20
        val rowsCount = if (items.isEmpty()) 1 else if (useTwoColumns) (items.size + 1) / 2 else items.size
        val rowHeight = 42f
        val headerHeight = 310f
        val footerHeight = 70f
        val totalHeight = (headerHeight + (rowsCount * rowHeight) + footerHeight).toInt().coerceAtLeast(480)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fundo escuro
        canvas.drawColor(Color.parseColor("#0F0F12"))

        // Card com cantos arredondados
        val cardRect = RectF(20f, 20f, width - 20f, totalHeight - 20f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181C")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, cardPaint)

        // Borda sutil Neon
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 28f, 28f, borderPaint)

        val leftX = 50f
        val rightX = width - 50f
        var currentY = 70f

        // Cabeçalho: Nome do App
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("CENTRAL DO MOTORISTA", leftX, currentY, brandPaint)

        // Título: Itens Bipados
        currentY += 38f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Listagem de Itens Bipados", leftX, currentY, titlePaint)

        // Linha divisória
        currentY += 20f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33333E")
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Informações da sessão
        val infoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9E9EA8")
            textSize = 21f
            typeface = Typeface.DEFAULT
        }
        val infoValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        currentY += 34f
        canvas.drawText("Entregador: ", leftX, currentY, infoLabelPaint)
        canvas.drawText(partnerName, leftX + infoLabelPaint.measureText("Entregador: "), currentY, infoValuePaint)

        currentY += 30f
        canvas.drawText("Rota: ", leftX, currentY, infoLabelPaint)
        canvas.drawText(routeName, leftX + infoLabelPaint.measureText("Rota: "), currentY, infoValuePaint)

        currentY += 30f
        canvas.drawText("Data: ", leftX, currentY, infoLabelPaint)
        canvas.drawText(sessionDateStr, leftX + infoLabelPaint.measureText("Data: "), currentY, infoValuePaint)

        // Resumo: Totais
        currentY += 38f
        val deliveredCount = items.count { !it.isReturned }
        val returnedCount = items.count { it.isReturned }

        val greenPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5252")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalPillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 19f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val totalText = "Total: ${items.size}"
        val delText = "✓ Entregues: $deliveredCount"
        val retText = "✗ Devolvidos: $returnedCount"

        canvas.drawText(totalText, leftX, currentY, totalPillPaint)
        canvas.drawText(delText, leftX + 180f, currentY, greenPillPaint)
        canvas.drawText(retText, leftX + 440f, currentY, redPillPaint)

        // Linha divisória antes dos itens
        currentY += 20f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Itens
        val deliveredTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val returnedTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5252")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            flags = flags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        val strikePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF5252")
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }

        currentY += 30f
        val startY = currentY

        if (items.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#9E9EA8")
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Nenhum item bipado registrado.", width / 2f, currentY + 30f, emptyPaint)
            currentY += 60f
        } else if (useTwoColumns) {
            val col1X = leftX + 10f
            val col2X = width / 2f + 15f
            items.forEachIndexed { index, item ->
                val col = index % 2
                val row = index / 2
                val itemY = startY + (row * rowHeight)
                val itemX = if (col == 0) col1X else col2X
                val paint = if (item.isReturned) returnedTextPaint else deliveredTextPaint
                val icon = if (item.isReturned) "✗ " else "✓ "
                val fullText = "$icon${item.barcode}"
                val truncated = truncateText(fullText, paint, 310f)

                canvas.drawText(truncated, itemX, itemY, paint)

                if (item.isReturned) {
                    val textW = paint.measureText(truncated)
                    val lineY = itemY - (paint.textSize * 0.32f)
                    canvas.drawLine(itemX, lineY, itemX + textW, lineY, strikePaint)
                }
            }
            currentY = startY + (rowsCount * rowHeight)
        } else {
            items.forEachIndexed { index, item ->
                val itemY = startY + (index * rowHeight)
                val paint = if (item.isReturned) returnedTextPaint else deliveredTextPaint
                val icon = if (item.isReturned) "✗ " else "✓ "
                val fullText = "$icon${item.barcode}"
                val truncated = truncateText(fullText, paint, width - 120f)

                canvas.drawText(truncated, leftX + 10f, itemY, paint)

                if (item.isReturned) {
                    val textW = paint.measureText(truncated)
                    val lineY = itemY - (paint.textSize * 0.32f)
                    canvas.drawLine(leftX + 10f, lineY, leftX + 10f + textW, lineY, strikePaint)
                }
            }
            currentY = startY + (items.size * rowHeight)
        }

        // Linha divisória antes do rodapé
        currentY += 15f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Rodapé
        currentY += 30f
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B6B76")
            textSize = 16f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Comprovante gerado em $nowStr", width / 2f, currentY, footerPaint)

        return bitmap
    }
}

data class ScannedItemExport(
    val barcode: String,
    val isReturned: Boolean
)

