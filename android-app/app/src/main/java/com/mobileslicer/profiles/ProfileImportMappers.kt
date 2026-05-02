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

internal fun JSONObject.profileConfigString(key: String, defaultValue: String = ""): String {
    if (!has(key) || isNull(key)) return defaultValue
    val value = opt(key)
    return when (value) {
        is JSONArray -> if (value.length() == 1) value.optString(0, defaultValue) else value.toString()
        else -> value?.toString().orEmpty().ifBlank { defaultValue }
    }
}

internal fun JSONObject.profileConfigPointString(key: String, defaultValue: String): String {
    if (!has(key) || isNull(key)) return defaultValue
    val value = opt(key)
    val values = when (value) {
        is JSONArray -> List(value.length()) { index -> value.opt(index)?.toString().orEmpty().trim().trim('"') }
        is String -> {
            val trimmed = value.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val parsed = runCatching { JSONArray(trimmed) }.getOrNull() ?: return trimmed.ifBlank { defaultValue }
                List(parsed.length()) { index -> parsed.opt(index)?.toString().orEmpty().trim().trim('"') }
            } else {
                return trimmed.ifBlank { defaultValue }
            }
        }
        else -> return value?.toString().orEmpty().ifBlank { defaultValue }
    }.filter { it.isNotBlank() && it != "nil" }
    return if (values.size >= 2) "${values[0]},${values[1]}" else defaultValue
}

internal val ORCA_GAP_FILL_TARGETS = setOf("everywhere", "topbottom", "nowhere")

internal fun parseProfileSelection(raw: String): Set<String> {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return emptySet()
    if (trimmed.startsWith("[")) {
        return runCatching {
            val array = JSONArray(trimmed)
            List(array.length()) { index -> array.optString(index).trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }.getOrElse { emptySet() }
    }
    return trimmed
        .split(';', '\n')
        .map { it.trim().trim('"') }
        .filter { it.isNotBlank() }
        .toSet()
}

internal fun encodeProfileSelection(selection: Iterable<String>): String =
    selection.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(";")

internal fun Set<String>.toggleSelection(value: String, selected: Boolean): Set<String> =
    if (selected) this + value else this - value

internal fun JSONObject.profileConfigFloat(key: String, defaultValue: Float): Float {
    val value = profileConfigScalar(key) ?: return defaultValue
    return value.toString().trim().toFloatOrNull() ?: defaultValue
}

internal fun JSONObject.profileConfigInt(key: String, defaultValue: Int): Int {
    val value = profileConfigScalar(key) ?: return defaultValue
    return value.toString().trim().toIntOrNull()
        ?: value.toString().trim().toFloatOrNull()?.toInt()
        ?: defaultValue
}

internal fun JSONObject.profileConfigOptionalString(key: String): String? =
    profileConfigScalar(key)?.toString()?.takeIf { it.isNotBlank() }

internal fun JSONObject.profileConfigOptionalFloat(key: String): Float? {
    val value = profileConfigScalar(key) ?: return null
    return value.toString().trim().toFloatOrNull()
}

internal fun JSONObject.profileConfigOptionalInt(key: String): Int? {
    val value = profileConfigScalar(key) ?: return null
    return value.toString().trim().toIntOrNull()
        ?: value.toString().trim().toFloatOrNull()?.toInt()
}

internal fun JSONObject.profileConfigOptionalBoolean(key: String): Boolean? {
    val value = profileConfigScalar(key) ?: return null
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        else -> when (value.toString().trim().lowercase(Locale.US)) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> null
        }
    }
}

internal fun JSONObject.profileConfigBoolean(key: String, defaultValue: Boolean): Boolean {
    val value = profileConfigScalar(key) ?: return defaultValue
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        else -> when (value.toString().trim().lowercase(Locale.US)) {
            "1", "true", "yes", "on" -> true
            "0", "false", "no", "off" -> false
            else -> defaultValue
        }
    }
}

internal fun JSONObject.profileConfigScalar(key: String): Any? {
    if (!has(key) || isNull(key)) return null
    val value = opt(key)
    return if (value is JSONArray) {
        if (value.length() == 0) null else value.opt(0)
    } else {
        value
    }
}

internal val filamentColorCache = Collections.synchronizedMap(mutableMapOf<String, Color>())

internal fun filamentColor(colorValue: String): Color {
    filamentColorCache[colorValue]?.let { return it }
    val cleaned = colorValue.trim().removePrefix("#")
    val hex = when (cleaned.length) {
        6 -> cleaned
        8 -> cleaned.take(6)
        else -> ""
    }
    val parsed = runCatching {
        Color(android.graphics.Color.parseColor("#$hex"))
    }.getOrDefault(PanelGreen.copy(alpha = 0.26f))
    filamentColorCache[colorValue] = parsed
    return parsed
}

internal fun profileDefaultFilamentDensityForMaterial(materialType: String): Float =
    when (materialType.uppercase(Locale.US)) {
        "ABS", "ASA" -> 1.04f
        "PETG", "PCTG" -> 1.27f
        "TPU", "TPE" -> 1.20f
        "PA", "PA-CF" -> 1.14f
        "PC" -> 1.20f
        else -> 1.24f
    }

internal fun profileDefaultFilamentMaxVolumetricSpeedForMaterial(materialType: String): Float =
    when (materialType.uppercase(Locale.US)) {
        "ABS", "ASA" -> 11f
        "PETG", "PCTG" -> 10f
        "TPU", "TPE" -> 3.5f
        "PA", "PA-CF", "PC" -> 8f
        else -> 12f
    }
