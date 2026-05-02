#pragma once

#include "libslic3r/Model.hpp"

#include <string>

namespace mobileslicer::orca_wrapper {

void apply_model_object_overrides(const std::string& json, Slic3r::Model& model);

} // namespace mobileslicer::orca_wrapper
