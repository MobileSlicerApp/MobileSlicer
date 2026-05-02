# Android UI

UI must be built using Kotlin + Jetpack Compose.

## Flow

1. Import model
2. Select preset
3. Adjust settings
4. Slice
5. Preview
6. Export

## Rules

* Touch-first design
* Large tap targets
* No desktop UI patterns
* Minimal navigation depth
* Keep OrcaSlicer concepts and terminology recognizable where they help users
* Present those concepts in a mobile-native flow rather than a desktop settings port
* `Profiles` owns slicer configuration UX
* app `Settings` owns app behavior and app appearance only

The UI is a core differentiator of this project.

## Current Override Truth

The current live UI truth is narrower than some older workspace/viewer notes in
this file:

* `Prepare` is the real STL workspace viewer that currently ships
* `Preview` now renders sliced G-code through native Orca `libvgcode`
* Android-owned Preview geometry generation is not the accepted product truth
  and must not be revived
* current debug APK native slicer/viewer code now builds with C++ `-O3` and
  `-fomit-frame-pointer`
* the old Benchy `59s` slice time is an unoptimized-debug baseline; the
  user-confirmed current path is about `4s`
* the user also confirmed STL rendering became much faster after the native
  debug-build optimization correction
* workspace session state now survives portrait/landscape recreation:
  * the staged STL is not intentionally discarded during configuration change
  * the recreated activity reseeds workspace state from a retained session
    holder instead of starting as a fresh import session
  * slicing after rotation must still work because the recreated native engine
    reloads the retained staged STL before slicing
  * current implementation handles rotation in-place and recreates only the
    Android viewer surface when the viewport size changes; this preserves the
    native Orca workspace while avoiding stale portrait-sized render surfaces in
    landscape
  * Preview pause/resume must rebind the current G-code source and layer range
    after renderer release, even when the slice key did not change
  * landscape keeps the same workspace behavior as portrait, but the collapsed
    bottom status dock is intentionally shorter and narrower so the viewer
    remains the primary surface

## Product Direction

Treat OrcaSlicer as the source of truth for:

* Printer / Filament / Process profile structure
* preset mental model
* familiar slicer vocabulary
* broad settings organization

Treat committed files under `README/Design Inspiration files/` and the product
logo assets as the source of truth for:

* card styling
* visual hierarchy
* app-shell composition
* touch-first interaction patterns

Current repo hygiene note: the previous screenshot references in that directory
were intentionally deleted. Do not assume missing screenshots are available.

Rule:

* Orca mental model underneath
* Mobile Slicer presentation on top

## Home Shell Ownership

Home shell entry points are split intentionally:

* `Profiles`
  * owns Printer / Filament / Process selection
  * will own deeper preset editing and custom preset creation over time
* top-right `Settings`
  * is the only app-settings entry point
  * uses a cog icon, not placeholder text chips
  * owns theme mode, accent color, legal/about/info, and future app-only preferences

Do not:

* keep a redundant `Settings` card on the home grid once the top-right settings action exists
* place printer, filament, or process settings inside app `Settings`

## Profiles Flow Truth

The current accepted `Profiles` navigation truth is:

* `Profiles` is a full page
* opening an individual Printer / Filament / Process profile now also uses a full-page editor surface inside the `Profiles` flow
* individual profile editing should not fall back to tall modal dialogs that stack on top of the Profiles page
* each editor page keeps the same bounded role:
  * back action in the header
  * profile-name field and save/create action in the header row
  * vertically scrollable editor content underneath
* current compact editor truth:
  * visible `Subtitle` fields are no longer part of the phone editor chrome
  * process editing uses a single-row section tab strip:
    * `Quality`
    * `Strength`
    * `Speed`
    * `Support`
    * `Multimaterial`
    * `Others`
  * printer editing now uses Orca-style category tabs:
    * `Basic information`
    * `Connection`
    * `Machine G-code`
    * `Multimaterial`
    * `Extruder`
    * `Motion ability`
    * `Notes`
  * filament editing now uses Orca-style category tabs:
    * `Filament`
    * `Cooling`
    * `Setting Overrides`
    * `Advanced`
    * `Multimaterial`
    * `Dependencies`
    * `Notes`
  * within profile tabs, field groups should keep Orca-style subgroup labels
    where Orca groups the controls, such as `Retraction when switching
    material`, `Flow ratio and Pressure Advance`, `Prime tower`, and `Support
    interface`

## Filament Picker Truth

The `Select Filament` picker follows mobile catalog behavior over desktop
compatibility gating:

* when a printer is selected, `Recommended` appears first and is selected by
  default
* `All` appears second and keeps every generated Orca filament searchable and
  importable
* `Recommended` and `All` show counts for the current search result set
* `All` should not show repeated visible rows for the same material just
  because Orca has printer/nozzle-specific JSON variants underneath
* generic material-name rows like `PLA / <vendor>` collapse to a single visible
  row; the picker chooses a printer-recommended row first, otherwise `Generic`
* non-recommended choices remain selectable, with clear row text that they are
  available but not Orca-recommended for the selected printer when the user is
  viewing `Recommended`; `All` hides that warning line for non-recommended rows
  to keep browsing readable
* selecting any row still imports a printer-bound filament profile copy, so the
  slicing configuration remains printer-specific
* the picker prebuilds and caches a compact row index per selected
  printer/nozzle; search and category toggles must filter that prepared index,
  not the raw duplicate-heavy Orca manifest
  * printer print-host controls intentionally live in the dedicated
    `Connection` tab and remain visible without enabling advanced slicer
    controls, because connecting a printer is an app workflow rather than an
    expert slicer-tuning workflow
  * printer connection runtime behavior is implemented incrementally; the
    current host matrix, Orca source references, security requirements,
    hardware-validation gaps, and Android network boundary live in
    `/README/PRINTER_CONNECTION.md`
  * only surfaced Android-owned fields are shown inside those tabs right now; the unsurfaced tabs stay as honest placeholders instead of claiming broader coverage
  * printer, filament, and process editors no longer show `Editing scope` cards
