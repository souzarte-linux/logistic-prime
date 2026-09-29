package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark

private val BlueInfo = Color(0xFF1976D2)

/**
 * Diálogo de confirmação para exclusão de pacote bipado por engano (TASK-DES-08).
 * Deixa explícito ao motorista que a remoção não conta como devolução.
 */
@Composable
fun RemoveStopConfirmDialog(
    stop: MasterRouteStop,
    onDismiss: () -> Unit,
    onConfirmRemove: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(RedAlert.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = "Remover Pacote da Rota?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Deseja remover o pacote #${stop.stopOrder} da lista de entregas?",
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )

                // Bloco com dados do pacote a ser removido
                Surface(
                    color = SurfaceDarkAlt,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "📦 Código: ${stop.barcode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )
                        if (!stop.recipientName.isNullOrBlank()) {
                            Text(
                                text = "👤 Destinatário: ${stop.recipientName}",
                                fontSize = 12.sp,
                                color = TextPrimaryDark
                            )
                        }
                        if (stop.fullAddress.isNotBlank()) {
                            Text(
                                text = "📍 Endereço: ${stop.fullAddress}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }

                // Banner informativo de impacto zero nas devoluções
                Surface(
                    color = BlueInfo.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BlueInfo.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BlueInfo,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Esta remoção NÃO será contabilizada como devolução. Suas métricas de entrega permanecerão limpas.",
                            fontSize = 11.sp,
                            color = TextPrimaryDark,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedAlert,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("REMOVER PACOTE DA ROTA", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar", color = TextSecondaryDark, fontSize = 12.sp)
            }
        }
    )
}
