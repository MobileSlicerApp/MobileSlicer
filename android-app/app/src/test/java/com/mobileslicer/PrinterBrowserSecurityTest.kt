package com.mobileslicer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrinterBrowserSecurityTest {
    @Test
    fun normalizePrinterWebUiUrlAllowsHttpsAndLocalHttp() {
        assertEquals("https://example.com/", normalizePrinterWebUiUrl("https://example.com"))
        assertEquals("http://192.168.1.42/", normalizePrinterWebUiUrl("http://192.168.1.42"))
        assertEquals("http://printer.local/", normalizePrinterWebUiUrl("printer.local"))
    }

    @Test
    fun normalizePrinterWebUiUrlRejectsPublicHttpAndUnsupportedSchemes() {
        assertNull(normalizePrinterWebUiUrl("http://example.com"))
        assertNull(normalizePrinterWebUiUrl("ftp://192.168.1.42"))
    }
}
