package com.mobileslicer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.mobileslicer.automation.AutomationConfigResolver
import com.mobileslicer.automation.AutomationSliceRequest
import com.mobileslicer.automation.AutomationSliceRunner
import com.mobileslicer.nativebridge.NativeEngineCallResult
import com.mobileslicer.nativebridge.NativeEngineCalls
import com.mobileslicer.nativebridge.NativeEngineErrorCode
import com.mobileslicer.nativebridge.NativeEngineHandle
import com.mobileslicer.nativebridge.NativeEngineSession
import com.mobileslicer.printerconnection.PrinterConnectionRepository
import com.mobileslicer.printerconnection.PrinterConnectionChoicesResult
import com.mobileslicer.printerconnection.PrinterConnectionResult
import com.mobileslicer.printerconnection.PrinterDiscoveryRepository
import com.mobileslicer.printerconnection.PrinterUploadAction
import com.mobileslicer.printerconnection.SimplyPrintOAuthClient
import com.mobileslicer.printerconnection.SimplyPrintOAuthResult
import com.mobileslicer.profiles.ProfileStore
import com.mobileslicer.profiles.ProfileStoreRepository
import com.mobileslicer.profiles.PrintHostType
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.storage.AppPreferenceStore
import com.mobileslicer.storage.GCODE_MIME_TYPE
import com.mobileslicer.storage.PreparedViewerMeshCache
import com.mobileslicer.storage.PrinterCredentialStore
import com.mobileslicer.storage.SavedProject
import com.mobileslicer.storage.SavedProjectRepository
import com.mobileslicer.viewer.StlMesh
import com.mobileslicer.viewer.StlMeshParser
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
import com.mobileslicer.workspace.GcodeSummaryParser
import com.mobileslicer.workspace.ImportedModelFormat
import com.mobileslicer.workspace.ModelImportTiming
import com.mobileslicer.workspace.ModelLoadResult
import com.mobileslicer.workspace.PlateObject
import com.mobileslicer.workspace.SlicePipelineTiming
import com.mobileslicer.workspace.SliceResult
import com.mobileslicer.workspace.WorkspacePreparationResult
import com.mobileslicer.workspace.WorkspacePreparationTiming
import com.mobileslicer.workspace.WorkspaceScreen
import com.mobileslicer.workspace.WorkspaceSessionViewModel
import com.mobileslicer.workspace.defaultNativeModelTransform
import com.mobileslicer.workspace.modelLoadStatusMessage
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import com.mobileslicer.viewer.MeshBounds

private data class NativePlateLoadRequest(
    val paths: Array<String>,
    val transforms: DoubleArray,
    val extruderIds: IntArray,
    val signature: String
)

private fun nativePlateLoadRequest(
    plateObjects: List<PlateObject>,
    printerBed: PrinterBedSpec
): NativePlateLoadRequest? {
    if (plateObjects.isEmpty()) return null
    val sliceObjects = plateObjects.filter { objectOnPlate ->
        objectOnPlate.format == ImportedModelFormat.Stl &&
            objectOnPlate.filePath.isNotBlank() &&
            (objectOnPlate.mesh != null || objectOnPlate.bounds != null) &&
            File(objectOnPlate.filePath).exists()
    }
    if (sliceObjects.size != plateObjects.size) return null

    val paths = Array(sliceObjects.size) { index -> sliceObjects[index].filePath }
    val transforms = DoubleArray(sliceObjects.size * 7)
    val extruderIds = IntArray(sliceObjects.size)
    val compactSlotIndexes = sliceObjects
        .map { it.filamentSlotIndex.coerceAtLeast(1) }
        .distinct()
        .sorted()
        .mapIndexed { index, slotIndex -> slotIndex to index + 1 }
        .toMap()
    sliceObjects.forEachIndexed { index, objectOnPlate ->
        val objectBounds = objectOnPlate.mesh?.bounds ?: objectOnPlate.bounds ?: return null
        val transform = defaultNativeModelTransform(objectBounds, printerBed, objectOnPlate.transform)
        val offset = index * 7
        transforms[offset + 0] = transform.xMm
        transforms[offset + 1] = transform.yMm
        transforms[offset + 2] = transform.zMm
        transforms[offset + 3] = transform.rotationXRadians
        transforms[offset + 4] = transform.rotationYRadians
        transforms[offset + 5] = transform.rotationZRadians
        transforms[offset + 6] = transform.uniformScale
        extruderIds[index] = compactSlotIndexes[objectOnPlate.filamentSlotIndex.coerceAtLeast(1)] ?: 1
    }
    val bedSignature = listOf(
        printerBed.originXmm,
        printerBed.originYmm,
        printerBed.widthMm,
        printerBed.depthMm,
        printerBed.maxHeightMm
    ).joinToString(",") { "%.3f".format(Locale.US, it) }
    val signature = "plate:bed=$bedSignature:" + sliceObjects.mapIndexed { index, objectOnPlate ->
        val originalSlot = objectOnPlate.filamentSlotIndex.coerceAtLeast(1)
        "$index:${objectOnPlate.nativeSourceKey}:slot=$originalSlot:${compactSlotIndexes[originalSlot] ?: 1}:${objectOnPlate.transform}"
    }.joinToString("|")
    return NativePlateLoadRequest(paths, transforms, extruderIds, signature)
}

