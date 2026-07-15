package com.notificationbox.app.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF4E616E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E5F4),
    onSecondaryContainer = Color(0xFF0A1E29),
    tertiary = Color(0xFF625A7C),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
    surfaceVariant = Color(0xFFDDE3E9),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DCDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFFB5C9D7),
    onSecondary = Color(0xFF20333E),
    secondaryContainer = Color(0xFF374A56),
    onSecondaryContainer = Color(0xFFD1E5F4),
    tertiary = Color(0xFFCCC1E9),
    background = Color(0xFF101417),
    surface = Color(0xFF101417),
    surfaceVariant = Color(0xFF41484D),
    error = Color(0xFFFFB4AB)
)

@Composable
fun NotificationBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
