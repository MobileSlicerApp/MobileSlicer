You are building a production-grade Android application named **Mobile Slicer**.

The objective is to embed the slicing engine of OrcaSlicer into a fully native, touch-first Android application, while maintaining long-term maintainability, clean architecture, and the ability to update the Orca codebase without major rewrites.

This is NOT a desktop port. This is a mobile-first product with a reused backend.

Current shipping milestone status: shipping ARM runtime proof for the integrated app flow on `RFCYA01ANVE` (`arm64-v8a`) is now achieved with final `.gcode` emission from the shipping APK path.

---

# CORE PRODUCT GOAL

Mobile Slicer must be:

* A clean, modern Android slicer
* Fully touch-first
* Backed by a native C++ slicing engine
* Architecturally stable under upstream updates
* Easy for new developers to understand and continue
* Better on phones than a desktop slicer squeezed onto a small screen

Practical product meaning:

* this project should not try to “win” by copying the largest desktop surface as fast as possible
* it should try to become the best mobile slicer by combining:
  * a trustworthy local slicer backend
  * clear touch-first workflow
  * bounded honest settings coverage
  * eventually, direct printer workflow
* feature decisions should favor the shortest good path from:
  * model found
  * to model adjusted
  * to model sliced
  * to file exported or print started

---

# ARCHITECTURE (STRICT SEPARATION)

You MUST enforce a 3-layer architecture:

## 1. Vendor Layer

Path: `/vendor/orcaslicer`

* Clean fork of OrcaSlicer
* No direct edits unless absolutely required
* Synced with upstream regularly
* Acts only as a dependency

---

## 2. Engine Wrapper Layer

Path: `/engine-wrapper`

* Written in C++
* Builds: `liborca_engine.so`
* Provides a stable **C API**
* ONLY layer allowed to interact with Orca internals

Purpose:

* Isolate upstream volatility
* Prevent Android layer breakage during updates

---

## 3. Android App Layer

Path: `/android-app`

* Kotlin + Jetpack Compose
* No direct Orca includes
* Communicates via JNI only

---

# ENGINE EXTRACTION

You must compile Orca in headless mode:

* Disable GUI (`SLIC3R_GUI=OFF`)
* Remove OpenGL, wxWidgets, desktop integrations
* Include only slicing logic

Required capabilities:

* STL / 3MF loading
* Config handling (printer, filament, process)
* Slicing pipeline
* G-code generation
* Time + filament estimates

## SECURITY POSTURE

The target product is safer by simplification than a cloud slicer:

* fully local by default
* no required account system
* no required backend service

But the project must not treat that as "security solved."

Security priorities for this app are:

* safe handling of untrusted imported files
* careful native/JNI boundary behavior
* minimal Android permission surface
* cautious introduction of future printer/network features

Follow `README/SECURITY.md` for the working security checklist.

---

# ENGINE WRAPPER API

Expose ONLY a stable C interface:

```c
typedef struct OrcaEngine OrcaEngine;

OrcaEngine* orca_create();
void orca_destroy(OrcaEngine* engine);

int orca_load_model(OrcaEngine* engine, const char* path);

int orca_set_config_json(OrcaEngine* engine, const char* json);

int orca_slice(OrcaEngine* engine);

const char* orca_get_gcode(OrcaEngine* engine);

const char* orca_get_estimates_json(OrcaEngine* engine);
```

Rules:

* No C++ types across boundary
* No exceptions across boundary
* JSON for structured data
* Explicit memory ownership

---

# JNI RULES

* 1:1 mapping with C API
* No fine-grained calls

Correct:

* Send config once
* Slice once
* Retrieve results

Incorrect:

* Many small JNI calls

---

# ANDROID UI (KOTLIN + COMPOSE)

This is REQUIRED.

Do NOT use:

* XML layouts
* Cross-platform frameworks

Flow:

1. Import model
2. Select preset
3. Adjust settings
4. Slice
5. Preview
6. Export

## PRODUCT UX DIRECTION

Use OrcaSlicer as the source of truth for:

* profile concepts
* slicer terminology
* settings grouping
* preset structure

Use committed Mobile Slicer design inspiration files and product logo assets as
the source of truth for:

* visual presentation
* interaction style
* layout hierarchy
* mobile-first feel

Current repo hygiene note: do not rely on missing local screenshot references.
The previous screenshot files under `README/Design Inspiration files/` were
intentionally deleted.

