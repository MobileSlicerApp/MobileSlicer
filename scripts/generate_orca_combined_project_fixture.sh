#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_ORCA_BIN="$ROOT_DIR/vendor/orcaslicer/build/package/bin/orca-slicer"
ORCA_BIN="${ORCA_SLICER_BIN:-$DEFAULT_ORCA_BIN}"
BASE_FIXTURE="${MOBILE_SLICER_ORCA_MODIFIER_PROJECT_SEED:-$ROOT_DIR/regression-fixtures/orca-project-references/modifier-object-settings/modifier-seed.3mf}"
ARTIFACT_ROOT="${MOBILE_SLICER_ORCA_COMBINED_PROJECT_ARTIFACT_ROOT:-$ROOT_DIR/artifacts/orca-project-fixtures}"
REFERENCE_DIR="${MOBILE_SLICER_ORCA_COMBINED_PROJECT_REFERENCE_DIR:-$ROOT_DIR/regression-fixtures/orca-project-references/combined-project-preservation}"
STAMP="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="$ARTIFACT_ROOT/combined-project-preservation-$STAMP"
ORCA_LIB_SHIM="$ROOT_DIR/tools/orcaslicer/lib-shim/usr/lib64:$ROOT_DIR/tools/orcaslicer/lib-shim"

usage() {
  cat <<'USAGE'
Usage:
  scripts/generate_orca_combined_project_fixture.sh

Generates a desktop-Orca canonical project 3MF fixture containing:
  - two plates,
  - named objects,
  - two object-to-filament assignments,
  - object-scoped process settings,
  - one parameter modifier volume with modifier-scoped process settings,
  - one object height/layer range with range-scoped process settings,
  - project settings/config entries,
  - Orca project thumbnails.

This is intentionally a project-only fixture. The local desktop Orca CLI accepts
this combined project package and preserves the relevant schemas on project
export. It still crashes before diagnostics when assemble-list height_ranges
are combined with sliced .gcode.3mf export, so this fixture must not be
described as sliced height-range parity.

Environment:
  ORCA_SLICER_BIN
      Orca binary to canonicalize the seed. Defaults to
      vendor/orcaslicer/build/package/bin/orca-slicer.
  MOBILE_SLICER_ORCA_MODIFIER_PROJECT_SEED
      Modifier seed package used as the base combined-project input.
  MOBILE_SLICER_ORCA_COMBINED_PROJECT_ARTIFACT_ROOT
      Artifact root for generated seed, logs, and canonical output package.
  MOBILE_SLICER_ORCA_COMBINED_PROJECT_REFERENCE_DIR
      Checked reference directory populated after the canonical audit passes.
USAGE
}

log() {
  printf '[generate_orca_combined_project_fixture] %s\n' "$*"
}

