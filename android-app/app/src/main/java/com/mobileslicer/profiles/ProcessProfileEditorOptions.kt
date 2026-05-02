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



internal class ProcessProfileEditorOptions {
    val seamPositionOptions = ProcessSeamPosition.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca seam_position = ${option.configValue}.") }
    val seamScarfTypeOptions = SeamScarfType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca seam_slope_type = ${option.configValue}.") }
    val wallSequenceOptions = WallSequence.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca wall_sequence = ${option.configValue}.") }
    val wallDirectionOptions = WallDirection.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca wall_direction = ${option.configValue}.") }
    val extraBridgeLayerOptions = ExtraBridgeLayerMode.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca enable_extra_bridge_layer = ${option.configValue}.") }
    val skirtTypeOptions = SkirtType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca skirt_type = ${option.configValue}.") }
    val draftShieldOptions = DraftShield.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca draft_shield = ${option.configValue}.") }
    val brimTypeOptions = BrimType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca brim_type = ${option.configValue}.") }
    val slicingModeOptions = SlicingMode.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca slicing_mode = ${option.configValue}.") }
    val printSequenceOptions = PrintSequence.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca print_sequence = ${option.configValue}.") }
    val printOrderOptions = PrintOrder.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca print_order = ${option.configValue}.") }
    val wipeTowerWallTypeOptions = WipeTowerWallType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca wipe_tower_wall_type = ${option.configValue}.") }
    val timelapseTypeOptions = TimelapseType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca timelapse_type = ${option.configValue}.") }
    val filamentMapModeOptions = FilamentMapMode.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca filament_map_mode = ${option.configValue}.") }
    val counterboreHoleBridgingOptions = CounterboreHoleBridging.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca counterbore_hole_bridging = ${option.configValue}.") }
    val topSurfacePatternOptions = TopSurfacePattern.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca top_surface_pattern = ${option.configValue}.") }
    val bottomSurfacePatternOptions = BottomSurfacePattern.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca bottom_surface_pattern = ${option.configValue}.") }
    val internalSolidPatternOptions = InternalSolidInfillPattern.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca internal_solid_infill_pattern = ${option.configValue}.") }
    val boolEnabledDisabledOptions =
        listOf(
            AppSettingOption(true, "Enabled", ""),
            AppSettingOption(false, "Disabled", "")
        )
    val infillCombinationOptions = boolEnabledDisabledOptions
    val internalBridgeFilterOptions = InternalBridgeFilterMode.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca dont_filter_internal_bridges = ${option.configValue}.") }
    val ensureVerticalShellThicknessOptions =
        EnsureVerticalShellThicknessMode.entries.map { option ->
            AppSettingOption(option, option.displayLabel, "Maps directly to Orca ensure_vertical_shell_thickness = ${option.configValue}.")
        }
    val wallGeneratorOptions = WallGenerator.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca wall_generator = ${option.configValue}.") }
    val wallInfillOrderOptions = WallInfillOrder.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca wall_infill_order = ${option.configValue}.") }
    val sparseInfillPatternOptions = SparseInfillPattern.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca sparse_infill_pattern = ${option.configValue}.") }
    val gapFillTargetOptions =
        listOf(
            AppSettingOption("everywhere", "Everywhere", "Maps directly to Orca gap_fill_target = everywhere."),
            AppSettingOption("topbottom", "Top and bottom surfaces", "Maps directly to Orca gap_fill_target = topbottom."),
            AppSettingOption("nowhere", "Nowhere", "Maps directly to Orca gap_fill_target = nowhere.")
        )
    val supportTypeOptions = SupportType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca support_type = ${option.configValue}.") }
    val supportStyleOptions = SupportStyle.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca support_style = ${option.configValue}.") }
    val supportInterfacePatternOptions =
        SupportInterfacePattern.entries.map { option ->
            AppSettingOption(option, option.displayLabel, "Maps directly to Orca support_interface_pattern = ${option.configValue}.")
        }
    val supportBasePatternOptions =
        SupportBasePattern.entries.map { option ->
            AppSettingOption(option, option.displayLabel, "Maps directly to Orca support_base_pattern = ${option.configValue}.")
        }
    val fuzzySkinOptions = FuzzySkinType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca fuzzy_skin = ${option.configValue}.") }
    val fuzzySkinModeOptions = FuzzySkinMode.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca fuzzy_skin_mode = ${option.configValue}.") }
    val fuzzySkinNoiseOptions = FuzzySkinNoiseType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca fuzzy_skin_noise_type = ${option.configValue}.") }
    val ironingTypeOptions = ProcessIroningType.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca ironing_type = ${option.configValue}.") }
    val ironingPatternOptions = IroningPattern.entries.map { option -> AppSettingOption(option, option.displayLabel, "Maps directly to Orca ironing_pattern = ${option.configValue}.") }
}
