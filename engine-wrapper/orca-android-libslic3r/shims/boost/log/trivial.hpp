#ifndef ORCA_ANDROID_LIBSLIC3R_BOOST_LOG_TRIVIAL_HPP
#define ORCA_ANDROID_LIBSLIC3R_BOOST_LOG_TRIVIAL_HPP

#include <iosfwd>

namespace boost::log::trivial {

enum severity_level {
    trace,
    debug,
    info,
    warning,
    error,
    fatal
};

class null_stream {
public:
    template <typename T>
    null_stream& operator<<(const T &)
    {
        return *this;
    }

    null_stream& operator<<(std::ostream& (*)(std::ostream&))
    {
        return *this;
    }

    null_stream& operator<<(std::ios& (*)(std::ios&))
    {
        return *this;
    }

    null_stream& operator<<(std::ios_base& (*)(std::ios_base&))
    {
        return *this;
    }
};

inline null_stream log_sink()
{
    return {};
}

} // namespace boost::log::trivial

#define BOOST_LOG_TRIVIAL(level) ::boost::log::trivial::log_sink()

#endif
