// Non-shipping Orca thumbnail renderer extraction probe.
//
// This file intentionally has no dependency on GLCanvas3D, wxWidgets,
// GUI_App, MainFrame, Plater, GLVolumeCollection, or Android EGL. It records
// the smallest role/camera contract that can be extracted from Orca's desktop
// thumbnail path and shared with MobileSlicer's Android renderer boundary.

#include <array>
#include <cmath>
#include <iostream>
#include <string_view>

namespace mobileslicer::orca_thumbnail_probe {

enum class Role {
    Gcode,
    Plate,
    NoLight,
    Top,
    Pick,
};

enum class CameraMode {
    AngledIso,
    TopPlate,
};

struct RoleContract {
    Role role;
    CameraMode camera_mode;
    bool picking;
    bool ban_light;
    float pitch_degrees;
    float yaw_degrees;
    float camera_distance_factor;
    float zoom_to_box_margin_factor;
    float broad_footprint_zoom_to_box_margin_factor;
    float box_horizontal_margin_factor;
    float box_vertical_margin_factor;
    float top_plate_margin;
};

struct Bounds {
    float min_x;
    float min_y;
    float min_z;
    float max_x;
    float max_y;
    float max_z;
};

struct Fixture {
    std::string_view id;
    Bounds bounds;
    float bed_width_mm;
    float bed_depth_mm;
    float bed_height_mm;
    int output_width;
    int output_height;
};

struct FramingMetrics {
    Bounds expanded_bounds;
    float footprint_mm;
    float height_mm;
    float zoom_margin_factor;
    float span_mm;
    float camera_distance_mm;
    float target_x;
    float target_y;
    float target_z;
    float projected_width;
    float projected_height;
    float aspect_independent_zoom;
    float angled_half_width;
    float angled_half_height;
    float top_half_width;
    float top_half_height;
};

constexpr float kAngledPitchDegrees = 45.0f;
constexpr float kAngledYawDegrees = -45.0f;
constexpr float kAngledCameraDistanceFactor = 4.0f;
constexpr float kAngledZoomToBoxMarginFactor = 1.025f;
constexpr float kAngledBroadFootprintZoomToBoxMarginFactor = 1.38f;
constexpr float kAngledBroadFootprintMinSizeMm = 40.0f;
constexpr float kAngledBroadFootprintMinHeightRatio = 0.4f;
constexpr float kAngledBroadFootprintMaxHeightRatio = 0.8f;
constexpr float kAngledBoxHorizontalMarginFactor = 0.01f;
constexpr float kAngledBoxVerticalMarginFactor = 0.02f;
constexpr float kTopPlateMargin = 1.02f;
constexpr int kSmallThumbnailSupersampleFactor = 2;
constexpr int kPackageSupersampleMaxOutputDimension = 128;
constexpr int kGcodeSupersampleMaxOutputDimension = 300;
constexpr int kSupersampleMaxRenderDimension = 600;

constexpr std::array<RoleContract, 5> kRoleContracts = {{
    {
        Role::Gcode,
        CameraMode::AngledIso,
        false,
        false,
        kAngledPitchDegrees,
        kAngledYawDegrees,
        kAngledCameraDistanceFactor,
        kAngledZoomToBoxMarginFactor,
        kAngledBroadFootprintZoomToBoxMarginFactor,
        kAngledBoxHorizontalMarginFactor,
        kAngledBoxVerticalMarginFactor,
        0.0f,
    },
    {
        Role::Plate,
        CameraMode::AngledIso,
        false,
        false,
        kAngledPitchDegrees,
        kAngledYawDegrees,
        kAngledCameraDistanceFactor,
        kAngledZoomToBoxMarginFactor,
        kAngledBroadFootprintZoomToBoxMarginFactor,
        kAngledBoxHorizontalMarginFactor,
        kAngledBoxVerticalMarginFactor,
        0.0f,
    },
    {
        Role::NoLight,
        CameraMode::AngledIso,
        false,
        true,
        kAngledPitchDegrees,
        kAngledYawDegrees,
        kAngledCameraDistanceFactor,
        kAngledZoomToBoxMarginFactor,
        kAngledBroadFootprintZoomToBoxMarginFactor,
        kAngledBoxHorizontalMarginFactor,
        kAngledBoxVerticalMarginFactor,
        0.0f,
    },
    {
        Role::Top,
        CameraMode::TopPlate,
        false,
        false,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        kTopPlateMargin,
    },
    {
        Role::Pick,
        CameraMode::TopPlate,
        true,
        true,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        0.0f,
        kTopPlateMargin,
    },
}};

constexpr std::array<Fixture, 5> kFramingFixtures = {{
    {
        "cube",
        {-10.0f, -10.0f, 0.0f, 10.0f, 10.0f, 20.0f},
        270.0f,
        270.0f,
        256.0f,
        512,
        512,
    },
    {
        "tall_box",
        {-10.0f, -10.0f, 0.0f, 10.0f, 10.0f, 120.0f},
        270.0f,
        270.0f,
        256.0f,
        512,
        512,
    },
    {
        "narrow_strip",
        {-80.0f, -2.0f, 0.0f, 80.0f, 2.0f, 4.0f},
        270.0f,
        270.0f,
        256.0f,
        512,
        512,
    },
    {
        "broad_mid_height",
        {-30.0f, -30.0f, 0.0f, 30.0f, 30.0f, 30.0f},
        270.0f,
        270.0f,
        256.0f,
        512,
        512,
    },
    {
        "h2d_two_object_layout",
        {-14.0f, -30.0f, 0.0f, 14.0f, 30.0f, 20.0f},
        350.0f,
        320.0f,
        325.0f,
        512,
        512,
    },
}};

constexpr std::string_view role_name(Role role)
{
    switch (role) {
    case Role::Gcode: return "gcode";
    case Role::Plate: return "plate";
    case Role::NoLight: return "no_light";
    case Role::Top: return "top";
    case Role::Pick: return "pick";
    }
    return "unknown";
}

constexpr std::string_view camera_mode_name(CameraMode mode)
{
    switch (mode) {
    case CameraMode::AngledIso: return "angled_iso";
    case CameraMode::TopPlate: return "top_plate";
    }
    return "unknown";
}

constexpr float max_float(float left, float right)
{
    return left > right ? left : right;
}

constexpr float min_float(float left, float right)
{
    return left < right ? left : right;
}

Bounds expand_bounds(Bounds bounds)
{
    const float width = bounds.max_x - bounds.min_x;
    const float depth = bounds.max_y - bounds.min_y;
    const float height = bounds.max_z - bounds.min_z;
    return {
        bounds.min_x - width * kAngledBoxHorizontalMarginFactor,
        bounds.min_y - depth * kAngledBoxHorizontalMarginFactor,
        bounds.min_z - height * kAngledBoxVerticalMarginFactor,
        bounds.max_x + width * kAngledBoxHorizontalMarginFactor,
        bounds.max_y + depth * kAngledBoxHorizontalMarginFactor,
        bounds.max_z + height * kAngledBoxVerticalMarginFactor,
    };
}

float angled_zoom_margin_factor(const Bounds &bounds)
{
    const float footprint = max_float(bounds.max_x - bounds.min_x, bounds.max_y - bounds.min_y);
    const float height = max_float(bounds.max_z - bounds.min_z, 0.0f);
    const bool broad_low_or_mid_height =
        footprint >= kAngledBroadFootprintMinSizeMm &&
        height >= footprint * kAngledBroadFootprintMinHeightRatio &&
        height <= footprint * kAngledBroadFootprintMaxHeightRatio;
    return broad_low_or_mid_height ? kAngledBroadFootprintZoomToBoxMarginFactor : kAngledZoomToBoxMarginFactor;
}

FramingMetrics framing_metrics(const Fixture &fixture)
{
    const Bounds expanded = expand_bounds(fixture.bounds);
    const float footprint = max_float(expanded.max_x - expanded.min_x, expanded.max_y - expanded.min_y);
    const float height = max_float(expanded.max_z - expanded.min_z, 0.0f);
    const float span = max_float(max_float(fixture.bed_width_mm, fixture.bed_depth_mm), fixture.bed_height_mm);
    const float distance = max_float(span, 100.0f) * kAngledCameraDistanceFactor;
    const float target_x = (expanded.min_x + expanded.max_x) * 0.5f;
    const float target_y = (expanded.min_y + expanded.max_y) * 0.5f;
    const float target_z = (expanded.min_z + expanded.max_z) * 0.5f;

    // This probe mirrors the Android renderer's deterministic framing choice
    // without depending on Android Matrix or EGL. For an isometric 45/-45
    // camera, the view-plane extents can be projected from the expanded box
    // using the camera's right and up vectors.
    constexpr float inv_sqrt_2 = 0.7071067811865476f;
    constexpr float inv_sqrt_3 = 0.5773502691896258f;
    constexpr float right_x = inv_sqrt_2;
    constexpr float right_y = inv_sqrt_2;
    constexpr float right_z = 0.0f;
    constexpr float up_x = -inv_sqrt_3;
    constexpr float up_y = inv_sqrt_3;
    constexpr float up_z = inv_sqrt_3;
    float min_view_x = 1.0e30f;
    float min_view_y = 1.0e30f;
    float max_view_x = -1.0e30f;
    float max_view_y = -1.0e30f;
    const std::array<float, 2> xs = {expanded.min_x, expanded.max_x};
    const std::array<float, 2> ys = {expanded.min_y, expanded.max_y};
    const std::array<float, 2> zs = {expanded.min_z, expanded.max_z};
    for (float x : xs) {
        for (float y : ys) {
            for (float z : zs) {
                const float centered_x = x - target_x;
                const float centered_y = y - target_y;
                const float centered_z = z - target_z;
                const float view_x = centered_x * right_x + centered_y * right_y + centered_z * right_z;
                const float view_y = centered_x * up_x + centered_y * up_y + centered_z * up_z;
                min_view_x = min_float(min_view_x, view_x);
                max_view_x = max_float(max_view_x, view_x);
                min_view_y = min_float(min_view_y, view_y);
                max_view_y = max_float(max_view_y, view_y);
            }
        }
    }

    const float projected_width = max_float(max_view_x - min_view_x, 1.0f);
    const float projected_height = max_float(max_view_y - min_view_y, 1.0f);
    const float zoom_margin = angled_zoom_margin_factor(expanded);
    const float zoom = max_float(
        min_float(
            static_cast<float>(fixture.output_width) / (projected_width * zoom_margin),
            static_cast<float>(fixture.output_height) / (projected_height * zoom_margin)),
        0.0001f);
    const float angled_half_width = fixture.output_width * 0.5f / zoom;
    const float angled_half_height = fixture.output_height * 0.5f / zoom;

    const float aspect_ratio = static_cast<float>(fixture.output_width) / max_float(static_cast<float>(fixture.output_height), 1.0f);
    const float bed_half_width = fixture.bed_width_mm * 0.5f;
    const float bed_half_depth = fixture.bed_depth_mm * 0.5f;
    const float top_half_height = max_float(bed_half_depth, bed_half_width / aspect_ratio) * kTopPlateMargin;
    const float top_half_width = top_half_height * aspect_ratio;

    return {
        expanded,
        footprint,
        height,
        zoom_margin,
        max_float(span, 100.0f),
        distance,
        target_x,
        target_y,
        target_z,
        projected_width,
        projected_height,
        zoom,
        angled_half_width,
        angled_half_height,
        top_half_width,
        top_half_height,
    };
}

void print_bounds(const Bounds &bounds)
{
    std::cout << "{";
    std::cout << "\"min_x\":" << bounds.min_x << ",";
    std::cout << "\"min_y\":" << bounds.min_y << ",";
    std::cout << "\"min_z\":" << bounds.min_z << ",";
    std::cout << "\"max_x\":" << bounds.max_x << ",";
    std::cout << "\"max_y\":" << bounds.max_y << ",";
    std::cout << "\"max_z\":" << bounds.max_z;
    std::cout << "}";
}

} // namespace mobileslicer::orca_thumbnail_probe

