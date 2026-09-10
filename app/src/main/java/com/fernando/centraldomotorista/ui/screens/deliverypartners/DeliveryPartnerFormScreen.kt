package com.fernando.centraldomotorista.ui.screens.deliverypartners

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.PartnerAvatar
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeIcon
import com.fernando.centraldomotorista.ui.screens.deliverypartners.components.getDeliveryTypeLabel
import com.fernando.centraldomotorista.ui.theme.*

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
    var cycleFixedDropdownExpanded by remember { mutableStateOf(false) }
    var customDayInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val selectedRouteName = remember(form.preferredRouteId, uiState.routes) {
        uiState.routes.firstOrNull { it.id == form.preferredRouteId }?.name ?: "Nenhuma (Sem preferência)"
    }

    // Discard changes dialog
    if (uiState.showDiscardAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDiscardAlert() },
            title = { Text("Descartar alterações?", fontWeight = FontWeight.Bold) },
            text = { Text("Existem alterações não salvas. Deseja realmente sair e descartar tudo?") },
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
            }
        )
    }

    // Delete confirm dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Entregador", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja excluir ${form.fullName}? Esta ação não pode ser desfeita.") },
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
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "EDITAR ENTREGADOR" else "NOVO ENTREGADOR",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                                contentDescription = "Excluir",
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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (uiState.isFormDirty) viewModel.requestCloseForm() else onNavigateBack()
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = { viewModel.savePartner() },
                        enabled = !uiState.isSaving && form.fullName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1.5f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (isEditing) "Salvar Alterações" else "Cadastrar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview do Avatar com Foto ou Iniciais
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PartnerAvatar(
                    photoUrl = form.photoUrl.ifBlank { null },
                    name = form.fullName,
                    size = 64.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (form.fullName.isNotBlank()) form.fullName else "Novo Parceiro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (form.photoUrl.isNotBlank()) "Foto personalizada" else "Iniciais automáticas",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // -------------------------------------------------------------
            // SEÇÃO: IDENTIFICAÇÃO E CONTATO
            // -------------------------------------------------------------
            SectionHeaderForm(title = "DADOS PESSOAIS & CONTATO", icon = Icons.Default.Person)

            // Nome Completo *
            OutlinedTextField(
                value = form.fullName,
                onValueChange = { viewModel.onFullNameChanged(it) },
                label = { Text("Nome Completo *") },
                singleLine = true,
                isError = form.fullName.isBlank(),
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

            // Telefone / Contato com máscara e WhatsApp
            OutlinedTextField(
                value = form.phone,
                onValueChange = { viewModel.onPhoneChanged(it) },
                label = { Text("Celular / Contato") },
                placeholder = { Text("(00) 00000-0000") },
                singleLine = true,
                visualTransformation = PhoneVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = formOutlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onIsWhatsappChanged(!form.isWhatsapp) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = form.isWhatsapp,
                    onCheckedChange = { viewModel.onIsWhatsappChanged(it) },
                    colors = CheckboxDefaults.colors(checkedColor = OrangeNeon)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Este número é WhatsApp",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Rede Social
            OutlinedTextField(
                value = form.socialMedia,
                onValueChange = { viewModel.onSocialMediaChanged(it) },
                label = { Text("Rede Social (Instagram, etc.)") },
                placeholder = { Text("@usuario ou link") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                colors = formOutlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // -------------------------------------------------------------
            // SEÇÃO: ENDEREÇO (ViaCEP)
            // -------------------------------------------------------------
            SectionHeaderForm(title = "ENDEREÇO (VIACEP)", icon = Icons.Default.LocationOn)

            // CEP
            OutlinedTextField(
                value = form.cep,
                onValueChange = { viewModel.onCepChanged(it) },
                label = { Text("CEP") },
                placeholder = { Text("00000-000") },
                singleLine = true,
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
                singleLine = true,
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
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                    colors = formOutlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // -------------------------------------------------------------
            // SEÇÃO: DADOS BANCÁRIOS E PIX
            // -------------------------------------------------------------
            SectionHeaderForm(title = "PAGAMENTOS (PIX)", icon = Icons.Default.AccountBalance)

            OutlinedTextField(
                value = form.pixKey,
                onValueChange = { viewModel.onPixKeyChanged(it) },
                label = { Text("Chave PIX") },
                placeholder = { Text("CPF, Celular, E-mail ou Aleatória") },
                singleLine = true,
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = formOutlinedColors(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // -------------------------------------------------------------
            // SEÇÃO: PARÂMETROS OPERACIONAIS
            // -------------------------------------------------------------
            SectionHeaderForm(title = "PARÂMETROS OPERACIONAIS", icon = Icons.Default.Settings)

            // Rota Preferida (Dropdown)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { routeDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Rota Preferida",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedRouteName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OrangeNeon)
                    }
                }

                DropdownMenu(
                    expanded = routeDropdownExpanded,
                    onDismissRequest = { routeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhuma (Sem preferência)") },
                        onClick = {
                            viewModel.onPreferredRouteChanged(null)
                            routeDropdownExpanded = false
                        }
                    )
                    uiState.routes.forEach { route ->
                        DropdownMenuItem(
                            text = { Text(route.name) },
                            onClick = {
                                viewModel.onPreferredRouteChanged(route.id)
                                routeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Valores de Taxa e Bônus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = form.packageRateText,
                    onValueChange = { viewModel.onPackageRateChanged(it) },
                    label = { Text("Taxa por Pacote (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = formOutlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = form.defaultBonusText,
                    onValueChange = { viewModel.onDefaultBonusChanged(it) },
                    label = { Text("Bônus Padrão (R$)") },
                    placeholder = { Text("0,00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = formOutlinedColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Tipo de Entrega
            Text(
                text = "Tipo de Entrega:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val types = listOf(
                    "a_pe" to "A pé",
                    "bike" to "Bike",
                    "moto" to "Moto",
                    "carro" to "Carro",
                    "utilitario" to "Utilitário"
                )
                types.forEach { (typeKey, label) ->
                    val isSelected = form.deliveryType == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onDeliveryTypeChanged(typeKey) },
                        label = { Text(label, fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = getDeliveryTypeIcon(typeKey),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangeNeon,
                            selectedLabelColor = Color.Black,
                            selectedLeadingIconColor = Color.Black
                        )
                    )
                }
            }

            // Avaliação / Classificação
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Classificação do Entregador:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    for (i in 1..5) {
                        IconButton(
                            onClick = { viewModel.onRatingChanged(i) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (i <= form.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "$i Estrelas",
                                tint = if (i <= form.rating) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SEÇÃO: CICLO DE REPASSE E STATUS
            // -------------------------------------------------------------
            SectionHeaderForm(title = "CICLO DE REPASSE & STATUS", icon = Icons.Default.Schedule)

            // Tipo de Ciclo: Fixo vs Variável
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = form.paymentCycleType == "fixed",
                    onClick = { viewModel.onPaymentCycleTypeChanged("fixed") },
                    label = { Text("Ciclo Fixo") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                        selectedLabelColor = OrangeNeon
                    )
                )
                FilterChip(
                    selected = form.paymentCycleType == "variable",
                    onClick = { viewModel.onPaymentCycleTypeChanged("variable") },
                    label = { Text("Ciclo Variável (Escalonado)") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangeNeon.copy(alpha = 0.2f),
                        selectedLabelColor = OrangeNeon
                    )
                )
            }

            if (form.paymentCycleType == "fixed") {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cycleFixedDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Frequência: ${form.paymentCycleFixed.replaceFirstChar { it.uppercase() }}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = OrangeNeon)
                        }
                    }

                    DropdownMenu(
                        expanded = cycleFixedDropdownExpanded,
                        onDismissRequest = { cycleFixedDropdownExpanded = false }
                    ) {
                        listOf("semanal" to "Semanal (a cada 7 dias)", "quinzenal" to "Quinzenal (a cada 15 dias)", "mensal" to "Mensal (a cada 30 dias)").forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.onPaymentCycleFixedChanged(key)
                                    cycleFixedDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Ciclo Variável
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sequência de Dias entre Repasses:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeNeon
                        )

                        form.paymentCycleVariableDays.forEachIndexed { index, days ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${index + 1}º ciclo: $days dias", fontSize = 13.sp)
                                Row {
                                    IconButton(
                                        onClick = { viewModel.removeVariableCycleDay(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover", tint = RedAlert, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customDayInput,
                                onValueChange = { customDayInput = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("Dias") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Button(
                                onClick = {
                                    val d = customDayInput.toIntOrNull()
                                    if (d != null && d > 0) {
                                        viewModel.addVariableCycleDay(d)
                                        customDayInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Adicionar")
                            }
                        }
                    }
                }
            }

            // Ativo / Inativo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Status do Entregador",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (form.active) "Entregador Ativo na equipe" else "Entregador Inativo (Oculto na rotina)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = form.active,
                    onCheckedChange = { viewModel.onActiveChanged(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = OrangeNeon
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderForm(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OrangeNeon, modifier = Modifier.size(18.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
            color = OrangeNeon
        )
    }
}

@Composable
private fun formOutlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OrangeNeon,
    focusedLabelColor = OrangeNeon,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
)

// Máscaras de formatação
class CpfVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 11) text.text.substring(0..10) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 2 || i == 5) out += "."
            if (i == 8) out += "-"
        }
        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset + 1
                if (offset <= 8) return offset + 2
                if (offset <= 11) return offset + 3
                return 14
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset - 1
                if (offset <= 11) return offset - 2
                if (offset <= 14) return offset - 3
                return 11
            }
        }
        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 11) text.text.substring(0..10) else text.text
        var out = ""
        for (i in trimmed.indices) {
            if (i == 0) out += "("
            out += trimmed[i]
            if (i == 1) out += ") "
            if (i == 6) out += "-"
        }
        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == 0) return 0
                if (offset <= 2) return offset + 1
                if (offset <= 7) return offset + 3
                if (offset <= 11) return offset + 4
                return 15
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 1) return 0
                if (offset <= 4) return offset - 1
                if (offset <= 10) return offset - 3
                if (offset <= 15) return offset - 4
                return 11
            }
        }
        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}

class CepVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 4) out += "-"
        }
        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                return 9
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 5) return offset
                if (offset <= 9) return offset - 1
                return 8
            }
        }
        return TransformedText(AnnotatedString(out), numberOffsetTranslator)
    }
}