* this keeps deep Process editing readable on phone without introducing a new top-level app section

## Current UI Boundary

* Last smoke-verified on-device shell:
  * top app bar with product logo at upper-left
  * hero Import card as the primary action
  * Profiles entry as the slicer-configuration entry point
  * current Profiles flow truth now uses full-page editors for individual printer / filament / process profiles instead of modal dialogs
  * separate current-model/actions section still visible in that last verified build
* Latest local build candidate now installed for user visual validation:
  * keeps the single top-right cog-style Settings action
  * opens Settings as a full-page app-settings screen instead of a partial sheet
  * makes Import and Profiles use matching island treatment
  * widens Profiles to the same width/span treatment as the Import island
  * refines the top-left logo treatment so it visually sits with the `Mobile Slicer` title text
  * removes the extra arrow icon from the Import island
  * removes the current-model/actions work area from Home
  * reduces the import helper chips to exactly `File Manager` and `STL / 3MF`
  * keeps Home focused on import + configuration rather than the future viewer workspace
  * introduces a first minimal `Workspace` screen boundary for loaded-model context and operations
  * routes successful import into `Workspace`
  * adds the first real model viewer in `Workspace` with a visible plate/grid surface
  * wires touch orbit / pan / zoom to that viewer
  * keeps visible `Slice model`, `Export`, and `Share` controls inside `Workspace` instead of Home
  * refines `Workspace` so the viewer is the dominant full-screen working surface
  * makes `Workspace` less card-framed by overlaying the action/context panel inside the workspace surface instead of stacking a separate panel underneath
  * wires shell surfaces and sheets to the selected app theme mode so `System`, `Light`, and `Dark` can visibly diverge
  * latest blocker-only viewer rewrite replaces both the failed Filament path and the later `GLSurfaceView` path with one bounded `SurfaceView` workspace backed by a dedicated OpenGL ES 2 render thread
  * latest blocker-only workspace pass keeps the top header slim, keeps the bottom overlay minimal, and keeps the selected printer visible through bed sizing and slicer-style model/bed summary pills instead of debug text
* Honest verification limit for this latest candidate:
  * local Kotlin compile succeeded for the Profiles full-page editor pass
  * full APK assembly is currently blocked by unrelated active viewer work in `TouchModelViewerView.kt`
  * coder did not claim fresh phone proof for this Profiles-editor navigation change in this pass
  * final visual confirmation of the full-page profile editors still requires user hands-on validation once the unrelated viewer build blocker clears

## Current Workspace Viewer Boundary

