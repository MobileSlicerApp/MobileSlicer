package com.mobileslicer.viewer

import android.opengl.Matrix
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class PickRay(
    val originX: Float,
    val originY: Float,
    val originZ: Float,
    val directionX: Float,
    val directionY: Float,
    val directionZ: Float
)

internal data class TriangleHit(
    val distance: Float,
    val u: Float,
    val v: Float
)

internal data class ProjectedObjectBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
    val nearestDepth: Float
)

internal fun screenRay(
    screenX: Float,
    screenY: Float,
    viewportWidth: Int,
    viewportHeight: Int,
    viewProjectionMatrix: FloatArray
): PickRay? {
    val width = viewportWidth.toFloat().coerceAtLeast(1f)
    val height = viewportHeight.toFloat().coerceAtLeast(1f)
    val ndcX = (screenX / width) * 2f - 1f
    val ndcY = 1f - (screenY / height) * 2f
    val inverse = FloatArray(16)
    if (!Matrix.invertM(inverse, 0, viewProjectionMatrix, 0)) {
        return null
    }
    val near = unprojectPoint(inverse, ndcX, ndcY, -1f) ?: return null
    val far = unprojectPoint(inverse, ndcX, ndcY, 1f) ?: return null
    val dx = far.xMm - near.xMm
    val dy = far.yMm - near.yMm
    val dz = far.zMm - near.zMm
    val length = sqrt(dx * dx + dy * dy + dz * dz)
    if (length <= 0.0001f) return null
    return PickRay(
        originX = near.xMm,
        originY = near.yMm,
        originZ = near.zMm,
        directionX = dx / length,
        directionY = dy / length,
        directionZ = dz / length
    )
}

internal fun intersectObjectBounds(ray: PickRay, objectBounds: PickableObjectBounds): Float? {
    val padding = 1.5f
    val minX = objectBounds.centerX - objectBounds.sizeX * 0.5f - padding
    val maxX = objectBounds.centerX + objectBounds.sizeX * 0.5f + padding
    val minY = objectBounds.centerY - objectBounds.sizeY * 0.5f - padding
    val maxY = objectBounds.centerY + objectBounds.sizeY * 0.5f + padding
    val minZ = objectBounds.centerZ - objectBounds.sizeZ * 0.5f - padding
    val maxZ = objectBounds.centerZ + objectBounds.sizeZ * 0.5f + padding
    var tMin = 0f
    var tMax = Float.POSITIVE_INFINITY

    fun applySlab(origin: Float, direction: Float, minValue: Float, maxValue: Float): Boolean {
        if (abs(direction) < 0.0001f) {
            return origin in minValue..maxValue
        }
        val invDirection = 1f / direction
        var t1 = (minValue - origin) * invDirection
        var t2 = (maxValue - origin) * invDirection
        if (t1 > t2) {
            val swap = t1
            t1 = t2
            t2 = swap
        }
        tMin = max(tMin, t1)
        tMax = min(tMax, t2)
        return tMin <= tMax
    }

    if (!applySlab(ray.originX, ray.directionX, minX, maxX)) return null
    if (!applySlab(ray.originY, ray.directionY, minY, maxY)) return null
    if (!applySlab(ray.originZ, ray.directionZ, minZ, maxZ)) return null
    return if (tMax >= 0f) tMin.coerceAtLeast(0f) else null
}

internal fun intersectTriangle(ray: PickRay, a: FloatArray, b: FloatArray, c: FloatArray): TriangleHit? {
    val edge1X = b[0] - a[0]
    val edge1Y = b[1] - a[1]
    val edge1Z = b[2] - a[2]
    val edge2X = c[0] - a[0]
    val edge2Y = c[1] - a[1]
    val edge2Z = c[2] - a[2]

    val pX = ray.directionY * edge2Z - ray.directionZ * edge2Y
    val pY = ray.directionZ * edge2X - ray.directionX * edge2Z
    val pZ = ray.directionX * edge2Y - ray.directionY * edge2X
    val determinant = edge1X * pX + edge1Y * pY + edge1Z * pZ
    if (abs(determinant) < TriangleEpsilon) return null

    val inverseDeterminant = 1f / determinant
    val tX = ray.originX - a[0]
    val tY = ray.originY - a[1]
    val tZ = ray.originZ - a[2]
    val u = (tX * pX + tY * pY + tZ * pZ) * inverseDeterminant
    if (u < -TriangleEpsilon || u > 1f + TriangleEpsilon) return null

    val qX = tY * edge1Z - tZ * edge1Y
    val qY = tZ * edge1X - tX * edge1Z
    val qZ = tX * edge1Y - tY * edge1X
    val v = (ray.directionX * qX + ray.directionY * qY + ray.directionZ * qZ) * inverseDeterminant
    if (v < -TriangleEpsilon || u + v > 1f + TriangleEpsilon) return null

    val distance = (edge2X * qX + edge2Y * qY + edge2Z * qZ) * inverseDeterminant
    return if (distance >= 0f) TriangleHit(distance = distance, u = u, v = v) else null
}

