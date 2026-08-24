package com.fernando.centraldomotorista.ui.screens.gasstations

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.model.GasStation
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
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "Selecionar no Mapa (em breve via Overpass API)", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Selecionar no Mapa",
                                        tint = OrangeNeon
                                    )
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

                        // Bandeira (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = brandMenuExpanded,
                            onExpandedChange = { brandMenuExpanded = !brandMenuExpanded }
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
                                AVAILABLE_BRANDS.forEach { brandOption ->
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
