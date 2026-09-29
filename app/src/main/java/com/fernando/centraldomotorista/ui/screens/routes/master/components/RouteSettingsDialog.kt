package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.preferences.RoutePreferences
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark

/**
 * Diálogo modal para configuração das preferências da Rota Master.
 * Permite ao motorista definir a política de expiração/retenção das fotos
 * de etiquetas armazenadas temporariamente no Supabase Storage.
 */
@Composable
fun RouteSettingsDialog(
    currentRetentionDays: Int = RoutePreferences.DEFAULT_PHOTO_RETENTION_DAYS,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var selectedDays by remember(currentRetentionDays) { mutableIntStateOf(currentRetentionDays) }

    val retentionOptions = listOf(
        1 to "1 dia",
        3 to "3 dias (Padrão)",
        7 to "7 dias",
        15 to "15 dias",
        30 to "30 dias"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OrangeNeon.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Text(
                    text = "Configurações da Rota",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimaryDark
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Seção: Limpeza Automática de Fotos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "LIMPEZA AUTOMÁTICA DE FOTOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon,
                        letterSpacing = 0.5.sp
                    )
                }

                // Card explicativo sobre economia de armazenamento no Supabase
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDarkAlt,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondaryDark,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = "As fotos das etiquetas são salvas como backup temporário para conferência operacional. Após o prazo selecionado, os arquivos físicos são excluídos automaticamente para poupar armazenamento na nuvem.",
                            fontSize = 11.5.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text(
                    text = "Prazo de retenção após conclusão:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )

                // Lista de opções selecionáveis
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    retentionOptions.forEach { (days, label) ->
                        val isSelected = selectedDays == days
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) OrangeNeon.copy(alpha = 0.12f) else SurfaceDarkAlt,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) OrangeNeon else Color.White.copy(alpha = 0.06f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDays = days }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedDays = days },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = OrangeNeon,
                                        unselectedColor = TextSecondaryDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedDays) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "SALVAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        }
    )
}
