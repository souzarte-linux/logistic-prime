package com.fernando.centraldomotorista.ui.screens.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.auth.AuthState
import com.fernando.centraldomotorista.auth.AuthViewModel
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
    val authState by authViewModel.authState.collectAsState()
    val googleAuthClient = GoogleAuthClient(context)

    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ícone / Logo do App em Destaque
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(OrangeNeon.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🚚",
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título Principal
            Text(
                text = "CENTRAL DO MOTORISTA",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo
            Text(
                text = "Controle financeiro e operacional para motoristas e entregadores de app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (authState) {
                is AuthState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = OrangeNeon,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "Autenticando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // Botão Oficial Google Sign-In
                    GoogleSignInButton(
                        onClick = {
                            authViewModel.clearError()
                            scope.launch {
                                when (val result = googleAuthClient.signIn()) {
                                    is SignInResult.Success -> {
                                        authViewModel.handleSignInSuccess(result.user)
                                    }
                                    is SignInResult.Error -> {
                                        authViewModel.handleSignInError(result.message)
                                    }
                                    is SignInResult.Cancelled -> {
                                        // Ação cancelada pelo usuário
                                    }
                                }
                            }
                        }
                    )

                    // Exibição de Mensagem de Erro
                    if (authState is AuthState.Error) {
                        val errorMessage = (authState as AuthState.Error).message
                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RedAlert.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = RedAlert,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .clickable { authViewModel.clearError() }
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Tentar novamente",
                                        tint = OrangeNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tentar novamente",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OrangeNeon
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Botão com o padrão visual oficial do Google Sign-In
 * (Fundo branco, ícone multicolorido do Google e texto oficial cinza escuro)
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, Color(0xFFDADCE0), RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            GoogleIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Entrar com Google",
                color = Color(0xFF1F1F1F),
                fontSize = 15.sp,
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
