package com.fernando.centraldomotorista.ui.screens.apps

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlatformScreen(
    platformId: String? = null,
    viewModel: PlatformsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isEditing = !platformId.isNullOrBlank()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pixTypeExpanded by remember { mutableStateOf(false) }

    // Carregamento inicial da plataforma
    LaunchedEffect(platformId) {
        if (!platformId.isNullOrBlank()) {
            viewModel.loadPlatformForEditing(platformId)
        } else {
            viewModel.openAddDialog()
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

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditing) "EDITAR PLATAFORMA" else "NOVA PLATAFORMA",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isEditing && uiState.name.isNotBlank()) {
                            Text(
                                text = uiState.name,
                                fontSize = 12.sp,
                                color = OrangeNeon,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Excluir plataforma",
                                tint = RedAlert
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.savePlatform(onSuccess = onNavigateBack)
                        },
                        enabled = !uiState.isSaving && uiState.name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(50.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditing) "SALVAR ALTERAÇÕES" else "VINCULAR PLATAFORMA",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
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
            // Container responsivo com centralização e limite de largura (ótimo para telas pequenas, médias e grandes)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 0. Card de Pré-visualização da Plataforma em Tempo Real
                    PlatformLivePreviewCard(
                        name = uiState.name,
                        segment = uiState.segment,
                        cycle = uiState.cycle,
                        paymentDay = uiState.paymentDay,
                        active = uiState.active
                    )

                    // 1. Card Identificação
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "NOME DA PLATAFORMA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.name,
                                onValueChange = { viewModel.onNameChanged(it) },
                                placeholder = { Text("Ex: iFood, Loggi, Mercado Envios, Rappi...") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = OrangeNeon)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 2. Card Segmento de Operação
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SEGMENTO DE OPERAÇÃO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isLogistica = uiState.segment == "logistica"
                                SelectableSegmentCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Logística",
                                    subtitle = "Cargas & pacotes",
                                    icon = Icons.Default.LocalShipping,
                                    isSelected = isLogistica,
                                    activeColor = BlueInfo,
                                    onClick = { viewModel.onSegmentChanged("logistica") }
                                )

                                val isDelivery = uiState.segment == "delivery"
                                SelectableSegmentCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Delivery",
                                    subtitle = "Refeições & express",
                                    icon = Icons.Default.TwoWheeler,
                                    isSelected = isDelivery,
                                    activeColor = GreenNeon,
                                    onClick = { viewModel.onSegmentChanged("delivery") }
                                )
                            }
                        }
                    }

                    // 3. Card Modelo de Pagamento
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "MODELO DE PAGAMENTO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val isProducao = uiState.paymentModel == "producao"
                                SelectableOptionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Produção",
                                    subtitle = "Por pacote ou entrega",
                                    isSelected = isProducao,
                                    onClick = { viewModel.onPaymentModelChanged("producao") }
                                )

                                val isDiaria = uiState.paymentModel == "diaria"
                                SelectableOptionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Diária",
                                    subtitle = "Valor fixo por dia",
                                    isSelected = isDiaria,
                                    onClick = { viewModel.onPaymentModelChanged("diaria") }
                                )
                            }
                        }
                    }

                    // 4. Card Ciclo de Pagamento
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CICLO DE REPASSE E FATURAMENTO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val cycleRows = listOf(
                                listOf("semanal" to "Semanal", "quinzenal" to "Quinzenal"),
                                listOf("mensal" to "Mensal", "misto" to "Variável")
                            )

                            cycleRows.forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { (key, label) ->
                                        val isSelected = uiState.cycle == key
                                        SelectableCycleCard(
                                            modifier = Modifier.weight(1f),
                                            label = label,
                                            isSelected = isSelected,
                                            onClick = { viewModel.onCycleChanged(key) }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Regras específicas do ciclo selecionado
                            when (uiState.cycle) {
                                "semanal" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Dia de fechamento semanal",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            WEEK_DAYS.forEach { day ->
                                                val isDaySelected = uiState.paymentDay.equals(day, ignoreCase = true)
                                                Surface(
                                                    onClick = { viewModel.onPaymentDayChanged(day) },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDaySelected) OrangeNeon else MaterialTheme.colorScheme.surface,
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isDaySelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = day,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 11.sp,
                                                            color = if (isDaySelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = uiState.fixedPayDelay.toString(),
                                            onValueChange = { str ->
                                                val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                viewModel.onFixedPayDelayChanged(num)
                                            },
                                            label = { Text("Prazo de pagamento (dias após fechamento)") },
                                            suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangeNeon,
                                                focusedLabelColor = OrangeNeon,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        Surface(
                                            color = OrangeNeon.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Se fechar toda ${uiState.paymentDay} com prazo de ${uiState.fixedPayDelay} dias, o repasse cai na ${uiState.paymentDay} seguinte da semana.",
                                                    fontSize = 11.sp,
                                                    color = OrangeNeon,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                "quinzenal", "mensal" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.fixedPayDelay.toString(),
                                            onValueChange = { str ->
                                                val num = str.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                viewModel.onFixedPayDelayChanged(num)
                                            },
                                            label = { Text("Prazo de pagamento (dias após fechamento)") },
                                            suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangeNeon,
                                                focusedLabelColor = OrangeNeon,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        val helpText = if (uiState.cycle == "quinzenal")
                                            "Fechamento automático no dia 15 e no último dia do mês. O pagamento é depositado ${uiState.fixedPayDelay} dias após cada fechamento."
                                        else
                                            "Fechamento automático no último dia do mês. O pagamento é depositado ${uiState.fixedPayDelay} dias após o fechamento."

                                        Surface(
                                            color = OrangeNeon.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = helpText,
                                                    fontSize = 11.sp,
                                                    color = OrangeNeon,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                "misto", "variavel" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Cortes & Prazos Personalizados",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            TextButton(onClick = { viewModel.addCycleEntry() }) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangeNeon)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Adicionar ciclo", color = OrangeNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        uiState.cycleEntries.forEachIndexed { index, entry ->
                                            CycleEntryCard(
                                                index = index,
                                                entry = entry,
                                                canRemove = uiState.cycleEntries.size > 1,
                                                onRemove = { viewModel.removeCycleEntry(index) },
                                                onUpdate = { cut, payDelay -> viewModel.updateCycleEntry(index, cut, payDelay) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Card Dados de Recebimento Bancário (Opcional)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "DADOS DE RECEBIMENTO (OPCIONAL)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.bankName,
                                onValueChange = { viewModel.onBankNameChanged(it) },
                                label = { Text("Instituição financeira") },
                                placeholder = { Text("Ex: Nubank, Itaú, Bradesco, Inter...") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OrangeNeon) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangeNeon,
                                    focusedLabelColor = OrangeNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = uiState.bankAgency,
                                    onValueChange = { viewModel.onBankAgencyChanged(it) },
                                    label = { Text("Agência") },
                                    placeholder = { Text("0001") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = uiState.bankAccount,
                                    onValueChange = { viewModel.onBankAccountChanged(it) },
                                    label = { Text("Conta") },
                                    placeholder = { Text("123456-7") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // 6. Card Chave PIX (Opcional)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CHAVE PIX PARA REPASSES (OPCIONAL)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = pixTypeExpanded,
                                    onExpandedChange = { pixTypeExpanded = !pixTypeExpanded },
                                    modifier = Modifier.width(125.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.pixKeyType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pixTypeExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangeNeon,
                                            focusedLabelColor = OrangeNeon,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = pixTypeExpanded,
                                        onDismissRequest = { pixTypeExpanded = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        PIX_KEY_TYPES.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type, color = MaterialTheme.colorScheme.onSurface) },
                                                onClick = {
                                                    viewModel.onPixKeyTypeChanged(type)
                                                    pixTypeExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = uiState.pixKey,
                                    onValueChange = { viewModel.onPixKeyChanged(it) },
                                    label = { Text("Chave PIX") },
                                    placeholder = { Text("Digite a chave") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    // 7. Card Status da Plataforma (Ativa / Inativa)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Status da Plataforma",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        color = if (uiState.active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (uiState.active) "ATIVA" else "INATIVA",
                                            color = if (uiState.active) GreenNeon else RedAlert,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Permitir selecionar esta plataforma em novos lançamentos de rota",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = uiState.active,
                                onCheckedChange = { viewModel.onActiveChanged(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = GreenNeon,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = RedAlert.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Diálogo de confirmação de exclusão
    if (showDeleteConfirmDialog && uiState.editingPlatformId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    "Excluir Plataforma?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Tem certeza que deseja excluir '${uiState.name}'? Histórico e corridas vinculadas permanecerão preservados.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePlatform(uiState.editingPlatformId!!, onSuccess = onNavigateBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Excluir Definitivamente", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ---------------------- Sub-componentes Especializados ----------------------

@Composable
private fun PlatformLivePreviewCard(
    name: String,
    segment: String,
    cycle: String,
    paymentDay: String?,
    active: Boolean
) {
    val isLogistica = segment.equals("logistica", ignoreCase = true)
    val segmentBadgeColor = if (isLogistica) BlueInfo else GreenNeon
    val segmentLabel = if (isLogistica) "Logística" else "Delivery"
    val cycleLabel = BillingCycleCalculator.getCycleDisplayLabel(cycle, paymentDay)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) OrangeNeon.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (active) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        if (active) OrangeNeon.copy(alpha = 0.5f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLogistica) Icons.Default.LocalShipping else Icons.Default.TwoWheeler,
                    contentDescription = null,
                    tint = if (active) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name.ifBlank { "Nome da Plataforma" },
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = segmentBadgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, segmentBadgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = segmentLabel,
                            color = segmentBadgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = cycleLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = if (active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (active) "ATIVA" else "INATIVA",
                            color = if (active) GreenNeon else RedAlert,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableSegmentCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) activeColor else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectableOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) OrangeNeon else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = OrangeNeon,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectableCycleCard(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) OrangeNeon else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun CycleEntryCard(
    index: Int,
    entry: CycleEntry,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onUpdate: (Int?, Int?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CICLO ${index + 1}",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = OrangeNeon,
                    letterSpacing = 1.sp
                )
                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remover ciclo",
                            tint = RedAlert,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = entry.cut.toString(),
                    onValueChange = { str ->
                        val v = str.filter { it.isDigit() }.toIntOrNull()
                        onUpdate(v, null)
                    },
                    label = { Text("Fechamento (dia)") },
                    placeholder = { Text("1-28") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = entry.payDelay.toString(),
                    onValueChange = { str ->
                        val v = str.filter { it.isDigit() }.toIntOrNull()
                        onUpdate(null, v)
                    },
                    label = { Text("Pagamento (dias)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            val payDayEst = entry.cut + entry.payDelay
            val feedback = if (payDayEst > 28)
                "Fecha dia ${entry.cut} → paga dia ${payDayEst - 28} do mês seguinte"
            else
                "Fecha dia ${entry.cut} → paga dia $payDayEst"

            Surface(
                color = OrangeNeon.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = feedback,
                    fontSize = 11.sp,
                    color = OrangeNeon,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
