# Kotlin Refactor Map

This document records the current Kotlin file boundaries after the large
non-networking split pass. It is meant to help future work land in the right
file instead of rebuilding the old monoliths.

## Verification Baseline

After the split pass, these commands passed:

```bash
scripts/verify_android.sh unit
scripts/verify_android.sh apk
```

The debug APK path is:

```text
android-app/app/build/outputs/apk/debug/app-debug.apk
```

## Main Activity Boundary

`MainActivity.kt` now owns activity startup, Compose root wiring, retained
activity state, and lifecycle cleanup only.

Supporting files:

* `MainActivityAutomation.kt` - automation intent handling and automation runner wiring.
* `MainActivityModelLoading.kt` - URI model loading and workspace mesh preparation.
* `MainActivitySlicing.kt` - native slice orchestration, thumbnail handoff, native failure presentation, and slice memory pressure release.
* `MainActivityGcodeActions.kt` - export, share, printer upload entry points, and printer status/test callbacks.
* `MainActivityCache.kt` - generated G-code, staged model, share cache retention.
* `MainActivityNativeRuntime.kt` - native engine lifecycle and Orca runtime resource staging.
* `MainActivityFileStaging.kt` - model file staging, automation file staging, URI streams, display-name lookup.
* `MainActivityStorage.kt` - profile and saved-project persistence with credential hydration/stripping.
* `MainActivityHelpers.kt` - small stateless helpers such as generated file naming and cleanup helpers.

The extension functions intentionally live in the same `com.mobileslicer`
package and use `internal` activity state. This keeps behavior unchanged while
avoiding another state container layer.

## Home / Model Loader Boundary

`ModelLoaderScreen.kt` still owns the main app-screen state machine and
workspace/home navigation.

Supporting files:

* `PrinterUploadRequestFlow.kt` - upload-request state object and upload job launcher.
* `PrinterBrowserScreen.kt` - in-app printer WebView/camera browser UI and URL safety helpers.
* `HomeLandingSections.kt` - home cards and landing-section UI.
* `SavedProjectHelpers.kt` - saved-project filename, thumbnail, timestamp, and plate-bounds helpers.

Printer browser behavior was moved intact. Networking protocol/client work is
still outside this refactor boundary.

## Profiles Boundary

`ProfilesScreen.kt` owns the profile-screen state machine: import/export
launchers, selected sub-screen routing, editor dialog routing, delete dialog,
and transfer dialogs.

Supporting files:

* `ProfilesLandingSection.kt` - home-page Profiles summary card.
* `ProfilesSelectedTabContent.kt` - selected Printer / Filament / Process tab content and card actions.
* `ProfileCards.kt` - reusable profile card/section UI.
* `ProfileTabStrip.kt` - tab strip UI.
* `ProfileScreenTypes.kt` - editor/delete request types.
* `ProfileSelectionScreens.kt` - package shell for selection screens.
* `OrcaPrinterSelectionScreen.kt` - Orca printer selection and import flow.
* `OrcaFilamentSelectionScreen.kt` - Orca filament selection and import flow.
* `ProcessPresetSelectionScreen.kt` - Orca process preset selection and import flow.
* `ProfileAssetImageLoader.kt` - profile thumbnail asset loading.

## Profile Editors

The profile editors are split by draft state, mapping, options, shared UI, and
tab content.

Printer editor:

* `PrinterProfileEditorDialog.kt` - printer editor dialog and tab body.
* `PrinterProfileEditorDraft.kt` - mutable draft state.
* `PrinterProfileEditorMapping.kt` - draft-to-profile mapping.
* `PrinterProfileEditorOptions.kt` - dropdown option catalogs.
* `PrinterProfileConnectionPicker.kt` - connection picker/status dialogs.

Filament editor:

* `FilamentProfileEditorDialog.kt` - filament editor dialog and tab body.
* `FilamentProfileEditorDraft.kt` - mutable draft state.
* `FilamentProfileEditorMapping.kt` - draft-to-profile mapping.
* `FilamentProfileEditorOptions.kt` - dropdown option catalogs.

