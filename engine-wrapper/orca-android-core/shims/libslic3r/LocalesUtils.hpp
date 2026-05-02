#ifndef ORCA_ANDROID_CORE_LIBSLIC3R_LOCALES_UTILS_HPP
#define ORCA_ANDROID_CORE_LIBSLIC3R_LOCALES_UTILS_HPP

#ifndef slic3r_LocalesUtils_hpp_
#define slic3r_LocalesUtils_hpp_

#include <string>
#include <string_view>

#include <clocale>

namespace Slic3r {

class CNumericLocalesSetter {
public:
    CNumericLocalesSetter();
    ~CNumericLocalesSetter();

private:
    locale_t original_locale_ = nullptr;
    locale_t c_locale_ = nullptr;
};

bool is_decimal_separator_point();
std::string float_to_string_decimal_point(double value, int precision = -1);
double string_to_double_decimal_point(std::string_view str, size_t* pos = nullptr);

} // namespace Slic3r

#endif // slic3r_LocalesUtils_hpp_

#endif
