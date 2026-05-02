#include "orca_wrapper_model_overrides.h"

#include "orca_wrapper_utils.h"

#include "libslic3r/Config.hpp"

#include <filesystem>

namespace mobileslicer::orca_wrapper {

void apply_model_object_overrides(const std::string& json, Slic3r::Model& model)
{
    if (json.empty()) {
        return;
    }

    const bool flow_rate_calibration =
        extract_bool(json, "mobile_slicer_calibration_active").value_or(false) &&
        extract_string(json, "mobile_slicer_calibration_type").value_or("") == "FlowRate";
    Slic3r::ConfigSubstitutionContext ctxt { Slic3r::ForwardCompatibilitySubstitutionRule::Disable };
    for (Slic3r::ModelObject* object : model.objects) {
        if (object == nullptr) {
            continue;
        }

        // PrintApply rebuilds object config from the default object config and then
        // reapplies ModelObject::config on top. Mirror object-owning overrides here so
        // default object-level values do not overwrite the wrapper config before export.
        if (const auto seam_position = extract_string(json, "seam_position")) {
            object->config.set_deserialize("seam_position", *seam_position, ctxt);
        }
        if (const auto value = extract_bool(json, "enable_support")) {
            object->config.set_deserialize("enable_support", *value ? "1" : "0", ctxt);
        }
        if (const auto value = extract_string(json, "support_type")) {
            object->config.set_deserialize("support_type", *value, ctxt);
        }
        if (const auto value = extract_string(json, "support_style")) {
            object->config.set_deserialize("support_style", *value, ctxt);
        }
        if (const auto value = extract_number(json, "support_threshold_angle")) {
            object->config.set_deserialize("support_threshold_angle", std::to_string(static_cast<int>(*value)), ctxt);
        }
        if (const auto value = extract_number(json, "raft_layers")) {
            object->config.set_deserialize("raft_layers", std::to_string(static_cast<int>(*value)), ctxt);
        }
        if (const auto value = extract_bool(json, "support_on_build_plate_only")) {
            object->config.set_deserialize("support_on_build_plate_only", *value ? "1" : "0", ctxt);
        } else if (const auto legacy_value = extract_bool(json, "support_buildplate_only")) {
            object->config.set_deserialize("support_on_build_plate_only", *legacy_value ? "1" : "0", ctxt);
        }
        if (const auto value = extract_number(json, "support_top_z_distance")) {
            object->config.set_deserialize("support_top_z_distance", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_bottom_z_distance")) {
            object->config.set_deserialize("support_bottom_z_distance", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_interface_top_layers")) {
            object->config.set_deserialize("support_interface_top_layers", std::to_string(static_cast<int>(*value)), ctxt);
        }
        if (const auto value = extract_number(json, "support_interface_bottom_layers")) {
            object->config.set_deserialize("support_interface_bottom_layers", std::to_string(static_cast<int>(*value)), ctxt);
        }
        if (const auto value = extract_number(json, "support_interface_spacing")) {
            object->config.set_deserialize("support_interface_spacing", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_bottom_interface_spacing")) {
            object->config.set_deserialize("support_bottom_interface_spacing", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_string(json, "support_interface_pattern")) {
            object->config.set_deserialize("support_interface_pattern", *value, ctxt);
        }
        if (const auto value = extract_bool(json, "support_interface_loop_pattern")) {
            object->config.set_deserialize("support_interface_loop_pattern", *value ? "1" : "0", ctxt);
        }
        if (const auto value = extract_string(json, "support_line_width")) {
            object->config.set_deserialize("support_line_width", *value, ctxt);
        }
        if (const auto value = extract_string(json, "support_base_pattern")) {
            object->config.set_deserialize("support_base_pattern", *value, ctxt);
        }
        if (const auto value = extract_number(json, "support_base_pattern_spacing")) {
            object->config.set_deserialize("support_base_pattern_spacing", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_speed")) {
            object->config.set_deserialize("support_speed", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_bool(json, "support_ironing")) {
            object->config.set_deserialize("support_ironing", *value ? "1" : "0", ctxt);
        }
        if (const auto value = extract_number(json, "support_ironing_flow")) {
            object->config.set_deserialize("support_ironing_flow", std::to_string(*value) + "%", ctxt);
        }
        if (const auto value = extract_number(json, "support_ironing_spacing")) {
            object->config.set_deserialize("support_ironing_spacing", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_expansion")) {
            object->config.set_deserialize("support_expansion", std::to_string(*value), ctxt);
        }
        if (const auto value = extract_number(json, "support_object_xy_distance")) {
            object->config.set_deserialize("support_object_xy_distance", std::to_string(*value), ctxt);
        }
        if (flow_rate_calibration) {
            std::string object_name = object->name;
            if (object_name.find("flowrate_") == std::string::npos && !object->input_file.empty()) {
                object_name = std::filesystem::path(object->input_file).stem().string();
            }
            const auto marker = object_name.find("flowrate_");
            if (marker != std::string::npos) {
                std::string modifier_text = object_name.substr(marker + std::string("flowrate_").size());
                const auto extension_marker = modifier_text.find('.');
                if (extension_marker != std::string::npos) {
                    modifier_text = modifier_text.substr(0, extension_marker);
                }
                const auto separator_marker = modifier_text.find_first_of("-_ ");
                if (separator_marker != std::string::npos) {
                    modifier_text = modifier_text.substr(0, separator_marker);
                }
                if (!modifier_text.empty() && modifier_text[0] == 'm') {
                    modifier_text[0] = '-';
                }
                try {
                    const double modifier = std::stod(modifier_text);
                    const double print_flow_ratio = 1.0 + modifier / 100.0;
                    object->config.set_deserialize("print_flow_ratio", std::to_string(print_flow_ratio), ctxt);
                } catch (...) {
                }
            }
        }
    }
}

} // namespace mobileslicer::orca_wrapper
