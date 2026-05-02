#include <fstream>
#include <iostream>
#include <map>
#include <memory>
#include <sstream>
#include <string>
#include <utility>
#include <vector>

#include "nlohmann/json.hpp"

#include "libslic3r/Config.hpp"
#include "libslic3r/PrintConfig.hpp"

namespace {

using json = nlohmann::json;

Slic3r::DynamicPrintConfig make_probe_config()
{
    static const std::vector<std::string> kProbeKeys = {
        "layer_height",
        "initial_layer_print_height",
        "nozzle_diameter",
        "filament_diameter",
        "hot_plate_temp",
        "hot_plate_temp_initial_layer",
        "nozzle_temperature",
        "nozzle_temperature_initial_layer",
        "travel_speed",
        "wall_loops",
    };

    std::unique_ptr<Slic3r::DynamicPrintConfig> config(
        Slic3r::DynamicPrintConfig::new_from_defaults_keys(kProbeKeys));
    return *config;
}

struct TranslationResult {
    bool ok { false };
    Slic3r::DynamicPrintConfig config = make_probe_config();
    std::vector<std::string> errors;
};

std::string read_file_to_string(const char *path)
{
    std::ifstream input(path, std::ios::binary);
    std::ostringstream buffer;
    buffer << input.rdbuf();
    return buffer.str();
}

void append_error(TranslationResult &result, std::string message)
{
    result.errors.push_back(std::move(message));
}

bool require_numeric(const json &root, const char *key, TranslationResult &result, double &out_value)
{
    auto it = root.find(key);
    if (it == root.end()) {
        append_error(result, std::string("missing required key: ") + key);
        return false;
    }
    if (!it->is_number()) {
        append_error(result, std::string("key must be numeric: ") + key);
        return false;
    }
    out_value = it->get<double>();
    return true;
}

bool optional_numeric(const json &root, const char *key, TranslationResult &result, double &out_value)
{
    auto it = root.find(key);
    if (it == root.end()) {
        return false;
    }
    if (!it->is_number()) {
        append_error(result, std::string("key must be numeric: ") + key);
        return false;
    }
    out_value = it->get<double>();
    return true;
}

void set_scalar_option(Slic3r::DynamicPrintConfig &config, const char *key, double value)
{
    Slic3r::ConfigSubstitutionContext ctxt { Slic3r::ForwardCompatibilitySubstitutionRule::Disable };
    config.set_deserialize(key, std::to_string(value), ctxt);
}

void set_int_option(Slic3r::DynamicPrintConfig &config, const char *key, int value)
{
    Slic3r::ConfigSubstitutionContext ctxt { Slic3r::ForwardCompatibilitySubstitutionRule::Disable };
    config.set_deserialize(key, std::to_string(value), ctxt);
}

TranslationResult translate_wrapper_json_to_config(const std::string &body)
{
    TranslationResult result;

    json root = json::parse(body, nullptr, false, true);
    if (root.is_discarded()) {
        append_error(result, "invalid JSON payload");
        return result;
    }
    if (!root.is_object()) {
        append_error(result, "top-level JSON value must be an object");
        return result;
    }

    double layer_height = 0.0;
    double first_layer_height = 0.0;
    double nozzle_diameter = 0.0;
    double filament_diameter = 0.0;
    double bed_temperature = 0.0;
    double print_temperature = 0.0;

    require_numeric(root, "layer_height", result, layer_height);
    require_numeric(root, "first_layer_height", result, first_layer_height);
    require_numeric(root, "nozzle_diameter", result, nozzle_diameter);
    require_numeric(root, "filament_diameter", result, filament_diameter);
    require_numeric(root, "bed_temperature", result, bed_temperature);
    require_numeric(root, "print_temperature", result, print_temperature);

    double travel_speed = 0.0;
    double perimeters = 0.0;
    const bool has_travel_speed = optional_numeric(root, "travel_speed", result, travel_speed);
    const bool has_perimeters = optional_numeric(root, "perimeters", result, perimeters);

    if (!result.errors.empty()) {
        return result;
    }

    set_scalar_option(result.config, "layer_height", layer_height);
    set_scalar_option(result.config, "initial_layer_print_height", first_layer_height);
    set_scalar_option(result.config, "nozzle_diameter", nozzle_diameter);
    set_scalar_option(result.config, "filament_diameter", filament_diameter);
    set_int_option(result.config, "hot_plate_temp", static_cast<int>(bed_temperature));
    set_int_option(result.config, "hot_plate_temp_initial_layer", static_cast<int>(bed_temperature));
    set_int_option(result.config, "nozzle_temperature", static_cast<int>(print_temperature));
    set_int_option(result.config, "nozzle_temperature_initial_layer", static_cast<int>(print_temperature));

    if (has_travel_speed) {
        set_scalar_option(result.config, "travel_speed", travel_speed);
    }
    if (has_perimeters) {
        set_int_option(result.config, "wall_loops", static_cast<int>(perimeters));
    }

    const std::map<std::string, std::string> validation_errors = result.config.validate(false);
    for (const auto &entry : validation_errors) {
        append_error(result, entry.first + ": " + entry.second);
    }

    if (result.errors.empty()) {
        result.ok = true;
    }
    return result;
}

void print_errors(const std::vector<std::string> &errors)
{
    for (const std::string &error : errors) {
        std::cerr << "error: " << error << "\n";
    }
}

bool print_option(const Slic3r::DynamicPrintConfig &config, const char *key)
{
    const Slic3r::ConfigOption *option = config.option(key);
    if (option == nullptr) {
        std::cerr << "error: missing translated config option: " << key << "\n";
        return false;
    }

    std::cout << key << "=" << option->serialize() << "\n";
    return true;
}

} // namespace

int main(int argc, char **argv)
{
    if (argc != 2) {
        std::cerr << "usage: orca_android_libslic3r_config_json_probe <config.json>\n";
        return 2;
    }

    const std::string body = read_file_to_string(argv[1]);
    if (body.empty()) {
        std::cerr << "error: failed to read config JSON from " << argv[1] << "\n";
        return 1;
    }

    TranslationResult result;
    try {
        result = translate_wrapper_json_to_config(body);
    } catch (const std::exception &error) {
        std::cerr << "error: exception while translating config JSON: " << error.what() << "\n";
        return 1;
    }

    if (!result.ok) {
        print_errors(result.errors);
        return 1;
    }

    bool readback_ok = true;
    readback_ok &= print_option(result.config, "layer_height");
    readback_ok &= print_option(result.config, "initial_layer_print_height");
    readback_ok &= print_option(result.config, "nozzle_diameter");
    readback_ok &= print_option(result.config, "filament_diameter");
    readback_ok &= print_option(result.config, "hot_plate_temp");
    readback_ok &= print_option(result.config, "hot_plate_temp_initial_layer");
    readback_ok &= print_option(result.config, "nozzle_temperature");
    readback_ok &= print_option(result.config, "nozzle_temperature_initial_layer");
    readback_ok &= print_option(result.config, "travel_speed");
    readback_ok &= print_option(result.config, "wall_loops");
    return readback_ok ? 0 : 1;
}
