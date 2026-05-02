add_library(orca_wrapper STATIC
    orca_wrapper.cpp
    orca_wrapper_calibration.cpp
    orca_wrapper_gcode.cpp
    orca_wrapper_model_overrides.cpp
    orca_wrapper_printable_validation.cpp
    orca_wrapper_utils.cpp
)

target_include_directories(orca_wrapper
    PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}
    PRIVATE
        ${CMAKE_SOURCE_DIR}/vendor/orcaslicer/src
)

target_link_libraries(orca_wrapper
    PRIVATE
        libslic3r
)

add_executable(orca_wrapper_test
    test_main.cpp
)

target_link_libraries(orca_wrapper_test
    PRIVATE
        orca_wrapper
)
