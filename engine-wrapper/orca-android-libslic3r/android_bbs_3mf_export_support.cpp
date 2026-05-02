#include "libslic3r/NSVGUtils.hpp"
#include "libslic3r/Utils.hpp"

#include <boost/filesystem/path.hpp>

#include <cstring>
#include <fstream>
#include <memory>
#include <sstream>
#include <string>

namespace Slic3r {

std::string encode_path(const char* path)
{
    return path == nullptr ? std::string() : std::string(path);
}

std::string decode_path(const char* path)
{
    return path == nullptr ? std::string() : std::string(path);
}

std::string xml_escape(std::string text, bool is_marked)
{
    std::string::size_type pos = 0;
    for (;;) {
        pos = text.find_first_of("\"'&<>", pos);
        if (pos == std::string::npos) {
            break;
        }

        std::string replacement;
        switch (text[pos]) {
        case '"': replacement = "&quot;"; break;
        case '\'': replacement = "&apos;"; break;
        case '&': replacement = "&amp;"; break;
        case '<': replacement = is_marked ? "<" : "&lt;"; break;
        case '>': replacement = is_marked ? ">" : "&gt;"; break;
        default: break;
        }

        text.replace(pos, 1, replacement);
        pos += replacement.size();
    }
    return text;
}

std::string xml_escape_double_quotes_attribute_value(std::string text)
{
    std::string::size_type pos = 0;
    for (;;) {
        pos = text.find_first_of("\"&<\r\n\t", pos);
        if (pos == std::string::npos) {
            break;
        }

        std::string replacement;
        switch (text[pos]) {
        case '"': replacement = "&quot;"; break;
        case '&': replacement = "&amp;"; break;
        case '<': replacement = "&lt;"; break;
        case '\r': replacement = "&#xD;"; break;
        case '\n': replacement = "&#xA;"; break;
        case '\t': replacement = "&#x9;"; break;
        default: break;
        }

        text.replace(pos, 1, replacement);
        pos += replacement.size();
    }
    return text;
}

std::string xml_unescape(std::string text)
{
    struct Replacement {
        const char* escaped;
        const char* raw;
    };
    for (const Replacement replacement : {
             Replacement{"&lt;", "<"},
             Replacement{"&gt;", ">"},
             Replacement{"&amp;", "&"},
             Replacement{"&apos;", "'"},
             Replacement{"&quot;", "\""},
         }) {
        std::string::size_type pos = 0;
        while ((pos = text.find(replacement.escaped, pos)) != std::string::npos) {
            text.replace(pos, std::strlen(replacement.escaped), replacement.raw);
            pos += std::strlen(replacement.raw);
        }
    }
    return text;
}

std::unique_ptr<std::string> read_from_disk(const std::string& path)
{
    std::ifstream input(path, std::ios::binary);
    if (!input) {
        return nullptr;
    }
    std::ostringstream buffer;
    buffer << input.rdbuf();
    return std::make_unique<std::string>(buffer.str());
}

void load_string_file(const boost::filesystem::path& path, std::string& output)
{
    std::ifstream input(path.string(), std::ios::binary);
    if (!input) {
        output.clear();
        return;
    }
    std::ostringstream buffer;
    buffer << input.rdbuf();
    output = buffer.str();
}

void save_string_file(const boost::filesystem::path& path, const std::string& value)
{
    std::ofstream output(path.string(), std::ios::binary | std::ios::trunc);
    output.write(value.data(), static_cast<std::streamsize>(value.size()));
}

} // namespace Slic3r
