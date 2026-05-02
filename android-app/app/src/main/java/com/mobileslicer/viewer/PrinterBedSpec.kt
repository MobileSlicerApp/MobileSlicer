package com.mobileslicer.viewer

internal data class PrinterBedSpec(
    val widthMm: Float,
    val depthMm: Float,
    val maxHeightMm: Float,
    val originXmm: Float = 0f,
    val originYmm: Float = 0f,
    val bedModelAssetPath: String = "",
    val bedTextureAssetPath: String = "",
    val bedTextureIncludesGrid: Boolean = false
)
