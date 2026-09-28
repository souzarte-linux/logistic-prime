package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold

/**
 * Card individual de parada/pacote para o Cockpit de Bordo veicular.
 */
@Composable
fun StopDeliveryCard(
    stop: MasterRouteStop,
    isNext: Boolean,
    onNavigateGps: (String) -> Unit,
    onUpdateStatus: (String, StopStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPending = stop.status == StopStatus.PENDENTE
    val borderStroke = if (isNext && isPending) {
        BorderStroke(1.5.dp, OrangeNeon)
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext && isPending) SurfaceDarkAlt else SurfaceDark
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 6.dp else 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabeçalho da Parada (Número, Próxima Parada Badge, Código de Rastreio)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${stop.stopOrder}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNext && isPending) OrangeNeon else TextPrimaryDark
                    )

                    if (isNext && isPending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = OrangeNeon.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "PRÓXIMA PARADA",
                                color = OrangeNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Badge de Status Atual ou Código de Barras
                Surface(
                    color = when (stop.status) {
                        StopStatus.ENTREGUE -> GreenNeon.copy(alpha = 0.15f)
                        StopStatus.AUSENTE -> YellowGold.copy(alpha = 0.15f)
                        StopStatus.DEVOLVIDO -> RedAlert.copy(alpha = 0.15f)
                        StopStatus.PENDENTE -> SurfaceDarkAlt
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (stop.status) {
                            StopStatus.ENTREGUE -> "✅ Entregue"
                            StopStatus.AUSENTE -> "👤 Ausente"
                            StopStatus.DEVOLVIDO -> "↩️ Devolvido"
                            StopStatus.PENDENTE -> "📦 ${stop.barcode.takeLast(10)}"
                        },
                        color = when (stop.status) {
                            StopStatus.ENTREGUE -> GreenNeon
                            StopStatus.AUSENTE -> YellowGold
                            StopStatus.DEVOLVIDO -> RedAlert
                            StopStatus.PENDENTE -> TextSecondaryDark
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nome do Destinatário
            if (!stop.recipientName.isNullOrBlank()) {
                Text(
                    text = "👤 ${stop.recipientName.uppercase()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Endereço Formatado
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stop.fullAddress.ifBlank { "Endereço não identificado na etiqueta" },
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )
            }

            // Código de barras completo se status não for pendente
            if (stop.status != StopStatus.PENDENTE) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Código: ${stop.barcode}",
                    fontSize = 11.sp,
                    color = TextSecondaryDark.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botão Primário Hero: NAVEGAR NO GPS
            Button(
                onClick = { onNavigateGps(stop.fullAddress) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NAVEGAR NO GPS (Maps / Waze)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Botões de Ação Rápida de 1 Toque para Atualização de Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Entregue
                OutlinedButton(
                    onClick = { onUpdateStatus(stop.id, StopStatus.ENTREGUE) },
                    border = BorderStroke(
                        1.dp,
                        if (stop.status == StopStatus.ENTREGUE) GreenNeon else GreenNeon.copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (stop.status == StopStatus.ENTREGUE) GreenNeon.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = GreenNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Entregue",
                        color = GreenNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Ausente
                OutlinedButton(
                    onClick = { onUpdateStatus(stop.id, StopStatus.AUSENTE) },
                    border = BorderStroke(
                        1.dp,
                        if (stop.status == StopStatus.AUSENTE) YellowGold else YellowGold.copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (stop.status == StopStatus.AUSENTE) YellowGold.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = YellowGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ausente",
                        color = YellowGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Devolver
                OutlinedButton(
                    onClick = { onUpdateStatus(stop.id, StopStatus.DEVOLVIDO) },
                    border = BorderStroke(
                        1.dp,
                        if (stop.status == StopStatus.DEVOLVIDO) RedAlert else RedAlert.copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (stop.status == StopStatus.DEVOLVIDO) RedAlert.copy(alpha = 0.2f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.AssignmentReturn,
                        contentDescription = null,
                        tint = RedAlert,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Devolver",
                        color = RedAlert,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
