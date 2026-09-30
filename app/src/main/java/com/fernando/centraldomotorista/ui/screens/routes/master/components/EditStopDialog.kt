package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold

/**
 * Diálogo modal para edição completa dos dados de uma parada no Cockpit de Bordo:
 * - Nome do Cliente / Destinatário
 * - Endereço Completo
 * - CEP
 * - Tipo de Pacote (Pacotinho vs Volumoso)
 * - Plataforma / Marketplace
 * - Observações
 */
@Composable
fun EditStopDialog(
    stop: MasterRouteStop,
    platforms: List<Platform>,
    onDismiss: () -> Unit,
    onSave: (
        recipientName: String?,
        fullAddress: String,
        cep: String?,
        packageType: PackageType,
        platformId: String?,
        notes: String?
    ) -> Unit
) {
    var recipientName by remember { mutableStateOf(stop.recipientName ?: "") }
    var fullAddress by remember { mutableStateOf(stop.fullAddress) }
    var cep by remember { mutableStateOf(stop.cep ?: "") }
    var selectedPackageType by remember { mutableStateOf(stop.packageType ?: PackageType.PACOTINHO) }
    var selectedPlatformId by remember { mutableStateOf(stop.platformId) }
    var notes by remember { mutableStateOf(stop.notes ?: "") }

    var isPlatformDropdownExpanded by remember { mutableStateOf(false) }

    val activePlatformName = platforms.firstOrNull { it.id == selectedPlatformId }?.name
        ?: stop.platformId?.takeIf { it.isNotBlank() }
        ?: "Plataforma Geral"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(18.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Editar Pacote #${stop.stopOrder}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Código em destaque
                Surface(
                    color = SurfaceDarkAlt,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📦 Código: ${stop.barcode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OrangeNeon,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Campo: Nome do Destinatário
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = { recipientName = it },
                    label = { Text("Nome do Cliente", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo: Endereço Completo
                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it },
                    label = { Text("Endereço Completo", fontSize = 11.sp) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Campo: CEP
                OutlinedTextField(
                    value = cep,
                    onValueChange = { cep = it },
                    label = { Text("CEP", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Seletor Tipo: Pacotinho vs Volumoso
                Text(
                    text = "Tipo de Pacote:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isPacote = selectedPackageType == PackageType.PACOTINHO
                    Surface(
                        color = if (isPacote) OrangeNeon else SurfaceDarkAlt,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isPacote) OrangeNeon else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPackageType = PackageType.PACOTINHO }
                    ) {
                        Text(
                            text = "📦 Pacotinho",
                            color = if (isPacote) Color.White else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    val isVolumoso = selectedPackageType == PackageType.VOLUMOSO
                    Surface(
                        color = if (isVolumoso) YellowGold else SurfaceDarkAlt,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isVolumoso) YellowGold else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedPackageType = PackageType.VOLUMOSO }
                    ) {
                        Text(
                            text = "🏋️ Volumoso",
                            color = if (isVolumoso) Color.Black else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Seletor Plataforma / Marketplace
                Text(
                    text = "Plataforma / Marketplace:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    color = SurfaceDarkAlt,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPlatformDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = activePlatformName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondaryDark
                        )
                    }

                    DropdownMenu(
                        expanded = isPlatformDropdownExpanded,
                        onDismissRequest = { isPlatformDropdownExpanded = false }
                    ) {
                        platforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform.name, fontSize = 12.sp) },
                                leadingIcon = if (platform.id == selectedPlatformId) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = OrangeNeon) }
                                } else null,
                                onClick = {
                                    selectedPlatformId = platform.id
                                    isPlatformDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Campo: Observações
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)", fontSize = 11.sp) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        recipientName.takeIf { it.isNotBlank() },
                        fullAddress.trim(),
                        cep.takeIf { it.isNotBlank() },
                        selectedPackageType,
                        selectedPlatformId,
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeNeon,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SALVAR ALTERAÇÕES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
