#include <memory>

#include "libslic3r/PrintConfig.hpp"

namespace orca_android_libslic3r {

int printconfig_impl_backed_subset_probe()
{
    Slic3r::DynamicPrintConfig dynamic_config = Slic3r::DynamicPrintConfig::full_print_config();
    std::unique_ptr<Slic3r::DynamicPrintConfig> defaults_only(
        Slic3r::DynamicPrintConfig::new_from_defaults_keys({"layer_height", "perimeters"}));

    const bool has_layer_height = dynamic_config.option("layer_height") != nullptr;
    const bool has_defaults_perimeters = defaults_only && defaults_only->option("perimeters") != nullptr;

    return (has_layer_height ? 1 : 0) + (has_defaults_perimeters ? 10 : 0);
}

} // namespace orca_android_libslic3r
