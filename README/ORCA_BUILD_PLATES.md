# Orca Build Plates

This file is the source of truth for the accepted Android `Prepare` build-plate
behavior.

## Decision

Mobile Slicer keeps Orca-specific build plates in the Android `Prepare`
workspace.

For imported Orca printer profiles, the workspace should use the same printer
bed assets OrcaSlicer declares in the profile catalog:

* `bed_model` for the physical plate/rim shape
* `bed_texture` for printer labels, stickers, warning marks, and surface art
* `printable_area` and printable height for profile dimensions and bounds checks

The previous generic Android procedural bed remains only as the fallback for
custom profiles or imported profiles that do not provide Orca bed assets.

## Asset Pipeline

`scripts/generate_orca_printer_assets.py` builds the Android printer asset
manifest from `vendor/orcaslicer/resources/profiles`.

The generated APK assets include:

* Orca bed STL models under `orca-printers/bed-models/`
* rasterized bed textures under `orca-printers/bed-textures/`
* printer manifest fields `bedModelAssetPath` and `bedTextureAssetPath`

Texture conversion keeps Android rendering simple:

* SVG textures are rasterized to PNG
* PNG textures are copied or normalized as PNG
* `cairosvg` is preferred when available
* ImageMagick `magick` is the fallback converter

Gradle tracks the relevant Orca profile inputs, including recursive machine and
process JSON plus STL, SVG, and PNG bed assets, so changes in vendored Orca bed
resources regenerate the Android assets.

The latest local generator proof produced:

* `359` printer presets
* `323` presets with Orca bed models
* `309` presets with Orca bed textures
* `219` unique bed STL files
* `188` unique rasterized bed texture PNG files

## Profile Flow

`OrcaPrinterPreset` stores the generated `bedModelAssetPath` and
`bedTextureAssetPath` values.

Imported `PrinterProfile` records and persists both fields, then passes them
through `PrinterProfile.toBedSpec()` into the workspace renderer. Existing
imported Orca profiles also have a derivation fallback from the stored Orca
family plus `bed_model` / `bed_texture`, so users do not have to reimport every
printer just to pick up generated bed assets.

## Render Flow

When an Orca bed asset path exists, the workspace intentionally does not upload
the old generic Android plate surface, walls, or border. This keeps the Orca
path visually correct and avoids doing extra renderer work for a hidden plate.

The Orca render path is:

1. load and cache the Orca STL bed model from APK assets
2. normalize the bed model just below the model grounding plane
3. split STL triangles into opaque top/rim faces and transparent underside
   faces by triangle normal direction
4. draw the opaque bed model with face culling
5. draw the underside as a separate transparent pass
6. draw a lightweight Orca-style grid overlay
7. draw the rasterized Orca bed texture on top so stickers and labels cover the
   grid from above
8. draw staged STL models and native Preview content through their normal paths

The grid is independent geometry, not baked into the texture. It mirrors Orca's
normal build-plate spacing for phone-sized beds: 10 mm lines with stronger
50 mm guides. In the Orca path the grid is double-sided, so it remains visible
through the transparent underside while the texture is front-face-only.

Some Orca textures, including Prusa Core One-family textures, already contain
their desktop grid. Those textures should not receive the Android overlay grid,
or the bed reads as doubled/heavy. The renderer suppresses the overlay grid for
known grid-bearing textures from the top view and keeps it for sticker-only
textures such as the Qidi path. For underside views, the renderer still keeps a
generated grid available and draws it bottom-only with face culling so the
transparent underside does not become a blank slab.

The accepted underside behavior is not a full-bed alpha wash. The bed model is
split so only downward-facing underside triangles are transparent; top labels,
stickers, and the main surface stay visually stable from above.

## Fallbacks And Limits

If no Orca bed model or texture is available, the workspace falls back to the
older procedural Android build plate. This keeps custom printers usable.

If an Orca asset path exists but one asset fails to load, the Orca path renders
the pieces that did load instead of silently replacing them with the old generic
plate.

Known refinement targets:

* honor any future profile-specific texture-area offset/crop metadata if needed
* tighten non-rectangular grid clipping for unusual bed silhouettes if visual
  proof shows it matters
* compare more printer families against desktop OrcaSlicer screenshots as the
  imported profile catalog expands

Current device proof on `RFCYA01ANVE` confirmed this behavior for Qidi Q2,
sampled Bambu P2S, and Prusa Core One-family workspace views.
