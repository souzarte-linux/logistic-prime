package com.fernando.centraldomotorista.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// O app suporta os dois modos. A cor de destaque (laranja) e as cores
// semânticas (verde/vermelho/azul/dourado) são as mesmas nos dois temas —
// só o fundo, as superfícies e o texto trocam de tom.

private val DarkColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDarkAlt,
    primary = OrangeNeon,
    onPrimary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    secondary = GreenNeon,
    error = RedAlert,
)

private val LightColorScheme = lightColorScheme(
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceLightAlt,
    primary = OrangeNeon,
    onPrimary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    secondary = GreenNeon,
    error = RedAlert,
)

/** Cor da bottom navigation bar, que tem um tom próprio em cada modo. */
@Composable
fun bottomNavColor(darkTheme: Boolean = isSystemInDarkTheme()): androidx.compose.ui.graphics.Color =
    if (darkTheme) BottomNavDark else BottomNavLight

/** Cor de texto secundário do tema atual (usada em ícones/labels não selecionados). */
@Composable
fun secondaryTextColor(darkTheme: Boolean = isSystemInDarkTheme()): androidx.compose.ui.graphics.Color =
    if (darkTheme) TextSecondaryDark else TextSecondaryLight

// Cantos arredondados usados nos cards em todas as telas (INÍCIO, PAINEL, etc.)
val CentralDoMotoristaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

// A tipografia não fixa cor própria — ela herda a cor de texto correta
// de cada tema (clara ou escura) através do MaterialTheme.colorScheme,
// então os Composables devem usar `MaterialTheme.colorScheme.onBackground`
// / `onSurfaceVariant` em vez de referenciar TextPrimaryDark etc. diretamente.
val CentralDoMotoristaTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
    ),
)

/**
 * Tema raiz do app. Por padrão segue o modo do sistema (claro/escuro),
 * mas aceita um valor manual — por exemplo, se você adicionar um botão
 * de "alternar tema" nas configurações do app.
 */
@Composable
fun CentralDoMotoristaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = CentralDoMotoristaShapes,
        typography = CentralDoMotoristaTypography,
        content = content,
    )
}