internal fun MainActivity.loadModelFileIntoNativeCache(modelFilePath: String): Boolean {
    val handle = NativeEngineHandle.fromRaw(ensureEngine())
    val modelFile = File(modelFilePath)
    if (handle == null || !modelFile.exists()) {
        nativeLoadedModelPath = null
        return false
    }
    if (nativeLoadedModelPath == modelFile.absolutePath) {
        return true
    }
    val loadResult = NativeEngineCalls.loadModel(handle, modelFile.absolutePath)
    return if (loadResult is NativeEngineCallResult.Success) {
        nativeLoadedModelPath = modelFile.absolutePath
        currentModelName = modelFile.nameWithoutExtension.removePrefix("selected-model-")
        true
    } else {
        NativeEngineCalls.clearGeneratedGcode(handle)
        nativeLoadedModelPath = null
        false
    }
}

internal fun MainActivity.loadPlateObjectsIntoNativeCache(
    plateObjects: List<PlateObject>,
    printerBed: PrinterBedSpec
): Boolean {
    val handle = NativeEngineHandle.fromRaw(ensureEngine())
    if (handle == null) {
        nativeLoadedModelPath = null
        return false
    }
    if (plateObjects.size == 1) {
        return loadModelFileIntoNativeCache(plateObjects.first().filePath)
    }
    val request = nativePlateLoadRequest(plateObjects, printerBed) ?: run {
        NativeEngineCalls.clearGeneratedGcode(handle)
        nativeLoadedModelPath = null
        return false
    }
    if (nativeLoadedModelPath == request.signature) {
        return true
    }
    val loadResult = NativeEngineCalls.loadPlateModels(handle, request.paths, request.transforms, request.extruderIds)
    return if (loadResult is NativeEngineCallResult.Success) {
        nativeLoadedModelPath = request.signature
        true
    } else {
        NativeEngineCalls.clearGeneratedGcode(handle)
        nativeLoadedModelPath = null
        false
    }
}

