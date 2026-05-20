package com.mobileslicer

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.mobileslicer.calibration.CalibrationJob
import com.mobileslicer.profiles.ActiveSlicerConfiguration
import com.mobileslicer.profiles.FilamentProfile
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.profiles.ProcessProfile
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
import com.mobileslicer.workspace.PlateObjectModifierMesh
import com.mobileslicer.workspace.PlateObjectGeometrySource
import com.mobileslicer.workspace.PlateObjectProcessOverride
import com.mobileslicer.workspace.PrimeTowerPlacementOverride
import com.mobileslicer.workspace.SliceResult
import com.mobileslicer.workspace.applyToNativeConfig
import com.mobileslicer.workspace.defaultNativeModelTransform
import com.mobileslicer.workspace.primeTowerPlacementForWorkspace
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

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

internal fun strictSliceProfileIssue(
    configuration: ActiveSlicerConfiguration,
    printer: PrinterProfile,
    processProfiles: List<ProcessProfile>,
    profileFilaments: List<FilamentProfile>,
    activePlateSlots: List<PlateFilamentSlot>
): String? {
    if (configuration.printer.id != printer.id) {
        return "Active printer profile mismatch. Re-select the printer in Profiles before slicing."
    }
    if (processProfiles.none { process -> process.id == configuration.process.id && process.printerProfileId == printer.id }) {
        return "Selected process profile is missing for ${printer.name}. Re-select or import the correct process in Profiles before slicing."
    }
    if (activePlateSlots.isEmpty()) {
        return "No material slot is selected for ${printer.name}. Re-select or import the filament in Profiles before slicing."
    }
    val missingSlot = activePlateSlots.firstOrNull { slot ->
        profileFilaments.none { filament ->
            filament.id == slot.filamentProfileId && filament.printerProfileId == printer.id
        }
    }
    if (missingSlot != null) {
        return "Material slot ${missingSlot.index} is not backed by a filament profile for ${printer.name}. Re-select or import the filament in Profiles before slicing."
    }
    return null
}

internal suspend fun prepareNativeConfigForPlatePlanning(
    context: Context,
    configuration: ActiveSlicerConfiguration,
    plateObjects: List<PlateObject>,
    processProfiles: List<ProcessProfile>,
    profileFilaments: List<FilamentProfile>,
    activePlateSlots: List<PlateFilamentSlot>,
    flushVolumes: PlateFlushVolumes?,
    primeTowerPlacementOverride: PrimeTowerPlacementOverride?,
    printer: PrinterProfile
): String {
    val repairFilamentOverridesJson = activePlateSlots
        .firstOrNull()
        ?.filamentProfileId
        ?.let { slotFilamentId ->
            profileFilaments.firstOrNull { it.id == slotFilamentId }
        }
        ?.orcaFilamentOverridesJson
        ?: configuration.filament.orcaFilamentOverridesJson
    val nativeBuildResult = configuration.toNativeSliceConfigBuildResult(context.applicationContext)
    val plateSliceConfigResult = applyPlateFilamentSlotsToNativeConfigResult(
        configJson = nativeBuildResult.json,
        slots = activePlateSlots,
        plateObjects = plateObjects,
        filaments = profileFilaments,
        flushVolumes = ensureFlushVolumesForSlots(
            slots = activePlateSlots,
            existing = flushVolumes,
            regenerateFromColors = false
        )
    )
    val placement = primeTowerPlacementForWorkspace(
        configuration = configuration,
        plateObjects = plateObjects,
        filamentSlots = activePlateSlots,
        printerBed = printer.toBedSpec(),
        override = primeTowerPlacementOverride
    )
    val repairedJson = repairNativeSliceConfigWithOrcaFilamentAssetsResult(
        context = context.applicationContext,
        printer = printer,
        filamentOverridesJson = repairFilamentOverridesJson,
        configJson = placement?.applyToNativeConfig(plateSliceConfigResult.json) ?: plateSliceConfigResult.json
    ).json
    return applyObjectProcessOverridesToNativeConfig(
        configJson = repairedJson,
        plateObjects = plateObjects,
        defaultProcess = configuration.process,
        availableProcesses = processProfiles,
        printerBed = printer.toBedSpec()
    )
}

