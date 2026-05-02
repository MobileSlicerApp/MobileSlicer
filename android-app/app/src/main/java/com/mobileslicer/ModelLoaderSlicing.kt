package com.mobileslicer

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.mobileslicer.calibration.CalibrationJob
import com.mobileslicer.profiles.ActiveSlicerConfiguration
import com.mobileslicer.profiles.FilamentProfile
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.profiles.jsonObjectLengthOrZero
import com.mobileslicer.profiles.repairNativeSliceConfigWithOrcaFilamentAssetsResult
import com.mobileslicer.profiles.toNativeSliceConfigBuildResult
import com.mobileslicer.viewer.MeshBounds
import com.mobileslicer.viewer.PrinterBedSpec
import com.mobileslicer.viewer.StlMesh
import com.mobileslicer.viewer.ViewerModelTransform
import com.mobileslicer.workspace.PlateFilamentSlot
import com.mobileslicer.workspace.PlateFlushVolumes
import com.mobileslicer.workspace.PlateObject
import com.mobileslicer.workspace.SliceResult

private const val VerboseSliceConfigTimingLogs = false

internal typealias ModelLoaderSliceRequestHandler = (
    String,
    List<PlateObject>,
    String?,
    StlMesh?,
    MeshBounds?,
    PrinterBedSpec,
    ViewerModelTransform?,
    String?
) -> SliceResult

internal suspend fun runModelLoaderSlice(
    context: Context,
    configuration: ActiveSlicerConfiguration,
    calibrationJob: CalibrationJob?,
    plateObjects: List<PlateObject>,
    profileFilaments: List<FilamentProfile>,
    activePlateSlots: List<PlateFilamentSlot>,
    flushVolumes: PlateFlushVolumes?,
    printer: PrinterProfile,
    modelFilePath: String?,
    preparedMesh: StlMesh?,
    modelBounds: MeshBounds?,
    modelTransform: ViewerModelTransform?,
    gcodeFileName: String,
    onSliceRequested: ModelLoaderSliceRequestHandler
): SliceResult {
    val repairFilamentOverridesJson = activePlateSlots
        .firstOrNull()
        ?.filamentProfileId
        ?.let { slotFilamentId ->
            profileFilaments.firstOrNull { it.id == slotFilamentId }
        }
        ?.orcaFilamentOverridesJson
        ?: configuration.filament.orcaFilamentOverridesJson
    val normalizedFlushVolumes = ensureFlushVolumesForSlots(
        slots = activePlateSlots,
        existing = flushVolumes,
        regenerateFromColors = false
    )
    val configStartedAtMs = SystemClock.elapsedRealtime()
    val nativeBuildResult = configuration.toNativeSliceConfigBuildResult(context.applicationContext)
    val calibrationStartedAtMs = SystemClock.elapsedRealtime()
    val nativeSliceConfigJson = calibrationJob
        ?.applyTemporaryOverrides(nativeBuildResult.json)
        ?: nativeBuildResult.json
    val calibrationMs = SystemClock.elapsedRealtime() - calibrationStartedAtMs
    val plateStartedAtMs = SystemClock.elapsedRealtime()
    val plateSliceConfigResult = applyPlateFilamentSlotsToNativeConfigResult(
        configJson = nativeSliceConfigJson,
        slots = activePlateSlots,
        plateObjects = plateObjects,
        filaments = profileFilaments,
        flushVolumes = normalizedFlushVolumes
    )
    val plateMs = SystemClock.elapsedRealtime() - plateStartedAtMs
    val repairResult = repairNativeSliceConfigWithOrcaFilamentAssetsResult(
        context = context.applicationContext,
        printer = printer,
        filamentOverridesJson = repairFilamentOverridesJson,
        configJson = plateSliceConfigResult.json
    )
    if (VerboseSliceConfigTimingLogs) {
        Log.i(
            "MobileSlicer",
            "slice_config_prepare elapsedMs=${SystemClock.elapsedRealtime() - configStartedAtMs} " +
                "printer=${printer.name} " +
                "filaments=${activePlateSlots.size} " +
                "objects=${plateObjects.size} " +
                "native_cache=${if (nativeBuildResult.cacheHit) "hit" else "miss"} " +
                "native_keyMs=${nativeBuildResult.timing.keyMs} " +
                "native_profileJsonMs=${nativeBuildResult.timing.profileJsonMs} " +
                "native_mergeMs=${nativeBuildResult.timing.mergeMs} " +
                "native_defaultsMs=${nativeBuildResult.timing.nativeDefaultsMs} " +
                "native_normalizeMs=${nativeBuildResult.timing.normalizeMs} " +
                "native_totalMs=${nativeBuildResult.timing.totalMs} " +
                "calibrationMs=$calibrationMs " +
                "plate_cache=${if (plateSliceConfigResult.cacheHit) "hit" else "miss"} " +
                "plateMs=$plateMs " +
                "repair_cache=${if (repairResult.cacheHit) "hit" else "miss"} " +
                "repairMs=${repairResult.elapsedMs} " +
                "profile_override_counts=" +
                "printer:${configuration.printer.orcaMachineOverridesJson.jsonObjectLengthOrZero()}," +
                "filament:${configuration.filament.orcaFilamentOverridesJson.jsonObjectLengthOrZero()}," +
                "process:${configuration.process.orcaProcessOverridesJson.jsonObjectLengthOrZero()}"
        )
    }
    val result = onSliceRequested(
        repairResult.json,
        plateObjects,
        modelFilePath,
        preparedMesh,
        modelBounds,
        printer.toBedSpec(),
        modelTransform,
        gcodeFileName
    )
    return result.copy(
        message = buildString {
            append(result.message)
            append('\n')
            append("Native slice inputs: ")
            append(configuration.nativeSliceTitle())
            calibrationJob?.let { job ->
                append('\n')
                append("Calibration overrides: ")
                append(job.options.summary(job.type))
            }
            if (result.sliced) {
                append('\n')
                append(configuration.nativeSliceBody())
                append("\nWorkspace/app-only state: ")
                append(configuration.appLayerOnlyBody())
            }
        }
    )
}
