package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrinterRuntimeStatusMappingTest {
    @Test
    fun statusResultInfersStructuredRuntimeFields() {
        val result = PrinterConnectionResult(
            success = true,
            title = "Printer status",
            detail = "State: printing • Progress: 42% • File: Benchy.gcode"
        ).withInferredRuntimeStatus(PrintHostType.OctoPrint)

        val status = result.runtimeStatus
        assertNotNull(status)
        checkNotNull(status)
        assertEquals(true, status.reachable)
        assertEquals(PrinterState.Printing, status.state)
        assertEquals(42, status.progressPercent)
        assertEquals("Benchy.gcode", status.currentFile)
        assertEquals("octoprint", status.hostType)
    }
}