Process editor:

* `ProcessProfileEditorDialog.kt` - process editor shell.
* `ProcessProfileEditorSelectedTabs.kt` - selected-tab dispatcher.
* `ProcessProfileEditorDraft.kt` - mutable draft state.
* `ProcessProfileEditorMapping.kt` - draft-to-profile mapping.
* `ProcessProfileEditorOptions.kt` - dropdown option catalogs.
* `ProcessProfileQualityTab.kt`
* `ProcessProfileStrengthTab.kt`
* `ProcessProfileSpeedTab.kt`
* `ProcessProfileSupportTab.kt`
* `ProcessProfileMultimaterialTab.kt`
* `ProcessProfileOthersTab.kt`

Shared editor UI lives in `ProfileEditorCommonUi.kt`.

## Profile Store / JSON / Defaults

Profile storage is split by data model, JSON conversion, defaults, and active
selection logic.

* `ProfileModels.kt` - core remaining profile model definitions.
* `PrinterProfileModels.kt`, `FilamentProfileModels.kt`, and related profile model files - domain profile data.
* `ProfileStoreModels.kt` - `ProfileStore` and related store models.
* `ProfileStoreRepository.kt` - repository facade.
* `ProfileStoreJson.kt` - top-level store JSON.
* `PrinterProfileJson.kt` - printer JSON import/export.
* `FilamentProfileJson.kt` - filament JSON import/export.
* `ProcessProfileJson.kt` - process JSON import/export.
* `ProfileJsonHelpers.kt` - shared JSON helpers.
* `ProfileStoreDefaults.kt` - default/fallback profile definitions.
* `ProfileStoreMerge.kt` - store merge helpers.
* `ActiveSlicerConfiguration.kt` - active slicer configuration model.
* `ActiveSlicerProfileSelection.kt` - active profile selection helpers.

Process defaults intentionally remain empty:

```kotlin
internal fun profileStoreDefaultProcessProfiles(): List<ProcessProfile> = emptyList()
```

There are no visible built-in process profiles. A process must come from Orca
import or user creation.

## Profile Import Boundary

Profile import parsing and mapping are split by phase:

* `ProfileImportParsing.kt` - device payload detection, ZIP/text parsing, and Orca preset type detection.
* `ProfileImportDeviceConverters.kt` - imported device JSON to printer/filament/process profile conversion.
* `ProfileImportFilamentMatching.kt` - filament baseline, inherited filament, material, and printer compatibility matching.
* `ProfileImportPresetMatching.kt` - inherited process and linked printer preset matching.
* `ProfileImportGeometryHelpers.kt` - Orca float-list and printable-area parsing.
* `ProfileImportStoreMerge.kt` - imported store merge and display-name lookup.
* `ProfileImportMappers.kt` - shared profile config scalar helpers, profile selection helpers, color/default material helpers.
* `OrcaPrinterImportMapping.kt` - Orca printer preset to `PrinterProfile`.
* `OrcaFilamentImportMapping.kt` - Orca filament preset to `FilamentProfile`.
* `OrcaProcessImportMapping.kt` - Orca process preset to `ProcessProfile`.

## Native Slice Configuration Boundary

`NativeSliceConfiguration.kt` now owns cache lookup, resolved Orca JSON overlay
ordering, normalization, and final serialization.

Section writers:

* `NativeSlicePrinterConfiguration.kt` - printer fields written into native config JSON.
* `NativeSliceFilamentConfiguration.kt` - filament fields written into native config JSON.
* `NativeSliceProcessConfiguration.kt` - process fields written into native config JSON.
* `NativeSliceFilamentOverrides.kt` - optional filament override keys.
* `NativeSliceConfigurationHelpers.kt` - scalar-list normalization, cache key/cache, merge helpers, and single-material defaults.

Ordering remains important:

1. Start from resolved Orca printer JSON.
2. Merge resolved Orca filament JSON.
3. Merge resolved Orca process JSON.
4. Overlay first-class MobileSlicer printer fields.
5. Overlay first-class MobileSlicer filament fields.
6. Overlay first-class MobileSlicer process fields.
7. Overlay optional filament overrides.
8. Apply single-material defaults and scalar-list normalization.

