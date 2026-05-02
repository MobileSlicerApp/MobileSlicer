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
import android.util.Log
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
internal fun CompactPreviewRangeSlider(
    layerCount: Int,
    selection: PreviewLayerSelection,
    maxRangeLayerSpan: Int,
    rangeSliderBounds: IntRange?,
    onSelectionChanged: (PreviewLayerSelection) -> Unit,
    onSelectionCommitted: (PreviewLayerSelection) -> Unit,
    onExpand: () -> Unit
) {
    val count = layerCount.coerceAtLeast(1)
    val maxSpan = maxRangeLayerSpan.coerceAtLeast(1)
    val sliderStart = rangeSliderBounds?.first?.coerceIn(1, count) ?: 1
    val sliderEnd = rangeSliderBounds?.last?.coerceIn(sliderStart, count) ?: count
    var draftSelection by remember(count, maxRangeLayerSpan, rangeSliderBounds, selection) { mutableStateOf(selection) }
    val selectedStart = draftSelection.rangeStartLayer.coerceIn(sliderStart, sliderEnd)
    val selectedEnd = if (maxSpan >= sliderEnd - sliderStart + 1) {
        draftSelection.rangeEndLayer.coerceIn(selectedStart, sliderEnd)
    } else {
        draftSelection.rangeEndLayer.coerceIn(selectedStart, sliderEnd).coerceAtMost(selectedStart + maxSpan - 1)
    }

    if (count > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RangeSlider(
                value = selectedStart.toFloat()..selectedEnd.toFloat(),
                onValueChange = { values ->
                    val requestedStart = values.start.toInt().coerceIn(sliderStart, sliderEnd)
                    val requestedEnd = values.endInclusive.toInt().coerceIn(requestedStart, sliderEnd)
                    val start: Int
                    val end: Int
                    if (maxSpan >= sliderEnd - sliderStart + 1) {
                        start = requestedStart
                        end = requestedEnd
                    } else {
                        val startDelta = kotlin.math.abs(requestedStart - selectedStart)
                        val endDelta = kotlin.math.abs(requestedEnd - selectedEnd)
                        if (endDelta > startDelta) {
                            end = requestedEnd.coerceIn(sliderStart + maxSpan - 1, sliderEnd)
                            start = (end - maxSpan + 1).coerceIn(sliderStart, sliderEnd)
                        } else {
                            start = requestedStart.coerceIn(sliderStart, sliderEnd - maxSpan + 1)
                            end = (start + maxSpan - 1).coerceIn(start, sliderEnd)
                        }
                    }
                    val next = draftSelection.copy(
                        mode = PreviewLayerMode.Range,
                        rangeStartLayer = start,
                        rangeEndLayer = end
                    )
                    draftSelection = next
                    onSelectionChanged(next)
                },
                onValueChangeFinished = {
                    onSelectionCommitted(draftSelection)
                },
                valueRange = sliderStart.toFloat()..sliderEnd.toFloat(),
                steps = (sliderEnd - sliderStart - 1).coerceAtLeast(0),
                colors = workspaceSliderColors(),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            TextButton(
                onClick = onExpand,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text("Menu")
            }
        }
    }
}

