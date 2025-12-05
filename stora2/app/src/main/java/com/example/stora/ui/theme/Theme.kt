package com.example.stora.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = StoraBlueLight,
    secondary = StoraYellow,
    tertiary = StoraBlue,
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = StoraBlueDark,
    onSecondary = StoraBlueDark,
    onTertiary = StoraWhite,
    onBackground = StoraWhite,
    onSurface = StoraWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = StoraBlueDark,
    secondary = StoraBlue,
    tertiary = StoraYellow,
    background = StoraBlueDark,
    surface = StoraWhite,
    onPrimary = StoraWhite,
    onSecondary = StoraWhite,
    onTertiary = StoraBlueDark,
    onBackground = StoraWhite,
    onSurface = StoraBlueDark
)

@Composable
fun StoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),

    dynamicColor: Boolean = true,
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

@Composable
fun STORATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = StoraBlueDark.toArgb()

            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}