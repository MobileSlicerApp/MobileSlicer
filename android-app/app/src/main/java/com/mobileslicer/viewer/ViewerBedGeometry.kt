package com.mobileslicer.viewer

import kotlin.math.max
import kotlin.math.sqrt

internal data class TriangleGeometry(
    val vertices: FloatArray,
    val normals: FloatArray
)

internal data class BedGeometry(
    val surface: TriangleGeometry,
    val grid: TriangleGeometry,
    val border: TriangleGeometry,
    val wall: TriangleGeometry
)

internal data class OrcaBedGridGeometry(
    val thin: TriangleGeometry,
    val bold: TriangleGeometry
)

internal fun buildBedGeometry(bed: PrinterBedSpec): BedGeometry {
    val halfWidth = bed.widthMm * 0.5f
    val halfDepth = bed.depthMm * 0.5f
    val plateThickness = 4.8f
    val topZ = 0f
    val bottomZ = -plateThickness
    val rimOutset = minOf(7f, halfWidth * 0.05f, halfDepth * 0.05f).coerceAtLeast(5f)
    val outerHalfWidth = halfWidth + rimOutset
    val outerHalfDepth = halfDepth + rimOutset
    val gridZ = topZ + 0.018f
    val outerPoints = floatArrayOf(
        -outerHalfWidth, -outerHalfDepth,
        outerHalfWidth, -outerHalfDepth,
        outerHalfWidth, outerHalfDepth,
        -outerHalfWidth, outerHalfDepth
    )
    val insetFramePoints = floatArrayOf(
        -halfWidth, -halfDepth,
        halfWidth, -halfDepth,
        halfWidth, halfDepth,
        -halfWidth, halfDepth
    )

    val bedTopVertices = buildConvexFan(points = outerPoints, z = topZ)
    val gridStripVertices = buildGridStripGeometry(
        widthMm = bed.widthMm - 2f,
        depthMm = bed.depthMm - 2f,
        stepMm = 30f,
        stripWidthMm = 0.65f,
        z = gridZ
    )
    val borderVertices = buildOutlineStripGeometry(
        points = insetFramePoints,
        stripWidthMm = 0.7f,
        z = gridZ + 0.004f
    )
    val wall = buildWallGeometry(points = outerPoints, top = topZ, bottom = bottomZ)
    return BedGeometry(
        surface = TriangleGeometry(vertices = bedTopVertices, normals = buildTopNormals(bedTopVertices)),
        grid = TriangleGeometry(vertices = gridStripVertices, normals = buildTopNormals(gridStripVertices)),
        border = TriangleGeometry(vertices = borderVertices, normals = buildTopNormals(borderVertices)),
        wall = wall
    )
}