This means:

* preserve the Orca mental model underneath
* present it as a touch-first Android product
* do not build a desktop Orca screen clone
* do not dump raw desktop settings lists directly into the app UI
* keep slicer configuration under `Profiles`
* keep app preferences under a separate app `Settings` flow
* treat feature breadth and product polish as separate goals:
  * a field does not count as product progress if it is only technically present
  * a mobile workflow feature should feel clear and intentional on-device, not merely inherited from desktop slicer concepts

## PRODUCT DIFFERENTIATION

Mobile Slicer should aim to differentiate on:

* fully local slicing on Android
* honest proof-backed settings integration
* stable architecture under backend updates
* a faster phone-native path from import to sliced/exported/sent print

It should not try to differentiate by:

* copying every desktop control immediately
* relying on cloud slicing to paper over device/workflow weaknesses
* overclaiming support for settings that are only partially wired

## LATER DIFFERENTIATORS

Once the Android core is materially stable, the strongest later differentiators are:

* richer workspace model operations
* direct printer send workflow
* bounded result / preview tools
* eventually, `STEP` import if it can be added without destabilizing the core product

---

# VISUAL DESIGN SYSTEM (ENFORCED)

UI must match the committed reference assets and product logo direction.

## Principles

* Clean
* Minimal
* Card-based
* Touch-first
* Streamlined

---

## Layout

* Top app bar with logo
* Hero card (Import)
* Profiles as a home-shell entry point for slicer configuration
* single top-right Settings action for app preferences
* Content sections

Large rounded cards only. No flat desktop layouts.

---

## Colors

* Dark theme primary
* Soft gradients
* Muted backgrounds
* User-selectable accent colors

---

## Components

Buttons:

* Primary (filled)
* Secondary (outlined)

Cards:

* Rounded
* Elevated or gradient

Settings:

* Tab-based (Printer / Filament / Process / Appearance)
* NOT system-style lists

Product ownership split:

* `Profiles`
  * Printer
  * Filament
  * Process
  * preset selection
  * preset editing
  * custom preset creation
* app `Settings`
  * theme mode
  * accent color
  * legal/about/info
  * app-only behavior/preferences

---

## Interaction

Use:

* Bottom sheets
* Tabs
* Expandable sections

Avoid:

* Hover logic
* Deep nesting

---

## Logo

* Use provided layered logo
* Top app bar placement
* Must remain consistent

---

# CENTRALIZED README SYSTEM (MANDATORY)

Create:

```
/README/
    CORE_GOAL.md
    ARCHITECTURE.md
    ENGINE_WRAPPER.md
    ANDROID_UI.md
    DESIGN_SYSTEM.md
    BUILD_SYSTEM.md
    UPDATE_WORKFLOW.md
    TASKS.md
    CHANGELOG.md
```

---

## REQUIRED RULE

Every developer MUST:

1. Read CORE_GOAL.md
2. Read ARCHITECTURE.md
3. Update TASKS.md
4. Log changes in CHANGELOG.md
5. Update PROJECT_SPEC.md when the product contract or milestone definition materially changes

Workflow ownership rule:

* the project supervisor owns `NEXT_PROMPT.md`
* coder runs may update verified-result docs, but they must not rewrite `NEXT_PROMPT.md` unless explicitly assigned the supervisor role
* coder runs must not draft, propose, or suggest replacement `NEXT_PROMPT.md` text; next-prompt wording and framing are supervisor-owned

---

# UPDATE STRATEGY

* Add upstream remote
* Maintain branch: `android-engine`
* Rebase regularly

Rules:

* Do NOT modify upstream unnecessarily
* All fixes go in wrapper

---

# BUILD SYSTEM

* Android NDK + CMake
* Output: `liborca_engine.so`
* Target: arm64-v8a

Must:

* Strip unused modules
* Remove desktop dependencies

---

# PERFORMANCE

* All slicing in native layer
* No UI thread blocking
* Background execution required

---

# INITIAL MILESTONE

Deliver:

* Headless Orca build
* Working wrapper
* Android app that:

  * Loads STL
  * Applies config
  * Generates G-code

---

# Future Engine Integration Plan

The current Android backend proves the architecture and end-to-end app flow, but it does not yet run the full Orca `libslic3r` slicing pipeline.

## Objective