* Latest local implementation:
  * workspace session now survives configuration-change recreation through a
    retained Android-side session holder
  * the staged STL is no longer deleted during configuration-change teardown
  * the recreated activity reseeds model/session state from that retained
    holder instead of treating rotation like a fresh import
  * slicing after rotation now reloads the retained staged STL into the
    recreated native engine before slicing, so the app does not regress into
    "model still visible but native slice says no model loaded"
  * import now stops after staged-cache copy plus native model acceptance; the previous Android-side STL mesh prep no longer blocks native acceptance
  * successful STL import now routes into `Workspace` only after native load finishes, so users land on a model-ready workspace instead of entering during native import
  * `Workspace` now prepares the imported STL through `StlMeshParser` after native acceptance, and it keeps an explicit preparing/loading state visible only while deferred viewer mesh prep and first-frame upload are still finishing
  * the active viewer path is now a bounded Android-app-side `SurfaceView` with a dedicated OpenGL ES 2 render thread
  * the failed Filament scene/material/sanity-mesh path is no longer active
  * the later `GLSurfaceView`-based viewer engine is also no longer active
  * user-confirmed current truth on `RFCYA01ANVE`:
    * the STL is now visibly rendered
    * the model is now on a visible printer bed
    * the workspace now reads as a slicer workspace instead of a blank/broken viewer
    * drag directions now behave correctly
    * zoom behavior is acceptable
  * the active scene is slicer-style and printer-bed-centric:
    * imported Orca printer profiles use generated Orca `bed_model` STL assets
      for the visible plate body and rim
    * imported Orca printer profiles use rasterized Orca `bed_texture` assets
      for stickers, labels, warning marks, and bed surface art
    * Orca-style grid geometry is drawn separately from the texture, with
      10 mm lines and stronger 50 mm guides on normal phone-sized beds
    * the grid stays visible through the transparent underside, while the top
      texture/stickers are front-face-only and cover the grid from above
    * grid-bearing Orca textures, including Prusa Core One-family beds, suppress
      the extra Android grid from the top view and draw it bottom-only for
      underside views
    * the old generic Android plate is not uploaded on the Orca asset path; it
      remains the fallback for custom or missing-asset profiles
    * origin cross on the bed
    * model centered on the bed in XY
    * model grounded to the plate at `z = 0`
    * practical orbit / pan / pinch-zoom camera around the bed instead of a generic floating preview
  * the selected printer preset now drives workspace bed dimensions, Orca
    bed-model assets, and Orca bed-texture assets when those fields are present
  * renderer scope is intentionally narrow, but no longer single-object or
    STL-result-only:
    * STL viewing is active in `Prepare`
    * multi-object plate sessions are active in `Prepare`
    * `3MF` viewing is still not implemented
    * sliced G-code Preview is active through native Orca `libvgcode`
    * Preview layer controls are active for native G-code range/single-layer
      inspection
    * Android-owned Preview extrusion geometry remains rejected; native Orca
      owns G-code interpretation/render data
  * accepted Preview interaction behavior:
    * dragging the Range/Single controls updates the visible native layer range
      without recreating the native G-code viewer
    * releasing the slider performs one final filtered native Preview reload
      for the selected range
    * oversized Preview starts with a native count-only layer planning pass;
      Android selects the first exact range under a conservative phone planning
      budget before enabling native G-code preview loading
    * native Preview loading and automatic layer chunk planning use the selected
      GCODE Preview Performance budget: Low end 400k, Mid range 750k, High end
      1m. The native hard ceiling remains 1m.
    * if native Preview loading still reports a too-large selected range,
      Android immediately asks the native planner for exact subranges and moves
      to the first safe range instead of leaving the error overlay active
    * user-confirmed on device on 2026-04-29: large G-code preview no longer
      shows a full-range failure before chunking; it calculates first and then
      loads the planned exact range
    * multicolor Preview defaults to Orca `ColorPrint` mode so prime/wipe tower
      extrusion color follows filament/tool semantics rather than the generic
      wipe-tower feature color
    * filtered Preview windows track the original global layer offset so live
      range updates map into native local layer IDs correctly
    * live native reloads while dragging and the two-entry native viewer cache
      were both tested and rejected after device review
  * performance boundary is intentionally lighter than the failed paths:
    * no Filament engine/material setup
    * no sanity tetrahedron fallback path
    * no `GLSurfaceView` render-loop helper overhead
    * no continuous render loop in steady state
    * static bed geometry uploaded once per bed change
    * prepared STL mesh is now deferred off the native-acceptance path and reused instead of reparsed on the render thread
    * model and bed geometry uploaded to GPU buffers once per scene change
    * latest bounded import/open pass now also surfaces timing facts in the workspace status and dock:
      * file staging time
      * native load time
      * deferred viewer mesh prep time
      * first visible workspace frame time when available
  * latest bounded hardening on this same renderer:
    * ignores no-op reapplication of identical mesh / bed / appearance state so Compose recomposition does not keep resetting viewer readiness
    * clears stale viewer failure when a new mesh or surface bind arrives so the current renderer can retry cleanly after a meaningful state change
    * rejects invalid binary STL triangle counts before oversized allocation
    * keeps binary-STL format sniffing non-fatal so valid ASCII STL files no longer die early on arbitrary bytes at offset `80..83`
    * caches shader handle lookups and appearance-derived colors instead of recreating them every frame
    * ignores tiny gesture deltas that would otherwise wake the render thread without a meaningful camera change
    * streams ASCII STL parsing line-by-line instead of first materializing the full file text
    * removes the prior full CPU-side translated vertex-array copy before GPU upload by applying model centering/grounding as a shader offset instead
    * replaces the old binary STL parser's tiny per-float file reads with buffered record parsing and removes per-triangle normal-array churn
    * uses the direct native STL load path instead of the slower generic model file reader for `.stl` imports
  * kept control-semantics path on this same renderer:
    * pinch direction stays in the corrected state so pinch-out zooms in and pinch-in zooms out
    * single-finger drag start accumulates from touch-down so orbit engages more reliably on slower drags
    * two-finger pan stays camera-relative instead of falling back to fixed world-axis movement
  * latest workspace cleanup on the current path:
    * top bar now foregrounds the selected printer and exact bed size instead of a generic workspace title
    * bottom overlay now reads as a tighter dock with fewer, smaller badges and a more compact summary
    * normal loaded state now favors concise model / bed / format context over a debug-like stack of pills
    * `Export` and `Share` now use lower visual emphasis than the primary `Slice` action
    * latest local visual pass removes the extra loaded-state bed badge and relies on one cleaner summary line so the dock stays quieter
    * current local follow-up also states explicitly in the workspace/result copy that a successful slice still leaves the viewer on the STL mesh and does not turn this surface into a toolpath preview
    * latest bounded result-surface follow-up now also adds one honest post-slice details layer derived from emitted G-code:
      * the slice-success dialog shows compact result details from emitted output
      * the workspace dock keeps those same bounded result facts visible after the dialog is dismissed
      * current surfaced result facts are limited to byte count, line count, layer-change count, observed `;TYPE:` markers, and wall/shell-related `;TYPE:` markers when present
      * this older STL-result-surface detail layer is superseded by the active
        native `libvgcode` Preview path once a slice succeeds
    * current dock simplification pass:
      * model and printer now share the leading badge row
      * duplicate format, model-size, and bed-size rows were removed from the
        bottom dock in both `Prepare` and `Preview`
      * post-slice timing is one shared row:
        `Prepare: x.x seconds • Slice: x.x seconds`
      * `Prepare` post-slice output now shows only
        `Layers: ... • Time: ... • Filament: ...`
      * `Preview` uses the same dot-separated layer/time/filament style
      * landscape `Preview` starts with a compact dock: a top-right `Menu`
        control plus the range slider only; tapping `Menu` restores the full
        Preview dock controls
      * the collapsed landscape Preview dock is a tight slider strip, with the
        `Menu` control on the same row as the range slider to avoid a tall blank
        panel
      * landscape `Prepare` uses the same `Menu` / `Hide` dock collapse control
        so the profile/save/slice action rows can be tucked away while checking
        the model
  * latest real interactive import/slice/export verification on `RFCYA01ANVE`:
    * exact path exercised:
      * Home -> `Import` -> Android `DocumentsUI` search -> `wall_smoke_box_20mm.stl` -> `Workspace` -> `Slice` -> `Export`
    * exact device model path:
      * `/sdcard/Download/wall_smoke_box_20mm.stl`
    * exact exported device artifact:
      * `/sdcard/Download/selected-model-wall_smoke_box_20mm.gcode`
    * exact verified outcome:
      * the STL now imports successfully through the reachable DocumentsUI path
      * the previous `Binary STL triangle count is invalid` failure is cleared for this ASCII STL path
      * slicing succeeds and the emitted-result details surface appears on device
      * export succeeds through the same interactive path
    * honest remaining limit:
      * the workspace still does not show real wall toolpaths or layer walls
      * users now get a working interactive slice/export path plus emitted-output facts, not an Orca-style preview
  * latest bed/scene refinement on the current path:
    * plate now uses a darker outer rim plus a lighter printable-area inset so the bed reads more like a slicer plate instead of one flat rectangle
    * minor and major grid lines are now constrained to the printable area for a cleaner bed silhouette
    * inset border and full-bed center guides are stronger so orientation and plate depth read more clearly
    * scene background is slightly darker so the STL stands off the plate more cleanly
    * latest local follow-up now turns that bed into a clearer rim / wall / printable-area composition so the plate reads less like layered overlays and more like a real bed with thickness
    * outer outline is softer while printable-area framing stays readable, so the bed silhouette is stronger without looking like a hard debug box
  * latest bounded performance pass on the current path:
    * caches orbit trig state for pan/camera updates instead of recomputing the same trig on every interaction and frame
    * skips redundant pause-state redraw wakeups
    * avoids an unnecessary normal-array copy during mesh upload
    * reduces boxed float churn in bed-grid generation and ASCII STL parsing
    * latest local follow-up also removes per-line ASCII STL `split(...)` allocations by parsing float triplets directly
    * latest local host-side duplicate-work benchmark over `_quarantine/root-dependency-dump/data/meshes/pig.stl` measured:
      * file staging: `0.20-0.22 ms`
      * old viewer mesh prep: `182.28 ms`
      * new viewer mesh prep: `2.25 ms`
      * parser speedup: `81.02x`
      * removed renderer transform-copy: `0.22 ms -> 0.0043 ms`
    * latest user-reported device measurements on `RFCYA01ANVE` for `selected-model-3DBenchy.stl` measured:
      * before this bounded detour: `Stage 29 ms` / `Native 3080 ms` / `Viewer prep 18509 ms` / `First frame 21766 ms`
      * after the viewer/parser fixes: `Stage 15 ms` / `Native 3095 ms` / `Viewer prep 121 ms` / `First frame 3352 ms`
      * after the bounded native STL fast path: `Stage 16 ms` / `Native 2937 ms` / `Viewer prep 119 ms` / `First frame 3166 ms`
    * honest current performance boundary:
      * the huge Android-side viewer-prep stall is fixed
      * the previous `Native 2937-3095 ms` import/load timing was captured
        before the native debug-build optimization correction
      * after the native debug-build optimization correction, the user reported
        that STL rendering became much faster
      * fresh post-fix import/load stage timing has not been pulled yet
