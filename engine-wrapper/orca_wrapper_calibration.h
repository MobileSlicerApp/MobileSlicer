#pragma once

#include "libslic3r/calib.hpp"

#include <string>

namespace mobileslicer::orca_wrapper {

Slic3r::Calib_Params extract_calibration_params(const std::string& json);

} // namespace mobileslicer::orca_wrapper