internal fun buildOrcaBedGridGeometry(bed: PrinterBedSpec): OrcaBedGridGeometry {
    val shortEdge = minOf(bed.widthMm, bed.depthMm)
    val step = when {
        shortEdge >= 6000f -> 100f
        shortEdge >= 1200f -> 50f
        shortEdge >= 600f -> 20f
        else -> 10f
    }
    val halfWidth = bed.widthMm * 0.5f
    val halfDepth = bed.depthMm * 0.5f
    val z = -0.045f
    val thin = ArrayList<Float>()
    val bold = ArrayList<Float>()

    fun appendLine(target: ArrayList<Float>, x0: Float, y0: Float, x1: Float, y1: Float, stripWidth: Float) {
        val dx = x1 - x0
        val dy = y1 - y0
        val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        val normalX = -dy / length * stripWidth * 0.5f
        val normalY = dx / length * stripWidth * 0.5f
        fun vertex(x: Float, y: Float) {
            target.add(x)
            target.add(y)
            target.add(z)
        }
        vertex(x0 + normalX, y0 + normalY)
        vertex(x0 - normalX, y0 - normalY)
        vertex(x1 - normalX, y1 - normalY)
        vertex(x0 + normalX, y0 + normalY)
        vertex(x1 - normalX, y1 - normalY)
        vertex(x1 + normalX, y1 + normalY)
    }

    fun targetFor(index: Int): ArrayList<Float> = if (index % 5 == 0) bold else thin
    fun widthFor(index: Int): Float = if (index % 5 == 0) 0.78f else 0.36f

    var index = 0
    var x = -halfWidth
    while (x <= halfWidth + 0.001f) {
        appendLine(targetFor(index), x.coerceIn(-halfWidth, halfWidth), -halfDepth, x.coerceIn(-halfWidth, halfWidth), halfDepth, widthFor(index))
        x += step
        index++
    }
    index = 0
    var y = -halfDepth
    while (y <= halfDepth + 0.001f) {
        appendLine(targetFor(index), -halfWidth, y.coerceIn(-halfDepth, halfDepth), halfWidth, y.coerceIn(-halfDepth, halfDepth), widthFor(index))
        y += step
        index++
    }

    val thinVertices = thin.toFloatArray()
    val boldVertices = bold.toFloatArray()
    return OrcaBedGridGeometry(
        thin = TriangleGeometry(vertices = thinVertices, normals = buildTopNormals(thinVertices)),
        bold = TriangleGeometry(vertices = boldVertices, normals = buildTopNormals(boldVertices))
    )
}

internal fun buildSelectedFootprintGeometry(selected: ModelObjectUpload): TriangleGeometry? {
    val padding = 2.5f
    val halfWidth = (selected.sizeX * 0.5f + padding).coerceAtLeast(5f)
    val halfDepth = (selected.sizeY * 0.5f + padding).coerceAtLeast(5f)
    val z = 0.048f
    val stripWidth = 1.15f
    val points = floatArrayOf(
        selected.centerX - halfWidth, selected.centerY - halfDepth,
        selected.centerX + halfWidth, selected.centerY - halfDepth,
        selected.centerX + halfWidth, selected.centerY + halfDepth,
        selected.centerX - halfWidth, selected.centerY + halfDepth
    )
    val vertices = buildOutlineStripGeometry(points, stripWidth, z)
    if (vertices.isEmpty()) return null
    return TriangleGeometry(vertices = vertices, normals = buildTopNormals(vertices))
}

internal fun buildTopNormals(vertices: FloatArray): FloatArray =
    FloatArray(vertices.size) { index -> if (index % 3 == 2) 1f else 0f }

private fun buildConvexFan(points: FloatArray, z: Float): FloatArray {
    val vertexCount = points.size / 2
    val values = FloatArray((vertexCount - 2) * 9)
    var writeIndex = 0
    val anchorX = points[0]
    val anchorY = points[1]
    for (index in 1 until vertexCount - 1) {
        values[writeIndex++] = anchorX
        values[writeIndex++] = anchorY
        values[writeIndex++] = z
        values[writeIndex++] = points[index * 2]
        values[writeIndex++] = points[index * 2 + 1]
        values[writeIndex++] = z
        values[writeIndex++] = points[(index + 1) * 2]
        values[writeIndex++] = points[(index + 1) * 2 + 1]
        values[writeIndex++] = z
    }
    return values
}