* Honest current format boundary:
  * STL viewing is now proven at the user-confirmed on-device boundary for the current workspace path
  * `3MF` viewing is not yet supported because the current Android import/load path still rejects `3MF`
* Deliberate next product direction for `Workspace`:
  * `Workspace` should evolve from a viewer-plus-actions surface into the main working surface for mobile slicing
  * the next meaningful model-operation additions should be staged in bounded passes:
    * selection
    * delete
    * move on bed
    * scale
    * rotate
    * auto-orient
    * flatten to face
    * arrange
  * these should stay touch-first and slicer-centric, not desktop-tool-clone heavy
* Honest verification limit:
  * user hands-on testing now confirms that the active workspace basically works on `RFCYA01ANVE` and that drag / zoom controls are acceptable
  * local source review and successful build confirm Home → Workspace routing, prepared STL reuse, selected-printer bed sizing, the bed/grid scene, and this bounded bed/performance refinement follow-up
  * rebuilt APK was installed on `RFCYA01ANVE`
  * coder also used `tools/adb` runtime probing in the latest bounded verification pass to confirm the real DocumentsUI import -> workspace -> slice -> export flow for `/sdcard/Download/wall_smoke_box_20mm.stl`
  * improved bed presentation and device feel still require fresh user confirmation on-device before any broader success claim

## Current Verified Profiles Flow

* `Profiles` is no longer only a picker flow:
  * it now opens as a dedicated slicer-configuration screen in the Android app layer
  * it remains separate from app `Settings`
* Current Android-app-side profile system now supports:
  * built-in Printer / Filament / Process presets
  * user-created custom profiles
  * duplicate / rename / edit / delete-custom CRUD behavior
  * local persistence of profile collections plus active selected profile ids
* Current editor structure:
  * domains are split into Printer / Filament / Process
  * each domain uses bounded Orca-style grouped sections instead of one undifferentiated form
* Current editable fields:
  * Printer:
    * name
    * subtitle
    * bed width
    * bed depth
    * max height
    * nozzle diameter
    * filament diameter
  * Filament:
    * name
    * subtitle
    * material type
    * nozzle temperature
    * bed temperature
    * cooling baseline
    * no cooling for first X layers
  * Process:
    * name
    * subtitle
    * first layer height
    * layer height
    * first layer print speed
    * top shell layers
    * bottom shell layers
    * seam position
    * precise wall
    * top surface pattern
    * wall/perimeter count
    * skirts
    * brim width
    * infill density
    * sparse infill pattern
    * print speed baseline