internal suspend fun runModelLoaderSlice(
    context: Context,
    configuration: ActiveSlicerConfiguration,
    calibrationJob: CalibrationJob?,
    plateObjects: List<PlateObject>,
    processProfiles: List<ProcessProfile>,
    profileFilaments: List<FilamentProfile>,
    activePlateSlots: List<PlateFilamentSlot>,
    flushVolumes: PlateFlushVolumes?,
    primeTowerPlacementOverride: PrimeTowerPlacementOverride?,
    printer: PrinterProfile,
    modelFilePath: String?,
    preparedMesh: StlMesh?,
    modelBounds: MeshBounds?,
    modelTransform: ViewerModelTransform?,
    gcodeFileName: String,
    onSliceRequested: ModelLoaderSliceRequestHandler
): SliceResult {
    strictSliceProfileIssue(
        configuration = configuration,
        printer = printer,
        processProfiles = processProfiles,
        profileFilaments = profileFilaments,
        activePlateSlots = activePlateSlots
    )?.let { issue ->
        Log.e("MobileSlicer", "slice_profile_blocked $issue")
        return SliceResult(
            message = "Slice blocked\n$issue",
            sliced = false
        )
    }
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
    val plateStartedAtMs = SystemClock.elapsedRealtime()
    val plateSliceConfigResult = applyPlateFilamentSlotsToNativeConfigResult(
        configJson = nativeBuildResult.json,
        slots = activePlateSlots,
        plateObjects = plateObjects,
        filaments = profileFilaments,
        flushVolumes = normalizedFlushVolumes
    )
    val primeTowerPlacement = primeTowerPlacementForWorkspace(
        configuration = configuration,
        plateObjects = plateObjects,
        filamentSlots = activePlateSlots,
        printerBed = printer.toBedSpec(),
        override = primeTowerPlacementOverride
    )
    val plateMs = SystemClock.elapsedRealtime() - plateStartedAtMs
    val repairResult = repairNativeSliceConfigWithOrcaFilamentAssetsResult(
        context = context.applicationContext,
        printer = printer,
        filamentOverridesJson = repairFilamentOverridesJson,
        configJson = primeTowerPlacement?.applyToNativeConfig(plateSliceConfigResult.json) ?: plateSliceConfigResult.json
    )
    val calibrationStartedAtMs = SystemClock.elapsedRealtime()
    val finalConfigJson = calibrationJob
        ?.applyTemporaryOverrides(repairResult.json)
        ?: repairResult.json
    val objectScopedConfigJson = applyObjectProcessOverridesToNativeConfig(
        configJson = finalConfigJson,
        plateObjects = plateObjects,
        defaultProcess = configuration.process,
        availableProcesses = processProfiles,
        printerBed = printer.toBedSpec()
    )
    val calibrationMs = SystemClock.elapsedRealtime() - calibrationStartedAtMs
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
        objectScopedConfigJson,
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
            append("Profiles: ")
            append(configuration.nativeSliceTitle())
            calibrationJob?.let { job ->
                append('\n')
                append("Calibration settings: ")
                append(job.options.summary(job.type))
            }
            if (result.sliced) {
                append('\n')
                append(configuration.nativeSliceBody())
            }
        }
    )
}

internal fun preservesImportedThreeMfProjectMaterials(plateObjects: List<PlateObject>): Boolean {
    if (plateObjects.size != 1) return false
    val objectOnPlate = plateObjects.single()
    val source = objectOnPlate.geometrySource as? PlateObjectGeometrySource.ThreeMfMeshExtract ?: return false
    if (source.originalPath.isBlank()) return false
    if (!File(source.originalPath).isFile) return false
    if (objectOnPlate.filamentSlotIndex != 1) return false
    if (objectOnPlate.paint.hasAnyPaintPayload) return false
    if (objectOnPlate.modifiers.any { it.enabled }) return false
    if (objectOnPlate.processOverride != null) return false
    return true
}