Replace the reduced Android-native slicing backend with a full headless Orca slicing engine on Android while keeping the wrapper API stable for JNI and UI.

## Current Backend Gap Audit

The current Android backend is a reduced native core, not a `libslic3r` port.

Current reduced backend characteristics:

* Uses `vendor/orcaslicer/deps_src/admesh` for STL parsing and validation
* Repairs STL geometry with a small Orca-aligned import repair routine
* Stores mesh state as `stl_file`
* Accepts minimal JSON but only extracts a few numeric fields
* Generates controlled synthetic G-code from the STL bounding box

What is not yet present:

* `Slic3r::Model` as the canonical loaded model representation
* `DynamicPrintConfig` / `PrintConfig` preset application
* `Slic3r::Print` orchestration and `PrintObject` processing
* Real slicing stages for walls, infill, supports, skirts, brims, or wipe towers
* Real G-code export through `Slic3r::GCode`
* Output estimates and richer structured results

This is the main truth gap in the project. The UI and wrapper loop are real. The engine backend behind `slice()` is still temporary.

## Minimum Real Orca Port Target

Phase 2 should not aim for the full desktop application. The minimum Android-compatible target is:

* Headless `libslic3r`
* Real `Slic3r::Model` loading for STL first
* Real `PrintConfig` / `DynamicPrintConfig` application from wrapper-owned JSON
* Real `Slic3r::Print` entry point for FFF slicing
* Real `Slic3r::GCode` output returned through the existing wrapper API

Explicitly excluded from this minimum target:

* wxWidgets and desktop UI
* OpenGL and preview rendering
* Desktop integrations and host services
* Android UI changes beyond the already working app loop
* Any wrapper API expansion that is not strictly required

## Dedicated Android Build Path

The real port must be developed beside the reduced backend, not on top of it.

Dedicated path:

* `engine-wrapper/orca-android-libslic3r/`

Purpose:

* isolate Android-specific headless `libslic3r` work
* keep the current shipping reduced backend stable
* prove native configuration and compilation milestones before backend replacement

Current status:

* the path is active for dependency bring-up work
* it is not linked into the app build
* it currently serves as the landing zone for Android-specific CMake, generated headers, and future shims
* a dependency-backed target now builds with the Android NDK for `x86_64`
* a config-focused target now also builds with the Android NDK for `x86_64`
* `arm64-v8a` experimental configure now succeeds for the print probe after ARM OpenSSL staging; compile and link now both succeed for `orca_android_libslic3r_print_gcode_probe`; ARM runtime proof is now complete on `RFCYA01ANVE` with real final output
* shipping integration into the app native boundary is now the next blocker on `arm64-v8a`
  * build command: `cd /home/peanut/Development/MobileSlicer/android-app && cmake --build app/.cxx/Debug/35l5eg2l/arm64-v8a --target orca_engine -j 6`
  * status: shipping ARM `orca_engine` link blocker is cleared
  * fixed with `android-app/app/src/main/cpp/getrandom_compat.cpp` under `ORCA_SHIPPING_REAL_WRAPPER`
  * result: link succeeds on `arm64-v8a` for shipping `orca_engine`
  * classification: previously shipping `link-level` + `dependency-level` ARM blocker now resolved
* implementation-backed config targets compile and link successfully
* `config_impl_subset` shared library links as `liborca_android_libslic3r_config_impl_subset.so`
* 15 real `libslic3r` source files compiled (config core + geometry)
* the shipping app still uses only the reduced backend

## Dependency Map

The dependency map below is based on the actual `vendor/orcaslicer/src/libslic3r/CMakeLists.txt` and related vendor CMake files in this repo.

### Required Core Dependencies

* `Boost`
  Current Android status: proven buildable for Android `x86_64` as a static dependency set with full header tree.
  Current status: ARM compile/link boundary for `Boost.Nowide` setenv is now cleared by compiling `Boost.Nowide` source from configured `ORCA_ANDROID_BOOST_SRC_ROOT` (`/tmp/orca-deps-src/boost-1.84.0`) in the ARM print probe target.
  Proposed strategy: keep source-equivalent linkage for `boost/nowide/cstdlib.cpp` (`setenv` path) as the minimum honest fix instead of adding a staged `libboost_nowide.a`.
  Current result: `boost::nowide::setenv` symbol is now resolved; ARM `orca_android_libslic3r_print_gcode_probe` config and link path proceeds past `Config.cpp` (`Slic3r::ConfigBase::setenv_() const`).