internal fun transformPosition(matrix: FloatArray, vertices: FloatArray, offset: Int, out: FloatArray) {
    val x = vertices[offset]
    val y = vertices[offset + 1]
    val z = vertices[offset + 2]
    out[0] = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12]
    out[1] = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13]
    out[2] = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
}

internal fun dotNormalWithRay(normal: FloatArray, ray: PickRay): Float =
    normal[0] * ray.directionX + normal[1] * ray.directionY + normal[2] * ray.directionZ

internal fun triangleNormal(a: FloatArray, b: FloatArray, c: FloatArray, out: FloatArray) {
    val edge1X = b[0] - a[0]
    val edge1Y = b[1] - a[1]
    val edge1Z = b[2] - a[2]
    val edge2X = c[0] - a[0]
    val edge2Y = c[1] - a[1]
    val edge2Z = c[2] - a[2]
    normalize(
        x = edge1Y * edge2Z - edge1Z * edge2Y,
        y = edge1Z * edge2X - edge1X * edge2Z,
        z = edge1X * edge2Y - edge1Y * edge2X,
        out = out
    )
}

internal fun projectObjectBounds(
    objectBounds: PickableObjectBounds,
    viewport: IntArray,
    viewportHeight: Int,
    viewProjectionMatrix: FloatArray,
    sceneSpan: Float
): ProjectedObjectBounds? {
    val halfX = objectBounds.sizeX * 0.5f
    val halfY = objectBounds.sizeY * 0.5f
    val halfZ = objectBounds.sizeZ * 0.5f
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var nearestDepth = Float.POSITIVE_INFINITY
    var projectedCount = 0
    for (x in floatArrayOf(objectBounds.centerX - halfX, objectBounds.centerX + halfX)) {
        for (y in floatArrayOf(objectBounds.centerY - halfY, objectBounds.centerY + halfY)) {
            for (z in floatArrayOf(objectBounds.centerZ - halfZ, objectBounds.centerZ + halfZ)) {
                val projected = projectPoint(x, y, z, viewport, viewProjectionMatrix) ?: continue
                minX = min(minX, projected.xMm)
                minY = min(minY, projected.yMm)
                maxX = max(maxX, projected.xMm)
                maxY = max(maxY, projected.yMm)
                nearestDepth = min(nearestDepth, projected.zMm)
                projectedCount += 1
            }
        }
    }
    if (projectedCount == 0) {
        val center = projectPoint(
            objectBounds.centerX,
            objectBounds.centerY,
            objectBounds.centerZ,
            viewport,
            viewProjectionMatrix
        ) ?: return null
        val radiusPx = (objectBounds.radius * 1.2f / sceneSpan.coerceAtLeast(1f) * viewportHeight)
            .coerceIn(20f, 90f)
        return ProjectedObjectBounds(
            minX = center.xMm - radiusPx,
            minY = center.yMm - radiusPx,
            maxX = center.xMm + radiusPx,
            maxY = center.yMm + radiusPx,
            nearestDepth = center.zMm
        )
    }
    return ProjectedObjectBounds(minX, minY, maxX, maxY, nearestDepth)
}

private fun unprojectPoint(
    inverseViewProjection: FloatArray,
    ndcX: Float,
    ndcY: Float,
    ndcZ: Float
): StlModelPlacement? {
    val input = floatArrayOf(ndcX, ndcY, ndcZ, 1f)
    val output = FloatArray(4)
    Matrix.multiplyMV(output, 0, inverseViewProjection, 0, input, 0)
    val w = output[3]
    if (abs(w) < 0.0001f) return null
    return StlModelPlacement(
        xMm = output[0] / w,
        yMm = output[1] / w,
        zMm = output[2] / w
    )
}

private fun normalize(x: Float, y: Float, z: Float, out: FloatArray) {
    val length = sqrt(x * x + y * y + z * z)
    if (length > TriangleEpsilon) {
        out[0] = x / length
        out[1] = y / length
        out[2] = z / length
    } else {
        out[0] = 0f
        out[1] = 0f
        out[2] = 1f
    }
}

private fun projectPoint(
    x: Float,
    y: Float,
    z: Float,
    viewport: IntArray,
    viewProjectionMatrix: FloatArray
): StlModelPlacement? {
    val input = floatArrayOf(x, y, z, 1f)
    val clip = FloatArray(4)
    Matrix.multiplyMV(clip, 0, viewProjectionMatrix, 0, input, 0)
    val w = clip[3]
    if (abs(w) < 0.0001f) return null
    val normalizedX = clip[0] / w
    val normalizedY = clip[1] / w
    val normalizedZ = clip[2] / w
    if (normalizedZ < -1.2f || normalizedZ > 1.2f) return null
    return StlModelPlacement(
        xMm = viewport[0] + (normalizedX + 1f) * viewport[2] * 0.5f,
        yMm = viewport[1] + (1f - normalizedY) * viewport[3] * 0.5f,
        zMm = normalizedZ
    )
}

private const val TriangleEpsilon = 0.000001f