* Current applied behavior:
  * selected printer bed dimensions still drive workspace bed sizing
  * current local `arm64-v8a` debug build now sends one bounded printer-geometry cluster, one bounded filament-temperature cluster, and bounded process settings into the native slice-config path:
    * bed width / depth / max height -> `printable_area` / `printable_height`
    * nozzle diameter -> `nozzle_diameter`
    * filament diameter -> `filament_diameter`
    * filament material -> `filament_type`
    * nozzle temperature -> `nozzle_temperature` / `nozzle_temperature_initial_layer`
    * bed temperature -> `hot_plate_temp` / `hot_plate_temp_initial_layer`
    * cooling baseline -> `fan_min_speed` / `fan_max_speed`
    * no cooling for first X layers -> `close_fan_the_first_x_layers`
    * layer height -> `layer_height` / `first_layer_height`
    * first layer print speed -> `initial_layer_speed`
    * top shell layers -> `top_shell_layers`
    * bottom shell layers -> `bottom_shell_layers`
    * seam position -> `seam_position`
    * precise wall -> `precise_outer_wall`
    * top surface pattern -> `top_surface_pattern`
    * wall / perimeter count -> `wall_loops`
    * skirts -> `skirt_loops`
    * brim width -> `brim_width`
    * infill density -> `sparse_infill_density`
    * sparse infill pattern -> `sparse_infill_pattern`
    * print speed baseline -> temporary fallback parent for still-unsurfaced speed keys only
  * exact real-device proof on `RFCYA01ANVE` is now:
    * nozzle diameter changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture
    * filament diameter changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture
    * nozzle temperature changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture, but only through the emitted start-temperature command
    * bed temperature changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture, but only through the emitted start-bed-temperature command
    * cooling baseline changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture, but only through emitted part-cooling fan commands
    * no cooling for first X layers changes exported G-code on the bounded `mobileslicer_test_cube.stl` fixture, but only by delaying the first emitted fan-enable command
    * layer height changes exported G-code
    * first layer print speed changes exported first-layer feedrates on the accepted `mobileslicer_test_cube.stl` fixture
    * top shell layers change exported G-code on the bounded `mobileslicer_test_cube.stl` fixture
    * bottom shell layers change exported G-code on the bounded `mobileslicer_test_cube.stl` fixture
    * top surface pattern changes exported G-code on the stronger flat-roof `mobileslicer_test_cube.stl` fixture
    * precise wall changes exported G-code on the bounded cached `selected-model-wall_smoke_box_20mm.stl` fixture
    * wall / perimeter count changes exported G-code on the stronger cached `selected-model-3DBenchy_1_.stl` fixture
    * infill density changes exported G-code on both the bounded `selected-model-ms_box_20mm.stl` fixture and the stronger cached `selected-model-3DBenchy_1_.stl` fixture
    * sparse infill pattern changes exported G-code on the accepted `mobileslicer_test_cube.stl` fixture, including the restored `Gyroid` path
    * print speed baseline changes exported G-code on the bounded cached `selected-model-ms_box_20mm.stl` fixture, and the current accepted cube rerun now also shows the hidden first-layer infill speed path honestly on-device
    * outer wall speed changes emitted `Outer wall` feedrates on the accepted `mobileslicer_test_cube.stl` fixture
    * inner wall speed changes emitted `Inner wall` feedrates on the accepted `mobileslicer_test_cube.stl` fixture
    * top surface speed changes emitted `Top surface` feedrates on the accepted `mobileslicer_test_cube.stl` fixture
    * travel speed changes emitted non-first-layer travel feedrates on the accepted `mobileslicer_test_cube.stl` fixture
    * outer wall acceleration changes emitted `Outer wall` acceleration handling on the accepted `mobileslicer_test_cube.stl` fixture
    * top surface acceleration changes emitted `Top surface` acceleration handling on the accepted `mobileslicer_test_cube.stl` fixture
    * sparse infill acceleration changes emitted `Sparse infill` acceleration handling on the accepted `mobileslicer_test_cube.stl` fixture
  * current compact Stage 2 classification on `RFCYA01ANVE`:
    * Printer: `Bed dimensions -> Error-state only`, `Nozzle diameter -> Device tested`, `Filament diameter -> Device tested`
    * Filament: `Filament material -> Config only`, `Max volumetric speed -> Device tested`, `First-layer nozzle temperature -> Config only`, `Other-layers nozzle temperature -> Config only`, `First-layer bed temperature -> Config only`, `Other-layers bed temperature -> Config only`, `Cooling baseline -> Config only`, `No cooling for first X layers -> Config only`
    * Process: `Layer height -> Device tested`, `First layer height -> Device tested`, `First layer print speed -> Device tested`, `First layer infill speed -> Device tested`, `First layer travel speed -> Device tested`, `Slow layers -> Device tested`, `Outer wall speed -> Device tested`, `Inner wall speed -> Device tested`, `Top surface speed -> Device tested`, `Travel speed -> Device tested`, `Outer wall acceleration -> Device tested`, `Inner wall acceleration -> Device tested`, `Top surface acceleration -> Device tested`, `Sparse infill acceleration -> Device tested`, `Top shell layers -> Device tested`, `Bottom shell layers -> Device tested`, `Seam position -> Device tested`, `Precise wall -> Device tested`, `Only one wall on top surfaces -> Stronger-fixture proven`, `Top surface pattern -> Stronger-fixture proven`, `Wall count -> Stronger-fixture proven`, `Skirts -> Device tested`, `Brim width -> Device tested`, `Infill density -> Device tested`, `Sparse infill pattern -> Device tested`
  * current special-case notes:
    * `bed dimensions` stay `Error-state only`: the fresh typed-extra rerun in this review used source model `/data/local/tmp/mobileslicer_test_cube.stl`, staged that to `/data/user/0/com.mobileslicer/cache/selected-model-mobileslicer_test_cube.stl`, exported baseline `/sdcard/Download/stage2_beddims_baseline.gcode`, and then failed the `10 x 10 x 10 mm` variant before export with `nativeError=printable volume exceeded via fallback check areaExceeded=1 heightExceeded=1 offendingLine=43 gcode="G1 X17.14 Y2.86 E.8648 ; perimeter"`; this remains a printable-volume rejection boundary, not toolpath reshaping proof
    * `filament_type` stays `Config only`: the current path writes Orca `filament_type` only and does not load a type-specific preset bundle from that field alone
    * `max volumetric speed` is now `Device tested`: the Filament editor now owns Orca `filament_max_volumetric_speed`, and the accepted gyroid `mobileslicer_test_cube.stl` `12 -> 24 mm3/s` proof on `RFCYA01ANVE` lifts emitted non-first-layer feedrates while feedrate-stripped motion stays equal
    * adaptive volumetric speed is surfaced in the Filament editor as `Config only - Waydroid`: it reaches Orca config as `filament_adaptive_volumetric_speed` plus `volumetric_speed_coefficients`, but still needs accepted emitted-G-code proof before it can be promoted to `Device tested`
    * `first_layer_print_speed` is now `Device tested`: the current bounded `mobileslicer_test_cube.stl` `100 -> 10 mm/s` proof on `RFCYA01ANVE` changes emitted first-layer `G1 F...` feedrates in `Brim`, `Inner wall`, and `Outer wall` blocks while geometry stays equal after feedrate stripping
    * `first layer infill speed` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `75 -> 15 mm/s` proof on `RFCYA01ANVE` changes first-layer `;TYPE:Bottom surface` feedrate from `G1 F4500` to `G1 F900` while feedrate-stripped motion stays equal
    * `first layer travel speed` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `50% -> 100%` proof on `RFCYA01ANVE` changes first-layer travel to the first infill point from `G1 ... F3600` to `G1 ... F7200` while feedrate-stripped motion stays equal
    * `slow layers` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `0 -> 3` proof on `RFCYA01ANVE` changes emitted early-layer feedrates above layer 1, including second-layer `Inner wall` / `Outer wall` moves dropping from `G1 F6000` to `G1 F3200`
    * bounded per-feature wall/top/travel speeds are now `Device tested`: the accepted `mobileslicer_test_cube.stl` proof subset on `RFCYA01ANVE` keeps normalized motion equal while one-variable reruns change emitted `Outer wall`, `Inner wall`, `Top surface`, and non-first-layer travel feedrates
    * `bridge_speed` is now `Device tested`: the accepted `stage2_bridge_speed_fixture.stl` `10 -> 40 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Overhang wall` and `;TYPE:Bridge` feedrates from `G1 F600` to `G1 F1807` while feedrate-stripped motion stays equal
    * `small perimeter threshold` is now `Device tested`: the accepted stronger `stage2_small_perimeter_array_fixture.stl` `0 -> 20 mm` proof on `RFCYA01ANVE` activates small-feature perimeter handling and changes emitted `;TYPE:Inner wall` / `;TYPE:Outer wall` feedrates from `G1 F6000` to `G1 F600`
    * `small perimeter speed` is now `Device tested`: the accepted stronger `stage2_small_perimeter_array_fixture.stl` `10 -> 50 mm/s` proof on `RFCYA01ANVE` changes those same emitted small-feature `Inner wall` / `Outer wall` feedrates from `G1 F600` to `G1 F3000`
    * `sparse infill speed` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Sparse infill` feedrates from `G1 F1200` to `G1 F3822` while feedrate-stripped motion stays equal
    * `internal solid infill speed` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `20 -> 80 mm/s` proof on `RFCYA01ANVE` changes emitted `;TYPE:Internal solid infill` feedrates from `G1 F1200` to `G1 F4800` while feedrate-stripped motion stays equal
    * `gap infill speed` remains `Config-labeling-only effect`: dedicated `stage2_gap_infill_strip_fixture.stl` and `stage2_gap_infill_narrow_strip_fixture.stl` reruns on `RFCYA01ANVE` echoed the config correctly but still did not emit a `;TYPE:Gap infill` block or change executable motion
    * bounded wall / top-surface / sparse-infill acceleration subset after the accepted reruns:
      * `outer wall acceleration` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` proof on `RFCYA01ANVE` removes the emitted `M204 S500` reset before `;TYPE:Outer wall` blocks while acceleration-stripped motion stays equal
      * `inner wall acceleration` is now `Device tested`: the accepted stronger `stage2_inner_wall_acceleration_tall_box_fixture.stl` `1000 -> 500 mm/s²` proof on `RFCYA01ANVE` changes emitted `M204` immediately before `;TYPE:Inner wall` from `M204 S1000` to `M204 S500` while acceleration-stripped motion stays equal
      * `top surface acceleration` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `500 -> 2000 mm/s²` rerun changes emitted `;TYPE:Top surface` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
      * `sparse infill acceleration` is now `Device tested`: the accepted `mobileslicer_test_cube.stl` `500 -> 1500 mm/s²` rerun changes emitted `;TYPE:Sparse infill` acceleration handling from `M204 S500` to `M204 S1500` while acceleration-stripped motion stays equal
    * `precise wall` is now `Device tested`: the current cached `wall_smoke_box_20mm` `precise_outer_wall true -> false` proof on `RFCYA01ANVE` changes stripped executable output, motion-only body, and `;TYPE:` sequence while setup/start commands stay equal
    * `top surface pattern` is `Stronger-fixture proven`: the older cached `selected-model-100ml_cube.stl` proof was overclaimed, but the accepted flat-roof `mobileslicer_test_cube.stl` `monotonicline -> concentric` proof on `RFCYA01ANVE` still changes stripped executable output and motion-only body while setup/start commands stay equal; the narrower missing-final-`Top surface` bug on that concentric fixture is now fixed on the real Android path, but the restored app output still drifts from the bounded Orca probe inside the final top-surface block
    * `only_one_wall_top` is now surfaced as `Only one wall on top surfaces` in the `Process` editor and carried through the app-owned config path; the accepted `mobileslicer_test_cube.stl` `true -> false` proof on `RFCYA01ANVE` changes the final roof-region wall/top-surface picture, so it is now `Stronger-fixture proven` without claiming same-config Orca parity
    * `wall_loops` stay `Stronger-fixture proven`: the exact reproduced missing-wall cube on `RFCYA01ANVE` is now fixed again for real export, but the cause was wrapper default `wall_generator = arachne`, not missing `wall_loops` transport; the one-variable `wall_loops` proof itself still comes from stronger fixtures such as `Box_v2.stl`
    * current rerun caveat for that exact cube fix: the reliable proof path used the app-cached STL copy under `/data/user/0/com.mobileslicer/cache/...`; direct external `/sdcard/Download/...` absolute-path loading was not the reliable rerun path on that pass because `nativeLoadModel` rejected it
    * `skirts` are now `Device tested`: the current bounded `mobileslicer_test_cube.stl` `0 -> 3` proof adds emitted skirt toolpaths on `RFCYA01ANVE`
    * `seam_position` is now `Device tested`: the latest user-confirmed on-device result on `RFCYA01ANVE` is authoritative for this repo's Stage 2 UI truth
    * `brim_width` is now `Device tested`: the current bounded `mobileslicer_test_cube.stl` `0 -> 4 mm` proof on `RFCYA01ANVE` now emits a first-layer `;TYPE:Brim` block and changes executable motion after the wrapper switches `brim_type` explicitly and the Android print subset links the real Orca `Brim.cpp`
    * `sparse infill pattern` is now `Device tested`: the accepted fixed `mobileslicer_test_cube.stl` matrix on `RFCYA01ANVE` now emits real `;TYPE:Sparse infill` blocks for `Grid`, `Gyroid`, and `Cubic`; the exact fix was linking the real `FillGyroid.cpp` into the Android print subset and removing the no-op `FillGyroid` stub
    * current proof-harness caveat from this review: raw `automation_config_json` shell delivery still does not count unless the device status artifact echoes the full JSON back intact; the accepted fresh top-surface rerun used shell-escaped compact JSON and verified the echoed config before counting the proof
  * `Profiles` and `Workspace` now label these as native slice request fields instead of generic app prep, and they now use one stable proof vocabulary:
    * `Device tested`
    * `Config only`
    * `Start-sequence only`
    * `Layer-change command only`
    * `Fan-command only`
    * `Stronger-fixture proven`
    * `Error-state only`
  * the repo still keeps those finer truth labels in docs
  * app-facing settings status should now collapse to:
    * `Config only`
    * `Device tested`
  * `Profiles` should show that simplified status directly inside each domain section through the compact `Setting truth` card, while the repo docs keep the finer proof detail
  * that per-setting truth-card UI is now verified live on `RFCYA01ANVE`, including the Filament-side cooling rows and `Process > First layer height`, `Process > Seam position`, `Process > Skirts`, `Process > Brim width`, and the bounded per-feature wall/top/travel speed subset
  * the `Filament` editor now also exposes `No cooling for first X layers` in `Cooling`
  * the `Process` editor now follows the Orca-style top-level order:
    * `Identity`
    * `Quality`
    * `Strength`
    * `Speed`
    * `Support`
    * `Multimaterial`
    * `Others`
  * current `Process` section placement is:
    * `Quality`: `Initial layer height`, `Layer height`, `Seam position`, `Top surface pattern`
    * `Strength`: `Top shell layers`, `Bottom shell layers`, `Wall / perimeter count`, `Infill density`, `Sparse infill pattern`, `Precise wall`, `Only one wall on top surfaces`, `Skirts`, `Brim width`
    * `Speed`: `First layer`, `First layer infill`, `First layer travel speed`, `Number of slow layers`, `Outer wall`, `Inner wall`, `Top surface`, `Travel`, and the current explicit per-feature speed/bridge controls
