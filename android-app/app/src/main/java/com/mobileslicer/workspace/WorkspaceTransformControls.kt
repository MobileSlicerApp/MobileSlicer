package com.mobileslicer.workspace

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.OpenableColumns
import com.mobileslicer.CompactWorkspaceBadge
import com.mobileslicer.R
import com.mobileslicer.appBackgroundGradient
import com.mobileslicer.appBodyColor
import com.mobileslicer.appCardColor
import com.mobileslicer.appCardColorMuted
import com.mobileslicer.appMutedColor
import com.mobileslicer.appOutlineColor
import com.mobileslicer.appTitleColor
import com.mobileslicer.profiles.ActiveSlicerConfiguration
import com.mobileslicer.profiles.PrintHostType
import com.mobileslicer.profiles.PrinterProfile
import com.mobileslicer.printerconnection.PrinterUploadAction
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
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.mobileslicer.viewer.MeshBounds
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
import com.mobileslicer.ui.theme.WorldViewColorOption
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sqrt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransformPopoverContent(
    printerBed: PrinterBedSpec,
    transform: ViewerModelTransform,
    onTransformChanged: (ViewerModelTransform?) -> Unit,
    onAutoOrientObjects: () -> Unit,
    onAutoArrangeObjects: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = appTitleColor()
    val outlineColor = appOutlineColor()
    var selectedTab by remember { mutableStateOf(TransformToolTab.Move) }
    var editingField by remember { mutableStateOf<TransformNumericField?>(null) }
    fun update(next: ViewerModelTransform) {
        onTransformChanged(
            next.copy(
                centerXmm = next.centerXmm.coerceIn(0f, printerBed.widthMm),
                centerYmm = next.centerYmm.coerceIn(0f, printerBed.depthMm),
                rotationXDegrees = normalizeDegrees(next.rotationXDegrees),
                rotationYDegrees = normalizeDegrees(next.rotationYDegrees),
                rotationZDegrees = normalizeDegrees(next.rotationZDegrees),
                uniformScale = next.uniformScale.coerceIn(0.05f, 20f)
            )
        )
    }
    fun runAutoOrient() {
        onAutoOrientObjects()
    }
    fun runAutoArrange() {
        onAutoArrangeObjects()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = appCardColor().copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 9.dp, end = 16.dp, bottom = 13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TransformToolTab.values().forEach { tab ->
                    val selected = selectedTab == tab
                    IconButton(
                        onClick = {
                            selectedTab = tab
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f) else appCardColorMuted().copy(alpha = 0.72f))
                            .border(1.dp, outlineColor.copy(alpha = if (selected) 0.18f else 0.52f), RoundedCornerShape(12.dp))
                    ) {
                        TransformToolIcon(
                            type = tab,
                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else titleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            when (selectedTab) {
                TransformToolTab.Move -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TransformSliderRow(
                            label = "X",
                            valueText = String.format(Locale.US, "%.1f mm", transform.centerXmm),
                            value = transform.centerXmm,
                            range = 0f..printerBed.widthMm,
                            onValueChange = { update(transform.copy(centerXmm = it)) },
                            onValueClick = { editingField = TransformNumericField.MoveX },
                            modifier = Modifier.weight(1f)
                        )
                        TransformSliderRow(
                            label = "Y",
                            valueText = String.format(Locale.US, "%.1f mm", transform.centerYmm),
                            value = transform.centerYmm,
                            range = 0f..printerBed.depthMm,
                            onValueChange = { update(transform.copy(centerYmm = it)) },
                            onValueClick = { editingField = TransformNumericField.MoveY },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TransformToolTab.Rotate -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TransformSliderRow(
                            label = "X",
                            valueText = String.format(Locale.US, "%.0f°", transform.rotationXDegrees),
                            value = transform.rotationXDegrees,
                            range = -180f..180f,
                            onValueChange = { update(transform.copy(rotationXDegrees = it)) },
                            onValueClick = { editingField = TransformNumericField.RotateX },
                            modifier = Modifier.weight(1f)
                        )
                        TransformSliderRow(
                            label = "Y",
                            valueText = String.format(Locale.US, "%.0f°", transform.rotationYDegrees),
                            value = transform.rotationYDegrees,
                            range = -180f..180f,
                            onValueChange = { update(transform.copy(rotationYDegrees = it)) },
                            onValueClick = { editingField = TransformNumericField.RotateY },
                            modifier = Modifier.weight(1f)
                        )
                        TransformSliderRow(
                            label = "Z",
                            valueText = String.format(Locale.US, "%.0f°", transform.rotationZDegrees),
                            value = transform.rotationZDegrees,
                            range = -180f..180f,
                            onValueChange = { update(transform.copy(rotationZDegrees = it)) },
                            onValueClick = { editingField = TransformNumericField.RotateZ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TransformToolTab.Scale -> {
                    TransformSliderRow(
                        label = "Uniform",
                        valueText = String.format(Locale.US, "%.0f%%", transform.uniformScale * 100f),
                        value = transform.uniformScale,
                        range = 0.25f..3f,
                        onValueChange = { update(transform.copy(uniformScale = it)) },
                        onValueClick = { editingField = TransformNumericField.Scale },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TransformToolTab.AutoOrient -> {
                    FilledTonalButton(
                        onClick = { runAutoOrient() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        TransformToolIcon(
                            type = TransformToolTab.AutoOrient,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Auto orient")
                    }
                }
                TransformToolTab.AutoArrange -> {
                    FilledTonalButton(
                        onClick = { runAutoArrange() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        TransformToolIcon(
                            type = TransformToolTab.AutoArrange,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Auto arrange")
                    }
                }
            }
        }
    }
    editingField?.let { field ->
        TransformValueDialog(
            field = field,
            transform = transform,
            printerBed = printerBed,
            onDismiss = { editingField = null },
            onApply = { value ->
                when (field) {
                    TransformNumericField.MoveX -> update(transform.copy(centerXmm = value))
                    TransformNumericField.MoveY -> update(transform.copy(centerYmm = value))
                    TransformNumericField.RotateX -> update(transform.copy(rotationXDegrees = value))
                    TransformNumericField.RotateY -> update(transform.copy(rotationYDegrees = value))
                    TransformNumericField.RotateZ -> update(transform.copy(rotationZDegrees = value))
                    TransformNumericField.Scale -> update(transform.copy(uniformScale = value / 100f))
                }
                editingField = null
            }
        )
    }
}

@Composable
internal fun TransformToolIcon(
    type: TransformToolTab,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        when (type) {
            TransformToolTab.Move -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.88f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.38f, h * 0.24f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.62f, h * 0.24f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.76f, h * 0.38f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.88f, h * 0.5f), Offset(w * 0.76f, h * 0.62f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TransformToolTab.Rotate -> {
                drawArc(tint, startAngle = 35f, sweepAngle = 285f, useCenter = false, topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), style = stroke)
                drawLine(tint, Offset(w * 0.78f, h * 0.24f), Offset(w * 0.88f, h * 0.14f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.78f, h * 0.24f), Offset(w * 0.92f, h * 0.28f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TransformToolTab.Scale -> {
                drawRect(tint, topLeft = Offset(w * 0.25f, h * 0.25f), size = Size(w * 0.50f, h * 0.50f), style = stroke)
                drawLine(tint, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.43f, h * 0.57f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.36f, h * 0.80f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.80f), Offset(w * 0.20f, h * 0.64f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.80f, h * 0.20f), Offset(w * 0.57f, h * 0.43f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.80f, h * 0.20f), Offset(w * 0.64f, h * 0.20f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.80f, h * 0.20f), Offset(w * 0.80f, h * 0.36f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TransformToolTab.AutoOrient -> {
                val path = Path().apply {
                    moveTo(w * 0.24f, h * 0.76f)
                    lineTo(w * 0.56f, h * 0.24f)
                    lineTo(w * 0.82f, h * 0.76f)
                    close()
                }
                drawPath(path, tint, style = stroke)
                drawArc(tint, startAngle = 200f, sweepAngle = 115f, useCenter = false, topLeft = Offset(w * 0.13f, h * 0.13f), size = Size(w * 0.74f, h * 0.74f), style = stroke)
                drawLine(tint, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.10f, h * 0.34f), strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.18f, h * 0.50f), Offset(w * 0.36f, h * 0.44f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            TransformToolTab.AutoArrange -> {
                drawRect(tint, topLeft = Offset(w * 0.16f, h * 0.16f), size = Size(w * 0.68f, h * 0.68f), style = stroke)
                drawRect(tint, topLeft = Offset(w * 0.24f, h * 0.24f), size = Size(w * 0.18f, h * 0.18f), style = stroke)
                drawRect(tint, topLeft = Offset(w * 0.58f, h * 0.24f), size = Size(w * 0.18f, h * 0.18f), style = stroke)
                drawRect(tint, topLeft = Offset(w * 0.24f, h * 0.58f), size = Size(w * 0.18f, h * 0.18f), style = stroke)
                drawRect(tint, topLeft = Offset(w * 0.58f, h * 0.58f), size = Size(w * 0.18f, h * 0.18f), style = stroke)
            }
        }
    }
}

@Composable
internal fun TransformValueDialog(
    field: TransformNumericField,
    transform: ViewerModelTransform,
    printerBed: PrinterBedSpec,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit
) {
    val (title, initialValue, suffix) = when (field) {
        TransformNumericField.MoveX -> Triple("X position", transform.centerXmm, "mm")
        TransformNumericField.MoveY -> Triple("Y position", transform.centerYmm, "mm")
        TransformNumericField.RotateX -> Triple("X rotation", transform.rotationXDegrees, "deg")
        TransformNumericField.RotateY -> Triple("Y rotation", transform.rotationYDegrees, "deg")
        TransformNumericField.RotateZ -> Triple("Z rotation", transform.rotationZDegrees, "deg")
        TransformNumericField.Scale -> Triple("Scale", transform.uniformScale * 100f, "%")
    }
    var text by remember(field, initialValue) {
        mutableStateOf(String.format(Locale.US, "%.1f", initialValue))
    }
    val parsedValue = text.toFloatOrNull()
    val validValue = parsedValue?.let { value ->
        when (field) {
            TransformNumericField.MoveX -> value.coerceIn(0f, printerBed.widthMm)
            TransformNumericField.MoveY -> value.coerceIn(0f, printerBed.depthMm)
            TransformNumericField.RotateX,
            TransformNumericField.RotateY,
            TransformNumericField.RotateZ -> normalizeDegrees(value)
            TransformNumericField.Scale -> value.coerceIn(5f, 2_000f)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                suffix = { Text(suffix) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { validValue?.let(onApply) },
                enabled = validValue != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun TransformSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = appTitleColor()
    val bodyColor = appBodyColor()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = titleColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = bodyColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(appCardColorMuted().copy(alpha = 0.76f))
                    .border(1.dp, appOutlineColor().copy(alpha = 0.36f), RoundedCornerShape(9.dp))
                    .clickable(onClick = onValueClick)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = workspaceSliderColors(),
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        )
    }
}

@Composable
internal fun workspaceSliderColors() = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.primary,
    activeTrackColor = MaterialTheme.colorScheme.primary,
    activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f),
    inactiveTrackColor = if (LocalAppDarkTheme.current) {
        Color(0xFF465160)
    } else {
        Color(0xFFD7DCE3)
    },
    inactiveTickColor = if (LocalAppDarkTheme.current) {
        Color(0xFF6D7784)
    } else {
        Color(0xFFAEB6C0)
    },
    disabledThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
    disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.46f),
    disabledActiveTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.24f),
    disabledInactiveTrackColor = if (LocalAppDarkTheme.current) {
        Color(0xFF465160).copy(alpha = 0.72f)
    } else {
        Color(0xFFD7DCE3)
    },
    disabledInactiveTickColor = if (LocalAppDarkTheme.current) {
        Color(0xFF6D7784).copy(alpha = 0.72f)
    } else {
        Color(0xFFAEB6C0)
    }
)
