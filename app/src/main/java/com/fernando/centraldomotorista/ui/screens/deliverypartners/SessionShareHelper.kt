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
}
