package com.mobileslicer.workspace

import androidx.compose.ui.graphics.Color
import com.mobileslicer.ui.theme.WorldViewColorOption
import com.mobileslicer.viewer.PrinterBedSpec
import com.mobileslicer.viewer.StlMesh
import com.mobileslicer.viewer.ViewerModelTransform
import kotlin.math.round

internal enum class PreviewLayerMode {
    Single,
    Range
}

internal enum class TransformToolTab {
    Move,
    Rotate,
    Scale,
    AutoOrient,
    AutoArrange
}

internal enum class TransformNumericField {
    MoveX,
    MoveY,
    RotateX,
    RotateY,
    RotateZ,
    Scale
}

internal data class PreviewLayerSelection(
    val mode: PreviewLayerMode,
    val singleLayer: Int,
    val rangeStartLayer: Int,
    val rangeEndLayer: Int
)

internal sealed interface WorkspaceViewerState {
    data object Empty : WorkspaceViewerState
    data object Preparing : WorkspaceViewerState
    data object Unsupported : WorkspaceViewerState
    data class Loaded(val mesh: StlMesh) : WorkspaceViewerState
    data class Error(val title: String, val message: String) : WorkspaceViewerState
}

internal fun defaultViewerModelTransform(printerBed: PrinterBedSpec): ViewerModelTransform =
    ViewerModelTransform(
        centerXmm = printerBed.widthMm * 0.5f,
        centerYmm = printerBed.depthMm * 0.5f,
        rotationXDegrees = 0f,
        rotationYDegrees = 0f,
        rotationZDegrees = 0f,
        uniformScale = 1f
    )

internal fun normalizeDegrees(value: Float): Float {
    var normalized = value
    while (normalized > 180f) normalized -= 360f
    while (normalized < -180f) normalized += 360f
    return normalized
}

internal fun nearestRightAngle(value: Float): Float =
    normalizeDegrees(round(value / 90f) * 90f)

internal fun selectedWorkspaceWorldColor(worldViewColor: WorldViewColorOption): Color =
    when (worldViewColor) {
        WorldViewColorOption.White -> Color(0xFFF3F7FC)
        WorldViewColorOption.Mist -> Color(0xFFDCE5EE)
        WorldViewColorOption.Slate -> Color(0xFF8E9AA6)
        WorldViewColorOption.Graphite -> Color(0xFF3F4852)
        WorldViewColorOption.Deep -> Color(0xFF071426)
        WorldViewColorOption.Navy -> Color(0xFF10233A)
        WorldViewColorOption.Charcoal -> Color(0xFF171B20)
        WorldViewColorOption.Black -> Color(0xFF020407)
    }
