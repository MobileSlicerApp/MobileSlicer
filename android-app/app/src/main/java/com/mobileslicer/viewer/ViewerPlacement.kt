package com.mobileslicer.viewer

import android.opengl.Matrix
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class ModelPlacement(
    val matrix: FloatArray,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float
)

internal data class TransformedBounds(
    val minX: Float,
    val maxX: Float,
    val minY: Float,
    val maxY: Float,
    val minZ: Float,
    val maxZ: Float,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float
)

internal fun buildModelPlacement(
    mesh: StlMesh,
    transform: ViewerModelTransform,
    bed: PrinterBedSpec
): ModelPlacement {
    val scale = transform.uniformScale.coerceIn(0.05f, 20f)
    val rotatedCenter = transformPoint(
        x = mesh.bounds.centerX,
        y = mesh.bounds.centerY,
        z = mesh.bounds.centerZ,
        scale = scale,
        rotationXDegrees = transform.rotationXDegrees,
        rotationYDegrees = transform.rotationYDegrees,
        rotationZDegrees = transform.rotationZDegrees
    )
    val rotatedBounds = transformedBounds(
        bounds = mesh.bounds,
        scale = scale,
        rotationXDegrees = transform.rotationXDegrees,
        rotationYDegrees = transform.rotationYDegrees,
        rotationZDegrees = transform.rotationZDegrees
    )
    val placementX = transform.centerXmm - bed.widthMm * 0.5f - rotatedCenter.xMm
    val placementY = transform.centerYmm - bed.depthMm * 0.5f - rotatedCenter.yMm
    val placementZ = -rotatedBounds.minZ
    val matrix = FloatArray(16)
    Matrix.setIdentityM(matrix, 0)
    Matrix.translateM(matrix, 0, placementX, placementY, placementZ)
    Matrix.rotateM(matrix, 0, transform.rotationXDegrees, 1f, 0f, 0f)
    Matrix.rotateM(matrix, 0, transform.rotationYDegrees, 0f, 1f, 0f)
    Matrix.rotateM(matrix, 0, transform.rotationZDegrees, 0f, 0f, 1f)
    Matrix.scaleM(matrix, 0, scale, scale, scale)
    return ModelPlacement(
        matrix = matrix,
        centerX = placementX + (rotatedBounds.minX + rotatedBounds.maxX) * 0.5f,
        centerY = placementY + (rotatedBounds.minY + rotatedBounds.maxY) * 0.5f,
        centerZ = rotatedBounds.sizeZ * 0.5f,
        sizeX = rotatedBounds.sizeX,
        sizeY = rotatedBounds.sizeY,
        sizeZ = rotatedBounds.sizeZ
    )
}

internal fun defaultBedCenteredPrinterTransform(bed: PrinterBedSpec): ViewerModelTransform =
    ViewerModelTransform(
        centerXmm = bed.widthMm * 0.5f,
        centerYmm = bed.depthMm * 0.5f,
        rotationXDegrees = 0f,
        rotationYDegrees = 0f,
        rotationZDegrees = 0f,
        uniformScale = 1f
    )

internal fun transformedBounds(
    bounds: MeshBounds,
    scale: Float,
    rotationXDegrees: Float,
    rotationYDegrees: Float,
    rotationZDegrees: Float
): TransformedBounds {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY
    val xs = floatArrayOf(bounds.minX, bounds.maxX)
    val ys = floatArrayOf(bounds.minY, bounds.maxY)
    val zs = floatArrayOf(bounds.minZ, bounds.maxZ)
    for (x in xs) {
        for (y in ys) {
            for (z in zs) {
                val point = transformPoint(x, y, z, scale, rotationXDegrees, rotationYDegrees, rotationZDegrees)
                minX = min(minX, point.xMm)
                minY = min(minY, point.yMm)
                minZ = min(minZ, point.zMm)
                maxX = max(maxX, point.xMm)
                maxY = max(maxY, point.yMm)
                maxZ = max(maxZ, point.zMm)
            }
        }
    }
    return TransformedBounds(
        minX = minX,
        maxX = maxX,
        minY = minY,
        maxY = maxY,
        minZ = minZ,
        maxZ = maxZ,
        sizeX = maxX - minX,
        sizeY = maxY - minY,
        sizeZ = maxZ - minZ
    )
}

internal fun transformPoint(
    x: Float,
    y: Float,
    z: Float,
    scale: Float,
    rotationXDegrees: Float,
    rotationYDegrees: Float,
    rotationZDegrees: Float
): StlModelPlacement {
    var tx = x * scale
    var ty = y * scale
    var tz = z * scale

    val rx = Math.toRadians(rotationXDegrees.toDouble())
    val cosX = cos(rx).toFloat()
    val sinX = sin(rx).toFloat()
    val yAfterX = ty * cosX - tz * sinX
    val zAfterX = ty * sinX + tz * cosX
    ty = yAfterX
    tz = zAfterX

    val ry = Math.toRadians(rotationYDegrees.toDouble())
    val cosY = cos(ry).toFloat()
    val sinY = sin(ry).toFloat()
    val xAfterY = tx * cosY + tz * sinY
    val zAfterY = -tx * sinY + tz * cosY
    tx = xAfterY
    tz = zAfterY

    val rz = Math.toRadians(rotationZDegrees.toDouble())
    val cosZ = cos(rz).toFloat()
    val sinZ = sin(rz).toFloat()
    return StlModelPlacement(
        xMm = tx * cosZ - ty * sinZ,
        yMm = tx * sinZ + ty * cosZ,
        zMm = tz
    )
}

internal fun modelRadius(placement: ModelPlacement): Float =
    max(max(placement.sizeX, placement.sizeY), placement.sizeZ) * 0.5f
