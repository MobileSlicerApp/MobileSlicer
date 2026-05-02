# Tasks

Historical notes in this file may still mention paths under
`_quarantine/root-dependency-dump*`. Those paths are no longer tracked repo
content. For fresh-clone reruns, use checked-in fixtures under
`proof-fixtures/` or `mobileslicer_test_cube.stl` unless a section is clearly
preserving old proof history.

Use this file in two modes:

* live planning/current sequencing:
  * read `## Staged Roadmap`
* preserved historical execution detail:
  * treat everything from `## Historical Appendix` downward as archive material,
    not default rerun instructions

## Staged Roadmap

The Android app is no longer blocked at "viewer completely broken," but it is
also not ready yet for broad feature expansion. Work should proceed in staged
feature tranches so new product features land on top of a stable workspace
baseline instead of restarting viewer churn every run.

Treat this roadmap as sequencing guidance, not as permission to do everything
at once. Each coder run should still take only the next bounded step.

Current viewer-boundary truth:

* `Prepare` is the working STL workspace in the live tree
* `Preview` now renders sliced G-code through native Orca `libvgcode`
* `Prepare` now has a working multi-object plate session:
  * `+` appends STL objects
  * tapping an object selects it without opening controls
  * `Transform` edits only the selected object
  * delete removes the selected object
  * adding objects preserves previously edited object transforms
  * slicing uses all objects on the plate
  * `Save plate` persists copied STL sources, object transforms, and the full
    current `ProfileStore` snapshot into Saved Projects
  * saved clone plates preserve shared source identity for repeated objects
    instead of expanding clones into unrelated copied STL sources
  * user-confirmed saved clone plates now reopen/slice at the expected
    same-session warm-path speed instead of the prior saved-plate slowdown
  * saved project rows show the user-provided project name, save timestamp,
    and a real viewer-captured thumbnail generated through `PixelCopy`
  * saved-project reopen resets the workspace timing clock before reload so
    stale prior-session first-frame timings do not leak into the dock
  * `Slice` now fails immediately before native slicing when transformed object
    bounds are outside the selected printer's printable width/depth/height
  * duplicate same-path objects avoid duplicate Android mesh prep and avoid
    repeated same-path native loads inside one plate-load call
  * transform slider edits debounce Preview invalidation
* current debug APK performance truth has changed:
  * native shipping slicer/viewer code now builds with `-O3` and
    `-fomit-frame-pointer`
  * native `libvgcode` is included in that optimized debug-build boundary
  * the old Benchy `59s` result is a historical unoptimized-debug baseline
  * user-confirmed current Benchy-class slice time is about `4s`
  * user also confirmed STL rendering became much faster
  * the app-visible timing split proved a later Benchy delay was summary-side,
    not native slicing:
    * before summary cleanup: `Summary 4520 ms`, `Total 6221 ms`
    * after summary cleanup: `Summary 682 ms`, `Total 2513 ms`
    * estimated print time is restored through a manual fallback parser
* Android-owned Preview geometry generation is the wrong boundary and must not
  be revived
* the attempted full native preview mesh export was rejected in its current
  form after phone-side memory failure
* the current working Preview path is native-owned:
  * Orca/native code owns G-code preview interpretation and render data
  * Android owns the shell, bed/grid, layer controls, and draw submission
  * filtering stays render-time only through native visible-layer ranges
  * the accepted layer-scrub behavior is now:
    * drag updates the visible native layer range without recreating the
      `libvgcode` viewer
    * release performs one final filtered native Preview reload for the selected
      range
    * filtered windows track their global layer offset so drag updates map into
      native local layer IDs correctly
    * large-model special Preview paths, live native reloads during drag, and
      the two-entry viewer-handle cache are rejected after device review
* preserve the `README/GCODE_VIEWER.md` parser invariants:
  * compact layer IDs starting at `0`
  * no layer increment until the first extrusion vertex for that layer is
    emitted
  * `EMoveType::Noop` separator vertices at path breaks to prevent fake
    diagonal connectors
* current workspace lifecycle truth also includes:
  * retained workspace session across portrait/landscape recreation is restored
  * the staged STL must survive configuration change
  * plate object IDs and per-object transforms must survive normal add/select
    flows without falling back to initial placement
  * slicing after rotation must keep working by reloading the retained staged
    STL into the recreated native engine before slicing
  * saved projects must stay independent from transient cache cleanup because
    reopening a saved plate depends on app-private copied source files
  * deleting a saved project must remove both its persisted index entry and its
    app-private saved-project folder
  * Settings Support is now an app-native tab with free/open/ad-free wording,
    optional Ko-fi support, and no feature-unlock incentive
  * app branding now uses the `MobileSlicer.svg` gradient logo for the launcher
    drawable and untinted Home wordmark icon

### Stage 1: Finish the STL Workspace Baseline

Current focus until accepted:

* keep the current working STL-on-bed path stable
* improve device performance
* improve build-plate / bed-scene presentation
* tighten workspace chrome
* keep multi-object plate behavior stable enough for normal prepare workflows
* preserve:
  * correct controls
  * crash-free entry
  * actual STL visibility
  * printer-driven bed sizing
  * selected-object transforms that persist through object add/delete/select
    flows

Exit criteria for Stage 1:

* user confirms the workspace is stable on-device
* user confirms controls feel right
* user confirms the bed/workspace presentation is acceptable
* user confirms performance is good enough that the workspace no longer feels fragile

No broader feature work should outrun this baseline.

### Stage 2: Make Printer / Filament / Process Matter

This stage is now started. The next product tranche is not preview features.
It is the Android app profile system.

Current printer import truth:

* imported Orca printers replace local built-in printer defaults
* fresh installs show no printer until one is imported or custom-created
* `Select Printer` uses the generated Orca printer catalog with thumbnails and
  per-printer lazy import bundles
* selecting a multi-nozzle printer now prompts for a specific nozzle size before
  import; the imported printer profile represents that exact printer/nozzle
  variant
* an imported printer preserves raw model JSON, raw nozzle machine JSON,
  resolved nozzle machine JSON, and source chains
* printer import also imports the Orca process presets compatible with that
  exact printer/nozzle machine name; Process profiles are visible immediately
  for the selected imported printer, with no local `Built-in` badge or hidden
  picker step
* the `Process Presets` button opens a searchable immutable-preset picker over
  the selected printer/nozzle's stored Orca presets; tapping a preset adds a new
  user process copy and does not change the active process by itself
* that picker reloads from the generated Orca printer bundle, not from
  user-editable stored process cards, so user renames/edits cannot mutate the
  preset library
* importing a process preset shows a short confirmation toast and creates a
  separately editable user process copy
* active selection includes the selected printer nozzle, and printer cards show
  the selected nozzle; the temporary `Change Nozzle` action was removed because
  it created another printer variant instead of editing the current card
* local unit coverage now verifies imported printer, filament, and process
  profiles preserve their resolved Orca JSON strings; final native JSON
  pass-through should still get an instrumentation-level audit because Android
  `JSONObject.put` is not executable in the local JVM test runner
* each imported process preserves raw Orca process JSON, resolved process JSON,
  and the resolved source chain
* slicing config starts from the full resolved Orca machine JSON so every Orca
  printer key is retained, merges resolved Orca filament JSON, merges resolved
  Orca process JSON, and then overlays first-class MobileSlicer fields, so every
  imported process key is retained even before MobileSlicer has a dedicated
  control for that key
* first-class `PrinterProfile` fields are populated from resolved Orca keys for
  bed, nozzle, machine limits, host, G-code, fan, multimaterial, retraction,
  and related printer hardware controls

Deferred printer-import follow-ups:

* import audit screen/log
* import schema versioning/migration
* refresh existing imports idempotently
* debug raw Orca data viewer
* dedicated proof tests for nozzle, bed, G-code, process filtering, and
  pass-through keys

Current filament import truth:

* `Select Filament` is scoped to generic Orca filament material presets for now
* visible filament names are normalized to material names such as `ABS`, `PLA`,
  and `PETG`, without the `Generic` prefix
* the picker is a flat material list; source-family headings such as `Qidi`,
  `MagicMaker`, and `Ratrig` are intentionally not shown
* built-in/default filament cards are removed from visible profile state, like
  printers; an internal fallback remains only to keep app state safe before the
  user selects a filament
* each imported filament preserves raw Orca filament JSON, resolved filament
  JSON, and the resolved source chain
* native slice config starts from the resolved Orca printer JSON, merges the
  resolved Orca filament JSON, then overlays first-class MobileSlicer fields
* imported filaments use the same no-built-in-badge presentation direction as
  imported printers
* model import is guarded by selected profile state:
  * no printer and no filament: combined profile-required dialog
  * no printer only: printer-specific dialog
  * no filament only: filament-specific dialog
  * no selected process for the selected printer/nozzle: process-specific dialog
* the active-selection summary is dependency-aware:
  * when there is no valid selected printer, process text is hidden instead of
    showing an orphaned process from a previous printer
  * deleting the selected printer clears or rebinds the selected process to a
    valid remaining printer/process pair
* profile cards use `Duplicate` rather than `Duplicate As Custom` so the action
  fits the current mobile card layout

Current calibration truth:

* Home includes a `Printer Calibrations` card under the model-import card
* calibration entry uses the same profile-required guard as model import:
  printer, filament, and selected process are required
* the calibration picker lists the generic Orca calibration set:
  * Pressure Advance
  * Flow Rate
  * Temperature Tower
  * Max Volumetric Speed
  * VFA
  * Retraction
  * Input Shaping Frequency
  * Input Shaping Damping
  * Cornering
* selecting a calibration opens Workspace with calibration-job metadata and
  records that calibration overrides are temporary
* calibration overrides must layer on top of selected imported printer,
  filament, and process data for the calibration job only
* calibration overrides must not mutate the saved printer, filament, or process
  profile until the user explicitly saves a final calibration result
* calibration geometry now uses Orca-derived bundled calibration assets under
  `android-app/app/src/main/assets/calib_stl/`
* the native print target links Orca's real `libslic3r/calib.cpp`, so Pressure
  Advance line/pattern helpers are no longer Android stubs
* next calibration work should preserve any per-object 3MF calibration
  modifiers that Orca depends on, then add result-save mapping back into
  editable profiles one calibration at a time

Current verification for the profile picker/import tranche:

* `cd android-app && ./gradlew :app:compileDebugKotlin --rerun-tasks`
* `cd android-app && ./gradlew :app:assembleDebug`
* `cd android-app && ./gradlew :app:installDebug`
* `cd android-app && ./gradlew :app:testDebugUnitTest` currently crashes inside
  the host Temurin 17 JVM test executor before test assertions are reported;
  rerun after the local JVM/Gradle daemon state is clean
* latest install target: physical device `SM-S938U1 - 16`

Repo hygiene requirement before the next feature tranche:

* the working tree is intentionally dirty with active source/doc work
* generated ignored build/artifact directories were removed; only local tool
  state remains ignored (`.android-sdk/`, `.codex`)
* no normal untracked paths remain; new source/doc paths are intent-to-add
  review candidates, not cleanup debris:
  * `README/ORCA_MULTI_MATERIAL.md`
  * `README/WORKSPACE_AUTO_ORIENT_ARRANGE.md`
  * `README/WORKSPACE_VIEWER_IMPROVEMENT_PLAN.md`
  * `android-app/app/src/main/java/com/mobileslicer/profiles/PrinterMaterialSlotPreferences.kt`
  * `android-app/app/src/main/java/com/mobileslicer/viewer/GcodePreviewDisplayMode.kt`
  * `android-app/app/src/main/java/com/mobileslicer/viewer/GcodePreviewPerformanceMode.kt`
* source-owned work should be reviewed and committed in coherent buckets; use
  `DIRTY_TREE_GUIDE.md`
* `scripts/verify_android.sh local` passed after cleanup; rerun before final
  commit/handoff if additional source changes are made

Focus:

* keep `Profiles` as the owner of slicer configuration UX
* connect the selected Printer / Filament / Process choices more meaningfully to the active Android app flow
* start with the smallest honest application path:
  * selected printer affects bed dimensions and workspace defaults
  * selected process affects bounded real native slice inputs where current architecture allows it
  * filament and printer hardware settings stay explicitly app-layer-only until the native path truly supports them, then move one bounded cluster at a time into honest `Config only` / `Device tested` status
* do not explode into full desktop-level settings yet

Goal:

* move `Profiles` from mostly UI/session groundwork toward real product behavior
* expand toward Orca-style `Printer` / `Filament` / `Process` settings coverage one bounded cluster at a time
* count a settings cluster as progress only when it is editable, persisted, and produces real verified slice/output behavior where applicable
* before broader UI/product feature expansion resumes, finish the Orca-settings coverage push tracked in `README/SETTINGS_CHECKLIST.md`
* do not surface settings or speed controls for a lane the app does not yet honestly own or emit on the Android path
* keep speed work focused on explicit Orca-owned per-feature controls; the old app-owned fallback speed control has been retired
* Orca printer import is now an active Stage 2 foundation item:
  * the old curated built-in printer list is being replaced by user-imported
    Orca printer presets
  * fresh installs should show no visible printer presets until the user adds
    one through `Select Printer`
  * `New Custom Printer` remains the only manual/custom printer path
  * imported Orca printers must preserve raw/resolved Orca data, not just a
    small lossy field subset
  * initial printer import must include name, family, thumbnail, nozzle list,
    bed width/depth from resolved `printable_area`, and max height from
    resolved `printable_height`
  * full printer-data translation should proceed field-by-field through
    `README/OrcaProfileMappingMatrix.md`

Temporary parallel detour rule:

* a separate bounded STL import / workspace-open performance pass may run in parallel with Stage 2 settings work
* that pass exists only to reduce current complex-geometry load latency
* it must stay confined to the model import/load/workspace-prep path
* it must not broaden into G-code viewer work, renderer redesign, or general UI redesign
* once that bounded pass lands, it must return control directly to the Orca-settings rollout instead of creating a new active milestone
* if both lanes need `MainActivity.kt`, ownership must stay split between:
  * settings/editor logic
  * workspace import/load/viewer-prep logic
* current bounded detour outcome:
  * import no longer blocks on the second Android-side STL parse before native model acceptance
  * `Workspace` now opens only after native load completes and the model is ready to see
  * `Workspace` may still briefly show an explicit preparing state while deferred viewer mesh prep and first-frame upload finish
  * the renderer no longer builds a duplicate translated vertex array before GPU upload
  * the binary STL parser hot path was rewritten to remove tiny per-float reads and per-triangle allocation churn
  * the native STL load path now uses the direct STL loader instead of the slower generic model file reader
  * local host benchmark over `_quarantine/root-dependency-dump/data/meshes/pig.stl` measured viewer prep dropping from `182.28 ms` to `2.25 ms`, with an `81.02x` parser speedup
  * user-reported device timings on `RFCYA01ANVE` for `selected-model-3DBenchy.stl` dropped from `First frame 21766 ms` with `Viewer prep 18509 ms` to `First frame 3166 ms` with `Viewer prep 119 ms`
  * after the native debug-build optimization correction, the user also
    reported that STL render became much faster
  * prior `Native 2.9-3.1 s` import/load timing should now be treated as an
    old unoptimized-debug measurement until a fresh post-fix timing is pulled
  * the next bounded supervisor step after this closed detour is to return directly to the active Orca-settings lane

Exit criteria for Stage 2:

* user-visible proof that profile choices matter in the app beyond labels alone
* no contract churn beyond what is justified
* docs honestly reflect what settings are actually applied versus only selected in UI
* settings expansion is happening in verified tranches instead of accumulating placeholder controls

Immediate Stage 2 foundation now in scope:

* imported Orca printer collections plus manual custom printer collections in
  the Android app layer
* bounded Orca-style grouped editors for Printer / Filament / Process
* local persistence and active-profile restoration
* printer profile edits affecting workspace bed dimensions
* controlled one-variable slice regression checks so profile-to-G-code claims stay proven instead of assumed
* expose already-supported Process controls in the app surface when they are still missing from the editor, such as `Initial layer height`
* next milestone direction:
  * bring Orca-style `Printer`, `Filament`, and `Process` settings into the app cluster by cluster
  * verify each cluster with real device/output evidence before treating it as complete
  * keep `README/SETTINGS_CHECKLIST.md` current so coverage is accountable instead of implied

