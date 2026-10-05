package com.fernando.centraldomotorista.ui.screens.routes.master.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.Marketplace
import com.fernando.centraldomotorista.data.model.MasterRouteStop
import com.fernando.centraldomotorista.data.model.PackageType
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.remote.api.ViaCepApi
import com.fernando.centraldomotorista.data.repository.MarketplaceRepository
import com.fernando.centraldomotorista.ui.components.CreateMarketplaceDialog
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.SurfaceDark
import com.fernando.centraldomotorista.ui.theme.SurfaceDarkAlt
import com.fernando.centraldomotorista.ui.theme.TextPrimaryDark
import com.fernando.centraldomotorista.ui.theme.TextSecondaryDark
import com.fernando.centraldomotorista.util.AddressFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Diálogo modal para edição completa dos dados de uma parada no Cockpit de Bordo ou Bipagem:
 * - Nome do Cliente / Destinatário
 * - Endereço Completo (atualizado com ViaCEP ao alterar CEP)
 * - CEP (com validação assíncrona automática)
 * - Tipo de Carga / Pacote (5 tipos operacionais)
 * - Transportadora / Plataforma
 * - Tomador / Marketplace (com opção + Nova Empresa sob demanda)
 * - Observações
 */
@Composable
fun EditStopDialog(
    stop: MasterRouteStop,
    platforms: List<Platform>,
    marketplaces: List<Marketplace> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        recipientName: String?,
        fullAddress: String,
        cep: String?,
        packageType: PackageType,
        platformId: String?,
        marketplaceName: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSearchingCep by remember { mutableStateOf(false) }
    var lastSearchedCep by remember { mutableStateOf<String?>(null) }

    var recipientName by remember { mutableStateOf(stop.recipientName ?: "") }
    var fullAddress by remember { mutableStateOf(stop.fullAddress) }
    var cep by remember { mutableStateOf(stop.cep ?: "") }
    var selectedPackageType by remember { mutableStateOf(stop.packageType ?: PackageType.PACOTINHO) }
    var selectedPlatformId by remember { mutableStateOf(stop.platformId) }
    var selectedMarketplaceName by remember { mutableStateOf(stop.marketplaceName ?: "") }
    var notes by remember { mutableStateOf(stop.notes ?: "") }

    var localMarketplaces by remember(marketplaces) {
        val custom = MarketplaceRepository.getCustomMarketplaces(context)
        mutableStateOf((marketplaces + custom).distinctBy { it.name.lowercase().trim() })
    }
    var showCreateMarketplaceDialog by remember { mutableStateOf(false) }

    var isPackageTypeDropdownExpanded by remember { mutableStateOf(false) }
    var isPlatformDropdownExpanded by remember { mutableStateOf(false) }
    var isMarketplaceDropdownExpanded by remember { mutableStateOf(false) }

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

                // Campo: CEP com busca automática via ViaCEP
                OutlinedTextField(
                    value = cep,
                    onValueChange = { newCep ->
                        cep = newCep
                        val cleanCep = newCep.replace(Regex("""\D"""), "")
                        if (cleanCep.length == 8 && cleanCep != lastSearchedCep) {
                            lastSearchedCep = cleanCep
                            scope.launch {
                                isSearchingCep = true
                                try {
                                    val viaCepResult = withContext(Dispatchers.IO) {
                                        ViaCepApi.instance.getAddressByCep(cleanCep)
                                    }
                                    if (viaCepResult.erro != true) {
                                        val officialStreet = viaCepResult.logradouro?.takeIf { it.isNotBlank() }
                                        val officialNeighborhood = viaCepResult.bairro?.takeIf { it.isNotBlank() }
                                        val officialCity = viaCepResult.localidade?.takeIf { it.isNotBlank() }
                                        val officialState = viaCepResult.uf?.takeIf { it.isNotBlank() }
                                        val formattedCep = "${cleanCep.substring(0, 5)}-${cleanCep.substring(5)}"

                                        val merged = AddressFormatter.mergeAddressPreservingDetails(
                                            previousAddress = fullAddress,
                                            officialStreet = officialStreet,
                                            officialNeighborhood = officialNeighborhood,
                                            officialCity = officialCity,
                                            officialState = officialState,
                                            cep = formattedCep,
                                            viaCepComplement = viaCepResult.complemento?.takeIf { it.isNotBlank() }
                                        )

                                        withContext(Dispatchers.Main) {
                                            fullAddress = merged
                                            cep = formattedCep
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Mantém o valor digitado caso ocorra falha de conexão
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isSearchingCep = false
                                    }
                                }
                            }
                        }
                    },
                    label = { Text("CEP", fontSize = 11.sp) },
                    singleLine = true,
                    trailingIcon = {
                        if (isSearchingCep) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = OrangeNeon
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Seletor Tipo de Carga / Pacote: Combobox com 5 opções operacionais
                Text(
                    text = "Tipo de Pacote / Carga:",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.SemiBold
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = SurfaceDarkAlt,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPackageTypeDropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cargoLabel = when (selectedPackageType) {
                                PackageType.PACOTINHO -> "📦 Pacote"
                                PackageType.VOLUMOSO -> "🏋️ Volumoso"
                                PackageType.DOCUMENTO -> "📄 Documento"
                                PackageType.COMIDA -> "🍔 Comida"
                                PackageType.FARMACIA -> "💊 Farmácia"
                            }
                            Text(
                                text = cargoLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeNeon
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Selecionar Tipo",
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isPackageTypeDropdownExpanded,
                        onDismissRequest = { isPackageTypeDropdownExpanded = false },
                        containerColor = SurfaceDarkAlt,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.background(SurfaceDarkAlt)
                    ) {
                        PackageType.entries.forEach { pkgType ->
                            val itemLabel = when (pkgType) {
                                PackageType.PACOTINHO -> "📦 Pacote"
                                PackageType.VOLUMOSO -> "🏋️ Volumoso"
                                PackageType.DOCUMENTO -> "📄 Documento"
                                PackageType.COMIDA -> "🍔 Comida"
                                PackageType.FARMACIA -> "💊 Farmácia"
                            }
                            val isSelected = pkgType == selectedPackageType
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = itemLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) OrangeNeon else TextPrimaryDark
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = TextPrimaryDark,
                                    leadingIconColor = TextPrimaryDark
                                ),
                                onClick = {
                                    selectedPackageType = pkgType
                                    isPackageTypeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Seletor Transportadora / Plataforma
                Text(
                    text = "🚚 Transportadora / Plataforma:",
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
                        onDismissRequest = { isPlatformDropdownExpanded = false },
                        containerColor = SurfaceDarkAlt,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.background(SurfaceDarkAlt)
                    ) {
                        platforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform.name, fontSize = 12.sp, color = TextPrimaryDark) },
                                leadingIcon = if (platform.id == selectedPlatformId) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = OrangeNeon) }
                                } else null,
                                colors = MenuDefaults.itemColors(textColor = TextPrimaryDark),
                                onClick = {
                                    selectedPlatformId = platform.id
                                    isPlatformDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Seletor Tomador / Marketplace
                Text(
                    text = "🏬 Tomador / Marketplace:",
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
                        .clickable { isMarketplaceDropdownExpanded = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedMarketplaceName.isNotBlank()) "🏬 $selectedMarketplaceName" else "🏬 Nenhum / Não Informado",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedMarketplaceName.isNotBlank()) OrangeNeon else TextSecondaryDark
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondaryDark
                        )
                    }

                    DropdownMenu(
                        expanded = isMarketplaceDropdownExpanded,
                        onDismissRequest = { isMarketplaceDropdownExpanded = false },
                        containerColor = SurfaceDarkAlt,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.background(SurfaceDarkAlt)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhum / Não informado", fontSize = 12.sp, color = TextPrimaryDark) },
                            colors = MenuDefaults.itemColors(textColor = TextPrimaryDark),
                            onClick = {
                                selectedMarketplaceName = ""
                                isMarketplaceDropdownExpanded = false
                            }
                        )
                        localMarketplaces.forEach { marketplace ->
                            val isSelected = marketplace.name.equals(selectedMarketplaceName, ignoreCase = true)
                            DropdownMenuItem(
                                text = { Text(marketplace.name, fontSize = 12.sp, color = if (isSelected) OrangeNeon else TextPrimaryDark) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = OrangeNeon) }
                                } else null,
                                colors = MenuDefaults.itemColors(textColor = TextPrimaryDark),
                                onClick = {
                                    selectedMarketplaceName = marketplace.name
                                    isMarketplaceDropdownExpanded = false
                                }
                            )
                        }

                        // Última opção: Cadastrar nova empresa / tomador sob demanda
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "+ Nova Empresa",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OrangeNeon
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = OrangeNeon,
                                leadingIconColor = OrangeNeon
                            ),
                            onClick = {
                                isMarketplaceDropdownExpanded = false
                                showCreateMarketplaceDialog = true
                            }
                        )
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
                        selectedMarketplaceName.takeIf { it.isNotBlank() },
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

    if (showCreateMarketplaceDialog) {
        CreateMarketplaceDialog(
            onDismiss = { showCreateMarketplaceDialog = false },
            onConfirm = { newName ->
                val created = MarketplaceRepository.saveCustomMarketplace(newName, context)
                if (localMarketplaces.none { it.name.equals(created.name, ignoreCase = true) }) {
                    localMarketplaces = localMarketplaces + created
                }
                selectedMarketplaceName = created.name
                showCreateMarketplaceDialog = false
            }
        )
    }
}
