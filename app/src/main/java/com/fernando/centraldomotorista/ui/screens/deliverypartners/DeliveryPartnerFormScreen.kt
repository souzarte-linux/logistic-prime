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
import com.fernando.centraldomotorista.ui.utils.CepVisualTransformation
import com.fernando.centraldomotorista.ui.utils.CpfVisualTransformation
import com.fernando.centraldomotorista.ui.utils.PhoneVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPartnerFormScreen(
    partnerId: String? = null,
    viewModel: DeliveryPartnersViewModel = viewModel(),
    onNavigateBack: () -> Unit
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
        } else {
            viewModel.openCreateForm()
        }
    }

    LaunchedEffect(uiState.partners) {
        if (!partnerId.isNullOrBlank() && form.id != partnerId) {
            val existing = uiState.partners.firstOrNull { it.id == partnerId }
            if (existing != null) {
                viewModel.openEditForm(existing)
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

    var showIncludeEndDateDialog by remember { mutableStateOf(false) }
    var pendingEndDate by remember { mutableStateOf<LocalDate?>(null) }

    // Confirmação de inclusão de valores da data final
    if (showIncludeEndDateDialog && pendingEndDate != null) {
        val formattedDate = pendingEndDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: ""
        AlertDialog(
            onDismissRequest = { showIncludeEndDateDialog = false },
            icon = {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(28.dp))
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
                        viewModel.onIncludeEndDateChanged(true)
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
                        viewModel.onIncludeEndDateChanged(false)
                        showIncludeEndDateDialog = false
                    }
                ) {
                    Text("Não incluir")
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

                // 1. Card: Dados Pessoais & Contato
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
                            text = "DADOS PESSOAIS & CONTATO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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

                // 2. Card: Endereço (ViaCEP)
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
                            text = "ENDEREÇO (VIACEP)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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

                // 3. Card: Dados Bancários & PIX
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
                            text = "DADOS BANCÁRIOS & PIX",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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

                // 4. Card: Parâmetros Operacionais
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PARÂMETROS OPERACIONAIS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Rota Preferida (Dropdown)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { routeDropdownExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                        Icon(Icons.Default.AltRoute, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(20.dp))
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
                                        viewModel.onPreferredRouteChanged(null)
                                        routeDropdownExpanded = false
                                    }
                                )
                                uiState.routes.forEach { route ->
                                    DropdownMenuItem(
                                        text = { Text(route.name, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            viewModel.onPreferredRouteChanged(route.id)
                                            routeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Taxa e Bônus
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = form.packageRateText,
                                onValueChange = { viewModel.onPackageRateChanged(it) },
                                label = { Text("Taxa / Pacote") },
                                prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = form.defaultBonusText,
                                onValueChange = { viewModel.onDefaultBonusChanged(it) },
                                label = { Text("Bônus Padrão") },
                                prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                placeholder = { Text("0,00") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = formOutlinedColors(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // SELEÇÃO DE TIPO DE ENTREGA / VEÍCULO
                        // Disposição em 2 linhas estruturadas e equilibradas: evita overflow e corrige desalinhamento do 'Utilitário'
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "TIPO DE ENTREGA / VEÍCULO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Linha 1: Moto, Carro, Utilitário (veículos motorizados de carga)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelectableDeliveryTypeCard(
                                    modifier = Modifier.weight(1f),
                                    key = "moto",
                                    label = "Moto",
                                    icon = Icons.Default.TwoWheeler,
                                    isSelected = form.deliveryType == "moto",
                                    onClick = { viewModel.onDeliveryTypeChanged("moto") }
                                )
                                SelectableDeliveryTypeCard(
                                    modifier = Modifier.weight(1f),
                                    key = "carro",
                                    label = "Carro",
                                    icon = Icons.Default.DirectionsCar,
                                    isSelected = form.deliveryType == "carro",
                                    onClick = { viewModel.onDeliveryTypeChanged("carro") }
                                )
                                SelectableDeliveryTypeCard(
                                    modifier = Modifier.weight(1f),
                                    key = "utilitario",
                                    label = "Utilitário",
                                    icon = Icons.Default.LocalShipping,
                                    isSelected = form.deliveryType == "utilitario",
                                    onClick = { viewModel.onDeliveryTypeChanged("utilitario") }
                                )
                            }

                            // Linha 2: Bike, A pé (veículos leves / modais ativos)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SelectableDeliveryTypeCard(
                                    modifier = Modifier.weight(1f),
                                    key = "bike",
                                    label = "Bicicleta",
                                    icon = Icons.Default.DirectionsBike,
                                    isSelected = form.deliveryType == "bike",
                                    onClick = { viewModel.onDeliveryTypeChanged("bike") }
                                )
                                SelectableDeliveryTypeCard(
                                    modifier = Modifier.weight(1f),
                                    key = "a_pe",
                                    label = "A pé (Express)",
                                    icon = Icons.Default.DirectionsWalk,
                                    isSelected = form.deliveryType == "a_pe",
                                    onClick = { viewModel.onDeliveryTypeChanged("a_pe") }
                                )
                            }
                        }

                        // CLASSIFICAÇÃO / AVALIAÇÃO
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CLASSIFICAÇÃO DO ENTREGADOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val ratingLabel = when (form.rating) {
                                1 -> "Iniciante (1/5)"
                                2 -> "Básico (2/5)"
                                3 -> "Intermediário (3/5)"
                                4 -> "Experiente (4/5)"
                                5 -> "Excelente (5/5)"
                                else -> "${form.rating}/5"
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                                onClick = { viewModel.onRatingChanged(i) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (i <= form.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = "$i Estrelas",
                                                    tint = if (i <= form.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
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
                    }
                }

                // 5. Card: CICLO DE PAGAMENTO (Fiel à tela "Editar Plataforma")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "CICLO DE PAGAMENTO",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Como o entregador parceiro recebe os repasses das entregas",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Grid 2x2 de Ciclos (Semanal, Quinzenal, Mensal, Variável)
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
                                    val isSelected = activeCycleKey == key
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

                        // Regras específicas do ciclo selecionado (iguais ao Gestor de Plataforma)
                        when (activeCycleKey) {
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
                                            val isDaySelected = form.paymentDay.equals(day, ignoreCase = true)
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
                                        value = form.fixedPayDelayText,
                                        onValueChange = { viewModel.onFixedPayDelayTextChanged(it) },
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
                                            Icon(Icons.Default.Info, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "Se fechar toda ${form.paymentDay} com prazo de ${form.fixedPayDelay} dias, o repasse cai na ${form.paymentDay} seguinte da semana.",
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
                                        value = form.fixedPayDelayText,
                                        onValueChange = { viewModel.onFixedPayDelayTextChanged(it) },
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

                                    val helpText = if (activeCycleKey == "quinzenal")
                                        "Fechamento automático no dia 15 e no último dia do mês. O pagamento é depositado ${form.fixedPayDelay} dias após cada fechamento."
                                    else
                                        "Fechamento automático no último dia do mês. O pagamento é depositado ${form.fixedPayDelay} dias após o fechamento."

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
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Cortes & Prazos Personalizados",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Defina o período do ciclo e visualize a data exata de pagamento",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Campo 1 e Campo 2: Início Ciclo e Final Ciclo
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Campo 1: Início Ciclo (tipo data)
                                        OutlinedTextField(
                                            value = form.formattedCycleStartDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Início Ciclo") },
                                            placeholder = { Text("dd/mm/aaaa") },
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    showDatePicker(context, form.cycleStartDateParsed ?: LocalDate.now()) { newDate ->
                                                        viewModel.onCycleStartDateSelected(newDate)
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Default.CalendarToday,
                                                        contentDescription = "Selecionar início do ciclo",
                                                        tint = OrangeNeon,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    showDatePicker(context, form.cycleStartDateParsed ?: LocalDate.now()) { newDate ->
                                                        viewModel.onCycleStartDateSelected(newDate)
                                                    }
                                                },
                                            colors = formOutlinedColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        // Campo 2: Final Ciclo (tipo data)
                                        OutlinedTextField(
                                            value = form.formattedCycleEndDate,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Final Ciclo") },
                                            placeholder = { Text("dd/mm/aaaa") },
                                            trailingIcon = {
                                                IconButton(onClick = {
                                                    showDatePicker(context, form.cycleEndDateParsed ?: LocalDate.now()) { newDate ->
                                                        viewModel.onCycleEndDateSelected(newDate)
                                                        pendingEndDate = newDate
                                                        showIncludeEndDateDialog = true
                                                    }
                                                }) {
                                                    Icon(
                                                        Icons.Default.CalendarToday,
                                                        contentDescription = "Selecionar final do ciclo",
                                                        tint = OrangeNeon,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    showDatePicker(context, form.cycleEndDateParsed ?: LocalDate.now()) { newDate ->
                                                        viewModel.onCycleEndDateSelected(newDate)
                                                        pendingEndDate = newDate
                                                        showIncludeEndDateDialog = true
                                                    }
                                                },
                                            colors = formOutlinedColors(),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    // Pergunta e Controle da Data Final (Feedback e Toggle Visual)
                                    Surface(
                                        onClick = { viewModel.onIncludeEndDateChanged(!form.includeEndDate) },
                                        color = if (form.includeEndDate) GreenNeon.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (form.includeEndDate) GreenNeon.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Incluir valores da data final no cálculo?",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (form.includeEndDate)
                                                        "Sim: Os valores e corridas do dia ${form.formattedCycleEndDate} serão computados no cálculo."
                                                    else
                                                        "Não: Os valores da data final ${form.formattedCycleEndDate} não entram no ciclo.",
                                                    fontSize = 11.sp,
                                                    color = if (form.includeEndDate) GreenNeon else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = form.includeEndDate,
                                                onCheckedChange = { viewModel.onIncludeEndDateChanged(it) },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.Black,
                                                    checkedTrackColor = GreenNeon
                                                )
                                            )
                                        }
                                    }

                                    // Campo 3: Dias para ser Pagos (número inteiro) - com placeholder e apaga ao clicar/focar
                                    OutlinedTextField(
                                        value = form.paymentDelayDaysText,
                                        onValueChange = { viewModel.onPaymentDelayDaysChanged(it) },
                                        label = { Text("Dias para ser Pagos") },
                                        placeholder = { Text("Ex: 7") },
                                        suffix = { Text("dias", fontWeight = FontWeight.Bold, color = OrangeNeon) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        colors = formOutlinedColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { focusState ->
                                                if (focusState.isFocused && form.paymentDelayDaysText.isNotBlank()) {
                                                    viewModel.clearPaymentDelayDays()
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    // Campo 4: Data Pagamento (tipo data - não editável)
                                    OutlinedTextField(
                                        value = form.calculatedPaymentDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Data Pagamento") },
                                        placeholder = { Text("Calculada automaticamente") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Payments, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(20.dp))
                                        },
                                        trailingIcon = {
                                            Icon(Icons.Default.Lock, contentDescription = "Campo não editável", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        },
                                        colors = formOutlinedColors(),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    // Card de destaque da data completa do pagamento conforme exemplo do usuário:
                                    Surface(
                                        color = OrangeNeon.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = OrangeNeon,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = form.formattedPaymentDateText,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = OrangeNeon,
                                                    lineHeight = 18.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Calculado automaticamente: Data Final (${form.formattedCycleEndDate}) + ${form.paymentDelayDays} dias.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Card: Status do Entregador (Ativo / Inativo - Padrão da tela de Plataforma)
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
                                    text = "Status do Entregador",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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
private fun SelectableDeliveryTypeCard(
    modifier: Modifier = Modifier,
    key: String,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) OrangeNeon.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isSelected) OrangeNeon else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) OrangeNeon else MaterialTheme.colorScheme.onSurface,
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
    entry: FormCycleEntry,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onCutChange: (String) -> Unit,
    onPayDelayChange: (String) -> Unit
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
                    value = entry.cutText,
                    onValueChange = onCutChange,
                    label = { Text("Fechamento (dia)") },
                    placeholder = { Text("1-31") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = entry.payDelayText,
                    onValueChange = onPayDelayChange,
                    label = { Text("Pagamento (dias)") },
                    placeholder = { Text("Dias") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeNeon,
                        focusedLabelColor = OrangeNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            val cut = entry.cutText.toIntOrNull() ?: 0
            val payDelay = entry.payDelayText.toIntOrNull() ?: 0
            val feedback = if (cut in 1..31) {
                val payDayEst = cut + payDelay
                if (payDayEst > 28) {
                    val nextMonthDay = if (payDayEst > 31) payDayEst - 30 else payDayEst
                    "Fecha dia $cut - Pago dia $nextMonthDay do mês seguinte"
                } else {
                    "Fecha dia $cut - Pago dia $payDayEst"
                }
            } else {
                "Informe o dia do fechamento (1 a 31)"
            }

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

@Composable
private fun formOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeNeon,
    focusedLabelColor = OrangeNeon,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

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


