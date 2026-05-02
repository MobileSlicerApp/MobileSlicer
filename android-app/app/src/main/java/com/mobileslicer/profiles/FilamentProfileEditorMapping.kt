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



internal fun FilamentProfileEditorDraft.toFilamentProfile(
    initial: FilamentProfile,
    isNew: Boolean
): FilamentProfile =
    initial.copy(
                    name = name.ifBlank { initial.name },
                    subtitle = subtitle.ifBlank { "Custom filament profile" },
                    builtIn = initial.builtIn && !isNew,
                    materialType = materialType.ifBlank { initial.materialType },
                    vendor = filamentVendor.ifBlank { "(Undefined)" },
                    soluble = filamentSoluble,
                    supportMaterial = filamentSupportMaterial,
                    filamentExtruderVariant = filamentExtruderVariant.ifBlank { initial.filamentExtruderVariant },
                    filamentSelfIndex = filamentSelfIndex.ifBlank { initial.filamentSelfIndex },
                    filamentChangeLengthMm = filamentChangeLength.parseFloatAtLeast(0f) ?: initial.filamentChangeLengthMm,
                    requiredNozzleHrc = requiredNozzleHrc.parseIntIn(0, 500) ?: initial.requiredNozzleHrc,
                    defaultFilamentColor = defaultFilamentColor,
                    diameterMm = filamentDiameter.parseFloatAtLeast(0f) ?: initial.diameterMm,
                    adhesivenessCategory = adhesivenessCategory.parseIntAtLeast(0) ?: initial.adhesivenessCategory,
                    densityGPerCm3 = filamentDensity.parseFloatAtLeast(0f) ?: initial.densityGPerCm3,
                    shrinkageXyPercent = shrinkageXy.parseFloatIn(50f, 150f) ?: initial.shrinkageXyPercent,
                    shrinkageZPercent = shrinkageZ.parseFloatIn(50f, 150f) ?: initial.shrinkageZPercent,
                    costPerKg = filamentCost.parseFloatAtLeast(0f) ?: initial.costPerKg,
                    softeningTemperatureC = softeningTemperature.parseIntIn(0, 1500) ?: initial.softeningTemperatureC,
                    idleTemperatureC = idleTemperature.parseIntIn(0, 1500) ?: initial.idleTemperatureC,
                    nozzleTemperatureRangeLowC = nozzleTemperatureRangeLow.parseIntIn(0, 1500) ?: initial.nozzleTemperatureRangeLowC,
                    nozzleTemperatureRangeHighC = nozzleTemperatureRangeHigh.parseIntIn(0, 1500) ?: initial.nozzleTemperatureRangeHighC,
                    flowRatio = flowRatio.parseFloatIn(0f, 2f) ?: initial.flowRatio,
                    retractionLengthMm = retractionLength.parseOptionalFloatAtLeast(0f, initial.retractionLengthMm),
                    zHopMm = filamentZHop.parseOptionalFloatAtLeast(0f, initial.zHopMm),
                    zHopType = filamentZHopType,
                    retractLiftAboveMm = filamentRetractLiftAbove.parseOptionalFloatAtLeast(0f, initial.retractLiftAboveMm),
                    retractLiftBelowMm = filamentRetractLiftBelow.parseOptionalFloatAtLeast(0f, initial.retractLiftBelowMm),
                    retractLiftEnforce = filamentRetractLiftEnforce,
                    retractionSpeedMmPerSec = retractionSpeed.parseOptionalFloatAtLeast(0f, initial.retractionSpeedMmPerSec),
                    deretractionSpeedMmPerSec = deretractionSpeed.parseOptionalFloatAtLeast(0f, initial.deretractionSpeedMmPerSec),
                    retractRestartExtraMm = filamentRetractRestartExtra.parseOptionalFloatAtLeast(0f, initial.retractRestartExtraMm),
                    retractionMinimumTravelMm = filamentRetractionMinimumTravel.parseOptionalFloatAtLeast(0f, initial.retractionMinimumTravelMm),
                    retractWhenChangingLayer = filamentRetractWhenChangingLayer,
                    wipe = filamentWipe,
                    wipeDistanceMm = filamentWipeDistance.parseOptionalFloatAtLeast(0f, initial.wipeDistanceMm),
                    retractBeforeWipePercent = filamentRetractBeforeWipe.parseOptionalIntIn(0, 100, initial.retractBeforeWipePercent),
                    longRetractionsWhenCut = filamentLongRetractionsWhenCut,
                    retractionDistancesWhenCut = filamentRetractionDistancesWhenCut.trim(),
                    ironingFlowPercent = filamentIroningFlow.parseOptionalFloatAtLeast(0f, initial.ironingFlowPercent),
                    ironingSpacingMm = filamentIroningSpacing.parseOptionalFloatAtLeast(0f, initial.ironingSpacingMm),
                    ironingInsetMm = filamentIroningInset.parseOptionalFloatAtLeast(0f, initial.ironingInsetMm),
                    ironingSpeedMmPerSec = filamentIroningSpeed.parseOptionalFloatAtLeast(0f, initial.ironingSpeedMmPerSec),
                    pressureAdvanceEnabled = pressureAdvanceEnabled,
                    pressureAdvance = pressureAdvance.parseFloatIn(0f, 2f) ?: initial.pressureAdvance,
                    pelletFlowCoefficient = pelletFlowCoefficient.parseFloatAtLeast(0f) ?: initial.pelletFlowCoefficient,
                    adaptivePressureAdvanceEnabled = adaptivePressureAdvanceEnabled,
                    adaptivePressureAdvanceOverhangsEnabled = adaptivePressureAdvanceOverhangsEnabled,
                    adaptivePressureAdvanceBridges = adaptivePressureAdvanceBridges.parseFloatIn(0f, 2f) ?: initial.adaptivePressureAdvanceBridges,
                    adaptivePressureAdvanceModel = adaptivePressureAdvanceModel.ifBlank { initial.adaptivePressureAdvanceModel },
                    maxVolumetricSpeedMm3PerSec = maxVolumetricSpeed.parseFloatAtLeast(0f) ?: initial.maxVolumetricSpeedMm3PerSec,
                    adaptiveVolumetricSpeedEnabled = adaptiveVolumetricSpeedEnabled,
                    volumetricSpeedCoefficients = volumetricSpeedCoefficients.trim(),
                    nozzleTemperatureInitialLayerC = nozzleTempInitialLayer.parseIntIn(0, 1500) ?: initial.nozzleTemperatureInitialLayerC,
                    nozzleTemperatureC = nozzleTemp.parseIntIn(0, 1500) ?: initial.nozzleTemperatureC,
                    chamberTemperatureC = chamberTemperature.parseIntIn(0, 1500) ?: initial.chamberTemperatureC,
                    activateChamberTemperatureControl = activateChamberTemperatureControl,
                    supertackPlateTemperatureInitialLayerC = supertackPlateTempInitialLayer.parseIntIn(0, 120) ?: initial.supertackPlateTemperatureInitialLayerC,
                    supertackPlateTemperatureC = supertackPlateTemp.parseIntIn(0, 120) ?: initial.supertackPlateTemperatureC,
                    coolPlateTemperatureInitialLayerC = coolPlateTempInitialLayer.parseIntIn(0, 120) ?: initial.coolPlateTemperatureInitialLayerC,
                    coolPlateTemperatureC = coolPlateTemp.parseIntIn(0, 300) ?: initial.coolPlateTemperatureC,
                    texturedCoolPlateTemperatureInitialLayerC = texturedCoolPlateTempInitialLayer.parseIntIn(0, 120) ?: initial.texturedCoolPlateTemperatureInitialLayerC,
                    texturedCoolPlateTemperatureC = texturedCoolPlateTemp.parseIntIn(0, 300) ?: initial.texturedCoolPlateTemperatureC,
                    engineeringPlateTemperatureInitialLayerC = engineeringPlateTempInitialLayer.parseIntIn(0, 300) ?: initial.engineeringPlateTemperatureInitialLayerC,
                    engineeringPlateTemperatureC = engineeringPlateTemp.parseIntIn(0, 300) ?: initial.engineeringPlateTemperatureC,
                    bedTemperatureInitialLayerC = bedTempInitialLayer.parseIntIn(0, 300) ?: initial.bedTemperatureInitialLayerC,
                    bedTemperatureC = bedTemp.parseIntIn(0, 300) ?: initial.bedTemperatureC,
                    texturedPlateTemperatureInitialLayerC = texturedPlateTempInitialLayer.parseIntIn(0, 300) ?: initial.texturedPlateTemperatureInitialLayerC,
                    texturedPlateTemperatureC = texturedPlateTemp.parseIntIn(0, 300) ?: initial.texturedPlateTemperatureC,
                    minFanSpeedPercent = minFanSpeed.parseIntIn(0, 100) ?: initial.minFanSpeedPercent,
                    coolingPercent = coolingPercent.parseIntIn(0, 100) ?: initial.coolingPercent,
                    noCoolingFirstLayers = noCoolingFirstLayers.parseIntAtLeast(0) ?: initial.noCoolingFirstLayers,
                    fullFanSpeedLayer = fullFanSpeedLayer.parseIntIn(0, 1000) ?: initial.fullFanSpeedLayer,
                    fanCoolingLayerTimeSeconds = fanCoolingLayerTime.parseFloatIn(0f, 1000f) ?: initial.fanCoolingLayerTimeSeconds,
                    slowDownLayerTimeSeconds = slowDownLayerTime.parseFloatIn(0f, 1000f) ?: initial.slowDownLayerTimeSeconds,
                    reduceFanStopStartFrequency = reduceFanStopStartFrequency,
                    slowDownForLayerCooling = slowDownForLayerCooling,
                    dontSlowDownOuterWall = filamentDontSlowDownOuterWall,
                    slowDownMinSpeedMmPerSec = slowDownMinSpeed.parseFloatAtLeast(0f) ?: initial.slowDownMinSpeedMmPerSec,
                    enableOverhangBridgeFan = enableOverhangBridgeFan,
                    overhangFanThreshold = overhangFanThreshold,
                    overhangFanSpeedPercent = overhangFanSpeed.parseIntIn(0, 100) ?: initial.overhangFanSpeedPercent,
                    internalBridgeFanSpeedPercent = filamentInternalBridgeFanSpeed.parseIntIn(-1, 100) ?: initial.internalBridgeFanSpeedPercent,
                    supportMaterialInterfaceFanSpeedPercent = filamentSupportInterfaceFanSpeed.parseIntIn(-1, 100) ?: initial.supportMaterialInterfaceFanSpeedPercent,
                    ironingFanSpeedPercent = ironingFanSpeed.parseIntIn(-1, 100) ?: initial.ironingFanSpeedPercent,
                    additionalCoolingFanSpeedPercent = additionalCoolingFanSpeed.parseIntIn(0, 100) ?: initial.additionalCoolingFanSpeedPercent,
                    activateAirFiltration = activateAirFiltration,
                    duringPrintExhaustFanSpeedPercent = duringPrintExhaustFanSpeed.parseIntIn(0, 100) ?: initial.duringPrintExhaustFanSpeedPercent,
                    completePrintExhaustFanSpeedPercent = completePrintExhaustFanSpeed.parseIntIn(0, 100) ?: initial.completePrintExhaustFanSpeedPercent,
                    minimalPurgeOnWipeTowerMm3 = minimalPurgeOnWipeTower.parseFloatAtLeast(0f) ?: initial.minimalPurgeOnWipeTowerMm3,
                    towerInterfacePreExtrusionDistanceMm = towerInterfacePreExtrusionDistance.parseFloatAtLeast(0f) ?: initial.towerInterfacePreExtrusionDistanceMm,
                    towerInterfacePreExtrusionLengthMm = towerInterfacePreExtrusionLength.parseFloatAtLeast(0f) ?: initial.towerInterfacePreExtrusionLengthMm,
                    towerIroningAreaMm2 = towerIroningArea.parseFloatAtLeast(0f) ?: initial.towerIroningAreaMm2,
                    towerInterfacePurgeVolumeMm = towerInterfacePurgeVolume.parseFloatAtLeast(0f) ?: initial.towerInterfacePurgeVolumeMm,
                    towerInterfacePrintTemperatureC = towerInterfacePrintTemperature.toIntOrNull() ?: initial.towerInterfacePrintTemperatureC,
                    longRetractionsWhenExtruderChange = longRetractionsWhenExtruderChange,
                    retractionDistanceWhenExtruderChangeMm = retractionDistanceWhenExtruderChange.parseOptionalFloatAtLeast(0f, initial.retractionDistanceWhenExtruderChangeMm),
                    loadingSpeedStartMmPerSec = loadingSpeedStart.parseFloatAtLeast(0f) ?: initial.loadingSpeedStartMmPerSec,
                    loadingSpeedMmPerSec = loadingSpeed.parseFloatAtLeast(0f) ?: initial.loadingSpeedMmPerSec,
                    unloadingSpeedStartMmPerSec = unloadingSpeedStart.parseFloatAtLeast(0f) ?: initial.unloadingSpeedStartMmPerSec,
                    unloadingSpeedMmPerSec = unloadingSpeed.parseFloatAtLeast(0f) ?: initial.unloadingSpeedMmPerSec,
                    toolchangeDelaySeconds = toolchangeDelay.parseFloatAtLeast(0f) ?: initial.toolchangeDelaySeconds,
                    coolingMoves = coolingMoves.parseIntIn(0, 20) ?: initial.coolingMoves,
                    coolingInitialSpeedMmPerSec = coolingInitialSpeed.parseFloatAtLeast(0f) ?: initial.coolingInitialSpeedMmPerSec,
                    coolingFinalSpeedMmPerSec = coolingFinalSpeed.parseFloatAtLeast(0f) ?: initial.coolingFinalSpeedMmPerSec,
                    stampingLoadingSpeedMmPerSec = stampingLoadingSpeed.parseFloatAtLeast(0f) ?: initial.stampingLoadingSpeedMmPerSec,
                    stampingDistanceMm = stampingDistance.parseFloatAtLeast(0f) ?: initial.stampingDistanceMm,
                    rammingParameters = rammingParameters.ifBlank { initial.rammingParameters },
                    multitoolRamming = multitoolRamming,
                    multitoolRammingVolumeMm3 = multitoolRammingVolume.parseFloatAtLeast(0f) ?: initial.multitoolRammingVolumeMm3,
                    multitoolRammingFlowMm3PerSec = multitoolRammingFlow.parseFloatAtLeast(0f) ?: initial.multitoolRammingFlowMm3PerSec,
                    compatiblePrinters = compatiblePrinters.trim(),
                    compatiblePrintersCondition = compatiblePrintersCondition.trim(),
                    compatiblePrints = compatiblePrints.trim(),
                    compatiblePrintsCondition = compatiblePrintsCondition.trim(),
                    filamentNotes = filamentNotes,
                    filamentStartGcode = filamentStartGcode,
	                    filamentEndGcode = filamentEndGcode
	                ).withChangedNativeFilamentOverridesFrom(initial)
