package com.mobileslicer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mobileslicer.ui.theme.LocalAppDarkTheme

internal data class AppSettingOption<T>(
    val value: T,
    val title: String,
    val subtitle: String,
    val swatchColor: Color? = null
)

@Composable
internal fun appBackgroundGradient(): List<Color> {
    val darkTheme = LocalAppDarkTheme.current
    return if (darkTheme) {
        listOf(Color(0xFF090C13), Color(0xFF121926), Color(0xFF0A0D14))
    } else {
        listOf(Color(0xFFFFFFFF), Color(0xFFF4F7FB), Color(0xFFE7EEF7))
    }
}

@Composable
internal fun appCardColor(): Color = MaterialTheme.colorScheme.surface

@Composable
internal fun appCardColorMuted(): Color =
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (LocalAppDarkTheme.current) 0.72f else 0.9f)

@Composable
internal fun appTitleColor(): Color = MaterialTheme.colorScheme.onBackground

@Composable
internal fun appBodyColor(): Color =
    MaterialTheme.colorScheme.onBackground.copy(alpha = if (LocalAppDarkTheme.current) 0.78f else 0.72f)

@Composable
internal fun appMutedColor(): Color =
    MaterialTheme.colorScheme.onBackground.copy(alpha = if (LocalAppDarkTheme.current) 0.62f else 0.58f)

@Composable
internal fun appOutlineColor(): Color =
    MaterialTheme.colorScheme.outline.copy(alpha = if (LocalAppDarkTheme.current) 0.65f else 0.5f)
