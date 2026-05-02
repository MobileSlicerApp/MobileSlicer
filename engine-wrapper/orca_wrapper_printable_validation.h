#pragma once

#include "libslic3r/PrintConfig.hpp"

#include <cstddef>
#include <filesystem>
#include <string>

namespace mobileslicer::orca_wrapper {

struct PrintableVolumeBounds {
    double min_x = 0.0;
    double max_x = 0.0;
    double min_y = 0.0;
    double max_y = 0.0;
    double max_z = 0.0;
    bool valid = false;
};

struct GcodePrintableVolumeViolation {
    bool printable_area_exceeded = false;
    bool printable_height_exceeded = false;
    bool has_extrusion = false;
    double min_x = 0.0;
    double max_x = 0.0;
    double min_y = 0.0;
    double max_y = 0.0;
    double max_z = 0.0;
    size_t offending_line = 0;
    std::string offending_gcode;

    [[nodiscard]] bool any() const
    {
        return printable_area_exceeded || printable_height_exceeded;
    }
};

PrintableVolumeBounds extract_printable_volume_bounds(const Slic3r::DynamicPrintConfig& config);

GcodePrintableVolumeViolation detect_printable_volume_violation(
    const std::filesystem::path& gcode_path,
    const PrintableVolumeBounds& bounds);

} // namespace mobileslicer::orca_wrapper
