package com.quietreader.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val Paper = Color(0xFFF4F2EC)
private val Ink = Color(0xFF2A2A28)
private val Muted = Color(0xFF77756F)
private val Line = Color(0xFFD9D6CD)
private val Surface = Color(0xFFFBFAF6)
private val Night = Color(0xFF20201E)
private val NightInk = Color(0xFFE7E2D8)
private val Accent = Color(0xFFB63A31)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Surface,
    primaryContainer = Line,
    onPrimaryContainer = Ink,
    secondary = Muted,
    onSecondary = Surface,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Line,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Accent,
)

private val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = Night,
    primaryContainer = Color(0xFF3B3934),
    onPrimaryContainer = NightInk,
    secondary = Color(0xFFBDB8AD),
    onSecondary = Night,
    background = Night,
    onBackground = NightInk,
    surface = Color(0xFF292825),
    onSurface = NightInk,
    surfaceVariant = Color(0xFF45423C),
    onSurfaceVariant = Color(0xFFBDB8AD),
    outline = Color(0xFF5A564E),
    error = Color(0xFFE17D70),
)

private val ReaderTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        lineHeight = 31.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun QuietReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ReaderTypography,
        content = content,
    )
}
