package com.mobileslicer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mobileslicer.ui.theme.AccentPaletteOption
import com.mobileslicer.ui.theme.LocalAppDarkTheme
import com.mobileslicer.ui.theme.PanelBlue
import com.mobileslicer.ui.theme.PanelCyan
import com.mobileslicer.ui.theme.PanelGreen
import com.mobileslicer.ui.theme.PanelGraphite
import com.mobileslicer.ui.theme.PanelOrange
import com.mobileslicer.ui.theme.PanelRed
import com.mobileslicer.ui.theme.PanelRose
import com.mobileslicer.ui.theme.PanelYellow
import com.mobileslicer.ui.theme.ThemeModeOption
import com.mobileslicer.ui.theme.WorldViewColorOption
import com.mobileslicer.viewer.GcodePreviewPerformanceMode

@Composable
internal fun SettingsScreen(
    appVersion: String,
    appPackageName: String,
    themeMode: ThemeModeOption,
    accentPalette: AccentPaletteOption,
    worldViewColor: WorldViewColorOption,
    showAdvancedProfileSettings: Boolean,
    gcodePreviewPerformanceMode: GcodePreviewPerformanceMode,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    onAccentPaletteSelected: (AccentPaletteOption) -> Unit,
    onWorldViewColorSelected: (WorldViewColorOption) -> Unit,
    onShowAdvancedProfileSettingsChanged: (Boolean) -> Unit,
    onGcodePreviewPerformanceModeSelected: (GcodePreviewPerformanceMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    val outlineColor = appOutlineColor()
    var selectedTab by rememberSaveable { mutableStateOf(SettingsTab.Appearance) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = appBackgroundGradient()
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(appCardColorMuted())
                    .border(1.dp, outlineColor, RoundedCornerShape(18.dp))
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = titleColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Appearance, advanced controls, project info, and support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor
                )
            }
        }
        SettingsTabStrip(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(top = 16.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (selectedTab) {
                SettingsTab.Appearance -> AppearanceSettingsSection(
                    themeMode = themeMode,
                    accentPalette = accentPalette,
                    worldViewColor = worldViewColor,
                    onThemeModeSelected = onThemeModeSelected,
                    onAccentPaletteSelected = onAccentPaletteSelected,
                    onWorldViewColorSelected = onWorldViewColorSelected
                )
                SettingsTab.Advanced -> AdvancedSettingsSection(
                    showAdvancedProfileSettings = showAdvancedProfileSettings,
                    gcodePreviewPerformanceMode = gcodePreviewPerformanceMode,
                    onShowAdvancedProfileSettingsChanged = onShowAdvancedProfileSettingsChanged,
                    onGcodePreviewPerformanceModeSelected = onGcodePreviewPerformanceModeSelected
                )
                SettingsTab.Info -> InfoSettingsSection(
                    appVersion = appVersion,
                    appPackageName = appPackageName,
                    outlineColor = outlineColor
                )
                SettingsTab.Support -> SupportSettingsSection()
            }
        }
    }
}

private enum class SettingsTab(val title: String) {
    Appearance("Appearance"),
    Advanced("Advanced"),
    Info("Info"),
    Support("Support")
}

@Composable
private fun SettingsTabStrip(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalAppDarkTheme.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (darkTheme) 0.14f else 0.1f)
                        } else {
                            appCardColor()
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f) else appOutlineColor(),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(onClick = { onTabSelected(tab) })
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) appTitleColor() else appMutedColor(),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    themeMode: ThemeModeOption,
    accentPalette: AccentPaletteOption,
    worldViewColor: WorldViewColorOption,
    onThemeModeSelected: (ThemeModeOption) -> Unit,
    onAccentPaletteSelected: (AccentPaletteOption) -> Unit,
    onWorldViewColorSelected: (WorldViewColorOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsSectionCard(
            title = "Theme mode",
            subtitle = "Choose how the app shell looks on this device right now."
        ) {
            SettingsOptionRow(
                options = listOf(
                    AppSettingOption(ThemeModeOption.System, "System", "Follow the device theme."),
                    AppSettingOption(ThemeModeOption.Light, "Light", "Bright surface treatment for the shell."),
                    AppSettingOption(ThemeModeOption.Dark, "Dark", "Dark card-first presentation.")
                ),
                selectedValue = themeMode,
                accentColor = selectedAccentColor(accentPalette),
                onSelected = onThemeModeSelected
            )
        }

        SettingsSectionCard(
            title = "Accent color",
            subtitle = "Use a curated accent palette for the app shell."
        ) {
            SettingsOptionRow(
                options = accentPaletteOptions(),
                selectedValue = accentPalette,
                accentColor = selectedAccentColor(accentPalette),
                onSelected = onAccentPaletteSelected
            )
        }

        SettingsSectionCard(
            title = "World view color",
            subtitle = "Choose the 3D world space behind the bed and model."
        ) {
            SettingsOptionRow(
                options = worldViewColorOptions(),
                selectedValue = worldViewColor,
                accentColor = selectedWorldColor(worldViewColor),
                onSelected = onWorldViewColorSelected
            )
        }
    }
}

