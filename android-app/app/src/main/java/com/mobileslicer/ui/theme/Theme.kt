package com.mobileslicer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeModeOption(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark")
}

enum class AccentPaletteOption(val label: String) {
    Blue("Blue"),
    Cyan("Cyan"),
    Green("Green"),
    Yellow("Yellow"),
    Rose("Rose"),
    Red("Red"),
    Orange("Orange"),
    Graphite("Graphite")
}

enum class WorldViewColorOption(val label: String) {
    Slate("Slate"),
    White("White"),
    Mist("Mist"),
    Graphite("Graphite"),
    Deep("Deep"),
    Navy("Navy"),
    Charcoal("Charcoal"),
    Black("Black")
}

val LocalAppDarkTheme = staticCompositionLocalOf { true }

private fun accentPrimary(accentPalette: AccentPaletteOption) = when (accentPalette) {
    AccentPaletteOption.Blue -> BlueAccent
    AccentPaletteOption.Cyan -> CyanAccent
    AccentPaletteOption.Green -> GreenAccent
    AccentPaletteOption.Yellow -> YellowAccent
    AccentPaletteOption.Rose -> RoseAccent
    AccentPaletteOption.Red -> RedAccent
    AccentPaletteOption.Orange -> OrangeAccent
    AccentPaletteOption.Graphite -> GraphiteAccent
}

private fun accentPanel(accentPalette: AccentPaletteOption) = when (accentPalette) {
    AccentPaletteOption.Blue -> PanelBlue
    AccentPaletteOption.Cyan -> PanelCyan
    AccentPaletteOption.Green -> PanelGreen
    AccentPaletteOption.Yellow -> PanelYellow
    AccentPaletteOption.Rose -> PanelRose
    AccentPaletteOption.Red -> PanelRed
    AccentPaletteOption.Orange -> PanelOrange
    AccentPaletteOption.Graphite -> PanelGraphite
}

@Composable
fun MobileSlicerTheme(
    themeMode: ThemeModeOption = ThemeModeOption.System,
    accentPalette: AccentPaletteOption = AccentPaletteOption.Blue,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeModeOption.System -> isSystemInDarkTheme()
        ThemeModeOption.Light -> false
        ThemeModeOption.Dark -> true
    }
    val primary = accentPrimary(accentPalette)
    val secondary = accentPanel(accentPalette)
    val accentContent = if (darkTheme) Ink050 else Ink900
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            primaryContainer = Slate800,
            secondaryContainer = Slate800,
            tertiaryContainer = Slate800,
            onPrimary = accentContent,
            onSecondary = accentContent,
            onPrimaryContainer = Ink050,
            onSecondaryContainer = Ink050,
            onTertiaryContainer = Ink050,
            background = Slate950,
            surface = Slate900,
            surfaceVariant = Slate850,
            surfaceContainerLowest = Slate950,
            surfaceContainerLow = Slate900,
            surfaceContainer = Slate900,
            surfaceContainerHigh = Slate850,
            surfaceContainerHighest = Slate800,
            onSurface = Ink050,
            onSurfaceVariant = Slate300
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            primaryContainer = Slate200,
            secondaryContainer = Slate200,
            tertiaryContainer = Slate200,
            background = Slate100,
            surface = Color.White,
            surfaceVariant = Slate200,
            onPrimary = accentContent,
            onSecondary = accentContent,
            onPrimaryContainer = Ink900,
            onSecondaryContainer = Ink900,
            onTertiaryContainer = Ink900,
            onBackground = Ink900,
            onSurface = Ink900,
            onSurfaceVariant = Ink900,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Slate100,
            surfaceContainer = Slate100,
            surfaceContainerHigh = Slate200,
            surfaceContainerHighest = Slate300
        )
    }

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
            content()
        }
    }
}
