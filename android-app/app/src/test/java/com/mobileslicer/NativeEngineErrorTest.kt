package com.mobileslicer

import com.mobileslicer.nativebridge.NativeEngineErrorCode
import com.mobileslicer.nativebridge.parseNativeEngineError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeEngineErrorTest {
    @Test
    fun parsesPrintableVolumeFailures() {
        val error = parseNativeEngineError("printable volume exceeded errorCode=12")

        requireNotNull(error)
        assertEquals(NativeEngineErrorCode.PrintableVolumeExceeded, error.code)
        assertEquals("printable volume exceeded errorCode=12", error.message)
    }

    @Test
    fun blankMessageHasNoStructuredError() {
        assertNull(parseNativeEngineError(" "))
    }
}