### Stage 3: Expand Orca-Style Settings Coverage

After the workspace baseline is accepted and Stage 2 has established the
profile-system foundation, the next priority is broader Orca-style settings
coverage before a dedicated optimization phase.

Focus:

* expand `Printer` settings coverage in bounded verified clusters
* expand `Filament` settings coverage in bounded verified clusters
* expand `Process` settings coverage in bounded verified clusters
* for each cluster:
  * add the editor surface
  * persist it cleanly
  * wire it into real behavior where supported
  * verify it with real output/device evidence before counting it as done

Goal:

* move toward full Orca-style settings/function coverage without filling the app with placeholder-only controls
* treat future Orca profile import/export as a later follow-on that should start only once the native settings surface is broad enough to support honest low-loss translation

Exit criteria for Stage 3:

* `Printer`, `Filament`, and `Process` settings are expanding cluster by cluster with real verified effect
* docs stay honest about which settings are:
  * editable only
  * app-layer only
  * truly part of real slicing behavior

### Stage 4: Expand Workspace Model Operations

After the settings/function milestone is materially farther along, add the next
practical workspace features.

Focus candidates:

* fit-to-bed / reset view
* one or two bounded model actions appropriate for a mobile slicer workspace
* stronger model-state presentation
* clearer workspace affordances tied to slicing workflow

Goal:

* make Workspace feel like a useful pre-slice working surface, not only a passive viewer

Exit criteria for Stage 4:

* the workspace supports a few meaningful, user-visible model operations
* the app still feels stable on-device

### Stage 5: Slice Results / Estimates / Workflow Tightening

Before advanced preview and before a dedicated optimization phase, tighten the
actual slicing workflow.

Focus:

* make slicing feedback/results more product-grade
* expose bounded useful result data if already available through current architecture
* improve the slice -> export/share loop so it feels intentional and not merely wired
* preserve honesty around what settings are actually applied

Goal:

* strengthen the real slicer workflow before visualization and optimization work becomes the main focus

Exit criteria for Stage 5:

* slice flow feels coherent
* user can understand what happened after slicing without raw/debug-style messaging

### Stage 6: Optimization And Preview Features After The Above

Only after the prior stages are materially complete should the project spend
real effort on:

* dedicated slicing/runtime optimization
* G-code preview
* layer view
* richer slice-result visualization
* potentially broader workspace visualization features

These are explicitly later-stage features, not immediate next work.

Reason:

* feature/function coverage is now the priority before a dedicated optimization phase
* optimization is more useful once the major real settings/function surface exists
* preview work is still easy to overbuild on top of a shaky product shell

### Comparison-Driven Adoption Plan

The Slice Beam comparison is useful as a product benchmark, but it should not
turn into blind copying. The main lessons to adopt are about product coverage
and workflow completeness, while preserving Mobile Slicer's stronger wrapper
separation and proof discipline.

Observed areas where Slice Beam is currently ahead:

* broader workspace model operations:
  * selection
  * move on bed
  * scale
  * rotate
  * auto-orient
  * flatten to face
  * arrange
* broader settings coverage driven from slicer-native config metadata
* real G-code viewer / preview path
* direct printer-send workflow
* `STEP` import through OpenCascade / OCCT

Observed areas where Mobile Slicer should keep its current advantage:

* stricter wrapper isolation from upstream Orca internals
* explicit proof classifications and UI truth labels
* bounded parity checks against reference Orca output
* tighter mobile-first restraint instead of dumping desktop surfaces directly

Adoption order for Mobile Slicer:

1. Finish the current core first
   * keep expanding settings in bounded verified clusters
   * keep the Orca parity harness alive as more fields land
   * do not lose the current truth/proof discipline while chasing feature breadth
2. Turn `Workspace` into a real working surface
   * add selection, delete, move-on-bed, scale, rotate, auto-orient, flatten, and arrange in bounded passes
   * treat this as the biggest user-facing product gap after settings correctness
3. Move toward metadata-driven settings expansion
   * do not hand-wire every Orca field forever
   * build a field-definition layer that can carry:
     * label
     * domain / section
     * input type
     * allowed values
     * backend key
     * proof label
   * keep the visible mobile surface curated even if the backing metadata grows broader
4. Add bounded slice-results / preview support
   * start with result summary, estimates, layer count, warnings, and bounded inspection tools
   * first bounded step now landed:
     * emitted-G-code-derived post-slice details in the success dialog and workspace dock
     * current scope is still metadata/trust only, not toolpath rendering
   * leave full desktop-like preview ambitions for later
5. Add direct printer workflow
   * start with simple local-network upload/send flows such as OctoPrint or Moonraker/Klipper
   * keep the first pass narrow:
     * saved printer host
     * upload sliced file
     * optional start-print action
     * clear success/failure feedback
6. Add `STEP` import later
   * treat `STEP` as a valuable later differentiator, not an immediate blocker
   * keep it isolated as a dedicated import subsystem if adopted
   * do not let it outrun core Android slicer correctness and workflow completion

How Mobile Slicer should try to become the best mobile slicer:

* win on workflow, not raw feature count
  * import
  * inspect
  * adjust a few meaningful settings
  * slice
  * preview/send/export
* stay fully local
* keep the UI clearly touch-first instead of becoming a compressed desktop clone
* treat proof/truth as a product feature:
  * if a setting is supported, it should be editable, classified, and honestly explained
* optimize for useful phone workflow, not only absolute benchmark speed
  * clear progress, cancellation, result clarity, and printer-send convenience matter
* eventually add one or two genuinely mobile-native convenience wins:
  * quick-print presets
  * recent model + printer combinations
  * import-from-share flow
  * one-tap reuse of the last known good setup

### Rules For Future Coder Runs

* take only one bounded step at a time
* do not reopen viewer architecture churn unless the current workspace regresses badly
* do not jump ahead to preview or optimization work while the current feature/settings stage is still unaccepted
* prioritize full Orca `Printer` / `Filament` / `Process` settings coverage before broader UI/features
* keep `Profiles` as the owner of slicer configuration
* keep app `Settings` as app-only preferences
* keep vendor / engine-wrapper / Android separation intact
* keep current workspace/result wording honest while preview is still absent:
  * the STL-on-bed surface is not a wall/shell toolpath preview
* update docs only to reflect verified truth

### Immediate Recommendation

The project has now started Stage 2.

So the immediate coder work should stay inside:

* profile-system UX refinement
* tightening which profile fields are actually proven to affect G-code
* one-variable-at-a-time slice regression checks using exported G-code comparisons
* expanding Orca-style settings coverage only in bounded verified tranches, not by shipping placeholder-only fields
* the current compact Stage 2 classification on `RFCYA01ANVE` is:
  * Printer:
    * `bed dimensions` -> `Error-state only`
    * `nozzle_diameter` -> `Device tested`
    * `filament_diameter` -> `Device tested`
  * Filament:
    * `filament_type` -> `Config only`
    * `filament_max_volumetric_speed` -> `Device tested`
    * `first_layer_nozzle_temperature` -> `Start-sequence only`
    * `other_layers_nozzle_temperature` -> `Layer-change command only`
    * `first_layer_bed_temperature` -> `Start-sequence only`
    * `other_layers_bed_temperature` -> `Layer-change command only`
    * `cooling_baseline` -> `Fan-command only`
    * `no_cooling_for_first_x_layers` -> `Fan-command only`
  * Process:
    * `layer_height` -> `Device tested`
    * `first_layer_height` -> `Device tested`
    * `first_layer_print_speed` -> `Device tested`
    * `top_shell_layers` -> `Device tested`
    * `bottom_shell_layers` -> `Device tested`
    * `seam_position` -> `Device tested`
    * `precise_outer_wall` -> `Device tested`
    * `only_one_wall_top` -> `Stronger-fixture proven`
    * `top_surface_pattern` -> `Stronger-fixture proven`
    * `wall_loops` -> `Stronger-fixture proven`
    * `skirts` -> `Device tested`
    * `brim_width` -> `Device tested`
    * `sparse_infill_density` -> `Device tested`
    * `sparse_infill_pattern` -> `Device tested`
* special-case boundaries that stay explicit in this stage:
  * `bed dimensions` stay `Error-state only`: they reach Orca as `printable_area` / `printable_height`, and the fresh typed-extra rerun in this review on `RFCYA01ANVE` used `/data/local/tmp/mobileslicer_test_cube.stl` staged to `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`; baseline exported `/sdcard/Download/stage2_beddims_baseline.gcode`, while the `10 x 10 x 10 mm` variant failed before export with `nativeError=printable volume exceeded via fallback check areaExceeded=1 heightExceeded=1 offendingLine=43 gcode="G1 X17.14 Y2.86 E.8648 ; perimeter"`
  * `filament_type` remains `Config only` as Orca `filament_type`; the current path does not load a type-specific filament preset bundle from that field alone
  * `first_layer_print_speed` is now `Device tested`; the current path already reaches Orca `initial_layer_speed`, and the accepted `mobileslicer_test_cube.stl` `100 -> 10 mm/s` proof on `RFCYA01ANVE` changes emitted first-layer feedrates while geometry stays equal after feedrate stripping
  * `initial_layer_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `75 -> 15 mm/s` proof on `RFCYA01ANVE` changes first-layer `Bottom surface` feedrate from `G1 F4500` to `G1 F900` while feedrate-stripped motion stays equal
  * `initial_layer_travel_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `50% -> 100%` proof on `RFCYA01ANVE` changes first-layer travel feedrate from `G1 ... F3600` to `G1 ... F7200` while feedrate-stripped motion stays equal
  * `slow_down_layers` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `0 -> 3` proof on `RFCYA01ANVE` changes emitted early-layer feedrates above layer 1, including second-layer `Inner wall` / `Outer wall` `G1 F6000 -> F3200`
  * `filament_max_volumetric_speed` is now `Device tested`; the accepted gyroid `mobileslicer_test_cube.stl` rerun on `RFCYA01ANVE` with fixed high-speed Process overrides changes emitted non-first-layer feedrates from capped `G1 F8843.491` to `G1 F15000` while feedrate-stripped motion stays equal
  * the bounded per-feature speed subset is now explicit app-owned Stage 2 coverage:
    * `outer_wall_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `100 -> 30 mm/s` proof on `RFCYA01ANVE` changes emitted `Outer wall` feedrates from `G1 F6000` to `G1 F1800` while normalized motion stays equal
    * `inner_wall_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `100 -> 30 mm/s` proof on `RFCYA01ANVE` changes emitted `Inner wall` feedrates while normalized motion stays equal after removing feedrate-only lines
    * `top_surface_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `100 -> 20 mm/s` proof on `RFCYA01ANVE` changes emitted `Top surface` feedrates from `G1 F6000` to `G1 F1200` while feedrate-stripped motion stays equal
    * `travel_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `120 -> 240 mm/s` proof on `RFCYA01ANVE` changes emitted non-first-layer travel feedrates from `G1 ... F7200` to `G1 ... F14400` while feedrate-stripped motion stays equal
  * the bounded bridge / small-perimeter lane is now explicit app-owned Stage 2 coverage:
    * `bridge_speed` is now `Device tested`; the accepted `stage2_bridge_speed_fixture.stl` `10 -> 40 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Overhang wall` and `;TYPE:Bridge` feedrates from `G1 F600` to `G1 F1807` while feedrate-stripped motion stays equal
    * `small_perimeter_threshold` is now `Device tested`; the accepted stronger `stage2_small_perimeter_array_fixture.stl` `0 -> 20 mm` proof on `RFCYA01ANVE` activates emitted small-feature perimeter handling and changes `;TYPE:Inner wall` / `;TYPE:Outer wall` feedrates from `G1 F6000` to `G1 F600`
    * `small_perimeter_speed` is now `Device tested`; once the threshold path is active on that same stronger fixture, the accepted `10 -> 50 mm/s` rerun changes those emitted small-feature perimeter feedrates from `G1 F600` to `G1 F3000`
  * the bounded infill / internal-solid / gap-infill lane is now explicit app-owned Stage 2 coverage:
    * `sparse_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Sparse infill` feedrates from `G1 F1200` to `G1 F3822` while feedrate-stripped motion stays equal
    * `internal_solid_infill_speed` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Internal solid infill` feedrates from `G1 F1200` to `G1 F4800` while feedrate-stripped motion stays equal
    * `gap_infill_speed` remains `Config-labeling-only effect`; current dedicated strip-fixture reruns echo config correctly but still do not emit a `Gap infill` block or change executable motion
  * the bounded wall / top-surface / sparse-infill acceleration lane is now explicit app-owned Stage 2 coverage:
    * `outer_wall_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` proof on `RFCYA01ANVE` removes the emitted `M204 S500` reset before `;TYPE:Outer wall` blocks while acceleration-stripped motion stays equal
    * `inner_wall_acceleration` is now `Device tested`; the accepted stronger `stage2_inner_wall_acceleration_tall_box_fixture.stl` `1000 -> 500 mm/s²` proof on `RFCYA01ANVE` changes emitted `M204` immediately before `;TYPE:Inner wall` from `M204 S1000` to `M204 S500` while acceleration-stripped motion stays equal
    * `top_surface_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 2000 mm/s²` rerun changes emitted `;TYPE:Top surface` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
    * `sparse_infill_acceleration` is now `Device tested`; the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` rerun changes emitted `;TYPE:Sparse infill` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
  * `top_surface_pattern` now reaches Orca `top_surface_pattern`; the older cached `selected-model-100ml_cube.stl` proof was overclaimed, but the accepted flat-roof `mobileslicer_test_cube.stl` `monotonicline -> concentric` proof on `RFCYA01ANVE` still changes stripped executable output and motion-only body while setup/start commands stay equal, so it remains `Stronger-fixture proven`; the narrower missing-final-`Top surface` bug on that concentric fixture is now fixed on the real Android path, but the restored top-surface block still drifts from the bounded Orca probe
  * `only_one_wall_top` is now explicit bounded Stage 2 `Process` coverage: the app persists `Only one wall on top surfaces`, sends it as Orca `only_one_wall_top`, and the accepted `mobileslicer_test_cube.stl` `true -> false` proof on `RFCYA01ANVE` changes the final roof-region `Inner wall` / `Outer wall` / `Top surface` picture; keep that truth at `Stronger-fixture proven` and do not inflate it into a same-config Orca parity claim
  * the interactive STL import blocker that was masking wall-validation is now narrowed and fixed:
    * valid ASCII STL files were dying during binary-format sniffing, not because `wall_loops` was broken
    * the restored interactive DocumentsUI path now imports, slices, and exports `wall_smoke_box_20mm.stl` on `RFCYA01ANVE`
    * the remaining wall-trust gap is still the mesh-only result surface, not a newly proven backend wall-count bug
  * `no_cooling_for_first_x_layers` is now `Fan-command only`; the bounded `mobileslicer_test_cube.stl` `1 -> 3` proof on `RFCYA01ANVE` keeps motion equal but delays the first emitted `M106 S255 ; enable fan`
  * `wall_loops` remain `Stronger-fixture proven`; the exact reproduced missing-wall cube on `RFCYA01ANVE` is now fixed on the real export path, but that blocker came from wrapper default `wall_generator = arachne`, not missing `wall_loops` transport. The one-variable `wall_loops` proof itself still comes from stronger fixtures such as `Box_v2.stl`
  * current proof-harness caveat from this review:
    * raw `automation_config_json` shell-intent injection did not count when the shell mangled the JSON payload
    * accepted fresh reruns should prefer typed automation extras or another clearly valid one-variable path
    * keep the direct external `/sdcard/Download/...` raw-file automation staging limit noted as a separate narrow caveat; the accepted fresh rerun used `/data/local/tmp/...` source plus app-cache staging
  * repo-hygiene task still pending:
    * generated ignored build/artifact directories were removed during the
      2026-05-01 cleanup pass
    * `scripts/verify_android.sh local` passed after that cleanup, then the
      regenerated Gradle/CMake outputs were removed again
    * current remaining cleanup is source review/staging by the buckets in
      `DIRTY_TREE_GUIDE.md`, not generated-file deletion
  * `top_shell_layers` and `bottom_shell_layers` are now `Device tested`; bounded `mobileslicer_test_cube.stl` `4 -> 6` and `3 -> 5` proofs on `RFCYA01ANVE` changed executable motion while setup and non-motion layer-change comparisons stayed equal
  * `precise_outer_wall` is now `Device tested`; bounded cached `selected-model-wall_smoke_box_20mm.stl` `true -> false` proof on `RFCYA01ANVE` changed line count, `;TYPE:` sequence, stripped executable output, and motion-only body while setup/start commands stayed equal
  * `skirts` are now `Device tested`; current bounded `mobileslicer_test_cube.stl` `0 -> 3` proof on `RFCYA01ANVE` adds emitted skirt toolpaths
  * `seam_position` is now `Device tested`; the latest user-confirmed on-device result on `RFCYA01ANVE` is the accepted authority for this repo's current Stage 2 runtime truth
  * `brim_width` is now `Device tested`; the bounded Android fix now switches Orca `brim_type` explicitly and links real `Brim.cpp`, and the accepted `mobileslicer_test_cube.stl` `0 -> 4 mm` proof on `RFCYA01ANVE` emits a real `;TYPE:Brim` block plus changed executable motion
  * `sparse_infill_pattern` is now `Device tested`; the accepted fixed `mobileslicer_test_cube.stl` matrix on `RFCYA01ANVE` emits real sparse infill for `Grid`, `Gyroid`, and `Cubic`, and the exact Android-side root cause was a no-op `FillGyroid` stub replacing the real `FillGyroid.cpp` in the bounded print subset
  * bounded Orca-parity sanity check:
    * current tiny matrix over `mobileslicer_test_cube.stl` now shows `baseline`, `skirts`, and `layer_height` matching the reference Orca side for `;TYPE:` sequence, motion-only body, and setup-command body
    * current remaining narrow mismatch is machine-end-command-only drift from the seeded wrapper `machine_end_gcode` block, not startup/setup drift and not post-feature toolpath drift on those tested cases
