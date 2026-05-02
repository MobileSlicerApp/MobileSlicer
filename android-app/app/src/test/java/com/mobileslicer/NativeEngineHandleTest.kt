package com.mobileslicer

import com.mobileslicer.nativebridge.NativeEngineCallResult
import com.mobileslicer.nativebridge.NativeEngineError
import com.mobileslicer.nativebridge.NativeEngineErrorCode
import com.mobileslicer.nativebridge.NativeEngineHandle
import com.mobileslicer.nativebridge.isSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeEngineHandleTest {
    @Test
    fun zeroRawHandleIsRejected() {
        assertNull(NativeEngineHandle.fromRaw(0L))
    }

    @Test
    fun nonZeroRawHandleIsAccepted() {
        val handle = requireNotNull(NativeEngineHandle.fromRaw(42L))

        assertEquals(42L, handle.raw)
    }

    @Test
    fun callResultExposesStableFailureStatus() {
        val result = NativeEngineCallResult.Failure(
            operation = "nativeSlice",
            error = NativeEngineError(
                code = NativeEngineErrorCode.PrintableVolumeExceeded,
                message = "printable volume exceeded"
            )
        )

        assertFalse(result.isSuccess())
        assertEquals(
            "nativeSlice failed: PrintableVolumeExceeded nativeError=printable volume exceeded",
            result.statusMessage
        )
    }

    @Test
    fun successResultIsSuccess() {
        assertTrue(NativeEngineCallResult.Success.isSuccess())
    }
}
