@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package app.forgetaboutit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import app.forgetaboutit.R

/** Design D, "midpoint" dark palette. */
object P {
    val Bg = Color(0xFF17191C)
    val Card = Color(0xFF212428)
    val Chip = Color(0xFF2C3036)
    val Line = Color(0xFF31353B)
    val Bar = Color(0xFF1C1F22)
    val BarLine = Color(0xFF2A2E33)
    val Track = Color(0xFF34393E)
    val Check = Color(0xFF52585F)
    val Mute = Color(0xFF62686F)
    val Text = Color(0xFFF2F2EE)
    val Dim = Color(0xFFA0A6AC)
    val Accent = Color(0xFF9DB4FF)
    val OnAccent = Color(0xFF0F1420)
}

private fun manrope(weight: Int, w: FontWeight) = Font(
    R.font.manrope,
    weight = w,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Manrope = FontFamily(
    manrope(400, FontWeight.Normal),
    manrope(500, FontWeight.Medium),
    manrope(600, FontWeight.SemiBold),
    manrope(700, FontWeight.Bold),
    manrope(800, FontWeight.ExtraBold),
)

private fun appTypography(): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = Manrope),
        displayMedium = b.displayMedium.copy(fontFamily = Manrope),
        displaySmall = b.displaySmall.copy(fontFamily = Manrope),
        headlineLarge = b.headlineLarge.copy(fontFamily = Manrope),
        headlineMedium = b.headlineMedium.copy(fontFamily = Manrope),
        headlineSmall = b.headlineSmall.copy(fontFamily = Manrope),
        titleLarge = b.titleLarge.copy(fontFamily = Manrope),
        titleMedium = b.titleMedium.copy(fontFamily = Manrope),
        titleSmall = b.titleSmall.copy(fontFamily = Manrope),
        bodyLarge = b.bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = b.bodyMedium.copy(fontFamily = Manrope),
        bodySmall = b.bodySmall.copy(fontFamily = Manrope),
        labelLarge = b.labelLarge.copy(fontFamily = Manrope),
        labelMedium = b.labelMedium.copy(fontFamily = Manrope),
        labelSmall = b.labelSmall.copy(fontFamily = Manrope),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = P.Accent,
            onPrimary = P.OnAccent,
            primaryContainer = P.Accent,
            onPrimaryContainer = P.OnAccent,
            secondaryContainer = P.Chip,
            onSecondaryContainer = P.Text,
            background = P.Bg,
            onBackground = P.Text,
            surface = P.Card,
            onSurface = P.Text,
            surfaceVariant = P.Chip,
            onSurfaceVariant = P.Dim,
            surfaceContainerHigh = P.Card,
            surfaceContainerHighest = P.Chip,
            outline = P.Line,
        ),
        typography = appTypography(),
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Manrope, color = P.Text)) {
            content()
        }
    }
}