* next bounded Process step for the supervisor:
  * profile-level fuzzy-skin Process config is now surfaced and bridged to Orca config as `Config only`
  * advanced cut/painting/multicolor work should first add durable project/session state and native `ModelVolume` facet annotation APIs
  * do not build seam/fuzzy/MMU painting as plain JSON process settings; Orca consumes them as `ModelVolume` facet annotations
* optimization remains a later milestone after much broader verified settings/function coverage

### Planned Orca-Style Settings Backlog

This backlog is the intended non-advanced Android settings surface pulled from
the recent Orca reference pass. It is not permission to bulk-import the whole
set at once. Keep importing one bounded cluster at a time, with the normal
proof and UI-truth rules.

Audit result:

* the latest bounded closed gaps are:
  * `Process > First layer height`
  * `Filament > First layer nozzle temperature`
  * `Filament > Other layers nozzle temperature`
  * `Filament > First layer bed temperature`
  * `Filament > Other layers bed temperature`
* the screenshots and source pass also support the following staged backlog
* a few important additions were not prominent in the screenshots but still
  belong in the planned surface:
  * first-layer vs other-layer temperature splits
  * shell layer counts and key surface patterns
  * travel / initial-layer speed controls
  * basic support enable/type controls

Near-term target clusters:

* `Process`
  * `First layer height`
  * `Seam position`
  * `one-wall` toggles
  * shell thickness
* `Filament`
  * `No cooling for first X layers`

Medium-term target clusters:

* `Process`
  * `Only one wall on top surfaces`
  * `Only one wall on first layer`
  * `Top shell thickness`
  * `Bottom shell thickness`
  * `Top surface density`
  * `Bottom surface density`
  * `Top surface pattern`
  * `Bottom surface pattern`
  * `Fill multiline`
  * `Sparse infill pattern`
  * `Internal solid infill pattern`
  * `Initial layer speed`
  * `Initial layer infill speed`
  * `Initial layer travel speed`
  * `Slow layers`
  * `Fuzzy skin`
  * `Fuzzy skin thickness`
  * `Fuzzy skin point distance`
  * `Fuzzy skin first layer`
  * `Fuzzy skin generator mode`
  * `Fuzzy skin noise controls`
  * `Outer wall speed`
  * `Inner wall speed`
  * `Small perimeters speed`
  * `Small perimeters threshold`
  * `Sparse infill speed`
  * `Internal solid infill speed`
  * `Top surface speed`
  * `Gap infill speed`
  * `Support speed`
  * `Support interface speed`
  * `Travel speed`
* `Filament`
  * fan threshold pairs:
    * min fan speed + layer time
    * max fan speed + layer time
  * `Keep fan always on`
  * `Slow down for layer cooling`
  * `Don't slow down outer walls`
  * `Force cooling for overhangs and bridges`
  * overhang slowdown toggles
  * `Retraction length`
  * `Z-hop height`

Later / higher-risk clusters:

* `Support`
  * `Enable support`
  * `Support type`
  * `Support threshold angle`
  * `Support threshold overlap`
  * `On build plate only`
  * later, support-filament selectors
* `Filament`
  * `Softening temperature`
  * `Idle temperature`
  * recommended nozzle temperature range
  * `Chamber temperature`
  * `Activate temperature control`
  * `Enable pressure advance`
  * auxiliary fan controls
  * exhaust / air-filtration controls
  * `Long retraction when cut`

Sequencing rule for this backlog:

* do not jump straight to the riskiest controls just because Orca exposes them
* prefer settings that are:
  * easy to explain in mobile UI
  * likely to have a clear exported-G-code or runtime effect
  * narrow enough to prove honestly on `RFCYA01ANVE`
* leave support-heavy, machine-dependent, and preset-dependent controls for
  later bounded runs

### Proof Workflow Rules

Future settings work must also follow `README/PROOF_WORKFLOW.md`.
Future shipping/network/import work must also follow `README/SECURITY.md`.

Minimum repo discipline:

* classify every field under the correct domain:
  * `Printer`
  * `Filament`
  * `Process`
* classify every field under the correct proof state:
  * `Missing`
  * `Parent surfaced, subset only`
  * `Config only`
  * `Config only - Waydroid`
  * `Device tested`
  * `Start-sequence only`
  * `Layer-change command only`
  * `Fan-command only`
  * `Stronger-fixture proven`
  * `Error-state only`
* do not let truth-bearing files remain untracked
* triage dirty files before “cleaning Git”:
  * real source changes -> keep visible for review
  * tracked generated junk -> leave for a dedicated cleanup run
  * ignore candidates -> add to `.gitignore`
* do not count a device proof run if it used a stale APK or stale install
* do not treat one mapped Orca key as proof of a full preset bundle unless the current path actually loads that broader preset behavior
* keep UI truth wording no broader than the current proof class
* make the app itself show per-setting truth in the correct `Printer` / `Filament` / `Process` section once a field is classified, not only the docs
* app-facing per-setting status language should collapse to:
  * `Config only`
  * `Device tested`
* finer proof detail can stay in repo truth docs, but `README/SETTINGS_CHECKLIST.md` should remain the tracked coverage/accountability surface
* current bounded seam note:
  * `Process > Seam position` now survives into exported output as Orca `seam_position`
  * current repo truth treats the field as `Device tested` because the latest user-confirmed on-device result on `RFCYA01ANVE` is the accepted authority for runtime classification

Imported-Orca-setting definition of done:

* when an Orca setting is brought into Mobile Slicer, the run is not complete until:
  * the setting is placed in the correct `Printer` / `Filament` / `Process` section
  * the backend linkage is classified honestly
  * the UI reflects that classification
  * the docs reflect that classification
  * device proof is recorded when applicable

Do not jump to G-code/layer preview yet.

### Current Stage 2 Slice Verification Rule

When a coder run claims a bounded profile field changes native slicing, verify it with a controlled one-variable comparison:

* keep the same STL loaded
* duplicate the active process profile
* change exactly one field in the duplicate
* slice once with the baseline profile and once with the modified profile
* compare the exported `.gcode` outputs directly
* record exactly what changed:
  * file byte count
  * line count
  * `;Z:` layer markers for layer-height checks
  * `;TYPE:` or similar wall / infill sections when present
  * executable-body equality after stripping comment/config noise when the question is whether toolpaths really changed
* if a fixture only changes metadata or exported config comments while the stripped executable body stays identical, treat that STL as too weak for the claim and rerun the same workflow on a stronger bounded fixture
* if a field changes executable output only on stronger fixtures, document that as `Stronger-fixture proven` rather than implying the setting will always produce an obvious visible difference on every model

Use the checklist status system for live truth claims:

* `Config only`:
  * the field reaches the real native slice-input/config path
  * no accepted real-device proof claim yet
* `Device tested`:
  * accepted real-device evidence exists on `RFCYA01ANVE`
* proof qualifiers such as `not yet generalized as user-meaningful` still
  belong in detailed proof notes when needed

Waydroid may be used for app-flow smoke checks, but it is not final truth for current wall/infill regression claims when its local build path differs from the real `RFCYA01ANVE` `arm64-v8a` wrapper path.

## Historical Appendix

Everything below this heading is preserved execution history, milestone
checklists, and older proof framing. Keep it for auditability, but do not treat
it as the current project boundary ahead of `CURRENT_STATUS.md` and the staged
roadmap above.

## Current State