@Composable
private fun AdvancedSettingsSection(
    showAdvancedProfileSettings: Boolean,
    gcodePreviewPerformanceMode: GcodePreviewPerformanceMode,
    onShowAdvancedProfileSettingsChanged: (Boolean) -> Unit,
    onGcodePreviewPerformanceModeSelected: (GcodePreviewPerformanceMode) -> Unit
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsSectionCard(
            title = "Advanced Orca profile settings",
            subtitle = "Hide deeper Orca-derived controls by default in the mobile editor."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = showAdvancedProfileSettings,
                        onCheckedChange = onShowAdvancedProfileSettingsChanged,
                        enabled = true
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show advanced profile settings",
                            style = MaterialTheme.typography.titleSmall,
                            color = titleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Off by default. When enabled, advanced-tagged slicer controls become visible in profile editors.",
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor
                        )
                    }
                }
                SettingsPill(label = if (showAdvancedProfileSettings) "Enabled" else "Off by default")
            }
        }

        SettingsSectionCard(
            title = "GCODE Preview Performance",
            subtitle = "Sets the exact G-code mesh preview budget. Lower values create more automatic layer chunks without reducing preview accuracy."
        ) {
            SettingsOptionRow(
                options = GcodePreviewPerformanceMode.entries.map { mode ->
                    AppSettingOption(
                        value = mode,
                        title = "${mode.displayLabel}: ${formatPreviewVertexBudget(mode.vertexBudget)}",
                        subtitle = mode.description
                    )
                },
                selectedValue = gcodePreviewPerformanceMode,
                accentColor = MaterialTheme.colorScheme.primary,
                onSelected = onGcodePreviewPerformanceModeSelected
            )
        }
    }
}

private fun formatPreviewVertexBudget(vertexBudget: Long): String =
    when (vertexBudget) {
        1_000_000L -> "1m"
        else -> "${vertexBudget / 1_000}k"
    }

@Composable
private fun InfoSettingsSection(
    appVersion: String,
    appPackageName: String,
    outlineColor: Color
) {
    SettingsSectionCard(
        title = "Project info",
        subtitle = "Product and build context for this Android app."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LegalInfoRow(
                title = "App info",
                value = "Mobile Slicer $appVersion\nPackage: $appPackageName"
            )
            HorizontalDivider(color = outlineColor)
            LegalInfoRow(
                title = "Current scope",
                value = "App shell, Profiles flow, and the proven import/load/slice/export/share path."
            )
        }
    }

    SettingsSectionCard(
        title = "Legal",
        subtitle = "Project and slicer legal context."
    ) {
        LegalInfoRow(
            title = "About / legal",
            value = "OrcaSlicer remains the source of truth for slicer concepts and terminology. This Settings screen is Android app-layer only and does not change the JNI or wrapper contract."
        )
    }
}

@Composable
private fun SupportSettingsSection() {
    val uriHandler = LocalUriHandler.current
    val bodyColor = appBodyColor()
    SettingsSectionCard(
        title = "Support MobileSlicer.",
        subtitle = ""
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "MobileSlicer is committed to always being free, open source, and forever ad free.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor
                )
                Text(
                    text = "I will never add features that ruin the user experience or charge for certain features, ever.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor
                )
                Text(
                    text = "Supporting is only optional if you really support the project and want to help develop it. All support goes 100% to project development, including testing on a wider range of phones and tablets, buying development equipment, and being able to invest more time into the project. I will never ask anyone to support this project through financial incentive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        uriHandler.openUri("https://ko-fi.com/mobileslicer")
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ko-fi",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF08111E),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Support MobileSlicer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF08111E),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Opens ko-fi.com",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1A3557),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                text = "Support is optional and never unlocks app features.",
                style = MaterialTheme.typography.bodySmall,
                color = bodyColor
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = appCardColorMuted())
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = bodyColor
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun <T> SettingsOptionRow(
    options: List<AppSettingOption<T>>,
    selectedValue: T,
    accentColor: Color,
    onSelected: (T) -> Unit
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    val darkTheme = LocalAppDarkTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = option.value == selectedValue,
                        onClick = { onSelected(option.value) }
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (option.value == selectedValue) {
                        MaterialTheme.colorScheme.primary.copy(alpha = if (darkTheme) 0.18f else 0.12f)
                    } else {
                        appCardColor()
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(option.swatchColor ?: if (option.value == selectedValue) accentColor else Color(0xFF5D6C80))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = titleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor
                        )
                    }
                    SettingsPill(label = if (option.value == selectedValue) "Selected" else "Tap to use")
                }
            }
        }
    }
}

