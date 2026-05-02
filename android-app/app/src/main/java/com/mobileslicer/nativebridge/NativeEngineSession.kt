package com.mobileslicer.nativebridge

internal class NativeEngineSession {
    private var rawHandle: Long = 0L

    val currentRawHandle: Long
        get() = rawHandle

    fun handleOrNull(): NativeEngineHandle? =
        NativeEngineHandle.fromRaw(rawHandle)

    fun ensureHandle(): NativeEngineHandle? {
        if (rawHandle == 0L) {
            rawHandle = NativeEngineBridge.nativeCreateEngine()
        }
        return handleOrNull()
    }

    fun ensureRawHandle(): Long =
        ensureHandle()?.raw ?: 0L

    fun clearGeneratedGcode() {
        handleOrNull()?.let(NativeEngineCalls::clearGeneratedGcode)
    }

    fun destroy() {
        val handle = rawHandle
        if (handle != 0L) {
            NativeEngineBridge.nativeDestroyEngine(handle)
            rawHandle = 0L
        }
    }
}
