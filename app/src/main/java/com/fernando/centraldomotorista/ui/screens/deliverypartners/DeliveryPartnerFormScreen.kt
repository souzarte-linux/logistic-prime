package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.fernando.centraldomotorista.data.model.CycleEntry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeIcon
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.common.cards.CollapsibleSectionCard
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerPlatformsSection
import com.fernando.centraldomotorista.ui.utils.CepVisualTransformation
import com.fernando.centraldomotorista.ui.utils.CpfVisualTransformation
import com.fernando.centraldomotorista.ui.utils.PhoneVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerFormScreen(
    partnerId: String? = null,
    viewModel: DeliveryPartnersViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPartnerPlatformEdit: (partnerId: String?, platformId: String?) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = uiState.formData
    val isEditing = !partnerId.isNullOrBlank()

    LaunchedEffect(partnerId) {
        if (!partnerId.isNullOrBlank()) {
            val existing = uiState.partners.firstOrNull { it.id == partnerId }
            if (existing != null) {
                viewModel.openEditForm(existing)
            } else {
                viewModel.loadData()
            }
            viewModel.loadPlatformsForPartner(partnerId)
        } else {
            viewModel.openCreateForm()
        }
    }

    LaunchedEffect(uiState.partners) {
        if (!partnerId.isNullOrBlank() && form.id != partnerId) {
            val existing = uiState.partners.firstOrNull { it.id == partnerId }
            if (existing != null) {
                viewModel.openEditForm(existing)
                viewModel.loadPlatformsForPartner(partnerId)
            }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    // Intercept back button if form is dirty
    BackHandler {
        if (uiState.isFormDirty) {
            viewModel.requestCloseForm()
        } else {
            onNavigateBack()
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var routeDropdownExpanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val selectedRouteName = remember(form.preferredRouteId, uiState.routes) {
        uiState.routes.firstOrNull { it.id == form.preferredRouteId }?.name ?: "Nenhuma (Sem preferência)"
    }

    // Ciclo ativo selecionado (compatível com a lógica de ciclo da tela Editar Plataforma)
    val activeCycleKey = remember(form.cycle, form.paymentCycleType, form.paymentCycleFixed) {
        if (form.cycle.isNotBlank()) form.cycle else if (form.paymentCycleType == "variable") "misto" else (form.paymentCycleFixed ?: "semanal")
    }

    // Discard changes dialog
    if (uiState.showDiscardAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDiscardAlert() },
            title = { Text("Descartar alterações?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Existem alterações não salvas. Deseja realmente sair e descartar tudo?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.forceCloseForm()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert, contentColor = Color.White)
                ) {
                    Text("Descartar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDiscardAlert() }) {
                    Text("Continuar Editando")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Entregador?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Tem certeza que deseja excluir ${form.fullName}? Histórico e sessões vinculadas permanecerão registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        form.id?.let {
                            viewModel.deletePartner(it)
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
                            text = if (isEditing) "EDITAR ENTREGADOR" else "NOVO ENTREGADOR",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isEditing && form.fullName.isNotBlank()) {
                            Text(
                                text = form.fullName,
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
                    IconButton(onClick = {
                        if (uiState.isFormDirty) viewModel.requestCloseForm() else onNavigateBack()
                    }) {
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
                                contentDescription = "Excluir entregador",
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
        containerColor = MaterialTheme.colorScheme.background,
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
                        onClick = {
                            if (uiState.isFormDirty) viewModel.requestCloseForm() else onNavigateBack()
                        },
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
                        onClick = { viewModel.savePartner() },
                        enabled = !uiState.isSaving && form.fullName.isNotBlank(),
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
                                color = Color.Black,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditing) "SALVAR ALTERAÇÕES" else "CADASTRAR ENTREGADOR",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // Container responsivo com centralização e limite de largura (ótimo para telas pequenas, médias e tablets)
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
                // 0. Card de Pré-visualização do Entregador em Tempo Real (igual ao Gestor de Plataforma)
                PartnerLivePreviewCard(
                    name = form.fullName,
                    photoUrl = form.photoUrl,
                    deliveryType = form.deliveryType,
                    cycleKey = activeCycleKey,
                    variableCount = form.cycleEntries.size,
                    rating = form.rating,
                    active = form.active
                )

                // 1. Sessão: Dados Pessoais & Contato (Colapsável / Expansível)
                CollapsibleSectionCard(
                    title = "Dados Pessoais & Contato",
                    subtitle = "Nome, CPF, celular e redes de comunicação",
                    icon = Icons.Default.Person,
                    initiallyExpanded = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Nome Completo *
                        OutlinedTextField(
                            value = form.fullName,
                            onValueChange = { viewModel.onFullNameChanged(it) },
                            label = { Text("Nome Completo *") },
                            placeholder = { Text("Ex: Carlos Silva") },
                            singleLine = true,
                            isError = form.fullName.isBlank(),
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // URL da Foto (Opcional)
                        OutlinedTextField(
                            value = form.photoUrl,
                            onValueChange = { viewModel.onPhotoUrlChanged(it) },
                            label = { Text("URL da Foto do Entregador (Opcional)") },
                            placeholder = { Text("https://exemplo.com/foto.jpg") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // CPF (Opcional com máscara e validação)
                        OutlinedTextField(
                            value = form.cpf,
                            onValueChange = { viewModel.onCpfChanged(it) },
                            label = { Text("CPF (Opcional)") },
                            placeholder = { Text("000.000.000-00") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = OrangeNeon)
                            },
                            visualTransformation = CpfVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            isError = uiState.cpfError != null,
                            supportingText = {
                                if (uiState.cpfError != null) {
                                    Text(uiState.cpfError!!, color = RedAlert)
                                } else {
                                    Text("Apenas números, com validação de dígitos", fontSize = 11.sp)
                                }
                            },
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Telefone / Contato com máscara
                        OutlinedTextField(
                            value = form.phone,
                            onValueChange = { viewModel.onPhoneChanged(it) },
                            label = { Text("Celular / Contato") },
                            placeholder = { Text("(00) 00000-0000") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = OrangeNeon)
                            },
                            visualTransformation = PhoneVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Checkbox WhatsApp
                        Surface(
                            onClick = { viewModel.onIsWhatsappChanged(!form.isWhatsapp) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (form.isWhatsapp) GreenNeon.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (form.isWhatsapp) GreenNeon.copy(alpha = 0.4f) else Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Checkbox(
                                    checked = form.isWhatsapp,
                                    onCheckedChange = { viewModel.onIsWhatsappChanged(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = GreenNeon,
                                        checkmarkColor = Color.Black
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Este número é WhatsApp",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Permite abrir conversa direta pelo app em um clique",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Rede Social
                        OutlinedTextField(
                            value = form.socialMedia,
                            onValueChange = { viewModel.onSocialMediaChanged(it) },
                            label = { Text("Rede Social (Instagram, etc.)") },
                            placeholder = { Text("@usuario ou link") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. Sessão: Endereço (ViaCEP) (Colapsável / Expansível)
                CollapsibleSectionCard(
                    title = "Endereço (ViaCEP)",
                    subtitle = "Localização residencial e busca automática de CEP",
                    icon = Icons.Default.Place,
                    initiallyExpanded = false
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // CEP
                        OutlinedTextField(
                            value = form.cep,
                            onValueChange = { viewModel.onCepChanged(it) },
                            label = { Text("CEP") },
                            placeholder = { Text("00000-000") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Place, contentDescription = null, tint = OrangeNeon)
                            },
                            visualTransformation = CepVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            trailingIcon = {
                                if (uiState.isSearchingCep) {
                                    CircularProgressIndicator(
                                        color = OrangeNeon,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Logradouro / Rua
                        OutlinedTextField(
                            value = form.street,
                            onValueChange = { viewModel.onStreetChanged(it) },
                            label = { Text("Logradouro / Endereço") },
                            placeholder = { Text("Ex: Av. Principal") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Home, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = form.number,
                                onValueChange = { viewModel.onNumberChanged(it) },
                                label = { Text("Número") },
                                placeholder = { Text("123") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = form.neighborhood,
                                onValueChange = { viewModel.onNeighborhoodChanged(it) },
                                label = { Text("Bairro") },
                                placeholder = { Text("Bairro") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.5f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = form.city,
                                onValueChange = { viewModel.onCityChanged(it) },
                                label = { Text("Cidade") },
                                placeholder = { Text("Cidade") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(2f)
                            )

                            OutlinedTextField(
                                value = form.state,
                                onValueChange = { viewModel.onStateChanged(it.take(2).uppercase()) },
                                label = { Text("UF") },
                                placeholder = { Text("UF") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 3. Sessão: Dados Bancários & PIX (Colapsável / Expansível)
                CollapsibleSectionCard(
                    title = "Dados Bancários & PIX",
                    subtitle = "Chave PIX e banco para repasse de comissões",
                    icon = Icons.Default.AccountBalance,
                    initiallyExpanded = false
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = form.pixKey,
                            onValueChange = { viewModel.onPixKeyChanged(it) },
                            label = { Text("Chave PIX para Repasse") },
                            placeholder = { Text("CPF, Celular, E-mail ou Aleatória") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = form.pixBank,
                            onValueChange = { viewModel.onPixBankChanged(it) },
                            label = { Text("Instituição / Banco (Opcional)") },
                            placeholder = { Text("Ex: Nubank, Inter, Bradesco...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = OrangeNeon)
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                            colors = formOutlinedColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. NOVA SESSÃO: APP & PLATAFORMA PARA ENTREGADOR PARCEIRO (Replicando Gestão de Plataformas)
                CollapsibleSectionCard(
                    title = "App & Plataforma",
                    subtitle = "Plataformas vinculadas, parâmetros e repasses do entregador",
                    icon = Icons.Default.Smartphone,
                    initiallyExpanded = true
                ) {
                    PartnerPlatformsSection(
                        platforms = uiState.platforms,
                        earningsMap = uiState.earningsMap,
                        selectedPlatformId = uiState.selectedPlatformId,
                        onSelectPlatform = { platform ->
                            viewModel.selectPlatformForPartner(platform)
                        },
                        onEditPlatform = { platId ->
                            onNavigateToPartnerPlatformEdit(form.id, platId)
                        },
                        onCreatePlatform = {
                            onNavigateToPartnerPlatformEdit(form.id, null)
                        },
                        onToggleActive = { platform ->
                            viewModel.togglePlatformActive(form.id, platform)
                        }
                    )
                }

                // 5. Sessão: Status do Entregador (Colapsável / Expansível)
                CollapsibleSectionCard(
                    title = "Status do Entregador",
                    subtitle = "Ativação no app para novas rotas e sessões de entrega",
                    icon = Icons.Default.ToggleOn,
                    initiallyExpanded = true
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
                                    text = "Status do Entregador",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = if (form.active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (form.active) "ATIVO" else "INATIVO",
                                        color = if (form.active) GreenNeon else RedAlert,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Permitir selecionar este parceiro em novas rotas e sessões de entrega",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = form.active,
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

// -------------------------------------------------------------
// SUB-COMPONENTES ESPECIALIZADOS
// -------------------------------------------------------------

@Composable
private fun PartnerLivePreviewCard(
    name: String,
    photoUrl: String,
    deliveryType: String,
    cycleKey: String,
    variableCount: Int,
    rating: Int,
    active: Boolean
) {
    val typeIcon = getDeliveryTypeIcon(deliveryType)
    val typeLabel = getDeliveryTypeLabel(deliveryType)
    val cycleLabel = when (cycleKey.lowercase()) {
        "semanal" -> "Semanal (7d)"
        "quinzenal" -> "Quinzenal (15d)"
        "mensal" -> "Mensal (30d)"
        "misto", "variavel" -> "Variável ($variableCount cortes)"
        else -> cycleKey.replaceFirstChar { it.uppercase() }
    }

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
            PartnerAvatar(
                photoUrl = photoUrl.ifBlank { null },
                name = name,
                size = 54.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name.ifBlank { "Nome do Entregador" },
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
                    // Badge Modal / Veículo
                    Surface(
                        color = BlueInfo.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BlueInfo.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(typeIcon, contentDescription = null, tint = BlueInfo, modifier = Modifier.size(11.dp))
                            Text(
                                text = typeLabel,
                                color = BlueInfo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Badge Ciclo
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

                    // Badge Status
                    Surface(
                        color = if (active) GreenNeon.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (active) "ATIVO" else "INATIVO",
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
private fun formOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeNeon,
    focusedLabelColor = OrangeNeon,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)


