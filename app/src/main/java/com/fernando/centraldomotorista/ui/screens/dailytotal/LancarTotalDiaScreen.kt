package com.fernando.centraldomotorista.ui.screens.dailytotal

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LancarTotalDiaScreen(
    itemId: String? = null,
    viewModel: LancarTotalDiaViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(itemId) {
        viewModel.initOrLoad(itemId)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    val isEditing = !uiState.editingId.isNullOrBlank()
    val screenTitle = if (isEditing) "EDITAR TOTAL DO DIA" else "LANÇAR TOTAL DO DIA"
    val buttonText = if (isEditing) "SALVAR ALTERAÇÕES ✓" else "CONFIRMAR LANÇAMENTO ✓"

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeNeon)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card de Pergunta: Subtrair rotas do dia
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "O valor a ser informado deverá ser reduzido das rotas realizadas no dia de hoje?",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.onSubtractRoutesChanged(true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.subtractRoutes) OrangeNeon else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (uiState.subtractRoutes) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("SIM", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.onSubtractRoutesChanged(false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!uiState.subtractRoutes) OrangeNeon else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (!uiState.subtractRoutes) Color.Black else MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("NÃO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Data e Hora
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Data
                    OutlinedTextField(
                        value = uiState.date.format(dateFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Data") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = OrangeNeon) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val d = uiState.date
                                DatePickerDialog(context, { _, y, m, day ->
                                    viewModel.onDateChanged(LocalDate.of(y, m + 1, day))
                                }, d.year, d.monthValue - 1, d.dayOfMonth).show()
                            },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Hora
                    OutlinedTextField(
                        value = uiState.time.format(timeFormatter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = OrangeNeon) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val t = uiState.time
                                TimePickerDialog(context, { _, h, min ->
                                    viewModel.onTimeChanged(LocalTime.of(h, min))
                                }, t.hour, t.minute, true).show()
                            },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Plataforma
                var isPlatformMenuOpen by remember { mutableStateOf(false) }
                val selectedPlat = uiState.platforms.firstOrNull { it.id == uiState.selectedPlatformId }

                ExposedDropdownMenuBox(
                    expanded = isPlatformMenuOpen,
                    onExpandedChange = { isPlatformMenuOpen = !isPlatformMenuOpen }
                ) {
                    OutlinedTextField(
                        value = selectedPlat?.name ?: "Selecione a Plataforma",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Plataforma") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPlatformMenuOpen) },
                        colors = OutlinedTextFieldDefaults.colors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isPlatformMenuOpen,
                        onDismissRequest = { isPlatformMenuOpen = false }
                    ) {
                        uiState.platforms.forEach { platform ->
                            DropdownMenuItem(
                                text = { Text(platform.name) },
                                onClick = {
                                    viewModel.onPlatformSelected(platform.id)
                                    isPlatformMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Distância (KM) e Valor (R$)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.distanceKmText,
                        onValueChange = { viewModel.onDistanceKmChanged(it) },
                        label = { Text("Distância (km)") },
                        placeholder = { Text("Ex: 10,5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = uiState.amountText,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        label = { Text("Valor (R$) *") },
                        placeholder = { Text("Ex: 150,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Tipo de produto
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TIPO DE PRODUTO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProductTypeButton(
                            icon = Icons.Default.Restaurant,
                            label = "Alimento",
                            selected = uiState.productType == "alimento",
                            onClick = { viewModel.onProductTypeSelected("alimento") },
                            modifier = Modifier.weight(1f)
                        )
                        ProductTypeButton(
                            icon = Icons.Default.Inventory2,
                            label = "Pacotes",
                            selected = uiState.productType == "pacote",
                            onClick = { viewModel.onProductTypeSelected("pacote") },
                            modifier = Modifier.weight(1f)
                        )
                        ProductTypeButton(
                            icon = Icons.Default.Description,
                            label = "Documentos",
                            selected = uiState.productType == "documento",
                            onClick = { viewModel.onProductTypeSelected("documento") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Observações
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Observações (opcional)") },
                    placeholder = { Text("Ex: Trânsito intenso, chuva...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botão de confirmação
                Button(
                    onClick = {
                        viewModel.save(onSuccess = onNavigateBack)
                    },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeNeon,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductTypeButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) OrangeNeon else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface,
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}
