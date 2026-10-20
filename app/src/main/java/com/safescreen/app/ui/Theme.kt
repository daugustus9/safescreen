package com.safescreen.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1E5BB8)
private val BlueLight = Color(0xFFAFC6FF)
private val HelpRed = Color(0xFFB3261E)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Color(0xFF2E7D32),
    error = HelpRed,
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    secondary = Color(0xFF81C784),
    error = Color(0xFFFFB4AB),
)

/** Fixed brand colors (no dynamic color) so cards, buttons and help read the same on every phone. */
@Composable
fun SafeScreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
