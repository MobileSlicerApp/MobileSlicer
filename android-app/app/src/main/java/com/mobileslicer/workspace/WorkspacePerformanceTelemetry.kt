package com.mobileslicer.workspace

internal data class WorkspacePerformanceSnapshot(
    val prepareDurationMs: Long?,
    val sliceDurationMs: Long?,
    val displayLodSummary: String,
    val warnings: List<String>
) {
    fun compactSummary(): String = buildList {
        prepareDurationMs?.let { add("Prepare: ${formatDurationSecondsTenths(it)}") }
        if (displayLodSummary.isNotBlank()) add(displayLodSummary)
        sliceDurationMs?.let { add("Slice: ${formatDurationSecondsTenths(it)}") }
        warnings.forEach { add(it) }
    }.joinToString(" • ")
}

internal fun workspacePerformanceSnapshot(
    importTiming: ModelImportTiming?,
    workspacePreparationTiming: WorkspacePreparationTiming?,
    firstVisibleWorkspaceFrameMs: Long?,
    sliceTiming: SlicePipelineTiming?
): WorkspacePerformanceSnapshot {
    val prepareDurationMs = firstVisibleWorkspaceFrameMs ?: run {
        var totalMs = 0L
        var hasTiming = false
        importTiming?.let {
            totalMs += it.stagingMs + it.nativeLoadMs
            hasTiming = true
        }
        workspacePreparationTiming?.let {
            totalMs += it.viewerMeshPrepMs
            hasTiming = true
        }
        totalMs.takeIf { hasTiming }
    }
    val displayLodSummary = workspacePreparationTiming
        ?.takeIf { it.reducedForDisplay && it.sourceTriangleCount != null && it.displayTriangleCount != null }
        ?.let { "Display LOD: ${it.displayTriangleCount}/${it.sourceTriangleCount} tris" }
        .orEmpty()
    val sliceDurationMs = sliceTiming?.totalMs
    return WorkspacePerformanceSnapshot(
        prepareDurationMs = prepareDurationMs,
        sliceDurationMs = sliceDurationMs,
        displayLodSummary = displayLodSummary,
        warnings = workspacePerformanceWarnings(
            prepareDurationMs = prepareDurationMs,
            sliceDurationMs = sliceDurationMs
        )
    )
}

private fun workspacePerformanceWarnings(
    prepareDurationMs: Long?,
    sliceDurationMs: Long?
): List<String> = buildList {
    if (prepareDurationMs != null && prepareDurationMs >= 15_000L) {
        add("Prepare slow")
    }
    if (sliceDurationMs != null && sliceDurationMs >= 120_000L) {
        add("Slice slow")
    }
}
