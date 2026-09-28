package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark

/**
 * Diálogo modal para encerramento de rota e hand-off de dados operacionais
 * para a tela financeira de ganhos (NewRouteScreen).
 */
@Composable
fun FinishRouteDialog(
    platformName: String?,
    totalPackages: Int,
    deliveredCount: Int,
    returnedCount: Int,
    onDismiss: () -> Unit,
    onFinishAndLaunchEarnings: (kmEnd: String) -> Unit,
    onFinishOnly: () -> Unit
) {
    var endKmText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "Encerrar Rota do Dia",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🎉 Parabéns, você concluiu as operações desta rota!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenNeon
                )

                // Card com Resumo Operacional
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "RESUMO OPERACIONAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )

                        if (!platformName.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Plataforma:", fontSize = 12.sp, color = TextSecondaryDark)
                                Text(platformName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total de Pacotes:", fontSize = 12.sp, color = TextSecondaryDark)
                            Text("$totalPackages", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Entregues com Sucesso:", fontSize = 12.sp, color = TextSecondaryDark)
                            Text("$deliveredCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenNeon)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Devoluções / Ausentes:", fontSize = 12.sp, color = TextSecondaryDark)
                            Text("$returnedCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RedAlert)
                        }
                    }
                }

                // Campo Odômetro Final (Opcional)
                OutlinedTextField(
                    value = endKmText,
                    onValueChange = { endKmText = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' } },
                    label = { Text("KM Final no Painel (Opcional)", fontSize = 12.sp) },
                    placeholder = { Text("Ex: 145820", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = SurfaceDarkAlt,
                        unfocusedContainerColor = SurfaceDarkAlt,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Botão Primário: Concluir e Lançar Ganhos
                Button(
                    onClick = { onFinishAndLaunchEarnings(endKmText) },
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
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CONCLUIR E LANÇAR GANHOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Botão Secundário: Apenas Fechar Rota
                OutlinedButton(
                    onClick = onFinishOnly,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Salvar Rota e Encerrar Sem Lançar Ganhos",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Voltar ao Cockpit", color = TextSecondaryDark)
            }
        }
    )
}
