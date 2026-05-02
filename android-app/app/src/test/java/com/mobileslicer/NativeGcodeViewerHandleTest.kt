package com.mobileslicer

import com.mobileslicer.nativebridge.NativeGcodeViewerHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeGcodeViewerHandleTest {
    @Test
    fun zeroRawViewerHandleIsRejected() {
        assertNull(NativeGcodeViewerHandle.fromRaw(0L))
    }

    @Test
    fun nonZeroRawViewerHandleIsAccepted() {
        val handle = requireNotNull(NativeGcodeViewerHandle.fromRaw(7L))

        assertEquals(7L, handle.raw)
    }
}