* [x] Build headless Orca engine
* [x] Create C wrapper
* [x] Integrate JNI bridge
* [x] Prove Android can load the wrapper library
* [x] Build Android validation APK
* [x] Validate Android JNI flow on Waydroid
* [x] Localize external Android SDK path into project directory
* [x] Model loading integrated through JNI
* [x] Load STL on Android
* [x] Android Orca core build + real model loading integrated
* [x] Replaced Android STL validation with real Orca model loading
* [x] First slicing call integrated on Android
* [x] Minimal controlled G-code generation integrated
* [x] G-code export/share flow completed
* [x] Minimal Android product loop validated in Waydroid
* [x] Shipping ARM runtime proof for the reduced shipping backend was achieved on `RFCYA01ANVE` (`arm64-v8a`) through the installed shipping APK (`com.mobileslicer`), including app load/slice/export flow.
* [x] Honest first shipping ARM runtime classification for the real `libslic3r`-backed wrapper path on `RFCYA01ANVE`
* [x] Fix the first shipping ARM real-wrapper native runtime crash during `Slice Model`
* [x] Prove shipping ARM real-wrapper slice completion
* [x] Prove shipping ARM real-wrapper export completion
* [x] Prove shipping ARM real-wrapper output artifact emission
* [x] Prove the shipping ARM emitted `.gcode` artifact is honestly real-wrapper output rather than reduced-backend synthetic output
* [x] Classify cleared-state shipping ARM rerun repeatability on `RFCYA01ANVE`
* [x] Trace the exact source of shipping ARM metadata drift in the real-wrapper / Orca-backed path
* [x] Fix the shipping ARM object-label metadata nondeterminism caused by uninitialized `PrintObject::m_id`
* [x] Isolate the shipping ARM `obj_1_Tassen Regal lang.stl` slice-time ANR cause and apply the minimum local fix
* [x] Classify repeatability of the no-ANR `obj_1_Tassen Regal lang.stl` shipping ARM run on `RFCYA01ANVE`
* [x] Broaden the shipping ARM matrix with one additional meaningful real staged fixture beyond the tetrahedron/cube/`obj1` set
* [x] Isolate the current shipping ARM picker/load-selection failure on `RFCYA01ANVE` that blocked the next new-fixture classification before load
* [x] Broaden the shipping ARM confidence set with one more meaningful real fixture beyond `ad_pig_real.stl`
* [x] Broaden the shipping ARM confidence set with one more meaningful local vendor STL fixture beyond `ae_sphere_real.stl`
* [x] Repeat the now-proven `af_goliath.stl` shipping ARM flow once from a cleared state to classify whether this added-confidence fixture is repeatable
* [x] Broaden the shipping ARM confidence set with one more meaningful local vendor STL fixture beyond `af_goliath.stl`
* [x] Repeat the now-proven `ag_vz330_bed.stl` shipping ARM flow once from a cleared state to classify whether this added-confidence fixture is repeatable
* [x] Narrow the strongest verified source-level cause boundary for `ag_vz330_bed.stl` executable-body drift and apply the smallest safe deterministic-order fix supported by evidence
* [x] Restore the minimum staged ARM dependency set, rebuild the shipping debug real-wrapper app, and runtime-verify the `PrintObject.cpp` deterministic-order fix against `ag_vz330_bed.stl`
* [x] Repeat the already-successful `ae_sphere_real.stl` shipping ARM flow from a cleared state and classify exact-fixture repeatability
* [x] Repeat the already-successful `est.stl` shipping ARM flow from a cleared state and classify exact-fixture repeatability
* [x] Repeat the already-successful `ad_pig_real.stl` shipping ARM flow from a cleared state and classify the exact repeatability boundary honestly
* [x] Narrow the strongest verified source-level cause boundary for `ad_pig_real.stl` executable-body drift using the preserved cleared-state shipping ARM evidence, without claiming a fix
* [x] Test one minimal source-only deterministic tie-break probe in `PrintObject::bridge_over_infill()` for `ad_pig_real.stl`, rebuild the shipping debug app, rerun on `RFCYA01ANVE`, and classify the result honestly
* [x] Test one minimal post-`diff()` `expansion_area` ordering probe in `PrintObject::bridge_over_infill()` for `ad_pig_real.stl`, rebuild the shipping debug app, rerun on `RFCYA01ANVE`, and classify the result honestly
* [x] Test one minimal `limiting_area` ordering probe in `PrintObject::bridge_over_infill()` for `ad_pig_real.stl`, rebuild the shipping debug app, rerun on `RFCYA01ANVE`, and classify the result honestly
* [x] Test one minimal clipped `bridging_area` ordering probe in `PrintObject::bridge_over_infill()` for `ad_pig_real.stl`, rebuild the shipping debug app, rerun on `RFCYA01ANVE`, and classify the result honestly
* [x] Run a clean-head upstream control test for `ad_pig_real.stl` on the non-interactive experimental Orca probe path and classify whether repeated nearest-equivalent upstream outputs also drift
* [x] Test one minimal subtraction/clipping probe on the clean-head non-interactive upstream control path for `ad_pig_real.stl` before promoting anything back to the shipping ARM app path
* [x] Promote the clean-head `union_safety_offset(bridging_area)` subtraction/clipping probe to the shipping ARM real-wrapper path for `ad_pig_real.stl`, rerun on `RFCYA01ANVE`, and classify the result honestly
* [x] Test one further minimal subtraction/clipping follow-up on the clean-head non-interactive upstream control path for `ad_pig_real.stl` after the first `union_safety_offset(bridging_area)` control probe, and classify whether it is worth promotion
* [x] Test one different minimal subtraction/clipping follow-up on the clean-head non-interactive upstream control path for `ad_pig_real.stl` that changes the `intersection(bridging_area, limiting_area)` clipper call instead of adding another late `union_safety_offset(bridging_area)` canonicalization, and classify whether it is worth promotion
* [x] Test one minimal subtraction-side follow-up on `diff(expansion_area, bridging_area)` on the clean-head non-interactive upstream control path for `ad_pig_real.stl`, and classify whether it is worth promotion
* [x] Test one minimal subtraction/clipping follow-up on `diff(bridging_area, total_top_area)` on the clean-head non-interactive upstream control path for `ad_pig_real.stl`, and classify whether it is worth promotion
* [x] Inspect the remaining call-level subtraction/clipping sites inside `PrintObject::bridge_over_infill()` on the clean-head non-interactive upstream control path for `ad_pig_real.stl`, and decide whether any one-toggle follow-up is still justified
* [x] Restore the documented clean-head control ARM dependency staging under `/tmp/orca-deps-install/*` and `/tmp/orca-deps-src/boost-1.84.0`, then rerun the blocked clean-head-only `expansion_area = intersection(expansion_area, deep_infill_area)` seed-shaping probe in `PrintObject::bridge_over_infill()` three times and classify it honestly before considering any different boundary
* [x] Review whether `area_to_be_bridge = intersection(area_to_be_bridge, deep_infill_area);` is the one remaining upstream-of-the-exhausted-toggle boundary inside `PrintObject::bridge_over_infill()` that still honestly earns a bounded clean-head-only probe after the mixed/regressive `expansion_area` seed-shaping result; stop the branch if that review does not support another single probe
* [x] Decide whether any bounded non-`PrintObject::bridge_over_infill()` upstream-core source probe is still honestly justified for `ad_pig_real.stl` after the completed clean-head control history
* [ ] Preserve the verified shipping ARM fixture matrix as stable project evidence: successful classified fixtures, repeatable exact-fixture cases, the legitimate rejection case, and the explicit `ad_pig_real.stl` succeeds/emits-output-but-not-repeatable classification, so future work does not depend on transient `/tmp` artifacts or on reopening speculative upstream-core probing
* [x] Replace the Android validation-style home screen with the first real product shell while preserving the existing wrapper-backed import/load/slice/export/share path and keeping engine architecture unchanged
* [x] Turn the `Profiles` placeholder into the first bounded product flow in the Android app layer with touch-first selection UX, session-persistent local presets, and home-screen reflection of the active choices, without changing the wrapper/JNI contract
* [x] Refine the home shell into a cleaner landing/configuration surface in the Android app layer by centering `Profiles`, slimming the `Printer / Filament / Process` tab treatment, and removing the home working-area section from the page layout, while keeping the future viewer workspace out of scope
* [x] Finish the first shell-fix pass in the Android app layer by removing the oversized home `P` badge, tightening the Profiles section, integrating the top-left logo treatment with the wordmark, and fixing theme-mode shell wiring so `System` / `Light` / `Dark` drive visible shell appearance changes
* [x] Refine the landing shell again in the Android app layer by converting `Settings` into a full-page app-settings screen, aligning Import and Profiles as matching islands, simplifying the import helper chips to `File Manager` and `STL / 3MF`, and keeping viewer/workspace work out of scope
* [x] Turn `Settings` into a bounded Android-app product flow with a single top-right entry point and app-only full-page preferences for theme mode, accent color, and legal/about, without changing the wrapper/JNI contract
* [x] Create the first minimal Android-app `Workspace` screen boundary, route successful model import into it, move visible current-model/slice/export/share controls out of Home, and remove the extra Import-island arrow icon without broadening into a real 3D or G-code viewer
* [x] Add the first real Android-app-side 3D model viewer to `Workspace` with a plate-like viewing surface and touch orbit / pan / zoom for imported STL files, while explicitly leaving `3MF` viewing and G-code/layer preview out of scope
* [x] Refine `Workspace` into a fuller full-screen model-working surface by making the viewer dominant, improving initial STL framing and plate presentation, and adding explicit loaded / empty / unsupported / error viewer states without starting G-code/layer preview work
* [x] Attempt the likeliest Android-app-side STL viewer runtime fix by replacing the naive `solid`-header ASCII detection with size-based binary STL detection, and reduce the remaining card-framed workspace feel by overlaying the model/action panel inside the workspace surface
* [x] Attempt the next Android-app-side STL viewer runtime fix after the parser heuristic change by removing the duplicate GL-thread STL reparse and reusing the already-prepared `StlMesh` for renderer buffer upload
* [x] Attempt the next Android-app-side STL viewer runtime fix after the parser and mesh-reuse changes by making the GL shader/setup path GLES-2-safe, validating shader/program creation explicitly, and surfacing precise viewer failure categories in-app
* [x] Attempt the next Android-app-side STL viewer runtime fix after parser, mesh-reuse, and shader/setup by keeping the renderer in continuous mode until the first successful frame after mesh handoff, and remove the remaining rounded/page-framed workspace shell so Workspace is closer to a true full-screen surface
* [x] Attempt the next Android-app-side STL viewer runtime fix after parser, mesh-reuse, shader/setup, and first-frame changes by explicitly binding the Compose-hosted `GLSurfaceView` lifecycle to `onResume()` / `onPause()`
* [x] Replace the current Android-app-side custom OpenGL STL viewer path with the smallest viable standard-`View` software-rendered STL workspace viewer, and tighten Workspace so it reads more like a viewer-first full-screen surface
* [x] Tighten `Workspace` chrome again so the viewer reads more like the true full-screen working surface and less like a screen containing a control panel
* [x] Replace the disqualified software/canvas viewer with one bounded Filament `SurfaceView` viewer path, prove the same Filament pipeline can render a tiny hardcoded sanity-check mesh first, then route the prepared imported STL mesh into that exact pipeline without JNI/wrapper contract changes
* [x] Replace the first crash-prone Filament entry path with a deferred safe-init path that keeps `TouchModelViewerView` constructor-time setup inert, removes runtime `filamat` compilation from the active device path, and converts managed Filament init/surface/render failures into bounded in-app viewer error state
* [x] Remove the failing Filament `gltfio` `UbershaderProvider` material dependency from the active viewer path, replace it with one bounded direct `filamat-android` unlit material setup, and keep the sanity-mesh-first proof split intact
* [x] Re-establish repo-to-device build truth for the Filament viewer by auditing current source and packaged APK contents for `gltfio` / `UbershaderProvider`, tracking the active viewer source in Git, and adding a temporary in-app `Workspace` build stamp
* [x] Narrow the active audited Filament blocker from stale setup ambiguity to the sanity-mesh first-draw preview path, and reduce the active `View` to a stricter no-post-processing single-preview configuration while keeping the build-truth stamp in place
* [x] Remove runtime Filament material compilation from the active Android app path, bake the minimal unlit viewer material as a prebuilt `.filamat` asset with matching host `matc`, and keep the sanity-mesh-first proof split intact
* [x] Restart the Android-app-side workspace viewer cleanly by discarding the active Filament path, restoring a bounded OpenGL ES 2 STL renderer, and rebuilding the scene around selected-printer bed dimensions, grounded model placement, and a slicer-style plate/grid workspace without widening JNI or wrapper contracts
* [x] Replace the later rejected `GLSurfaceView` workspace engine too by rebuilding `Workspace` around a dedicated `SurfaceView` render thread, one STL-on-bed mesh path, curated printer-bed sizing, GPU buffer reuse, and minimal slicer-style overlay chrome without widening JNI or wrapper contracts
* [x] Audit the new `SurfaceView` renderer as the final allowed bespoke attempt and apply only the minimum hardening for no-op recomposition churn, stale failure retry behavior, and invalid binary STL allocation guards before device validation
* [x] Get the Android-app-side STL viewer actually rendering on-bed on `RFCYA01ANVE` so the active blocker is no longer viewer existence
* [x] Apply the first bounded performance and workspace-chrome cleanup pass on the kept `SurfaceView` renderer without reopening renderer architecture or broadening feature scope
* [x] Correct the active workspace touch semantics so the current `SurfaceView` renderer now behaves acceptably on-device for drag and zoom
* [x] Confirm on-device that orbit / pan / pinch now feel correct enough to clear the touch-control blocker on the current STL-on-bed workspace
* [x] Apply the next bounded performance and workspace cleanup pass on the kept `SurfaceView` renderer without reopening renderer architecture or broadening feature scope
* [x] Apply the next bounded build-plate and workspace-dock refinement pass on the kept `SurfaceView` renderer without reopening renderer architecture or broadening feature scope
* [x] Apply the next bounded bed/performance follow-up on the kept `SurfaceView` renderer without reopening renderer architecture or broadening feature scope
* [ ] Confirm on-device that the latest bed/scene refinement feels more like a believable slicer plate view without regressing the now-working STL-on-bed path
* [x] Start Stage 2 by replacing the temporary `Profiles` picker with a real Android-app-side editable profile system with the older built-in/custom Printer / Filament / Process profile foundation, local persistence, and grouped editors
  * Superseded direction: visible printer presets now move to imported Orca printers plus manual custom printers, with no curated built-in printer list.
* [ ] Confirm on-device that the new `Profiles` screen supports create / duplicate / rename / edit / delete-custom flows cleanly and that printer bed edits visibly change workspace bed dimensions
* [x] Make `Profiles` show compact per-setting truth labels for the currently imported Orca-related fields and verify those labels live on `RFCYA01ANVE`
* [x] Refine the older Stage 2 profile-system foundation so built-in/custom behavior is clearer, active profile state is more visible, and numeric profile editors are cleaner on mobile
  * Superseded direction: imported Orca printer profiles replace built-in printer profiles.
* [x] Audit the real Android native slice-input path and stop overstating app-layer profile summaries where the JNI/wrapper path does not actually consume them
* [x] Wire the smallest bounded truthful slice-input tranche from `Profiles` into the current local `arm64-v8a` native slice path:
  * process layer height
  * process wall / perimeter count
  * process infill density
* [x] Reduce wasted `Preparing viewer` time on the Home -> Workspace path by parsing/caching the STL mesh at successful import time and reusing that prepared mesh on workspace entry instead of reparsing the same STL again
* [x] Add the first bounded post-slice result-details surface derived from emitted G-code so users are no longer left with only the raw mesh as the slice-result surface
* [ ] Confirm on-device that the refined `Profiles` UX feels clean, that native-slice versus app-only wording reads clearly, that process layer/wall/infill edits really change generated G-code, and that entering `Workspace` still feels faster after import

## What Works

