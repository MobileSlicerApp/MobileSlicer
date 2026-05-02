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



internal class FilamentProfileEditorDraft(initial: FilamentProfile) {
    var name by mutableStateOf(initial.name)
    var subtitle by mutableStateOf(initial.subtitle)
    var materialType by mutableStateOf(initial.materialType)
    var filamentVendor by mutableStateOf(initial.vendor)
    var filamentSoluble by mutableStateOf(initial.soluble)
    var filamentSupportMaterial by mutableStateOf(initial.supportMaterial)
    var filamentExtruderVariant by mutableStateOf(initial.filamentExtruderVariant)
    var filamentSelfIndex by mutableStateOf(initial.filamentSelfIndex)
    var filamentChangeLength by mutableStateOf(initial.filamentChangeLengthMm.toString())
    var requiredNozzleHrc by mutableStateOf(initial.requiredNozzleHrc.toString())
    var defaultFilamentColor by mutableStateOf(initial.defaultFilamentColor)
    var filamentDiameter by mutableStateOf(initial.diameterMm.toString())
    var adhesivenessCategory by mutableStateOf(initial.adhesivenessCategory.toString())
    var filamentDensity by mutableStateOf(initial.densityGPerCm3.toString())
    var shrinkageXy by mutableStateOf(initial.shrinkageXyPercent.toString())
    var shrinkageZ by mutableStateOf(initial.shrinkageZPercent.toString())
    var filamentCost by mutableStateOf(initial.costPerKg.toString())
    var softeningTemperature by mutableStateOf(initial.softeningTemperatureC.toString())
    var idleTemperature by mutableStateOf(initial.idleTemperatureC.toString())
    var nozzleTemperatureRangeLow by mutableStateOf(initial.nozzleTemperatureRangeLowC.toString())
    var nozzleTemperatureRangeHigh by mutableStateOf(initial.nozzleTemperatureRangeHighC.toString())
    var flowRatio by mutableStateOf(initial.flowRatio.toString())
    var retractionLength by mutableStateOf(initial.retractionLengthMm?.toString().orEmpty())
    var filamentZHop by mutableStateOf(initial.zHopMm?.toString().orEmpty())
    var filamentZHopType by mutableStateOf(initial.zHopType)
    var filamentRetractLiftAbove by mutableStateOf(initial.retractLiftAboveMm?.toString().orEmpty())
    var filamentRetractLiftBelow by mutableStateOf(initial.retractLiftBelowMm?.toString().orEmpty())
    var filamentRetractLiftEnforce by mutableStateOf(initial.retractLiftEnforce)
    var retractionSpeed by mutableStateOf(initial.retractionSpeedMmPerSec?.toString().orEmpty())
    var deretractionSpeed by mutableStateOf(initial.deretractionSpeedMmPerSec?.toString().orEmpty())
    var filamentRetractRestartExtra by mutableStateOf(initial.retractRestartExtraMm?.toString().orEmpty())
    var filamentRetractionMinimumTravel by mutableStateOf(initial.retractionMinimumTravelMm?.toString().orEmpty())
    var filamentRetractWhenChangingLayer by mutableStateOf(initial.retractWhenChangingLayer)
    var filamentWipe by mutableStateOf(initial.wipe)
    var filamentWipeDistance by mutableStateOf(initial.wipeDistanceMm?.toString().orEmpty())
    var filamentRetractBeforeWipe by mutableStateOf(initial.retractBeforeWipePercent?.toString().orEmpty())
    var filamentLongRetractionsWhenCut by mutableStateOf(initial.longRetractionsWhenCut)
    var filamentRetractionDistancesWhenCut by mutableStateOf(initial.retractionDistancesWhenCut)
    var filamentIroningFlow by mutableStateOf(initial.ironingFlowPercent?.toString().orEmpty())
    var filamentIroningSpacing by mutableStateOf(initial.ironingSpacingMm?.toString().orEmpty())
    var filamentIroningInset by mutableStateOf(initial.ironingInsetMm?.toString().orEmpty())
    var filamentIroningSpeed by mutableStateOf(initial.ironingSpeedMmPerSec?.toString().orEmpty())
    var pressureAdvanceEnabled by mutableStateOf(initial.pressureAdvanceEnabled)
    var pressureAdvance by mutableStateOf(initial.pressureAdvance.toString())
    var pelletFlowCoefficient by mutableStateOf(initial.pelletFlowCoefficient.toString())
    var adaptivePressureAdvanceEnabled by mutableStateOf(initial.adaptivePressureAdvanceEnabled)
    var adaptivePressureAdvanceOverhangsEnabled by mutableStateOf(initial.adaptivePressureAdvanceOverhangsEnabled)
    var adaptivePressureAdvanceBridges by mutableStateOf(initial.adaptivePressureAdvanceBridges.toString())
    var adaptivePressureAdvanceModel by mutableStateOf(initial.adaptivePressureAdvanceModel)
    var adaptiveVolumetricSpeedEnabled by mutableStateOf(initial.adaptiveVolumetricSpeedEnabled)
    var volumetricSpeedCoefficients by mutableStateOf(initial.volumetricSpeedCoefficients)
    var maxVolumetricSpeed by mutableStateOf(initial.maxVolumetricSpeedMm3PerSec.toString())
    var nozzleTempInitialLayer by mutableStateOf(initial.nozzleTemperatureInitialLayerC.toString())
    var nozzleTemp by mutableStateOf(initial.nozzleTemperatureC.toString())
    var chamberTemperature by mutableStateOf(initial.chamberTemperatureC.toString())
    var activateChamberTemperatureControl by mutableStateOf(initial.activateChamberTemperatureControl)
    var supertackPlateTempInitialLayer by mutableStateOf(initial.supertackPlateTemperatureInitialLayerC.toString())
    var supertackPlateTemp by mutableStateOf(initial.supertackPlateTemperatureC.toString())
    var coolPlateTempInitialLayer by mutableStateOf(initial.coolPlateTemperatureInitialLayerC.toString())
    var coolPlateTemp by mutableStateOf(initial.coolPlateTemperatureC.toString())
    var texturedCoolPlateTempInitialLayer by mutableStateOf(initial.texturedCoolPlateTemperatureInitialLayerC.toString())
    var texturedCoolPlateTemp by mutableStateOf(initial.texturedCoolPlateTemperatureC.toString())
    var engineeringPlateTempInitialLayer by mutableStateOf(initial.engineeringPlateTemperatureInitialLayerC.toString())
    var engineeringPlateTemp by mutableStateOf(initial.engineeringPlateTemperatureC.toString())
    var bedTempInitialLayer by mutableStateOf(initial.bedTemperatureInitialLayerC.toString())
    var bedTemp by mutableStateOf(initial.bedTemperatureC.toString())
    var texturedPlateTempInitialLayer by mutableStateOf(initial.texturedPlateTemperatureInitialLayerC.toString())
    var texturedPlateTemp by mutableStateOf(initial.texturedPlateTemperatureC.toString())
    var minFanSpeed by mutableStateOf(initial.minFanSpeedPercent.toString())
    var coolingPercent by mutableStateOf(initial.coolingPercent.toString())
    var noCoolingFirstLayers by mutableStateOf(initial.noCoolingFirstLayers.toString())
    var fullFanSpeedLayer by mutableStateOf(initial.fullFanSpeedLayer.toString())
    var fanCoolingLayerTime by mutableStateOf(initial.fanCoolingLayerTimeSeconds.toString())
    var slowDownLayerTime by mutableStateOf(initial.slowDownLayerTimeSeconds.toString())
    var reduceFanStopStartFrequency by mutableStateOf(initial.reduceFanStopStartFrequency)
    var slowDownForLayerCooling by mutableStateOf(initial.slowDownForLayerCooling)
    var filamentDontSlowDownOuterWall by mutableStateOf(initial.dontSlowDownOuterWall)
    var slowDownMinSpeed by mutableStateOf(initial.slowDownMinSpeedMmPerSec.toString())
    var enableOverhangBridgeFan by mutableStateOf(initial.enableOverhangBridgeFan)
    var overhangFanThreshold by mutableStateOf(initial.overhangFanThreshold)
    var overhangFanSpeed by mutableStateOf(initial.overhangFanSpeedPercent.toString())
    var filamentInternalBridgeFanSpeed by mutableStateOf(initial.internalBridgeFanSpeedPercent.toString())
    var filamentSupportInterfaceFanSpeed by mutableStateOf(initial.supportMaterialInterfaceFanSpeedPercent.toString())
    var ironingFanSpeed by mutableStateOf(initial.ironingFanSpeedPercent.toString())
    var additionalCoolingFanSpeed by mutableStateOf(initial.additionalCoolingFanSpeedPercent.toString())
    var activateAirFiltration by mutableStateOf(initial.activateAirFiltration)
    var duringPrintExhaustFanSpeed by mutableStateOf(initial.duringPrintExhaustFanSpeedPercent.toString())
    var completePrintExhaustFanSpeed by mutableStateOf(initial.completePrintExhaustFanSpeedPercent.toString())
    var minimalPurgeOnWipeTower by mutableStateOf(initial.minimalPurgeOnWipeTowerMm3.toString())
    var towerInterfacePreExtrusionDistance by mutableStateOf(initial.towerInterfacePreExtrusionDistanceMm.toString())
    var towerInterfacePreExtrusionLength by mutableStateOf(initial.towerInterfacePreExtrusionLengthMm.toString())
    var towerIroningArea by mutableStateOf(initial.towerIroningAreaMm2.toString())
    var towerInterfacePurgeVolume by mutableStateOf(initial.towerInterfacePurgeVolumeMm.toString())
    var towerInterfacePrintTemperature by mutableStateOf(initial.towerInterfacePrintTemperatureC.toString())
    var longRetractionsWhenExtruderChange by mutableStateOf(initial.longRetractionsWhenExtruderChange)
    var retractionDistanceWhenExtruderChange by mutableStateOf(initial.retractionDistanceWhenExtruderChangeMm?.toString().orEmpty())
    var loadingSpeedStart by mutableStateOf(initial.loadingSpeedStartMmPerSec.toString())
    var loadingSpeed by mutableStateOf(initial.loadingSpeedMmPerSec.toString())
    var unloadingSpeedStart by mutableStateOf(initial.unloadingSpeedStartMmPerSec.toString())
    var unloadingSpeed by mutableStateOf(initial.unloadingSpeedMmPerSec.toString())
    var toolchangeDelay by mutableStateOf(initial.toolchangeDelaySeconds.toString())
    var coolingMoves by mutableStateOf(initial.coolingMoves.toString())
    var coolingInitialSpeed by mutableStateOf(initial.coolingInitialSpeedMmPerSec.toString())
    var coolingFinalSpeed by mutableStateOf(initial.coolingFinalSpeedMmPerSec.toString())
    var stampingLoadingSpeed by mutableStateOf(initial.stampingLoadingSpeedMmPerSec.toString())
    var stampingDistance by mutableStateOf(initial.stampingDistanceMm.toString())
    var rammingParameters by mutableStateOf(initial.rammingParameters)
    var multitoolRamming by mutableStateOf(initial.multitoolRamming)
    var multitoolRammingVolume by mutableStateOf(initial.multitoolRammingVolumeMm3.toString())
    var multitoolRammingFlow by mutableStateOf(initial.multitoolRammingFlowMm3PerSec.toString())
    var compatiblePrinters by mutableStateOf(initial.compatiblePrinters)
    var compatiblePrintersCondition by mutableStateOf(initial.compatiblePrintersCondition)
    var compatiblePrints by mutableStateOf(initial.compatiblePrints)
    var compatiblePrintsCondition by mutableStateOf(initial.compatiblePrintsCondition)
    var filamentNotes by mutableStateOf(initial.filamentNotes)
    var filamentStartGcode by mutableStateOf(initial.filamentStartGcode)
    var filamentEndGcode by mutableStateOf(initial.filamentEndGcode)
    var selectedTab by mutableStateOf(FilamentEditorTab.Filament)
}
