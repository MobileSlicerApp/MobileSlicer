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



internal class PrinterProfileEditorDraft(initial: PrinterProfile) {
    var name by mutableStateOf(initial.name)
    var subtitle by mutableStateOf(initial.subtitle)
    var bedWidth by mutableStateOf(initial.bedWidthMm.toInt().toString())
    var bedDepth by mutableStateOf(initial.bedDepthMm.toInt().toString())
    var maxHeight by mutableStateOf(initial.maxHeightMm.toInt().toString())
    var bedExcludeArea by mutableStateOf(initial.bedExcludeArea)
    var wrappingExcludeArea by mutableStateOf(initial.wrappingExcludeArea)
    var headWrapDetectZone by mutableStateOf(initial.headWrapDetectZone)
    var bedCustomTexture by mutableStateOf(initial.bedCustomTexture)
    var bedCustomModel by mutableStateOf(initial.bedCustomModel)
    var bedModel by mutableStateOf(initial.bedModel)
    var bedShape by mutableStateOf(initial.bedShape)
    var bedTexture by mutableStateOf(initial.bedTexture)
    var bedTextureArea by mutableStateOf(initial.bedTextureArea)
    var bottomTextureRect by mutableStateOf(initial.bottomTextureRect)
    var bottomTextureEndName by mutableStateOf(initial.bottomTextureEndName)
    var imageBedType by mutableStateOf(initial.imageBedType)
    var useDoubleExtruderDefaultTexture by mutableStateOf(initial.useDoubleExtruderDefaultTexture)
    var useRectGrid by mutableStateOf(initial.useRectGrid)
    var supportMultiBedTypes by mutableStateOf(initial.supportMultiBedTypes)
    var defaultBedType by mutableStateOf(initial.defaultBedType)
    var bestObjectPosition by mutableStateOf(initial.bestObjectPosition)
    var zOffset by mutableStateOf(initial.zOffsetMm.toString())
    var preferredOrientation by mutableStateOf(initial.preferredOrientationDegrees.toString())
    var bedMeshMin by mutableStateOf(initial.bedMeshMin)
    var bedMeshMax by mutableStateOf(initial.bedMeshMax)
    var bedMeshProbeDistance by mutableStateOf(initial.bedMeshProbeDistance)
    var adaptiveBedMeshMargin by mutableStateOf(initial.adaptiveBedMeshMarginMm.toString())
    var nozzleDiameter by mutableStateOf(initial.nozzleDiameterMm.toString())
    var filamentDiameter by mutableStateOf(initial.filamentDiameterMm.toString())
    var nozzleVolume by mutableStateOf(initial.nozzleVolumeMm3.toString())
    var nozzleVolumeType by mutableStateOf(initial.nozzleVolumeType)
    var nozzleHeight by mutableStateOf(initial.nozzleHeightMm.toString())
    var grabLength by mutableStateOf(initial.grabLengthMm.toString())
    var extruderVariantList by mutableStateOf(initial.extruderVariantList)
    var printerExtruderId by mutableStateOf(initial.printerExtruderId)
    var printerExtruderVariant by mutableStateOf(initial.printerExtruderVariant)
    var masterExtruderId by mutableStateOf(initial.masterExtruderId.toString())
    var physicalExtruderMap by mutableStateOf(initial.physicalExtruderMap)
    var extrudersCount by mutableStateOf(initial.extrudersCount)
    var extruderAmsCount by mutableStateOf(initial.extruderAmsCount)
    var extruderMaxNozzleCount by mutableStateOf(initial.extruderMaxNozzleCount)
    var extruderType by mutableStateOf(initial.extruderType)
    var extruderColor by mutableStateOf(initial.extruderColor)
    var extruderPrintableHeight by mutableStateOf(initial.extruderPrintableHeightMm.toString())
    var extruderPrintableArea by mutableStateOf(initial.extruderPrintableArea)
    var minLayerHeight by mutableStateOf(initial.minLayerHeightMm.toString())
    var maxLayerHeight by mutableStateOf(initial.maxLayerHeightMm.toString())
    var extruderOffset by mutableStateOf(initial.extruderOffset)
    var printerModel by mutableStateOf(initial.printerModel)
    var machineTech by mutableStateOf(initial.machineTech)
    var machineFamily by mutableStateOf(initial.machineFamily)
    var printerTechnology by mutableStateOf(initial.printerTechnology)
    var printerVariant by mutableStateOf(initial.printerVariant)
    var hotendModel by mutableStateOf(initial.hotendModel)
    var boxId by mutableStateOf(initial.boxId)
    var enablePreHeating by mutableStateOf(initial.enablePreHeating)
    var fanDirection by mutableStateOf(initial.fanDirection)
    var hotendCoolingRate by mutableStateOf(initial.hotendCoolingRate)
    var hotendHeatingRate by mutableStateOf(initial.hotendHeatingRate)
    var activeFeederMotorName by mutableStateOf(initial.activeFeederMotorName)
    var autoDisableFilterOnOverheat by mutableStateOf(initial.autoDisableFilterOnOverheat)
    var autoToolchangeCommand by mutableStateOf(initial.autoToolchangeCommand)
    var coolingFilterEnabled by mutableStateOf(initial.coolingFilterEnabled)
    var crealityFlushTime by mutableStateOf(initial.crealityFlushTime)
    var groupAlgoWithTime by mutableStateOf(initial.groupAlgoWithTime)
    var isArtillery by mutableStateOf(initial.isArtillery)
    var isSupport3mf by mutableStateOf(initial.isSupport3mf)
    var isSupportAirCondition by mutableStateOf(initial.isSupportAirCondition)
    var isSupportMqtt by mutableStateOf(initial.isSupportMqtt)
    var isSupportMultiBox by mutableStateOf(initial.isSupportMultiBox)
    var isSupportTimelapse by mutableStateOf(initial.isSupportTimelapse)
    var machineLedLightExist by mutableStateOf(initial.machineLedLightExist)
    var machineHotendChangeTime by mutableStateOf(initial.machineHotendChangeTime)
    var machinePlatformMotionEnable by mutableStateOf(initial.machinePlatformMotionEnable)
    var machinePrepareCompensationTime by mutableStateOf(initial.machinePrepareCompensationTime)
    var multiZone by mutableStateOf(initial.multiZone)
    var multiZoneNumber by mutableStateOf(initial.multiZoneNumber)
    var nozzleFlushDataset by mutableStateOf(initial.nozzleFlushDataset)
    var rammingPressureAdvanceValue by mutableStateOf(initial.rammingPressureAdvanceValue)
    var rightIconOffsetBed by mutableStateOf(initial.rightIconOffsetBed)
    var scanFolder by mutableStateOf(initial.scanFolder)
    var supportBoxTempControl by mutableStateOf(initial.supportBoxTempControl)
    var supportCoolingFilter by mutableStateOf(initial.supportCoolingFilter)
    var supportMultiFilament by mutableStateOf(initial.supportMultiFilament)
    var supportObjectSkipFlush by mutableStateOf(initial.supportObjectSkipFlush)
    var supportWanNetwork by mutableStateOf(initial.supportWanNetwork)
    var toolChangeTemperatureWait by mutableStateOf(initial.toolChangeTemperatureWait)
    var upwardCompatibleMachine by mutableStateOf(initial.upwardCompatibleMachine)
    var vendorUrl by mutableStateOf(initial.vendorUrl)
    var useActivePelletFeeding by mutableStateOf(initial.useActivePelletFeeding)
    var useExtruderRotationVolume by mutableStateOf(initial.useExtruderRotationVolume)
    var printerStructure by mutableStateOf(initial.printerStructure)
    var gcodeFlavor by mutableStateOf(initial.gcodeFlavor)
    var pelletModdedPrinter by mutableStateOf(initial.pelletModdedPrinter)
    var useThirdPartyPrintHost by mutableStateOf(initial.useThirdPartyPrintHost)
    var scanFirstLayer by mutableStateOf(initial.scanFirstLayer)
    var useRelativeEDistances by mutableStateOf(initial.useRelativeEDistances)
    var useFirmwareRetraction by mutableStateOf(initial.useFirmwareRetraction)
    var powerLossRecoveryMode by mutableStateOf(initial.powerLossRecoveryMode)
    var disableM73 by mutableStateOf(initial.disableM73)
    var thumbnails by mutableStateOf(initial.thumbnails)
    var thumbnailsInternal by mutableStateOf(initial.thumbnailsInternal)
    var thumbnailsInternalSwitch by mutableStateOf(initial.thumbnailsInternalSwitch)
    var remainingTimes by mutableStateOf(initial.remainingTimes)
    var printHostType by mutableStateOf(initial.printHostType)
    var printerAgent by mutableStateOf(initial.printerAgent)
    var printHost by mutableStateOf(initial.printHost)
    var printHostWebUi by mutableStateOf(initial.printHostWebUi)
    var printHostAuthorizationType by mutableStateOf(initial.printHostAuthorizationType)
    var printHostApiKey by mutableStateOf(initial.printHostApiKey)
    var printHostPort by mutableStateOf(initial.printHostPort)
    var printHostGroup by mutableStateOf(initial.printHostGroup)
    var printHostCaFile by mutableStateOf(initial.printHostCaFile)
    var printHostUser by mutableStateOf(initial.printHostUser)
    var printHostPassword by mutableStateOf(initial.printHostPassword)
    var printHostSslIgnoreRevoke by mutableStateOf(initial.printHostSslIgnoreRevoke)
    var timeCost by mutableStateOf(initial.timeCost.toString())
    var fanSpeedupTime by mutableStateOf(initial.fanSpeedupTimeSeconds.toString())
    var fanSpeedupOverhangsOnly by mutableStateOf(initial.fanSpeedupOverhangsOnly)
    var fanKickstartTime by mutableStateOf(initial.fanKickstartTimeSeconds.toString())
    var extruderClearanceRadius by mutableStateOf(initial.extruderClearanceRadiusMm.toString())
    var extruderClearanceHeightToRod by mutableStateOf(initial.extruderClearanceHeightToRodMm.toString())
    var extruderClearanceHeightToLid by mutableStateOf(initial.extruderClearanceHeightToLidMm.toString())
    var extruderClearanceDistToRod by mutableStateOf(initial.extruderClearanceDistToRodMm.toString())
    var nozzleType by mutableStateOf(initial.nozzleType)
    var nozzleHrc by mutableStateOf(initial.nozzleHrc.toString())
    var auxiliaryFan by mutableStateOf(initial.auxiliaryFan)
    var supportChamberTempControl by mutableStateOf(initial.supportChamberTempControl)
    var supportAirFiltration by mutableStateOf(initial.supportAirFiltration)
    var singleExtruderMultiMaterial by mutableStateOf(initial.singleExtruderMultiMaterial)
    var manualFilamentChange by mutableStateOf(initial.manualFilamentChange)
    var bedTemperatureFormula by mutableStateOf(initial.bedTemperatureFormula)
    var wipeTowerType by mutableStateOf(initial.wipeTowerType)
    var purgeInPrimeTower by mutableStateOf(initial.purgeInPrimeTower)
    var enableFilamentRamming by mutableStateOf(initial.enableFilamentRamming)
    var coolingTubeRetraction by mutableStateOf(initial.coolingTubeRetractionMm.toString())
    var coolingTubeLength by mutableStateOf(initial.coolingTubeLengthMm.toString())
    var parkingPositionRetraction by mutableStateOf(initial.parkingPositionRetractionMm.toString())
    var extraLoadingMove by mutableStateOf(initial.extraLoadingMoveMm.toString())
    var highCurrentOnFilamentSwap by mutableStateOf(initial.highCurrentOnFilamentSwap)
    var machineLoadFilamentTime by mutableStateOf(initial.machineLoadFilamentTimeSeconds.toString())
    var machineUnloadFilamentTime by mutableStateOf(initial.machineUnloadFilamentTimeSeconds.toString())
    var machineToolChangeTime by mutableStateOf(initial.machineToolChangeTimeSeconds.toString())
    var fileStartGcode by mutableStateOf(initial.fileStartGcode)
    var machineStartGcode by mutableStateOf(initial.machineStartGcode)
    var machineEndGcode by mutableStateOf(initial.machineEndGcode)
    var printingByObjectGcode by mutableStateOf(initial.printingByObjectGcode)
    var beforeLayerChangeGcode by mutableStateOf(initial.beforeLayerChangeGcode)
    var layerChangeGcode by mutableStateOf(initial.layerChangeGcode)
    var timeLapseGcode by mutableStateOf(initial.timeLapseGcode)
    var wrappingDetectionGcode by mutableStateOf(initial.wrappingDetectionGcode)
    var changeFilamentGcode by mutableStateOf(initial.changeFilamentGcode)
    var changeExtrusionRoleGcode by mutableStateOf(initial.changeExtrusionRoleGcode)
    var machinePauseGcode by mutableStateOf(initial.machinePauseGcode)
    var templateCustomGcode by mutableStateOf(initial.templateCustomGcode)
    var emitMachineLimitsToGcode by mutableStateOf(initial.emitMachineLimitsToGcode)
    var resonanceAvoidance by mutableStateOf(initial.resonanceAvoidance)
    var silentMode by mutableStateOf(initial.silentMode)
    var minResonanceAvoidanceSpeed by mutableStateOf(initial.minResonanceAvoidanceSpeedMmPerSec.toString())
    var maxResonanceAvoidanceSpeed by mutableStateOf(initial.maxResonanceAvoidanceSpeedMmPerSec.toString())
    var machineMaxSpeedX by mutableStateOf(initial.machineMaxSpeedX.toString())
    var machineMaxSpeedY by mutableStateOf(initial.machineMaxSpeedY.toString())
    var machineMaxSpeedZ by mutableStateOf(initial.machineMaxSpeedZ.toString())
    var machineMaxSpeedE by mutableStateOf(initial.machineMaxSpeedE.toString())
    var machineMaxAccelerationX by mutableStateOf(initial.machineMaxAccelerationX.toString())
    var machineMaxAccelerationY by mutableStateOf(initial.machineMaxAccelerationY.toString())
    var machineMaxAccelerationZ by mutableStateOf(initial.machineMaxAccelerationZ.toString())
    var machineMaxAccelerationE by mutableStateOf(initial.machineMaxAccelerationE.toString())
    var machineMaxAccelerationExtruding by mutableStateOf(initial.machineMaxAccelerationExtruding.toString())
    var machineMaxAccelerationRetracting by mutableStateOf(initial.machineMaxAccelerationRetracting.toString())
    var machineMaxAccelerationTravel by mutableStateOf(initial.machineMaxAccelerationTravel.toString())
    var machineMaxJerkX by mutableStateOf(initial.machineMaxJerkX.toString())
    var machineMaxJerkY by mutableStateOf(initial.machineMaxJerkY.toString())
    var machineMaxJerkZ by mutableStateOf(initial.machineMaxJerkZ.toString())
    var machineMaxJerkE by mutableStateOf(initial.machineMaxJerkE.toString())
    var machineMaxJunctionDeviation by mutableStateOf(initial.machineMaxJunctionDeviation.toString())
    var machineMinExtrudingRate by mutableStateOf(initial.machineMinExtrudingRateMmPerSec.toString())
    var machineMinTravelRate by mutableStateOf(initial.machineMinTravelRateMmPerSec.toString())
    var retractionLength by mutableStateOf(initial.retractionLengthMm.toString())
    var retractRestartExtra by mutableStateOf(initial.retractRestartExtraMm.toString())
    var retractionSpeed by mutableStateOf(initial.retractionSpeedMmPerSec.toString())
    var deretractionSpeed by mutableStateOf(initial.deretractionSpeedMmPerSec.toString())
    var retractionMinimumTravel by mutableStateOf(initial.retractionMinimumTravelMm.toString())
    var retractWhenChangingLayer by mutableStateOf(initial.retractWhenChangingLayer)
    var retractOnTopLayer by mutableStateOf(initial.retractOnTopLayer)
    var wipe by mutableStateOf(initial.wipe)
    var wipeDistance by mutableStateOf(initial.wipeDistanceMm.toString())
    var retractBeforeWipe by mutableStateOf(initial.retractBeforeWipePercent.toString())
    var retractLiftEnforce by mutableStateOf(initial.retractLiftEnforce)
    var zHopType by mutableStateOf(initial.zHopType)
    var zHopWhenPrime by mutableStateOf(initial.zHopWhenPrime)
    var zLiftType by mutableStateOf(initial.zLiftType)
    var zHop by mutableStateOf(initial.zHopMm.toString())
    var travelSlope by mutableStateOf(initial.travelSlopeDegrees.toString())
    var retractLiftAbove by mutableStateOf(initial.retractLiftAboveMm.toString())
    var retractLiftBelow by mutableStateOf(initial.retractLiftBelowMm.toString())
    var retractLengthToolchange by mutableStateOf(initial.retractLengthToolchangeMm.toString())
    var retractRestartExtraToolchange by mutableStateOf(initial.retractRestartExtraToolchangeMm.toString())
    var enableLongRetractionWhenCut by mutableStateOf(initial.enableLongRetractionWhenCut)
    var longRetractionsWhenCut by mutableStateOf(initial.longRetractionsWhenCut)
    var retractionDistanceWhenCut by mutableStateOf(initial.retractionDistanceWhenCutMm.toString())
    var printerNotes by mutableStateOf(initial.printerNotes)
}
