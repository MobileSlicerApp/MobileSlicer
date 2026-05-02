#include "orca_wrapper.h"

#include <cstdio>
#include <cstdlib>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <string>

namespace {

std::filesystem::path write_sample_stl()
{
    const auto path = std::filesystem::temp_directory_path() / "orca_wrapper_sample_cube.stl";
    std::ofstream out(path);
    out << "solid cube\n"
           "facet normal 0 0 -1\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 20 20 0\n"
           "  vertex 20 0 0\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 0 -1\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 0 20 0\n"
           "  vertex 20 20 0\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 0 1\n"
           " outer loop\n"
           "  vertex 0 0 20\n"
           "  vertex 20 0 20\n"
           "  vertex 20 20 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 0 1\n"
           " outer loop\n"
           "  vertex 0 0 20\n"
           "  vertex 20 20 20\n"
           "  vertex 0 20 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 -1 0\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 20 0 0\n"
           "  vertex 20 0 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 -1 0\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 20 0 20\n"
           "  vertex 0 0 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 1 0\n"
           " outer loop\n"
           "  vertex 0 20 0\n"
           "  vertex 0 20 20\n"
           "  vertex 20 20 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 0 1 0\n"
           " outer loop\n"
           "  vertex 0 20 0\n"
           "  vertex 20 20 20\n"
           "  vertex 20 20 0\n"
           " endloop\n"
           "endfacet\n"
           "facet normal -1 0 0\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 0 0 20\n"
           "  vertex 0 20 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal -1 0 0\n"
           " outer loop\n"
           "  vertex 0 0 0\n"
           "  vertex 0 20 20\n"
           "  vertex 0 20 0\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 1 0 0\n"
           " outer loop\n"
           "  vertex 20 0 0\n"
           "  vertex 20 20 20\n"
           "  vertex 20 0 20\n"
           " endloop\n"
           "endfacet\n"
           "facet normal 1 0 0\n"
           " outer loop\n"
           "  vertex 20 0 0\n"
           "  vertex 20 20 0\n"
           "  vertex 20 20 20\n"
           " endloop\n"
           "endfacet\n"
           "endsolid cube\n";
    return path;
}

} // namespace

int main(int argc, char** argv)
{
    const std::filesystem::path stl_path = argc > 1 ? std::filesystem::path(argv[1]) : write_sample_stl();
    const char* config_json = R"({
        "layer_height": 0.2,
        "first_layer_height": 0.2,
        "fill_density": 15,
        "skirts": 1,
        "gcode_comments": true,
        "start_gcode": ""
    })";

    OrcaEngine* engine = orca_create();
    if (engine == nullptr) {
        std::cerr << "Failed: engine creation failed\n";
        return 1;
    }

    const int load_result = orca_load_model(engine, stl_path.c_str());
    const int config_result = orca_set_config_json(engine, config_json);
    const int slice_result = (load_result == 0 && config_result == 0) ? orca_slice(engine) : -1;
    const char* gcode = orca_get_gcode(engine);

    if (load_result == 0 && config_result == 0 && slice_result == 0 && gcode != nullptr && gcode[0] != '\0') {
        std::cout << "Success: wrapper loaded, configured, and sliced a sample model.\n";
        std::cout << "G-code bytes: " << std::char_traits<char>::length(gcode) << "\n";
        orca_destroy(engine);
        return 0;
    }

    std::cerr << "Failed:"
              << " load_result=" << load_result
              << " config_result=" << config_result
              << " slice_result=" << slice_result
              << "\n";
    orca_destroy(engine);
    return 1;
}
