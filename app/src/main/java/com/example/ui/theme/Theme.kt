package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PregonPrimaryDark,
    onPrimary = PregonOnPrimaryDark,
    primaryContainer = PregonPrimaryContainerDark,
    onPrimaryContainer = PregonOnPrimaryContainerDark,
    secondary = PregonSecondaryDark,
    onSecondary = PregonOnSecondaryDark,
    secondaryContainer = PregonSecondaryContainerDark,
    onSecondaryContainer = PregonOnSecondaryContainerDark,
    tertiary = PregonTertiaryDark,
    onTertiary = PregonOnTertiaryDark,
    background = PregonBackgroundDark,
    onBackground = PregonOnBackgroundDark,
    surface = PregonSurfaceDark,
    onSurface = PregonOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = PregonPrimary,
    onPrimary = PregonOnPrimary,
    primaryContainer = PregonPrimaryContainer,
    onPrimaryContainer = PregonOnPrimaryContainer,
    secondary = PregonSecondary,
    onSecondary = PregonOnSecondary,
    secondaryContainer = PregonSecondaryContainer,
    onSecondaryContainer = PregonOnSecondaryContainer,
    tertiary = PregonTertiary,
    onTertiary = PregonOnTertiary,
    background = PregonBackground,
    onBackground = PregonOnBackground,
    surface = PregonSurface,
    onSurface = PregonOnSurface,
    surfaceVariant = PregonSurfaceVariant,
    onSurfaceVariant = PregonOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinctive brand theme by default
    content: @Composable () -> Unit,
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
