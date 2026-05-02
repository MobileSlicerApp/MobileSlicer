#ifndef ORCA_ANDROID_CORE_HPP
#define ORCA_ANDROID_CORE_HPP

#include <string>

namespace orca_android_core {

class __attribute__((visibility("default"))) ModelLoader {
public:
    ModelLoader() = default;
    ~ModelLoader();

    ModelLoader(const ModelLoader&) = delete;
    ModelLoader& operator=(const ModelLoader&) = delete;

    bool load_model(const char* path);
    bool set_config_json(const char* json);
    bool slice();
    void clear_generated_gcode();
    void clear();

    bool has_model() const;
    const std::string& loaded_path() const;
    const std::string& gcode() const;

private:
    struct Impl;

    std::string loaded_path_;
    Impl* impl_ = nullptr;
    bool has_model_ = false;
};

} // namespace orca_android_core

#endif
