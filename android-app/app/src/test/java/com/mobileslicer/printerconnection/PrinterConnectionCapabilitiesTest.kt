package com.mobileslicer.printerconnection

import com.mobileslicer.profiles.PrintHostType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class PrinterConnectionCapabilitiesTest {
    @Test
    fun bambuLanKeepsSendFeatureVisibleButRoutesThroughDeviceAgent() {
        val capabilities = PrintHostType.BambuLan.connectionCapabilities()
        val fields = PrintHostType.BambuLan.connectionFieldSpecs().associateBy { it.field }

        assertTrue(capabilities.canUpload)
        assertTrue(capabilities.canUploadAndStart)
        assertTrue(capabilities.requiresApiKeyOrToken)
        assertTrue(capabilities.requiresDeviceSerial)
        assertEquals(PrinterConnectionUploadRoute.BambuLanAgent, capabilities.uploadRoute)
        assertEquals("Bambu LAN IP or hostname", fields.getValue(PrinterConnectionField.Host).label)
        assertEquals("LAN access code", fields.getValue(PrinterConnectionField.ApiKey).label)
        assertTrue(fields.getValue(PrinterConnectionField.BambuBedType).visible)
        assertFalse(fields.getValue(PrinterConnectionField.Authorization).visible)
    }

    @Test
    fun simplyPrintUsesExternalImportAndDoesNotExposeDirectStart() {
        val capabilities = PrintHostType.SimplyPrint.connectionCapabilities()

        assertTrue(capabilities.canUpload)
        assertFalse(capabilities.canUploadAndStart)
        assertTrue(capabilities.canQueue)
        assertEquals(PrinterConnectionUploadRoute.ExternalImport, capabilities.uploadRoute)
    }

    @Test
    fun flashAirMatchesOrcaUploadOnlyBehavior() {
        val capabilities = PrintHostType.FlashAir.connectionCapabilities()

        assertTrue(capabilities.canUpload)
        assertFalse(capabilities.canUploadAndStart)
        assertEquals(PrinterConnectionUploadRoute.HttpPrintHost, capabilities.uploadRoute)
    }

    @Test
    fun hostSpecificBrowseLabelsComeFromCapabilities() {
        assertEquals("Browse Storage", PrintHostType.PrusaLink.connectionCapabilities().browseTargetsLabel)
        assertEquals("Browse Printers", PrintHostType.Repetier.connectionCapabilities().browseTargetsLabel)
        assertEquals("Browse Groups", PrintHostType.Repetier.connectionCapabilities().browseGroupsLabel)
    }
}
