package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.DeliveryPartner
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.StopStatus
import com.fernando.centraldomotorista.data.model.TransferStatus
import com.fernando.centraldomotorista.ui.theme.GreenNeon
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.ui.theme.YellowGold

/**
 * Card individual de parada com suporte a estado expandido/contraído (Prompt 5)
 * e atribuição cruzada a parceiros de entrega (Prompt 6).
 */
@Composable
fun StopDeliveryCard(
    stop: MasterRouteStop,
    isNext: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateGps: (String) -> Unit,
    onUpdateStatus: (String, StopStatus) -> Unit,
    onAssignToPartner: ((partnerId: String) -> Unit)? = null,
    onEditStop: (MasterRouteStop) -> Unit = {},
    onDeleteStop: (MasterRouteStop) -> Unit = {},
    partners: List<DeliveryPartner> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isPending = stop.status == StopStatus.PENDENTE
    val isTransferred = stop.transferStatus == TransferStatus.ATRIBUIDO_PENDENTE
    var showPartnerMenu by remember { mutableStateOf(false) }

    val borderStroke = when {
        isTransferred -> BorderStroke(1.dp, YellowGold)
        isNext && isPending -> BorderStroke(1.5.dp, OrangeNeon)
        else -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNext && isPending) SurfaceDarkAlt else SurfaceDark
        ),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isNext) 6.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // --- Linha Superior (Sempre Visível mesmo Contraído) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "#${stop.stopOrder}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNext && isPending) OrangeNeon else TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        val recipient = stop.recipientName?.ifBlank { null }
                        Text(
                            text = recipient?.uppercase() ?: "PACOTE SEM NOME",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📦 ${stop.barcode}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                            if (!stop.marketplaceName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• 🏬 ${stop.marketplaceName}",
                                    fontSize = 11.sp,
                                    color = OrangeNeon,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de Transferência ou Status
                    if (isTransferred) {
                        Surface(
                            color = YellowGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "🛵 Enviado ao Parceiro",
                                color = YellowGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
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
                                    StopStatus.ENTREGUE -> "Entregue"
                                    StopStatus.AUSENTE -> "Ausente"
                                    StopStatus.DEVOLVIDO -> "Devolvido"
                                    StopStatus.PENDENTE -> if (isNext) "Próxima" else "Pendente"
                                },
                                color = when (stop.status) {
                                    StopStatus.ENTREGUE -> GreenNeon
                                    StopStatus.AUSENTE -> YellowGold
                                    StopStatus.DEVOLVIDO -> RedAlert
                                    StopStatus.PENDENTE -> if (isNext) OrangeNeon else TextSecondaryDark
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Contrair" else "Expandir",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- Conteúdo Expandido (Detalhes do Endereço, Ações e Botões) ---
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    // Tipo de Pacote Badge + Marketplace Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (stop.packageType == PackageType.VOLUMOSO) YellowGold.copy(alpha = 0.18f) else SurfaceDarkAlt,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (stop.packageType == PackageType.VOLUMOSO) "🏋️ Volumoso" else "📦 Pacotinho",
                                color = if (stop.packageType == PackageType.VOLUMOSO) YellowGold else TextSecondaryDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (!stop.marketplaceName.isNullOrBlank()) {
                            Surface(
                                color = OrangeNeon.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, OrangeNeon.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "🏬 ${stop.marketplaceName}",
                                    color = OrangeNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (!stop.cep.isNullOrBlank()) {
                            Text(
                                text = "CEP ${stop.cep}",
                                fontSize = 11.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Endereço Formatado Completo
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
                            color = TextPrimaryDark,
                            lineHeight = 18.sp
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NAVEGAR NO GPS (Maps / Waze)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botões de Status em 1 Toque
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
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenNeon, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Entregue", color = GreenNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Icon(Icons.Default.PersonOff, contentDescription = null, tint = YellowGold, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ausente", color = YellowGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, tint = RedAlert, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Devolver", color = RedAlert, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Ação de Atribuição Cruzada para Parceiro (Prompt 6)
                    if (isPending && onAssignToPartner != null && partners.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showPartnerMenu = true },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isTransferred) "Reatribuir Parceiro" else "Transferir para Parceiro",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }

                            DropdownMenu(
                                expanded = showPartnerMenu,
                                onDismissRequest = { showPartnerMenu = false }
                            ) {
                                partners.forEach { partner ->
                                    DropdownMenuItem(
                                        text = { Text(partner.fullName, fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        onClick = {
                                            showPartnerMenu = false
                                            onAssignToPartner(partner.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ações de Gestão de Pacote: Editar e Excluir
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onEditStop(stop) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceDarkAlt
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = OrangeNeon,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Editar",
                                color = TextPrimaryDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { onDeleteStop(stop) },
                            border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.35f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = RedAlert.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remover Pacote da Rota",
                                tint = RedAlert,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Remover Pacote",
                                color = RedAlert,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
