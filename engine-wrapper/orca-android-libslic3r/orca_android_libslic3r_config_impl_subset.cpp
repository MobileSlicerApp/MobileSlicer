#include <memory>
#include <string>

#include "libslic3r/Config.hpp"
#include "libslic3r/PrintConfig.hpp"

namespace orca_android_libslic3r {

int config_impl_backed_subset_probe()
{
    const std::string escaped = Slic3r::escape_string_cstyle("alpha\nbeta");

    Slic3r::DynamicPrintConfig dynamic_config = Slic3r::DynamicPrintConfig::full_print_config();
    std::unique_ptr<Slic3r::DynamicPrintConfig> defaults_only(
        Slic3r::DynamicPrintConfig::new_from_defaults_keys({"layer_height", "perimeters"}));

    const bool has_layer_height = dynamic_config.option("layer_height") != nullptr;
    const bool has_defaults_layer_height = defaults_only && defaults_only->option("layer_height") != nullptr;

    return static_cast<int>(escaped.size()) +
        (has_layer_height ? 10 : 0) +
        (has_defaults_layer_height ? 100 : 0);
}

} // namespace orca_android_libslic3r
