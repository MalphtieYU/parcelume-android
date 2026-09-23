package com.parcelinbox.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF171717)
val CanvasWhite = Color(0xFFFBFAFC)
val Lavender = Color(0xFFE8CFF5)
val LavenderSoft = Color(0xFFF4EAFB)
val Sky = Color(0xFFDCEBFF)
val Butter = Color(0xFFFFE8B8)
val Lime = Color(0xFFF2F4A8)
val Mint = Color(0xFFDDF3E8)
val Muted = Color(0xFF77727D)

@Composable
fun ParcelInboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ink,
            onPrimary = Color.White,
            primaryContainer = Lavender,
            onPrimaryContainer = Ink,
            secondary = Color(0xFF7452A4),
            background = CanvasWhite,
            onBackground = Ink,
            surface = Color.White,
            onSurface = Ink,
            surfaceVariant = Color(0xFFF3F1F4),
            onSurfaceVariant = Muted,
            error = Color(0xFFB3261E)
        ),
        content = content
    )
}
