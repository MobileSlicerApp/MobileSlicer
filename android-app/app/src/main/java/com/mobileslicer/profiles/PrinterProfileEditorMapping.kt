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
import com.mobileslicer.printerconnection.PrinterConnectionChoice
import com.mobileslicer.printerconnection.PrinterConnectionChoicesResult
import com.mobileslicer.printerconnection.SimplyPrintOAuthResult
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



internal fun PrinterProfileEditorDraft.toPrinterProfile(
    initial: PrinterProfile,
    isNew: Boolean
): PrinterProfile =
    initial.copy(
                    name = name.ifBlank { initial.name },
                    subtitle = subtitle.ifBlank { "Custom printer profile" },
                    builtIn = initial.builtIn && !isNew,
                    bedWidthMm = bedWidth.toFloatOrNull()?.coerceAtLeast(80f) ?: initial.bedWidthMm,
                    bedDepthMm = bedDepth.toFloatOrNull()?.coerceAtLeast(80f) ?: initial.bedDepthMm,
                    maxHeightMm = maxHeight.toFloatOrNull()?.coerceAtLeast(80f) ?: initial.maxHeightMm,
                    bedExcludeArea = bedExcludeArea,
                    wrappingExcludeArea = wrappingExcludeArea,
                    headWrapDetectZone = headWrapDetectZone,
                    bedCustomTexture = bedCustomTexture.trim(),
                    bedCustomModel = bedCustomModel.trim(),
                    bedModel = bedModel.trim(),
                    bedShape = bedShape.trim(),
                    bedTexture = bedTexture.trim(),
                    bedTextureArea = bedTextureArea.trim(),
                    bottomTextureRect = bottomTextureRect.trim(),
                    bottomTextureEndName = bottomTextureEndName.trim(),
                    imageBedType = imageBedType.trim(),
                    useDoubleExtruderDefaultTexture = useDoubleExtruderDefaultTexture.trim(),
                    useRectGrid = useRectGrid,
                    supportMultiBedTypes = supportMultiBedTypes,
                    defaultBedType = defaultBedType,
                    bestObjectPosition = bestObjectPosition.ifBlank { initial.bestObjectPosition },
                    zOffsetMm = zOffset.toFloatOrNull() ?: initial.zOffsetMm,
                    preferredOrientationDegrees = preferredOrientation.parseFloatIn(-360f, 360f) ?: initial.preferredOrientationDegrees,
                    bedMeshMin = bedMeshMin.ifBlank { initial.bedMeshMin },
                    bedMeshMax = bedMeshMax.ifBlank { initial.bedMeshMax },
                    bedMeshProbeDistance = bedMeshProbeDistance.ifBlank { initial.bedMeshProbeDistance },
                    adaptiveBedMeshMarginMm = adaptiveBedMeshMargin.parseFloatAtLeast(0f) ?: initial.adaptiveBedMeshMarginMm,
                    nozzleDiameterMm = nozzleDiameter.toFloatOrNull()?.coerceAtLeast(0.2f) ?: initial.nozzleDiameterMm,
                    filamentDiameterMm = filamentDiameter.toFloatOrNull()?.coerceAtLeast(1f) ?: initial.filamentDiameterMm,
                    nozzleVolumeMm3 = nozzleVolume.parseFloatAtLeast(0f) ?: initial.nozzleVolumeMm3,
                    nozzleVolumeType = nozzleVolumeType,
                    nozzleHeightMm = nozzleHeight.parseFloatAtLeast(0f) ?: initial.nozzleHeightMm,
                    grabLengthMm = grabLength.parseFloatAtLeast(0f) ?: initial.grabLengthMm,
                    extruderVariantList = extruderVariantList.ifBlank { initial.extruderVariantList },
                    printerExtruderId = printerExtruderId.ifBlank { initial.printerExtruderId },
                    printerExtruderVariant = printerExtruderVariant.ifBlank { initial.printerExtruderVariant },
                    masterExtruderId = masterExtruderId.parseIntAtLeast(1) ?: initial.masterExtruderId,
                    physicalExtruderMap = physicalExtruderMap.ifBlank { initial.physicalExtruderMap },
                    extrudersCount = extrudersCount.trim(),
                    extruderAmsCount = extruderAmsCount,
                    extruderMaxNozzleCount = extruderMaxNozzleCount.trim(),
                    extruderType = extruderType,
                    extruderColor = extruderColor.trim(),
                    extruderPrintableHeightMm = extruderPrintableHeight.parseFloatAtLeast(0f) ?: initial.extruderPrintableHeightMm,
                    extruderPrintableArea = extruderPrintableArea,
                    minLayerHeightMm = minLayerHeight.parseFloatAtLeast(0f) ?: initial.minLayerHeightMm,
                    maxLayerHeightMm = maxLayerHeight.parseFloatAtLeast(0f) ?: initial.maxLayerHeightMm,
                    extruderOffset = extruderOffset.ifBlank { initial.extruderOffset },
                    printerModel = printerModel.trim(),
                    machineTech = machineTech.trim(),
                    machineFamily = machineFamily.trim(),
                    printerTechnology = printerTechnology,
                    printerVariant = printerVariant.trim(),
                    hotendModel = hotendModel.trim(),
                    boxId = boxId.trim(),
                    enablePreHeating = enablePreHeating,
                    fanDirection = fanDirection.trim(),
                    hotendCoolingRate = hotendCoolingRate.trim(),
                    hotendHeatingRate = hotendHeatingRate.trim(),
                    activeFeederMotorName = activeFeederMotorName.trim(),
                    autoDisableFilterOnOverheat = autoDisableFilterOnOverheat.trim(),
                    autoToolchangeCommand = autoToolchangeCommand.trim(),
                    coolingFilterEnabled = coolingFilterEnabled.trim(),
                    crealityFlushTime = crealityFlushTime.trim(),
                    groupAlgoWithTime = groupAlgoWithTime.trim(),
                    isArtillery = isArtillery.trim(),
                    isSupport3mf = isSupport3mf.trim(),
                    isSupportAirCondition = isSupportAirCondition.trim(),
                    isSupportMqtt = isSupportMqtt.trim(),
                    isSupportMultiBox = isSupportMultiBox.trim(),
                    isSupportTimelapse = isSupportTimelapse.trim(),
                    machineLedLightExist = machineLedLightExist.trim(),
                    machineHotendChangeTime = machineHotendChangeTime.trim(),
                    machinePlatformMotionEnable = machinePlatformMotionEnable.trim(),
                    machinePrepareCompensationTime = machinePrepareCompensationTime.trim(),
                    multiZone = multiZone.trim(),
                    multiZoneNumber = multiZoneNumber.trim(),
                    nozzleFlushDataset = nozzleFlushDataset.trim(),
                    rammingPressureAdvanceValue = rammingPressureAdvanceValue.trim(),
                    rightIconOffsetBed = rightIconOffsetBed.trim(),
                    scanFolder = scanFolder.trim(),
                    supportBoxTempControl = supportBoxTempControl.trim(),
                    supportCoolingFilter = supportCoolingFilter.trim(),
                    supportMultiFilament = supportMultiFilament.trim(),
                    supportObjectSkipFlush = supportObjectSkipFlush.trim(),
                    supportWanNetwork = supportWanNetwork.trim(),
                    toolChangeTemperatureWait = toolChangeTemperatureWait.trim(),
                    upwardCompatibleMachine = upwardCompatibleMachine.trim(),
                    vendorUrl = vendorUrl.trim(),
                    useActivePelletFeeding = useActivePelletFeeding.trim(),
                    useExtruderRotationVolume = useExtruderRotationVolume.trim(),
                    printerStructure = printerStructure,
                    gcodeFlavor = gcodeFlavor,
                    pelletModdedPrinter = pelletModdedPrinter,
                    useThirdPartyPrintHost = useThirdPartyPrintHost,
                    scanFirstLayer = scanFirstLayer,
                    useRelativeEDistances = useRelativeEDistances,
                    useFirmwareRetraction = useFirmwareRetraction,
                    powerLossRecoveryMode = powerLossRecoveryMode,
                    disableM73 = disableM73,
                    thumbnails = thumbnails,
                    thumbnailsInternal = thumbnailsInternal.trim(),
                    thumbnailsInternalSwitch = thumbnailsInternalSwitch.trim(),
                    remainingTimes = remainingTimes.trim(),
                    printHostType = printHostType,
                    printerAgent = printerAgent.trim(),
                    printHost = printHost.trim(),
                    printHostWebUi = printHostWebUi.trim(),
                    printHostAuthorizationType = printHostAuthorizationType,
                    printHostApiKey = printHostApiKey,
                    printHostPort = printHostPort,
                    printHostGroup = printHostGroup.trim(),
                    printHostCaFile = printHostCaFile.trim(),
                    printHostUser = printHostUser,
                    printHostPassword = printHostPassword,
                    printHostSslIgnoreRevoke = printHostSslIgnoreRevoke,
                    timeCost = timeCost.parseFloatAtLeast(0f) ?: initial.timeCost,
                    fanSpeedupTimeSeconds = fanSpeedupTime.parseFloatAtLeast(0f) ?: initial.fanSpeedupTimeSeconds,
                    fanSpeedupOverhangsOnly = fanSpeedupOverhangsOnly,
                    fanKickstartTimeSeconds = fanKickstartTime.parseFloatAtLeast(0f) ?: initial.fanKickstartTimeSeconds,
                    extruderClearanceRadiusMm = extruderClearanceRadius.parseFloatAtLeast(0f) ?: initial.extruderClearanceRadiusMm,
                    extruderClearanceHeightToRodMm = extruderClearanceHeightToRod.parseFloatAtLeast(0f) ?: initial.extruderClearanceHeightToRodMm,
                    extruderClearanceHeightToLidMm = extruderClearanceHeightToLid.parseFloatAtLeast(0f) ?: initial.extruderClearanceHeightToLidMm,
                    extruderClearanceDistToRodMm = extruderClearanceDistToRod.parseFloatAtLeast(0f) ?: initial.extruderClearanceDistToRodMm,
                    nozzleType = nozzleType,
                    nozzleHrc = nozzleHrc.toIntOrNull()?.coerceIn(0, 500) ?: initial.nozzleHrc,
                    auxiliaryFan = auxiliaryFan,
                    supportChamberTempControl = supportChamberTempControl,
                    supportAirFiltration = supportAirFiltration,
                    singleExtruderMultiMaterial = singleExtruderMultiMaterial,
                    manualFilamentChange = manualFilamentChange,
                    bedTemperatureFormula = bedTemperatureFormula,
                    wipeTowerType = wipeTowerType,
                    purgeInPrimeTower = purgeInPrimeTower,
                    enableFilamentRamming = enableFilamentRamming,
                    coolingTubeRetractionMm = coolingTubeRetraction.parseFloatAtLeast(0f) ?: initial.coolingTubeRetractionMm,
                    coolingTubeLengthMm = coolingTubeLength.parseFloatAtLeast(0f) ?: initial.coolingTubeLengthMm,
                    parkingPositionRetractionMm = parkingPositionRetraction.parseFloatAtLeast(0f) ?: initial.parkingPositionRetractionMm,
                    extraLoadingMoveMm = extraLoadingMove.toFloatOrNull() ?: initial.extraLoadingMoveMm,
                    highCurrentOnFilamentSwap = highCurrentOnFilamentSwap,
                    machineLoadFilamentTimeSeconds = machineLoadFilamentTime.parseFloatAtLeast(0f) ?: initial.machineLoadFilamentTimeSeconds,
                    machineUnloadFilamentTimeSeconds = machineUnloadFilamentTime.parseFloatAtLeast(0f) ?: initial.machineUnloadFilamentTimeSeconds,
                    machineToolChangeTimeSeconds = machineToolChangeTime.parseFloatAtLeast(0f) ?: initial.machineToolChangeTimeSeconds,
                    fileStartGcode = fileStartGcode,
                    machineStartGcode = machineStartGcode,
                    machineEndGcode = machineEndGcode,
                    printingByObjectGcode = printingByObjectGcode,
                    beforeLayerChangeGcode = beforeLayerChangeGcode,
                    layerChangeGcode = layerChangeGcode,
                    timeLapseGcode = timeLapseGcode,
                    wrappingDetectionGcode = wrappingDetectionGcode,
                    changeFilamentGcode = changeFilamentGcode,
                    changeExtrusionRoleGcode = changeExtrusionRoleGcode,
                    machinePauseGcode = machinePauseGcode,
                    templateCustomGcode = templateCustomGcode,
                    emitMachineLimitsToGcode = emitMachineLimitsToGcode,
                    resonanceAvoidance = resonanceAvoidance,
                    silentMode = silentMode,
                    minResonanceAvoidanceSpeedMmPerSec = minResonanceAvoidanceSpeed.parseFloatAtLeast(0f) ?: initial.minResonanceAvoidanceSpeedMmPerSec,
                    maxResonanceAvoidanceSpeedMmPerSec = maxResonanceAvoidanceSpeed.parseFloatAtLeast(0f) ?: initial.maxResonanceAvoidanceSpeedMmPerSec,
                    machineMaxSpeedX = machineMaxSpeedX.parseFloatAtLeast(0f) ?: initial.machineMaxSpeedX,
                    machineMaxSpeedY = machineMaxSpeedY.parseFloatAtLeast(0f) ?: initial.machineMaxSpeedY,
                    machineMaxSpeedZ = machineMaxSpeedZ.parseFloatAtLeast(0f) ?: initial.machineMaxSpeedZ,
                    machineMaxSpeedE = machineMaxSpeedE.parseFloatAtLeast(0f) ?: initial.machineMaxSpeedE,
                    machineMaxAccelerationX = machineMaxAccelerationX.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationX,
                    machineMaxAccelerationY = machineMaxAccelerationY.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationY,
                    machineMaxAccelerationZ = machineMaxAccelerationZ.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationZ,
                    machineMaxAccelerationE = machineMaxAccelerationE.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationE,
                    machineMaxAccelerationExtruding = machineMaxAccelerationExtruding.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationExtruding,
                    machineMaxAccelerationRetracting = machineMaxAccelerationRetracting.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationRetracting,
                    machineMaxAccelerationTravel = machineMaxAccelerationTravel.parseFloatAtLeast(0f) ?: initial.machineMaxAccelerationTravel,
                    machineMaxJerkX = machineMaxJerkX.parseFloatAtLeast(0f) ?: initial.machineMaxJerkX,
                    machineMaxJerkY = machineMaxJerkY.parseFloatAtLeast(0f) ?: initial.machineMaxJerkY,
                    machineMaxJerkZ = machineMaxJerkZ.parseFloatAtLeast(0f) ?: initial.machineMaxJerkZ,
                    machineMaxJerkE = machineMaxJerkE.parseFloatAtLeast(0f) ?: initial.machineMaxJerkE,
                    machineMaxJunctionDeviation = machineMaxJunctionDeviation.parseFloatIn(0f, 0.3f) ?: initial.machineMaxJunctionDeviation,
                    machineMinExtrudingRateMmPerSec = machineMinExtrudingRate.parseFloatAtLeast(0f) ?: initial.machineMinExtrudingRateMmPerSec,
                    machineMinTravelRateMmPerSec = machineMinTravelRate.parseFloatAtLeast(0f) ?: initial.machineMinTravelRateMmPerSec,
                    retractionLengthMm = retractionLength.parseFloatAtLeast(0f) ?: initial.retractionLengthMm,
                    retractRestartExtraMm = retractRestartExtra.toFloatOrNull() ?: initial.retractRestartExtraMm,
                    retractionSpeedMmPerSec = retractionSpeed.parseFloatAtLeast(0f) ?: initial.retractionSpeedMmPerSec,
                    deretractionSpeedMmPerSec = deretractionSpeed.parseFloatAtLeast(0f) ?: initial.deretractionSpeedMmPerSec,
                    retractionMinimumTravelMm = retractionMinimumTravel.parseFloatAtLeast(0f) ?: initial.retractionMinimumTravelMm,
                    retractWhenChangingLayer = retractWhenChangingLayer,
                    retractOnTopLayer = retractOnTopLayer.trim(),
                    wipe = wipe,
                    wipeDistanceMm = wipeDistance.parseFloatAtLeast(0f) ?: initial.wipeDistanceMm,
                    retractBeforeWipePercent = retractBeforeWipe.parseIntIn(0, 100) ?: initial.retractBeforeWipePercent,
                    retractLiftEnforce = retractLiftEnforce,
                    zHopType = zHopType,
                    zHopWhenPrime = zHopWhenPrime.trim(),
                    zLiftType = zLiftType.trim(),
                    zHopMm = zHop.parseFloatIn(0f, 5f) ?: initial.zHopMm,
                    travelSlopeDegrees = travelSlope.parseFloatIn(1f, 90f) ?: initial.travelSlopeDegrees,
                    retractLiftAboveMm = retractLiftAbove.parseFloatAtLeast(0f) ?: initial.retractLiftAboveMm,
                    retractLiftBelowMm = retractLiftBelow.parseFloatAtLeast(0f) ?: initial.retractLiftBelowMm,
                    retractLengthToolchangeMm = retractLengthToolchange.parseFloatAtLeast(0f) ?: initial.retractLengthToolchangeMm,
                    retractRestartExtraToolchangeMm = retractRestartExtraToolchange.toFloatOrNull() ?: initial.retractRestartExtraToolchangeMm,
                    enableLongRetractionWhenCut = enableLongRetractionWhenCut,
                    longRetractionsWhenCut = longRetractionsWhenCut,
                    retractionDistanceWhenCutMm = retractionDistanceWhenCut.parseFloatIn(10f, 18f) ?: initial.retractionDistanceWhenCutMm,
	                    printerNotes = printerNotes
	                ).withChangedNativePrinterOverridesFrom(initial)
