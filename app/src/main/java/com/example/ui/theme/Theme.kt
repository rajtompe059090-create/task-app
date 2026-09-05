package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF004690),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = VibrantPurpleContainer,
    onSecondary = OnVibrantPurple,
    tertiary = VibrantCoralContainer,
    onTertiary = OnVibrantCoral,
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF191C22),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF242832),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantBlue,
    onPrimary = Color.White,
    primaryContainer = VibrantBlueContainer,
    onPrimaryContainer = OnVibrantBlueContainer,
    secondary = VibrantGreen,
    onSecondary = Color.White,
    secondaryContainer = VibrantGreenContainer,
    onSecondaryContainer = OnVibrantGreen,
    tertiary = VibrantAmber,
    onTertiary = Color.White,
    tertiaryContainer = VibrantAmberContainer,
    onTertiaryContainer = OnVibrantAmber,
    background = VibrantBg,
    onBackground = VibrantTextPrimary,
    surface = VibrantSurface,
    onSurface = VibrantTextPrimary,
    surfaceVariant = VibrantSurfaceVariant,
    onSurfaceVariant = VibrantTextSecondary,
    outline = VibrantBorder,
    outlineVariant = VibrantBorderSubtle,
    error = VibrantError,
    errorContainer = VibrantErrorContainer,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
