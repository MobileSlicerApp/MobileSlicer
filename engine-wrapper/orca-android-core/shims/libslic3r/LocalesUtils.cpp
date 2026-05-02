#include "libslic3r/LocalesUtils.hpp"

#include <iomanip>
#include <sstream>

namespace Slic3r {

CNumericLocalesSetter::CNumericLocalesSetter()
{
    c_locale_ = newlocale(LC_NUMERIC_MASK, "C", nullptr);
    if (c_locale_ != nullptr) {
        original_locale_ = uselocale(c_locale_);
    }
}

CNumericLocalesSetter::~CNumericLocalesSetter()
{
    if (c_locale_ != nullptr) {
        uselocale(original_locale_);
        freelocale(c_locale_);
    }
}

bool is_decimal_separator_point()
{
    const auto* numeric_locale = localeconv();
    return numeric_locale != nullptr &&
           numeric_locale->decimal_point != nullptr &&
           numeric_locale->decimal_point[0] == '.';
}

std::string float_to_string_decimal_point(double value, int precision)
{
    std::ostringstream stream;
    stream.imbue(std::locale::classic());
    if (precision >= 0) {
        stream << std::fixed << std::setprecision(precision);
    }
    stream << value;
    return stream.str();
}

double string_to_double_decimal_point(std::string_view str, size_t* pos)
{
    std::string value(str);
    return std::stod(value, pos);
}

} // namespace Slic3r
