package com.mobileslicer.viewer

internal enum class GcodePreviewPerformanceMode(
    val vertexBudget: Long,
    val displayLabel: String,
    val description: String
) {
    Low(
        vertexBudget = 400_000L,
        displayLabel = "Low end",
        description = "400k vertices per exact Preview chunk for older or slower phones."
    ),
    MidRange(
        vertexBudget = 750_000L,
        displayLabel = "Mid range",
        description = "750k vertices per exact Preview chunk for most phones."
    ),
    HighEnd(
        vertexBudget = 1_000_000L,
        displayLabel = "High end",
        description = "1m vertices per exact Preview chunk for stronger phones."
    );

    companion object {
        const val HARD_VERTEX_CEILING: Long = 1_000_000L
        val Default: GcodePreviewPerformanceMode = MidRange

        fun fromStoredName(value: String?): GcodePreviewPerformanceMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
