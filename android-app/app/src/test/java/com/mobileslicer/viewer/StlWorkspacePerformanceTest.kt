package com.mobileslicer.viewer

import java.io.File
import java.nio.file.Files
import java.io.RandomAccessFile
import kotlin.math.max
import kotlin.math.sqrt
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class StlWorkspacePerformanceTest {
    private val legacyLocalFixture =
        File("../../_quarantine/root-dependency-dump/data/meshes/pig.stl")

    private data class TimingSample(
        val stagingMs: Double,
        val oldViewerPrepMs: Double,
        val viewerPrepMs: Double,
        val oldTransformCopyMs: Double,
        val newTransformPrepMs: Double
    )

    @Test
    fun benchmarkComplexStlWorkspacePreparationHotspots() {
        val source = benchmarkFixture().canonicalFile
        assumeTrue("Expected complex STL fixture at ${source.absolutePath}", source.exists())

        val samples = buildList {
            repeat(5) {
                add(runSample(source))
            }
        }
        val median = samples.sortedBy { it.viewerPrepMs }[samples.size / 2]

        if (System.getenv("MOBILESLICER_PRINT_STL_BENCHMARKS") == "1") {
            println(
                buildString {
                    append("fixture=")
                    append(source.absolutePath)
                    append('\n')
                    append("median stagingMs=")
                    append("%.2f".format(median.stagingMs))
                    append('\n')
                    append("median oldViewerPrepMs=")
                    append("%.2f".format(median.oldViewerPrepMs))
                    append('\n')
                    append("median viewerPrepMs=")
                    append("%.2f".format(median.viewerPrepMs))
                    append('\n')
                    append("median oldTransformCopyMs=")
                    append("%.2f".format(median.oldTransformCopyMs))
                    append('\n')
                    append("median newTransformPrepMs=")
                    append("%.4f".format(median.newTransformPrepMs))
                    append('\n')
                    append("median parserSpeedupX=")
                    append("%.2f".format(median.oldViewerPrepMs / median.viewerPrepMs))
                    append('\n')
                    append("median oldFirstVisibleExcludingNativeMs=")
                    append("%.2f".format(median.stagingMs + median.oldViewerPrepMs + median.oldTransformCopyMs))
                    append('\n')
                    append("median newUserVisibleImportExcludingNativeMs=")
                    append("%.2f".format(median.stagingMs))
                    append('\n')
                    append("median newWorkspaceOpenExcludingNativeMs=")
                    append("%.2f".format(median.viewerPrepMs + median.newTransformPrepMs))
                }
            )
        }
    }

    private fun benchmarkFixture(): File {
        val override = System.getenv("MOBILESLICER_STL_BENCH_FIXTURE")?.trim().orEmpty()
        if (override.isNotEmpty()) {
            return File(override)
        }
        return legacyLocalFixture
    }

    private fun runSample(source: File): TimingSample {
        val staged = Files.createTempFile("mobileslicer-import-bench-", ".stl").toFile()
        try {
            val stagingMs = measureMs {
                source.inputStream().use { input ->
                    staged.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val oldViewerPrepMs = measureMs {
                legacyParseBinary(staged)
            }

            lateinit var mesh: StlMesh
            val viewerPrepMs = measureMs {
                mesh = StlMeshParser.parse(staged)
            }

            val oldTransformCopyMs = measureMs {
                oldRendererTransformCopy(mesh)
            }
            val newTransformPrepMs = measureMs {
                newRendererTransformPrep(mesh)
            }

            return TimingSample(
                stagingMs = stagingMs,
                oldViewerPrepMs = oldViewerPrepMs,
                viewerPrepMs = viewerPrepMs,
                oldTransformCopyMs = oldTransformCopyMs,
                newTransformPrepMs = newTransformPrepMs
            )
        } finally {
            staged.delete()
        }
    }

    private fun oldRendererTransformCopy(mesh: StlMesh) {
        val offsetX = -mesh.bounds.centerX
        val offsetY = -mesh.bounds.centerY
        val offsetZ = -mesh.bounds.minZ
        val transformed = FloatArray(mesh.vertices.size)

        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY

        var index = 0
        while (index < mesh.vertices.size) {
            val x = mesh.vertices[index] + offsetX
            val y = mesh.vertices[index + 1] + offsetY
            val z = mesh.vertices[index + 2] + offsetZ
            transformed[index] = x
            transformed[index + 1] = y
            transformed[index + 2] = z
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            minZ = minOf(minZ, z)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
            maxZ = maxOf(maxZ, z)
            index += 3
        }

        val sceneSpan = max(
            220f,
            max(max(maxX - minX, maxY - minY), maxZ - minZ).coerceAtLeast(40f)
        )
        assertTrue(sceneSpan >= 40f)
        assertTrue(transformed.isNotEmpty())
    }

    private fun newRendererTransformPrep(mesh: StlMesh) {
        val modelOffsetX = -mesh.bounds.centerX
        val modelOffsetY = -mesh.bounds.centerY
        val modelOffsetZ = -mesh.bounds.minZ
        val sceneSpan = max(
            220f,
            max(max(mesh.bounds.sizeX, mesh.bounds.sizeY), mesh.bounds.sizeZ).coerceAtLeast(40f)
        )
        assertTrue(sceneSpan >= 40f)
        assertTrue(modelOffsetX.isFinite())
        assertTrue(modelOffsetY.isFinite())
        assertTrue(modelOffsetZ.isFinite())
    }

    private fun measureMs(block: () -> Unit): Double {
        val startedAt = System.nanoTime()
        block()
        return (System.nanoTime() - startedAt) / 1_000_000.0
    }

    private fun legacyParseBinary(file: File): StlMesh {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(80L)
            val triangleCount = readLittleEndianInt(raf)
            val vertices = FloatArray(triangleCount * 9)
            val normals = FloatArray(triangleCount * 9)
            var vertexIndex = 0

            var minX = Float.POSITIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var minZ = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            var maxZ = Float.NEGATIVE_INFINITY

            repeat(triangleCount) {
                val nx = readLittleEndianFloat(raf)
                val ny = readLittleEndianFloat(raf)
                val nz = readLittleEndianFloat(raf)
                val normal = legacyNormalize(nx, ny, nz)

                repeat(3) {
                    val x = readLittleEndianFloat(raf)
                    val y = readLittleEndianFloat(raf)
                    val z = readLittleEndianFloat(raf)

                    vertices[vertexIndex] = x
                    normals[vertexIndex] = normal[0]
                    vertexIndex++
                    vertices[vertexIndex] = y
                    normals[vertexIndex] = normal[1]
                    vertexIndex++
                    vertices[vertexIndex] = z
                    normals[vertexIndex] = normal[2]
                    vertexIndex++

                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    minZ = minOf(minZ, z)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                    maxZ = maxOf(maxZ, z)
                }
                raf.skipBytes(2)
            }

            return StlMesh(
                vertices = vertices,
                normals = normals,
                triangleCount = triangleCount,
                bounds = MeshBounds(minX, minY, minZ, maxX, maxY, maxZ)
            )
        }
    }

    private fun legacyNormalize(x: Float, y: Float, z: Float): FloatArray {
        val length = sqrt(x * x + y * y + z * z)
        return if (length > 0.0001f) {
            floatArrayOf(x / length, y / length, z / length)
        } else {
            floatArrayOf(0f, 0f, 1f)
        }
    }

    private fun readLittleEndianInt(raf: RandomAccessFile): Int {
        val b0 = raf.read()
        val b1 = raf.read()
        val b2 = raf.read()
        val b3 = raf.read()
        return b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
    }

    private fun readLittleEndianFloat(raf: RandomAccessFile): Float {
        return Float.fromBits(readLittleEndianInt(raf))
    }
}
