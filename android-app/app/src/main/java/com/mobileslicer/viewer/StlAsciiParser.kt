package com.mobileslicer.viewer

import java.io.File
import java.io.InputStreamReader
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlin.math.sqrt

internal fun parseAsciiForDisplay(file: File, maxDisplayTriangles: Int): PreparedViewerMesh {
    val sourceSummary = parseAsciiBoundsAndCount(file)
    if (sourceSummary.triangleCount <= maxDisplayTriangles) {
        return parseAscii(file).let { mesh ->
            PreparedViewerMesh(
                mesh = mesh,
                sourceTriangleCount = mesh.triangleCount,
                displayTriangleCount = mesh.triangleCount,
                sourceBounds = mesh.bounds,
                reducedForDisplay = false
            )
        }
    }

    val vertices = FloatCollector(maxDisplayTriangles * 9)
    val normals = FloatCollector(maxDisplayTriangles * 9)
    val parsedVector = FloatArray(3)
    val parsedNormal = FloatArray(3)
    var currentFacetNormalX = 0f
    var currentFacetNormalY = 0f
    var currentFacetNormalZ = 1f
    val triangleVertices = FloatArray(9)
    var vertexInTriangle = 0
    var triangleIndex = 0

    InputStreamReader(file.inputStream(), asciiDecoder()).use { reader ->
        reader.buffered().useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("facet normal ")) {
                    if (parseVector(line, prefixLength = 13, out = parsedVector)) {
                        normalizeNormal(parsedVector[0], parsedVector[1], parsedVector[2], parsedNormal)
                        currentFacetNormalX = parsedNormal[0]
                        currentFacetNormalY = parsedNormal[1]
                        currentFacetNormalZ = parsedNormal[2]
                    }
                } else if (line.startsWith("vertex ")) {
                    require(parseVector(line, prefixLength = 7, out = parsedVector)) {
                        "ASCII STL vertex line is malformed."
                    }
                    val base = vertexInTriangle * 3
                    triangleVertices[base] = parsedVector[0]
                    triangleVertices[base + 1] = parsedVector[1]
                    triangleVertices[base + 2] = parsedVector[2]
                    vertexInTriangle++
                    if (vertexInTriangle == 3) {
                        if (shouldKeepDisplayTriangle(
                                triangleIndex = triangleIndex,
                                sourceTriangleCount = sourceSummary.triangleCount,
                                displayTriangleBudget = maxDisplayTriangles
                            )
                        ) {
                            appendTriangle(
                                vertices = vertices,
                                normals = normals,
                                triangleVertices = triangleVertices,
                                normalX = currentFacetNormalX,
                                normalY = currentFacetNormalY,
                                normalZ = currentFacetNormalZ
                            )
                        }
                        triangleIndex++
                        vertexInTriangle = 0
                    }
                }
            }
        }
    }
    require(vertexInTriangle == 0) { "ASCII STL vertex count is incomplete." }

    val selectedTriangles = vertices.size / 9
    return PreparedViewerMesh(
        mesh = StlMesh(
            vertices = vertices.toFloatArray(),
            normals = normals.toFloatArray(),
            triangleCount = selectedTriangles,
            bounds = sourceSummary.bounds
        ),
        sourceTriangleCount = sourceSummary.triangleCount,
        displayTriangleCount = selectedTriangles,
        sourceBounds = sourceSummary.bounds,
        reducedForDisplay = true
    )
}