internal fun applyObjectProcessOverridesToNativeConfig(
    configJson: String,
    plateObjects: List<PlateObject>,
    defaultProcess: ProcessProfile? = null,
    availableProcesses: List<ProcessProfile> = emptyList(),
    printerBed: PrinterBedSpec? = null
): String {
    val objectEntries = JSONArray()
    val modifierEntries = JSONArray()
    plateObjects.forEachIndexed { index, objectOnPlate ->
        val override = objectOnPlate.processOverride ?: return@forEachIndexed
        val effectiveProcess = objectProcessForNativeOverrides(
            override = override,
            defaultProcess = defaultProcess,
            availableProcesses = availableProcesses
        ) ?: return@forEachIndexed
        val nativeOverrides = runCatching { JSONObject(effectiveProcess.orcaProcessOverridesJson.takeIf { it.isNotBlank() } ?: "{}") }.getOrNull()
            ?: return@forEachIndexed
        if (nativeOverrides.length() == 0) return@forEachIndexed
        objectEntries.put(
            JSONObject()
                .put("mobileObjectId", objectOnPlate.id)
                .put("plateObjectIndex", index)
                .put("selectedProcessId", override.selectedProcessId ?: effectiveProcess.id)
                .put("config", nativeOverrides)
        )
    }
    plateObjects.forEachIndexed { objectIndex, objectOnPlate ->
        objectOnPlate.modifiers.forEach { modifier ->
            val entry = modifier.toNativeModifierProcessEntry(
                plateObjectIndex = objectIndex,
                mobileObjectId = objectOnPlate.id,
                defaultProcess = defaultProcess,
                availableProcesses = availableProcesses,
                printerBed = printerBed
            ) ?: return@forEach
            modifierEntries.put(entry)
        }
    }
    if (objectEntries.length() == 0 && modifierEntries.length() == 0) return configJson
    return runCatching {
        val root = JSONObject(configJson)
        if (objectEntries.length() > 0) {
            root.put("mobile_slicer_object_process_overrides", objectEntries)
        }
        if (modifierEntries.length() > 0) {
            root.put("mobile_slicer_modifier_process_overrides", modifierEntries)
        }
        root.toString()
    }.getOrDefault(configJson)
}

private fun PlateObjectModifierMesh.toNativeModifierProcessEntry(
    plateObjectIndex: Int,
    mobileObjectId: Long,
    defaultProcess: ProcessProfile?,
    availableProcesses: List<ProcessProfile>,
    printerBed: PrinterBedSpec?
): JSONObject? {
    if (!enabled || filePath.isBlank()) return null
    val effectiveProcess = objectProcessForNativeOverrides(
        override = processOverride,
        defaultProcess = defaultProcess,
        availableProcesses = availableProcesses
    ) ?: return null
    val nativeOverrides = runCatching { JSONObject(effectiveProcess.orcaProcessOverridesJson.takeIf { it.isNotBlank() } ?: "{}") }.getOrNull()
        ?: return null
    if (nativeOverrides.length() == 0) return null
    return JSONObject()
        .put("mobileObjectId", mobileObjectId)
        .put("modifierId", id)
        .put("plateObjectIndex", plateObjectIndex)
        .put("label", label)
        .put("path", filePath)
        .put("selectedProcessId", processOverride.selectedProcessId ?: effectiveProcess.id)
        .put("config", nativeOverrides)
        .also { entry ->
            val modifierBounds = this.bounds
            if (modifierBounds != null && printerBed != null) {
                val nativeTransform = defaultNativeModelTransform(modifierBounds, printerBed, transform)
                entry.put(
                    "transform",
                    JSONObject()
                        .put("xMm", nativeTransform.xMm)
                        .put("yMm", nativeTransform.yMm)
                        .put("zMm", nativeTransform.zMm)
                        .put("rotationXRadians", nativeTransform.rotationXRadians)
                        .put("rotationYRadians", nativeTransform.rotationYRadians)
                        .put("rotationZRadians", nativeTransform.rotationZRadians)
                        .put("uniformScale", nativeTransform.uniformScale)
                        .also { transformJson ->
                            nativeTransform.orientationMatrix?.let { matrix ->
                                transformJson.put("orientationMatrix", JSONArray(matrix))
                            }
                        }
                )
            }
        }
}

private fun objectProcessForNativeOverrides(
    override: PlateObjectProcessOverride,
    defaultProcess: ProcessProfile?,
    availableProcesses: List<ProcessProfile>
): ProcessProfile? {
    override.editedProcessProfile?.let { return it }
    val defaultId = defaultProcess?.id.orEmpty()
    val selectedId = override.selectedProcessId?.takeIf { it.isNotBlank() } ?: return null
    if (selectedId == defaultId) return null
    return availableProcesses.firstOrNull { it.id == selectedId }
}