* `android-app/` scaffolded with Kotlin, Compose, NDK, and CMake
* JNI bridge exposes `nativeCreateEngine`, `nativeDestroyEngine`, and `nativeLoadModel`
* Android native target is `liborca_engine.so`
* Compose screen supports STL selection and wrapper-backed native loading
* `./gradlew :app:assembleDebug` succeeds in `android-app/`
* Waydroid launch validation completed on `x86_64`
* Native model loading can be exercised repeatedly through the same engine handle
* Android SDK is exposed through project-local `.android-sdk`
* Android wrapper now links against `liborca_core_android.so`
* STL selection in Waydroid now loads through the real Orca-derived STL parser path
* Android UI now exposes a wrapper-backed first slice action with success/failure handling
* Export writes generated `.gcode` files through the Android document picker
* Share launches the Android chooser from the existing app flow
* The larger default-path fixture `obj_1_Tassen Regal lang.stl` now loads, slices, exports, and emits output on `RFCYA01ANVE`
* The fixed no-ANR result for `obj_1_Tassen Regal lang.stl` is now repeated once on the same shipping ARM path
* The additional staged real fixture `est.stl` now loads, slices, exports, and emits output on `RFCYA01ANVE`
* The additional staged real fixture `est.stl` is now repeatable for this exact shipping ARM fixture boundary with timestamp-only full-file drift and matching comment-stripped executable G-code
* The staged real fixture `ad_pig_real.stl` now loads, slices, exports, and emits output on `RFCYA01ANVE` when selected through the safer list-view picker path
* The staged real fixture `ad_pig_real.stl` still loads, slices, exports, and emits output on `RFCYA01ANVE`, but the cleared-state rerun is not repeatable for this exact fixture because the executable G-code body drifts beyond the generated-at header line
* The current strongest honest source-level cause boundary for `ad_pig_real.stl` is now narrower than the prior runtime-only classification:
  * no ANR or crash evidence accompanies the rerun drift
  * no perimeter drift was found in the full-file diff
  * the drift is concentrated in sparse infill, bridge, internal-bridge, internal-solid-infill, and some downstream top-surface sections
  * sorted executable-body comparison still differs, so this is not just final G-code emission ordering
  * the strongest current source boundary is order-sensitive internal bridge-over-infill candidate expansion in `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
* The one minimal source-only tie-break probe in the later `PrintObject::bridge_over_infill()` candidate ordering path has now been runtime-tested on `RFCYA01ANVE`
* That probe changed the `ad_pig_real.stl` executable-body drift but did not collapse it:
  * baseline stripped hash: `a81d2f489b966971590c8cc6d12acf426bd902cbc34e4db2b815d8349353eed8`
  * preserved rerun stripped hash: `f736f79ead0b70edcc03f9c39c36f817ea673370af39f74821f7574c4c37650f`
  * probed rerun stripped hash: `df9abc97ed282720eeaa78b7b01c2eca921f8d8cb120ed59caa6db5920311b49`
  * baseline versus probe stripped `diff -u` hunks: `107`
  * preserved rerun versus probe stripped `diff -u` hunks: `60`
  * honest classification remains: `ad_pig_real.stl` succeeds, emits output, and is not repeatable for this exact fixture
* The one minimal deeper source-only bridge-angle reuse probe in `PrintObject::bridge_over_infill()` has now also been runtime-tested on `RFCYA01ANVE`
* That deeper probe also changed the `ad_pig_real.stl` executable-body drift but did not collapse it:
  * deeper probed rerun full hash: `2f895c917c6a775f041624c28a24ab718c367df33e734efe6eba47b252b5ddcd`
  * deeper probed rerun stripped hash: `bfba0c3cee89d1318272b03987fa2c2e6a656522af374347a7fccd49864c6876`
  * baseline versus deeper probe stripped `diff -u` hunks: `80`
  * preserved rerun versus deeper probe stripped `diff -u` hunks: `63`
  * sorted executable-body hashes still differ across baseline, preserved rerun, tie-break probe, and deeper probe
  * honest classification remains: `ad_pig_real.stl` succeeds, emits output, and is not repeatable for this exact fixture
* The one minimal post-`diff()` `expansion_area` ordering probe in `PrintObject::bridge_over_infill()` has now also been runtime-tested on `RFCYA01ANVE`
* That `expansion_area` probe also changed the `ad_pig_real.stl` executable-body drift but did not collapse it:
  * probed rerun full hash: `9cfc2a4c3fc6deb20c49fbd3f53158526f51ab49a8edbb8a3eee7d5f54e2604f`
  * probed rerun stripped hash: `a5cb603e0f5353e3dffd704fb590be37e3dfad91dcf00cbbd6a9e70468c35317`
  * baseline versus `expansion_area` probe full `diff -u` hunks: `98`
  * preserved rerun versus `expansion_area` probe full `diff -u` hunks: `47`
  * preserved rerun versus `expansion_area` probe stripped `diff -u` hunks: `42`
  * sorted executable-body hashes still differ across baseline, preserved rerun, tie-break probe, deeper probe, and `expansion_area` probe
  * honest classification remains: `ad_pig_real.stl` succeeds, emits output, and is not repeatable for this exact fixture
* The one minimal `limiting_area` ordering probe in `PrintObject::bridge_over_infill()` has now also been runtime-tested on `RFCYA01ANVE`
* That `limiting_area` probe also changed the `ad_pig_real.stl` executable-body drift but did not collapse it:
  * probed rerun full hash: `b8f496a422ad6563ae55f99b6b341ef156826514cb6acfa805d479da8765afed`
  * probed rerun stripped hash: `2854c495a81735f71622e5899cff8115f3e9e2fc68b38ef1adb0b772be54904b`
  * baseline versus `limiting_area` probe full `diff -u` hunks: `54`
  * preserved rerun versus `limiting_area` probe full `diff -u` hunks: `141`
  * preserved rerun versus `limiting_area` probe stripped `diff -u` hunks: `135`
  * sorted executable-body hashes still differ across baseline, preserved rerun, tie-break probe, deeper probe, `expansion_area` probe, and `limiting_area` probe
  * honest classification remains: `ad_pig_real.stl` succeeds, emits output, and is not repeatable for this exact fixture
* The one minimal clipped `bridging_area` ordering probe in `PrintObject::bridge_over_infill()` has now also been runtime-tested on `RFCYA01ANVE`
* That clipped `bridging_area` probe also changed the `ad_pig_real.stl` executable-body drift but did not collapse it:
  * probed rerun full hash: `201310b112887ad31227b7725db61b074d76d34ad9bbf59d8e4b90b71d4e681d`
  * probed rerun stripped hash: `dd48b3969851a336741cc34013139e61286344246ada025596faaad9bd1fa949`
  * baseline versus clipped `bridging_area` probe stripped `diff -u` hunks: `38`
  * preserved rerun versus clipped `bridging_area` probe stripped `diff -u` hunks: `120`
  * `limiting_area` probe versus clipped `bridging_area` probe stripped `diff -u` hunks: `30`
  * sorted executable-body comparison still differs across all seven preserved outputs
  * honest classification remains: `ad_pig_real.stl` succeeds, emits output, and is not repeatable for this exact fixture
* A clean-head upstream control test has now also been run for `ad_pig_real.stl` using the non-interactive experimental Orca `Print + GCode` probe:
  * exact control path:
    * detached clean-head worktree `/tmp/mobileslicer-upstream-control`
    * commit `343772d6`
    * target `orca_android_libslic3r_print_gcode_probe`
  * exact fixture:
    * `_quarantine/root-dependency-dump/data/meshes/pig.stl`
  * exact nearest-equivalent config used:
    * `layer_height=0.2`
    * `first_layer_height=0.2`
    * `nozzle_diameter=0.4`
    * `filament_diameter=1.75`
    * `bed_temperature=45`
    * `print_temperature=200`
    * `travel_speed=120`
    * `perimeters=2`
  * exact unavoidable mismatch:
    * shipping MobileSlicer slices with empty JSON overrides on top of `DynamicPrintConfig::full_print_config()`
    * the control probe requires explicit keys and therefore only reaches a nearest-equivalent config, not proven exact parity
  * repeated control outputs still differ:
    * run1 full `sha256=ff0ea99c1759bcc3ec95f5df39c9a90b802d59d25de502a42c85cacb88076a1d`
    * run2 full `sha256=f1e23d819e596dda25673d89f93c5fb49dc837b7c9a73f0957b2a26d2826fe64`
    * run3 full `sha256=7dd1b6737c7f6bdaff6635bc434e16572ec71f9429303dbf0f32f168ebdb5c86`
    * run1 versus run2 stripped `diff -u` hunks: `82`
    * run1 versus run3 stripped `diff -u` hunks: `117`
    * run2 versus run3 stripped `diff -u` hunks: `120`
    * sorted stripped hashes still differ across all three control outputs
  * drift shape remains meaningfully similar to the shipping MobileSlicer drift:
    * executable-body drift, not header-only drift
    * no perimeter drift found
    * concentrated in bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * upstream control test
    * upstream also drifts
    * likely MobileSlicer-specific only: not supported by this control evidence
* One minimal subtraction/clipping probe has now also been tested on that clean-head non-interactive upstream control path:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact probe:
    * after `bridging_area = intersection(bridging_area, limiting_area)`, the clipped result is canonicalized once with `union_safety_offset(bridging_area)` before downstream subtraction mutates `expansion_area`
  * why this was the next smallest real closure step:
    * it directly targets the remaining subtraction/clipping boundary after the already-tested ordering probes
    * it reuses the same geometry-normalizing primitive that `construct_anchored_polygon(...)` already applies before later clipping re-fragments the result
  * exact repeated probe outputs still differ:
    * run1 full `sha256=4b764e048ff2a85f9b41fee4d743a318759dccb85e1d3e8b1d33187c13231c1c`
    * run2 full `sha256=e353496235138189366d29d721ff2898de4ab670872ee3f3930cc6c01ab0074a`
    * run3 full `sha256=9fbf0ec9bebd201205b3dd7af2658937f414c822ad63860fa7b17fedaf22f2eb`
    * run1 versus run2 stripped `diff -u` hunks: `38`
    * run1 versus run3 stripped `diff -u` hunks: `56`
    * run2 versus run3 stripped `diff -u` hunks: `51`
    * sorted stripped hashes still differ across all three probed control outputs
  * exact comparison versus the prior clean-head control:
    * prior stripped hunk counts were `82`, `117`, and `120`
    * the probed control reduced those to `38`, `56`, and `51`
    * the drift class changed materially but did not collapse
  * drift shape remains meaningfully similar:
    * executable-body drift
    * no perimeter drift
    * still concentrated in bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * subtraction/clipping probe
    * drift changed but remains
    * likely upstream-core involved
    * not proven exact same root cause
* That same `union_safety_offset(bridging_area)` subtraction/clipping probe has now also been runtime-tested on the shipping ARM real-wrapper path:
  * exact repo file changed:
    * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact probe:
    * immediately after `bridging_area = intersection(bridging_area, limiting_area)`, canonicalize once with `union_safety_offset(bridging_area)`
  * exact shipping ARM output:
    * `/tmp/mobileslicer-ad-pig-probe7/selected-model-ad_pig_real.gcode`
    * full `sha256=574f0e33233267707a62d64c7cf678f78eb5bc820fe6e860710543659911ed21`
    * stripped `sha256=3449ccb86f5b8f52afc16c6d1718b33f19e3d58a8813fd4d82a7d9cb2e544419`
    * `7121292` bytes
    * `218156` lines
  * exact comparisons:
    * baseline versus promoted subtraction/clipping probe stripped `diff -u` hunks: `67`
    * preserved rerun versus promoted subtraction/clipping probe stripped `diff -u` hunks: `128`
    * clipped `bridging_area` ordering probe versus promoted subtraction/clipping probe stripped `diff -u` hunks: `90`
    * sorted stripped hashes still differ across all eight shipping outputs
  * drift shape remains meaningfully similar:
    * executable-body drift
    * no perimeter drift
    * bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * `ad_pig_real.stl` succeeds
    * `ad_pig_real.stl` emits output
    * `ad_pig_real.stl` is not repeatable for this exact fixture
    * subtraction/clipping probe
    * drift changed but remains
* One further clean-head subtraction/clipping follow-up has now also been tested after that first control probe:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact follow-up probe:
    * after `bridging_area = diff(bridging_area, total_top_area)`, canonicalize once with `union_safety_offset(bridging_area)` before `expansion_area = diff(expansion_area, bridging_area)`
  * why this was the next smallest real closure step:
    * it stays inside the same subtraction / clipping region
    * it targets the final late geometry fragmentation path that still runs after the already-tested `intersection(bridging_area, limiting_area)` canonicalization
  * exact repeated follow-up outputs still differ:
    * run1 full `sha256=beaba75f8bd6d897009d3c00b5ae337ca102f33ebfedac3f385f4cce4ece753b`
    * run2 full `sha256=604cccf29d9d34badf8d3359c84cb7a62f3e3a33246ca49a0e382d30eaa3e5b6`
    * run3 full `sha256=6972d4495a5ce170c93391739e68d673a66344deb48247d5d388af7efc8fd23d`
    * run1 versus run2 stripped `diff -u` hunks: `69`
    * run1 versus run3 stripped `diff -u` hunks: `73`
    * run2 versus run3 stripped `diff -u` hunks: `124`
    * sorted stripped hashes still differ across all three follow-up outputs
  * exact comparison versus the prior clean-head probe:
    * prior stripped hunk counts were `38`, `56`, and `51`
    * this follow-up worsened the repeated-run profile to `69`, `73`, and `124`
  * drift shape remains meaningfully similar:
    * executable-body drift
    * no perimeter drift
    * bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * subtraction/clipping probe
    * drift changed but remains
    * likely upstream-core involved
    * not proven exact same root cause
    * not worth promotion to the shipping ARM app path yet
* One different minimal clean-head subtraction/clipping follow-up has now also been tested after that:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact follow-up probe:
    * `bridging_area = intersection(bridging_area, limiting_area, ApplySafetyOffset::Yes);`
  * why this was the next smallest real closure step:
    * probe1 showed the strongest control-path improvement at the same `intersection(bridging_area, limiting_area)` boundary
    * `intersection()` already supports `ApplySafetyOffset::Yes`, so this stays on the same subtraction/clipping boundary without adding another post-op `union_safety_offset(bridging_area)` canonicalization
    * it changes exactly one clipper-call parameter and does not stack a broader geometry rewrite
  * exact repeated follow-up outputs still differ:
    * run1 full `sha256=7c141cb92bf3291e6fdc7ff8ea1df638f10cd44db3656426011cac6c93d1aed9`
    * run2 full `sha256=b12bd43e663dfe31a3f15961799d648c68b6e746492b5111c4724f1703568a40`
    * run3 full `sha256=a2939b3def384f409cfecf8a48904291afabe4cf9c1ebf6d8ceb5a284b1f4dd1`
    * run1 versus run2 stripped `diff -u` hunks: `63`
    * run1 versus run3 stripped `diff -u` hunks: `128`
    * run2 versus run3 stripped `diff -u` hunks: `119`
    * sorted stripped hashes still differ across all three follow-up outputs
  * exact comparison versus preserved control evidence:
    * baseline repeated-run stripped hunk counts were `82`, `117`, and `120`
    * probe1 repeated-run stripped hunk counts were `38`, `56`, and `51`
    * probe2 repeated-run stripped hunk counts were `69`, `73`, and `124`
    * this follow-up produced `63`, `128`, and `119`
  * drift shape remains meaningfully similar:
    * executable-body drift
    * no perimeter drift
    * bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * subtraction/clipping probe
    * drift changed but remains
    * likely upstream-core involved
    * not proven exact same root cause
    * not worth promotion to the shipping ARM app path yet
* One subtraction-side clean-head follow-up has now also been tested on the paired `expansion_area` update:
  * exact file changed:
    * `/tmp/mobileslicer-upstream-control/vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact follow-up probe:
    * `expansion_area = diff(expansion_area, bridging_area, ApplySafetyOffset::Yes);`
  * why this was the next smallest real closure step:
    * the earlier post-probe1 follow-ups had all stayed on the `bridging_area` side
    * `diff()` already supports `ApplySafetyOffset::Yes`, so this directly targeted the paired subtraction-side boundary without adding another `union_safety_offset(bridging_area)` canonicalization
    * it changes exactly one subtraction-call parameter and does not stack a broader geometry rewrite
  * exact repeated follow-up outputs still differ:
    * run1 full `sha256=c90142024dc25cd46e99eb6c4762b4be33aae5634c39ca0afdfe614de53a1cb7`
    * run2 full `sha256=fdcca04b35d34505760a959d6007cfb4afcf0fd7f183d2e4737725e0c22a0e9b`
    * run3 full `sha256=64c1eac7a13ceb4d1479f126d26edb49f00af38d2b97594b32632bf355e739ed`
    * run1 versus run2 stripped `diff -u` hunks: `128`
    * run1 versus run3 stripped `diff -u` hunks: `104`
    * run2 versus run3 stripped `diff -u` hunks: `136`
    * sorted stripped hashes still differ across all three follow-up outputs
  * exact comparison versus preserved control evidence:
    * baseline repeated-run stripped hunk counts were `82`, `117`, and `120`
    * probe1 repeated-run stripped hunk counts were `38`, `56`, and `51`
    * probe2 repeated-run stripped hunk counts were `69`, `73`, and `124`
    * probe3 repeated-run stripped hunk counts were `63`, `128`, and `119`
    * this follow-up produced `128`, `104`, and `136`
  * drift shape remains meaningfully similar:
    * executable-body drift
    * no perimeter drift
    * bridge / infill / internal-bridge / internal-solid-infill / downstream top-surface sections
  * honest classification:
    * subtraction/clipping probe
    * drift changed but remains
    * likely upstream-core involved
    * not proven exact same root cause
    * not worth promotion to the shipping ARM app path yet
* The staged real fixture `ae_sphere_real.stl` now loads, slices, exports, and emits output on `RFCYA01ANVE`
* The staged real fixture `ae_sphere_real.stl` is now repeatable for this exact shipping ARM fixture boundary with timestamp-only full-file drift and matching comment-stripped executable G-code
* The staged vendor fixture `af_goliath.stl` now loads, slices, exports, and emits output on `RFCYA01ANVE`
* The staged vendor fixture `af_goliath.stl` is now repeatable for this exact shipping ARM path with timestamp-only full-file drift and matching comment-stripped executable G-code
* The staged vendor fixture `ag_vz330_bed.stl` now loads, slices, exports, emits output, and is now repeatable for this exact shipping ARM fixture boundary
* The current strongest verified source-level cause boundary for the prior `ag_vz330_bed.stl` drift is bridge-over-infill candidate ordering in `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
* The minimal deterministic ordering patch for those candidate surfaces is now runtime-verified on the proven shipping debug real-wrapper path
* The minimum staged ARM dependency set under `/tmp/orca-deps-install` and `/tmp/orca-deps-src` is restored enough for `:app:assembleDebug` to succeed again
* The current successful classified shipping ARM fixture set is:
  * `aa_matrix_tetra_20mm.stl`
  * `ac_matrix_cube.stl`
  * `est.stl`
  * `obj_1_Tassen Regal lang.stl`
  * `ad_pig_real.stl`
  * `ae_sphere_real.stl`
  * `af_goliath.stl`
  * `ag_vz330_bed.stl`
* The current repeatable exact-fixture set is:
  * `aa_matrix_tetra_20mm.stl`
  * `est.stl`
  * `ae_sphere_real.stl`
  * `obj_1_Tassen Regal lang.stl`
  * `af_goliath.stl`
  * `ag_vz330_bed.stl`
* The current legitimate default-path rejection set is:
  * `ab_matrix_tetra_small.stl`

## Partially Implemented

* The reduced native backend still exists in-tree as the historical/fallback path when the real-wrapper staging gate is not satisfied
* The current proven shipping ARM boundary on `RFCYA01ANVE` is no longer reduced-output; it emits real-wrapper / Orca-style G-code
* The smallest remaining closure step after classifying the `ad_pig_real.stl` repeatability blocker is now one exact cause-boundary investigation:
  * the shipping debug path still builds and installs again
  * `ag_vz330_bed.stl` remains repeatable for this exact fixture only after the verified `PrintObject.cpp` fix
  * `est.stl` is now also repeatable for this exact fixture only
  * `ae_sphere_real.stl` is now also repeatable for this exact fixture only
  * `ad_pig_real.stl` still succeeds end-to-end but is not yet repeatable for this exact fixture
  * the tested later tie-break probe changed the drift but did not collapse it
  * the tested deeper bridge-angle reuse probe changed the drift but did not collapse it
  * the tested post-`diff()` `expansion_area` ordering probe changed the drift but did not collapse it
  * the tested `limiting_area` ordering probe changed the drift but did not collapse it
  * the clean-head upstream control path also drifts under a nearest-equivalent config, so the current evidence no longer supports treating the blocker as likely MobileSlicer-specific only
  * the clean-head subtraction/clipping probe changed the repeated-run profile materially but still did not collapse the drift
  * the current strongest narrowed source-level suspect boundary is still the geometry-sensitive subtraction / clipping path inside `PrintObject::bridge_over_infill()`
  * probe1 remains the strongest clean-head subtraction/clipping signal so far
  * the direct `diff(bridging_area, total_top_area, ApplySafetyOffset::Yes)` follow-up also changed the drift but regressed materially overall versus probe1
  * the remaining call-level subtraction/clipping sites were inspected without patching
  * no one-toggle follow-up remained clearly justified after probes1 through probe5
  * the call-level subtraction/clipping-toggle branch now appears exhausted, so the next bounded step should shift to a different boundary inside `PrintObject::bridge_over_infill()` before any new shipping ARM promotion
* The current shipping ARM real-wrapper stability boundary is now honestly classified from a stopped small-matrix run on `RFCYA01ANVE`
  * keep this section as the detailed matrix/history ledger rather than duplicating the shorter current-state summary above
  * exact matrix actually executed before the first blocker:
    * run 1:
      * STL: `aa_matrix_tetra_20mm.stl`
      * config variant: shipping default path only
      * result: install pass, launch pass, load pass, slice pass, export pass
      * artifact: `/tmp/mobileslicer-matrix/run1/selected-model-aa_matrix_tetra_20mm.gcode`
      * `sha256=80b6c77cc80507019346357f772d6640b3a93a527c935a1b52557c66e255f1fb`
      * `370659` bytes
      * `13168` lines
    * run 2:
      * STL: `aa_matrix_tetra_20mm.stl`
      * config variant: shipping default path only
      * result: launch pass, load pass, slice pass, export pass
      * artifact: `/tmp/mobileslicer-matrix/run2/selected-model-aa_matrix_tetra_20mm.gcode`
      * `sha256=14d8bc67d034d15bc3084538a2140fe52841dadbb8dfcd32f2e57dcfc7d7e636`
      * `370659` bytes
      * `13168` lines
      * comparison: full-file hash differs by header timestamp only; comment-stripped hashes match at `90a0c6af40fe34a3ea0388c981425ddfee81aba9f15d9c73082ea17f759425e8`
    * run 3:
      * STL: `ab_matrix_tetra_small.stl`
      * config variant: shipping default path only
      * result: launch pass, load pass, slice fail, export not reached
      * artifact: none emitted
      * exact blocker text: `Slice failed`
      * expected output path missing: `/sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
      * crash buffer: empty
  * honest classification:
    * repeated single-case stable: yes
    * small-matrix stable: no
    * current blocker class: `slice`
    * current scope classification: general until proven ARM-specific
  * no meaningful config-variant matrix was exercised because the shipping app path currently exposes no config controls without contract churn
