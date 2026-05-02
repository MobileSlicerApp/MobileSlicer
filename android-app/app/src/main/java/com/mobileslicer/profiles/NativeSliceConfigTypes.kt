package com.mobileslicer.profiles

internal data class NativeSliceConfigCacheKey(
    val printerId: String,
    val filamentId: String,
    val processId: String,
    val printerConfigSignature: Int,
    val filamentConfigSignature: Int,
    val processConfigSignature: Int,
    val assetBackedResolve: Boolean = false
)

internal object NativeSliceConfigCache {
    private const val MaxEntries = 8
    private val configs = object : LinkedHashMap<NativeSliceConfigCacheKey, String>(MaxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<NativeSliceConfigCacheKey, String>?): Boolean =
            size > MaxEntries
    }

    @Synchronized
    fun get(key: NativeSliceConfigCacheKey): String? = configs[key]

    @Synchronized
    fun put(key: NativeSliceConfigCacheKey, value: String) {
        configs[key] = value
    }

    @Synchronized
    fun clear() {
        configs.clear()
    }
}

internal data class NativeSliceConfigTiming(
    val keyMs: Long = 0,
    val profileJsonMs: Long = 0,
    val mergeMs: Long = 0,
    val nativeDefaultsMs: Long = 0,
    val normalizeMs: Long = 0,
    val totalMs: Long = 0
)

internal data class NativeSliceConfigBuildResult(
    val json: String,
    val cacheHit: Boolean,
    val timing: NativeSliceConfigTiming
)

internal data class NativeSliceConfigRepairResult(
    val json: String,
    val cacheHit: Boolean,
    val elapsedMs: Long
)