* `cereal`
  Current Android status: integrated successfully as a header-only dependency in the experimental path.
  Build blocker: none for header-only use.
  Proposed strategy: use the exact upstream version already referenced by Orca build assets, `v1.3.0`, vendored under the experimental Android path instead of introducing a separate package manager or shipping-target dependency.
  Current result: `PrintConfig.hpp` and `DynamicPrintConfig` now compile on Android in the experimental path.

* `OpenSSL`
  Current Android status: builds successfully for Android `x86_64` and `arm64-v8a` as static `libcrypto.a` and `libssl.a` using OpenSSL 3.1.7.
  Build blocker: none for the current config implementation milestone.
  Proposed strategy: cross-compile with NDK clang through OpenSSL's own `Configure` script targeting `android-x86_64`, keep static-only, isolate under the experimental CMake path.
  Current result: `Config.cpp` now compiles successfully with staged Android OpenSSL headers; `openssl/md5.h` is resolved.

* `CGAL`
  Current Android status: header-only Android install is proven and consumable in the experimental path.
  Build blocker: package-config consumption is less reliable than direct wiring because CGAL still expects desktop-style Boost discovery behavior.
  Proposed strategy: keep CGAL header-only for the first Android milestone and link GMP / MPFR explicitly in the experimental path.
  Current result: CGAL-backed geometry code compiles in the dependency subset target.

* `GMP` / `MPFR`
  Current Android status: both now build successfully for Android `x86_64` as static libraries.
  Build blocker: none for the initial Android bring-up.
  Proposed strategy: keep them as static foundational dependencies for CGAL-backed Android builds.
  Current result: both are linked successfully by the dependency subset target.

* `TBB`
  Current Android status: proven buildable for Android `x86_64` as static `libtbb.a` and `libtbbmalloc.a`.
  Build blocker: shared `tbbmalloc` failed under Android linker/version-script behavior; `TBB_STRICT=OFF` also broke Android compiler flag handling in oneTBB.
  Proposed strategy: keep static TBB for the Android port and avoid shared allocator bring-up until there is a concrete need.
  Current result: real `libslic3r` execution-policy headers using TBB compile in the experimental path.

* `OpenCV`
  Current Android status: required by upstream `libslic3r` CMake; absent from current Android build.
  Build blocker: large native package plus Android packaging and ABI handling.
  Proposed strategy: identify the exact modules really needed by the minimum FFF slicing path and keep the Android port scoped to those.

* `OpenCASCADE`
  Current Android status: required by upstream `libslic3r` CMake for CAD-related functionality; absent from current Android build.
  Build blocker: large desktop-oriented dependency with broad native surface area.
  Proposed strategy: treat as a major risk item; verify whether STL-first Android slicing can isolate STEP/CAD-heavy code paths enough to defer or patch this dependency in the first Android slice milestone.

* `draco`
  Current Android status: required by upstream `libslic3r` CMake; absent from current Android build.
  Build blocker: additional native dependency not yet wired for Android.
  Proposed strategy: confirm whether the minimum STL-first Android slice path touches Draco at runtime; if not, keep the dependency available but outside the first proof milestone where possible.

* `JPEG`, `PNG`, `ZLIB`, `EXPAT`, `OpenSSL`
  Current Android status: required by upstream `libslic3r` CMake; not wired into current Android app build.
  Build blocker: packaging and CMake discovery for Android.
  Proposed strategy: treat as standard native support dependencies and stage them after the geometry/concurrency blockers.

### Optional or Conditional Dependencies

* `OpenVDB`
  Current Android status: optional in upstream `libslic3r` CMake, enabled only if `OpenVDB::openvdb` exists.
  Build blocker: brings in `OpenEXR`, `Imath`, `Blosc`, and TBB-related complexity.
  Proposed strategy: keep disabled for the first FFF Android port target unless a proven runtime requirement appears.

* `OpenEXR` / `Imath`
  Current Android status: required by the vendor `OpenVDB` dependency path, not by the reduced Android core.
  Build blocker: large transitive image/math stack through OpenVDB.
  Proposed strategy: defer with OpenVDB until the minimum FFF port is working.