* Current app-layer-only scope:
  * no remaining imported Orca-related printer / filament / process profile field is app-only in the current Stage 2 tranche
* Truth-label rule for Stage 2 UI wording:
  * app-facing setting labels should be only:
    * `Config only`
    * `Device tested`
  * map the finer repo truth into those app labels like this:
    * `Config only`:
      * the field is surfaced and wired, but the repo does not yet carry accepted real-device proof
    * `Device tested`:
      * accepted real-device evidence exists, even if the repo keeps finer nuance such as `Fan-command only`, `Start-sequence only`, or `Stronger-fixture proven`
  * `README/SETTINGS_CHECKLIST.md` is now the accountability list for full Orca-style settings coverage
* Next milestone direction:
  * `Profiles` should continue expanding toward full Orca-style `Printer` / `Filament` / `Process` settings coverage
  * that expansion should happen one bounded settings cluster at a time
  * a settings cluster does not count as implemented product progress unless it produces real verified app behavior or slice/output change, not just editable UI
  * broader Orca-style settings/function coverage now takes priority over further non-settings UI work
  * `README/SETTINGS_CHECKLIST.md` should be kept current as the accountability surface for that expansion
  * long-term, the current hand-wired field approach should give way to a more metadata-driven settings layer so broader Orca-backed coverage can scale without uncontrolled UI sprawl
