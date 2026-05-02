# Native Error Contract

The JNI layer still exposes the raw native error string through
`NativeEngineBridge.nativeGetLastError(handle)` for diagnostics.

Android UI code should prefer the typed wrapper surface in
`NativeEngineHandle.kt`. `NativeEngineHandle.fromRaw(...)` rejects null native
handles, and `NativeEngineCalls` converts boolean-returning JNI calls into
`NativeEngineCallResult` values with stable `NativeEngineErrorCode` diagnostics.
G-code preview code should use `NativeGcodeViewerHandle.kt` so viewer handles
are not mixed with slicer engine handles.

`NativeEngineBridge.nativeGetLastEngineError(handle)` remains available when a
call site still has to use the raw JNI surface directly. It returns a
`NativeEngineError` with a stable `NativeEngineErrorCode` and the original
message. Branch on the code for behavior and keep the message for logging or
display.

Current codes are defined in `NativeEngineError.kt`. New native failures should
be added there before UI code starts matching against a raw string.
