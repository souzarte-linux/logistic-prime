package com.fernando.centraldomotorista.ui.screens.historico.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.TransactionItem
import com.fernando.centraldomotorista.ui.screens.historico.DayGroup
import com.fernando.centraldomotorista.ui.screens.historico.TransactionCard
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private fun BigDecimal.formatCurrency(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(this)
}

/**
 * Componente que renderiza um dia na linha do tempo contínua do Histórico.
 *
 * @param day Dados do grupo do dia (label, lista de transações, saldo).
 * @param isFirst Se é o primeiro dia exibido na semana (se true, a linha superior não é desenhada).
 * @param isLast Se é o último dia exibido na semana (se true, a linha inferior não é desenhada).
 * @param onEditTransaction Callback para editar a transação selecionada.
 * @param onDeleteTransaction Callback para excluir a transação selecionada.
 * @param onClickTransaction Callback para abrir detalhes/visualização da transação.
 */
@Composable
fun TimelineDayItem(
    day: DayGroup,
    isFirst: Boolean,
    isLast: Boolean,
    onEditTransaction: (TransactionItem) -> Unit,
    onDeleteTransaction: (TransactionItem) -> Unit,
    onClickTransaction: (TransactionItem) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Coluna lateral com linha do tempo contínua e indicador/dot circular OrangeNeon
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2f
                    val dotCenterY = 12.dp.toPx()
                    val dotRadius = 4.5.dp.toPx()
                    val strokeWidth = 2.dp.toPx()

                    val lineColor = OrangeNeon.copy(alpha = 0.35f)
                    val dotColor = OrangeNeon

                    // Linha superior estendendo até o topo se não for o primeiro dia
                    if (!isFirst) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, dotCenterY - dotRadius),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Halo sutil ao redor do dot
                    drawCircle(
                        color = dotColor.copy(alpha = 0.2f),
                        radius = dotRadius + 2.5.dp.toPx(),
                        center = Offset(centerX, dotCenterY)
                    )

                    // Dot circular OrangeNeon no alinhamento do título do dia
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(centerX, dotCenterY)
                    )

                    // Linha inferior estendendo até a base se não for o último dia
                    if (!isLast) {
                        drawLine(
                            color = lineColor,
                            start = Offset(centerX, dotCenterY + dotRadius),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokeWidth
                        )
                    }
                }
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Conteúdo do Dia: Título, Lista de Transações e Saldo do Dia
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Título do Dia (ex: TERÇA-FEIRA, 15 DE SET)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = day.label.uppercase(Locale.ROOT),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                )
            }

            // Lista de Transações do Dia
            day.items.forEach { item ->
                TransactionCard(
                    item = item,
                    onClick = { onClickTransaction(item) },
                    onEdit = { onEditTransaction(item) },
                    onDelete = { onDeleteTransaction(item) }
                )
            }

            // Linha "SALDO DO DIA"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SALDO DO DIA",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Text(
                        text = day.balance.formatCurrency(),
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (day.balance >= BigDecimal.ZERO) OrangeNeon else RedAlert
                        )
                    )
                }
            }
        }
    }
}
