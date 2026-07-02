package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = CyberTeal,
    tertiary = AlertOrange,
    background = SlateDark,
    surface = SlateMedium,
    onBackground = TextWhite,
    onSurface = TextLightGray,
    error = AlertRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
