#include "Flow.hpp"

namespace Slic3r {

FlowErrorNegativeSpacing::FlowErrorNegativeSpacing()
    : FlowError("Flow::spacing() produced negative spacing. Did you set some extrusion width too small?")
{}

float Flow::rounded_rectangle_extrusion_spacing(float width, float height)
{
    auto out = width - height * float(1. - 0.25 * PI);
    if (out <= 0.f)
        throw FlowErrorNegativeSpacing();
    return out;
}

float Flow::bridge_extrusion_spacing(float dmr)
{
    return dmr + BRIDGE_EXTRA_SPACING;
}

} // namespace Slic3r