* `wxWidgets`, `OpenGL`, `GLEW`, `OpenCSG`
  Current Android status: desktop-oriented and outside the headless target.
  Build blocker: not suitable for the intended Android engine layer.
  Proposed strategy: exclude entirely from the Android engine port.

### Vendored Internal Libraries Used by `libslic3r`

These are not the primary Android risk, but they are part of the real build graph and must be accounted for:

* `admesh`
* `libigl`
* `libnest2d`
* `clipper`
* `Clipper2`
* `mcut`
* `qhull`
* `qoi`
* `semver`
* `noise::noise`

## High-Level Steps

1. Keep the reduced backend as the shipping path while Phase 2 work happens in the dedicated experimental Android `libslic3r` path.
2. Prove that the experimental path configures cleanly with the Android NDK and generated Orca headers.
3. Bring up foundational required dependencies for Android: Boost, GMP, MPFR, CGAL, TBB, and the minimum support libraries.
4. Build a headless native target that can include and link real `libslic3r` code without JNI involvement.
5. Stage the next transitive non-core dependencies needed for `PrintConfig`, starting with `cereal`.
6. Prove real config type compilation using `PrintConfig` / `DynamicPrintConfig`.
7. Compile and link the config implementation units, starting with `Config.cpp` and `PrintConfig.cpp`.
   Status: COMPLETE
   * `config_impl_subset` shared library links successfully on Android `x86_64`
   * includes 15 real `libslic3r` source files (config core + geometry)
   * 4 subsystems stubbed to keep the target minimal
8. Prove real model representation using `Slic3r::Model` for STL first.
   Status: COMPLETE
   * `orca_android_libslic3r_model_stl_probe` compiles and links on Android `x86_64`
   * the probe uses real `Model.cpp`, `TriangleMesh.cpp`, `Format/STL.cpp`, Admesh, Qhull, and semver support
   * Waydroid runtime proof succeeded with `objects=1 volumes=1 instances=0 facets=4` on the checked-in tetrahedron STL fixture
9. Prove wrapper-owned JSON can be translated into a real `PrintConfig` / `DynamicPrintConfig`.
   Status: COMPLETE
   * `orca_android_libslic3r_config_json_probe` compiles, links, and runs on Android `x86_64`
   * the probe parses flat wrapper-style JSON and applies it to a real `DynamicPrintConfig`
   * Waydroid runtime proof succeeded with round-tripped values for `layer_height`, `initial_layer_print_height`, `nozzle_diameter`, `filament_diameter`, `hot_plate_temp`, `hot_plate_temp_initial_layer`, `nozzle_temperature`, `nozzle_temperature_initial_layer`, `travel_speed`, and `wall_loops`
   * missing-key and invalid-value fixtures both fail honestly
10. Prove one end-to-end native slice call using real `Slic3r::Print` and `Slic3r::GCode`.
   Status: CLEAN AND COMPLETE in Waydroid `x86_64` (first real final-output proof)
   * `orca_android_libslic3r_print_gcode_probe` now compiles and links on Android `x86_64`
   * the probe reuses the completed STL model-load path and wrapper-style JSON config-application path
* current state is clean experimental completion through final export with target `.gcode` output
  * `arm64-v8a` configure and build now succeed for `orca_android_libslic3r_print_gcode_probe` after staged ARM OpenSSL and `BuildVolume_Type` enum fix; Boost.Nowide `setenv` source closure is wired via `ORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`
  * ARM execution is now confirmed on `RFCYA01ANVE` and host artifact `/tmp/orca_android_libslic3r_print_probe_output-arm64.gcode` is available
   * the first expansion wave added `PrintBase.cpp`, `PrintApply.cpp`, `Print.cpp`, `PrintObject.cpp`, `PrintObjectSlice.cpp`, `PrintRegion.cpp`, `PlaceholderParser.cpp`, `Layer.cpp`, `LayerRegion.cpp`, `Surface.cpp`, `SurfaceCollection.cpp`, `Slicing.cpp`, `Flow.cpp`, `Support/SupportMaterial.cpp`, `GCode.cpp`, `GCodeWriter.cpp`, `Extruder.cpp`, and `ExtrusionEntityCollection.cpp`
   * the second real closure wave now also compiles with `PerimeterGenerator.cpp`, `ExtrusionEntity.cpp`, `SlicingAdaptive.cpp`, `PrincipalComponents2D.cpp`, `Algorithm/RegionExpansion.cpp`, `Feature/Interlocking/InterlockingGenerator.cpp`, and `GCode/AvoidCrossingPerimeters.cpp`
   * the current essential follow-up layer now also includes the corrected `deps_src/clipper/clipper_z.cpp`, `EdgeGrid.cpp`, `BridgeDetector.cpp`, and `VariableWidth.cpp`
   * `ClipperLib_Z::*`, `EdgeGrid::Grid::create(...)`, `BridgeDetector::*`, and `variable_width(...)` are now cleared
   * interlocking is intentionally re-isolated for the current milestone because the probe remains single-material and STL-only
   * full `GCode/GCodeProcessor.cpp` is still not linked; only narrow compatibility stubs remain for helper behavior such as `get_last_z_from_gcode(...)` and `get_last_position_from_gcode(...)`
   * the next honest blocker is split between:
     * likely essential export/runtime helpers: `GCodeProcessor::{initialize,check_multi_extruder_gcode_valid,finalize}`, `ToolOrdering::*`, `rename_file(...)`, `get_utf8_sequence_length(...)`
     * optional feature/helper branches: Arachne wall-toolpaths, timelapse picker, thumbnails, EdgeGrid PNG/SVG debug helpers