* The first stopped second-fixture blocker on `ab_matrix_tetra_small.stl` is now exactly isolated in the shipping ARM path
  * exact refined blocker classification:
    * class: `slice`
    * scope: general, not currently evidenced as ARM-specific
    * status: legitimate slicer rejection under current shipping defaults, not a crash bug
  * exact native log evidence from the shipping app rerun:
    * `/tmp/mobileslicer-ab-debug/ab-repro-focused.txt`
    * `E MobileSlicerNative: orca_slice: One object has an empty first layer and can't be printed. Please Cut the bottom or enable supports.`
  * exact UI evidence:
    * `/tmp/mobileslicer-ab-debug/ab-repro-postslice3.xml`
    * app still shows `Slice failed`
    * export/share remain disabled
  * exact output result:
    * `/sdcard/3dPrinting/selected-model-ab_matrix_tetra_small.gcode`
    * not emitted
  * exact crash result:
    * `/tmp/mobileslicer-ab-debug/ab-repro-crash.txt`
    * empty
  * exact source/config evidence used for this classification:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:1749`
      * Orca throws `One object has an empty first layer and can't be printed. Please Cut the bottom or enable supports.`
    * `engine-wrapper/orca_wrapper.cpp`
      * shipping wrapper uses `DynamicPrintConfig::full_print_config()` plus only `gcode_comments` and `start_gcode` overrides
      * no support-generation or cut-bottom override is applied in the current shipping path
    * `android-app/app/src/main/java/com/mobileslicer/MainActivity.kt`
      * shipping app exposes only `Select Model`, `Slice Model`, `Export G-code`, and `Share G-code`
      * no existing no-churn control enables supports or cut-bottom
    * `engine-wrapper/orca-android-libslic3r/testdata/tetrahedron_ascii.stl`
      * source geometry is a 1 mm tetrahedron, matching the staged `ab_matrix_tetra_small.stl` fixture
* The next staged fixture `ac_matrix_cube.stl` is now proven through the same shipping ARM path
  * exact staged path:
    * `/sdcard/3dPrinting/ac_matrix_cube.stl`
  * exact commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/ac_matrix_cube.gcode /sdcard/3dPrinting/selected-model-ac_matrix_cube.gcode`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input swipe 720 1400 720 2600 300`
    * `sleep 1`
    * `./tools/adb -s RFCYA01ANVE shell input tap 395 2070`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 5`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
  * exact result:
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-ac_matrix_cube.gcode`
      * `/tmp/mobileslicer-ac-matrix/selected-model-ac_matrix_cube.gcode`
      * `sha256=ecf93b99b79a402fcd036aaefc701b4fed4670f2f67567685fa4d6fe25b099bf`
      * `417425` bytes
      * `12370` lines
  * exact UI evidence:
    * `/tmp/mobileslicer-ac-matrix/ac-loaded-ui.xml`
      * `sha256=54b532080730966833112e62759ab3c762698859331e89a5a47dcae88d578fea`
      * `11033` bytes
      * contains `Model loaded successfully`
    * `/tmp/mobileslicer-ac-matrix/ac-sliced-ui.xml`
      * `sha256=a60a51292a1f7bcd026d05e442405f1b81205f6342ee50343261efa29eb8653f`
      * `10981` bytes
      * contains `Slice successful`
    * `/tmp/mobileslicer-ac-matrix/ac-exported-ui.xml`
      * `sha256=0b4a535ed0914355e10c7f41b7bf176c3b316d6fcb0ec21e75b45ce20886d99a`
      * `11023` bytes
      * contains `Export successful`
* A larger default-path shipping fixture now exposes a separate concrete blocker after the `aa` / `ab` / `ac` matrix boundary
  * that blocker is now isolated and minimally fixed in the same shipping ARM path
  * exact cause classification:
    * app-threading / responsiveness bug
    * `Slice Model` called `NativeEngineBridge.nativeSlice(...)` synchronously from the main thread
    * the UI thread waited inside JNI/native slice completion instead of yielding
  * exact file/function/thread boundary:
    * `android-app/app/src/main/java/com/mobileslicer/MainActivity.kt`
    * `ModelLoaderScreen` `onSliceModel`
    * `sliceCurrentModel()`
    * UI thread before the fix
  * exact minimal fix:
    * moved `onSliceRequested()` execution to `Dispatchers.Default`
    * kept the JNI/wrapper contract unchanged
    * added wrapper stage timing logs in `engine-wrapper/orca_wrapper.cpp`
  * exact rerun result after the fix:
    * launch: pass
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-obj_1_Tassen_Regal_lang.gcode`
      * `/tmp/mobileslicer-obj1-anr/selected-model-obj_1_Tassen_Regal_lang.gcode`
      * `sha256=62564d73014d2f37ed37d37cb1d08ccc950b20e6e0d71e8a545474e9ee5450df`
      * `24754690` bytes
      * `702556` lines
  * exact evidence:
    * `/tmp/mobileslicer-obj1-anr/obj1-during-slice.xml`
      * contains `Slicing...`
    * `/tmp/mobileslicer-obj1-anr/obj1-postslice.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-obj1-anr/obj1-exported.xml`
      * contains `Export successful`
    * `/tmp/mobileslicer-obj1-anr/obj1-logcat-app-fixed.txt`
      * shows `sliceCurrentModel:start thread=DefaultDispatcher-worker-1 isMain=false`
      * shows `orca_slice` stage timings through `export_gcode`
    * `/tmp/mobileslicer-obj1-anr/obj1-logcat-anr-fixed.txt`
      * contains no Mobile Slicer ANR lines
  * exact repeat rerun result for the same fixture after the fix:
    * launch: pass
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-obj_1_Tassen_Regal_lang.gcode`
      * `/tmp/mobileslicer-obj1-repeat/selected-model-obj_1_Tassen_Regal_lang.gcode`
      * `sha256=4709defa31952d630d6d16f285e793d4b56c85fb80081be8060af2d5475a8e89`
      * `24754690` bytes
      * `702556` lines
  * exact repeat-run evidence:
    * `/tmp/mobileslicer-obj1-repeat/obj1-repeat-during-slice.xml`
      * contains `Slicing...`
    * `/tmp/mobileslicer-obj1-repeat/obj1-repeat-postslice2.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-obj1-repeat/obj1-repeat-exported.xml`
      * contains `Export successful`
    * `/tmp/mobileslicer-obj1-repeat/obj1-logcat-app-repeat.txt`
      * shows `sliceCurrentModel:start thread=DefaultDispatcher-worker-2 isMain=false`
      * shows `orca_slice` stage timings through `export_gcode`
    * `/tmp/mobileslicer-obj1-repeat/obj1-logcat-anr-repeat.txt`
      * contains no Mobile Slicer ANR lines
  * exact repeatability classification for this fixture:
    * two fixed shipping ARM runs now complete load, slice, export, and output emission without ANR
    * full-file hashes differ only by the generated-at timestamp header
    * comment-stripped executable G-code matches exactly at `sha256=3a5fb853584bac9c5cbdce6a44ac74011ec7958f8032b7a2d491fe8a6383a704`
    * this proves repeatability for this exact fixture only, not broad larger-fixture stability
* One additional meaningful real staged fixture now broadens the current shipping ARM confidence boundary
  * chosen fixture:
    * `/sdcard/3dPrinting/est.stl`
    * staged size: about `2.13 MB`
  * exact commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-est.gcode /sdcard/3dPrinting/est.gcode`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1040 2380`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-loaded.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 8`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-postslice.xml`
    * `sleep 35`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-postslice2.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-export-dialog.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/est-exported.xml`
    * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-est.gcode /tmp/mobileslicer-est/selected-model-est.gcode`
  * exact result:
    * load: pass
    * slice: pass
    * export: pass
    * emitted output: pass
  * exact emitted artifact:
    * `/sdcard/3dPrinting/selected-model-est.gcode`
    * `/tmp/mobileslicer-est/selected-model-est.gcode`
    * `sha256=a6a53ce1f4158bb5708e0203a270cd85da38d8e715ea87fe3ff6c26430f9a43e`
    * `15690430` bytes
    * `523489` lines
  * exact evidence:
    * `/tmp/mobileslicer-est/est-loaded.xml`
      * contains `Model loaded successfully`
    * `/tmp/mobileslicer-est/est-postslice2.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-est/est-exported.xml`
      * contains `Export successful`
    * `/tmp/mobileslicer-est/est-logcat-app.txt`
      * shows `sliceCurrentModel:start ... isMain=false`
      * shows `process stageMs=19716`
      * shows `export_gcode stageMs=26168`
      * shows `sliceCurrentModel:end ... elapsedMs=46095 sliced=true`
    * `/tmp/mobileslicer-est/est-logcat-anr.txt`
      * contains no Mobile Slicer ANR lines
  * exact classification:
    * success
    * not a legitimate slicer rejection
    * not a concrete shipping bug
  * honest matrix classification after this run:
    * successful fixtures now include `aa_matrix_tetra_20mm.stl`, `ac_matrix_cube.stl`, `est.stl`, and `obj_1_Tassen Regal lang.stl`
    * known rejection under current defaults remains `ab_matrix_tetra_small.stl`
    * shipping confidence is broader, but still limited to the currently classified fixture set
* The previously inconclusive `ad_pig_real.stl` picker/load boundary is now honestly classified on `RFCYA01ANVE`
  * chosen fixture:
    * `/sdcard/3dPrinting/ad_pig_real.stl`
    * staged from `_quarantine/root-dependency-dump/data/meshes/pig.stl`
  * exact commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE push _quarantine/root-dependency-dump/data/meshes/pig.stl '/sdcard/3dPrinting/ad_pig_real.stl'`
    * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-ad_pig_real.gcode /sdcard/3dPrinting/ad_pig_real.gcode`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-picker.xml`
    * safer picker path:
      * `./tools/adb -s RFCYA01ANVE shell input tap 1272 722`
      * `sleep 2`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-list-picker.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 460 1685`
      * `sleep 3`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-post.xml`
    * continued after load proof:
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
      * `sleep 8`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-slice1.xml`
      * `sleep 40`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-slice2.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
      * `sleep 2`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-export-dialog.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
      * `sleep 3`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ad-pig-rerun-exported.xml`
      * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-ad_pig_real.gcode /tmp/mobileslicer-ad-pig-rerun/selected-model-ad_pig_real.gcode`
  * exact picker evidence:
    * grid dump:
      * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-picker.xml`
      * card bounds: `[734,1670][1356,2471]`
      * title bounds: `[909,2320][1222,2386]`
    * list dump used for selection:
      * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-list-picker.xml`
      * row bounds: `[0,1595][1440,1851]`
      * title bounds: `[252,1642][667,1729]`
      * preview icon bounds: `[1188,1595][1440,1847]`
      * exact tap used: `(460,1685)`
  * exact foreground/activity evidence:
    * before selection:
      * `topResumedActivity=ActivityRecord{267660849 u0 com.google.android.documentsui/com.android.documentsui.picker.PickActivity t10913}`
    * after selection:
      * `topResumedActivity=ActivityRecord{46489231 u0 com.mobileslicer/.MainActivity t10913}`
  * exact result:
    * picker opened: pass
    * selection attempted: pass
    * model loaded: pass
    * slice started: pass
    * export reached: pass
    * emitted output: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-ad_pig_real.gcode`
      * `/tmp/mobileslicer-ad-pig-rerun/selected-model-ad_pig_real.gcode`
      * `sha256=9074f126e0c0074373e8106c7a9d5a91a09723d86cd7dcc3ac2f9333f17dda67`
      * `7121341` bytes
      * `218158` lines
  * exact UI/log evidence:
    * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-post.xml`
      * contains `Model loaded successfully`
    * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-slice1.xml`
      * contains `Slicing...`
    * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-slice2.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-ad-pig-rerun/ad-pig-rerun-exported.xml`
      * contains `Export successful`
    * focused app log command:
      * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg "MobileSlicer|sliceCurrentModel|orca_slice|export"`
      * `process stageMs=16985 totalMs=17078`
      * `export_gcode stageMs=10381 totalMs=27459`
      * `sliceCurrentModel:end ... elapsedMs=27516 sliced=true`
    * crash buffer:
      * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
      * no output
  * exact classification:
    * model load was reached
    * the previous boundary is now classified as picker automation / tap-path ambiguity in `DocumentsUI`
    * not system-navigation interference as the final blocker classification
    * not an app-side load bug
    * not a slicer rejection
  * honest matrix classification after this run:
    * successful fixtures now include `aa_matrix_tetra_20mm.stl`, `ac_matrix_cube.stl`, `est.stl`, `obj_1_Tassen Regal lang.stl`, and `ad_pig_real.stl`
    * known rejection under current defaults remains `ab_matrix_tetra_small.stl`
    * broader confidence remains true only for the currently classified set
* One more meaningful real fixture now broadens the shipping ARM confidence set beyond the previously classified successful set
  * chosen fixture:
    * `/sdcard/3dPrinting/ae_sphere_real.stl`
    * staged from `_quarantine/root-dependency-dump/data/meshes/sphere.stl`
  * exact commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE push _quarantine/root-dependency-dump/data/meshes/sphere.stl '/sdcard/3dPrinting/ae_sphere_real.stl'`
    * `./tools/adb -s RFCYA01ANVE shell rm -f /sdcard/3dPrinting/selected-model-ae_sphere_real.gcode /sdcard/3dPrinting/ae_sphere_real.gcode`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-picker.xml`
    * picker was already in list view
    * accidental probe:
      * tapped search at `1188 197`
      * recovered with back tap `98 197`
      * re-dumped picker with `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-picker2.xml`
    * exact row/title interaction:
      * `ae_sphere_real.stl` title bounds `[252,1898][769,1985]`
      * preview bounds `[1188,1851][1440,2103]`
      * first title-center tap `(510,1941)` did not leave picker
      * second tap `(510,1941)` returned to the app
      * loaded-state dump: `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-post2.xml`
    * continued after load proof:
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
      * `sleep 6`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-slice1.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
      * `sleep 2`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-export-dialog.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
      * `sleep 3`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/ae-sphere-exported.xml`
      * `./tools/adb -s RFCYA01ANVE pull /sdcard/3dPrinting/selected-model-ae_sphere_real.gcode /tmp/mobileslicer-ae-sphere/selected-model-ae_sphere_real.gcode`
  * exact result:
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-ae_sphere_real.gcode`
      * `/tmp/mobileslicer-ae-sphere/selected-model-ae_sphere_real.gcode`
      * `sha256=c1f05edc480d08d1028ca53eafce68db775e7d8f4ca6aad656e80ae72530308c`
      * `21340` bytes
      * `698` lines
  * exact UI/log evidence:
    * `/tmp/mobileslicer-ae-sphere/ae-sphere-post2.xml`
      * contains `Model loaded successfully`
    * `/tmp/mobileslicer-ae-sphere/ae-sphere-slice1.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-ae-sphere/ae-sphere-exported.xml`
      * contains `Export successful`
    * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg "MobileSlicer|sliceCurrentModel|orca_slice|export"`
      * `process stageMs=51 totalMs=78`
      * `export_gcode stageMs=2919 totalMs=2998`
      * `sliceCurrentModel:end ... elapsedMs=3000 sliced=true`
    * crash buffer:
      * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
      * no output
  * exact classification:
    * success
    * not a legitimate slicer rejection
    * not a concrete shipping bug
    * not a picker/path blocker
  * honest matrix classification after this run:
    * successful fixtures now include `aa_matrix_tetra_20mm.stl`, `ac_matrix_cube.stl`, `est.stl`, `obj_1_Tassen Regal lang.stl`, `ad_pig_real.stl`, and `ae_sphere_real.stl`
    * known rejection under current defaults remains `ab_matrix_tetra_small.stl`
    * broader confidence increases, but this is still not an all-fixtures stability claim
