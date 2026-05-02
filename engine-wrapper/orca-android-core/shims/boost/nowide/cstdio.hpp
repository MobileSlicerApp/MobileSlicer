#ifndef ORCA_ANDROID_CORE_BOOST_NOWIDE_CSTDIO_HPP
#define ORCA_ANDROID_CORE_BOOST_NOWIDE_CSTDIO_HPP

#include <cstdio>

namespace boost::nowide {

inline std::FILE* fopen(const char* filename, const char* mode)
{
    return std::fopen(filename, mode);
}

inline std::FILE* freopen(const char* filename, const char* mode, std::FILE* stream)
{
    return std::freopen(filename, mode, stream);
}

} // namespace boost::nowide

#endif
