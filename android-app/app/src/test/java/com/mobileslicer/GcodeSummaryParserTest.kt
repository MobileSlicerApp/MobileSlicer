package com.mobileslicer

import com.mobileslicer.workspace.GcodeSummaryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GcodeSummaryParserTest {
    @Test
    fun parsesNativeSummaryText() {
        val summary = GcodeSummaryParser.fromNativeSummary(
            "bytes=123|lines=7|layers=2|types=Outer wall,Inner wall|walls=Outer wall|time=1h 02m|grams=3.25"
        )

        requireNotNull(summary)
        assertEquals(123, summary.byteCount)
        assertEquals(7, summary.lineCount)
        assertEquals(2, summary.layerChangeCount)
        assertEquals(listOf("Outer wall", "Inner wall"), summary.observedTypes)
        assertEquals(listOf("Outer wall"), summary.wallShellTypes)
        assertEquals("1h 02m", summary.estimatedPrintTimeText)
        assertEquals(3.25, summary.filamentUsedGrams!!, 0.0001)
    }

    @Test
    fun parsesNativePreviewInfoFromOrcaSummary() {
        val summary = GcodeSummaryParser.fromNativeSummary(NativePreviewSummaryFixture.fullSummary)

        requireNotNull(summary)
        assertEquals(2, summary.previewInfo.lineTypes.size)
        assertEquals("Outer wall", summary.previewInfo.lineTypes.first().label)
        assertEquals(2, summary.previewInfo.lineTypes.first().nativeId)
        assertEquals(1, summary.previewInfo.filaments.size)
        assertEquals("Bambu PLA Basic", summary.previewInfo.filaments.first().label)
        assertEquals(60.0, summary.previewInfo.totalSeconds!!, 0.0001)
        assertEquals(2, summary.previewInfo.filamentChanges)
    }

    @Test
    fun parsesNativePreviewLabelsWithEscapedDelimitersAndUtf8() {
        val encodedLabel = "Caf%C3%A9%20PLA%2C%20Blue%3B%20A%7CB"
        val summary = GcodeSummaryParser.fromNativeSummary(
            "bytes=123|lines=7|layers=2|time=1h|grams=3.25" +
                "|previewLineTypes=role,2,$encodedLabel,#FF7D38,12.5,20.0,1.0,2.96" +
                "|previewFilaments=1,$encodedLabel,#00AA99,1.0,2.0,0.0,0.0,0.5,1.0,0.25,0.5,1.75,3.5,0.09" +
                "|previewTotals=totalSeconds=60.0,prepareSeconds=5.0,modelSeconds=55.0,cost=0.09,filamentChanges=2,extruderChanges=1"
        )

        requireNotNull(summary)
        assertEquals(1, summary.previewInfo.lineTypes.size)
        assertEquals(1, summary.previewInfo.filaments.size)
        assertEquals("Café PLA, Blue; A|B", summary.previewInfo.lineTypes.single().label)
        assertEquals("Café PLA, Blue; A|B", summary.previewInfo.filaments.single().label)
    }

    @Test
    fun ignoresMalformedUnknownAndPartialNativePreviewRows() {
        val summary = GcodeSummaryParser.fromNativeSummary(
            "bytes=123|lines=7|layers=2|time=1h|grams=3.25" +
                "|previewLineTypes=unknown,2,Travel,#38489B,4.0,6.4,0.0,0.0;role,not-int,Outer,#FF7D38,1,1,1,1;role,2,Outer,#FF7D38,1,1,1,1" +
                "|previewFilaments=bad-row;1,PLA,#00AA99,1.0,2.0,0.0,0.0,0.5,1.0,0.25,0.5,1.75,3.5,0.09"
        )

        requireNotNull(summary)
        assertEquals(1, summary.previewInfo.lineTypes.size)
        assertEquals("Outer", summary.previewInfo.lineTypes.single().label)
        assertEquals(1, summary.previewInfo.filaments.size)
        assertEquals("PLA", summary.previewInfo.filaments.single().label)
        assertNull(summary.previewInfo.totalSeconds)
        assertEquals(0, summary.previewInfo.filamentChanges)
    }

    @Test
    fun defaultsBlankNativePreviewFilamentLabelAndInvalidColor() {
        val summary = GcodeSummaryParser.fromNativeSummary(
            "bytes=123|lines=7|layers=2|time=1h|grams=3.25" +
                "|previewFilaments=3,,not-a-color,1.0,2.0,0.0,0.0,0.5,1.0,0.25,0.5,1.75,3.5,0.09"
        )

        requireNotNull(summary)
        assertEquals(1, summary.previewInfo.filaments.size)
        assertEquals("Filament 3", summary.previewInfo.filaments.single().label)
        assertEquals("#8FC1FF", summary.previewInfo.filaments.single().colorHex)
    }

    @Test
    fun parsesDocumentedNativePreviewContractFixtureWithinSmokeBudget() {
        val startedAtNs = System.nanoTime()
        val summary = GcodeSummaryParser.fromNativeSummary(NativePreviewSummaryFixture.fullSummary)
        val elapsedMs = (System.nanoTime() - startedAtNs) / 1_000_000.0

        requireNotNull(summary)
        assertTrue("native preview summary parse exceeded smoke-test budget: $elapsedMs ms", elapsedMs < 1_000.0)
        assertEquals(2, summary.previewInfo.lineTypes.size)
        assertEquals(1, summary.previewInfo.filaments.size)
        assertEquals(60.0, summary.previewInfo.totalSeconds!!, 0.0001)
    }

    @Test
    fun normalizesNativeSummaryTotalEstimatedTime() {
        val summary = GcodeSummaryParser.fromNativeSummary(
            "bytes=123|lines=7|layers=2|time=47m 12s; total estimated time: 53m 21s|grams=11.22"
        )

        requireNotNull(summary)
        assertEquals("53m 21s", summary.estimatedPrintTimeText)
        assertEquals(11.22, summary.filamentUsedGrams!!, 0.0001)
    }

    @Test
    fun rejectsNativeSummaryMissingRequiredFields() {
        assertNull(GcodeSummaryParser.fromNativeSummary("bytes=123|lines=7"))
    }

    @Test
    fun summarizesGcodeFallbackMetrics() {
        val gcode = """
            ; filament_diameter: 1.75
            ; filament_density: 1.24
            ;LAYER_CHANGE
            ;TYPE:Outer wall
            G90
            M82
            G1 X0 Y0 Z0.2 F1200
            G1 X10 Y0 E1.0 F1200
            ; filament used [mm] = 100.0
        """.trimIndent()

        val summary = GcodeSummaryParser.fromGcode(gcode)

        assertEquals(gcode.length, summary.byteCount)
        assertEquals(9, summary.lineCount)
        assertEquals(1, summary.layerChangeCount)
        assertEquals(listOf("Outer wall"), summary.observedTypes)
        assertEquals(listOf("Outer wall"), summary.wallShellTypes)
        assertTrue((summary.filamentUsedGrams ?: 0.0) > 0.0)
    }

    @Test
    fun summarizesOrcaChangeLayerMarkers() {
        val gcode = """
            ; CHANGE_LAYER
            ;Z:0.2
            G1 X10 Y0 E1.0 F1200
            ; CHANGE_LAYER
            ;Z:0.4
            G1 X20 Y0 E2.0 F1200
        """.trimIndent()

        val summary = GcodeSummaryParser.fromGcode(gcode)

        assertEquals(2, summary.layerChangeCount)
    }

    @Test
    fun usesOrcaTotalEstimatedTimeForDisplay() {
        val gcode = """
            ; model printing time: 47m 12s; total estimated time: 53m 21s
            ; CHANGE_LAYER
            G1 X10 Y0 E1.0 F1200
            ; filament used [g] = 11.22, 0.00
        """.trimIndent()

        val summary = GcodeSummaryParser.fromGcode(gcode)

        assertEquals("53m 21s", summary.estimatedPrintTimeText)
    }

    @Test
    fun includesWipeTowerFilamentWhenOrcaTotalIsMissing() {
        val gcode = """
            ; filament used [g] = 11.00, 2.00
            ; filament used for wipe tower [g] = 3.50
            ; CHANGE_LAYER
            G1 X10 Y0 E1.0 F1200
        """.trimIndent()

        val summary = GcodeSummaryParser.fromGcode(gcode)

        assertEquals(16.50, summary.filamentUsedGrams!!, 0.0001)
    }

    @Test
    fun doesNotDoubleCountWipeTowerWhenOrcaTotalExists() {
        val gcode = """
            ; filament used [g] = 11.00, 2.00
            ; filament used for wipe tower [g] = 3.50
            ; total filament used [g] = 20.00
            ; CHANGE_LAYER
            G1 X10 Y0 E1.0 F1200
        """.trimIndent()

        val summary = GcodeSummaryParser.fromGcode(gcode)

        assertEquals(20.00, summary.filamentUsedGrams!!, 0.0001)
    }
}

private object NativePreviewSummaryFixture {
    const val fullSummary: String =
        "bytes=123|lines=7|layers=2|time=1h|grams=3.25" +
            "|previewLineTypes=role,2,Outer wall,#FF7D38,12.5,20.0,1.0,2.96;option,0,Travel,#38489B,4.0,6.4,0.0,0.0" +
            "|previewFilaments=1,Bambu PLA Basic,#00AA99,1.0,2.0,0.0,0.0,0.5,1.0,0.25,0.5,1.75,3.5,0.09" +
            "|previewTotals=totalSeconds=60.0,prepareSeconds=5.0,modelSeconds=55.0,cost=0.09,filamentChanges=2,extruderChanges=1"
}