@Composable
private fun SettingsPill(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = appBodyColor(),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(appCardColor().copy(alpha = 0.36f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun LegalInfoRow(
    title: String,
    value: String
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = titleColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = bodyColor
        )
    }
}

private fun selectedAccentColor(accentPalette: AccentPaletteOption): Color =
    when (accentPalette) {
        AccentPaletteOption.Blue -> PanelBlue
        AccentPaletteOption.Cyan -> PanelCyan
        AccentPaletteOption.Green -> PanelGreen
        AccentPaletteOption.Yellow -> PanelYellow
        AccentPaletteOption.Rose -> PanelRose
        AccentPaletteOption.Red -> PanelRed
        AccentPaletteOption.Orange -> PanelOrange
        AccentPaletteOption.Graphite -> PanelGraphite
    }

private fun accentPaletteOptions(): List<AppSettingOption<AccentPaletteOption>> =
    listOf(
        AppSettingOption(AccentPaletteOption.Blue, "Blue", "Clean product blue.", PanelBlue),
        AppSettingOption(AccentPaletteOption.Cyan, "Cyan", "Sharp technical accent.", PanelCyan),
        AppSettingOption(AccentPaletteOption.Green, "Green", "Classic utility green.", PanelGreen),
        AppSettingOption(AccentPaletteOption.Yellow, "Yellow", "Bright caution-style accent.", PanelYellow),
        AppSettingOption(AccentPaletteOption.Rose, "Rose", "Soft magenta accent.", PanelRose),
        AppSettingOption(AccentPaletteOption.Red, "Red", "Direct high-visibility accent.", PanelRed),
        AppSettingOption(AccentPaletteOption.Orange, "Orange", "Warm practical accent.", PanelOrange),
        AppSettingOption(AccentPaletteOption.Graphite, "Graphite", "Neutral tool accent.", PanelGraphite)
    )

private fun selectedWorldColor(worldViewColor: WorldViewColorOption): Color =
    when (worldViewColor) {
        WorldViewColorOption.White -> Color(0xFFF3F7FC)
        WorldViewColorOption.Mist -> Color(0xFFDCE5EE)
        WorldViewColorOption.Slate -> Color(0xFF8E9AA6)
        WorldViewColorOption.Graphite -> Color(0xFF3F4852)
        WorldViewColorOption.Deep -> Color(0xFF071426)
        WorldViewColorOption.Navy -> Color(0xFF10233A)
        WorldViewColorOption.Charcoal -> Color(0xFF171B20)
        WorldViewColorOption.Black -> Color(0xFF020407)
    }

private fun worldViewColorOptions(): List<AppSettingOption<WorldViewColorOption>> =
    listOf(
        AppSettingOption(WorldViewColorOption.Slate, "Slate", "Default neutral gray-blue workspace.", selectedWorldColor(WorldViewColorOption.Slate)),
        AppSettingOption(WorldViewColorOption.White, "White", "Clean bright 3D workspace.", selectedWorldColor(WorldViewColorOption.White)),
        AppSettingOption(WorldViewColorOption.Mist, "Mist", "Soft light gray-blue workspace.", selectedWorldColor(WorldViewColorOption.Mist)),
        AppSettingOption(WorldViewColorOption.Graphite, "Graphite", "Dark neutral workspace.", selectedWorldColor(WorldViewColorOption.Graphite)),
        AppSettingOption(WorldViewColorOption.Deep, "Deep", "Deep blue world space for high contrast.", selectedWorldColor(WorldViewColorOption.Deep)),
        AppSettingOption(WorldViewColorOption.Navy, "Navy", "Readable dark blue workspace.", selectedWorldColor(WorldViewColorOption.Navy)),
        AppSettingOption(WorldViewColorOption.Charcoal, "Charcoal", "Classic dark workspace.", selectedWorldColor(WorldViewColorOption.Charcoal)),
        AppSettingOption(WorldViewColorOption.Black, "Black", "Near-black 3D workspace.", selectedWorldColor(WorldViewColorOption.Black))
    )