internal fun MainActivity.sliceCurrentModel(
        configJson: String,
        plateObjects: List<PlateObject>,
        modelFilePath: String?,
        preparedMesh: StlMesh?,
        modelBounds: MeshBounds?,
        printerBed: PrinterBedSpec,
        modelTransform: ViewerModelTransform?,
        suggestedGcodeFileName: String? = null,
        allowEngineRecovery: Boolean = true
    ): SliceResult {
        val pipelineStartedAt = SystemClock.elapsedRealtime()
        val handle = NativeEngineHandle.fromRaw(ensureEngine())
        if (handle == null) {
            return SliceResult("Slice failed\nNative engine is unavailable.", sliced = false)
        }
        var modelReloadMs = 0L
        val shouldUsePlateLoad = plateObjects.size > 1
        if (shouldUsePlateLoad) {
            val request = nativePlateLoadRequest(plateObjects, printerBed)
            if (request == null && plateObjects.isEmpty()) {
                return SliceResult("Slice failed\nNo plate objects are ready to slice.", sliced = false)
            }
            if (request == null) {
                return SliceResult("Slice failed\nOne or more plate objects are still preparing.", sliced = false)
            }
            if (nativeLoadedModelPath != request.signature) {
                val modelReloadStartedAt = SystemClock.elapsedRealtime()
                val loadPlateResult = NativeEngineCalls.loadPlateModels(handle, request.paths, request.transforms, request.extruderIds)
                if (loadPlateResult !is NativeEngineCallResult.Success) {
                    NativeEngineCalls.clearGeneratedGcode(handle)
                    nativeLoadedModelPath = null
                    return SliceResult(
                        "Slice failed\nUnable to load all plate objects into the native engine.\n${loadPlateResult.statusMessage}",
                        sliced = false
                    )
                }
                modelReloadMs = SystemClock.elapsedRealtime() - modelReloadStartedAt
                nativeLoadedModelPath = request.signature
            }
        } else {
            val singlePlateObject = plateObjects.singleOrNull()
            val stagedModel = singlePlateObject?.filePath?.let(::File)?.takeIf { it.exists() }
                ?: modelFilePath?.let(::File)?.takeIf { it.exists() }
                ?: stagedModelFile?.takeIf { it.exists() }
                ?: return SliceResult("Slice failed\nno model loaded", sliced = false)
            val bounds = singlePlateObject?.mesh?.bounds
                ?: singlePlateObject?.bounds
                ?: preparedMesh?.bounds
                ?: modelBounds
                ?: return SliceResult("Slice failed\nModel placement bounds are not ready.", sliced = false)
            if (nativeLoadedModelPath != stagedModel.absolutePath) {
                val modelReloadStartedAt = SystemClock.elapsedRealtime()
                val loadResult = NativeEngineCalls.loadModel(handle, stagedModel.absolutePath)
                if (loadResult !is NativeEngineCallResult.Success) {
                    NativeEngineCalls.clearGeneratedGcode(handle)
                    nativeLoadedModelPath = null
                    return SliceResult(
                        "Slice failed\nUnable to load the staged model into the native engine.\n${loadResult.statusMessage}",
                        sliced = false
                    )
                }
                modelReloadMs = SystemClock.elapsedRealtime() - modelReloadStartedAt
                nativeLoadedModelPath = stagedModel.absolutePath
            }
            val transform = defaultNativeModelTransform(
                bounds,
                printerBed,
                singlePlateObject?.transform ?: modelTransform
            )
            val transformResult = NativeEngineCalls.setModelTransform(
                handle,
                transform.xMm,
                transform.yMm,
                transform.zMm,
                transform.rotationXRadians,
                transform.rotationYRadians,
                transform.rotationZRadians,
                transform.uniformScale
            )
            if (transformResult !is NativeEngineCallResult.Success) {
                return SliceResult(
                    "Slice failed\nNative model transform could not be applied.\n${transformResult.statusMessage}",
                    sliced = false
                )
            }
        }
        val configStartedAt = SystemClock.elapsedRealtime()
        val configResult = NativeEngineCalls.setConfigJson(handle, configJson)
        if (configResult !is NativeEngineCallResult.Success) {
            return SliceResult(
                "Slice failed\nNative slice configuration could not be applied.\n${configResult.statusMessage}",
                sliced = false
            )
        }
        val configMs = SystemClock.elapsedRealtime() - configStartedAt

        val nativeSliceStartedAt = SystemClock.elapsedRealtime()
        var nativeSliceResult = NativeEngineCalls.slice(handle)
        val firstNativeSliceMs = SystemClock.elapsedRealtime() - nativeSliceStartedAt
        if (
            nativeSliceResult !is NativeEngineCallResult.Success &&
            (nativeSliceResult as? NativeEngineCallResult.Failure)?.error?.message.orEmpty()
                .equals("vector", ignoreCase = true)
        ) {
            Log.w(
                MainActivity.TAG,
                "sliceCurrentModel: native slice failed with vector after ${firstNativeSliceMs}ms; retrying full native slice once on same engine"
            )
            releaseSliceMemoryPressure()
            nativeSliceResult = NativeEngineCalls.slice(handle)
        }
        val nativeSliceSucceeded = nativeSliceResult is NativeEngineCallResult.Success
        if (!nativeSliceSucceeded) {
            val nativeError = (nativeSliceResult as? NativeEngineCallResult.Failure)?.error?.message.orEmpty()
            if (shouldRecoverNativeEngine(nativeError)) {
                if (allowEngineRecovery) {
                    Log.w(MainActivity.TAG, "sliceCurrentModel: native slice failed with $nativeError")
                } else {
                    Log.w(MainActivity.TAG, "sliceCurrentModel: native slice failed with $nativeError after native engine recovery")
                }
            }
        }

        val result = if (nativeSliceSucceeded) {
            val nativeSliceMs = SystemClock.elapsedRealtime() - nativeSliceStartedAt
            val gcodeFileName = suggestedGcodeFileName
                ?.takeIf { it.isNotBlank() }
                ?: suggestGcodeFileName(currentModelName, configJson)
            val gcodeFile = File(cacheDir, "latest-slice-$gcodeFileName")
            val writeGcodeStartedAt = SystemClock.elapsedRealtime()
            val writeGcodeResult = NativeEngineCalls.writeGcodeToFile(handle, gcodeFile.absolutePath)
            val writeGcodeMs = SystemClock.elapsedRealtime() - writeGcodeStartedAt
            if (writeGcodeResult !is NativeEngineCallResult.Success || !gcodeFile.exists() || gcodeFile.length() <= 0L) {
                val nativeError = (writeGcodeResult as? NativeEngineCallResult.Failure)?.error?.message.orEmpty()
                SliceResult(
                    buildString {
                        append("Slice failed\nNo G-code file was returned.")
                        if (nativeError.isNotBlank()) {
                            append("\n")
                            append(nativeError)
                        }
                    },
                    sliced = false
                )
            } else {
                cleanupGeneratedGcodeCache(retainedPaths = retainedCachePaths(listOf(gcodeFile), includeCurrentGcode = false))
                val summaryStartedAt = SystemClock.elapsedRealtime()
                val nativeSummaryText = NativeEngineCalls.getGcodeSummary(handle)
                val summary = GcodeSummaryParser.fromNativeSummary(nativeSummaryText)
                val summaryMs = SystemClock.elapsedRealtime() - summaryStartedAt
                if (summary == null) {
                    val nativeError = NativeEngineCalls.getLastErrorMessage(handle)
                    return SliceResult(
                        buildString {
                            append("Slice failed\nNative G-code summary was missing or incomplete.")
                            if (!nativeSummaryText.isNullOrBlank()) {
                                append("\nSummary: ")
                                append(nativeSummaryText.take(240))
                            }
                            if (nativeError.isNotBlank()) {
                                append("\n")
                                append(nativeError)
                            }
                        },
                        sliced = false,
                        timing = SlicePipelineTiming(
                            modelReloadMs = modelReloadMs,
                            configMs = configMs,
                            nativeSliceMs = nativeSliceMs,
                            writeGcodeMs = writeGcodeMs,
                            summaryMs = summaryMs,
                            totalMs = SystemClock.elapsedRealtime() - pipelineStartedAt
                        )
                    )
                }
                val totalMs = SystemClock.elapsedRealtime() - pipelineStartedAt
                SliceResult(
                    message = "Slice successful",
                    sliced = true,
                    gcodeFilePath = gcodeFile.absolutePath,
                    fileName = gcodeFileName,
                    summary = summary,
                    timing = SlicePipelineTiming(
                        modelReloadMs = modelReloadMs,
                        configMs = configMs,
                        nativeSliceMs = nativeSliceMs,
                        writeGcodeMs = writeGcodeMs,
                        summaryMs = summaryMs,
                        totalMs = totalMs
                    )
                )
            }
        } else {
            SliceResult(describeNativeSliceFailure().userMessage, sliced = false)
        }
        return result
    }

