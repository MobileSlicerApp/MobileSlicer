package com.mobileslicer.profiles

internal fun derivedFirstLayerPrintSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.15f)

internal fun derivedFirstLayerInfillSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.375f)

internal const val DEFAULT_FIRST_LAYER_TRAVEL_SPEED_PERCENT = 50
internal const val DEFAULT_SLOW_DOWN_LAYERS = 0
internal const val DEFAULT_INITIAL_LAYER_ACCELERATION_MM_PER_SEC2 = 300f
internal const val DEFAULT_INITIAL_LAYER_JERK_MM_PER_SEC = 9f
internal const val DEFAULT_FIRST_LAYER_FLOW_RATIO = 1f

internal fun derivedOuterWallSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.5f)

internal fun derivedInnerWallSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.5f)

internal fun derivedTopSurfaceSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.5f)

internal fun derivedTravelSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(120f, printSpeedMmPerSec * 0.6f)

internal const val DEFAULT_OUTER_WALL_ACCELERATION_MM_PER_SEC2 = 500f
internal const val DEFAULT_INNER_WALL_ACCELERATION_MM_PER_SEC2 = 10000f
internal const val DEFAULT_TOP_SURFACE_ACCELERATION_MM_PER_SEC2 = 500f
internal const val DEFAULT_SPARSE_INFILL_ACCELERATION_MM_PER_SEC2 = 500f
internal const val DEFAULT_DEFAULT_ACCELERATION_MM_PER_SEC2 = 500f
internal const val DEFAULT_INNER_WALL_JERK_MM_PER_SEC = 9f
internal const val DEFAULT_INNER_WALL_FLOW_RATIO = 1f
internal const val DEFAULT_OUTER_WALL_JERK_MM_PER_SEC = 9f
internal const val DEFAULT_OUTER_WALL_FLOW_RATIO = 1f
internal const val DEFAULT_TOP_SOLID_INFILL_FLOW_RATIO = 1f
internal const val DEFAULT_BOTTOM_SOLID_INFILL_FLOW_RATIO = 1f
internal const val DEFAULT_OVERHANG_1_4_SPEED = "0"
internal const val DEFAULT_OVERHANG_2_4_SPEED = "0"
internal const val DEFAULT_OVERHANG_3_4_SPEED = "0"
internal const val DEFAULT_OVERHANG_4_4_SPEED = "0"
internal const val DEFAULT_OVERHANG_FLOW_RATIO = 1f
internal const val DEFAULT_DONT_SLOW_DOWN_OUTER_WALL = false
internal const val DEFAULT_ENABLE_PRIME_TOWER = true
internal const val DEFAULT_PRIME_TOWER_WIDTH_MM = 60f
internal const val DEFAULT_STANDBY_TEMPERATURE_DELTA_C = -5
internal const val DEFAULT_WIPE_TOWER_NO_SPARSE_LAYERS = false
internal const val DEFAULT_ENABLE_SUPPORT = false
internal const val DEFAULT_SUPPORT_THRESHOLD_ANGLE_DEGREES = 30
internal const val DEFAULT_SUPPORT_BUILDPLATE_ONLY = false
internal const val DEFAULT_SUPPORT_TOP_Z_DISTANCE_MM = 0.2f
internal const val DEFAULT_SUPPORT_BOTTOM_Z_DISTANCE_MM = 0.2f
internal const val DEFAULT_SUPPORT_INTERFACE_TOP_LAYERS = 2
internal const val DEFAULT_SUPPORT_INTERFACE_BOTTOM_LAYERS = 2
internal const val DEFAULT_SUPPORT_INTERFACE_SPACING_MM = 0.5f
internal const val DEFAULT_SUPPORT_BOTTOM_INTERFACE_SPACING_MM = 0.5f
internal const val DEFAULT_SUPPORT_INTERFACE_SPEED_MM_PER_SEC = 80f
internal const val DEFAULT_SUPPORT_INTERFACE_FLOW_RATIO = 1f
internal const val DEFAULT_SUPPORT_MATERIAL_INTERFACE_FAN_SPEED = "-1"
internal val DEFAULT_SUPPORT_INTERFACE_PATTERN = SupportInterfacePattern.Auto
internal const val DEFAULT_SUPPORT_INTERFACE_LOOP_PATTERN = false
internal const val DEFAULT_SUPPORT_LINE_WIDTH = "0"
internal val DEFAULT_SUPPORT_BASE_PATTERN = SupportBasePattern.Default
internal const val DEFAULT_SUPPORT_BASE_PATTERN_SPACING_MM = 2.5f
internal const val DEFAULT_SUPPORT_SPEED_MM_PER_SEC = 150f
internal const val DEFAULT_SUPPORT_FLOW_RATIO = 1f
internal const val DEFAULT_SUPPORT_OBJECT_ELEVATION_MM = 5f
internal const val DEFAULT_SUPPORT_MAX_BRIDGE_LENGTH_MM = 0f
internal const val DEFAULT_SUPPORT_IRONING = false
internal const val DEFAULT_SUPPORT_IRONING_FLOW_PERCENT = 10f
internal const val DEFAULT_SUPPORT_IRONING_SPACING_MM = 0.1f
internal const val DEFAULT_SUPPORT_EXPANSION_MM = 0f
internal const val DEFAULT_SUPPORT_OBJECT_XY_DISTANCE_MM = 0.35f

internal fun derivedBridgeSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.125f)

internal fun derivedSmallPerimeterSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(1f, derivedOuterWallSpeedMmPerSec(printSpeedMmPerSec) * 0.5f)

internal fun derivedSparseInfillSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.5f)

internal fun derivedInternalSolidInfillSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.5f)

internal fun derivedGapInfillSpeedMmPerSec(printSpeedMmPerSec: Int): Float =
    kotlin.math.max(10f, printSpeedMmPerSec * 0.25f)

internal const val DEFAULT_SMALL_PERIMETER_THRESHOLD_MM = 0f