* One more meaningful local vendor STL fixture now broadens the shipping ARM confidence set beyond the previously classified successful set
  * chosen fixture:
    * `/sdcard/3dPrinting/af_goliath.stl`
    * staged from `vendor/orcaslicer/resources/profiles/Vzbot/goliath.stl`
  * exact commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE push 'vendor/orcaslicer/resources/profiles/Vzbot/goliath.stl' '/sdcard/3dPrinting/af_goliath.stl'`
    * `./tools/adb -s RFCYA01ANVE shell rm -f '/sdcard/3dPrinting/selected-model-af_goliath.gcode' '/sdcard/3dPrinting/af_goliath.gcode'`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-picker.xml`
    * picker was already in list view
    * exact row/title interaction:
      * `af_goliath.stl` title bounds `[252,2154][614,2241]`
      * metadata bounds `[252,2255][680,2312]`
      * preview bounds `[1188,2107][1440,2359]`
      * title-center tap `(433,2197)`
      * loaded-state dump: `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-post.xml`
    * continued after load proof:
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
      * `sleep 8`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-slice1.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
      * `sleep 2`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-export-dialog.xml`
      * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
      * `sleep 3`
      * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-exported.xml`
      * `./tools/adb -s RFCYA01ANVE pull '/sdcard/3dPrinting/selected-model-af_goliath.gcode' /tmp/mobileslicer-af-goliath/selected-model-af_goliath.gcode`
  * exact result:
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-af_goliath.gcode`
      * `/tmp/mobileslicer-af-goliath/selected-model-af_goliath.gcode`
      * `sha256=63f80d74815ee4a336e92cfe79f9fc67aebddfbfb94826302bec9af4064ba3f8`
      * `1610335` bytes
      * `49672` lines
  * exact UI/log evidence:
    * `/tmp/mobileslicer-af-goliath/af-goliath-picker.xml`
      * picker package: `com.google.android.documentsui`
      * list view is active because the toolbar exposes `Grid view`
      * `af_goliath.stl` is visible without scrolling
    * `/tmp/mobileslicer-af-goliath/af-goliath-post.xml`
      * contains `Model loaded successfully`
      * contains `selected-model-af_goliath.stl`
    * `/tmp/mobileslicer-af-goliath/af-goliath-slice1.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-af-goliath/af-goliath-export-dialog.xml`
      * contains `selected-model-af_goliath.gcode`
    * `/tmp/mobileslicer-af-goliath/af-goliath-exported.xml`
      * contains `Export successful`
    * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg "MobileSlicer|sliceCurrentModel|orca_slice|export"`
      * `process stageMs=3730 totalMs=3770`
      * `export_gcode stageMs=12751 totalMs=16522`
      * `sliceCurrentModel:end ... elapsedMs=16537 sliced=true`
    * crash buffer:
      * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
      * no output
  * exact classification:
    * success
    * not a legitimate slicer rejection
    * not a concrete shipping bug
    * not a picker/path blocker
  * honest matrix classification after this run:
    * successful fixtures now include `aa_matrix_tetra_20mm.stl`, `ac_matrix_cube.stl`, `est.stl`, `obj_1_Tassen Regal lang.stl`, `ad_pig_real.stl`, `ae_sphere_real.stl`, and `af_goliath.stl`
    * known rejection under current defaults remains `ab_matrix_tetra_small.stl`
    * broader confidence increases, but this is still not an all-fixtures stability claim
* The added-confidence `af_goliath.stl` case is now honestly classified as repeatable from a cleared shipping ARM state
  * exact cleared-state repeat commands/interactions used:
    * `./tools/adb -s RFCYA01ANVE shell rm -f '/sdcard/3dPrinting/selected-model-af_goliath.gcode' '/sdcard/3dPrinting/af_goliath.gcode'`
    * `./tools/adb -s RFCYA01ANVE logcat -c`
    * `./tools/adb -s RFCYA01ANVE shell am force-stop com.google.android.documentsui`
    * `./tools/adb -s RFCYA01ANVE shell pm clear com.mobileslicer`
    * `./tools/adb -s RFCYA01ANVE push 'vendor/orcaslicer/resources/profiles/Vzbot/goliath.stl' '/sdcard/3dPrinting/af_goliath.stl'`
    * `./tools/adb -s RFCYA01ANVE shell am start -W -n com.mobileslicer/.MainActivity`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1534`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-repeat-picker.xml`
    * picker stayed in list view with:
      * title bounds `[252,2154][614,2241]`
      * metadata bounds `[252,2255][680,2312]`
      * preview bounds `[1188,2107][1440,2359]`
      * title-center tap `(433,2197)`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-repeat-post.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1765`
    * `sleep 8`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-repeat-slice1.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 720 1995`
    * `sleep 2`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-repeat-export-dialog.xml`
    * `./tools/adb -s RFCYA01ANVE shell input tap 1237 2840`
    * `sleep 3`
    * `./tools/adb -s RFCYA01ANVE shell uiautomator dump /sdcard/af-goliath-repeat-exported.xml`
    * `./tools/adb -s RFCYA01ANVE pull '/sdcard/3dPrinting/selected-model-af_goliath.gcode' /tmp/mobileslicer-af-goliath-repeat/selected-model-af_goliath.gcode`
  * exact repeat result:
    * load: pass
    * slice: pass
    * export: pass
    * emitted artifact:
      * `/sdcard/3dPrinting/selected-model-af_goliath.gcode`
      * `/tmp/mobileslicer-af-goliath-repeat/selected-model-af_goliath.gcode`
      * `sha256=bf61322954017e003b24762def8a1f26726f940cdf47f17650edfeaa5571fe81`
      * `1610335` bytes
      * `49672` lines
  * exact evidence:
    * `/tmp/mobileslicer-af-goliath-repeat/af-goliath-repeat-post.xml`
      * contains `Model loaded successfully`
    * `/tmp/mobileslicer-af-goliath-repeat/af-goliath-repeat-slice1.xml`
      * contains `Slice successful`
    * `/tmp/mobileslicer-af-goliath-repeat/af-goliath-repeat-exported.xml`
      * contains `Export successful`
    * focused app log command:
      * `./tools/adb -s RFCYA01ANVE logcat -d -v threadtime | rg "MobileSlicer|sliceCurrentModel|orca_slice|export"`
      * `process stageMs=3639 totalMs=3677`
      * `export_gcode stageMs=12611 totalMs=16288`
      * `sliceCurrentModel:end ... elapsedMs=16305 sliced=true`
    * crash buffer:
      * `./tools/adb -s RFCYA01ANVE logcat -d -b crash -v threadtime`
      * no output
  * exact repeatability classification:
    * success
    * repeatable for this fixture
    * full-file hash differs by timestamp header only
    * comment-stripped executable G-code matches exactly at `sha256=f651b6701dc21bf5ecef270c5109e31ebdcfb4c0a9cd785b99f2829f2964c1fe`
    * this does not prove all-fixtures stability
* Cleared-state shipping ARM reruns on `RFCYA01ANVE` are now honestly classified as deterministic toolpath with timestamp-only remaining metadata drift
  * run 1:
    * `/tmp/mobileslicer-repeatability/run1.gcode`
    * `sha256=d82daea5b499cbf5a61ece0b6627ccc2610eba678260aff60caafccfbfd4177c`
    * `375843` bytes
    * `13168` lines
  * run 2:
    * `/tmp/mobileslicer-repeatability/run2.gcode`
    * `sha256=c6bd67b41aca2903146583d3603eec9c201b06e1579231477058b05a9289a173`
    * `375459` bytes
    * `13168` lines
  * both reruns retain the same real-wrapper signatures and identical comment-stripped executable G-code:
    * `sha256=90a0c6af40fe34a3ea0388c981425ddfee81aba9f15d9c73082ea17f759425e8`
    * `326741` bytes
    * `11682` lines
* Source-level metadata classification is now complete for the current shipping real-wrapper path
  * header timestamp emission:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:2530` writes `; generated by %s on %s`
    * the header label comes from `engine-wrapper/orca-android-libslic3r/android_libslic3r_stubs.cpp:43-45`
    * the timestamp comes from `vendor/orcaslicer/src/libslic3r/Time.hpp:35-36`
  * object-label comment emission:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:5218-5221`
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:5355-5359`
  * printed `id:` source:
    * `PrintObject::get_id()` -> `vendor/orcaslicer/src/libslic3r/Print.hpp:468-469`
    * backing field `PrintObject::m_id` -> `vendor/orcaslicer/src/libslic3r/Print.hpp:580`
  * deterministic assignment path that is currently skipped:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:7804-7842`
    * `GCode::set_object_info(Print *print)` assigns `object->set_id(object_id++)`
  * exact gating that skips deterministic assignment in the current path:
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:2657-2658` only calls `set_object_info()` when `m_enable_exclude_object` is true
    * `vendor/orcaslicer/src/libslic3r/GCode.cpp:5443` sets `m_enable_exclude_object = m_config.exclude_object`
    * `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp:3651-3663` defaults `gcode_label_objects=true` but `exclude_object=false`
  * current honesty classification:
    * timestamp drift is expected metadata-only behavior
    * object-id drift is unexpected uninitialized comment metadata, not a deterministic upstream object identity
    * current observed drift still remains comment-only after artifact comparison
* The object-label metadata bug is now fixed and rerun-verified in the shipping path
  * exact source file changed:
    * `vendor/orcaslicer/src/libslic3r/PrintObject.cpp`
  * exact fix:
    * `PrintObject::PrintObject(...)` now initializes `m_id` from `model_object->id().id`
  * exact reason the previous path was wrong:
    * `gcode_label_objects` comments read `PrintObject::m_id`
    * `exclude_object=false` meant `GCode::set_object_info()` never ran
    * constructor left `m_id` uninitialized
  * exact post-fix rerun artifacts:
    * run 1:
      * `/tmp/mobileslicer-repeatability-fix/run1.gcode`
      * `sha256=92854b397cdb8db987b3d75821e63b8c9a3d7f05a8f4d00dd22b41d45e16baa9`
      * `372195` bytes
      * `13168` lines
    * run 2:
      * `/tmp/mobileslicer-repeatability-fix/run2.gcode`
      * `sha256=c74d73f60df575c0081a431e5ba17948b227aa576146926efd4fee134dedc950`
      * `372195` bytes
      * `13168` lines
  * exact object comment result after the fix:
    * both reruns emit stable object comment IDs:
      * `; printing object selected-model-tetrahedron_20mm_ascii_fresh.stl id:3 copy 0`
      * `; stop printing object selected-model-tetrahedron_20mm_ascii_fresh.stl id:3 copy 0`
    * both reruns still contain `192` object comment lines
  * exact remaining drift after the fix:
    * full-file hashes still differ
    * `diff -u` now shows only the header timestamp line changing
    * comment-stripped hashes remain identical:
      * `sha256=90a0c6af40fe34a3ea0388c981425ddfee81aba9f15d9c73082ea17f759425e8`
  * current honesty classification:
    * object-ID metadata drift is resolved
    * timestamp-only metadata drift remains
* A dedicated experimental Android `libslic3r` path now exists and compiles dependency-backed, config-backed, and implementation-backed native targets; it is not linked into the app, but it now links, executes, slices, and emits final real `.gcode` in the isolated experimental path
* `PrintConfig` / `DynamicPrintConfig` type compilation is proven on Android
* `Config.cpp` and `PrintConfig.cpp` implementation units now compile and link successfully on Android
* `printconfig_impl_subset` static library archives successfully
* `config_impl_subset` shared library links successfully as `liborca_android_libslic3r_config_impl_subset.so`
* `orca_android_libslic3r_model_stl_probe` now compiles, links, and runs on Android `x86_64`
* Real STL-first `Slic3r::Model` loading is proven in the isolated experimental path
* `orca_android_libslic3r_config_json_probe` now compiles, links, and runs on Android `x86_64`
* Narrow real config application is proven in the isolated experimental path through wrapper-style JSON to `DynamicPrintConfig`
* `orca_android_libslic3r_print_gcode_probe` now compiles, links, and executes on Android `x86_64`
* The second real slicing-core source layer also compiles in the isolated print probe
* Arm64 experimental-path configure/build for the same probe now reaches full configure + link success on `orca_android_libslic3r_print_gcode_probe` after `BuildVolume.hpp` enum typing was fixed in `vendor/orcaslicer/src/libslic3r/BuildVolume.hpp` using `signed char`; ARM runtime proof is now complete on `RFCYA01ANVE`
* The corrected essential follow-up runtime layer now compiles as well: `clipper_z.cpp`, `EdgeGrid.cpp`, `BridgeDetector.cpp`, `VariableWidth.cpp`
* Shipping app integration is now attempted on ARM with a fresh native rebuild; current outcome is now a closed post-gate native linker boundary followed by fixed first runtime crashes and a verified on-device slice/export pass:
  * minimum ARM gate inputs are now restaged at the expected shipping paths:
    * `/tmp/orca-deps-src/boost-1.84.0`
    * `/tmp/orca-deps-install/arm64-boost-cgal/lib/libboost_filesystem.a`
    * `/tmp/orca-deps-install/arm64-boost-headers/include/boost/...`
    * `/tmp/orca-deps-install/arm64-tbb-static2/lib/libtbb.a`
    * `/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a`
    * `/tmp/orca-deps-install/arm64-openssl/lib/libssl.a`
    * `/tmp/orca-deps-install/arm64-cgal/include/CGAL`
  * fresh shipping rebuild command:
    * `cd /home/peanut/Development/MobileSlicer && rm -rf android-app/app/.cxx android-app/app/build/intermediates/cxx android-app/app/build/.cxx`
    * `cd /home/peanut/Development/MobileSlicer/android-app && JAVA_HOME=/usr/lib/jvm/temurin-17-jdk GRADLE_USER_HOME=/tmp/gradle-cache ./gradlew :app:assembleDebug --no-daemon --stacktrace`
  * exact configure evidence:
    * `android-app/app/.cxx/Debug/35l5eg2l/arm64-v8a/CMakeCache.txt`
      * `ORCA_ANDROID_LIBSLIC3R_ENABLE:BOOL=ON`
      * `ORCA_ANDROID_BOOST_PREFIX:PATH=/tmp/orca-deps-install/arm64-boost-cgal`
      * `ORCA_ANDROID_BOOST_HEADERS_DIR:PATH=/tmp/orca-deps-install/arm64-boost-headers/include`
    * the previous fallback line `Missing staged Android experimental libs; falling back to reduced backend wrapper.` no longer appears in this run
  * most recent exact boundary at that stage:
    * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/logs/arm64-v8a/build_stdout_targets.txt`
      * `liborca_android_libslic3r_config_impl_subset` and `orca_engine` link statements are present and complete in this run (no failure reproduced).
      * exact evidence:
        * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/logs/arm64-v8a/build_stdout_targets.txt:18907` (config_impl_subset link)
        * `.../build_stdout_targets.txt:20295` (print_gcode_lib archive link)
        * `.../build_stdout_targets.txt:20296` (orca_engine shared link)
  * historical exact first blocker before this pass:
    * `/tmp/orca-rebuild-logs/shipping_after_gmp_mpfr_stage_run.log:17786-17840`
    * `/tmp/gradle-cache/daemon/8.9/daemon-216527.out.log:35802` (ninja shared-link of `liborca_android_libslic3r_config_impl_subset.so`)
    * `liborca_android_libslic3r_config_impl_subset.so`
    * unresolved symbols included `Clipper2Lib::ClipperBase::*`, `Clipper2Lib::ClipperOffset::*`, `Slic3r::chain_clipper_polynodes(...)`
    * exact boundary class: link-time source/dependency-closure after target generation (Clipper2 era)
    * `orca_android_libslic3r_print_gcode_lib` is now generated in target graph and its archive link is now successful in this run.
  * `ORCA_SHIPPING_USE_REAL_LIBSLIC3R` is `ON` in cache, and real-wrapper selection is now reached and linked through `orca_engine`.
  * shipping APK + ARM native artifacts exist locally:
    * `android-app/app/build/outputs/apk/debug/app-debug.apk`
    * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/obj/arm64-v8a/liborca_engine.so`
    * `android-app/app/build/intermediates/cxx/Debug/35l5eg2l/obj/arm64-v8a/liborca_android_libslic3r_config_impl_subset.so`
  * no fresh reduced-wrapper fallback message appears in the current generated tree:
    * `rg -n "falling back to reduced backend wrapper" android-app/app/build/intermediates/cxx android-app/app/.cxx -S`
    * result: no matches
  * first exact crash cause, now fixed:
    * `vendor/orcaslicer/src/libslic3r/Config.cpp:1805-1808` (`StaticConfig::set_defaults`) applied enum defaults through `opt->set(def->default_value.get())`
    * `vendor/orcaslicer/src/libslic3r/Config.hpp:2121-2127` previously copied enum `values` without copying `keys_map`
    * exact invalid state at crash boundary: `ConfigOptionEnumsGenericTempl<false>::keys_map == nullptr`
    * exact surfaced missing key during diagnosis: `extruder_type`
    * classification: `native runtime` / `config-owned`
  * next exact crash cause, now fixed:
    * `engine-wrapper/orca_wrapper.cpp:205-207` previously called `print.export_gcode(..., nullptr, nullptr)`
    * exact invalid state at crash boundary: `GCodeProcessorResult *result == nullptr`
    * classification: `wrapper-path behavior` / `export-contract-owned`
  * current shipping ARM device runtime classification on `RFCYA01ANVE` is now honest:
    * install: pass
    * launch: pass
    * model load: pass
    * slice: pass
    * export: pass
    * output artifact: emitted at `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
  * output-validation boundary is now closed with exact artifact proof from a fresh rerun:
    * on-device artifact:
      * `/sdcard/3dPrinting/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
      * size `375843`
    * pulled host artifact:
      * `/tmp/mobileslicer-artifacts/selected-model-tetrahedron_20mm_ascii_fresh.gcode`
      * `sha256=5f79db9fe5d4c7c576e187b26cda0a0452c29216700cc1a1110eafca987abc14`
      * `13168` lines
    * proving signatures observed:
      * `; HEADER_BLOCK_START`
      * `; EXECUTABLE_BLOCK_START`
      * `;TYPE:Skirt`
      * `;TYPE:Sparse infill`
      * `;TYPE:Internal Bridge`
      * `; printing object selected-model-tetrahedron_20mm_ascii_fresh.stl id:11484594880478881415 copy 0`
      * `; filament used [mm] = 213.53`
      * `; total layers count = 100`
      * `; CONFIG_BLOCK_START`
      * `; thumbnails = 48x48/PNG,300x300/PNG`
    * reduced-path contradiction used for proof:
      * `engine-wrapper/orca-android-core/orca_android_core.cpp` only emits `; generated by orca_core_android`, `; source=orcaslicer_android_wrapper`, `;LAYER:<n>` bounding-box loops, then `M400` / `M84`
      * those reduced-only markers are absent from the shipping artifact
      * the shipping artifact contains config serialization and feature/toolpath structures the reduced backend never generates
* The next real export/post-processing layer now compiles too: `GCode/ToolOrdering.cpp`, `GCode/ToolOrderUtils.cpp`, `FilamentGroup.cpp`, `FilamentGroupUtils.cpp`, `ParameterUtils.cpp`
* The next likely essential G-code runtime layer now compiles too: `CustomGCode.cpp`, `GCode/CoolingBuffer.cpp`, `GCode/PrintExtents.cpp`
* The next minimal partial `GCodeProcessor` construction layer now compiles too through real `CommandProcessor` trie support in the experimental stub path
* The next real export-helper layer now compiles too: `GCode/PressureEqualizer.cpp`, `GCode/SmallAreaInfillFlowCompensator.cpp`
* The next essential bookkeeping/config/time layer is now partly real in the experimental probe: `GCodeReader.cpp`, `Time.cpp`, and lifted real `GCodeProcessor` / `GCodeProcessorResult` reset-state bodies
* The next layer-feature runtime units now compile too in the isolated print probe: `ElephantFootCompensation.cpp`, `Fill/Fill.cpp`, `GCode/AdaptivePAProcessor.cpp`, `GCode/AdaptivePAInterpolator.cpp`, `GCode/SpiralVase.cpp`, `Arachne/utils/ExtrusionLine.cpp`
* The first real `Print` + `GCode` milestone is no longer blocked at link-time
* The previous linked Android `No layers were detected` failure is now root-caused and cleared
* Real temporary G-code has now been emitted by the experimental print probe for the 20 mm tetrahedron fixture
* Final export completion now succeeds in the isolated experimental probe and emits final `.gcode` output
* OpenSSL 3.1.7 is staged and proven for Android `x86_64`
* Full Boost 1.84.0 header tree (including Beast) is staged and proven

## Current Focus

* [x] Isolate the first shipping ARM real-wrapper small-matrix blocker on `RFCYA01ANVE`
  * fixture: `ab_matrix_tetra_small.stl`
  * exact observed failure: `Slice failed` after successful model load
  * exact isolated slicer reason: `One object has an empty first layer and can't be printed. Please Cut the bottom or enable supports.`
  * exact classification: `slice`
  * current scope classification: general, not ARM-specific in current evidence
  * export artifact: not emitted
