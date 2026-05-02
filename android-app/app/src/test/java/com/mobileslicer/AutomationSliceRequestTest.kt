package com.mobileslicer

import com.mobileslicer.automation.AutomationSliceRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutomationSliceRequestTest {
    @Test
    fun parsesAutomationPathsWithDefaultStatusFile() {
        val paths = AutomationSliceRequest.pathsFromValues(
            action = AutomationSliceRequest.ACTION_AUTOMATE_SLICE,
            modelPath = "/tmp/model.3mf",
            outputPath = "/tmp/output.gcode",
            statusPath = null
        )

        requireNotNull(paths)
        assertEquals("/tmp/model.3mf", paths.modelPath)
        assertEquals("/tmp/output.gcode", paths.outputPath)
        assertEquals("/tmp/output.gcode.status.txt", paths.statusPath)
    }

    @Test
    fun ignoresNonAutomationAction() {
        val paths = AutomationSliceRequest.pathsFromValues(
            action = "android.intent.action.VIEW",
            modelPath = "/tmp/model.3mf",
            outputPath = "/tmp/output.gcode",
            statusPath = "/tmp/status.txt"
        )

        assertNull(paths)
    }

    @Test
    fun rejectsMissingRequiredPaths() {
        assertNull(
            AutomationSliceRequest.pathsFromValues(
                action = AutomationSliceRequest.ACTION_AUTOMATE_SLICE,
                modelPath = "",
                outputPath = "/tmp/output.gcode",
                statusPath = null
            )
        )
        assertNull(
            AutomationSliceRequest.pathsFromValues(
                action = AutomationSliceRequest.ACTION_AUTOMATE_SLICE,
                modelPath = "/tmp/model.3mf",
                outputPath = " ",
                statusPath = null
            )
        )
    }
}
