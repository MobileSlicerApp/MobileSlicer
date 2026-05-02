package com.mobileslicer

import com.mobileslicer.automation.AutomationSliceTiming
import com.mobileslicer.automation.AutomationSliceNativeMetrics
import com.mobileslicer.automation.automationSliceSuccessStatus
import com.mobileslicer.automation.parseAutomationSliceNativeMetrics
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationSliceRunnerTest {
    @Test
    fun successStatusIncludesPhaseTimings() {
        val outputFile = createTempFile(prefix = "automation-status", suffix = ".gcode")
        outputFile.writeText("G1 X1 Y1 E1\n")

        val status = automationSliceSuccessStatus(
            modelFile = File("/tmp/model.stl"),
            stagedModel = File("/tmp/staged-model.stl"),
            outputFile = outputFile,
            timing = AutomationSliceTiming(
                stagingMs = 1,
                nativeLoadMs = 2,
                placementMs = 3,
                configMs = 4,
                nativeSliceMs = 5,
                writeGcodeMs = 6,
                totalMs = 21
            ),
            nativeMetrics = AutomationSliceNativeMetrics(
                previewMoves = 7,
                previewCacheBuilt = true,
                previewCacheComplete = true,
                previewCachedVertices = 8,
                previewCacheBuildMs = 9
            ),
            configJson = """{"layer_height":0.2}"""
        )

        assertTrue(status.startsWith("success:"))
        assertTrue(status.contains("bytes=${outputFile.length()}"))
        assertTrue(status.contains("stagingMs=1"))
        assertTrue(status.contains("nativeLoadMs=2"))
        assertTrue(status.contains("placementMs=3"))
        assertTrue(status.contains("configMs=4"))
        assertTrue(status.contains("nativeSliceMs=5"))
        assertTrue(status.contains("writeGcodeMs=6"))
        assertTrue(status.contains("previewMoves=7"))
        assertTrue(status.contains("previewCacheBuilt=1"))
        assertTrue(status.contains("previewCacheComplete=1"))
        assertTrue(status.contains("previewCachedVertices=8"))
        assertTrue(status.contains("previewCacheBuildMs=9"))
        assertTrue(status.contains("elapsedMs=21"))
        assertTrue(status.contains("""config={"layer_height":0.2}"""))
    }

    @Test
    fun parsesNativeSliceMetrics() {
        val metrics = parseAutomationSliceNativeMetrics(
            "previewMoves=123|previewCacheBuilt=1|previewCacheComplete=0|previewCachedVertices=456|previewCacheBuildMs=7"
        )

        assertEquals(123L, metrics.previewMoves)
        assertTrue(metrics.previewCacheBuilt)
        assertEquals(false, metrics.previewCacheComplete)
        assertEquals(456L, metrics.previewCachedVertices)
        assertEquals(7L, metrics.previewCacheBuildMs)
    }
}
