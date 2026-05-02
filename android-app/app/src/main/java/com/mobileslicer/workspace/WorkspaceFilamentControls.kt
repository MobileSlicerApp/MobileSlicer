package com.mobileslicer.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobileslicer.appCardColor
import com.mobileslicer.appCardColorMuted
import com.mobileslicer.appOutlineColor
import com.mobileslicer.appTitleColor
import com.mobileslicer.profiles.FilamentProfile
import com.mobileslicer.profiles.PrinterProfile
import java.util.Locale

@Composable
internal fun WorkspaceFilamentStrip(
    slots: List<PlateFilamentSlot>,
    selectedSlotIndex: Int,
    onSlotClick: (Int) -> Unit,
    onAddSlot: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        slots.sortedBy { it.index }.forEach { slot ->
            FilamentSlotPill(
                slot = slot,
                selected = slot.index == selectedSlotIndex,
                onClick = { onSlotClick(slot.index) }
            )
        }
        Button(
            onClick = onAddSlot,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("+")
        }
    }
}

@Composable
private fun FilamentSlotPill(
    slot: PlateFilamentSlot,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) appCardColor().copy(alpha = 0.92f) else appCardColorMuted().copy(alpha = 0.86f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else appOutlineColor().copy(alpha = 0.52f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slot.index.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = appTitleColor(),
                fontWeight = FontWeight.SemiBold
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(slotColor(slot.colorHex))
                    .border(1.dp, appOutlineColor().copy(alpha = 0.48f), RoundedCornerShape(5.dp))
            )
            Text(
                text = slot.label.ifBlank { slot.materialType.ifBlank { "Filament" } },
                style = MaterialTheme.typography.labelMedium,
                color = appTitleColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            slot.physicalNozzleIndex?.let { nozzleIndex ->
                Text(
                    text = "N$nozzleIndex",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilamentSlotSheet(
    slot: PlateFilamentSlot,
    selectedObjectLabel: String?,
    availableFilaments: List<FilamentProfile>,
    physicalNozzleCount: Int,
    onAssignToSelected: () -> Unit,
    onColorSelected: (String) -> Unit,
    onFilamentSelected: (FilamentProfile) -> Unit,
    onNozzleSelected: (Int?) -> Unit,
    onRemoveSlot: () -> Unit,
    onDismiss: () -> Unit
) {
    var sheetPage by remember(slot.index) { mutableStateOf(FilamentSheetPage.Main) }
    var customHue by remember(slot.index) { mutableFloatStateOf(210f) }
    var customSaturation by remember(slot.index) { mutableFloatStateOf(0.86f) }
    var customValue by remember(slot.index) { mutableFloatStateOf(0.95f) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorChoices = listOf(
        "#F44336", "#FF5722", "#FF9800", "#FFC107", "#FFEB3B", "#8BC34A",
        "#4CAF50", "#009688", "#00BCD4", "#03A9F4", "#2196F3", "#3F51B5",
        "#9C27B0", "#E91E63", "#795548", "#FFFFFF"
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.56f)
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (sheetPage) {
                FilamentSheetPage.Main -> FilamentSlotMainPage(
                    slot = slot,
                    selectedObjectLabel = selectedObjectLabel,
                    colorChoices = colorChoices,
                    physicalNozzleCount = physicalNozzleCount,
                    onAssignToSelected = onAssignToSelected,
                    onColorSelected = onColorSelected,
                    onNozzleSelected = onNozzleSelected,
                    onRemoveSlot = onRemoveSlot,
                    onProfilePageRequested = { sheetPage = FilamentSheetPage.Profile },
                    onCustomColorPageRequested = { sheetPage = FilamentSheetPage.CustomColor }
                )
                FilamentSheetPage.Profile -> FilamentSlotProfilePage(
                    availableFilaments = availableFilaments,
                    onBack = { sheetPage = FilamentSheetPage.Main },
                    onFilamentSelected = onFilamentSelected
                )
                FilamentSheetPage.CustomColor -> FilamentSlotCustomColorPage(
                    hue = customHue,
                    saturation = customSaturation,
                    value = customValue,
                    onHueChanged = { customHue = it },
                    onSaturationChanged = { customSaturation = it },
                    onValueChanged = { customValue = it },
                    onBack = { sheetPage = FilamentSheetPage.Main },
                    onColorSelected = {
                        onColorSelected(hsvToColorHex(customHue, customSaturation, customValue))
                        sheetPage = FilamentSheetPage.Main
                    }
                )
            }
        }
    }
}

@Composable
private fun FilamentSlotMainPage(
    slot: PlateFilamentSlot,
    selectedObjectLabel: String?,
    colorChoices: List<String>,
    physicalNozzleCount: Int,
    onAssignToSelected: () -> Unit,
    onColorSelected: (String) -> Unit,
    onNozzleSelected: (Int?) -> Unit,
    onRemoveSlot: () -> Unit,
    onProfilePageRequested: () -> Unit,
    onCustomColorPageRequested: () -> Unit
) {
    Text(
        text = "Filament ${slot.index}: ${slot.materialType.ifBlank { slot.label.ifBlank { "Filament" } }}",
        style = MaterialTheme.typography.titleMedium,
        color = appTitleColor(),
        fontWeight = FontWeight.SemiBold
    )
    Button(
        onClick = onAssignToSelected,
        enabled = selectedObjectLabel != null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(selectedObjectLabel?.let { "Assign to $it" } ?: "No object selected")
    }
    Text(
        text = "Profile",
        style = MaterialTheme.typography.labelLarge,
        color = appTitleColor(),
        fontWeight = FontWeight.SemiBold
    )
    Button(
        onClick = onProfilePageRequested,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = appCardColorMuted(),
            contentColor = appTitleColor()
        )
    ) {
        Text(
            text = slot.label.ifBlank { "Select profile" },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    Text(
        text = "Color",
        style = MaterialTheme.typography.labelLarge,
        color = appTitleColor(),
        fontWeight = FontWeight.SemiBold
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        colorChoices.forEach { colorHex ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(slotColor(colorHex))
                    .border(1.dp, appOutlineColor(), RoundedCornerShape(12.dp))
                    .clickable { onColorSelected(colorHex) }
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                    )
                )
                .border(1.dp, appOutlineColor(), RoundedCornerShape(12.dp))
                .clickable(onClick = onCustomColorPageRequested),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
    if (physicalNozzleCount > 1) {
        Text(
            text = "Nozzle",
            style = MaterialTheme.typography.labelLarge,
            color = appTitleColor(),
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilamentSlotNozzleButton(
                label = "Auto",
                selected = slot.physicalNozzleIndex == null,
                onClick = { onNozzleSelected(null) }
            )
            repeat(physicalNozzleCount) { nozzleOffset ->
                val nozzleIndex = nozzleOffset + 1
                FilamentSlotNozzleButton(
                    label = "N$nozzleIndex",
                    selected = slot.physicalNozzleIndex == nozzleIndex,
                    onClick = { onNozzleSelected(nozzleIndex) }
                )
            }
        }
    }
    if (slot.index > 1) {
        TextButton(
            onClick = onRemoveSlot,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete filament ${slot.index}")
        }
    }
}

@Composable
private fun FilamentSlotProfilePage(
    availableFilaments: List<FilamentProfile>,
    onBack: () -> Unit,
    onFilamentSelected: (FilamentProfile) -> Unit
) {
    TextButton(onClick = onBack) {
        Text("Back")
    }
    Text(
        text = "Select profile",
        style = MaterialTheme.typography.titleMedium,
        color = appTitleColor(),
        fontWeight = FontWeight.SemiBold
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        items(availableFilaments) { filament ->
            Button(
                onClick = { onFilamentSelected(filament) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = appCardColorMuted(),
                    contentColor = appTitleColor()
                )
            ) {
                Text(
                    text = listOf(filament.name, filament.materialType)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" • "),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilamentSlotCustomColorPage(
    hue: Float,
    saturation: Float,
    value: Float,
    onHueChanged: (Float) -> Unit,
    onSaturationChanged: (Float) -> Unit,
    onValueChanged: (Float) -> Unit,
    onBack: () -> Unit,
    onColorSelected: () -> Unit
) {
    TextButton(onClick = onBack) {
        Text("Back")
    }
    Text(
        text = "Custom color",
        style = MaterialTheme.typography.titleMedium,
        color = appTitleColor(),
        fontWeight = FontWeight.SemiBold
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(slotColor(hsvToColorHex(hue, saturation, value)))
    )
    CompactColorSlider("Hue", hue, 0f..360f, onHueChanged)
    CompactColorSlider("Saturation", saturation, 0f..1f, onSaturationChanged)
    CompactColorSlider("Brightness", value, 0f..1f, onValueChanged)
    Button(
        onClick = onColorSelected,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Use custom color")
    }
}

@Composable
private fun FilamentSlotNozzleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else appCardColorMuted(),
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else appTitleColor()
        )
    ) {
        Text(label)
    }
}

@Composable
private fun CompactColorSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = label,
            color = appTitleColor(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.height(34.dp)
        )
    }
}

internal fun slotColor(colorHex: String): Color =
    runCatching {
        Color(android.graphics.Color.parseColor(colorHex.ifBlank { "#8FC1FF" }))
    }.getOrDefault(Color(0xFF8FC1FF))

internal fun PrinterProfile.physicalNozzleCount(): Int {
    fun parseNozzleValue(value: Any?): Int = when (value) {
        is org.json.JSONArray -> value.length()
        is String -> value.split(',', ';').map { it.trim() }.count { it.isNotBlank() }
        else -> 0
    }
    val resolvedCount = runCatching {
        org.json.JSONObject(orcaResolvedMachineJson).opt("nozzle_diameter")
    }.getOrNull().let(::parseNozzleValue)
    if (resolvedCount > 0) return resolvedCount
    val modelCount = runCatching {
        org.json.JSONObject(orcaMachineModelJson).opt("nozzle_diameter")
    }.getOrNull().let(::parseNozzleValue)
    if (modelCount > 0) return modelCount
    return extrudersCount.toIntOrNull()?.takeIf { it > 0 } ?: 1
}

private enum class FilamentSheetPage {
    Main,
    Profile,
    CustomColor
}

private fun hsvToColorHex(hue: Float, saturation: Float, value: Float): String {
    val color = android.graphics.Color.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 360f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f)
        )
    )
    return String.format(
        Locale.US,
        "#%02X%02X%02X",
        android.graphics.Color.red(color),
        android.graphics.Color.green(color),
        android.graphics.Color.blue(color)
    )
}
