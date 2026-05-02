package com.mobileslicer.viewer

internal enum class GcodePreviewDisplayMode(val nativeId: Int, val label: String) {
    Auto(-1, "Auto"),
    FeatureType(1, "Feature"),
    Filament(2, "Filament"),
    Speed(3, "Speed"),
    Temperature(12, "Temperature"),
    FanSpeed(11, "Fan"),
    VolumetricFlowRate(7, "Flow"),
    LayerTime(9, "Layer Time")
}
