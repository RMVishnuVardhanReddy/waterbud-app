package com.waterbud.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.waterbud.app.data.ThemeMode

private val Aqua = Color(0xFF2E9BDE)
private val AquaDark = Color(0xFF1C6FA6)
private val Teal = Color(0xFF26C6DA)

private val LightColors = lightColorScheme(
    primary = Aqua, onPrimary = Color.White,
    secondary = Teal, background = Color(0xFFF4FAFE),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FC3F0), onPrimary = Color(0xFF00344F),
    secondary = Teal, background = Color(0xFF0E1621),
    surface = Color(0xFF16222E)
)

/** Pure-black for AMOLED screens — a popular premium perk. */
private val AmoledColors = DarkColors.copy(
    background = Color.Black, surface = Color(0xFF0A0A0A)
)

@Composable
fun WaterbudTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        else -> true
    }
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val ctx = LocalContext.current

    val colors = when {
        themeMode == ThemeMode.AMOLED -> AmoledColors
        dynamicAvailable && themeMode == ThemeMode.SYSTEM ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}
