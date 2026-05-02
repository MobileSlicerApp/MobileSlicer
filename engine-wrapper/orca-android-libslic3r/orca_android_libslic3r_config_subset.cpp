#include <cstddef>
#include <type_traits>

#include "libslic3r/PrintConfig.hpp"

namespace orca_android_libslic3r {

int config_backed_subset_probe()
{
    static_assert(std::is_default_constructible_v<Slic3r::PrintConfig>);
    static_assert(std::is_default_constructible_v<Slic3r::FullPrintConfig>);
    static_assert(std::is_default_constructible_v<Slic3r::DynamicPrintConfig>);
    static_assert(sizeof(Slic3r::PrintConfigDef) > 0);
    static_assert(sizeof(Slic3r::PrintConfig) > 0);
    static_assert(sizeof(Slic3r::FullPrintConfig) > 0);
    static_assert(sizeof(Slic3r::DynamicPrintConfig) > 0);

    Slic3r::PrintConfig print_config;
    Slic3r::FullPrintConfig full_config;
    Slic3r::DynamicPrintConfig dynamic_config;

    const auto *print_def = print_config.def();
    const auto *full_def = full_config.def();
    const auto *dynamic_def = dynamic_config.def();

    return static_cast<int>(
        (print_def != nullptr ? 1U : 0U) +
        (full_def != nullptr ? 1U : 0U) +
        (dynamic_def != nullptr ? 1U : 0U));
}

} // namespace orca_android_libslic3r