fail() {
  printf '[generate_orca_combined_project_fixture] ERROR: %s\n' "$*" >&2
  exit 1
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

[[ -x "$ORCA_BIN" ]] || fail "Orca binary is not executable: $ORCA_BIN"
[[ -f "$BASE_FIXTURE" ]] || fail "Missing modifier seed fixture: $BASE_FIXTURE"
mkdir -p "$RUN_DIR/work" "$RUN_DIR/output"

ORCA_ENV=(
  env
  -u WAYLAND_DISPLAY
  -u EGL_PLATFORM
  -u OBS_VKCAPTURE
  -u OBS_VKCAPTURE_QUIET
  -u VK_INSTANCE_LAYERS
  -u QT_WAYLAND_RECONNECT
  -u DRI_PRIME
  -u MANGOHUD
  "LD_LIBRARY_PATH=$ORCA_LIB_SHIM${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
  "XDG_RUNTIME_DIR=${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
  "XDG_SESSION_TYPE=x11"
  "GLFW_PLATFORM=x11"
  "GDK_BACKEND=x11"
  "QT_QPA_PLATFORM=xcb"
)

SEED_PATH="$RUN_DIR/combined-seed.3mf"
PACKAGE_NAME="combined-project-preservation.3mf"
PACKAGE_PATH="$RUN_DIR/output/$PACKAGE_NAME"
LOG_PATH="$RUN_DIR/orca.log"

python3 - "$BASE_FIXTURE" "$RUN_DIR/work" "$SEED_PATH" <<'PY'
import shutil
import sys
import zipfile
from pathlib import Path

base_fixture = Path(sys.argv[1])
work = Path(sys.argv[2])
seed_path = Path(sys.argv[3])

if work.exists():
    shutil.rmtree(work)
work.mkdir(parents=True)
with zipfile.ZipFile(base_fixture) as source:
    source.extractall(work)

(work / "Metadata/layer_config_ranges.xml").write_text(
    """<?xml version="1.0" encoding="utf-8"?>
<objects>
 <object id="3">
  <range min_z="0" max_z="6">
   <option opt_key="sparse_infill_density">75%</option>
   <option opt_key="wall_loops">6</option>
  </range>
 </object>
</objects>
""",
    encoding="utf-8",
)

if seed_path.exists():
    seed_path.unlink()
with zipfile.ZipFile(seed_path, "w", compression=zipfile.ZIP_DEFLATED) as target:
    for path in sorted(work.rglob("*")):
        if path.is_file():
            target.write(path, path.relative_to(work).as_posix())
PY

log "Artifacts: $RUN_DIR"
log "Orca binary: $ORCA_BIN"
set +e
python3 - "$LOG_PATH" "${ORCA_ENV[@]}" "$ORCA_BIN" \
  --outputdir "$RUN_DIR/output" \
  --export-3mf "$PACKAGE_NAME" \
  "$SEED_PATH" <<'PY'
import resource
import subprocess
import sys

log_path = sys.argv[1]
command = sys.argv[2:]
resource.setrlimit(resource.RLIMIT_CORE, (0, 0))
with open(log_path, "wb") as log:
    result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, check=False)
raise SystemExit(result.returncode if result.returncode >= 0 else 128 + abs(result.returncode))
PY
ORCA_STATUS="$?"
set -e
[[ "$ORCA_STATUS" -eq 0 ]] || fail "Orca exited with status $ORCA_STATUS; log: $LOG_PATH"
[[ -f "$PACKAGE_PATH" ]] || fail "Orca did not write expected package: $PACKAGE_PATH"

python3 "$ROOT_DIR/scripts/orca_3mf_project_preservation_audit.py" \
  --three-mf "$PACKAGE_PATH" \
  --min-plate-count 2 \
  --min-object-count 2 \
  --require-plate-names \
  --require-object-names \
  --require-filament-assignments \
  --require-object-settings \
  --require-modifier-volumes \
  --require-modifier-settings \
  --require-layer-ranges \
  --require-layer-range-settings \
  --require-project-thumbnails \
  --require-project-settings \
  --pretty > "$RUN_DIR/audit.json"

rm -rf "$REFERENCE_DIR"
mkdir -p "$REFERENCE_DIR"
cp "$PACKAGE_PATH" "$REFERENCE_DIR/$PACKAGE_NAME"
cp "$SEED_PATH" "$REFERENCE_DIR/combined-seed.3mf"
cp "$RUN_DIR/audit.json" "$REFERENCE_DIR/audit.json"
cat > "$REFERENCE_DIR/README.md" <<EOF
# Combined Orca Project Preservation Fixture

Generated by \`scripts/generate_orca_combined_project_fixture.sh\`.

- Orca binary: \`$ORCA_BIN\`
- Source artifact directory: \`$RUN_DIR\`
- Seed package: \`combined-seed.3mf\`
- Canonical Orca package: \`$PACKAGE_NAME\`

This fixture combines the project features that are separately covered by the
rich-object, modifier, and height-range fixtures:

- two plates,
- named objects,
- two object-to-filament assignments,
- object-scoped process metadata,
- one parameter modifier volume,
- modifier-scoped process metadata,
- one height/layer range with range-scoped process metadata,
- project thumbnails,
- project, model, and slice config entries.

This is intentionally a project-only fixture. The local desktop Orca CLI accepts
this combined project and preserves the combined schema on project export, but
the same height-range path is still not safe to claim as sliced .gcode.3mf
parity because the local desktop Orca CLI crashes before diagnostics when
assemble-list height_ranges are combined with sliced export.
EOF

log "Generated combined Orca project fixture: $REFERENCE_DIR"
