package com.mobileslicer.calibration

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mobileslicer.CompactWorkspaceBadge
import com.mobileslicer.R
import com.mobileslicer.appBackgroundGradient
import com.mobileslicer.appBodyColor
import com.mobileslicer.appCardColor
import com.mobileslicer.appCardColorMuted
import com.mobileslicer.appMutedColor
import com.mobileslicer.appOutlineColor
import com.mobileslicer.appTitleColor
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.profiles.activeConfiguration
import org.json.JSONObject
import java.io.File
import java.util.Locale

@Composable
internal fun CalibrationOptionsDialog(
    calibration: CalibrationType,
    defaultOptions: CalibrationOptions,
    onDismiss: () -> Unit,
    onConfirm: (CalibrationOptions) -> Unit
) {
    var extruderType by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.extruderType) }
    var method by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.method) }
    var filamentType by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.filamentType) }
    var flowPass by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.flowPass) }
    var start by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.startValue) }
    var end by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.endValue) }
    var step by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.stepValue) }
    var printNumbers by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.printNumbers) }
    var flowRatioBaseline by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.flowRatioBaseline) }
    var patternAccelerations by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.patternAccelerations) }
    var patternSpeeds by rememberSaveable(calibration.name) { mutableStateOf(defaultOptions.patternSpeeds) }
    val bodyColor = appBodyColor()

    fun applyPressureAdvanceDefaults(nextMethod: String = method, nextExtruderType: String = extruderType) {
        val defaults = pressureAdvanceDefaults(method = nextMethod, extruderType = nextExtruderType)
        method = defaults.method
        extruderType = defaults.extruderType
        start = defaults.startValue
        end = defaults.endValue
        step = defaults.stepValue
        printNumbers = defaults.printNumbers
        patternAccelerations = defaults.patternAccelerations
        patternSpeeds = defaults.patternSpeeds
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(calibration.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (calibration) {
                    CalibrationType.PressureAdvance -> {
                        OptionSection("Extruder type") {
                            RadioRow("DDE", extruderType == "DDE") { applyPressureAdvanceDefaults(nextExtruderType = "DDE") }
                            RadioRow("Bowden", extruderType == "Bowden") { applyPressureAdvanceDefaults(nextExtruderType = "Bowden") }
                        }
                        OptionSection("Method") {
                            listOf("PA Tower", "PA Line", "PA Pattern").forEach { value ->
                                RadioRow(value, method == value) { applyPressureAdvanceDefaults(nextMethod = value) }
                            }
                        }
                        NumericField(start, { start = it }, "Start PA")
                        NumericField(end, { end = it }, "End PA")
                        NumericField(step, { step = it }, "PA step")
                        if (method == "PA Pattern") {
                            NumericField(
                                patternAccelerations,
                                { patternAccelerations = it },
                                "Accelerations",
                                keyboardType = KeyboardType.Text
                            )
                            NumericField(patternSpeeds, { patternSpeeds = it }, "Speeds", keyboardType = KeyboardType.Text)
                        }
                        if (method == "PA Line") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = printNumbers, onCheckedChange = { printNumbers = it })
                                Text("Print numbers", color = bodyColor)
                            }
                        } else if (printNumbers) {
                            Text("Print numbers enabled", color = bodyColor)
                        }
                    }
                    CalibrationType.TemperatureTower -> {
                        OptionSection("Filament type") {
                            listOf("PLA", "ABS/ASA", "PETG", "PCTG", "TPU", "PA-CF", "PET-CF", "Custom").forEach { value ->
                                RadioRow(value, filamentType == value) { filamentType = value }
                            }
                        }
                        NumericField(start, { start = it }, "Start temp", "°C")
                        NumericField(end, { end = it }, "End temp", "°C")
                        NumericField(step, { step = it }, "Temp step", "°C")
                    }
                    CalibrationType.MaxVolumetricSpeed -> {
                        NumericField(start, { start = it }, "Start volumetric speed", "mm3/s")
                        NumericField(end, { end = it }, "End volumetric speed", "mm3/s")
                        NumericField(step, { step = it }, "Step", "mm3/s")
                    }
                    CalibrationType.FlowRate -> {
                        OptionSection("Calibration Type") {
                            RadioRow(FLOW_RATE_COMPLETE, flowPass == FLOW_RATE_COMPLETE) {
                                flowPass = FLOW_RATE_COMPLETE
                            }
                            RadioRow(FLOW_RATE_FINE, flowPass == FLOW_RATE_FINE) {
                                flowPass = FLOW_RATE_FINE
                            }
                        }
                        if (flowPass == FLOW_RATE_FINE) {
                            NumericField(flowRatioBaseline, { flowRatioBaseline = it }, "Flow ratio")
                            Text(
                                text = "Orca accepts values where 0.0 < flow ratio < 2.0.",
                                style = MaterialTheme.typography.bodySmall,
                                color = appMutedColor()
                            )
                        }
                    }
                    CalibrationType.Retraction -> {
                        NumericField(start, { start = it }, "Start retraction", "mm")
                        NumericField(end, { end = it }, "End retraction", "mm")
                        NumericField(step, { step = it }, "Step", "mm")
                    }
                    CalibrationType.Vfa -> {
                        NumericField(start, { start = it }, "Start speed", "mm/s")
                        NumericField(end, { end = it }, "End speed", "mm/s")
                        NumericField(step, { step = it }, "Speed step", "mm/s")
                    }
                    CalibrationType.InputShapingFrequency -> {
                        NumericField(start, { start = it }, "Start frequency", "Hz")
                        NumericField(end, { end = it }, "End frequency", "Hz")
                        NumericField(step, { step = it }, "Frequency step", "Hz")
                    }
                    CalibrationType.InputShapingDamping -> {
                        NumericField(start, { start = it }, "Start damping")
                        NumericField(end, { end = it }, "End damping")
                        NumericField(step, { step = it }, "Damping step")
                    }
                    CalibrationType.Cornering -> {
                        NumericField(start, { start = it }, "Start cornering speed", "mm/s")
                        NumericField(end, { end = it }, "End cornering speed", "mm/s")
                        NumericField(step, { step = it }, "Step", "mm/s")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CalibrationOptions(
                            extruderType = extruderType,
                            method = method,
                            filamentType = filamentType,
                            startValue = start,
                            endValue = end,
                            stepValue = step,
                            printNumbers = printNumbers,
                            flowPass = flowPass,
                            flowRatioBaseline = flowRatioBaseline,
                            patternAccelerations = patternAccelerations,
                            patternSpeeds = patternSpeeds
                        )
                    )
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OptionSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = appTitleColor(),
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, color = appBodyColor())
    }
}

@Composable
private fun NumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
