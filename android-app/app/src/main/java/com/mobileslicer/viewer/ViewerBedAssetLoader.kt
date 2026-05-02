package com.mobileslicer.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

private const val ORCA_BED_MODEL_TOP_Z = -0.08f
private const val ORCA_BED_TEXTURE_Z = -0.055f

internal fun loadOrcaBedModelGeometry(context: Context, bed: PrinterBedSpec): TriangleGeometry? {
    val assetPath = bed.bedModelAssetPath.takeIf { it.isNotBlank() } ?: return null
    val cacheFile = cachedAssetFile(context, assetPath)
    if (!cacheFile.exists() || cacheFile.length() == 0L) {
        cacheFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    val mesh = StlMeshParser.parse(cacheFile)
    if (mesh.triangleCount <= 0) return null

    val filteredMesh = removeDownwardFacingBedTriangles(mesh)
    val vertices = filteredMesh.vertices
    val zOffset = ORCA_BED_MODEL_TOP_Z - mesh.bounds.maxZ
    var index = 2
    while (index < vertices.size) {
        vertices[index] += zOffset
        index += 3
    }
    return TriangleGeometry(vertices = vertices, normals = filteredMesh.normals)
}

internal data class SplitBedModelGeometry(
    val opaque: TriangleGeometry,
    val underside: TriangleGeometry
)

internal fun loadSplitOrcaBedModelGeometry(context: Context, bed: PrinterBedSpec): SplitBedModelGeometry? {
    val assetPath = bed.bedModelAssetPath.takeIf { it.isNotBlank() } ?: return null
    val cacheFile = cachedAssetFile(context, assetPath)
    if (!cacheFile.exists() || cacheFile.length() == 0L) {
        cacheFile.parentFile?.mkdirs()
        context.assets.open(assetPath).use { input ->
            cacheFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
    val mesh = StlMeshParser.parse(cacheFile)
    if (mesh.triangleCount <= 0) return null
    val split = splitBedTriangles(mesh)
    val zOffset = ORCA_BED_MODEL_TOP_Z - mesh.bounds.maxZ
    offsetZ(split.opaqueVertices, zOffset)
    offsetZ(split.undersideVertices, zOffset)
    return SplitBedModelGeometry(
        opaque = TriangleGeometry(vertices = split.opaqueVertices, normals = split.opaqueNormals),
        underside = TriangleGeometry(vertices = split.undersideVertices, normals = split.undersideNormals)
    )
}

private data class FilteredBedMesh(
    val vertices: FloatArray,
    val normals: FloatArray
)

private fun removeDownwardFacingBedTriangles(mesh: StlMesh): FilteredBedMesh {
    val vertices = ArrayList<Float>(mesh.vertices.size)
    val normals = ArrayList<Float>(mesh.normals.size)
    var index = 0
    while (index < mesh.vertices.size) {
        val normalZ = (
            mesh.normals[index + 2] +
                mesh.normals[index + 5] +
                mesh.normals[index + 8]
            ) / 3f
        if (normalZ > -0.25f) {
            repeat(9) { offset ->
                vertices.add(mesh.vertices[index + offset])
                normals.add(mesh.normals[index + offset])
            }
        }
        index += 9
    }
    return FilteredBedMesh(
        vertices = vertices.toFloatArray(),
        normals = normals.toFloatArray()
    )
}

private data class SplitBedMesh(
    val opaqueVertices: FloatArray,
    val opaqueNormals: FloatArray,
    val undersideVertices: FloatArray,
    val undersideNormals: FloatArray
)

private fun splitBedTriangles(mesh: StlMesh): SplitBedMesh {
    val opaqueVertices = ArrayList<Float>(mesh.vertices.size)
    val opaqueNormals = ArrayList<Float>(mesh.normals.size)
    val undersideVertices = ArrayList<Float>()
    val undersideNormals = ArrayList<Float>()
    var index = 0
    while (index < mesh.vertices.size) {
        val normalZ = (
            mesh.normals[index + 2] +
                mesh.normals[index + 5] +
                mesh.normals[index + 8]
            ) / 3f
        val targetVertices = if (normalZ < -0.25f) undersideVertices else opaqueVertices
        val targetNormals = if (normalZ < -0.25f) undersideNormals else opaqueNormals
        repeat(9) { offset ->
            targetVertices.add(mesh.vertices[index + offset])
            targetNormals.add(mesh.normals[index + offset])
        }
        index += 9
    }
    return SplitBedMesh(
        opaqueVertices = opaqueVertices.toFloatArray(),
        opaqueNormals = opaqueNormals.toFloatArray(),
        undersideVertices = undersideVertices.toFloatArray(),
        undersideNormals = undersideNormals.toFloatArray()
    )
}

private fun offsetZ(vertices: FloatArray, zOffset: Float) {
    var index = 2
    while (index < vertices.size) {
        vertices[index] += zOffset
        index += 3
    }
}

internal data class BedTextureGeometry(
    val vertices: FloatArray,
    val uvs: FloatArray,
    val bitmap: Bitmap
)

internal fun loadOrcaBedTextureGeometry(context: Context, bed: PrinterBedSpec): BedTextureGeometry? {
    val assetPath = bed.bedTextureAssetPath.takeIf { it.isNotBlank() } ?: return null
    val bitmap = context.assets.open(assetPath).use { input ->
        BitmapFactory.decodeStream(input)
    } ?: return null
    val halfWidth = bed.widthMm * 0.5f
    val halfDepth = bed.depthMm * 0.5f
    val vertices = floatArrayOf(
        -halfWidth, -halfDepth, ORCA_BED_TEXTURE_Z,
        halfWidth, -halfDepth, ORCA_BED_TEXTURE_Z,
        halfWidth, halfDepth, ORCA_BED_TEXTURE_Z,
        -halfWidth, -halfDepth, ORCA_BED_TEXTURE_Z,
        halfWidth, halfDepth, ORCA_BED_TEXTURE_Z,
        -halfWidth, halfDepth, ORCA_BED_TEXTURE_Z
    )
    val uvs = floatArrayOf(
        0f, 1f,
        1f, 1f,
        1f, 0f,
        0f, 1f,
        1f, 0f,
        0f, 0f
    )
    return BedTextureGeometry(vertices = vertices, uvs = uvs, bitmap = bitmap)
}

private fun cachedAssetFile(context: Context, assetPath: String): File {
    val extension = assetPath.substringAfterLast('.', "stl").ifBlank { "stl" }
    val safeName = assetPath.hashCode().toUInt().toString(16) + "." + extension
    return File(File(context.cacheDir, "orca-bed-models"), safeName)
}