private fun buildWallGeometry(points: FloatArray, top: Float, bottom: Float): TriangleGeometry {
    val pointCount = points.size / 2
    val vertices = FloatArray(pointCount * 18)
    val normals = FloatArray(pointCount * 18)
    var vertexWriteIndex = 0
    var normalWriteIndex = 0
    for (index in 0 until pointCount) {
        val nextIndex = (index + 1) % pointCount
        val x0 = points[index * 2]
        val y0 = points[index * 2 + 1]
        val x1 = points[nextIndex * 2]
        val y1 = points[nextIndex * 2 + 1]
        val edgeX = x1 - x0
        val edgeY = y1 - y0
        val length = max(0.001f, sqrt(edgeX * edgeX + edgeY * edgeY))
        val normalX = edgeY / length
        val normalY = -edgeX / length

        val edgeVertices = floatArrayOf(
            x0, y0, top, x0, y0, bottom, x1, y1, bottom,
            x0, y0, top, x1, y1, bottom, x1, y1, top
        )
        edgeVertices.copyInto(vertices, destinationOffset = vertexWriteIndex)
        vertexWriteIndex += edgeVertices.size
        repeat(6) {
            normals[normalWriteIndex++] = normalX
            normals[normalWriteIndex++] = normalY
            normals[normalWriteIndex++] = 0f
        }
    }
    return TriangleGeometry(vertices = vertices, normals = normals)
}

private fun buildGridStripGeometry(
    widthMm: Float,
    depthMm: Float,
    stepMm: Float,
    stripWidthMm: Float,
    z: Float
): FloatArray {
    val halfGridWidth = widthMm * 0.5f
    val halfGridDepth = depthMm * 0.5f
    val halfStrip = stripWidthMm * 0.5f
    val values = ArrayList<Float>()

    fun appendRect(x0: Float, y0: Float, x1: Float, y1: Float) {
        values.add(x0)
        values.add(y0)
        values.add(z)
        values.add(x1)
        values.add(y0)
        values.add(z)
        values.add(x1)
        values.add(y1)
        values.add(z)
        values.add(x0)
        values.add(y0)
        values.add(z)
        values.add(x1)
        values.add(y1)
        values.add(z)
        values.add(x0)
        values.add(y1)
        values.add(z)
    }

    fun appendVertical(x: Float) {
        appendRect(
            x0 = (x - halfStrip).coerceAtLeast(-halfGridWidth),
            y0 = -halfGridDepth,
            x1 = (x + halfStrip).coerceAtMost(halfGridWidth),
            y1 = halfGridDepth
        )
    }

    fun appendHorizontal(y: Float) {
        appendRect(
            x0 = -halfGridWidth,
            y0 = (y - halfStrip).coerceAtLeast(-halfGridDepth),
            x1 = halfGridWidth,
            y1 = (y + halfStrip).coerceAtMost(halfGridDepth)
        )
    }

    appendVertical(0f)
    appendHorizontal(0f)

    var offset = stepMm
    while (offset < halfGridWidth - 0.001f) {
        appendVertical(offset)
        appendVertical(-offset)
        offset += stepMm
    }

    offset = stepMm
    while (offset < halfGridDepth - 0.001f) {
        appendHorizontal(offset)
        appendHorizontal(-offset)
        offset += stepMm
    }

    return FloatArray(values.size) { index -> values[index] }
}

private fun buildOutlineStripGeometry(points: FloatArray, stripWidthMm: Float, z: Float): FloatArray {
    val halfStrip = stripWidthMm * 0.5f
    val pointCount = points.size / 2
    if (pointCount < 2) return FloatArray(0)
    val values = ArrayList<Float>()

    fun add(x: Float, y: Float) {
        values.add(x)
        values.add(y)
        values.add(z)
    }

    for (index in 0 until pointCount) {
        val nextIndex = (index + 1) % pointCount
        val x0 = points[index * 2]
        val y0 = points[index * 2 + 1]
        val x1 = points[nextIndex * 2]
        val y1 = points[nextIndex * 2 + 1]
        val dx = x1 - x0
        val dy = y1 - y0
        val length = max(0.001f, sqrt(dx * dx + dy * dy))
        val nx = -dy / length * halfStrip
        val ny = dx / length * halfStrip

        add(x0 - nx, y0 - ny)
        add(x1 - nx, y1 - ny)
        add(x1 + nx, y1 + ny)
        add(x0 - nx, y0 - ny)
        add(x1 + nx, y1 + ny)
        add(x0 + nx, y0 + ny)
    }

    return FloatArray(values.size) { index -> values[index] }
}
