#include <cstdlib>
#include <exception>
#include <iostream>
#include <string>

#include "libslic3r/Format/STL.hpp"
#include "libslic3r/Model.hpp"

int main(int argc, char **argv)
{
    if (argc != 2) {
        std::cerr << "usage: orca_android_libslic3r_model_stl_probe <input.stl>\n";
        return 2;
    }

    try {
        Slic3r::Model model;
        if (!Slic3r::load_stl(argv[1], &model)) {
            std::cerr << "load_stl returned false for: " << argv[1] << "\n";
            return 1;
        }

        const bool has_object = !model.objects.empty();
        const bool has_volume = has_object && !model.objects.front()->volumes.empty();
        const bool mesh_nonempty =
            has_volume && !model.objects.front()->volumes.front()->mesh().empty();

        if (!(has_object && has_volume && mesh_nonempty)) {
            std::cerr << "model load incomplete:"
                      << " objects=" << model.objects.size()
                      << " volumes=" << (has_object ? model.objects.front()->volumes.size() : 0)
                      << " instances=" << (has_object ? model.objects.front()->instances.size() : 0)
                      << " mesh_nonempty=" << (mesh_nonempty ? 1 : 0)
                      << "\n";
            return 1;
        }

        std::cout << "objects=" << model.objects.size()
                  << " volumes=" << model.objects.front()->volumes.size()
                  << " instances=" << model.objects.front()->instances.size()
                  << " facets=" << model.objects.front()->volumes.front()->mesh().facets_count()
                  << "\n";
        return 0;
    } catch (const std::exception &ex) {
        std::cerr << "exception: " << ex.what() << "\n";
        return 1;
    }
}