11. Replace the reduced backend behind the existing wrapper API only after the real path is stable.
12. Revalidate Android load, slice, export, and share without changing the UI or JNI surface.

## Backend Replacement Milestones

* Milestone 1: experimental Android Orca path configures
* Milestone 2: foundational dependency chain builds for one Android ABI
* Milestone 3: dependency-backed `libslic3r` execution-policy target compiles
* Milestone 4: `cereal` is integrated for Android and `PrintConfig` / `DynamicPrintConfig` headers compile
* Milestone 5: config implementation units compile and link in native code — COMPLETE
  * `Config.cpp` and `PrintConfig.cpp` compile and link successfully
  * `config_impl_subset` shared library links as `liborca_android_libslic3r_config_impl_subset.so`
  * 15 real `libslic3r` source files included (config core + geometry)
  * 4 subsystems stubbed (ShortestPath, MedialAxis/Voronoi, GCodeThumbnails, utils.cpp)
* Milestone 6: real `Slic3r::Model` load works in native code — COMPLETE
  * `orca_android_libslic3r_model_stl_probe` executable links on Android `x86_64`
  * runtime proof succeeded in Waydroid with real STL import
  * additional real sources included: `ObjectID.cpp`, `Geometry/ConvexHull.cpp`, `TriangleMesh.cpp`, `Model.cpp`, `Format/STL.cpp`, Admesh STL reader units, semver C, and Qhull
  * additional controlled stubs isolate non-STL import and model-editing branches
* Milestone 7: real config application works through wrapper-owned JSON — COMPLETE
  * `orca_android_libslic3r_config_json_probe` executable links on Android `x86_64`
  * Waydroid runtime proof succeeded for a narrow wrapper-style JSON subset
  * no additional real `libslic3r` implementation units were required beyond the config implementation milestone
* Milestone 8: real `Print` pipeline runs in native code
  * status: CLEAN COMPLETION PROVEN in Waydroid `x86_64` and confirmed ARM `arm64-v8a` on `RFCYA01ANVE` (clean export artifact emitted)
  * `orca_android_libslic3r_print_gcode_probe` now compiles, links, and executes on Android `x86_64`
  * `arm64-v8a` configure/build both now succeed for `orca_android_libslic3r_print_gcode_probe` after adding `ORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0` and reusing the same `Boost.Nowide cstdlib.cpp` source-closure strategy used on x86_64
