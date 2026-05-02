// G-code file helpers for orca_wrapper.cpp. Included inside the anonymous namespace.

static std::filesystem::path make_temp_gcode_path()
{
    const auto temp_dir = std::filesystem::temp_directory_path();
    for (int attempt = 0; attempt < 100; ++attempt) {
        const auto candidate = temp_dir / ("orca_wrapper_" + std::to_string(::getpid()) + "_" + std::to_string(attempt) + ".gcode");
        if (!std::filesystem::exists(candidate)) {
            return candidate;
        }
    }
    return temp_dir / "orca_wrapper_fallback.gcode";
}
static std::string read_file_to_string(const std::filesystem::path& path)
{
    std::ifstream input(path, std::ios::binary);
    std::ostringstream buffer;
    buffer << input.rdbuf();
    return buffer.str();
}

static bool replace_first(std::string& text, const std::string& from, const std::string& to)
{
    const size_t pos = text.find(from);
    if (pos == std::string::npos) {
        return false;
    }
    text.replace(pos, from.size(), to);
    return true;
}

static unsigned int extract_total_layer_count(const std::string& gcode)
{
    const std::string marker = "; total layers count = ";
    const size_t marker_pos = gcode.find(marker);
    if (marker_pos != std::string::npos) {
        const char* start = gcode.c_str() + marker_pos + marker.size();
        char* end = nullptr;
        const unsigned long parsed = std::strtoul(start, &end, 10);
        if (end != start && parsed > 0 && parsed <= std::numeric_limits<unsigned int>::max()) {
            return static_cast<unsigned int>(parsed);
        }
    }

    unsigned int count = 0;
    size_t pos = 0;
    while (pos < gcode.size()) {
        const size_t layer_change_pos = gcode.find("\n;LAYER_CHANGE", pos);
        const size_t change_layer_pos = gcode.find("\n; CHANGE_LAYER", pos);
        const size_t next_pos = std::min(layer_change_pos, change_layer_pos);
        if (next_pos == std::string::npos) {
            break;
        }
        ++count;
        pos = next_pos + 1;
    }
    return count;
}

static void finalize_android_gcode_header(const std::filesystem::path& path)
{
    std::string gcode = read_file_to_string(path);
    if (gcode.empty()) {
        return;
    }

    bool changed = false;
    const unsigned int total_layers = extract_total_layer_count(gcode);
    if (total_layers > 0) {
        changed |= replace_first(
            gcode,
            ";_GP_TOTAL_LAYER_NUMBER_PLACEHOLDER",
            "; total layer number: " + std::to_string(total_layers)
        );
    }

    if (changed) {
        std::ofstream output(path, std::ios::binary | std::ios::trunc);
        output << gcode;
    }
}