## Workspace / Viewer Boundary

Workspace UI is split into reusable panels and controls:

* `WorkspaceScreen.kt` - workspace screen orchestration.
* `WorkspaceViewerSurface.kt` - viewer surface bridge.
* `WorkspaceTopBar.kt` - top bar.
* `WorkspacePrinterSendSheet.kt` - send-to-printer sheet.
* `WorkspaceViewerStatePanel.kt` - viewer state/status.
* `WorkspaceControlPanel.kt` - main workspace controls.
* `WorkspaceObjectIcons.kt` - object icon helpers.
* `WorkspaceTransformControls.kt` - object transform controls.
* `WorkspacePreviewLayerControls.kt` - Preview layer controls.

Viewer/rendering files:

* `TouchModelViewerView.kt` - Android view, gestures, and render-thread bridge.
* `WorkspaceRenderThread.kt` - render-thread state owner and GL lifecycle.
* `WorkspaceRenderGlHelpers.kt` - EGL/GL upload helpers.
* `WorkspaceRenderDrawHelpers.kt` - stateless triangle draw helper.
* `GcodePreviewRenderer.kt` - native G-code preview renderer wrapper.

`WorkspaceRenderThread.kt` remains stateful by design. Further splitting should
not move mutable EGL/GL lifecycle state into disconnected helpers unless the
renderer is intentionally redesigned.

## Calibration Boundary

Calibration files:

* `CalibrationsScreen.kt` - calibration screen shell.
* `CalibrationDefinitions.kt` - calibration job definitions and option defaults.
* `CalibrationCards.kt` - calibration list/card UI.
* `CalibrationOptionsDialog.kt` - per-calibration option dialogs.

Calibration config remains temporary per-job JSON override behavior; saved
printer, filament, and process profiles are not mutated.

## Enums / Visibility Registry

Profile enums are split by domain:

* `ProfileTab.kt`
* `PrinterProfileEnums.kt`
* `ProcessProfileEnums.kt`

`ProfileSettingVisibility.kt` is intentionally still a large enum registry.
Kotlin enum entries cannot be split across files without replacing the enum API
used throughout the profile editor. Supporting types/helpers were extracted to:

* `OrcaSettingMetadataModels.kt`
* `ProfileSettingVisibilityHelpers.kt`

Treat `ProfileSettingVisibility.kt` as data/registry, not as a normal logic
class that needs method extraction.

## Networking Boundary

Printer networking is actively being split separately. The current non-networking
refactor intentionally avoided protocol behavior changes.

Networking files include:

* `PrinterConnectionRepository.kt`
* `PrinterConnectionModels.kt`
* `PrinterConnectionAuth.kt`
* `PrinterConnectionFormatting.kt`
* `PrinterConnectionUrls.kt`
* per-provider clients such as `OctoKlipperConnectionClient.kt`,
  `PrusaConnectionClient.kt`, `DuetConnectionClient.kt`,
  `BambuLanConnectionClient.kt`, `FlashforgeConnectionClient.kt`, and related
  provider clients.

During the non-networking split, only compile-only fixes were made here:

* restored missing imports used by existing repository code
* removed a duplicate nested `PrinterProtocol` after a top-level
  `PrinterProtocol` already existed

## Current Large-File Exceptions

Some files remain large because they are registry-like or state-heavy:

* `ModelLoaderScreen.kt` - app screen state machine and workspace/home orchestration.
* `ProfileSettingVisibility.kt` - enum registry.
* `WorkspaceRenderThread.kt` - render-thread state owner.
* `ProcessProfileEditorSelectedTabs.kt` - process tab routing into already-split tab components.
* `PrinterProfileEditorDialog.kt` / `FilamentProfileEditorDialog.kt` - editor tab bodies still hold dense field UI.
* `CalibrationDefinitions.kt` - calibration job/default registry.

Before splitting these further, prefer a behavior-preserving extraction with a
small verification run. Avoid changing public profile enum names or render
thread state ownership casually.