internal fun parseAscii(file: File): StlMesh {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY
    val estimatedVertexCount = ((file.length() / 48L).coerceIn(256L, STL_MAX_TRIANGLES.toLong() * 3L)).toInt()
    val vertices = FloatCollector(estimatedVertexCount * 3)
    val normals = FloatCollector(estimatedVertexCount * 3)
    var currentFacetNormalX = 0f
    var currentFacetNormalY = 0f
    var currentFacetNormalZ = 1f
    val parsedVector = FloatArray(3)
    var vertexCount = 0

    InputStreamReader(file.inputStream(), asciiDecoder()).use { reader ->
        reader.buffered().useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("facet normal ")) {
                    if (parseVector(line, prefixLength = 13, out = parsedVector)) {
                        val length = sqrt(
                            parsedVector[0] * parsedVector[0] +
                                parsedVector[1] * parsedVector[1] +
                                parsedVector[2] * parsedVector[2]
                        )
                        if (length > 0.0001f) {
                            currentFacetNormalX = parsedVector[0] / length
                            currentFacetNormalY = parsedVector[1] / length
                            currentFacetNormalZ = parsedVector[2] / length
                        } else {
                            currentFacetNormalX = 0f
                            currentFacetNormalY = 0f
                            currentFacetNormalZ = 1f
                        }
                    }
                } else if (line.startsWith("vertex ")) {
                    require(parseVector(line, prefixLength = 7, out = parsedVector)) {
                        "ASCII STL vertex line is malformed."
                    }
                    val x = parsedVector[0]
                    val y = parsedVector[1]
                    val z = parsedVector[2]

                    vertices.append(x, y, z)
                    normals.append(currentFacetNormalX, currentFacetNormalY, currentFacetNormalZ)
                    vertexCount++

                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    minZ = minOf(minZ, z)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                    maxZ = maxOf(maxZ, z)
                }
            }
        }
    }

    require(vertexCount > 0) { "ASCII STL has no vertices." }
    require(vertexCount % 3 == 0) { "ASCII STL vertex count is incomplete." }

    return StlMesh(
        vertices = vertices.toFloatArray(),
        normals = normals.toFloatArray(),
        triangleCount = vertexCount / 3,
        bounds = MeshBounds(minX, minY, minZ, maxX, maxY, maxZ)
    )
}

internal fun parseAsciiBoundsAndCount(file: File): BoundsAndTriangleCount {
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var minZ = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    var maxZ = Float.NEGATIVE_INFINITY
    val parsedVector = FloatArray(3)
    var vertexCount = 0

    InputStreamReader(file.inputStream(), asciiDecoder()).use { reader ->
        reader.buffered().useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("vertex ")) {
                    require(parseVector(line, prefixLength = 7, out = parsedVector)) {
                        "ASCII STL vertex line is malformed."
                    }
                    val x = parsedVector[0]
                    val y = parsedVector[1]
                    val z = parsedVector[2]
                    vertexCount++

                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    minZ = minOf(minZ, z)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                    maxZ = maxOf(maxZ, z)
                }
            }
        }
    }

    require(vertexCount > 0) { "ASCII STL has no vertices." }
    require(vertexCount % 3 == 0) { "ASCII STL vertex count is incomplete." }
    return BoundsAndTriangleCount(
        bounds = MeshBounds(minX, minY, minZ, maxX, maxY, maxZ),
        triangleCount = vertexCount / 3
    )
}

internal fun appendTriangle(
    vertices: FloatCollector,
    normals: FloatCollector,
    triangleVertices: FloatArray,
    normalX: Float,
    normalY: Float,
    normalZ: Float
) {
    vertices.append(triangleVertices[0], triangleVertices[1], triangleVertices[2])
    vertices.append(triangleVertices[3], triangleVertices[4], triangleVertices[5])
    vertices.append(triangleVertices[6], triangleVertices[7], triangleVertices[8])
    repeat(3) {
        normals.append(normalX, normalY, normalZ)
    }
}

internal data class BoundsAndTriangleCount(
    val bounds: MeshBounds,
    val triangleCount: Int
)

internal fun asciiDecoder(): CharsetDecoder {
    return StandardCharsets.UTF_8.newDecoder().apply {
        onMalformedInput(CodingErrorAction.REPORT)
        onUnmappableCharacter(CodingErrorAction.REPORT)
    }
}

internal fun parseVector(line: String, prefixLength: Int, out: FloatArray): Boolean {
    var index = prefixLength
    var part = 0
    val length = line.length

    while (part < 3) {
        while (index < length && line[index].isWhitespace()) {
            index++
        }
        if (index >= length) return false

        val start = index
        while (index < length && !line[index].isWhitespace()) {
            index++
        }
        out[part] = line.substring(start, index).toFloat()
        part++
    }
    return true
}

internal class FloatCollector(initialCapacity: Int) {
    private var values = FloatArray(initialCapacity.coerceAtLeast(12))
    var size = 0
        private set

    fun append(x: Float, y: Float, z: Float) {
        ensureCapacity(3)
        values[size++] = x
        values[size++] = y
        values[size++] = z
    }

    fun toFloatArray(): FloatArray = values.copyOf(size)

    private fun ensureCapacity(extra: Int) {
        if (size + extra <= values.size) return
        var nextSize = values.size
        while (size + extra > nextSize) {
            nextSize *= 2
        }
        values = values.copyOf(nextSize)
    }
}
