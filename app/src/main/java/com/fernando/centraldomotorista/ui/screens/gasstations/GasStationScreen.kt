package com.fernando.centraldomotorista.ui.screens.gasstations

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.GasStation
import com.fernando.centraldomotorista.data.remote.dto.NearbyGasStation
import com.fernando.centraldomotorista.ui.theme.*

val AVAILABLE_BRANDS = listOf("Shell", "BR / Petrobras", "Ipiranga", "Raízen", "Ale", "TotalEnergies", "Outra")
val ALL_FUEL_TYPES = listOf("Gasolina Comum", "Gasolina Aditivada", "Etanol", "GNV", "Diesel")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GasStationScreen(
    viewModel: GasStationViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var brandMenuExpanded by remember { mutableStateOf(false) }

    // Launcher de permissões de localização
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.findNearbyGasStations(context)
        } else {
            Toast.makeText(
                context,
                "Permissão de GPS necessária para localizar postos próximos.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestGpsAndFindStations() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            viewModel.findNearbyGasStations(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "POSTOS DE GASOLINA",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // 1. Formulário de Cadastro / Edição
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.editingStationId != null) "EDITAR POSTO" else "CADASTRAR NOVO POSTO",
                                color = OrangeNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.isLocatingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(4.dp),
                                        color = OrangeNeon,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    FilledTonalIconButton(
                                        onClick = { requestGpsAndFindStations() },
                                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor = OrangeNeon.copy(alpha = 0.15f),
                                            contentColor = OrangeNeon
                                        ),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GpsFixed,
                                            contentDescription = "Localizar Postos no Mapa (GPS)",
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (uiState.editingStationId != null) {
                                    TextButton(onClick = { viewModel.cancelEditing() }) {
                                        Text("Cancelar", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Nome Fantasia (Obrigatório)
                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.onNameChanged(it) },
                            label = { Text("Nome do Posto * (Fantasia)") },
                            placeholder = { Text("Ex: Posto Ipiranga Centro") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Apelido (Opcional)
                        OutlinedTextField(
                            value = uiState.nickname,
                            onValueChange = { viewModel.onNicknameChanged(it) },
                            label = { Text("Apelido / Ponto de Referência (Opcional)") },
                            placeholder = { Text("Ex: Posto da esquina com a padaria") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Bandeira (Dropdown + Botão "+")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = brandMenuExpanded,
                                onExpandedChange = { brandMenuExpanded = !brandMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = uiState.brand,
                                    onValueChange = { viewModel.onBrandChanged(it) },
                                    label = { Text("Bandeira") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandMenuExpanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = Color.DarkGray,
                                        unfocusedLabelColor = Color.Gray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = brandMenuExpanded,
                                    onDismissRequest = { brandMenuExpanded = false },
                                    modifier = Modifier.background(SurfaceDark)
                                ) {
                                    uiState.allBrands.forEach { brandOption ->
                                        DropdownMenuItem(
                                            text = { Text(brandOption, color = Color.White) },
                                            onClick = {
                                                viewModel.onBrandChanged(brandOption)
                                                brandMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.openAddBrandDialog() },
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Nova Bandeira",
                                    tint = OrangeNeon
                                )
                            }
                        }

                        // Seção Endereço
                        Text(
                            text = "ENDEREÇO",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // CEP com busca automática
                        OutlinedTextField(
                            value = uiState.cep,
                            onValueChange = { viewModel.onCepChanged(it) },
                            label = { Text("CEP (Busca automática)") },
                            placeholder = { Text("00000-000") },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.isSearchingCep) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = OrangeNeon, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar CEP", tint = OrangeNeon)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Rua e Número
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.street,
                                onValueChange = { viewModel.onStreetChanged(it) },
                                label = { Text("Rua / Av.") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(2.5f)
                            )
                            OutlinedTextField(
                                value = uiState.number,
                                onValueChange = { viewModel.onNumberChanged(it) },
                                label = { Text("Nº *") },
                                placeholder = { Text("123") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Bairro, Cidade e Estado
                        OutlinedTextField(
                            value = uiState.neighborhood,
                            onValueChange = { viewModel.onNeighborhoodChanged(it) },
                            label = { Text("Bairro") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.city,
                                onValueChange = { viewModel.onCityChanged(it) },
                                label = { Text("Cidade") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(2.5f)
                            )
                            OutlinedTextField(
                                value = uiState.state,
                                onValueChange = { viewModel.onStateChanged(it.uppercase()) },
                                label = { Text("UF") },
                                placeholder = { Text("SP") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Combustíveis Disponíveis (Checkboxes)
                        Text(
                            text = "COMBUSTÍVEIS DISPONÍVEIS",
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ALL_FUEL_TYPES.forEach { fuelType ->
                                val isChecked = uiState.selectedFuelTypes.contains(fuelType)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleFuelType(fuelType) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { viewModel.toggleFuelType(fuelType) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = OrangeNeon,
                                            checkmarkColor = Color.Black,
                                            uncheckedColor = Color.Gray
                                        )
                                    )
                                    Text(
                                        text = fuelType,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                            }
                        }

                        // Botão Salvar Posto
                        Button(
                            onClick = { viewModel.saveStation() },
                            enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (uiState.editingStationId != null) "Atualizar Posto" else "Salvar Posto",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Título da Lista e Busca
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "POSTOS CADASTRADOS (${uiState.stations.size})",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Buscar por nome, bandeira ou endereço...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Itens da Lista
            val filteredStations = uiState.stations.filter { station ->
                val query = uiState.searchQuery.trim().lowercase()
                query.isEmpty() ||
                        station.name.lowercase().contains(query) ||
                        station.brand.lowercase().contains(query) ||
                        (station.nickname?.lowercase()?.contains(query) == true) ||
                        (station.address?.lowercase()?.contains(query) == true)
            }

            if (filteredStations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (uiState.isLoading) "Carregando postos..." else "Nenhum posto encontrado.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredStations, key = { it.id }) { station ->
                    GasStationCard(
                        station = station,
                        onEdit = { viewModel.startEditing(station) },
                        onDelete = { viewModel.deleteStation(station.id) }
                    )
                }
            }
        }
    }

    if (uiState.showNearbyStationsDialog && uiState.userLatitude != null && uiState.userLongitude != null) {
        NearbyGasStationsDialog(
            userLat = uiState.userLatitude!!,
            userLon = uiState.userLongitude!!,
            stations = uiState.nearbyStations,
            isLoading = uiState.isLocatingGps,
            onSelectStation = { station ->
                viewModel.selectNearbyStation(context, station)
            },
            onDismiss = { viewModel.closeNearbyStationsDialog() }
        )
    }

    if (uiState.isAddBrandDialogOpen) {
        var brandNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.closeAddBrandDialog() },
            title = { Text("Nova Bandeira de Posto", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = brandNameInput,
                    onValueChange = { brandNameInput = it },
                    label = { Text("Nome da Bandeira / Distribuidora") },
                    placeholder = { Text("Ex: Ipiranga, Petrobras...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addBrand(brandNameInput)
                    },
                    enabled = brandNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Salvar Bandeira")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeAddBrandDialog() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun NearbyGasStationsDialog(
    userLat: Double,
    userLon: Double,
    stations: List<NearbyGasStation>,
    isLoading: Boolean,
    onSelectStation: (NearbyGasStation) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header do Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "POSTOS PRÓXIMOS (GPS)",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (stations.isEmpty() && !isLoading) "Nenhum posto encontrado num raio de 5km" else "${stations.size} postos encontrados num raio de 5km",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.LightGray
                        )
                    }
                }

                // 1. Mapa Interativo (Leaflet Dark Mode via AndroidView WebView)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = WebViewClient()

                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onStationSelected(stationId: String) {
                                        val match = stations.firstOrNull { it.id == stationId }
                                        if (match != null) {
                                            post { onSelectStation(match) }
                                        }
                                    }
                                }, "AndroidMapBridge")

                                val html = generateLeafletMapHtml(userLat, userLon, stations)
                                loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
                            }
                        },
                        update = { webView ->
                            val html = generateLeafletMapHtml(userLat, userLon, stations)
                            webView.loadDataWithBaseURL("https://osm.org", html, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. Lista de Postos Próximos
                Text(
                    text = "SELECIONE UM POSTO PARA PREENCHER O FORMULÁRIO:",
                    color = OrangeNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = OrangeNeon, strokeWidth = 2.dp)
                            Text(
                                text = "Buscando postos próximos no OpenStreetMap...",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else if (stations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum posto de gasolina localizado nas proximidades.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stations, key = { it.id }) { station ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectStation(station) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Ícone e Distância
                                    Surface(
                                        color = OrangeNeon.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocalGasStation,
                                                contentDescription = null,
                                                tint = OrangeNeon,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = if (station.distanceMeters >= 1000) "${String.format(java.util.Locale.US, "%.1f", station.distanceMeters / 1000f)}km" else "${station.distanceMeters.toInt()}m",
                                                color = OrangeNeon,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Dados do Posto
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = station.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                color = OrangeNeon.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = station.brand,
                                                    color = OrangeNeon,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = station.fullAddress,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (station.fuelTypes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                station.fuelTypes.take(3).forEach { fuel ->
                                                    Text(
                                                        text = fuel,
                                                        color = Color.Gray,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier
                                                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { onSelectStation(station) },
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Usar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateLeafletMapHtml(
    userLat: Double,
    userLon: Double,
    stations: List<NearbyGasStation>
): String {
    val stationsJsonArray = stations.joinToString(",") { station ->
        val safeName = station.name.replace("'", "\\'").replace("\"", "\\\"")
        val safeBrand = station.brand.replace("'", "\\'").replace("\"", "\\\"")
        val safeAddress = station.fullAddress.replace("'", "\\'").replace("\"", "\\\"")
        val distText = if (station.distanceMeters >= 1000) "${String.format(java.util.Locale.US, "%.1f", station.distanceMeters / 1000f)}km" else "${station.distanceMeters.toInt()}m"
        """
        {
            "id": "${station.id}",
            "name": "$safeName",
            "brand": "$safeBrand",
            "address": "$safeAddress",
            "lat": ${station.latitude},
            "lon": ${station.longitude},
            "dist": "$distText"
        }
        """.trimIndent()
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: #121212;
                }
                .leaflet-container {
                    background-color: #121212;
                }
                .custom-user-pin {
                    width: 16px;
                    height: 16px;
                    background: #00e5ff;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 12px #00e5ff;
                }
                .custom-station-pin {
                    background: #ff5500;
                    border: 2px solid #ffffff;
                    border-radius: 50%;
                    color: white;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: bold;
                    font-size: 11px;
                    box-shadow: 0 0 8px rgba(255, 85, 0, 0.8);
                }
                .leaflet-popup-content-wrapper {
                    background: #1e1e1e;
                    color: #ffffff;
                    border: 1px solid #ff5500;
                    border-radius: 10px;
                    padding: 4px;
                }
                .leaflet-popup-tip {
                    background: #1e1e1e;
                }
                .popup-title {
                    font-weight: bold;
                    color: #ff5500;
                    font-size: 13px;
                }
                .popup-sub {
                    color: #cccccc;
                    font-size: 11px;
                    margin: 3px 0;
                }
                .popup-btn {
                    background: #ff5500;
                    color: #000;
                    font-weight: bold;
                    border: none;
                    border-radius: 6px;
                    padding: 5px 10px;
                    margin-top: 5px;
                    cursor: pointer;
                    width: 100%;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', { zoomControl: false }).setView([$userLat, $userLon], 14);

                L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                    maxZoom: 19,
                    attribution: '© OpenStreetMap'
                }).addTo(map);

                // Marcador do Usuário
                var userIcon = L.divIcon({
                    className: 'custom-user-pin',
                    iconSize: [16, 16],
                    iconAnchor: [8, 8]
                });
                L.marker([$userLat, $userLon], { icon: userIcon }).addTo(map).bindPopup("<b>Você está aqui</b>");

                // Marcadores dos Postos
                var stations = [$stationsJsonArray];
                stations.forEach(function(st) {
                    var stationIcon = L.divIcon({
                        className: 'custom-station-pin',
                        html: '⛽',
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });

                    var marker = L.marker([st.lat, st.lon], { icon: stationIcon }).addTo(map);
                    
                    var popupHtml = '<div class="popup-title">' + st.name + '</div>' +
                                    '<div class="popup-sub"><b>Bandeira:</b> ' + st.brand + ' (' + st.dist + ')</div>' +
                                    '<div class="popup-sub">' + st.address + '</div>' +
                                    '<button class="popup-btn" onclick="selectStation(\'' + st.id + '\')">Selecionar este Posto</button>';
                    
                    marker.bindPopup(popupHtml);
                });

                function selectStation(id) {
                    if (window.AndroidMapBridge && window.AndroidMapBridge.onStationSelected) {
                        window.AndroidMapBridge.onStationSelected(id);
                    }
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
fun GasStationCard(
    station: GasStation,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    if (!station.nickname.isNullOrBlank()) {
                        Text(
                            text = "\"${station.nickname}\"",
                            color = OrangeNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    color = OrangeNeon.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = station.brand,
                        color = OrangeNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (!station.address.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Text(
                        text = station.address,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            if (station.fuelTypes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    station.fuelTypes.take(4).forEach { fuel ->
                        Surface(
                            color = Color.DarkGray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = fuel,
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editar", color = Color.LightGray, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir", color = RedAlert, fontSize = 12.sp)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Posto", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir o posto \"${station.name}\"?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
