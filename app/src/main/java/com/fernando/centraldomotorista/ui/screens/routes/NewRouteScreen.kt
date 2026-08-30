package com.fernando.centraldomotorista.ui.screens.routes

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import com.fernando.centraldomotorista.ui.utils.KmVisualTransformation
import com.fernando.centraldomotorista.ui.utils.SuffixVisualTransformation
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRouteScreen(
    viewModel: NewRouteViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onRouteSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var platformMenuExpanded by remember { mutableStateOf(false) }
    var productTypeMenuExpanded by remember { mutableStateOf(false) }
    var showVolumososModal by remember { mutableStateOf(false) }
    var isBreakMinutesFocused by remember { mutableStateOf(false) }

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
            onRouteSaved()
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
                        text = "LANÇAR GANHOS POR ROTA",
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
            // Card Principal do Formulário
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Seção Plataforma com Ícone
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "PLATAFORMA",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        val selectedPlatform = uiState.platforms.firstOrNull { it.id == uiState.selectedPlatformId }
                        val platformLabel = selectedPlatform?.name
                            ?: if (uiState.platforms.isEmpty()) "Nenhuma plataforma ativa" else "Selecione a Plataforma"

                        ExposedDropdownMenuBox(
                            expanded = platformMenuExpanded,
                            onExpandedChange = { if (uiState.platforms.isNotEmpty()) platformMenuExpanded = !platformMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = platformLabel,
                                onValueChange = {},
                                readOnly = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeNeon)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = platformMenuExpanded,
                                onDismissRequest = { platformMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                uiState.platforms.forEach { platform ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(platform.name, color = Color.White, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${platform.segment.replaceFirstChar { it.uppercase() }} • Ciclo: ${platform.cycle}",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.onPlatformSelected(platform.id)
                                            platformMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Origem e Destino (Lado a Lado)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.origin,
                                onValueChange = { viewModel.onOriginChanged(it) },
                                label = { Text("Origem") },
                                placeholder = { Text("Ex: Galpão Cajamar") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.TripOrigin, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = uiState.destination,
                                onValueChange = { viewModel.onDestinationChanged(it) },
                                label = { Text("Destino") },
                                placeholder = { Text("Ex: Centro / Bairros") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 3. Tipo de Produto (Largura Total)
                        val selectedProductTypeOption = AVAILABLE_PRODUCT_TYPES.firstOrNull { it.code == uiState.selectedProductTypeCode }
                            ?: AVAILABLE_PRODUCT_TYPES.first()

                        ExposedDropdownMenuBox(
                            expanded = productTypeMenuExpanded,
                            onExpandedChange = { productTypeMenuExpanded = !productTypeMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedProductTypeOption.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo de Produto") },
                                leadingIcon = {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = OrangeNeon)
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productTypeMenuExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = productTypeMenuExpanded,
                                onDismissRequest = { productTypeMenuExpanded = false },
                                modifier = Modifier.background(SurfaceDark)
                            ) {
                                AVAILABLE_PRODUCT_TYPES.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, color = Color.White) },
                                        onClick = {
                                            viewModel.onProductTypeSelected(option.code)
                                            productTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                        // 4. SEÇÃO PACOTINHOS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "PACOTINHOS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.smallPackagesCountText,
                                        onValueChange = { viewModel.onSmallPackagesCountChanged(it) },
                                        label = { Text("Qtd. Pacotinhos") },
                                        placeholder = { Text("0") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangeNeon,
                                            focusedLabelColor = OrangeNeon,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = uiState.smallPackagesUnitPriceText,
                                        onValueChange = { viewModel.onSmallPackagesUnitPriceChanged(it) },
                                        label = { Text("Valor Unitário") },
                                        placeholder = { Text("0,00") },
                                        singleLine = true,
                                        visualTransformation = CurrencyVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangeNeon,
                                            focusedLabelColor = OrangeNeon,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Valor Total Pacotinhos:",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currencyFormatter.format(uiState.smallPackagesTotal),
                                        color = GreenNeon,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // 5. SEÇÃO VOLUMOSOS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AllInbox, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(20.dp))
                                    Text(
                                        text = "VOLUMOSOS",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }

                                OutlinedTextField(
                                    value = uiState.largePackagesCountText,
                                    onValueChange = { viewModel.onLargePackagesCountChanged(it) },
                                    label = { Text("Quantidade de Volumosos") },
                                    placeholder = { Text("0") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = Color.DarkGray,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Radio Buttons: Valor Único vs Valor Individual
                                Text(
                                    text = "FORMA DE PRECIFICAÇÃO",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.onLargePackagePricingModeChanged(false) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!uiState.isLargePackageIndividualValue) OrangeNeon else SurfaceDark,
                                            contentColor = if (!uiState.isLargePackageIndividualValue) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("Valor Único", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.onLargePackagePricingModeChanged(true)
                                            val count = uiState.largePackagesCountText.toIntOrNull() ?: 0
                                            if (count > 0) {
                                                showVolumososModal = true
                                            } else {
                                                Toast.makeText(context, "Informe a quantidade de volumosos primeiro", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (uiState.isLargePackageIndividualValue) OrangeNeon else SurfaceDark,
                                            contentColor = if (uiState.isLargePackageIndividualValue) Color.Black else Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp)
                                    ) {
                                        Text("Valor Individual", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                if (!uiState.isLargePackageIndividualValue) {
                                    // Campo Valor Único
                                    OutlinedTextField(
                                        value = uiState.largePackageSingleUnitPriceText,
                                        onValueChange = { viewModel.onLargePackageSingleUnitPriceChanged(it) },
                                        label = { Text("Valor Unitário Volumoso") },
                                        placeholder = { Text("0,00") },
                                        singleLine = true,
                                        visualTransformation = CurrencyVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangeNeon,
                                            focusedLabelColor = OrangeNeon,
                                            unfocusedBorderColor = Color.DarkGray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    // Botão / Resumo de Valores Individuais
                                    val count = uiState.largePackagesCountText.toIntOrNull() ?: 0
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (count > 0) {
                                                    showVolumososModal = true
                                                } else {
                                                    Toast.makeText(context, "Informe a quantidade de volumosos primeiro", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Configuração Individual",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "${uiState.largePackagesIndividualPrices.size} itens configurados",
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                            }
                                            Text(
                                                text = "Editar Valores",
                                                color = OrangeNeon,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Valor Total Volumosos:",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currencyFormatter.format(uiState.largePackagesTotal),
                                        color = GreenNeon,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // 6. Gorjeta e Bônus (Opcionais)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.tipText,
                                onValueChange = { viewModel.onTipChanged(it) },
                                label = { Text("Gorjeta (Opcional)") },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                visualTransformation = CurrencyVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = uiState.bonusText,
                                onValueChange = { viewModel.onBonusChanged(it) },
                                label = { Text("Bônus (Opcional)") },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                visualTransformation = CurrencyVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // 7. CARD CONSOLIDADO DE TOTAIS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkAlt),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "QUANTIDADE TOTAL DE PACOTES",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = OrangeNeon.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.totalPackagesCount} pacotes",
                                            color = OrangeNeon,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.DarkGray)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "VALOR TOTAL DA ROTA",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = currencyFormatter.format(uiState.totalAmount),
                                        color = GreenNeon,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.6f), modifier = Modifier.padding(vertical = 4.dp))

                        // 8. Horário de Início e Fim + Minutos de Pausa + Odômetro + Distância Total
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = OrangeNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "HORÁRIOS & TEMPO DE PAUSA",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.startTime.format(timeFormatter),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hora Início") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showTimePicker(context, uiState.startTime) { newTime ->
                                            viewModel.onStartTimeChanged(newTime)
                                        }
                                    }) {
                                        Icon(Icons.Default.AccessTime, contentDescription = "Hora Início", tint = OrangeNeon)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showTimePicker(context, uiState.startTime) { newTime ->
                                            viewModel.onStartTimeChanged(newTime)
                                        }
                                    }
                            )

                            OutlinedTextField(
                                value = uiState.endTime.format(timeFormatter),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Hora Fim") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        showTimePicker(context, uiState.endTime) { newTime ->
                                            viewModel.onEndTimeChanged(newTime)
                                        }
                                    }) {
                                        Icon(Icons.Default.AccessTime, contentDescription = "Hora Fim", tint = OrangeNeon)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showTimePicker(context, uiState.endTime) { newTime ->
                                            viewModel.onEndTimeChanged(newTime)
                                        }
                                    }
                            )
                        }

                        OutlinedTextField(
                            value = uiState.breakMinutesText,
                            onValueChange = { viewModel.onBreakMinutesChanged(it) },
                            label = { Text("Minutos de Pausa") },
                            placeholder = {
                                if (!isBreakMinutesFocused) {
                                    Text("0 Minutos", color = Color.Gray)
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (!isBreakMinutesFocused && uiState.breakMinutesText.isNotEmpty()) {
                                SuffixVisualTransformation(" Minutos")
                            } else {
                                VisualTransformation.None
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isBreakMinutesFocused = it.isFocused }
                        )

                        // Tempo Trabalhado (Cálculo Automático: Hora Final - Hora Inicial - Minutos de Pausa)
                        OutlinedTextField(
                            value = uiState.workedTimeFormatted,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tempo Trabalhado") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                disabledTextColor = Color.White,
                                disabledBorderColor = Color.DarkGray,
                                disabledLabelColor = Color.LightGray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 9. KM Inicial e KM Final do Odômetro (Opcional)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.startKmText,
                                onValueChange = { viewModel.onStartKmChanged(it) },
                                label = { Text("KM Inicial (Opcional)") },
                                placeholder = { Text("0") },
                                singleLine = true,
                                visualTransformation = KmVisualTransformation(" KM"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = uiState.endKmText,
                                onValueChange = { viewModel.onEndKmChanged(it) },
                                label = { Text("KM Final (Opcional)") },
                                placeholder = { Text("0") },
                                singleLine = true,
                                visualTransformation = KmVisualTransformation(" KM"),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = Color.DarkGray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Distância Total (KM) - ÚLTIMO CAMPO DO FORMULÁRIO (Check / Done para fechar teclado)
                        OutlinedTextField(
                            value = uiState.distanceKmText,
                            onValueChange = { viewModel.onDistanceKmChanged(it) },
                            label = { Text("Distância Total (KM)") },
                            placeholder = { Text("0,0") },
                            singleLine = true,
                            visualTransformation = KmVisualTransformation(" KM"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 10. Botão "Salvar Rota"
                        Button(
                            onClick = {
                                viewModel.saveRoute {
                                    // Callback executado no sucesso
                                }
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
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Salvar Rota",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal de Valores Individuais de Volumosos
    if (showVolumososModal) {
        val count = uiState.largePackagesCountText.toIntOrNull() ?: 1
        VolumososModal(
            quantity = count,
            initialPrices = uiState.largePackagesIndividualPrices,
            onConfirm = { prices ->
                viewModel.onLargePackagesIndividualPricesConfirmed(prices)
                showVolumososModal = false
            },
            onDismiss = { showVolumososModal = false }
        )
    }
}

fun showTimePicker(
    context: android.content.Context,
    currentTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            onTimeSelected(LocalTime.of(hourOfDay, minute))
        },
        currentTime.hour,
        currentTime.minute,
        true
    ).show()
}
