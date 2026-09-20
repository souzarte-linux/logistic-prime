package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.data.billing.BillingCycleCalculator
import com.fernando.centraldomotorista.data.model.CycleEntry
import com.fernando.centraldomotorista.data.model.Platform
import com.fernando.centraldomotorista.data.model.PlatformRules
import com.fernando.centraldomotorista.ui.common.cards.CollapsibleSectionCard
import com.fernando.centraldomotorista.ui.screens.apps.PIX_KEY_TYPES
import com.fernando.centraldomotorista.ui.screens.apps.WEEK_DAYS
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeIcon
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tela Replicada "Nova Plataforma" e "Editar Plataforma" no contexto do Entregador Parceiro.
 * Atende com precisão aos requisitos:
 * 1. Cada sessão da tela é colapsável/expansível (contrair e expandir com animação suave).
 * 2. Aloca dentro de si as 5 sessões:
 *    - Parâmetros Operacionais (Rota preferida, taxa por pacote, bônus padrão)
 *    - Tipo de Entrega / Veículo (Moto, Carro, Utilitário, Bicicleta, A pé)
 *    - Classificação do Entregador (1 a 5 estrelas com indicador textual)
 *    - Ciclo de Pagamento (Semanal, Quinzenal, Mensal, Variável)
 *    - Cortes & Prazos Personalizados (Ciclos variáveis com início, fim, switch de inclusão, dias e data calculada)
 * 3. Utiliza o mesmo banco de dados (`platforms` no Supabase).
 * 4. Layout 100% responsivo para todos os campos em qualquer tamanho de tela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerPlatformEditScreen(
    partnerId: String? = null,
    platformId: String? = null,
    deliveryPartnersViewModel: DeliveryPartnersViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val partnersUiState by deliveryPartnersViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isEditing = !platformId.isNullOrBlank()

    LaunchedEffect(partnerId) {
        if (!partnerId.isNullOrBlank()) {
            deliveryPartnersViewModel.loadPlatformsForPartner(partnerId)
        }
    }

    // Busca a plataforma se for edição
    val existingPlatform = remember(platformId, partnersUiState.platforms) {
        if (!platformId.isNullOrBlank()) {
            partnersUiState.platforms.firstOrNull { it.id == platformId }
        } else null
    }

    // Estado do formulário da plataforma
    var name by remember { mutableStateOf(existingPlatform?.name ?: "") }
    var segment by remember { mutableStateOf(existingPlatform?.segment ?: "logistica") }
    var paymentModel by remember { mutableStateOf(existingPlatform?.paymentModel ?: "producao") }
    var cycle by remember { mutableStateOf(existingPlatform?.cycle ?: "semanal") }
    var paymentDay by remember { mutableStateOf(existingPlatform?.paymentDay ?: "QUA") }
    var fixedPayDelayText by remember {
        mutableStateOf(existingPlatform?.rules?.fixedPayDelay?.toString() ?: "7")
    }
    var bankName by remember { mutableStateOf(existingPlatform?.bankName ?: "") }
    var bankAgency by remember { mutableStateOf(existingPlatform?.bankAgency ?: "") }
    var bankAccount by remember { mutableStateOf(existingPlatform?.bankAccount ?: "") }
    var pixKeyType by remember { mutableStateOf(existingPlatform?.pixKeyType ?: "CPF") }
    var pixKey by remember { mutableStateOf(existingPlatform?.pixKey ?: "") }
    var active by remember { mutableStateOf(existingPlatform?.active ?: true) }

    // Parâmetros operacionais e regras de entrega do parceiro
    var selectedRouteId by remember { mutableStateOf(partnersUiState.formData.preferredRouteId) }
    var packageRateText by remember { mutableStateOf(partnersUiState.formData.packageRateText) }
    var defaultBonusText by remember { mutableStateOf(partnersUiState.formData.defaultBonusText) }
    var deliveryType by remember { mutableStateOf(partnersUiState.formData.deliveryType) }
    var rating by remember { mutableIntStateOf(partnersUiState.formData.rating) }
    var variableCycles by remember {
        mutableStateOf(partnersUiState.formData.variableCycles.ifEmpty { listOf(VariableCycleFormEntry()) })
    }

    // Estados de colapso/expansão de cada sessão
    var expandIdentification by remember { mutableStateOf(true) }
    var expandSegment by remember { mutableStateOf(true) }
    var expandPaymentModel by remember { mutableStateOf(true) }
    var expandOperationalParams by remember { mutableStateOf(true) }
    var expandVehicleType by remember { mutableStateOf(true) }
    var expandRating by remember { mutableStateOf(true) }
    var expandPaymentCycle by remember { mutableStateOf(true) }
    var expandVariableCycles by remember { mutableStateOf(true) }
    var expandBanking by remember { mutableStateOf(false) }
    var expandPix by remember { mutableStateOf(false) }
    var expandStatus by remember { mutableStateOf(true) }

    // Controles de diálogo
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showIncludeEndDateDialog by remember { mutableStateOf(false) }
    var showTooltipInfoDialog by remember { mutableStateOf(false) }
    var pendingEndDate by remember { mutableStateOf<LocalDate?>(null) }
    var pendingCycleIndex by remember { mutableIntStateOf(0) }
    var pixTypeExpanded by remember { mutableStateOf(false) }
    var routeDropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val selectedRouteName = remember(selectedRouteId, partnersUiState.routes) {
        partnersUiState.routes.firstOrNull { it.id == selectedRouteId }?.name ?: "Nenhuma (Sem preferência)"
    }

    BackHandler { onNavigateBack() }

    // Diálogo de confirmação de inclusão da data final no ciclo variável
    if (showIncludeEndDateDialog && pendingEndDate != null) {
        val formattedDate = pendingEndDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
        AlertDialog(
            onDismissRequest = { showIncludeEndDateDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Valores da Data Final",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Deseja incluir os valores e corridas da data final ($formattedDate) no cálculo deste ciclo?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val list = variableCycles.toMutableList()
                        if (pendingCycleIndex in list.indices) {
                            list[pendingCycleIndex] = list[pendingCycleIndex].copy(includeEndDate = true)
                            variableCycles = list
                        }
                        showIncludeEndDateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenNeon, contentColor = Color.Black)
                ) {
                    Text("Sim, incluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val list = variableCycles.toMutableList()
                        if (pendingCycleIndex in list.indices) {
                            list[pendingCycleIndex] = list[pendingCycleIndex].copy(includeEndDate = false)
                            variableCycles = list
                        }
                        showIncludeEndDateDialog = false
                    }
                ) {
                    Text("Não incluir")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Tooltip informativo da data final
    if (showTooltipInfoDialog) {
        AlertDialog(
            onDismissRequest = { showTooltipInfoDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(28.dp))
            },
            title = {
                Text(
                    text = "Inclusão da Data Final",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Selecionado 'Sim': Todos os valores e corridas realizados no dia final do ciclo serão computados e somados no fechamento.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Selecionado 'Não': O ciclo encerra as apurações antes da data final, não computando os lançamentos do último dia.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTooltipInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Entendi", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Diálogo de confirmação de exclusão
    if (showDeleteConfirmDialog && existingPlatform != null) {
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
                    "Tem certeza que deseja excluir '${existingPlatform.name}'? O histórico operacional permanecerá preservado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        deliveryPartnersViewModel.deletePlatformFromPartner(partnerId = partnerId, platformId = existingPlatform.id) {
                            Toast.makeText(context, "Plataforma excluída com sucesso!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
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
                        if (name.isNotBlank()) {
                            Text(
                                text = name,
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            val delayNum = fixedPayDelayText.filter { it.isDigit() }.toIntOrNull() ?: 7
                            val updatedRules = PlatformRules(
                                fixedPayDelay = delayNum,
                                cycleEntries = listOf(CycleEntry(1, delayNum))
                            )
                            val platformToSave = Platform(
                                id = existingPlatform?.id ?: "",
                                userId = existingPlatform?.userId ?: "",
                                name = name.trim(),
                                cycle = cycle,
                                paymentDay = if (cycle == "semanal") paymentDay else null,
                                active = active,
                                segment = segment,
                                paymentModel = paymentModel,
                                rules = updatedRules,
                                bankName = bankName.ifBlank { null },
                                bankAgency = bankAgency.ifBlank { null },
                                bankAccount = bankAccount.ifBlank { null },
                                pixKeyType = pixKeyType,
                                pixKey = pixKey.ifBlank { null }
                            )

                            // Salva a plataforma no banco e sincroniza com o formulário do entregador
                            deliveryPartnersViewModel.savePlatformForPartner(
                                partnerId = partnerId,
                                platform = platformToSave,
                                preferredRouteId = selectedRouteId,
                                packageRateText = packageRateText,
                                defaultBonusText = defaultBonusText,
                                deliveryType = deliveryType,
                                rating = rating,
                                variableCycles = variableCycles,
                                onComplete = {
                                    isSaving = false
                                    Toast.makeText(context, "Plataforma salva com sucesso!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }
                            )
                        },
                        enabled = !isSaving && name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(50.dp)
                    ) {
                        if (isSaving) {
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 0. Card de Pré-visualização em Tempo Real (fiel ao Gestor de Plataformas)
                PlatformPartnerLivePreviewCard(
                    name = name,
                    segment = segment,
                    cycle = cycle,
                    paymentDay = paymentDay,
                    deliveryType = deliveryType,
                    rating = rating,
                    active = active
                )

                // 1. SESSÃO: IDENTIFICAÇÃO DA PLATAFORMA (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Identificação da Plataforma",
                    subtitle = "Nome e detalhes da empresa operadora",
                    icon = Icons.Default.Storefront,
                    initiallyExpanded = true,
                    isExpandedControlled = expandIdentification,
                    onExpandedChange = { expandIdentification = it }
                ) {
                    Text(
                        text = "NOME DA PLATAFORMA *",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Ex: iFood, Loggi, Mercado Envios, Shopee...") },
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

                // 2. SESSÃO: SEGMENTO DE OPERAÇÃO (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Segmento de Operação",
                    subtitle = "Modalidade de atuação da plataforma",
                    icon = Icons.Default.Category,
                    initiallyExpanded = true,
                    isExpandedControlled = expandSegment,
                    onExpandedChange = { expandSegment = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isLogistica = segment == "logistica"
                        SelectableSegmentCardItem(
                            modifier = Modifier.weight(1f),
                            title = "Logística",
                            subtitle = "Cargas & pacotes",
                            icon = Icons.Default.LocalShipping,
                            isSelected = isLogistica,
                            activeColor = BlueInfo,
                            onClick = { segment = "logistica" }
                        )

                        val isDelivery = segment == "delivery"
                        SelectableSegmentCardItem(
                            modifier = Modifier.weight(1f),
                            title = "Delivery",
                            subtitle = "Refeições & express",
                            icon = Icons.Default.TwoWheeler,
                            isSelected = isDelivery,
                            activeColor = GreenNeon,
                            onClick = { segment = "delivery" }
                        )
                    }
                }

                // 3. SESSÃO: MODELO DE PAGAMENTO (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Modelo de Pagamento",
                    subtitle = "Formato de cálculo do repasse da plataforma",
                    icon = Icons.Default.Payments,
                    initiallyExpanded = true,
                    isExpandedControlled = expandPaymentModel,
                    onExpandedChange = { expandPaymentModel = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isProducao = paymentModel == "producao"
                        SelectableOptionCardItem(
                            modifier = Modifier.weight(1f),
                            title = "Produção",
                            subtitle = "Por pacote ou entrega",
                            isSelected = isProducao,
                            onClick = { paymentModel = "producao" }
                        )

                        val isDiaria = paymentModel == "diaria"
                        SelectableOptionCardItem(
                            modifier = Modifier.weight(1f),
                            title = "Diária",
                            subtitle = "Valor fixo por dia",
                            isSelected = isDiaria,
                            onClick = { paymentModel = "diaria" }
                        )
                    }
                }

                // 4. SESSÃO ALOCADA: PARÂMETROS OPERACIONAIS (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Parâmetros Operacionais",
                    subtitle = "Rota de preferência, taxa por pacote e bônus",
                    icon = Icons.Default.AltRoute,
                    initiallyExpanded = true,
                    isExpandedControlled = expandOperationalParams,
                    onExpandedChange = { expandOperationalParams = it }
                ) {
                    // Rota Preferida (Dropdown)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { routeDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AltRoute,
                                        contentDescription = null,
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Rota Preferida",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = selectedRouteName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OrangeNeon)
                            }
                        }

                        DropdownMenu(
                            expanded = routeDropdownExpanded,
                            onDismissRequest = { routeDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Nenhuma (Sem preferência)", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    selectedRouteId = null
                                    routeDropdownExpanded = false
                                }
                            )
                            partnersUiState.routes.forEach { route ->
                                DropdownMenuItem(
                                    text = { Text(route.name, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        selectedRouteId = route.id
                                        routeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Taxa e Bônus Padrão
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = packageRateText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == ',' || it == '.' }
                                packageRateText = clean
                            },
                            label = { Text("Taxa / Pacote", fontSize = 11.sp) },
                            prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = defaultBonusText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == ',' || it == '.' }
                                defaultBonusText = clean
                            },
                            label = { Text("Bônus Padrão", fontSize = 11.sp) },
                            prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                            placeholder = { Text("0,00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OrangeNeon,
                                focusedLabelColor = OrangeNeon,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 5. SESSÃO ALOCADA: TIPO DE ENTREGA / VEÍCULO (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Tipo de Entrega / Veículo",
                    subtitle = "Modal de transporte e locomoção nas entregas",
                    icon = Icons.Default.DirectionsCar,
                    initiallyExpanded = true,
                    isExpandedControlled = expandVehicleType,
                    onExpandedChange = { expandVehicleType = it }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Linha 1: Moto, Carro, Utilitário
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectableDeliveryTypeCardItem(
                                modifier = Modifier.weight(1f),
                                label = "Moto",
                                icon = Icons.Default.TwoWheeler,
                                isSelected = deliveryType == "moto",
                                onClick = { deliveryType = "moto" }
                            )
                            SelectableDeliveryTypeCardItem(
                                modifier = Modifier.weight(1f),
                                label = "Carro",
                                icon = Icons.Default.DirectionsCar,
                                isSelected = deliveryType == "carro",
                                onClick = { deliveryType = "carro" }
                            )
                            SelectableDeliveryTypeCardItem(
                                modifier = Modifier.weight(1f),
                                label = "Utilitário",
                                icon = Icons.Default.LocalShipping,
                                isSelected = deliveryType == "utilitario",
                                onClick = { deliveryType = "utilitario" }
                            )
                        }

                        // Linha 2: Bicicleta, A pé
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SelectableDeliveryTypeCardItem(
                                modifier = Modifier.weight(1f),
                                label = "Bicicleta",
                                icon = Icons.Default.DirectionsBike,
                                isSelected = deliveryType == "bike",
                                onClick = { deliveryType = "bike" }
                            )
                            SelectableDeliveryTypeCardItem(
                                modifier = Modifier.weight(1f),
                                label = "A pé (Express)",
                                icon = Icons.Default.DirectionsWalk,
                                isSelected = deliveryType == "a_pe",
                                onClick = { deliveryType = "a_pe" }
                            )
                        }
                    }
                }

                // 6. SESSÃO ALOCADA: CLASSIFICAÇÃO DO ENTREGADOR (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Classificação do Entregador",
                    subtitle = "Nível de experiência e pontualidade (1 a 5 estrelas)",
                    icon = Icons.Default.Star,
                    initiallyExpanded = true,
                    isExpandedControlled = expandRating,
                    onExpandedChange = { expandRating = it }
                ) {
                    val ratingLabel = when (rating) {
                        1 -> "Iniciante (1/5)"
                        2 -> "Básico (2/5)"
                        3 -> "Intermediário (3/5)"
                        4 -> "Experiente (4/5)"
                        5 -> "Excelente (5/5)"
                        else -> "$rating/5"
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row {
                                for (i in 1..5) {
                                    IconButton(
                                        onClick = { rating = i },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "$i Estrelas",
                                            tint = if (i <= rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color(0xFFFFB300).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ratingLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 7. SESSÃO ALOCADA: CICLO DE PAGAMENTO (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Ciclo de Pagamento",
                    subtitle = "Frequência e prazo de repasse da plataforma",
                    icon = Icons.Default.CalendarToday,
                    initiallyExpanded = true,
                    isExpandedControlled = expandPaymentCycle,
                    onExpandedChange = { expandPaymentCycle = it }
                ) {
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
                                val isSelected = cycle == key
                                SelectableCycleCardItem(
                                    modifier = Modifier.weight(1f),
                                    label = label,
                                    isSelected = isSelected,
                                    onClick = { cycle = key }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Configurações do ciclo selecionado
                    when (cycle) {
                        "semanal" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
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
                                        val isDaySelected = paymentDay.equals(day, ignoreCase = true)
                                        Surface(
                                            onClick = { paymentDay = day },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDaySelected) OrangeNeon else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
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
                                    value = fixedPayDelayText,
                                    onValueChange = { fixedPayDelayText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Prazo de pagamento (dias após fechamento)") },
                                    suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                    singleLine = true,
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
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Se fechar toda $paymentDay com prazo de $fixedPayDelayText dias, o repasse cai na $paymentDay seguinte da semana.",
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
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = fixedPayDelayText,
                                    onValueChange = { fixedPayDelayText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Prazo de pagamento (dias após fechamento)") },
                                    suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OrangeNeon,
                                        focusedLabelColor = OrangeNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                val helpText = if (cycle == "quinzenal")
                                    "Fechamento automático no dia 15 e no último dia do mês. O pagamento é depositado $fixedPayDelayText dias após cada fechamento."
                                else
                                    "Fechamento automático no último dia do mês. O pagamento é depositado $fixedPayDelayText dias após o fechamento."

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
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
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
                    }
                }

                // 8. SESSÃO ALOCADA: CORTES & PRAZOS PERSONALIZADOS (Expansível / Colapsável)
                // Exibida sempre que o ciclo for Variável / Misto
                if (cycle == "misto" || cycle == "variavel") {
                    CollapsibleSectionCard(
                        title = "Cortes & Prazos Personalizados",
                        subtitle = "Datas exatas de fechamento e dia do depósito",
                        icon = Icons.Default.Tune,
                        initiallyExpanded = true,
                        isExpandedControlled = expandVariableCycles,
                        onExpandedChange = { expandVariableCycles = it }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            variableCycles.forEachIndexed { index, cycleEntry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Cabeçalho do Ciclo
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = OrangeNeon.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "CICLO ${index + 1}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.sp,
                                                    color = OrangeNeon,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            if (variableCycles.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        val list = variableCycles.toMutableList()
                                                        list.removeAt(index)
                                                        variableCycles = list
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remover ciclo",
                                                        tint = RedAlert,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Campo 1 e Campo 2: Início Ciclo e Final Ciclo na mesma linha responsiva
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = cycleEntry.formattedStartDate,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Início Ciclo", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                                placeholder = { Text("dd/mm/aaaa", fontSize = 11.sp) },
                                                singleLine = true,
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        showDatePicker(context, cycleEntry.startDateParsed ?: LocalDate.now()) { newDate: LocalDate ->
                                                            val list = variableCycles.toMutableList()
                                                            list[index] = list[index].copy(startDate = newDate.toString())
                                                            variableCycles = list
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.CalendarToday,
                                                            contentDescription = "Selecionar início",
                                                            tint = OrangeNeon,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        showDatePicker(context, cycleEntry.startDateParsed ?: LocalDate.now()) { newDate: LocalDate ->
                                                            val list = variableCycles.toMutableList()
                                                            list[index] = list[index].copy(startDate = newDate.toString())
                                                            variableCycles = list
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            OutlinedTextField(
                                                value = cycleEntry.formattedEndDate,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Final Ciclo", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                                                placeholder = { Text("dd/mm/aaaa", fontSize = 11.sp) },
                                                singleLine = true,
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        showDatePicker(context, cycleEntry.endDateParsed ?: LocalDate.now()) { newDate: LocalDate ->
                                                            val list = variableCycles.toMutableList()
                                                            list[index] = list[index].copy(endDate = newDate.toString())
                                                            variableCycles = list
                                                            pendingCycleIndex = index
                                                            pendingEndDate = newDate
                                                            showIncludeEndDateDialog = true
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.CalendarToday,
                                                            contentDescription = "Selecionar final",
                                                            tint = OrangeNeon,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        showDatePicker(context, cycleEntry.endDateParsed ?: LocalDate.now()) { newDate: LocalDate ->
                                                            val list = variableCycles.toMutableList()
                                                            list[index] = list[index].copy(endDate = newDate.toString())
                                                            variableCycles = list
                                                            pendingCycleIndex = index
                                                            pendingEndDate = newDate
                                                            showIncludeEndDateDialog = true
                                                        }
                                                    },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }

                                        // Switch de inclusão da data final com tooltip
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f, fill = false),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "Incluir valores da data final no cálculo?",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = { showTooltipInfoDialog = true },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = "Informações",
                                                        tint = OrangeNeon,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = cycleEntry.includeEndDate,
                                                onCheckedChange = { checked ->
                                                    val list = variableCycles.toMutableList()
                                                    list[index] = list[index].copy(includeEndDate = checked)
                                                    variableCycles = list
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.Black,
                                                    checkedTrackColor = GreenNeon
                                                )
                                            )
                                        }

                                        // Campo: Dias para ser Pagos
                                        OutlinedTextField(
                                            value = cycleEntry.paymentDelayDaysText,
                                            onValueChange = { input ->
                                                val list = variableCycles.toMutableList()
                                                list[index] = list[index].copy(paymentDelayDaysText = input.filter { it.isDigit() })
                                                variableCycles = list
                                            },
                                            label = { Text("Dias para ser Pagos", fontSize = 11.sp) },
                                            placeholder = { Text("Ex: 7", fontSize = 12.sp) },
                                            suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon, fontSize = 12.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        // Campo: Data Pagamento (calculada automaticamente)
                                        OutlinedTextField(
                                            value = cycleEntry.calculatedPaymentDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Data Pagamento", fontSize = 11.sp) },
                                            placeholder = { Text("Calculada automaticamente", fontSize = 11.sp) },
                                            leadingIcon = {
                                                Icon(Icons.Default.Payments, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
                                            },
                                            trailingIcon = {
                                                Icon(Icons.Default.Lock, contentDescription = "Bloqueado", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(15.dp))
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        // Card de destaque da data completa
                                        Surface(
                                            color = OrangeNeon.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = OrangeNeon,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = cycleEntry.formattedPaymentDateText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = OrangeNeon,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Botão para adicionar mais ciclos
                            OutlinedButton(
                                onClick = {
                                    val last = variableCycles.lastOrNull()
                                    val newStart = last?.endDateParsed?.plusDays(1) ?: LocalDate.now()
                                    val newEnd = newStart.plusDays(6)
                                    val list = variableCycles.toMutableList()
                                    list.add(
                                        VariableCycleFormEntry(
                                            startDate = newStart.toString(),
                                            endDate = newEnd.toString(),
                                            includeEndDate = true,
                                            paymentDelayDaysText = "7"
                                        )
                                    )
                                    variableCycles = list
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = OrangeNeon),
                                border = BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrangeNeon)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Adicionar Ciclo de Pagamento", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // 9. SESSÃO: DADOS BANCÁRIOS (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Dados de Recebimento Bancário (Opcional)",
                    subtitle = "Conta bancária para depósitos diretos",
                    icon = Icons.Default.AccountBalance,
                    initiallyExpanded = false,
                    isExpandedControlled = expandBanking,
                    onExpandedChange = { expandBanking = it }
                ) {
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Instituição financeira") },
                        placeholder = { Text("Ex: Nubank, Itaú, Bradesco, Inter...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OrangeNeon) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = bankAgency,
                            onValueChange = { bankAgency = it },
                            label = { Text("Agência") },
                            placeholder = { Text("0001") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = bankAccount,
                            onValueChange = { bankAccount = it },
                            label = { Text("Conta") },
                            placeholder = { Text("123456-7") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 10. SESSÃO: CHAVE PIX (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Chave PIX para Repasses (Opcional)",
                    subtitle = "Chave PIX para transferências instantâneas",
                    icon = Icons.Default.QrCode,
                    initiallyExpanded = false,
                    isExpandedControlled = expandPix,
                    onExpandedChange = { expandPix = it }
                ) {
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
                                value = pixKeyType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tipo") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pixTypeExpanded) },
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
                                            pixKeyType = type
                                            pixTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = pixKey,
                            onValueChange = { pixKey = it },
                            label = { Text("Chave PIX") },
                            placeholder = { Text("Digite a chave") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // 11. SESSÃO: STATUS DA PLATAFORMA (Expansível / Colapsável)
                CollapsibleSectionCard(
                    title = "Status da Plataforma",
                    subtitle = "Ativação no aplicativo para o entregador",
                    icon = Icons.Default.ToggleOn,
                    initiallyExpanded = true,
                    isExpandedControlled = expandStatus,
                    onExpandedChange = { expandStatus = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (active) "ATIVA" else "INATIVA",
                                        color = if (active) GreenNeon else RedAlert,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Permitir selecionar esta plataforma em novos lançamentos",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = active,
                            onCheckedChange = { active = it },
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

// -------------------------------------------------------------
// SUB-COMPONENTES AUXILIARES
// -------------------------------------------------------------

@Composable
private fun PlatformPartnerLivePreviewCard(
    name: String,
    segment: String,
    cycle: String,
    paymentDay: String?,
    deliveryType: String,
    rating: Int,
    active: Boolean
) {
    val isLogistica = segment.equals("logistica", ignoreCase = true)
    val segmentBadgeColor = if (isLogistica) BlueInfo else GreenNeon
    val segmentLabel = if (isLogistica) "Logística" else "Delivery"
    val cycleLabel = BillingCycleCalculator.getCycleDisplayLabel(cycle, paymentDay)
    val vehicleIcon = getDeliveryTypeIcon(deliveryType)
    val vehicleLabel = getDeliveryTypeLabel(deliveryType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (active) OrangeNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vehicleIcon,
                            contentDescription = null,
                            tint = OrangeNeon,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = name.ifBlank { "Nome da Plataforma" },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = vehicleLabel,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "★ $rating/5",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }

                Surface(
                    color = if (active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (active) "ATIVA" else "INATIVA",
                        color = if (active) GreenNeon else RedAlert,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = segmentBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, segmentBadgeColor.copy(alpha = 0.4f))
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
            }
        }
    }
}

@Composable
private fun SelectableSegmentCardItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectableOptionCardItem(
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
        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectableDeliveryTypeCardItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OrangeNeon.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SelectableCycleCardItem(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OrangeNeon.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun showDatePicker(
    context: Context,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    ).show()
}