* Planned non-advanced Orca-style editor surface:
  * recently closed app-surface gap:
    * `Process > Initial layer height` is now its own editable control above `Layer height`
    * this exposes the already-supported `first_layer_height` native path; it is not a new backend setting import
  * near-term additions the app should grow toward:
    * `Process`: `one-wall` toggles, shell thickness, key surface patterns, infill patterns
  * medium-term additions:
    * `Process`: one-wall toggles, shell thickness, key surface patterns, infill patterns, first-layer speed cluster, remaining per-feature speed subsets
    * `Filament`: fan-threshold pairs, `Keep fan always on`, `Slow down for layer cooling`, `Don't slow down outer walls`, `Force cooling for overhangs and bridges`, `Retraction length`, `Z-hop height`
  * later / higher-risk additions:
    * `Support`: enable/type/threshold/basic placement controls
    * `Filament`: chamber/idle/softening/pressure-advance/aux-fan/exhaust-style controls
* UI rule for this planned surface:
  * if a field is treated as part of the supported app-owned `Printer` / `Filament` / `Process` surface, it needs a real editable control in the appropriate editor
  * do not quietly couple two user-facing settings together in UI just because the backend can currently infer one from the other
  * for surfaced Orca-backed settings, the `Show advanced profile settings`
    gate must follow Orca's own config mode metadata from
    `vendor/orcaslicer/src/libslic3r/PrintConfig.cpp`
    * `comAdvanced` => hide by default unless the toggle is enabled
    * `comSimple` => show by default
    * do not rely on `README/SETTINGS_CHECKLIST.md` tags alone when adding a
      new surfaced setting; the checklist must mirror Orca mode, not replace it
    * if Orca's raw mode metadata disagrees with the actual current Orca GUI
      placement for a surfaced setting, follow Orca GUI layout in
      `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp` for app-facing visibility
      behavior
    * if a surfaced control is a documented special-case that does not exist as
      a normal live mode-tagged Orca config entry in this vendored tree, do not
      treat missing/default mode metadata as proof that it is simple; follow
      the documented checklist exception instead
    * this gate is only visibility; it must not be used as a tab/category
      model, because Orca has real `Advanced` pages in some editors
  * for surfaced Orca-backed settings, tab ownership and in-tab order must also
    follow Orca's GUI layout in `vendor/orcaslicer/src/slic3r/GUI/Tab.cpp`
    * use Orca's page ownership first
    * Filament page ownership is `Filament`, `Cooling`, `Setting Overrides`,
      `Advanced`, `Multimaterial`, `Dependencies`, `Notes`
    * Process page ownership is `Quality`, `Strength`, `Speed`, `Support`,
      `Multimaterial`, `Others`
    * inside each page, keep the same high-level option-group order Orca uses
      unless the mobile surface is intentionally narrower and the deviation is
      documented
    * Orca's `Advanced` filament page is a real top-level page for filament
      start/end G-code; flow ratio, Pressure Advance, temperature, and
      volumetric speed controls stay on Orca's `Filament` page even when their
      visibility is advanced-only
  * Orca-like naming can stay user-facing even when the effective backend variable name differs, for example:
    * UI label: `First layer height`
    * effective Orca parameter: `initial_layer_print_height`
