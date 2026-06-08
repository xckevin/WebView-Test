package com.xckevin.android.app.webview.test.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = Blue700,
    secondary = Slate500,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate700,
    tertiary = Teal600,
    onTertiary = Color.White,
    tertiaryContainer = Teal50,
    onTertiaryContainer = Teal600,
    error = Red500,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red500,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    outlineVariant = Slate200,
    inverseSurface = Slate800,
    inverseOnSurface = Slate50,
    inversePrimary = Blue300,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = Blue900,
    primaryContainer = Blue900,
    onPrimaryContainer = Blue300,
    secondary = Slate400,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate300,
    tertiary = Teal300,
    onTertiary = Teal900,
    tertiaryContainer = Teal900,
    onTertiaryContainer = Teal300,
    error = Red300,
    onError = Red900,
    errorContainer = Red900,
    onErrorContainer = Red300,
    background = Slate850,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate700,
    inverseSurface = Slate100,
    inverseOnSurface = Slate900,
    inversePrimary = Blue600,
)

@Composable
fun WebViewTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
