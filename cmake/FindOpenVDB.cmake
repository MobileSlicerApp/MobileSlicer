include(FindPackageHandleStandardArgs)

find_path(OpenVDB_INCLUDE_DIR
    NAMES openvdb/openvdb.h openvdb/version.h
    PATHS /usr/include /usr/local/include
)

find_library(OpenVDB_openvdb_LIBRARY
    NAMES openvdb libopenvdb
    PATHS /usr/lib64 /usr/lib /lib64 /lib /usr/local/lib64 /usr/local/lib
)

find_library(IlmBase_Half_LIBRARY
    NAMES Half-2_5 Half-2_4 Half
    PATHS /usr/lib64 /usr/lib /lib64 /lib /usr/local/lib64 /usr/local/lib
)

find_library(Blosc_LIBRARY
    NAMES blosc libblosc
    PATHS /usr/lib64 /usr/lib /lib64 /lib /usr/local/lib64 /usr/local/lib
)

find_package_handle_standard_args(OpenVDB
    REQUIRED_VARS
        OpenVDB_INCLUDE_DIR
        OpenVDB_openvdb_LIBRARY
)

if(OpenVDB_FOUND)
    set(OpenVDB_INCLUDE_DIRS "${OpenVDB_INCLUDE_DIR}")
    set(OpenVDB_LIBRARIES "${OpenVDB_openvdb_LIBRARY}")
    set(OpenVDB_openvdb_FOUND TRUE)

    if(NOT TARGET IlmBase::Half AND IlmBase_Half_LIBRARY)
        add_library(IlmBase::Half UNKNOWN IMPORTED)
        set_target_properties(IlmBase::Half PROPERTIES
            IMPORTED_LOCATION "${IlmBase_Half_LIBRARY}"
        )
    endif()

    if(NOT TARGET Blosc::blosc AND Blosc_LIBRARY)
        add_library(Blosc::blosc UNKNOWN IMPORTED)
        set_target_properties(Blosc::blosc PROPERTIES
            IMPORTED_LOCATION "${Blosc_LIBRARY}"
        )
    endif()

    if(NOT TARGET OpenVDB::openvdb)
        add_library(OpenVDB::openvdb UNKNOWN IMPORTED)
        set(_openvdb_link_targets "")
        if(TARGET IlmBase::Half)
            list(APPEND _openvdb_link_targets IlmBase::Half)
        endif()
        if(TARGET Blosc::blosc)
            list(APPEND _openvdb_link_targets Blosc::blosc)
        endif()
        if(TARGET TBB::tbb)
            list(APPEND _openvdb_link_targets TBB::tbb)
        endif()

        set_target_properties(OpenVDB::openvdb PROPERTIES
            IMPORTED_LOCATION "${OpenVDB_openvdb_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${OpenVDB_INCLUDE_DIR}"
            INTERFACE_LINK_LIBRARIES "${_openvdb_link_targets}"
        )
    endif()
endif()
