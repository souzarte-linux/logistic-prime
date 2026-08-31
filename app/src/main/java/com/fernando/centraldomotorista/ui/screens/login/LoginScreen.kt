package com.fernando.centraldomotorista.ui.screens.login

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernando.centraldomotorista.auth.AuthState
import com.fernando.centraldomotorista.auth.AuthViewModel
import com.fernando.centraldomotorista.auth.BiometricAuthHelper
import com.fernando.centraldomotorista.auth.GoogleAuthClient
import com.fernando.centraldomotorista.auth.SignInResult
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val googleAuthClient = remember { GoogleAuthClient(context) }

    val savedEmail by authViewModel.rememberedEmail.collectAsStateWithLifecycle()
    val savedRememberMe by authViewModel.rememberMe.collectAsStateWithLifecycle()
    val isBiometricEnabled by authViewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isBiometricPrompted by authViewModel.isBiometricPrompted.collectAsStateWithLifecycle()

    var emailText by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMeChecked by remember { mutableStateOf(false) }

    // Estados de diálogo
    var showSignUpDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showEnableBiometricDialog by remember { mutableStateOf(false) }

    val isBiometricAvailable = remember { BiometricAuthHelper.isBiometricAvailable(context) }

    // Preenche o e-mail salvo se "Lembrar-me" estiver ativo
    LaunchedEffect(savedEmail, savedRememberMe) {
        if (savedEmail.isNotBlank()) {
            emailText = savedEmail
            rememberMeChecked = savedRememberMe
        }
    }

    // Monitora sucesso de login para solicitar ativação de biometria se aplicável
    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            if (isBiometricAvailable && !isBiometricPrompted && !isBiometricEnabled) {
                showEnableBiometricDialog = true
            } else {
                onLoginSuccess()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Ícone / Logo do App em Destaque
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(OrangeNeon.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🚚",
                    fontSize = 34.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Título Principal
            Text(
                text = "CENTRAL DO MOTORISTA",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtítulo
            Text(
                text = "Controle financeiro e operacional para motoristas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Card Principal de Formulário de Login
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Acesse sua conta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Campo de E-mail
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it },
                        label = { Text("E-mail") },
                        placeholder = { Text("seu.email@exemplo.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = OrangeNeon)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            cursorColor = OrangeNeon
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Campo de Senha com Ícone de Olho
                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { passwordText = it },
                        label = { Text("Senha") },
                        placeholder = { Text("Digite sua senha") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = OrangeNeon)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                authViewModel.signInWithEmail(
                                    email = emailText,
                                    pass = passwordText,
                                    remember = rememberMeChecked
                                )
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon,
                            cursorColor = OrangeNeon
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Checkbox "Lembrar-me" + Link "Esqueci minha senha"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMeChecked = !rememberMeChecked }
                        ) {
                            Checkbox(
                                checked = rememberMeChecked,
                                onCheckedChange = { rememberMeChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = OrangeNeon,
                                    checkmarkColor = Color.Black
                                )
                            )
                            Text(
                                text = "Lembrar-me",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { showForgotPasswordDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Esqueci minha senha",
                                fontSize = 12.sp,
                                color = OrangeNeon,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Botão Entrar por E-mail/Senha
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.signInWithEmail(
                                email = emailText,
                                pass = passwordText,
                                remember = rememberMeChecked
                            )
                        },
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Entrar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Botão Rápido de Biometria (se habilitado)
                    if (isBiometricAvailable && isBiometricEnabled) {
                        OutlinedButton(
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuthHelper.showBiometricPrompt(
                                        activity = activity,
                                        onSuccess = {
                                            authViewModel.checkCurrentUser()
                                            onLoginSuccess()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Biometria indisponível na atividade atual", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangeNeon),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Digital",
                                    tint = OrangeNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Entrar com Impressão Digital",
                                    color = OrangeNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Divisor "OU"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        Text(
                            text = "OU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    }

                    // Botão Google Sign-In
                    GoogleSignInButton(
                        onClick = {
                            authViewModel.clearError()
                            scope.launch {
                                when (val result = googleAuthClient.signIn()) {
                                    is SignInResult.Success -> {
                                        authViewModel.handleSignInSuccess(result.supabaseUser)
                                    }
                                    is SignInResult.Error -> {
                                        authViewModel.handleSignInError(result.message)
                                    }
                                    is SignInResult.Cancelled -> {}
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Link para Cadastro de Nova Conta
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ainda não tem uma conta? ",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Criar conta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeNeon,
                    modifier = Modifier.clickable { showSignUpDialog = true }
                )
            }

            // Exibição de Mensagem de Erro
            if (authState is AuthState.Error) {
                val errorMessage = (authState as AuthState.Error).message
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = RedAlert.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = RedAlert,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { authViewModel.clearError() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = RedAlert,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DIÁLOGO: Criar Conta Nova
    if (showSignUpDialog) {
        var signUpName by remember { mutableStateOf("") }
        var signUpEmail by remember { mutableStateOf("") }
        var signUpPassword by remember { mutableStateOf("") }
        var signUpConfirmPassword by remember { mutableStateOf("") }
        var signUpPassVisible by remember { mutableStateOf(false) }
        var isSubmittingSignUp by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingSignUp) showSignUpDialog = false },
            title = {
                Text(
                    text = "Criar Nova Conta",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = OrangeNeon
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Preencha seus dados para criar seu acesso na Central do Motorista.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = signUpName,
                        onValueChange = { signUpName = it },
                        label = { Text("Nome Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        )
                    )

                    OutlinedTextField(
                        value = signUpEmail,
                        onValueChange = { signUpEmail = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        )
                    )

                    OutlinedTextField(
                        value = signUpPassword,
                        onValueChange = { signUpPassword = it },
                        label = { Text("Senha (mínimo 6 dígitos)") },
                        singleLine = true,
                        visualTransformation = if (signUpPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { signUpPassVisible = !signUpPassVisible }) {
                                Icon(
                                    imageVector = if (signUpPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        )
                    )

                    OutlinedTextField(
                        value = signUpConfirmPassword,
                        onValueChange = { signUpConfirmPassword = it },
                        label = { Text("Confirmar Senha") },
                        singleLine = true,
                        visualTransformation = if (signUpPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (signUpPassword != signUpConfirmPassword) {
                            Toast.makeText(context, "As senhas não conferem.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingSignUp = true
                        authViewModel.signUpWithEmail(
                            email = signUpEmail,
                            pass = signUpPassword,
                            fullName = signUpName,
                            remember = rememberMeChecked,
                            onSuccess = {
                                isSubmittingSignUp = false
                                showSignUpDialog = false
                                Toast.makeText(context, "Conta cadastrada com sucesso!", Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isSubmittingSignUp = false
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSubmittingSignUp && signUpEmail.isNotBlank() && signUpPassword.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    if (isSubmittingSignUp) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Cadastrar", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSignUpDialog = false },
                    enabled = !isSubmittingSignUp
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // DIÁLOGO: Esqueci Minha Senha (Recuperação de E-mail)
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(emailText) }
        var isSubmittingReset by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingReset) showForgotPasswordDialog = false },
            title = {
                Text(
                    text = "Recuperar Senha",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = OrangeNeon
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Digite o e-mail cadastrado. Se o e-mail existir, você receberá um link para redefinir sua senha.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("E-mail para recuperação") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OrangeNeon,
                            focusedLabelColor = OrangeNeon
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingReset = true
                        authViewModel.sendPasswordResetEmail(resetEmail) { _, message ->
                            isSubmittingReset = false
                            showForgotPasswordDialog = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSubmittingReset && resetEmail.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    if (isSubmittingReset) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Enviar Link", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isSubmittingReset
                ) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // DIÁLOGO PÓS-LOGIN: Ativar Biometria
    if (showEnableBiometricDialog) {
        AlertDialog(
            onDismissRequest = {
                authViewModel.setBiometricEnabled(false)
                showEnableBiometricDialog = false
                onLoginSuccess()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Ativar Entrada por Digital?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Deseja usar sua impressão digital para entrar mais rapidamente nas próximas vezes?",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.setBiometricEnabled(true)
                        showEnableBiometricDialog = false
                        Toast.makeText(context, "Entrada por digital ativada!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeNeon, contentColor = Color.Black)
                ) {
                    Text("Ativar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        authViewModel.setBiometricEnabled(false)
                        showEnableBiometricDialog = false
                        onLoginSuccess()
                    }
                ) {
                    Text("Agora não", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

/**
 * Botão com o padrão visual oficial do Google Sign-In
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleIcon(modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Entrar com Google",
                color = Color(0xFF1F1F1F),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Ícone oficial de 4 cores do Google desenhado em Canvas
 */
@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Azul (#4285F4)
        drawPath(
            path = Path().apply {
                moveTo(w * 0.95f, h * 0.51f)
                cubicTo(w * 0.95f, h * 0.47f, w * 0.94f, h * 0.43f, w * 0.93f, h * 0.40f)
                lineTo(w * 0.50f, h * 0.50f)
                lineTo(w * 0.50f, h * 0.68f)
                lineTo(w * 0.76f, h * 0.68f)
                cubicTo(w * 0.75f, h * 0.74f, w * 0.71f, h * 0.79f, w * 0.65f, h * 0.83f)
                lineTo(w * 0.78f, h * 0.93f)
                cubicTo(w * 0.86f, h * 0.85f, w * 0.95f, h * 0.70f, w * 0.95f, h * 0.51f)
                close()
            },
            color = Color(0xFF4285F4)
        )

        // Verde (#34A853)
        drawPath(
            path = Path().apply {
                moveTo(w * 0.50f, h * 0.96f)
                cubicTo(w * 0.63f, h * 0.96f, w * 0.74f, h * 0.92f, w * 0.82f, h * 0.84f)
                lineTo(w * 0.69f, h * 0.74f)
                cubicTo(w * 0.65f, h * 0.77f, w * 0.58f, h * 0.79f, w * 0.50f, h * 0.79f)
                cubicTo(w * 0.38f, h * 0.79f, w * 0.28f, h * 0.71f, w * 0.24f, h * 0.60f)
                lineTo(w * 0.10f, h * 0.71f)
                cubicTo(w * 0.18f, h * 0.86f, w * 0.33f, h * 0.96f, w * 0.50f, h * 0.96f)
                close()
            },
            color = Color(0xFF34A853)
        )

        // Amarelo (#FBBC05)
        drawPath(
            path = Path().apply {
                moveTo(w * 0.24f, h * 0.60f)
                cubicTo(w * 0.23f, h * 0.57f, w * 0.22f, h * 0.53f, w * 0.22f, h * 0.50f)
                cubicTo(w * 0.22f, h * 0.47f, w * 0.23f, h * 0.43f, w * 0.24f, h * 0.40f)
                lineTo(w * 0.10f, h * 0.29f)
                cubicTo(w * 0.06f, h * 0.37f, w * 0.04f, h * 0.43f, w * 0.04f, h * 0.50f)
                cubicTo(w * 0.04f, h * 0.57f, w * 0.06f, h * 0.63f, w * 0.10f, h * 0.71f)
                lineTo(w * 0.24f, h * 0.60f)
                close()
            },
            color = Color(0xFFFBBC05)
        )

        // Vermelho (#EA4335)
        drawPath(
            path = Path().apply {
                moveTo(w * 0.50f, h * 0.21f)
                cubicTo(w * 0.57f, h * 0.21f, w * 0.64f, h * 0.24f, w * 0.69f, h * 0.28f)
                lineTo(w * 0.83f, h * 0.14f)
                cubicTo(w * 0.74f, h * 0.06f, w * 0.63f, h * 0.04f, w * 0.50f, h * 0.04f)
                cubicTo(w * 0.33f, h * 0.04f, w * 0.18f, h * 0.14f, w * 0.10f, h * 0.29f)
                lineTo(w * 0.24f, h * 0.40f)
                cubicTo(w * 0.28f, h * 0.29f, w * 0.38f, h * 0.21f, w * 0.50f, h * 0.21f)
                close()
            },
            color = Color(0xFFEA4335)
        )
    }
}