* Future workflow direction that belongs in UI planning too:
  * after the core settings/workspace surface is materially stronger, the app should grow toward:
    * clearer slice-result summary
    * bounded preview/inspection tools
    * direct printer upload/send workflow
  * those should be treated as later workflow wins, not as permission to outrun current slicer correctness
* Current UX/readiness refinements:
  * built-in versus custom profile behavior is now clearer:
    * built-ins are editable and persisted on-device but remain protected from deletion
    * custom profiles can be duplicated, renamed, edited, and deleted
  * active selection now uses a fuller summary card instead of only a single-line recap
  * numeric profile fields now use numeric keyboards on mobile
  * successful STL import now prepares the `StlMesh` immediately so `Workspace` can reuse the cached mesh instead of reparsing the STL on entry in the same session
* Honest verification limit:
  * local source review and successful build confirm the dedicated `Profiles` screen, the new Android-app-side profile model, local persistence, refined CRUD behavior, printer-bed-to-workspace wiring, cached STL mesh reuse on workspace entry, and bounded JNI config wiring for native `layer_height`, `first_layer_height`, `wall_loops`, and `sparse_infill_density` on the current local `arm64-v8a` build
  * local `x86_64` app packaging is now disabled by default because the Android
    app must not silently link the reduced wrapper path; Waydroid checks require
    explicit real-wrapper x86_64 dependency staging
  * rebuilt APK was installed on `RFCYA01ANVE`
  * coder did not use `tools/adb` for taps, dumps, logs, screenshots, or runtime probing
  * hands-on device confirmation is still required for the full editing flow and for proving print-speed G-code effect before any stronger product claim

## Current Preview Info Boundary

* `Preview` now has an `Info` action beside `Prepare` when the current slice
  exposes rich native G-code statistics. The bottom sheet is named
  `G-code stats`.
* The UI intentionally uses a bottom sheet with `Line Type`, `Filament`, `Time`,
  and `Cost` tabs:
  * this keeps Orca's information model but avoids a desktop-sized floating
    legend on a phone viewport
  * `Line Type` rows mirror Orca/libvgcode colors and expose real show/hide
    behavior
  * `Filament`, `Time`, and `Cost` are compact mobile summaries of Orca
    `GCodeProcessorResult` data, not Android-side guesses
* Future preview inspection additions should keep this same rule:
  * source data comes from Orca/native G-code processing
  * Android owns presentation and interaction state only
  * toggles that imply render changes must call native viewer state, not only
    alter text in the sheet
  * seam visibility depends on processor-backed libvgcode preview vertices
    (`GCodeProcessorResult::moves` -> `EMoveType::Seam`), not a UI-only stats
    row
* Rotation recreates the viewer `SurfaceView` at the new viewport size, then
  restores camera orbit/pan/zoom and replays explicit line-type visibility
  choices so the sheet and rendered preview stay in sync.

## Workspace Preview Chrome Boundary

* In Preview, the top bar is reserved for commands: `Send`, printer `UI`,
  `Info`, and `Prepare`.
* Printer runtime state stays visible in Preview, but it lives in the bottom
  workspace panel as a compact status badge. Do not put `Printing`, `Standby`,
  or similar status chips back into the top row; narrow phones wrap the
  `Prepare` button when that row is crowded.
* Compact landscape mode may shrink the bottom panel, but the same status badge
  still appears there so runtime state does not disappear after rotation.
* Prepare mode multi-object selection should stay dense: selected object,
  object count, and `Objects` access belong in one short row rather than a tall
  repeated-card layout.

## Current Theme Boundary

* Exact cause of the prior theme issue:
  * theme mode state was already persisted and passed into `MobileSlicerTheme`
  * the shell still used many hardcoded dark-only colors and gradients, so switching theme mode did not make the app visually behave as expected
* Latest local fix:
  * shell background, cards, sheets, settings rows, tab strip, and top-bar chrome now derive from the active Material theme instead of staying hardcoded dark
  * accent palettes are app/UI colors and remain separate from the Workspace 3D world color
  * accent-filled controls now use a mode-based foreground rule for parity:
    * light theme uses dark ink foreground on every accent
    * dark theme uses near-white gray foreground on every accent
    * do not reintroduce per-accent luminance foreground switching unless the full palette is redesigned and checked on device
    * every future accent color must be tested against this same foreground rule in both light and dark theme before it is kept
  * the current accent set is `Blue`, `Cyan`, `Green`, `Yellow`, `Rose`, `Red`, `Orange`, and `Graphite`
  * the current world-view set is `Slate`, `White`, `Mist`, `Graphite`, `Deep`, `Navy`, `Charcoal`, and `Black`; `Slate` is the default world color
* Honest verification limit:
  * local code review and successful build confirm the theme wiring change
  * final visible confirmation of `System`, `Light`, and `Dark` behavior still requires hands-on device validation

## Current Settings Boundary

* Latest local implementation:
  * `Settings` is now a full-page screen reached from the top-right cog action
  * scope remains app-only:
    * theme mode
    * accent color
    * 3D world-view color
    * an `Advanced` placeholder with an off-by-default checkbox-style gate for future deeper Orca profile controls
    * legal/about/info
    * support placeholder
* Honest verification limit:
  * local source review and successful build confirm the screen-level routing change
  * final visual and interaction confirmation on-device still requires user hands-on validation