@Composable
internal fun PreviewLayerControls(
    layerCount: Int,
    selection: PreviewLayerSelection,
    sliceSummary: SliceResultSummary,
    maxRangeLayerSpan: Int,
    rangeSliderBounds: IntRange?,
    onSelectionChanged: (PreviewLayerSelection) -> Unit,
    onSelectionCommitted: (PreviewLayerSelection) -> Unit
) {
    val count = layerCount.coerceAtLeast(1)
    val bodyColor = appBodyColor()
    val maxSpan = maxRangeLayerSpan.coerceAtLeast(1)
    val sliderStart = rangeSliderBounds?.first?.coerceIn(1, count) ?: 1
    val sliderEnd = rangeSliderBounds?.last?.coerceIn(sliderStart, count) ?: count
    var draftSelection by remember(count, maxRangeLayerSpan, rangeSliderBounds, selection) { mutableStateOf(selection) }
    val selectedStart = draftSelection.rangeStartLayer.coerceIn(sliderStart, sliderEnd)
    val selectedEnd = if (maxSpan >= sliderEnd - sliderStart + 1) {
        draftSelection.rangeEndLayer.coerceIn(selectedStart, sliderEnd)
    } else {
        draftSelection.rangeEndLayer.coerceIn(selectedStart, sliderEnd).coerceAtMost(selectedStart + maxSpan - 1)
    }
    val singleLayer = draftSelection.singleLayer.coerceIn(1, count)
    val rangeText = when (draftSelection.mode) {
        PreviewLayerMode.Single -> "Layer $singleLayer of $count"
        PreviewLayerMode.Range -> "Layer $selectedStart-$selectedEnd of $count"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$rangeText • Time: ${sliceSummary.estimatedPrintTimeLabel()} • Filament: ${sliceSummary.filamentUsedLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = bodyColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            PreviewLayerModeButton(
                label = "Range",
                selected = draftSelection.mode == PreviewLayerMode.Range,
                onClick = {
                    val next = draftSelection.copy(mode = PreviewLayerMode.Range)
                    draftSelection = next
                    onSelectionChanged(next)
                    onSelectionCommitted(next)
                },
                modifier = Modifier.weight(1f)
            )
            PreviewLayerModeButton(
                label = "Single",
                selected = draftSelection.mode == PreviewLayerMode.Single,
                onClick = {
                    val next = draftSelection.copy(mode = PreviewLayerMode.Single)
                    draftSelection = next
                    onSelectionChanged(next)
                    onSelectionCommitted(next)
                },
                modifier = Modifier.weight(1f)
            )
        }
        when (draftSelection.mode) {
            PreviewLayerMode.Single -> {
                if (count > 1) {
                    Slider(
                        value = singleLayer.toFloat(),
                        onValueChange = { value ->
                            val next = draftSelection.copy(singleLayer = value.toInt().coerceIn(1, count))
                            draftSelection = next
                            onSelectionChanged(next)
                        },
                        onValueChangeFinished = {
                            onSelectionCommitted(draftSelection)
                        },
                        valueRange = 1f..count.toFloat(),
                        steps = (count - 2).coerceAtLeast(0),
                        colors = workspaceSliderColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    )
                }
            }
            PreviewLayerMode.Range -> {
                if (count > 1) {
                    RangeSlider(
                        value = selectedStart.toFloat()..selectedEnd.toFloat(),
                        onValueChange = { values ->
                            val requestedStart = values.start.toInt().coerceIn(sliderStart, sliderEnd)
                            val requestedEnd = values.endInclusive.toInt().coerceIn(requestedStart, sliderEnd)
                            val start: Int
                            val end: Int
                            if (maxSpan >= sliderEnd - sliderStart + 1) {
                                start = requestedStart
                                end = requestedEnd
                            } else {
                                val startDelta = kotlin.math.abs(requestedStart - selectedStart)
                                val endDelta = kotlin.math.abs(requestedEnd - selectedEnd)
                                if (endDelta > startDelta) {
                                    end = requestedEnd.coerceIn(sliderStart + maxSpan - 1, sliderEnd)
                                    start = (end - maxSpan + 1).coerceIn(sliderStart, sliderEnd)
                                } else {
                                    start = requestedStart.coerceIn(sliderStart, sliderEnd - maxSpan + 1)
                                    end = (start + maxSpan - 1).coerceIn(start, sliderEnd)
                                }
                            }
                            val next = draftSelection.copy(
                                rangeStartLayer = start,
                                rangeEndLayer = end
                            )
                            draftSelection = next
                            // Push every drag tick into the existing native
                            // viewer as a cheap visible-layer-window update.
                            onSelectionChanged(next)
                        },
                        onValueChangeFinished = {
                            onSelectionCommitted(draftSelection)
                        },
                        valueRange = sliderStart.toFloat()..sliderEnd.toFloat(),
                        steps = (sliderEnd - sliderStart - 1).coerceAtLeast(0),
                        colors = workspaceSliderColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun PreviewLayerModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(label)
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (LocalAppDarkTheme.current) {
                    Color(0xFF303846)
                } else {
                    Color(0xFFD7DCE3)
                },
                contentColor = appTitleColor()
            ),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Text(label)
        }
    }
}
