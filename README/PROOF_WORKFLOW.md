# Proof Workflow

This file defines the standing proof discipline for Mobile Slicer feature and
slice-setting work.

Use it when:

* wiring a new `Printer`, `Filament`, or `Process` field
* reviewing a claimed on-device proof
* narrowing UI wording
* deciding whether a setting is still missing, only config-transported, or already proven under the current Stage 2 checklist statuses

## Current Truth Rule

Keep `CURRENT_STATUS.md` short and current.

* `CURRENT_STATUS.md` is for the live verified boundary only
* `README/CHANGELOG.md` is for historical proof detail
* if current-state wording starts turning into a diary, move that detail into the changelog
* `NEXT_PROMPT.md` is an operational handoff file, not a truth source
* prompt text, review notes, and operator instructions should not be treated as verified product truth unless they are also reflected in the real truth docs

## Tracked-File Hygiene

If a file affects any of the following, it must not be left untracked:

* repo truth
* UI truth text
* slice-input wiring
* proof workflow
* proof artifact interpretation

Examples:

* `ProfileModels.kt`
* `MainActivity.kt`
* `CURRENT_STATUS.md`
* `README/ANDROID_UI.md`
* `README/TASKS.md`
* `README/BUILD_SYSTEM.md`
* `README/CHANGELOG.md`

Do not rely on an untracked file as the only place where a milestone boundary or
proof wording lives.

## Dirty Tree Triage

Do not treat every dirty-file bucket the same.

When the worktree is noisy, classify it into exactly these buckets first:

* real source changes
  * feature work, proof wiring, UI truth text, docs, prompts, or workflow files
  * examples:
    * `android-app/app/src/main/java/com/mobileslicer/MainActivity.kt`
    * `android-app/app/src/main/java/com/mobileslicer/ProfileModels.kt`
    * `engine-wrapper/orca_wrapper.cpp`
    * `CURRENT_STATUS.md`
    * `README/ANDROID_UI.md`
* tracked generated junk
  * generated outputs that are already tracked in Git and therefore cannot be solved with `.gitignore`
  * current known example:
    * `engine-wrapper/orca-android-libslic3r/android-deps-build/openssl-src`
  * do not mix cleanup of this bucket into setting-proof runs unless the task is explicitly about dependency/build-tree cleanup
  * this bucket should eventually get its own bounded cleanup run instead of staying mixed into feature review forever
* ignore candidates
  * local tool caches, transient logs, and generated scratch state that should never enter review
  * current example:
    * `android-app/.kotlin/`

Expected action by bucket:

* stage truth-bearing source changes so review can see them
* add ignore rules for true local-tool noise
* leave tracked generated junk alone during feature/proof work unless the run is explicitly a cleanup run

## Domain Classification

Every newly wired or reviewed setting must be classified in its correct domain:

* `Printer`
* `Filament`
* `Process`

Do not count a field as progress merely because the editor exists. A setting only
counts as real progress when the repo can state honestly which of the proof
classes below it belongs to.

Imported-Orca-setting rule:

* every Orca setting brought into the app must be placed in the correct UI section:
  * `Printer`
  * `Filament`
  * `Process`
* the run is not complete until that setting is also classified honestly in both docs and UI

## Canonical Statuses

Use the settings checklist status system as the canonical current vocabulary for
live docs and UI truth:

* `Missing`
  * the setting is not yet surfaced in the app
* `Parent surfaced, subset only`
  * the parent control exists, but only some Orca modes or sub-controls are
    currently surfaced
* `Config only`
  * the setting is surfaced and reaches the real config/native path
  * this does not yet claim accepted real-device proof
* `Config only - Waydroid`
  * exploratory config-path proof exists only on Waydroid
  * do not collapse this into real-device proof
* `Device tested`
  * accepted real-device evidence exists on `RFCYA01ANVE`

## Legacy Mapping

Older notes may still use finer or earlier labels. Interpret them like this:

* `app-only`
  * usually means a field existed in Android UI/persistence only
  * in current checklist terms, this should normally be treated as not yet
    surfaced truth until the field is moved into the checklist honestly
* `Source-wired`
  * current live equivalent: `Config only`
* `Device-proven`
  * current live equivalent: `Device tested`

Do not introduce new live truth claims using the legacy labels when the
checklist status can say the same thing more clearly.

## Proof Qualifiers

Use these as proof qualifiers or changelog language when the bare checklist
status is not specific enough:

* `Error-state only`
  * the current path proves only that violating the field can fail slice or
    export
  * do not imply toolpath reshaping or broader preset behavior from this class
    alone