internal fun MainActivity.shouldRecoverNativeEngine(nativeError: String): Boolean {
        val normalized = nativeError.lowercase(Locale.US)
        return normalized == "vector" || normalized.contains("bad_alloc")
    }

internal fun MainActivity.releaseSliceMemoryPressure() {
        preparedViewerMeshCache.clear()
        NativeEngineCalls.trimMemory()
        Runtime.getRuntime().gc()
    }

internal fun MainActivity.describeNativeSliceFailure(): NativeSliceFailurePresentation {
        val handle = nativeEngineSession.handleOrNull()
        val nativeError = handle?.let(NativeEngineCalls::getLastEngineError)
        if (nativeError?.code == NativeEngineErrorCode.PrintableVolumeExceeded) {
            return NativeSliceFailurePresentation(
                userMessage = "Slice failed\nPrintable volume exceeded.\nCurrent Printer bed dimensions rejected emitted extrusion outside the configured printable area or height.",
                automationStatus = nativeError.automationStatus
            )
        }
        return if (nativeError != null) {
            NativeSliceFailurePresentation(
                userMessage = "Slice failed\n${nativeError.message}",
                automationStatus = "nativeSlice returned false ${nativeError.automationStatus}"
            )
        } else {
            NativeSliceFailurePresentation(
                userMessage = "Slice failed",
                automationStatus = "nativeSlice returned false"
            )
        }
    }
