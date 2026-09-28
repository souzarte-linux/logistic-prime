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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold

/**
 * Card Hero superior do Cockpit exibindo métricas consolidadas da rota ativa:
 * Progresso percentual, contadores de status e botão de acesso rápido à bipagem adicional.
 */
@Composable
fun RouteProgressHero(
    platformName: String?,
    startLocation: String,
    totalPackages: Int,
    deliveredCount: Int,
    pendingCount: Int,
    returnedCount: Int,
    onAddMoreStops: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalPackages > 0) {
        deliveredCount.toFloat() / totalPackages.toFloat()
    } else 0f

    val percentage = (progress * 100).toInt().coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Linha Superior: Origem/Plataforma e Botão de Scanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = platformName ?: "Rota Operacional",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "🏁 Partida: $startLocation",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }

                Button(
                    onClick = onAddMoreStops,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDarkAlt,
                        contentColor = OrangeNeon
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Bipar Mais",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Bipar Mais",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de Progresso e Percentual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progresso: $deliveredCount de $totalPackages Entregues",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "$percentage%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeNeon
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = OrangeNeon,
                trackColor = SurfaceDarkAlt,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chips com Contadores de Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Entregues
                Surface(
                    color = GreenNeon.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, GreenNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$deliveredCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenNeon
                        )
                        Text(
                            text = "Entregues",
                            fontSize = 11.sp,
                            color = GreenNeon
                        )
                    }
                }

                // Pendentes
                Surface(
                    color = YellowGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, YellowGold.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$pendingCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowGold
                        )
                        Text(
                            text = "Pendentes",
                            fontSize = 11.sp,
                            color = YellowGold
                        )
                    }
                }

                // Devoluções / Ausentes
                Surface(
                    color = RedAlert.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$returnedCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedAlert
                        )
                        Text(
                            text = "Devoluções",
                            fontSize = 11.sp,
                            color = RedAlert
                        )
                    }
                }
            }
        }
    }
}
