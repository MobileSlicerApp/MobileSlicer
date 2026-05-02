package com.mobileslicer.printerconnection

import org.junit.Assert.assertEquals
import org.junit.Test

class PrinterConnectionFormattingTest {
    @Test
    fun bambuPackageFileNameNormalizesToGcode3mf() {
        assertEquals("Benchy.gcode.3mf", "Benchy.gcode".toBambuPackageFileName())
        assertEquals("Benchy.gcode.3mf", "Benchy.3mf".toBambuPackageFileName())
        assertEquals("Benchy.gcode.3mf", "Benchy.gcode.3mf".toBambuPackageFileName())
        assertEquals("bad_name.gcode.3mf", "bad/name".toBambuPackageFileName())
    }
}
