#pragma once

#include <cstddef>
#include <optional>
#include <string>
#include <string_view>
#include <vector>

namespace mobileslicer::orca_wrapper {

std::string trim_copy(std::string value);
std::string trim_copy(std::string_view value);
std::string_view trim_view(std::string_view value);
bool starts_with_case_insensitive(std::string_view value, const char* prefix);
bool command_matches(std::string_view command, const char* expected);
bool is_preview_ignored_command(std::string_view command);
std::string lowercase_copy(std::string_view value);
void append_unique_limited(std::vector<std::string>& values, std::string value, size_t limit);
bool has_stl_extension(const std::string& path);

std::string unescape_json_string(std::string_view value);
void invalidate_json_scalar_index();
std::optional<std::string_view> indexed_json_value(const std::string& json, const std::string& key);
std::optional<std::string_view> first_json_array_item(std::string_view raw);

std::optional<double> extract_number(const std::string& json, const std::string& key);
std::optional<double> extract_number_any(const std::string& json, std::initializer_list<const char*> keys);
std::optional<bool> extract_bool(const std::string& json, const std::string& key);
std::optional<std::string> extract_string(const std::string& json, const std::string& key);
std::optional<std::string> extract_string_any(const std::string& json, std::initializer_list<const char*> keys);
std::optional<std::vector<std::string>> extract_string_vector_exact(const std::string& json, const std::string& key);
std::vector<double> parse_number_list(const std::string& value);
std::optional<std::string> extract_config_scalar_or_list_string(
    const std::string& json,
    const std::string& key,
    char list_separator = ',');

} // namespace mobileslicer::orca_wrapper