* [x] After the second-fixture blocker was understood, resume the stopped small matrix honestly
  * continued staged fixture: `ac_matrix_cube.stl`
  * exact result: load pass, slice pass, export pass, emitted `/sdcard/3dPrinting/selected-model-ac_matrix_cube.gcode`
* [ ] Re-run the accepted-fixture matrix only
  * fixtures:
    * `aa_matrix_tetra_20mm.stl`
    * `ac_matrix_cube.stl`
  * keep `ab_matrix_tetra_small.stl` classified as a known rejected default-path fixture unless new evidence disproves it
  * do not invent config variants unless the current shipping app path can exercise them without contract churn
* [ ] Decide the smallest product handling for known Orca slice rejections in the shipping path
  * keep scope narrow
  * no UI redesign
  * no config UI expansion
  * no JNI/wrapper contract churn unless required

* [x] Audit the current reduced Android-native backend against the real Orca `libslic3r` pipeline
* [x] Define the minimum Android headless Orca port target
* [x] Create a dedicated non-shipping Android `libslic3r` build path
* [x] Document the real dependency graph and known blockers
* [x] Bring up the first Android-native dependency chain for the experimental `libslic3r` path
* [x] Prove a headless dependency-backed `libslic3r` compile for one Android ABI
* [x] Stage `cereal` for Android in the experimental `libslic3r` path
* [x] Prove `PrintConfig` / `DynamicPrintConfig` header compilation on Android
* [x] Stage Boost.Beast headers for `PrintConfig.cpp`
* [x] Stage Android OpenSSL for `Config.cpp`
* [x] Compile config implementation units `Config.cpp` and `PrintConfig.cpp`
* [x] Link config implementation target with all required `libslic3r` source units
* [x] Prove real `Slic3r::Model` load without changing JNI or UI
* [x] Prove narrow wrapper-style JSON application into real `DynamicPrintConfig`
* [x] Prove real `Print` plus `GCode` generation behind the wrapper contract
  * status: clean final-output completion proven in Waydroid on Android `x86_64`
  * exact target built: `orca_android_libslic3r_print_gcode_probe`
  * exact runtime command: `tools/adb shell /data/local/tmp/orca_android_libslic3r_print_gcode_probe /data/local/tmp/tetrahedron_20mm_ascii.stl /data/local/tmp/config_probe_valid.json`
  * final artifact: `/data/local/tmp/orca_android_libslic3r_print_probe_output.gcode`
  * validated blocker root-cause: `layer_change_gcode` / `most_used_physical_extruder_id` placeholder contract gap
* `arm64-v8a` configure/build proof for this same target is now successful after aligning Boost.Nowide source closure with `ORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0` in the ARM configure command
  * configure command used:
    * `cmake -S /home/peanut/Development/MobileSlicer/engine-wrapper/orca-android-libslic3r -B /tmp/orca-android-libslic3r-build-print-arm64 -DCMAKE_TOOLCHAIN_FILE=/home/peanut/Development/MobileSlicer/.android-sdk/ndk/26.3.11579264/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DORCA_ANDROID_LIBSLIC3R_ENABLE=ON -DORCA_ANDROID_BOOST_PREFIX=/tmp/orca-deps-install/arm64-boost-cgal -DORCA_ANDROID_BOOST_HEADERS_DIR=/tmp/orca-deps-install/arm64-boost-headers/include -DORCA_ANDROID_TBB_PREFIX=/tmp/orca-deps-install/arm64-tbb-static2 -DORCA_ANDROID_GMP_PREFIX=/tmp/orca-deps-install/arm64-gmp -DORCA_ANDROID_MPFR_PREFIX=/tmp/orca-deps-install/arm64-mpfr -DORCA_ANDROID_CGAL_PREFIX=/tmp/orca-deps-install/arm64-cgal -DORCA_ANDROID_OPENSSL_INCLUDE_DIR=/tmp/orca-deps-install/arm64-openssl/include -DORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a -DORCA_ANDROID_OPENSSL_SSL_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libssl.a -DORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`
    * build command used:
      * `cmake --build /tmp/orca-android-libslic3r-build-print-arm64 --target orca_android_libslic3r_print_gcode_probe -j$(nproc)`
    * first reproduced failure before fix:
      * `undefined symbol: boost::nowide::setenv(char const*, char const*, int)` (from `vendor/orcaslicer/src/libslic3r/Config.cpp:770`)
  * minimum honest boundary fix:
    * set `ORCA_ANDROID_BOOST_SRC_ROOT` in ARM configure so source-compiled `Boost.Nowide/cstdlib.cpp` participates in ARM probe linking
  * result:
    * `orca_android_libslic3r_print_gcode_probe` now links successfully on `arm64-v8a`
  * next exact action:
    * confirm ARM runtime target availability and execute the isolated probe flow on `RFCYA01ANVE` using the same STL + config fixture and binary
    * runtime validation is now complete on ARM with final output at `/tmp/orca_android_libslic3r_print_probe_output-arm64.gcode`
* [x] Resolve shipping ARM integration linker blocker at `orca_engine` link
  * exact blocker command:
    * `cd /home/peanut/Development/MobileSlicer/android-app && cmake --build app/.cxx/Debug/35l5eg2l/arm64-v8a --target orca_engine -j 6`
  * exact blocker resolved:
    * `ld.lld: undefined symbol: getrandom` (from `boost/filesystem/src/unique_path.cpp` in `libboost_filesystem.a`)
    * fixed by adding `android-app/app/src/main/cpp/getrandom_compat.cpp` to shipping `orca_engine` sources when `ORCA_SHIPPING_REAL_WRAPPER=ON`
    * proof command:
      * `cd /home/peanut/Development/MobileSlicer/android-app && cmake --build app/.cxx/Debug/35l5eg2l/arm64-v8a --target orca_engine -j 6`
      * first executed blocker in this run after fix: next exact blocker is ARM runtime proof boundary on device after successful link
  * target: `liborca_engine.so` on `arm64-v8a` shipping target

## Phase 2 Milestones

* Milestone 1: experimental Android Orca core config succeeds
* Milestone 2: foundational Android dependency chain builds for one ABI
* Milestone 3: dependency-backed `libslic3r` execution-policy target compiles
* Milestone 4: `cereal` is integrated and config headers compile on Android
* Milestone 5: config implementation units compile and link — COMPLETE
  * `config_impl_subset` shared library links as `liborca_android_libslic3r_config_impl_subset.so`
  * 15 real `libslic3r` source files compiled (config core + geometry)
  * 4 subsystems stubbed (ShortestPath, MedialAxis/Voronoi, GCodeThumbnails, utils.cpp)
* Milestone 6: real model load works through Orca path — COMPLETE
  * `orca_android_libslic3r_model_stl_probe` executable links on Android `x86_64`
  * Waydroid runtime proof succeeded with `objects=1 volumes=1 instances=0 facets=4`
  * additional real model-load sources include `ObjectID.cpp`, `Geometry/ConvexHull.cpp`, `TriangleMesh.cpp`, `Model.cpp`, `Format/STL.cpp`, Admesh STL reader units, semver C, and Qhull
* Milestone 7: real config path works — COMPLETE
  * `orca_android_libslic3r_config_json_probe` executable links on Android `x86_64`
  * Waydroid runtime proof succeeded for a narrow wrapper-style JSON subset
  * no extra real `libslic3r` implementation units were needed beyond the config implementation milestone
* Milestone 8: real slicing pipeline runs
  * status: CLEAN COMPLETION PROVEN on Android `x86_64` and confirmed ARM `arm64-v8a` (`RFCYA01ANVE`) (final output emitted)
  * `orca_android_libslic3r_print_gcode_probe` compiles, links, and executes on Android `x86_64`
  * `orca_android_libslic3r_print_gcode_probe` now also compiles, links, and executes on ARM device `RFCYA01ANVE`
  * the essential runtime follow-up layer now compiles successfully
  * the real `ToolOrdering` export/runtime layer now compiles successfully as well
  * the real `CustomGCode` / `CoolingBuffer` / `PrintExtents` layer now compiles successfully as well
  * the GCode bookkeeping/config/time blocker is now partly real rather than stubbed
  * the `elephant_foot_compensation` / Adaptive-PA / Spiral-Vase / Arachne-length layer is now real and compiled
  * seam placement and retract-when-crossing-perimeters are now real runtime, not optional
  * final runtime proof now includes clean `_do_export()` completion and `.gcode` artifact emission
* current shipping-closing work is now past the first real-wrapper runtime blocker:
  * shipping `arm64-v8a` passes the pre-`add_subdirectory` gate, config-impl target generation, native linker closure, install, launch, model load, slice, export, and output emission.
  * the two exact fixed runtime causes were:
    * enum metadata loss during static default assignment (`extruder_type` surfaced first, `keys_map` dropped in `ConfigOptionEnumsGenericTempl::set`)
    * null export result pointer passed by the wrapper into `Print::export_gcode(...)`
  * emitted shipping-app `.gcode` is now proven real-wrapper output by exact artifact signatures and contradiction against the reduced backend generator
  * rerun repeatability is now classified:
    * not byte-identical across cleared-state reruns
    * stable executable G-code after isolating comment-only drift
    * timestamp drift is expected current-time metadata
    * object-id comment drift has now been fixed with deterministic model-backed initialization
* Milestone 8a: arm64 production-boundary configure/build proof
  * status: configure and link both now succeed for `orca_android_libslic3r_print_gcode_probe` after `ORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0` and ARM nowide source-equivalent wiring
  * configure command:
    * `cmake -S /home/peanut/Development/MobileSlicer/engine-wrapper/orca-android-libslic3r -B /tmp/orca-android-libslic3r-build-print-arm64 -DCMAKE_TOOLCHAIN_FILE=/home/peanut/Development/MobileSlicer/.android-sdk/ndk/26.3.11579264/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 -DORCA_ANDROID_LIBSLIC3R_ENABLE=ON -DORCA_ANDROID_BOOST_PREFIX=/tmp/orca-deps-install/arm64-boost-cgal -DORCA_ANDROID_BOOST_HEADERS_DIR=/tmp/orca-deps-install/arm64-boost-headers/include -DORCA_ANDROID_TBB_PREFIX=/tmp/orca-deps-install/arm64-tbb-static2 -DORCA_ANDROID_GMP_PREFIX=/tmp/orca-deps-install/arm64-gmp -DORCA_ANDROID_MPFR_PREFIX=/tmp/orca-deps-install/arm64-mpfr -DORCA_ANDROID_CGAL_PREFIX=/tmp/orca-deps-install/arm64-cgal -DORCA_ANDROID_OPENSSL_INCLUDE_DIR=/tmp/orca-deps-install/arm64-openssl/include -DORCA_ANDROID_OPENSSL_CRYPTO_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libcrypto.a -DORCA_ANDROID_OPENSSL_SSL_LIBRARY=/tmp/orca-deps-install/arm64-openssl/lib/libssl.a -DORCA_ANDROID_BOOST_SRC_ROOT=/tmp/orca-deps-src/boost-1.84.0`
    * configure result: passed (`-- Configuring done`)
  * build command: `cmake --build /tmp/orca-android-libslic3r-build-print-arm64 --target orca_android_libslic3r_print_gcode_probe -j$(nproc)`
  * exact boundary outcome:
    * no ARM nowide linker blocker remains in this configure/build proof
  * next boundary:
    * next boundary is shipping-integration runtime with real-wrapper activation and artifact evidence in the app path
* Milestone 9: real G-code generation replaces reduced output — COMPLETE at the shipping ARM artifact-validation boundary
* Milestone 9a: shipping ARM rerun repeatability classification — COMPLETE
  * two cleared-state reruns on `RFCYA01ANVE` both emitted real-wrapper artifacts
  * full-file hashes differ, but comment-stripped executable G-code is identical
  * exact classification: semantically stable but byte-different
* Milestone 10: existing export/share flow continues unchanged

## Notes

Update this file continuously.
