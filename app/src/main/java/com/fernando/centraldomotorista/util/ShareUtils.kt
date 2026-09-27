package com.fernando.centraldomotorista.util

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
import com.fernando.centraldomotorista.data.repository.BillingCycleWithTotals
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object ShareUtils {

    fun shareFatura(
        context: Context,
        item: BillingCycleWithTotals
    ) {
        try {
            val shortDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
            val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

            val periodStartStr = item.cycle.periodStart.format(shortDateFormatter)
            val periodEndStr = item.cycle.periodEnd.format(shortDateFormatter)
            val quantDias = ChronoUnit.DAYS.between(item.cycle.periodStart, item.cycle.periodEnd) + (if (item.cycle.includeEndDate) 1 else 0)

            val totalAmountStr = currencyFormatter.format(item.totalAmount)
            val cleanTotalStr = totalAmountStr.replace("R$", "").trim()

            val shareText = """
                📄 *DETALHES DA FATURA - LOGÍSTICA PRIME*
                🚚 *Plataforma:* ${item.platformName}
                💰 *Valor Total:* R$ $cleanTotalStr
                📦 *Qtd. Pacotes:* ${item.packageCount} pacotes
                📅 *Período:* $periodStartStr a $periodEndStr ($quantDias dias)
            """.trimIndent()

            val bitmap = generateFaturaBitmap(item, periodStartStr, periodEndStr, quantDias, totalAmountStr)

            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, "fatura_${item.cycle.id.take(8)}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Detalhes da Fatura - ${item.platformName}")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartilhar Detalhes da Fatura").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e("ShareUtils", "Erro ao compartilhar fatura: ${e.message}", e)
            Toast.makeText(context, "Erro ao gerar compartilhamento da fatura", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFaturaBitmap(
        item: BillingCycleWithTotals,
        periodStartStr: String,
        periodEndStr: String,
        quantDias: Long,
        totalAmountStr: String
    ): Bitmap {
        val width = 800
        val height = 860
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

        // Borda Neon
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardRect, 32f, 32f, borderPaint)

        val leftX = 64f
        val rightX = width - 64f
        var currentY = 82f

        // Marca
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.1f
        }
        canvas.drawText("LOGÍSTICA PRIME", leftX, currentY, brandPaint)

        // Título
        currentY += 42f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Detalhes da Fatura", leftX, currentY, titlePaint)

        // Divisória
        currentY += 24f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#33333E")
            strokeWidth = 2f
        }
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

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

        val orangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 23f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        fun drawRow(label: String, value: String, customValuePaint: Paint = valuePaint) {
            currentY += 46f
            canvas.drawText(label, leftX, currentY, labelPaint)
            canvas.drawText(value, rightX, currentY, customValuePaint)
        }

        drawRow("Plataforma:", item.platformName)
        drawRow("Período Início:", periodStartStr)
        drawRow("Período Final:", periodEndStr)
        drawRow("Quant. Dias Apurados:", "$quantDias dias de Apuração", orangePaint)
        drawRow("Qtd. Pacotes:", "${item.packageCount} pacotes")
        drawRow("Corridas / Rotas:", "${item.routeCount} corridas")

        if (item.dailyCount > 0) {
            drawRow("Diárias:", "${item.dailyCount} diárias")
        }

        currentY += 24f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        // Valor Total
        currentY += 54f
        val amountLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val amountValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E676")
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Valor Total Líquido:", leftX, currentY, amountLabelPaint)
        canvas.drawText(totalAmountStr, rightX, currentY, amountValuePaint)

        currentY += 34f
        canvas.drawLine(leftX, currentY, rightX, currentY, dividerPaint)

        currentY += 40f
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B6B76")
            textSize = 17f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Fatura gerada pelo app Logística Prime", width / 2f, currentY, footerPaint)

        return bitmap
    }
}