* current shipping-boundary status:
    * status: cleared
    * exact result: no longer blocking
    * resolved by: `android-app/app/src/main/cpp/getrandom_compat.cpp` (included when `ORCA_SHIPPING_REAL_WRAPPER` is ON)
    * classification: resolved link-level / dependency-level blocker; next exact boundary is ARM runtime proof
  * current essential runtime closures already added:
    * Clipper-Z runtime geometry
    * `EdgeGrid` support for avoid-crossing-perimeters travel planning
    * bridge detection
    * variable-width perimeter helpers
    * real `ToolOrdering.cpp` plus its helper cluster (`ToolOrderUtils.cpp`, `FilamentGroup.cpp`, `FilamentGroupUtils.cpp`, `ParameterUtils.cpp`)
    * real `CustomGCode.cpp`, `GCode/CoolingBuffer.cpp`, and `GCode/PrintExtents.cpp`
    * real `CommandProcessor` trie support for the partial `GCodeProcessor` constructor path
    * real `GCode/PressureEqualizer.cpp`
    * real `GCode/SmallAreaInfillFlowCompensator.cpp`
  * current bookkeeping/config/time status:
    * real vendor `GCodeReader.cpp` is now linked
    * real vendor `Time.cpp` is now linked
    * real lifted `GCodeProcessor` bookkeeping now replaces the earlier reduced stubs for `CachedPosition::reset()`, `CpColor::reset()`, `TimeMachine::{State::reset(),CustomGCodeTime::reset(),reset()}`, `TimeProcessor::reset()`, `UsedFilaments::reset()`, `GCodeProcessorResult::reset()`, and `contains_reserved_tags(...)`
    * real lifted `GCodeProcessor::reset()` and `GCodeProcessor::apply_config(const PrintConfig&)` are also now used in the experimental path
  * current layer-feature closure status:
    * real `ElephantFootCompensation.cpp`, `Fill/Fill.cpp`, `GCode/AdaptivePAProcessor.cpp`, `GCode/AdaptivePAInterpolator.cpp`, `GCode/SpiralVase.cpp`, and `Arachne/utils/ExtrusionLine.cpp` are now compiled in the experimental probe
    * this clears `elephant_foot_compensation(...)`, `AdaptivePAProcessor::{AdaptivePAProcessor,process_layer}`, `AdaptivePAInterpolator::{parseAndSetData,operator()}`, `SpiralVase::process_layer(...)`, and `Arachne::ExtrusionLine::getLength() const`
  * verified fill-base closure progress from the latest rebuild:
    * real `Fill/FillBase.cpp`, `Fill/FillRectilinear.cpp`, and `Fill/FillCrossHatch.cpp` are now included in the isolated print probe
    * real `ShortestPath.cpp` plus Clipper2 offset/runtime units are now included for the active fill path
    * exact real lifted Clipper2 conversion helpers are now used in the experimental stub layer to keep the active fill path moving without broadening the target further
    * active default fill patterns for the current probe are now backed by real Orca units:
      * `ipRectilinear`
      * `ipMonotonic`
      * `ipMonotonicLine`
      * `ipCrossHatch`
    * optional factory-surfaced fill families remain temporarily isolated with experimental no-op key-function stubs:
      * `FillConcentric`
      * `FillConcentricInternal`
      * `FillHoneycomb`
      * `Fill3DHoneycomb`
      * `FillGyroid`
      * `FillTpmsD`
      * `FillTpmsFK`
      * `FillLine`
      * plane-path fills
      * `FillAdaptive::Filler`
      * `FillLightning::Filler`
   * additional real runtime closure now included:
     * `GCode/SeamPlacer.cpp`
     * `GCode/RetractWhenCrossingPerimeters.cpp`
     * `ShortEdgeCollapse.cpp`
     * `TriangleSetSampling.cpp`
     * `NormalUtils.cpp`
   * this confirms that:
     * `SeamPlacer::place_seam(...)` is real executed runtime, not just surfaced optional code
     * `RetractWhenCrossingPerimeters::travel_inside_internal_regions(...)` is now treated as real export/runtime support
   * current optional or feature-scoped branches still isolated or candidates for isolation:
     * support generation data (`TreeSupportData::{TreeSupportData,clear_nodes}`)
     * custom-facet geometry helpers (`smooth_outward(...)`, `slice_mesh_slabs(...)`, `triangulate_expolygon_3d(...)`)
     * thumbnails
     * EdgeGrid PNG/SVG debug helpers
     * filament-group color-distance helper from `FlushVolPredictor.cpp`
  * `ToolOrdering.cpp` is now confirmed to be real runtime, not optional:
    * `Print::process()` and `GCode::_do_export()` both construct it directly
  * `GCodeProcessor` is still only partially real in the experimental path:
    * `initialize(...)`, `check_multi_extruder_gcode_valid(...)`, `finalize(...)`, `process_file(...)`, `process_buffer(...)`, and `get_gcode_last_filament(...)` are still experimental compatibility stubs
    * the reset/config/time bookkeeping surface above is now real or lifted from real Orca code
    * this means the unresolved layer has moved away from parser/time bookkeeping and into broader real print/runtime features
  * verified Android runtime result from this run:
    * the previous `No layers were detected` failure was not a legitimate 20 mm model/config issue
    * it was caused by an executed experimental `slice_mesh_ex(...)` stub still linked into the print probe
    * after replacing that executed stub path with real `TriangleMeshSlicer.cpp`, the 20 mm fixture completes `print.process()` and emits real temporary G-code on Android `x86_64`
    * final `Print + GCode` export is now complete on Android `x86_64` with real output at `/data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
    * representative first lines in emitted output:
      * `; HEADER_BLOCK_START`
      * `M201 X1000 Y1000 Z500 E5000`
  * current blocker classification:
    * original `No layers were detected`: missing executed runtime implementation
    * current post-export blocker: cleared by config/preset contract updates in `PrintConfig.cpp`
      * added `most_used_physical_extruder_id` and `curr_physical_extruder_id` to
        `s_CustomGcodeSpecificPlaceholders` (`layer_change_gcode`, `timelapse_gcode`)
      * added config definitions for those keys in `CustomGcodeSpecificConfigDef`
* Milestone 9: real G-code replaces reduced output behind the wrapper
* Milestone 10: existing Android export/share flow works unchanged on top of the new backend

## Known Risks

* The full Orca dependency graph is materially larger than the current Android-native subset
* `libslic3r` CMake currently assumes a desktop-class dependency environment
* OpenCASCADE may be the largest single blocker for a minimal STL-first Android target
* Config implementation linkage required 15 real `libslic3r` source files and 4 stubbed subsystems; future model and slicing milestones will require progressively more of the `libslic3r` source set and may unstub currently-deferred subsystems
* TBB and CGAL are early hard gates, but not the last Android bring-up blockers
* Native binary size and ABI build time will rise sharply
* The port must not leak Orca internals above the wrapper while these changes happen

## Config Implementation Status

Config header bring-up is complete for the experimental Android path:

* `PrintConfig`
* `FullPrintConfig`
* `DynamicPrintConfig`

That milestone proves type compilation and default construction only.

The completed config implementation milestone was real config implementation linkage through:

* `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp`
* `vendor/orcaslicer/src/libslic3r/Config.cpp`

Current result:

* `Config.cpp` and `PrintConfig.cpp` compile and link on Android `x86_64`
* `config_impl_subset` shared library links successfully as `liborca_android_libslic3r_config_impl_subset.so`
* 15 real `libslic3r` source files are compiled (config core + geometry subsystem)
* Boost.Nowide `setenv` resolved through direct source compilation
* 4 subsystems stubbed to avoid pulling in the full slicing pipeline
* real wrapper-style JSON translation into `DynamicPrintConfig` is now proven in a narrow isolated Android executable
* no extra real `libslic3r` implementation units were needed for the config-application milestone
* the probe maps wrapper-style keys onto real stored Orca keys such as `initial_layer_print_height`, `hot_plate_temp`, and `nozzle_temperature_initial_layer`

What is proven:

* config implementation shared library links on Android
* representative config behavior is available in native code
* representative wrapper-style JSON values can be applied, validated, and read back through real `DynamicPrintConfig`
* the geometry subsystem (Polygon, Clipper, BoundingBox, etc.) compiles for Android
* Boost.Nowide can be consumed from source for individual functions

Next action:

* keep expanding from model load plus config application into the minimum real `Print` path
* keep the single-material probe isolated from nonessential feature branches:
  * interlocking remains restubbed instead of chasing `VoxelUtils.cpp`
  * EdgeGrid PNG/SVG export helpers stay stubbed
  * Arachne wall-toolpaths stay isolated unless runtime proves they are required
* next likely essential runtime decision:
  * either add or partially stub the deeper `GCodeProcessor` bookkeeping methods now exposed by the narrow export-state stubs
  * and evaluate whether `GCodeReader.cpp` integration plus `GCodeProcessorResult` helper closure is the smallest honest next real/runtime layer
* replace model-load stubs incrementally only when `Print` / `GCode` milestones require them

---

# NON-NEGOTIABLE RULES

* No desktop UI port
* No bypassing wrapper
* No tight coupling
* No ignoring README system

---

# FINAL OBJECTIVE

Mobile Slicer must remain:

* Clean
* Fast
* Maintainable
* Mobile-native
* Easy to extend

Everything must support this.
