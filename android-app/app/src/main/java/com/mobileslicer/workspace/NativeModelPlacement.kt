package com.mobileslicer.workspace

import com.mobileslicer.viewer.MeshBounds
import com.mobileslicer.viewer.PrinterBedSpec
import com.mobileslicer.viewer.ViewerModelTransform
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal data class NativeModelTransform(
    val xMm: Double,
    val yMm: Double,
    val zMm: Double,
    val rotationXRadians: Double,
    val rotationYRadians: Double,
    val rotationZRadians: Double,
    val uniformScale: Double
)

internal fun nativeModelTransformFromArray(values: DoubleArray, offset: Int = 0): NativeModelTransform {
    require(offset >= 0 && offset + 6 < values.size) { "Native transform offset is outside the source array." }
    return NativeModelTransform(
        xMm = values[offset + 0],
        yMm = values[offset + 1],
        zMm = values[offset + 2],
        rotationXRadians = values[offset + 3],
        rotationYRadians = values[offset + 4],
        rotationZRadians = values[offset + 5],
        uniformScale = values[offset + 6]
    )
}

internal fun NativeModelTransform.writeTo(values: DoubleArray, offset: Int = 0) {
    require(offset >= 0 && offset + 6 < values.size) { "Native transform offset is outside the target array." }
    values[offset + 0] = xMm
    values[offset + 1] = yMm
    values[offset + 2] = zMm
    values[offset + 3] = rotationXRadians
    values[offset + 4] = rotationYRadians
    values[offset + 5] = rotationZRadians
    values[offset + 6] = uniformScale
}

internal fun defaultNativeModelTransform(
    bounds: MeshBounds,
    printerBed: PrinterBedSpec,
    modelTransform: ViewerModelTransform?
): NativeModelTransform {
    val transform = modelTransform ?: ViewerModelTransform(
        centerXmm = printerBed.widthMm * 0.5f,
        centerYmm = printerBed.depthMm * 0.5f,
        rotationXDegrees = 0f,
        rotationYDegrees = 0f,
        rotationZDegrees = 0f,
        uniformScale = 1f
    )
    val scale = transform.uniformScale.coerceIn(0.05f, 20f).toDouble()
    val rotationXRadians = Math.toRadians(transform.rotationXDegrees.toDouble())
    val rotationYRadians = Math.toRadians(transform.rotationYDegrees.toDouble())
    val rotationZRadians = Math.toRadians(transform.rotationZDegrees.toDouble())
    val transformedCenter = transformPoint(
        x = bounds.centerX.toDouble(),
        y = bounds.centerY.toDouble(),
        z = bounds.centerZ.toDouble(),
        scale = scale,
        rotationXRadians = rotationXRadians,
        rotationYRadians = rotationYRadians,
        rotationZRadians = rotationZRadians
    )
    val transformedBounds = transformedBounds(bounds, scale, rotationXRadians, rotationYRadians, rotationZRadians)
    return NativeModelTransform(
        xMm = printerBed.originXmm.toDouble() + transform.centerXmm.toDouble() - transformedCenter.xMm,
        yMm = printerBed.originYmm.toDouble() + transform.centerYmm.toDouble() - transformedCenter.yMm,
        zMm = -transformedBounds.minZ,
        rotationXRadians = rotationXRadians,
        rotationYRadians = rotationYRadians,
        rotationZRadians = rotationZRadians,
        uniformScale = scale
    )
}

internal fun nativeModelTransformToViewerTransform(
    bounds: MeshBounds,
    printerBed: PrinterBedSpec,
    nativeTransform: NativeModelTransform
): ViewerModelTransform {
    val transformedCenter = transformPoint(
        x = bounds.centerX.toDouble(),
        y = bounds.centerY.toDouble(),
        z = bounds.centerZ.toDouble(),
        scale = nativeTransform.uniformScale,
        rotationXRadians = nativeTransform.rotationXRadians,
        rotationYRadians = nativeTransform.rotationYRadians,
        rotationZRadians = nativeTransform.rotationZRadians
    )
    return ViewerModelTransform(
        centerXmm = (nativeTransform.xMm - printerBed.originXmm.toDouble() + transformedCenter.xMm).toFloat(),
        centerYmm = (nativeTransform.yMm - printerBed.originYmm.toDouble() + transformedCenter.yMm).toFloat(),
        rotationXDegrees = Math.toDegrees(nativeTransform.rotationXRadians).toFloat(),
        rotationYDegrees = Math.toDegrees(nativeTransform.rotationYRadians).toFloat(),
        rotationZDegrees = Math.toDegrees(nativeTransform.rotationZRadians).toFloat(),
        uniformScale = nativeTransform.uniformScale.toFloat()
    )
}

private data class TransformedPointD(
    val xMm: Double,
    val yMm: Double,
    val zMm: Double
)

private data class TransformedBoundsD(
    val minZ: Double
)

private fun transformedBounds(
    bounds: MeshBounds,
    scale: Double,
    rotationXRadians: Double,
    rotationYRadians: Double,
    rotationZRadians: Double
): TransformedBoundsD {
    var minZ = Double.POSITIVE_INFINITY
    val xs = doubleArrayOf(bounds.minX.toDouble(), bounds.maxX.toDouble())
    val ys = doubleArrayOf(bounds.minY.toDouble(), bounds.maxY.toDouble())
    val zs = doubleArrayOf(bounds.minZ.toDouble(), bounds.maxZ.toDouble())
    for (x in xs) {
        for (y in ys) {
            for (z in zs) {
                minZ = min(
                    minZ,
                    transformPoint(x, y, z, scale, rotationXRadians, rotationYRadians, rotationZRadians).zMm
                )
            }
        }
    }
    return TransformedBoundsD(minZ = minZ)
}

private fun transformPoint(
    x: Double,
    y: Double,
    z: Double,
    scale: Double,
    rotationXRadians: Double,
    rotationYRadians: Double,
    rotationZRadians: Double
): TransformedPointD {
    var tx = x * scale
    var ty = y * scale
    var tz = z * scale

    val cosX = cos(rotationXRadians)
    val sinX = sin(rotationXRadians)
    val yAfterX = ty * cosX - tz * sinX
    val zAfterX = ty * sinX + tz * cosX
    ty = yAfterX
    tz = zAfterX

    val cosY = cos(rotationYRadians)
    val sinY = sin(rotationYRadians)
    val xAfterY = tx * cosY + tz * sinY
    val zAfterY = -tx * sinY + tz * cosY
    tx = xAfterY
    tz = zAfterY

    val cosZ = cos(rotationZRadians)
    val sinZ = sin(rotationZRadians)
    return TransformedPointD(
        xMm = tx * cosZ - ty * sinZ,
        yMm = tx * sinZ + ty * cosZ,
        zMm = tz
    )
}
