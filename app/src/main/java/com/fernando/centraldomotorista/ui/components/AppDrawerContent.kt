package com.fernando.centraldomotorista.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fernando.centraldomotorista.data.model.Profile
import com.fernando.centraldomotorista.navigation.Screen
import com.fernando.centraldomotorista.ui.theme.OrangeNeon
import com.fernando.centraldomotorista.ui.theme.RedAlert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Conteúdo do Menu Lateral (Modal Navigation Drawer) da aplicação.
 * Acessível a partir de todas as abas principais através do ícone hambúrguer ou gesto de deslizar da borda esquerda.
 */
@Composable
fun AppDrawerContent(
    profile: Profile?,
    currentRoute: String?,
    isDarkMode: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onNavigateToRoute: (String) -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCadastroExpanded by rememberSaveable { mutableStateOf(true) }
    var isFinanceiroExpanded by rememberSaveable { mutableStateOf(true) }

    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Cabeçalho do Usuário com Avatar Real e Boas-Vindas Contextuais
            val currentHour = remember { java.time.LocalTime.now().hour }
            val saudacao = when (currentHour) {
                in 5..11 -> "Bom dia"
                in 12..17 -> "Boa tarde"
                else -> "Boa noite"
            }
            val driverFullName = profile?.fullName?.trim() ?: "Motorista"
            val firstName = driverFullName.split("\\s+".toRegex()).firstOrNull() ?: "Motorista"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DriverAvatar(
                    avatarUrl = profile?.avatarUrl,
                    name = profile?.fullName,
                    modifier = Modifier.size(52.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "$saudacao, $firstName! 👋",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile?.email ?: "Logística & Entregas",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = OrangeNeon.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        val vehicleType = profile?.vehicle?.replaceFirstChar { it.uppercase() } ?: "Moto"
                        Text(
                            text = "$vehicleType • Parceiro",
                            color = OrangeNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Início
            val isInicioSelected = currentRoute == Screen.Inicio.route || currentRoute == null
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Home, contentDescription = null, tint = OrangeNeon) },
                label = { Text("Início", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                selected = isInicioSelected,
                onClick = {
                    onNavigateToRoute(Screen.Inicio.route)
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = OrangeNeon.copy(alpha = 0.15f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // 3. Seção CADASTRO (Expansível / Colapsável, expandida por padrão)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isCadastroExpanded = !isCadastroExpanded },
                color = OrangeNeon.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangeNeon.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Cadastro",
                            tint = OrangeNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "CADASTRO",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Empresas, Postos, Rotas...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = if (isCadastroExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCadastroExpanded) "Recolher" else "Expandir",
                        tint = OrangeNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Itens da Seção Cadastro
            AnimatedVisibility(
                visible = isCadastroExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 🏢 Empresas
                    DrawerCadastroItem(
                        icon = Icons.Default.Business,
                        title = "Empresas",
                        subtitle = "Prestadoras de Serviços",
                        onClick = { onNavigateToRoute("empresas") }
                    )

                    // ⛽ Postos de Gasolina
                    DrawerCadastroItem(
                        icon = Icons.Default.LocalGasStation,
                        title = "Postos de Gasolina",
                        subtitle = "Postos e abastecimento",
                        onClick = { onNavigateToRoute("postos") }
                    )

                    // 📱 App & Plataforma
                    DrawerCadastroItem(
                        icon = Icons.Default.Apps,
                        title = "App & Plataforma",
                        subtitle = "Plataformas de entrega e repasse",
                        onClick = { onNavigateToRoute("plataformas?fromDrawer=true") }
                    )

                    // 🛣️ Rotas
                    DrawerCadastroItem(
                        icon = Icons.AutoMirrored.Filled.AltRoute,
                        title = "Rotas",
                        subtitle = "Rotas de entrega cadastradas",
                        onClick = { onNavigateToRoute("delivery_routes") }
                    )

                    // 👤 Novo Entregador
                    DrawerCadastroItem(
                        icon = Icons.Default.PersonAdd,
                        title = "Novo Entregador",
                        subtitle = "Cadastrar novo entregador",
                        onClick = { onNavigateToRoute("create_delivery_partner") }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // 4. Seção FINANCEIRO & OPERACIONAL (Expansível / Colapsável, expandida por padrão)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isFinanceiroExpanded = !isFinanceiroExpanded },
                color = OrangeNeon.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangeNeon.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Financeiro & Operacional",
                            tint = OrangeNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "FINANCEIRO & OPERACIONAL",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = OrangeNeon
                        )
                        Text(
                            text = "Cartões, Parceiros, Peças...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = if (isFinanceiroExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isFinanceiroExpanded) "Recolher" else "Expandir",
                        tint = OrangeNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Itens da Seção Financeiro & Operacional
            AnimatedVisibility(
                visible = isFinanceiroExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 6.dp, end = 6.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 💳 Gerenciamento de Cartões
                    DrawerCadastroItem(
                        icon = Icons.Default.CreditCard,
                        title = "Gerenciamento de Cartões",
                        subtitle = "Cartões, emissores e bandeiras",
                        onClick = { onNavigateToRoute("credit_cards") }
                    )

                    // 💰 Contas a Receber / Faturas
                    DrawerCadastroItem(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = "Contas a Receber",
                        subtitle = "Faturas e ciclos de pagamento",
                        onClick = { onNavigateToRoute("faturas") }
                    )

                    // 🛵 Entregadores Parceiros
                    DrawerCadastroItem(
                        icon = Icons.Default.TwoWheeler,
                        title = "Entregadores Parceiros",
                        subtitle = "Cadastro e gestão de motoristas",
                        onClick = { onNavigateToRoute("delivery_partners") }
                    )

                    // 🔧 Monitoramento Peças
                    DrawerCadastroItem(
                        icon = Icons.Default.Build,
                        title = "Monitoramento Peças",
                        subtitle = "Controle de trocas e manutenção",
                        onClick = { onNavigateToRoute("monitoramento-pecas") }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // 5. Relatórios & Extrato
            val isRelatoriosSelected = currentRoute == Screen.Relatorios.route
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Assessment, contentDescription = null, tint = if (isRelatoriosSelected) OrangeNeon else MaterialTheme.colorScheme.onSurfaceVariant) },
                label = { Text("Relatórios & Extrato", color = MaterialTheme.colorScheme.onSurface, fontWeight = if (isRelatoriosSelected) FontWeight.Bold else FontWeight.Normal) },
                selected = isRelatoriosSelected,
                onClick = { onNavigateToRoute(Screen.Relatorios.route) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = OrangeNeon.copy(alpha = 0.15f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))

            // 6. Alternador Modo Escuro / Claro
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = "Modo Escuro",
                        tint = OrangeNeon
                    )
                },
                label = {
                    Text(
                        text = "Modo Escuro",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                badge = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onThemeToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = OrangeNeon,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                },
                selected = false,
                onClick = { onThemeToggle(!isDarkMode) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 7. Sair da Conta
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = RedAlert) },
                label = { Text("Sair da Conta", color = RedAlert, fontWeight = FontWeight.Bold) },
                selected = false,
                onClick = onSignOutClick,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DrawerCadastroItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangeNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DriverAvatar(
    avatarUrl: String?,
    name: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(avatarUrl) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(avatarUrl) { mutableStateOf(false) }

    LaunchedEffect(avatarUrl) {
        if (!avatarUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL(avatarUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    val stream = connection.getInputStream()
                    val decoded = BitmapFactory.decodeStream(stream)
                    withContext(Dispatchers.Main) {
                        bitmap = decoded
                        loadFailed = decoded == null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        loadFailed = true
                    }
                }
            }
        }
    }

    val initials = remember(name) {
        if (name.isNullOrBlank()) "M"
        else {
            val parts = name.trim().split("\\s+".toRegex())
            if (parts.size >= 2) {
                "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
            } else {
                parts[0].take(2).uppercase()
            }
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(OrangeNeon, Color(0xFFFF5722))
                )
            )
            .border(2.dp, OrangeNeon.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !loadFailed) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Foto do motorista",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }
    }
}
