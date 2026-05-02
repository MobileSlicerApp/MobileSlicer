#include "orca_wrapper_printable_validation.h"

#include "libslic3r/Config.hpp"
#include "libslic3r/PrintConfig.hpp"

#include <algorithm>
#include <fstream>
#include <optional>
#include <sstream>

namespace mobileslicer::orca_wrapper {
namespace {

std::optional<double> parse_axis_value(const std::string& token)
{
    if (token.size() < 2) {
        return std::nullopt;
    }
    try {
        return std::stod(token.substr(1));
    } catch (...) {
        return std::nullopt;
    }
}

} // namespace

PrintableVolumeBounds extract_printable_volume_bounds(const Slic3r::DynamicPrintConfig& config)
{
    PrintableVolumeBounds bounds;
    const auto* printable_area = config.option<Slic3r::ConfigOptionPoints>("printable_area");
    if (printable_area == nullptr || printable_area->values.empty()) {
        return bounds;
    }

    double min_x = printable_area->values.front().x();
    double max_x = printable_area->values.front().x();
    double min_y = printable_area->values.front().y();
    double max_y = printable_area->values.front().y();
    for (const auto& point : printable_area->values) {
        min_x = std::min(min_x, point.x());
        max_x = std::max(max_x, point.x());
        min_y = std::min(min_y, point.y());
        max_y = std::max(max_y, point.y());
    }

    bounds.min_x = min_x;
    bounds.max_x = max_x;
    bounds.min_y = min_y;
    bounds.max_y = max_y;
    bounds.max_z = config.opt_float("printable_height");
    bounds.valid = true;
    return bounds;
}

GcodePrintableVolumeViolation detect_printable_volume_violation(
    const std::filesystem::path& gcode_path,
    const PrintableVolumeBounds& bounds)
{
    GcodePrintableVolumeViolation violation;
    if (!bounds.valid) {
        return violation;
    }

    std::ifstream input(gcode_path);
    if (!input.is_open()) {
        return violation;
    }

    constexpr double xy_tolerance_mm = 5.0;
    constexpr double z_tolerance_mm = 1e-6;
    double current_x = 0.0;
    double current_y = 0.0;
    double current_z = 0.0;
    double current_e = 0.0;
    bool relative_xyz = false;
    bool relative_extrusion = false;
    bool have_extrusion_position = false;
    size_t line_number = 0;
    std::string line;

    while (std::getline(input, line)) {
        ++line_number;
        const auto comment_start = line.find(';');
        const std::string code = line.substr(0, comment_start);
        std::istringstream tokens(code);
        std::string command;
        tokens >> command;
        if (command.empty()) {
            continue;
        }

        if (command == "G90" || command == "g90") {
            relative_xyz = false;
            continue;
        }
        if (command == "G91" || command == "g91") {
            relative_xyz = true;
            continue;
        }
        if (command == "M82" || command == "m82") {
            relative_extrusion = false;
            continue;
        }
        if (command == "M83" || command == "m83") {
            relative_extrusion = true;
            continue;
        }
        if (command == "G92" || command == "g92") {
            std::string token;
            while (tokens >> token) {
                if (token.empty()) {
                    continue;
                }
                if (token[0] == 'E' || token[0] == 'e') {
                    if (const auto value = parse_axis_value(token)) {
                        current_e = *value;
                        have_extrusion_position = true;
                    }
                }
            }
            continue;
        }
        if (command != "G0" && command != "g0" && command != "G1" && command != "g1") {
            continue;
        }

        double next_x = current_x;
        double next_y = current_y;
        double next_z = current_z;
        double next_e = current_e;
        bool has_e = false;
        bool has_spatial_move = false;
        bool extruding = false;
        std::string token;
        while (tokens >> token) {
            if (token.empty()) {
                continue;
            }
            const char axis = token[0];
            const auto value = parse_axis_value(token);
            if (!value.has_value()) {
                continue;
            }
            switch (axis) {
            case 'X':
            case 'x':
                next_x = relative_xyz ? current_x + *value : *value;
                has_spatial_move = true;
                break;
            case 'Y':
            case 'y':
                next_y = relative_xyz ? current_y + *value : *value;
                has_spatial_move = true;
                break;
            case 'Z':
            case 'z':
                next_z = relative_xyz ? current_z + *value : *value;
                has_spatial_move = true;
                break;
            case 'E':
            case 'e': {
                has_e = true;
                if (relative_extrusion) {
                    extruding = *value > 0.0;
                    next_e = current_e + *value;
                } else {
                    extruding = !have_extrusion_position || *value > current_e;
                    next_e = *value;
                }
                break;
            }
            default:
                break;
            }
        }

        current_x = next_x;
        current_y = next_y;
        current_z = next_z;
        if (has_e) {
            current_e = next_e;
            have_extrusion_position = true;
        }

        if (!extruding || !has_spatial_move) {
            continue;
        }

        if (!violation.has_extrusion) {
            violation.min_x = current_x;
            violation.max_x = current_x;
            violation.min_y = current_y;
            violation.max_y = current_y;
            violation.max_z = current_z;
            violation.has_extrusion = true;
        } else {
            violation.min_x = std::min(violation.min_x, current_x);
            violation.max_x = std::max(violation.max_x, current_x);
            violation.min_y = std::min(violation.min_y, current_y);
            violation.max_y = std::max(violation.max_y, current_y);
            violation.max_z = std::max(violation.max_z, current_z);
        }

        const bool outside_xy =
            current_x < bounds.min_x - xy_tolerance_mm ||
            current_x > bounds.max_x + xy_tolerance_mm ||
            current_y < bounds.min_y - xy_tolerance_mm ||
            current_y > bounds.max_y + xy_tolerance_mm;
        const bool outside_z = current_z > bounds.max_z + z_tolerance_mm;
        if (!outside_xy && !outside_z) {
            continue;
        }

        violation.printable_area_exceeded |= outside_xy;
        violation.printable_height_exceeded |= outside_z;
        if (violation.offending_line == 0) {
            violation.offending_line = line_number;
            violation.offending_gcode = line;
        }
    }

    return violation;
}

} // namespace mobileslicer::orca_wrapper
