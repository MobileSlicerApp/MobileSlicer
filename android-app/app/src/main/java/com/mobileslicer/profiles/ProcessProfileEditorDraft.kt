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



internal class ProcessProfileEditorDraft(initial: ProcessProfile) {
    var name by mutableStateOf(initial.name)
    var subtitle by mutableStateOf(initial.subtitle)
    var firstLayerHeight by mutableStateOf(initial.firstLayerHeightMm.toString())
    var layerHeight by mutableStateOf(initial.layerHeightMm.toString())
    var firstLayerPrintSpeed by mutableStateOf(initial.firstLayerPrintSpeedMmPerSec.toString())
    var firstLayerInfillSpeed by mutableStateOf(initial.firstLayerInfillSpeedMmPerSec.toString())
    var firstLayerTravelSpeedPercent by mutableStateOf(initial.firstLayerTravelSpeedPercent.toString())
    var slowDownLayers by mutableStateOf(initial.slowDownLayers.toString())
    var initialLayerAcceleration by mutableStateOf(initial.initialLayerAccelerationMmPerSec2.toString())
    var initialLayerJerk by mutableStateOf(initial.initialLayerJerkMmPerSec.toString())
    var firstLayerFlowRatio by mutableStateOf(initial.firstLayerFlowRatio.toString())
    var printExtruderId by mutableStateOf(initial.printExtruderId)
    var printExtruderVariant by mutableStateOf(initial.printExtruderVariant)
    var outerWallSpeed by mutableStateOf(initial.outerWallSpeedMmPerSec.toString())
    var innerWallSpeed by mutableStateOf(initial.innerWallSpeedMmPerSec.toString())
    var topSurfaceSpeed by mutableStateOf(initial.topSurfaceSpeedMmPerSec.toString())
    var travelSpeed by mutableStateOf(initial.travelSpeedMmPerSec.toString())
    var defaultAcceleration by mutableStateOf(initial.defaultAccelerationMmPerSec2.toString())
    var outerWallAcceleration by mutableStateOf(initial.outerWallAccelerationMmPerSec2.toString())
    var innerWallAcceleration by mutableStateOf(initial.innerWallAccelerationMmPerSec2.toString())
    var topSurfaceAcceleration by mutableStateOf(initial.topSurfaceAccelerationMmPerSec2.toString())
    var sparseInfillAcceleration by mutableStateOf(initial.sparseInfillAccelerationMmPerSec2.toString())
    var internalSolidInfillAcceleration by mutableStateOf(initial.internalSolidInfillAcceleration)
    var travelAcceleration by mutableStateOf(initial.travelAccelerationMmPerSec2.toString())
    var accelToDecelEnable by mutableStateOf(initial.accelToDecelEnable)
    var accelToDecelFactor by mutableStateOf(initial.accelToDecelFactorPercent.toString())
    var defaultJunctionDeviation by mutableStateOf(initial.defaultJunctionDeviationMm.toString())
    var defaultJerk by mutableStateOf(initial.defaultJerkMmPerSec.toString())
    var innerWallJerk by mutableStateOf(initial.innerWallJerkMmPerSec.toString())
    var infillJerk by mutableStateOf(initial.infillJerkMmPerSec.toString())
    var topSurfaceJerk by mutableStateOf(initial.topSurfaceJerkMmPerSec.toString())
    var travelJerk by mutableStateOf(initial.travelJerkMmPerSec.toString())
    var innerWallFlowRatio by mutableStateOf(initial.innerWallFlowRatio.toString())
    var outerWallJerk by mutableStateOf(initial.outerWallJerkMmPerSec.toString())
    var outerWallFlowRatio by mutableStateOf(initial.outerWallFlowRatio.toString())
    var topSolidInfillFlowRatio by mutableStateOf(initial.topSolidInfillFlowRatio.toString())
    var bottomSolidInfillFlowRatio by mutableStateOf(initial.bottomSolidInfillFlowRatio.toString())
    var overhang1_4Speed by mutableStateOf(initial.overhang1_4Speed)
    var overhang2_4Speed by mutableStateOf(initial.overhang2_4Speed)
    var overhang3_4Speed by mutableStateOf(initial.overhang3_4Speed)
    var overhang4_4Speed by mutableStateOf(initial.overhang4_4Speed)
    var enableOverhangSpeed by mutableStateOf(initial.enableOverhangSpeed)
    var slowdownForCurledPerimeters by mutableStateOf(initial.slowdownForCurledPerimeters)
    var overhangFlowRatio by mutableStateOf(initial.overhangFlowRatio.toString())
    var dontSlowDownOuterWall by mutableStateOf(initial.dontSlowDownOuterWall)
    var bridgeAcceleration by mutableStateOf(initial.bridgeAcceleration)
    var bridgeSpeed by mutableStateOf(initial.bridgeSpeedMmPerSec.toString())
    var bridgeAngleDegrees by mutableStateOf(initial.bridgeAngleDegrees.toString())
    var bridgeDensityPercent by mutableStateOf(initial.bridgeDensityPercent.toString())
    var bridgeFlowRatio by mutableStateOf(initial.bridgeFlowRatio.toString())
    var bridgeNoSupport by mutableStateOf(initial.bridgeNoSupport)
    var thickInternalBridges by mutableStateOf(initial.thickInternalBridges)
    var extraBridgeLayer by mutableStateOf(initial.extraBridgeLayer)
    var internalBridgeAngleDegrees by mutableStateOf(initial.internalBridgeAngleDegrees.toString())
    var internalBridgeDensityPercent by mutableStateOf(initial.internalBridgeDensityPercent.toString())
    var internalBridgeFlowRatio by mutableStateOf(initial.internalBridgeFlowRatio.toString())
    var internalBridgeSpeed by mutableStateOf(initial.internalBridgeSpeed)
    var internalBridgeFanSpeed by mutableStateOf(initial.internalBridgeFanSpeed)
    var internalBridgeSupportThickness by mutableStateOf(initial.internalBridgeSupportThickness)
    var maxVolumetricExtrusionRateSlope by mutableStateOf(initial.maxVolumetricExtrusionRateSlope.toString())
    var maxVolumetricExtrusionRateSlopeSegmentLength by mutableStateOf(initial.maxVolumetricExtrusionRateSlopeSegmentLengthMm.toString())
    var extrusionRateSmoothingExternalPerimeterOnly by mutableStateOf(initial.extrusionRateSmoothingExternalPerimeterOnly)
    var smallPerimeterSpeed by mutableStateOf(initial.smallPerimeterSpeedMmPerSec.toString())
    var smallPerimeterThreshold by mutableStateOf(initial.smallPerimeterThresholdMm.toString())
    var sparseInfillSpeed by mutableStateOf(initial.sparseInfillSpeedMmPerSec.toString())
    var internalSolidInfillSpeed by mutableStateOf(initial.internalSolidInfillSpeedMmPerSec.toString())
    var gapInfillSpeed by mutableStateOf(initial.gapInfillSpeedMmPerSec.toString())
    var adaptiveLayerHeight by mutableStateOf(initial.adaptiveLayerHeight)
    var topShellLayers by mutableStateOf(initial.topShellLayers.toString())
    var bottomShellLayers by mutableStateOf(initial.bottomShellLayers.toString())
    var topShellThickness by mutableStateOf(initial.topShellThicknessMm.toString())
    var bottomShellThickness by mutableStateOf(initial.bottomShellThicknessMm.toString())
    var topSurfaceDensityPercent by mutableStateOf(initial.topSurfaceDensityPercent.toString())
    var bottomSurfaceDensityPercent by mutableStateOf(initial.bottomSurfaceDensityPercent.toString())
    var seamPosition by mutableStateOf(initial.seamPosition)
    var staggeredInnerSeams by mutableStateOf(initial.staggeredInnerSeams)
    var seamGap by mutableStateOf(initial.seamGap)
    var seamScarfType by mutableStateOf(initial.seamScarfType)
    var seamScarfConditional by mutableStateOf(initial.seamScarfConditional)
    var scarfAngleThreshold by mutableStateOf(initial.scarfAngleThresholdDegrees.toString())
    var scarfOverhangThreshold by mutableStateOf(initial.scarfOverhangThresholdPercent.toString())
    var scarfJointSpeed by mutableStateOf(initial.scarfJointSpeed)
    var scarfJointFlowRatio by mutableStateOf(initial.scarfJointFlowRatio.toString())
    var seamScarfStartHeight by mutableStateOf(initial.seamScarfStartHeight)
    var seamScarfEntireLoop by mutableStateOf(initial.seamScarfEntireLoop)
    var seamScarfMinLength by mutableStateOf(initial.seamScarfMinLengthMm.toString())
    var seamScarfSteps by mutableStateOf(initial.seamScarfSteps.toString())
    var seamScarfInnerWalls by mutableStateOf(initial.seamScarfInnerWalls)
    var roleBasedWipeSpeed by mutableStateOf(initial.roleBasedWipeSpeed)
    var wipeSpeed by mutableStateOf(initial.wipeSpeed)
    var wipeOnLoops by mutableStateOf(initial.wipeOnLoops)
    var wipeBeforeExternalLoop by mutableStateOf(initial.wipeBeforeExternalLoop)
    var hasScarfJointSeam by mutableStateOf(initial.hasScarfJointSeam)
    var counterboreHoleBridging by mutableStateOf(initial.counterboreHoleBridging)
    var preciseOuterWall by mutableStateOf(initial.preciseOuterWall)
    var sliceClosingRadius by mutableStateOf(initial.sliceClosingRadiusMm.toString())
    var preciseZHeight by mutableStateOf(initial.preciseZHeight)
    var onlyOneWallFirstLayer by mutableStateOf(initial.onlyOneWallFirstLayer)
    var onlyOneWallTopSurfaces by mutableStateOf(initial.onlyOneWallTopSurfaces)
    var printInfillFirst by mutableStateOf(initial.printInfillFirst)
    var wallDirection by mutableStateOf(initial.wallDirection)
    var printFlowRatio by mutableStateOf(initial.printFlowRatio.toString())
    var topSurfacePattern by mutableStateOf(initial.topSurfacePattern)
    var bottomSurfacePattern by mutableStateOf(initial.bottomSurfacePattern)
    var internalSolidInfillPattern by mutableStateOf(initial.internalSolidInfillPattern)
    var solidInfillDirectionDegrees by mutableStateOf(initial.solidInfillDirectionDegrees.toString())
    var solidInfillRotateTemplate by mutableStateOf(initial.solidInfillRotateTemplate)
    var lineWidth by mutableStateOf(initial.lineWidth)
    var outerWallLineWidth by mutableStateOf(initial.outerWallLineWidth)
    var innerWallLineWidth by mutableStateOf(initial.innerWallLineWidth)
    var initialLayerLineWidth by mutableStateOf(initial.initialLayerLineWidth)
    var initialLayerMinBeadWidthPercent by mutableStateOf(initial.initialLayerMinBeadWidthPercent.toString())
    var elefantFootCompensationLayers by mutableStateOf(initial.elefantFootCompensationLayers.toString())
    var topSurfaceLineWidth by mutableStateOf(initial.topSurfaceLineWidth)
    var internalSolidInfillLineWidth by mutableStateOf(initial.internalSolidInfillLineWidth)
    var minWidthTopSurface by mutableStateOf(initial.minWidthTopSurface)
    var sparseInfillLineWidth by mutableStateOf(initial.sparseInfillLineWidth)
    var infillDirectionDegrees by mutableStateOf(initial.infillDirectionDegrees.toString())
    var sparseInfillRotateTemplate by mutableStateOf(initial.sparseInfillRotateTemplate)
    var alignInfillDirectionToModel by mutableStateOf(initial.alignInfillDirectionToModel)
    var infillWallOverlapPercent by mutableStateOf(initial.infillWallOverlapPercent.toString())
    var topBottomInfillWallOverlapPercent by mutableStateOf(initial.topBottomInfillWallOverlapPercent.toString())
    var infillAnchor by mutableStateOf(initial.infillAnchor)
    var infillAnchorMax by mutableStateOf(initial.infillAnchorMax)
    var infillCombination by mutableStateOf(initial.infillCombination)
    var infillCombinationMaxLayerHeight by mutableStateOf(initial.infillCombinationMaxLayerHeight)
    var minimumSparseInfillAreaMm2 by mutableStateOf(initial.minimumSparseInfillAreaMm2.toString())
    var alternateExtraWall by mutableStateOf(initial.alternateExtraWall)
    var extraSolidInfills by mutableStateOf(initial.extraSolidInfills)
    var detectThinWall by mutableStateOf(initial.detectThinWall)
    var detectOverhangWall by mutableStateOf(initial.detectOverhangWall)
    var makeOverhangPrintable by mutableStateOf(initial.makeOverhangPrintable)
    var makeOverhangPrintableAngle by mutableStateOf(initial.makeOverhangPrintableAngleDegrees.toString())
    var makeOverhangPrintableHoleSize by mutableStateOf(initial.makeOverhangPrintableHoleSizeMm2.toString())
    var overhangReverse by mutableStateOf(initial.overhangReverse)
    var overhangReverseInternalOnly by mutableStateOf(initial.overhangReverseInternalOnly)
    var overhangReverseThreshold by mutableStateOf(initial.overhangReverseThreshold)
    var thickBridges by mutableStateOf(initial.thickBridges)
    var resolution by mutableStateOf(initial.resolutionMm.toString())
    var interfaceShells by mutableStateOf(initial.interfaceShells)
    var dontFilterInternalBridges by mutableStateOf(initial.dontFilterInternalBridges)
    var detectNarrowInternalSolidInfill by mutableStateOf(initial.detectNarrowInternalSolidInfill)
    var elefantFootCompensation by mutableStateOf(initial.elefantFootCompensationMm.toString())
    var applyTopSurfaceCompensation by mutableStateOf(initial.applyTopSurfaceCompensation)
    var ensureVerticalShellThickness by mutableStateOf(initial.ensureVerticalShellThickness)
    var wallGenerator by mutableStateOf(initial.wallGenerator)
    var wallTransitionAngle by mutableStateOf(initial.wallTransitionAngleDegrees.toString())
    var wallTransitionFilterDeviation by mutableStateOf(initial.wallTransitionFilterDeviationPercent.toString())
    var wallTransitionLength by mutableStateOf(initial.wallTransitionLengthPercent.toString())
    var wallDistributionCount by mutableStateOf(initial.wallDistributionCount.toString())
    var minBeadWidth by mutableStateOf(initial.minBeadWidthPercent.toString())
    var minFeatureSize by mutableStateOf(initial.minFeatureSizePercent.toString())
    var minLengthFactor by mutableStateOf(initial.minLengthFactorMm.toString())
    var wallInfillOrder by mutableStateOf(initial.wallInfillOrder)
    var wallSequence by mutableStateOf(initial.wallSequence)
    var enableArcFitting by mutableStateOf(initial.enableArcFitting)
    var reduceCrossingWall by mutableStateOf(initial.reduceCrossingWall)
    var maxTravelDetourDistance by mutableStateOf(initial.maxTravelDetourDistance)
    var holeToPolyhole by mutableStateOf(initial.holeToPolyhole)
    var holeToPolyholeThreshold by mutableStateOf(initial.holeToPolyholeThreshold)
    var holeToPolyholeTwisted by mutableStateOf(initial.holeToPolyholeTwisted)
    var extraPerimetersOnOverhangs by mutableStateOf(initial.extraPerimetersOnOverhangs)
    var xyHoleCompensation by mutableStateOf(initial.xyHoleCompensationMm.toString())
    var xyContourCompensation by mutableStateOf(initial.xyContourCompensationMm.toString())
    var wallCount by mutableStateOf(initial.wallCount.toString())
    var infillPercent by mutableStateOf(initial.infillPercent.toString())
    var sparseInfillPattern by mutableStateOf(initial.sparseInfillPattern)
    var skinInfillDensity by mutableStateOf(initial.skinInfillDensity.toString())
    var skeletonInfillDensity by mutableStateOf(initial.skeletonInfillDensity.toString())
    var infillLockDepth by mutableStateOf(initial.infillLockDepthMm.toString())
    var skinInfillDepth by mutableStateOf(initial.skinInfillDepthMm.toString())
    var skinInfillLineWidth by mutableStateOf(initial.skinInfillLineWidth)
    var skeletonInfillLineWidth by mutableStateOf(initial.skeletonInfillLineWidth)
    var symmetricInfillYAxis by mutableStateOf(initial.symmetricInfillYAxis)
    var infillShiftStep by mutableStateOf(initial.infillShiftStepMm.toString())
    var infillOverhangAngle by mutableStateOf(initial.infillOverhangAngleDegrees.toString())
    var sparseInfillFilament by mutableStateOf(initial.sparseInfillFilament.toString())
    var sparseInfillFlowRatio by mutableStateOf(initial.sparseInfillFlowRatio.toString())
    var internalSolidInfillFlowRatio by mutableStateOf(initial.internalSolidInfillFlowRatio.toString())
    var setOtherFlowRatios by mutableStateOf(initial.setOtherFlowRatios)
    var smallAreaInfillFlowCompensation by mutableStateOf(initial.smallAreaInfillFlowCompensation)
    var lateralLatticeAngle1 by mutableStateOf(initial.lateralLatticeAngle1Degrees.toString())
    var lateralLatticeAngle2 by mutableStateOf(initial.lateralLatticeAngle2Degrees.toString())
    var fillMultiline by mutableStateOf(initial.fillMultiline.toString())
    var gapFillTarget by mutableStateOf(initial.gapFillTarget.takeIf { it in ORCA_GAP_FILL_TARGETS } ?: "nowhere")
    var filterOutGapFill by mutableStateOf(initial.filterOutGapFillMm.toString())
    var gapFillFlowRatio by mutableStateOf(initial.gapFillFlowRatio.toString())
    var enableSupport by mutableStateOf(initial.enableSupport)
    var supportType by mutableStateOf(initial.supportType)
    var supportStyle by mutableStateOf(initial.supportStyle)
    var supportThresholdAngle by mutableStateOf(initial.supportThresholdAngleDegrees.toString())
    var supportThresholdOverlap by mutableStateOf(initial.supportThresholdOverlap)
    var supportBuildplateOnly by mutableStateOf(initial.supportBuildplateOnly)
    var supportCriticalRegionsOnly by mutableStateOf(initial.supportCriticalRegionsOnly)
    var supportRemoveSmallOverhang by mutableStateOf(initial.supportRemoveSmallOverhang)
    var raftFirstLayerDensity by mutableStateOf(initial.raftFirstLayerDensityPercent.toString())
    var raftFirstLayerExpansion by mutableStateOf(initial.raftFirstLayerExpansionMm.toString())
    var raftLayers by mutableStateOf(initial.raftLayers.toString())
    var raftContactDistance by mutableStateOf(initial.raftContactDistanceMm.toString())
    var raftExpansion by mutableStateOf(initial.raftExpansionMm.toString())
    var supportFilament by mutableStateOf(initial.supportFilament.toString())
    var supportInterfaceFilament by mutableStateOf(initial.supportInterfaceFilament.toString())
    var supportInterfaceNotForBody by mutableStateOf(initial.supportInterfaceNotForBody)
    var supportTopZDistance by mutableStateOf(initial.supportTopZDistanceMm.toString())
    var supportBottomZDistance by mutableStateOf(initial.supportBottomZDistanceMm.toString())
    var supportInterfaceTopLayers by mutableStateOf(initial.supportInterfaceTopLayers.toString())
    var supportInterfaceBottomLayers by mutableStateOf(initial.supportInterfaceBottomLayers.toString())
    var supportInterfaceSpacing by mutableStateOf(initial.supportInterfaceSpacingMm.toString())
    var supportBottomInterfaceSpacing by mutableStateOf(initial.supportBottomInterfaceSpacingMm.toString())
    var supportInterfaceSpeed by mutableStateOf(initial.supportInterfaceSpeedMmPerSec.toString())
    var supportInterfaceFlowRatio by mutableStateOf(initial.supportInterfaceFlowRatio.toString())
    var supportMaterialInterfaceFanSpeed by mutableStateOf(initial.supportMaterialInterfaceFanSpeed)
    var supportInterfacePattern by mutableStateOf(initial.supportInterfacePattern)
    var supportInterfaceLoopPattern by mutableStateOf(initial.supportInterfaceLoopPattern)
    var supportLineWidth by mutableStateOf(initial.supportLineWidth)
    var supportBasePattern by mutableStateOf(initial.supportBasePattern)
    var supportBasePatternSpacing by mutableStateOf(initial.supportBasePatternSpacingMm.toString())
    var supportAngle by mutableStateOf(initial.supportAngleDegrees.toString())
    var supportSpeed by mutableStateOf(initial.supportSpeedMmPerSec.toString())
    var supportFlowRatio by mutableStateOf(initial.supportFlowRatio.toString())
    var supportObjectElevation by mutableStateOf(initial.supportObjectElevationMm.toString())
    var supportMaxBridgeLength by mutableStateOf(initial.supportMaxBridgeLengthMm.toString())
    var supportIroning by mutableStateOf(initial.supportIroning)
    var supportIroningPattern by mutableStateOf(initial.supportIroningPattern)
    var supportIroningFlow by mutableStateOf(initial.supportIroningFlowPercent.toString())
    var supportIroningSpacing by mutableStateOf(initial.supportIroningSpacingMm.toString())
    var supportExpansion by mutableStateOf(initial.supportExpansionMm.toString())
    var supportObjectXyDistance by mutableStateOf(initial.supportObjectXyDistanceMm.toString())
    var supportObjectFirstLayerGap by mutableStateOf(initial.supportObjectFirstLayerGapMm.toString())
    var independentSupportLayerHeight by mutableStateOf(initial.independentSupportLayerHeight)
    var treeSupportTipDiameter by mutableStateOf(initial.treeSupportTipDiameterMm.toString())
    var treeSupportBranchDistance by mutableStateOf(initial.treeSupportBranchDistanceMm.toString())
    var treeSupportBranchDistanceOrganic by mutableStateOf(initial.treeSupportBranchDistanceOrganicMm.toString())
    var treeSupportTopRate by mutableStateOf(initial.treeSupportTopRatePercent.toString())
    var treeSupportBranchAngle by mutableStateOf(initial.treeSupportBranchAngleDegrees.toString())
    var treeSupportBranchDiameter by mutableStateOf(initial.treeSupportBranchDiameterMm.toString())
    var treeSupportBranchDiameterOrganic by mutableStateOf(initial.treeSupportBranchDiameterOrganicMm.toString())
    var treeSupportBranchDiameterAngle by mutableStateOf(initial.treeSupportBranchDiameterAngleDegrees.toString())
    var treeSupportBranchAngleOrganic by mutableStateOf(initial.treeSupportBranchAngleOrganicDegrees.toString())
    var treeSupportPreferredBranchAngle by mutableStateOf(initial.treeSupportPreferredBranchAngleDegrees.toString())
    var treeSupportAutoBrim by mutableStateOf(initial.treeSupportAutoBrim)
    var treeSupportBrimWidth by mutableStateOf(initial.treeSupportBrimWidthMm.toString())
    var treeSupportWallCount by mutableStateOf(initial.treeSupportWallCount.toString())
    var enablePrimeTower by mutableStateOf(initial.enablePrimeTower)
    var primeTowerWidth by mutableStateOf(initial.primeTowerWidthMm.toString())
    var primeTowerDetails by mutableStateOf(PrimeTowerDetailsDraft.fromProfile(initial))
    var enableTowerInterfaceFeatures by mutableStateOf(initial.enableTowerInterfaceFeatures)
    var enableTowerInterfaceCooldownDuringTower by mutableStateOf(initial.enableTowerInterfaceCooldownDuringTower)
    var singleExtruderMultiMaterialPriming by mutableStateOf(initial.singleExtruderMultiMaterialPriming)
    var standbyTemperatureDelta by mutableStateOf(initial.standbyTemperatureDeltaC.toString())
    var wipeTowerNoSparseLayers by mutableStateOf(initial.wipeTowerNoSparseLayers)
    var flushIntoInfill by mutableStateOf(initial.flushIntoInfill)
    var flushIntoObjects by mutableStateOf(initial.flushIntoObjects)
    var flushIntoSupport by mutableStateOf(initial.flushIntoSupport)
    var skirts by mutableStateOf(initial.skirts.toString())
    var skirtType by mutableStateOf(initial.skirtType)
    var minSkirtLength by mutableStateOf(initial.minSkirtLengthMm.toString())
    var skirtDistance by mutableStateOf(initial.skirtDistanceMm.toString())
    var skirtStartAngle by mutableStateOf(initial.skirtStartAngleDegrees.toString())
    var skirtSpeed by mutableStateOf(initial.skirtSpeedMmPerSec.toString())
    var skirtHeight by mutableStateOf(initial.skirtHeightLayers.toString())
    var draftShield by mutableStateOf(initial.draftShield)
    var singleLoopDraftShield by mutableStateOf(initial.singleLoopDraftShield)
    var brimType by mutableStateOf(initial.brimType)
    var brimWidth by mutableStateOf(initial.brimWidthMm.toString())
    var brimObjectGap by mutableStateOf(initial.brimObjectGapMm.toString())
    var brimUseEfcOutline by mutableStateOf(initial.brimUseEfcOutline)
    var combineBrims by mutableStateOf(initial.combineBrims)
    var brimEars by mutableStateOf(initial.brimEars)
    var brimEarsDetectionLength by mutableStateOf(initial.brimEarsDetectionLengthMm.toString())
    var brimEarsMaxAngle by mutableStateOf(initial.brimEarsMaxAngleDegrees.toString())
    var slicingMode by mutableStateOf(initial.slicingMode)
    var printSequence by mutableStateOf(initial.printSequence)
    var printOrder by mutableStateOf(initial.printOrder)
    var spiralMode by mutableStateOf(initial.spiralMode)
    var specialModeDetails by mutableStateOf(SpecialModeDetailsDraft.fromProfile(initial))
    var reduceInfillRetraction by mutableStateOf(initial.reduceInfillRetraction)
    var gcodeOutputDetails by mutableStateOf(GcodeOutputDetailsDraft.fromProfile(initial))
    var filamentMapMode by mutableStateOf(initial.filamentMapMode)
    var allowMixTemp by mutableStateOf(initial.allowMixTemp)
    var allowMulticolorOnePlate by mutableStateOf(initial.allowMulticolorOnePlate)
    var filenameFormat by mutableStateOf(initial.filenameFormat)
    var postProcessScripts by mutableStateOf(initial.postProcessScripts)
    var processNotes by mutableStateOf(initial.notes)
    var fuzzySkin by mutableStateOf(initial.fuzzySkin)
    var fuzzySkinThickness by mutableStateOf(initial.fuzzySkinThicknessMm.toString())
    var fuzzySkinPointDistance by mutableStateOf(initial.fuzzySkinPointDistanceMm.toString())
    var fuzzySkinFirstLayer by mutableStateOf(initial.fuzzySkinFirstLayer)
    var fuzzySkinMode by mutableStateOf(initial.fuzzySkinMode)
    var fuzzySkinNoiseType by mutableStateOf(initial.fuzzySkinNoiseType)
    var fuzzySkinScale by mutableStateOf(initial.fuzzySkinScaleMm.toString())
    var fuzzySkinOctaves by mutableStateOf(initial.fuzzySkinOctaves.toString())
    var fuzzySkinPersistence by mutableStateOf(initial.fuzzySkinPersistence.toString())
    var ironingType by mutableStateOf(initial.ironingType)
    var ironingPattern by mutableStateOf(initial.ironingPattern)
    var ironingFlow by mutableStateOf(initial.ironingFlowPercent.toString())
    var ironingSpacing by mutableStateOf(initial.ironingSpacingMm.toString())
    var ironingInset by mutableStateOf(initial.ironingInsetMm.toString())
    var ironingAngle by mutableStateOf(initial.ironingAngleDegrees.toString())
    var ironingAngleFixed by mutableStateOf(initial.ironingAngleFixed)
    var ironingSpeed by mutableStateOf(initial.ironingSpeedMmPerSec.toString())
    var selectedTab by mutableStateOf(ProcessEditorTab.Quality)
}