#ifdef MOBILE_SLICER_ORCA_THUMBNAIL_CONTRACT_PROBE_MAIN
int main()
{
    using namespace mobileslicer::orca_thumbnail_probe;
    std::cout << "{\n";
    std::cout << "  \"schema_version\": 1,\n";
    std::cout << "  \"source\": \"vendor/orcaslicer/src/slic3r/GUI/GLCanvas3D.cpp\",\n";
    std::cout << "  \"roles\": [\n";
    for (std::size_t index = 0; index < kRoleContracts.size(); ++index) {
        const RoleContract &contract = kRoleContracts[index];
        std::cout << "    {\n";
        std::cout << "      \"role\": \"" << role_name(contract.role) << "\",\n";
        std::cout << "      \"camera_mode\": \"" << camera_mode_name(contract.camera_mode) << "\",\n";
        std::cout << "      \"picking\": " << (contract.picking ? "true" : "false") << ",\n";
        std::cout << "      \"ban_light\": " << (contract.ban_light ? "true" : "false") << ",\n";
        std::cout << "      \"pitch_degrees\": " << contract.pitch_degrees << ",\n";
        std::cout << "      \"yaw_degrees\": " << contract.yaw_degrees << ",\n";
        std::cout << "      \"camera_distance_factor\": " << contract.camera_distance_factor << ",\n";
        std::cout << "      \"zoom_to_box_margin_factor\": " << contract.zoom_to_box_margin_factor << ",\n";
        std::cout << "      \"broad_footprint_zoom_to_box_margin_factor\": " << contract.broad_footprint_zoom_to_box_margin_factor << ",\n";
        std::cout << "      \"box_horizontal_margin_factor\": " << contract.box_horizontal_margin_factor << ",\n";
        std::cout << "      \"box_vertical_margin_factor\": " << contract.box_vertical_margin_factor << ",\n";
        std::cout << "      \"top_plate_margin\": " << contract.top_plate_margin << "\n";
        std::cout << "    }" << (index + 1 == kRoleContracts.size() ? "\n" : ",\n");
    }
    std::cout << "  ],\n";
    std::cout << "  \"supersampling\": {\n";
    std::cout << "    \"small_thumbnail_supersample_factor\": " << kSmallThumbnailSupersampleFactor << ",\n";
    std::cout << "    \"package_supersample_max_output_dimension\": " << kPackageSupersampleMaxOutputDimension << ",\n";
    std::cout << "    \"gcode_supersample_max_output_dimension\": " << kGcodeSupersampleMaxOutputDimension << ",\n";
    std::cout << "    \"supersample_max_render_dimension\": " << kSupersampleMaxRenderDimension << "\n";
    std::cout << "  },\n";
    std::cout << "  \"framing_fixtures\": [\n";
    for (std::size_t index = 0; index < kFramingFixtures.size(); ++index) {
        const Fixture &fixture = kFramingFixtures[index];
        const FramingMetrics metrics = framing_metrics(fixture);
        std::cout << "    {\n";
        std::cout << "      \"id\": \"" << fixture.id << "\",\n";
        std::cout << "      \"input_bounds\": ";
        print_bounds(fixture.bounds);
        std::cout << ",\n";
        std::cout << "      \"expanded_bounds\": ";
        print_bounds(metrics.expanded_bounds);
        std::cout << ",\n";
        std::cout << "      \"bed_width_mm\": " << fixture.bed_width_mm << ",\n";
        std::cout << "      \"bed_depth_mm\": " << fixture.bed_depth_mm << ",\n";
        std::cout << "      \"bed_height_mm\": " << fixture.bed_height_mm << ",\n";
        std::cout << "      \"output_width\": " << fixture.output_width << ",\n";
        std::cout << "      \"output_height\": " << fixture.output_height << ",\n";
        std::cout << "      \"footprint_mm\": " << metrics.footprint_mm << ",\n";
        std::cout << "      \"height_mm\": " << metrics.height_mm << ",\n";
        std::cout << "      \"zoom_margin_factor\": " << metrics.zoom_margin_factor << ",\n";
        std::cout << "      \"span_mm\": " << metrics.span_mm << ",\n";
        std::cout << "      \"camera_distance_mm\": " << metrics.camera_distance_mm << ",\n";
        std::cout << "      \"target_x\": " << metrics.target_x << ",\n";
        std::cout << "      \"target_y\": " << metrics.target_y << ",\n";
        std::cout << "      \"target_z\": " << metrics.target_z << ",\n";
        std::cout << "      \"projected_width\": " << metrics.projected_width << ",\n";
        std::cout << "      \"projected_height\": " << metrics.projected_height << ",\n";
        std::cout << "      \"aspect_independent_zoom\": " << metrics.aspect_independent_zoom << ",\n";
        std::cout << "      \"angled_half_width\": " << metrics.angled_half_width << ",\n";
        std::cout << "      \"angled_half_height\": " << metrics.angled_half_height << ",\n";
        std::cout << "      \"top_half_width\": " << metrics.top_half_width << ",\n";
        std::cout << "      \"top_half_height\": " << metrics.top_half_height << "\n";
        std::cout << "    }" << (index + 1 == kFramingFixtures.size() ? "\n" : ",\n");
    }
    std::cout << "  ]\n";
    std::cout << "}\n";
    return 0;
}
#endif
