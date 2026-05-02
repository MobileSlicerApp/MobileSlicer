package com.mobileslicer.profiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import com.mobileslicer.AppSettingOption
import com.mobileslicer.R
import com.mobileslicer.SoftPill
import com.mobileslicer.appBackgroundGradient
import com.mobileslicer.appBodyColor
import com.mobileslicer.appCardColor
import com.mobileslicer.appCardColorMuted
import com.mobileslicer.appMutedColor
import com.mobileslicer.appOutlineColor
import com.mobileslicer.appTitleColor
import com.mobileslicer.printerconnection.PrinterConnectionChoicesResult
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.mobileslicer.viewer.StlMesh
import com.mobileslicer.nativebridge.NativeEngineBridge
import com.mobileslicer.ui.theme.AccentPaletteOption
import com.mobileslicer.viewer.PrinterBedSpec
import com.mobileslicer.printerconnection.SimplyPrintOAuthResult
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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal fun OrcaPrinterPreset.toImportedPrinterProfile(
    bundle: OrcaPrinterImportBundle,
    selectedNozzleDiameterMm: Float
): PrinterProfile {
    val resolvedMachines = bundle.resolvedMachineJsons.mapNotNull { text ->
        runCatching { JSONObject(text) }.getOrNull()
    }
    val selectedIndex = resolvedMachines.indexOfFirst { resolvedMachine ->
        resolvedMachine.profileConfigFloat("nozzle_diameter", activeNozzleDiameterMm).let { nozzle ->
            kotlin.math.abs(nozzle - selectedNozzleDiameterMm) < 0.001f
        }
    }.takeIf { it >= 0 }
    val resolved = selectedIndex?.let { resolvedMachines[it] }
        ?: runCatching { JSONObject(bundle.resolvedMachineJson) }.getOrElse { JSONObject() }
    val machineModel = runCatching { JSONObject(bundle.machineModelJson) }.getOrElse { JSONObject() }
    val selectedNozzle = resolved.profileConfigFloat("nozzle_diameter", selectedNozzleDiameterMm)
    val nozzles = nozzleDiameters.split(';', ',')
        .mapNotNull { it.trim().toFloatOrNull() }
        .filter { it > 0f }
        .distinct()
        .sorted()
    val variantSuffix = formatNozzle(selectedNozzle)
    return PrinterProfile(
        id = "orca_${profilePath.hashCode().toUInt().toString(16)}_${variantSuffix.replace('.', '_')}",
        name = name,
        subtitle = "${variantSuffix} mm nozzle",
        builtIn = false,
        bedWidthMm = bedWidthMm,
        bedDepthMm = bedDepthMm,
        maxHeightMm = maxHeightMm,
        bedExcludeArea = resolved.profileConfigString("bed_exclude_area"),
        wrappingExcludeArea = resolved.profileConfigString("wrapping_exclude_area"),
        headWrapDetectZone = resolved.profileConfigString("head_wrap_detect_zone"),
        bedCustomTexture = resolved.profileConfigString("bed_custom_texture"),
        bedCustomModel = resolved.profileConfigString("bed_custom_model"),
        bedModel = resolved.profileConfigString("bed_model"),
        bedModelAssetPath = bedModelAssetPath,
        bedShape = resolved.profileConfigString("bed_shape"),
        bedTexture = resolved.profileConfigString("bed_texture"),
        bedTextureAssetPath = bedTextureAssetPath,
        bedTextureArea = resolved.profileConfigString("bed_texture_area"),
        bottomTextureRect = resolved.profileConfigString("bottom_texture_rect"),
        bottomTextureEndName = resolved.profileConfigString("bottom_texture_end_name"),
        imageBedType = resolved.profileConfigString("image_bed_type"),
        useDoubleExtruderDefaultTexture = resolved.profileConfigString("use_double_extruder_default_texture"),
        useRectGrid = resolved.profileConfigBoolean("use_rect_grid", false),
        supportMultiBedTypes = resolved.profileConfigBoolean(
            "support_multi_bed_types",
            machineModel.profileConfigBoolean("support_multi_bed_types", false)
        ),
        defaultBedType = DefaultBedType.fromConfigValue(
            resolved.profileConfigString("default_bed_type")
                .ifBlank { machineModel.profileConfigString("default_bed_type") }
        ),
        bestObjectPosition = resolved.profileConfigString("best_object_pos", "0.5x0.5"),
        zOffsetMm = resolved.profileConfigFloat("z_offset", 0f),
        preferredOrientationDegrees = resolved.profileConfigFloat("preferred_orientation", 0f),
        bedMeshMin = resolved.profileConfigPointString("bed_mesh_min", "-99999,-99999"),
        bedMeshMax = resolved.profileConfigPointString("bed_mesh_max", "99999,99999"),
        bedMeshProbeDistance = resolved.profileConfigPointString("bed_mesh_probe_distance", "50,50"),
        adaptiveBedMeshMarginMm = resolved.profileConfigFloat("adaptive_bed_mesh_margin", 0f),
        nozzleDiameterMm = selectedNozzle,
        filamentDiameterMm = resolved.profileConfigFloat("filament_diameter", 1.75f),
        nozzleVolumeMm3 = resolved.profileConfigFloat("nozzle_volume", 0f),
        nozzleVolumeType = NozzleVolumeType.fromConfigValue(resolved.profileConfigString("nozzle_volume_type", "Standard")),
        nozzleHeightMm = resolved.profileConfigFloat("nozzle_height", 2.5f),
        grabLengthMm = resolved.profileConfigFloat("grab_length", 0f),
        extruderVariantList = resolved.profileConfigString("extruder_variant_list", "Direct Drive Standard"),
        printerExtruderId = resolved.profileConfigString("printer_extruder_id", "1"),
        printerExtruderVariant = resolved.profileConfigString("printer_extruder_variant", "Direct Drive Standard"),
        masterExtruderId = resolved.profileConfigInt("master_extruder_id", 1),
        physicalExtruderMap = resolved.profileConfigString("physical_extruder_map", "0"),
        extrudersCount = resolved.profileConfigString("extruders_count"),
        extruderAmsCount = resolved.profileConfigString("extruder_ams_count"),
        extruderMaxNozzleCount = resolved.profileConfigString("extruder_max_nozzle_count"),
        extruderType = ExtruderType.fromConfigValue(resolved.profileConfigString("extruder_type", ExtruderType.DirectDrive.configValue)),
        extruderColor = resolved.profileConfigString("extruder_colour"),
        extruderPrintableHeightMm = resolved.profileConfigFloat("extruder_printable_height", 0f),
        extruderPrintableArea = resolved.profileConfigString("extruder_printable_area"),
        minLayerHeightMm = resolved.profileConfigFloat("min_layer_height", 0.07f),
        maxLayerHeightMm = resolved.profileConfigFloat("max_layer_height", 0f),
        extruderOffset = resolved.profileConfigString("extruder_offset", "0x0"),
        printerModel = resolved.profileConfigString("printer_model"),
        machineTech = resolved.profileConfigString("machine_tech"),
        machineFamily = resolved.profileConfigString("family"),
        printerTechnology = PrinterTechnology.fromConfigValue(resolved.profileConfigString("printer_technology", PrinterTechnology.Fff.configValue)),
        printerVariant = resolved.profileConfigString("printer_variant"),
        hotendModel = resolved.profileConfigString("hotend_model"),
        boxId = resolved.profileConfigString("box_id"),
        enablePreHeating = resolved.profileConfigBoolean("enable_pre_heating", false),
        fanDirection = resolved.profileConfigString("fan_direction"),
        hotendCoolingRate = resolved.profileConfigString("hotend_cooling_rate"),
        hotendHeatingRate = resolved.profileConfigString("hotend_heating_rate"),
        activeFeederMotorName = resolved.profileConfigString("active_feeder_motor_name"),
        autoDisableFilterOnOverheat = resolved.profileConfigString("auto_disable_filter_on_overheat"),
        autoToolchangeCommand = resolved.profileConfigString("auto_toolchange_command"),
        coolingFilterEnabled = resolved.profileConfigString("cooling_filter_enabled"),
        crealityFlushTime = resolved.profileConfigString("creality_flush_time"),
        groupAlgoWithTime = resolved.profileConfigString("group_algo_with_time"),
        isArtillery = resolved.profileConfigString("is_artillery"),
        isSupport3mf = resolved.profileConfigString("is_support_3mf"),
        isSupportAirCondition = resolved.profileConfigString("is_support_air_condition"),
        isSupportMqtt = resolved.profileConfigString("is_support_mqtt"),
        isSupportMultiBox = resolved.profileConfigString("is_support_multi_box"),
        isSupportTimelapse = resolved.profileConfigString("is_support_timelapse"),
        machineLedLightExist = resolved.profileConfigString("machine_LED_light_exist"),
        machineHotendChangeTime = resolved.profileConfigString("machine_hotend_change_time"),
        machinePlatformMotionEnable = resolved.profileConfigString("machine_platform_motion_enable"),
        machinePrepareCompensationTime = resolved.profileConfigString("machine_prepare_compensation_time"),
        multiZone = resolved.profileConfigString("multi_zone"),
        multiZoneNumber = resolved.profileConfigString("multi_zone_number"),
        nozzleFlushDataset = resolved.profileConfigString("nozzle_flush_dataset"),
        rammingPressureAdvanceValue = resolved.profileConfigString("ramming_pressure_advance_value"),
        rightIconOffsetBed = resolved.profileConfigString("right_icon_offset_bed"),
        scanFolder = resolved.profileConfigString("scan_folder"),
        supportBoxTempControl = resolved.profileConfigString("support_box_temp_control"),
        supportCoolingFilter = resolved.profileConfigString("support_cooling_filter"),
        supportMultiFilament = resolved.profileConfigString("support_multi_filament"),
        supportObjectSkipFlush = resolved.profileConfigString("support_object_skip_flush"),
        supportWanNetwork = resolved.profileConfigString("support_wan_network"),
        toolChangeTemperatureWait = resolved.profileConfigString("tool_change_temprature_wait"),
        upwardCompatibleMachine = resolved.profileConfigString("upward_compatible_machine"),
        vendorUrl = resolved.profileConfigString("url"),
        useActivePelletFeeding = resolved.profileConfigString("use_active_pellet_feeding"),
        useExtruderRotationVolume = resolved.profileConfigString("use_extruder_rotation_volume"),
        printerStructure = PrinterStructure.fromConfigValue(resolved.profileConfigString("printer_structure", PrinterStructure.Undefined.configValue)),
        gcodeFlavor = GcodeFlavor.fromConfigValue(resolved.profileConfigString("gcode_flavor", GcodeFlavor.MarlinLegacy.configValue)),
        pelletModdedPrinter = resolved.profileConfigBoolean("pellet_modded_printer", false),
        useThirdPartyPrintHost = resolved.profileConfigBoolean("bbl_use_printhost", false),
        scanFirstLayer = resolved.profileConfigBoolean("scan_first_layer", false),
        useRelativeEDistances = resolved.profileConfigBoolean("use_relative_e_distances", true),
        useFirmwareRetraction = resolved.profileConfigBoolean("use_firmware_retraction", false),
        powerLossRecoveryMode = PowerLossRecoveryMode.fromConfigValue(resolved.profileConfigString("enable_power_loss_recovery", PowerLossRecoveryMode.PrinterConfiguration.configValue)),
        disableM73 = resolved.profileConfigBoolean("disable_m73", false),
        thumbnails = resolved.profileConfigString("thumbnails", resolved.profileConfigString("thumbnail_size", "48x48/PNG,300x300/PNG")),
        thumbnailsInternal = resolved.profileConfigString("thumbnails_internal"),
        thumbnailsInternalSwitch = resolved.profileConfigString("thumbnails_internal_switch"),
        remainingTimes = resolved.profileConfigString("remaining_times"),
        printHostType = PrintHostType.fromConfigValue(resolved.profileConfigString("host_type", PrintHostType.OctoPrint.configValue)),
        printerAgent = resolved.profileConfigString("printer_agent"),
        printHost = resolved.profileConfigString("print_host"),
        printHostWebUi = resolved.profileConfigString("print_host_webui"),
        printHostAuthorizationType = PrintHostAuthorizationType.fromConfigValue(resolved.profileConfigString("printhost_authorization_type", PrintHostAuthorizationType.Key.configValue)),
        printHostApiKey = resolved.profileConfigString("printhost_apikey"),
        printHostPort = resolved.profileConfigString("printhost_port"),
        printHostGroup = resolved.profileConfigString("printhost_group"),
        printHostCaFile = resolved.profileConfigString("printhost_cafile"),
        printHostUser = resolved.profileConfigString("printhost_user"),
        printHostPassword = resolved.profileConfigString("printhost_password"),
        printHostSslIgnoreRevoke = resolved.profileConfigBoolean("printhost_ssl_ignore_revoke", false),
        timeCost = resolved.profileConfigFloat("time_cost", 0f),
        fanSpeedupTimeSeconds = resolved.profileConfigFloat("fan_speedup_time", 0f),
        fanSpeedupOverhangsOnly = resolved.profileConfigBoolean("fan_speedup_overhangs", true),
        fanKickstartTimeSeconds = resolved.profileConfigFloat("fan_kickstart", 0f),
        extruderClearanceRadiusMm = resolved.profileConfigFloat("extruder_clearance_radius", resolved.profileConfigFloat("extruder_clearance_max_radius", 40f)),
        extruderClearanceHeightToRodMm = resolved.profileConfigFloat("extruder_clearance_height_to_rod", 40f),
        extruderClearanceHeightToLidMm = resolved.profileConfigFloat("extruder_clearance_height_to_lid", 120f),
        extruderClearanceDistToRodMm = resolved.profileConfigFloat("extruder_clearance_dist_to_rod", 0f),
        nozzleType = NozzleType.fromConfigValue(resolved.profileConfigString("nozzle_type", NozzleType.Undefined.configValue)),
        nozzleHrc = resolved.profileConfigInt("nozzle_hrc", 0),
        auxiliaryFan = resolved.profileConfigBoolean("auxiliary_fan", false),
        supportChamberTempControl = resolved.profileConfigBoolean("support_chamber_temp_control", true),
        supportAirFiltration = resolved.profileConfigBoolean("support_air_filtration", true),
        singleExtruderMultiMaterial = resolved.profileConfigBoolean("single_extruder_multi_material", true),
        manualFilamentChange = resolved.profileConfigBoolean("manual_filament_change", false),
        bedTemperatureFormula = BedTemperatureFormula.fromConfigValue(resolved.profileConfigString("bed_temperature_formula", BedTemperatureFormula.ByHighestTemp.configValue)),
        wipeTowerType = WipeTowerType.fromConfigValue(resolved.profileConfigString("wipe_tower_type", WipeTowerType.Type2.configValue)),
        purgeInPrimeTower = resolved.profileConfigBoolean("purge_in_prime_tower", true),
        enableFilamentRamming = resolved.profileConfigBoolean("enable_filament_ramming", true),
        coolingTubeRetractionMm = resolved.profileConfigFloat("cooling_tube_retraction", 91.5f),
        coolingTubeLengthMm = resolved.profileConfigFloat("cooling_tube_length", 5f),
        parkingPositionRetractionMm = resolved.profileConfigFloat("parking_pos_retraction", 92f),
        extraLoadingMoveMm = resolved.profileConfigFloat("extra_loading_move", -2f),
        highCurrentOnFilamentSwap = resolved.profileConfigBoolean("high_current_on_filament_swap", false),
        machineLoadFilamentTimeSeconds = resolved.profileConfigFloat("machine_load_filament_time", 0f),
        machineUnloadFilamentTimeSeconds = resolved.profileConfigFloat("machine_unload_filament_time", 0f),
        machineToolChangeTimeSeconds = resolved.profileConfigFloat("machine_tool_change_time", resolved.profileConfigFloat("machine_switch_extruder_time", 0f)),
        fileStartGcode = resolved.profileConfigString("file_start_gcode"),
        machineStartGcode = resolved.profileConfigString("machine_start_gcode"),
        machineEndGcode = resolved.profileConfigString("machine_end_gcode"),
        printingByObjectGcode = resolved.profileConfigString("printing_by_object_gcode"),
        beforeLayerChangeGcode = resolved.profileConfigString("before_layer_change_gcode"),
        layerChangeGcode = resolved.profileConfigString("layer_change_gcode"),
        timeLapseGcode = resolved.profileConfigString("time_lapse_gcode"),
        wrappingDetectionGcode = resolved.profileConfigString("wrapping_detection_gcode"),
        changeFilamentGcode = resolved.profileConfigString("change_filament_gcode", resolved.profileConfigString("toolchange_gcode")),
        changeExtrusionRoleGcode = resolved.profileConfigString("change_extrusion_role_gcode"),
        machinePauseGcode = resolved.profileConfigString("machine_pause_gcode", resolved.profileConfigString("pause_gcode")),
        templateCustomGcode = resolved.profileConfigString("template_custom_gcode"),
        emitMachineLimitsToGcode = resolved.profileConfigBoolean("emit_machine_limits_to_gcode", true),
        resonanceAvoidance = resolved.profileConfigBoolean("resonance_avoidance", false),
        silentMode = resolved.profileConfigBoolean("silent_mode", false),
        minResonanceAvoidanceSpeedMmPerSec = resolved.profileConfigFloat("min_resonance_avoidance_speed", 70f),
        maxResonanceAvoidanceSpeedMmPerSec = resolved.profileConfigFloat("max_resonance_avoidance_speed", 120f),
        machineMaxSpeedX = resolved.profileConfigFloat("machine_max_speed_x", 500f),
        machineMaxSpeedY = resolved.profileConfigFloat("machine_max_speed_y", 500f),
        machineMaxSpeedZ = resolved.profileConfigFloat("machine_max_speed_z", 12f),
        machineMaxSpeedE = resolved.profileConfigFloat("machine_max_speed_e", 120f),
        machineMaxAccelerationX = resolved.profileConfigFloat("machine_max_acceleration_x", 1000f),
        machineMaxAccelerationY = resolved.profileConfigFloat("machine_max_acceleration_y", 1000f),
        machineMaxAccelerationZ = resolved.profileConfigFloat("machine_max_acceleration_z", 500f),
        machineMaxAccelerationE = resolved.profileConfigFloat("machine_max_acceleration_e", 5000f),
        machineMaxAccelerationExtruding = resolved.profileConfigFloat("machine_max_acceleration_extruding", 1500f),
        machineMaxAccelerationRetracting = resolved.profileConfigFloat("machine_max_acceleration_retracting", 1500f),
        machineMaxAccelerationTravel = resolved.profileConfigFloat("machine_max_acceleration_travel", 0f),
        machineMaxJerkX = resolved.profileConfigFloat("machine_max_jerk_x", 10f),
        machineMaxJerkY = resolved.profileConfigFloat("machine_max_jerk_y", 10f),
        machineMaxJerkZ = resolved.profileConfigFloat("machine_max_jerk_z", 0.2f),
        machineMaxJerkE = resolved.profileConfigFloat("machine_max_jerk_e", 2.5f),
        machineMaxJunctionDeviation = resolved.profileConfigFloat("machine_max_junction_deviation", 0.01f),
        machineMinExtrudingRateMmPerSec = resolved.profileConfigFloat("machine_min_extruding_rate", 0f),
        machineMinTravelRateMmPerSec = resolved.profileConfigFloat("machine_min_travel_rate", 0f),
        retractionLengthMm = resolved.profileConfigFloat("retraction_length", 0.8f),
        retractRestartExtraMm = resolved.profileConfigFloat("retract_restart_extra", 0f),
        retractionSpeedMmPerSec = resolved.profileConfigFloat("retraction_speed", 30f),
        deretractionSpeedMmPerSec = resolved.profileConfigFloat("deretraction_speed", 0f),
        retractionMinimumTravelMm = resolved.profileConfigFloat("retraction_minimum_travel", 2f),
        retractWhenChangingLayer = resolved.profileConfigBoolean("retract_when_changing_layer", false),
        retractOnTopLayer = resolved.profileConfigString("retract_on_top_layer"),
        wipe = resolved.profileConfigBoolean("wipe", false),
        wipeDistanceMm = resolved.profileConfigFloat("wipe_distance", 1f),
        retractBeforeWipePercent = resolved.profileConfigInt("retract_before_wipe", 100),
        retractLiftEnforce = RetractLiftEnforce.fromConfigValue(resolved.profileConfigString("retract_lift_enforce", RetractLiftEnforce.AllSurfaces.configValue)),
        zHopType = ZHopType.fromConfigValue(resolved.profileConfigString("z_hop_types", ZHopType.Slope.configValue)),
        zHopWhenPrime = resolved.profileConfigString("z_hop_when_prime"),
        zLiftType = resolved.profileConfigString("z_lift_type"),
        zHopMm = resolved.profileConfigFloat("z_hop", 0.4f),
        travelSlopeDegrees = resolved.profileConfigFloat("travel_slope", 3f),
        retractLiftAboveMm = resolved.profileConfigFloat("retract_lift_above", 0f),
        retractLiftBelowMm = resolved.profileConfigFloat("retract_lift_below", 0f),
        retractLengthToolchangeMm = resolved.profileConfigFloat("retract_length_toolchange", 10f),
        retractRestartExtraToolchangeMm = resolved.profileConfigFloat("retract_restart_extra_toolchange", 0f),
        enableLongRetractionWhenCut = LongRetractionWhenCutMode.fromConfigValue(resolved.profileConfigInt("enable_long_retraction_when_cut", 0)),
        longRetractionsWhenCut = resolved.profileConfigBoolean("long_retractions_when_cut", false),
        retractionDistanceWhenCutMm = resolved.profileConfigFloat("retraction_distances_when_cut", 18f),
        printerNotes = resolved.profileConfigString("printer_notes"),
        profileSource = "orca",
        thumbnailAssetPath = coverAssetPath,
        orcaFamily = family,
        orcaMachineModelPath = profilePath,
        orcaMachineModelJson = bundle.machineModelJson,
        orcaResolvedMachineJson = resolved.toString(),
        orcaNozzleMachinePaths = nozzleMachinePaths,
        orcaNozzleMachineJsons = bundle.nozzleMachineJsons,
        orcaResolvedMachineJsons = bundle.resolvedMachineJsons,
        orcaResolvedSourceChains = selectedIndex?.let { listOf(resolvedSourceChains.getOrNull(it).orEmpty()) } ?: resolvedSourceChains,
        availableNozzleDiameters = nozzles
    )
}
