package com.mobileslicer.viewer

import java.io.File
import kotlin.math.max

internal data class MeshBounds(
    val minX: Float,
    val minY: Float,
    val minZ: Float,
    val maxX: Float,
    val maxY: Float,
    val maxZ: Float
) {
    val centerX: Float get() = (minX + maxX) * 0.5f
    val centerY: Float get() = (minY + maxY) * 0.5f
    val centerZ: Float get() = (minZ + maxZ) * 0.5f
    val sizeX: Float get() = maxX - minX
    val sizeY: Float get() = maxY - minY
    val sizeZ: Float get() = maxZ - minZ
    val radius: Float
        get() = max(
            max(sizeX, sizeY),
            sizeZ
        ) * 0.5f
}

internal data class StlMesh(
    val vertices: FloatArray,
    val normals: FloatArray,
    val triangleCount: Int,
    val bounds: MeshBounds
)

internal data class PreparedViewerMesh(
    val mesh: StlMesh,
    val sourceTriangleCount: Int,
    val displayTriangleCount: Int,
    val sourceBounds: MeshBounds,
    val reducedForDisplay: Boolean
)

internal const val STL_MAX_TRIANGLES: Int = Int.MAX_VALUE / 9

internal object StlMeshParser {
    const val DEFAULT_MAX_DISPLAY_TRIANGLES = 1_000_000

    fun parse(file: File): StlMesh {
        require(file.exists() && file.length() > 0L) { "STL file is empty." }
        return if (looksLikeBinaryStl(file)) {
            parseBinary(file)
        } else {
            parseAscii(file)
        }
    }

    fun parseForDisplay(
        file: File,
        maxDisplayTriangles: Int = DEFAULT_MAX_DISPLAY_TRIANGLES
    ): PreparedViewerMesh {
        require(maxDisplayTriangles > 0) { "Display triangle budget must be positive." }
        require(file.exists() && file.length() > 0L) { "STL file is empty." }
        return if (looksLikeBinaryStl(file)) {
            parseBinaryForDisplay(file, maxDisplayTriangles)
        } else {
            parseAsciiForDisplay(file, maxDisplayTriangles)
        }
    }

    fun parseBounds(file: File): MeshBounds {
        require(file.exists() && file.length() > 0L) { "STL file is empty." }
        return if (looksLikeBinaryStl(file)) {
            parseBinaryBounds(file)
        } else {
            parseAsciiBoundsAndCount(file).bounds
        }
    }
}
