package com.beautymanager.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta premium própria (não usamos Material You dinâmico de propósito: a marca precisa
 * ser consistente em qualquer aparelho, no espírito Notion/Stripe/Nubank — não a cor do
 * papel de parede do usuário). Tom principal: mauve/rosé profundo; acento: dourado suave.
 */
private val MauvePrimary = Color(0xFF8E3B57)
private val MauvePrimaryContainer = Color(0xFFFFD9E2)
private val MauveOnPrimaryContainer = Color(0xFF3B0018)
private val GoldSecondary = Color(0xFFB08968)
private val GoldSecondaryContainer = Color(0xFFF6E4D2)
private val DeepPlumTertiary = Color(0xFF5C4A72)

private val LightColorScheme = lightColorScheme(
    primary = MauvePrimary,
    onPrimary = Color.White,
    primaryContainer = MauvePrimaryContainer,
    onPrimaryContainer = MauveOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoldSecondaryContainer,
    tertiary = DeepPlumTertiary,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFF3E7EA),
    onBackground = Color(0xFF201A1B),
    onSurface = Color(0xFF201A1B),
    error = Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB1C3),
    onPrimary = Color(0xFF5E1133),
    primaryContainer = Color(0xFF74253F),
    onPrimaryContainer = MauvePrimaryContainer,
    secondary = Color(0xFFE0C1A8),
    onSecondary = Color(0xFF412C1B),
    secondaryContainer = Color(0xFF5A422F),
    tertiary = Color(0xFFC9B7DE),
    background = Color(0xFF1C1517),
    surface = Color(0xFF1C1517),
    surfaceVariant = Color(0xFF4D4143),
    onBackground = Color(0xFFECE0E1),
    onSurface = Color(0xFFECE0E1),
    error = Color(0xFFFFB4AB)
)

@Composable
fun BeautyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BeautyManagerTypography,
        content = content
    )
}
