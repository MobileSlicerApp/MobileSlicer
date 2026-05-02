package com.mobileslicer.viewer

import kotlin.math.sqrt

internal fun shouldKeepDisplayTriangle(
    triangleIndex: Int,
    sourceTriangleCount: Int,
    displayTriangleBudget: Int
): Boolean {
    if (sourceTriangleCount <= displayTriangleBudget) return true
    val previousBucket = triangleIndex.toLong() * displayTriangleBudget / sourceTriangleCount
    val nextBucket = (triangleIndex.toLong() + 1L) * displayTriangleBudget / sourceTriangleCount
    return nextBucket > previousBucket
}

internal fun normalizeNormal(x: Float, y: Float, z: Float, out: FloatArray) {
    val normalLength = sqrt(x * x + y * y + z * z)
    if (normalLength > 0.0001f) {
        out[0] = x / normalLength
        out[1] = y / normalLength
        out[2] = z / normalLength
    } else {
        out[0] = 0f
        out[1] = 0f
        out[2] = 1f
    }
}