* `Start-sequence only`
  * exported output changed only in emitted setup / temperature commands, not
    toolpath motion
* `Layer-change command only`
  * exported output changed only in emitted non-motion commands inserted after a
    layer change
  * do not imply start-sequence drift or toolpath-motion change from this class
    alone
* `Fan-command only`
  * exported output changed only in emitted part-cooling / fan commands, not
    toolpath motion
* `Config-labeling-only effect`
  * exported output changed only in config or comment labeling, not emitted
    setup commands or executable motion
* `Stronger-fixture proven`
  * the field is proven only on stronger or specific fixtures
  * do not imply general visible impact on every model
* `not yet generalized as user-meaningful`
  * export proof exists, but the repo does not claim the effect is always
    obvious in ordinary use

## Required Proof Artifacts

For every claimed real-device proof run, record all of the following:

* actual device model path used
* exact config JSON for baseline and each variant
* device artifact path
* host-copied artifact path
* file byte count
* file line count
* `;TYPE:` or equivalent structural differences when relevant
* executable-body equality after stripping comment/config noise
* one concrete example diff line when a meaningful change exists

When relevant, also record:

* whether emitted temperature commands changed
* whether emitted fan commands changed
* whether `G0` / `G1` motion changed
* whether only extrusion values changed
* whether the effect is limited to start-sequence commands
* whether the effect is limited to non-motion layer-change commands

Preferred temporary artifact layout:

* `/tmp/mobileslicer-proof/<field-or-cluster>/<run-id>/...`

If an older ad-hoc path is reused for a bounded rerun, keep it consistent inside that run
and still record the exact paths.

## Device-Proof Validity

A device proof run does not count unless the automation or workflow actually ran.

Use a validity discipline like this when automation is involved:

* wake and unlock the device
* keep the screen awake when needed
* prefer a fresh launch when the automation entry point only runs in `onCreate`
* wait for the generated status artifact rather than trusting launch alone
* verify the actual staged model path rather than guessing an external path
* do not count a proof run if the proof used a stale APK, stale install, or parallel build/install race

If the app was warm-started, background-frozen, or skipped the automation entry
point, do not treat the result as valid proof.

Practical stale-APK rule:

* build completes
* install completes
* only then can the proof run count

## Fixture Taxonomy

Keep fixture roles explicit and stable.

Current known roles:

* weak bounded proof fixture:
  * `/data/user/0/com.mobileslicer/cache/selected-model-ms_box_20mm.stl`
  * useful for:
    * `nozzle_diameter`
    * `filament_diameter`
    * `nozzle_temperature` start-sequence effect
    * `sparse_infill_density`
  * too weak for:
    * `wall_loops`
  * currently not useful for:
    * `bed_temperature`
* stronger wall/infill fixture:
  * `/data/user/0/com.mobileslicer/cache/selected-model-3DBenchy_1_.stl`
  * required to reproduce:
    * `wall_loops`
  * also reproduces:
    * `sparse_infill_density`

If a fixture proves too weak for a claim, record that explicitly instead of
silently switching models.

## UI Truth Rule

UI truth should lag proof slightly, not lead it.

* if a field is newly wired today, do not present it as product truth until the
  proof survives review
* if a field has only a `Start-sequence only` effect, say that
* if a field has only a `Layer-change command only` effect, say that
* if a field is only `Stronger-fixture proven`, say that
* if a field is still `Config only` or `Config only - Waydroid`, say that

When an Orca setting is imported into the app, the UI should expose its current
status in the correct domain rather than hiding that status only in docs or
changelog text.

Do not overclaim preset semantics:

* one mapped Orca key does not automatically equal a full Orca preset bundle
* if the app currently writes only one narrow Orca-facing key, classify only that narrow meaning
* do not describe a field as “material presets”, “printer presets”, or similar unless the current path really loads and applies that broader preset behavior

Definition of done for an imported Orca setting:

* the setting exists in the correct `Printer` / `Filament` / `Process` section
* the backend linkage is classified honestly
* the UI reflects that classification
* the docs reflect that classification
* device proof is recorded when applicable

## Supervisor Review Checklist

When reviewing coder output, check:

* real device used?
* right domain classification?
* one variable changed per comparison?
* actual staged model path recorded?
* exact config JSON recorded?
* correct fixture strength used?
* stripped executable-body comparison checked?
* effect classified honestly?
* UI/docs kept narrower than or equal to proof?
* truth-bearing files tracked in Git?

If any of those are missing, the step is not fully closed.
