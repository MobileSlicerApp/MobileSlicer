package com.mobileslicer

import com.mobileslicer.nativebridge.NativeEngineSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeEngineSessionTest {
    @Test
    fun newSessionStartsWithoutHandle() {
        val session = NativeEngineSession()

        assertEquals(0L, session.currentRawHandle)
        assertNull(session.handleOrNull())
    }

    @Test
    fun destroyWithoutHandleIsNoop() {
        val session = NativeEngineSession()

        session.destroy()

        assertEquals(0L, session.currentRawHandle)
    }
}
