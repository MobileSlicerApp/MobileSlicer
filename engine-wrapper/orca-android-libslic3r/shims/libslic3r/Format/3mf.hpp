#ifndef slic3r_Format_3mf_hpp_
#define slic3r_Format_3mf_hpp_

#include <string>

namespace Slic3r {

class Model;
struct ConfigSubstitutionContext;
class DynamicPrintConfig;
struct ThumbnailData;

class PrusaFileParser
{
public:
    PrusaFileParser() = default;
    ~PrusaFileParser() = default;

    bool check_3mf_from_prusa(const std::string filename);
    void _start_element_handler(const char *name, const char **attributes);
    void _characters_handler(const char *s, int len);
};

enum {
    support_points_format_version = 1
};

enum {
    drain_holes_format_version = 1
};

extern bool load_3mf(const char* path, DynamicPrintConfig& config,
                     ConfigSubstitutionContext& config_substitutions, Model* model,
                     bool check_version);

extern bool store_3mf(const char* path, Model* model, const DynamicPrintConfig* config,
                      bool fullpath_sources, const ThumbnailData* thumbnail_data = nullptr,
                      bool zip64 = true);

} // namespace Slic3r

#endif
