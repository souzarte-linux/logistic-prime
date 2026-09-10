package com.fernando.centraldomotorista.ui.screens.deliverypartners

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fernando.centraldomotorista.ui.theme.*
import com.fernando.centraldomotorista.ui.utils.CurrencyVisualTransformation
import com.fernando.centraldomotorista.ui.utils.cleanCurrencyInput
import com.fernando.centraldomotorista.ui.utils.parseCurrency
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosePartnerSessionScreen(
    sessionId: String,
    viewModel: ClosePartnerSessionViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSessionClosed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR")) }

    LaunchedEffect(sessionId) {
        viewModel.loadData(sessionId)
    }

    LaunchedEffect(uiState.sessionFinalized) {
        if (uiState.sessionFinalized) {
            Toast.makeText(context, "Sessão finalizada e pagamento lançado com sucesso!", Toast.LENGTH_LONG).show()
            onSessionClosed()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    val session = uiState.session
    val partner = uiState.partner
    val route = uiState.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "FECHAR SESSÃO E PAGAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.finalizeAndPay() },
                        enabled = !uiState.isFinalizing && session != null && partner != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (uiState.isFinalizing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Finalizar e Pagar (R$ ${String.format(Locale("pt", "BR"), "%.2f", uiState.amountPaid)})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // 1. Resumo da Sessão em Andamento
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(OrangeNeon.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TwoWheeler,
                                            contentDescription = null,
                                            tint = OrangeNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = partner?.fullName ?: "Entregador",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = route?.name ?: "Sem Rota",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = OrangeNeon.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "EM ANDAMENTO",
                                        color = OrangeNeon,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Início da Sessão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val formattedStartTime = remember(session?.startTime) {
                                        session?.startTime?.atZoneSameInstant(ZoneId.systemDefault())?.format(timeFormatter) ?: "--:--"
                                    }
                                    Text(
                                        text = formattedStartTime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pacotes Expedidos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session?.expectedPackageCount ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Pacotes Bipados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${session?.scannedCount ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OrangeNeon
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Hora de Fim
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "HORA DE TÉRMINO",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val curTime = uiState.endTime.atZoneSameInstant(ZoneId.systemDefault())
                                    TimePickerDialog(
                                        context,
                                        { _, hour, min ->
                                            viewModel.onEndTimeChanged(hour, min)
                                        },
                                        curTime.hour,
                                        curTime.minute,
                                        true
                                    ).show()
                                },
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = OrangeNeon)
                                    Text(
                                        text = uiState.endTime.atZoneSameInstant(ZoneId.systemDefault()).format(timeFormatter),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text("Alterar", fontSize = 12.sp, color = OrangeNeon, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 3. Pacotes Entregues e Pacotes Devolvidos
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Pacotes Entregues
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PACOTES ENTREGUES",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.deliveredCountText,
                                onValueChange = { viewModel.onDeliveredCountChanged(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GreenNeon)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Pacotes Devolvidos
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PACOTES DEVOLVIDOS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = uiState.returnedCountText,
                                onValueChange = { viewModel.onReturnedCountChanged(it) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                leadingIcon = {
                                    Icon(Icons.Default.KeyboardReturn, contentDescription = null, tint = RedAlert)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RedAlert,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Alerta de Divergência se Entregues + Devolvidos != ScannedCount
                if (uiState.hasDivergence) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = RedAlert.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RedAlert.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Atenção: Contagem Divergente",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RedAlert
                                    )
                                    Text(
                                        text = "Total informado (${uiState.totalAccounted}) ≠ Expedidos (${uiState.basePackageCount}). Entregues + Devolvidos deve ser igual aos pacotes expedidos.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Cálculo Automático do Valor a Pagar
                item {
                    val rate = if ((session?.packageRate ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) session!!.packageRate else (partner?.packageRate ?: java.math.BigDecimal.ZERO)
                    val bonus = if ((session?.defaultBonus ?: java.math.BigDecimal.ZERO) > java.math.BigDecimal.ZERO) session!!.defaultBonus else (partner?.defaultBonus ?: java.math.BigDecimal.ZERO)
                    val delivered = uiState.deliveredCount
                    val subtotal = java.math.BigDecimal(delivered).multiply(rate)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GreenNeon.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CÁLCULO DO PAGAMENTO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = GreenNeon
                                )
                                Text(
                                    text = "Automático",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Detalhamento do cálculo
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$delivered pct × R$ ${String.format(Locale("pt", "BR"), "%.2f", rate)}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", subtotal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (bonus > java.math.BigDecimal.ZERO) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "+ Bônus padrão da rota",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", bonus)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GreenNeon
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Sugestão Calculada:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "R$ ${String.format(Locale("pt", "BR"), "%.2f", uiState.suggestedAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = GreenNeon
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            // Campo editável: Valor Total a Pagar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VALOR TOTAL A PAGAR (EDITÁVEL)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = uiState.amountPaidText,
                                    onValueChange = { input ->
                                        val clean = cleanCurrencyInput(input)
                                        val decimal = parseCurrency(clean)
                                        viewModel.onAmountPaidChanged(clean, decimal)
                                    },
                                    prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = GreenNeon) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = CurrencyVisualTransformation(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GreenNeon,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Este valor será lançado automaticamente como despesa na categoria 'equipe' e abaterá o Lucro Líquido de hoje.",
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
