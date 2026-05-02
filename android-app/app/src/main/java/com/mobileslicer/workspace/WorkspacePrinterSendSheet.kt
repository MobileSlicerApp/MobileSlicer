package com.mobileslicer.workspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import com.mobileslicer.CompactWorkspaceBadge
import com.mobileslicer.R
import com.mobileslicer.appBackgroundGradient
import com.mobileslicer.appBodyColor
import com.mobileslicer.appCardColor
import com.mobileslicer.appTitleColor
import com.mobileslicer.profiles.ActiveSlicerConfiguration
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.profiles.PrintHostType
import com.mobileslicer.printerconnection.BambuLanPrintOptions
import com.mobileslicer.printerconnection.bambuLanPrintOptions
import com.mobileslicer.printerconnection.connectionCapabilities
import com.mobileslicer.printerconnection.PrinterUploadAction
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mobileslicer.viewer.MeshBounds
import com.mobileslicer.viewer.StlMesh
import com.mobileslicer.nativebridge.NativeEngineBridge
import com.mobileslicer.ui.theme.AccentPaletteOption
import com.mobileslicer.viewer.PrinterBedSpec
import com.mobileslicer.viewer.TouchModelViewerView
import com.mobileslicer.viewer.ViewerModelTransform
import com.mobileslicer.viewer.ViewerPlateObject
import com.mobileslicer.ui.theme.MobileSlicerTheme
import com.mobileslicer.ui.theme.PanelAmber
import com.mobileslicer.ui.theme.PanelBlue
import com.mobileslicer.ui.theme.PanelGreen
import com.mobileslicer.ui.theme.PanelLavender
import com.mobileslicer.ui.theme.PanelSlate
import com.mobileslicer.ui.theme.LocalAppDarkTheme
import com.mobileslicer.ui.theme.ThemeModeOption
import com.mobileslicer.ui.theme.WorldViewColorOption
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrinterSendSheet(
    sending: Boolean,
    suggestedFileName: String,
    printerProfile: PrinterProfile,
    onUpload: (String, BambuLanPrintOptions?) -> Unit,
    onUploadAndStart: (String, BambuLanPrintOptions?) -> Unit,
    onQueue: (String, BambuLanPrintOptions?) -> Unit,
    onDismiss: () -> Unit
) {
    val capabilities = printerProfile.connectionCapabilities()
    val defaultBambuOptions = printerProfile.bambuLanPrintOptions()
    var remoteFileName by rememberSaveable(suggestedFileName) {
        mutableStateOf(suggestedFileName.toPrinterUploadFileName())
    }
    var bambuBedType by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.bedType) }
    var bambuUseAms by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.useAms) }
    var bambuAmsMapping by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.amsMapping) }
    var bambuNozzleMapping by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.nozzleMapping) }
    var bambuBedLeveling by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.bedLeveling) }
    var bambuFlowCalibration by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.flowCalibration) }
    var bambuVibrationCalibration by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.vibrationCalibration) }
    var bambuTimelapse by rememberSaveable(printerProfile.id) { mutableStateOf(defaultBambuOptions.timelapse) }
    var showBambuAdvanced by rememberSaveable(printerProfile.id) { mutableStateOf(false) }
    val finalRemoteFileName = remoteFileName.toPrinterUploadFileName()
    fun currentBambuOptions(): BambuLanPrintOptions? =
        if (printerProfile.printHostType == PrintHostType.BambuLan) {
            BambuLanPrintOptions(
                bedType = bambuBedType.trim(),
                useAms = bambuUseAms,
                amsMapping = bambuAmsMapping.trim(),
                nozzleMapping = bambuNozzleMapping.trim(),
                bedLeveling = bambuBedLeveling,
                flowCalibration = bambuFlowCalibration,
                vibrationCalibration = bambuVibrationCalibration,
                timelapse = bambuTimelapse
            )
        } else {
            null
        }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = appCardColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Send to Printer",
                style = MaterialTheme.typography.titleMedium,
                color = appTitleColor()
            )
            OutlinedTextField(
                value = remoteFileName,
                onValueChange = { remoteFileName = it },
                label = { Text("Upload name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (printerProfile.printHostType == PrintHostType.BambuLan) {
                OutlinedTextField(
                    value = bambuBedType,
                    onValueChange = { bambuBedType = it },
                    label = { Text("Bed type") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                BambuSendSwitch("Bed leveling", bambuBedLeveling, { bambuBedLeveling = it })
                TextButton(
                    onClick = { showBambuAdvanced = !showBambuAdvanced },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showBambuAdvanced) "Hide Bambu options" else "Bambu options")
                }
                if (showBambuAdvanced) {
                    BambuSendSwitch("Use AMS", bambuUseAms, { bambuUseAms = it })
                    BambuSendSwitch("Flow calibration", bambuFlowCalibration, { bambuFlowCalibration = it })
                    BambuSendSwitch("Vibration calibration", bambuVibrationCalibration, { bambuVibrationCalibration = it })
                    BambuSendSwitch("Timelapse", bambuTimelapse, { bambuTimelapse = it })
                    OutlinedTextField(
                        value = bambuAmsMapping,
                        onValueChange = { bambuAmsMapping = it },
                        label = { Text("AMS mapping") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bambuNozzleMapping,
                        onValueChange = { bambuNozzleMapping = it },
                        label = { Text("Nozzle mapping") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (capabilities.canUploadAndStart) {
                Button(
                    onClick = { onUploadAndStart(finalRemoteFileName, currentBambuOptions()) },
                    enabled = !sending && finalRemoteFileName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Upload & Start")
                }
            }
            Button(
                onClick = { onUpload(finalRemoteFileName, currentBambuOptions()) },
                enabled = !sending && finalRemoteFileName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Upload Only")
            }
            if (capabilities.canQueue) {
                Button(
                    onClick = { onQueue(finalRemoteFileName, currentBambuOptions()) },
                    enabled = !sending && finalRemoteFileName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Upload to Queue")
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !sending
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun BambuSendSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = appBodyColor(), style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

internal fun String.toPrinterUploadFileName(): String {
    val cleaned = trim()
        .replace(Regex("""[\\/:*?"<>|]+"""), "_")
        .replace(Regex("""\s+"""), " ")
        .ifBlank { "mobile_slicer_output.gcode" }
    return if (cleaned.endsWith(".gcode", ignoreCase = true)) cleaned else "$cleaned.gcode"
}

internal fun compactPrinterStatusLabel(message: String?): String? {
    val detail = message
        ?.lineSequence()
        ?.drop(1)
        ?.joinToString(" ")
        ?.ifBlank { null }
        ?: return null
    if (detail.startsWith("Status unavailable", ignoreCase = true) || detail.startsWith("Connection", ignoreCase = true)) {
        return null
    }
    val parts = detail.split(" • ").map { it.trim() }
    val state = parts.firstOrNull { it.startsWith("State:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
        ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    val progress = parts.firstOrNull { it.startsWith("Progress:", ignoreCase = true) }
        ?.substringAfter(':')
        ?.trim()
    return listOfNotNull(state, progress).joinToString(" ").ifBlank { null }
}
