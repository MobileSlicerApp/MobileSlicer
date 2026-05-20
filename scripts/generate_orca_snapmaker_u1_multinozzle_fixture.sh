#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_ORCA_BIN="$ROOT_DIR/vendor/orcaslicer/build/package/bin/orca-slicer"
ORCA_BIN="${ORCA_SLICER_BIN:-$DEFAULT_ORCA_BIN}"
ARTIFACT_ROOT="${MOBILE_SLICER_ORCA_SNAPMAKER_U1_ARTIFACT_ROOT:-$ROOT_DIR/artifacts/orca-project-fixtures}"
REFERENCE_DIR="${MOBILE_SLICER_ORCA_SNAPMAKER_U1_REFERENCE_DIR:-$ROOT_DIR/regression-fixtures/orca-project-references/snapmaker-u1-multinozzle}"
STAMP="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="$ARTIFACT_ROOT/snapmaker-u1-multinozzle-$STAMP"
ORCA_LIB_SHIM="$ROOT_DIR/tools/orcaslicer/lib-shim/usr/lib64:$ROOT_DIR/tools/orcaslicer/lib-shim"

fail() {
  printf '[generate_orca_snapmaker_u1_multinozzle_fixture] ERROR: %s\n' "$*" >&2
  exit 1
}

log() {
  printf '[generate_orca_snapmaker_u1_multinozzle_fixture] %s\n' "$*"
}

[[ -x "$ORCA_BIN" ]] || fail "Orca binary is not executable: $ORCA_BIN"
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

python3 - "$ROOT_DIR" "$RUN_DIR/work" <<'PY'
import json
import math
import struct
import sys
from pathlib import Path

root = Path(sys.argv[1])
work = Path(sys.argv[2])
work.mkdir(parents=True, exist_ok=True)
profile_root = root / "vendor/orcaslicer/resources/profiles"
profile_paths = sorted(profile_root.glob("**/*.json"))


def write_cube(path: Path, name: str, size: float = 12.0) -> None:
    h = size / 2.0
    vertices = [
        (-h, -h, 0), (h, -h, 0), (h, h, 0), (-h, h, 0),
        (-h, -h, size), (h, -h, size), (h, h, size), (-h, h, size),
    ]
    faces = [
        (0, 1, 2), (0, 2, 3), (4, 6, 5), (4, 7, 6),
        (0, 4, 5), (0, 5, 1), (1, 5, 6), (1, 6, 2),
        (2, 6, 7), (2, 7, 3), (3, 7, 4), (3, 4, 0),
    ]
    data = bytearray(name.encode("ascii", "ignore")[:80].ljust(80, b"\0"))
    data.extend(struct.pack("<I", len(faces)))
    for a, b, c in faces:
        ax, ay, az = vertices[a]
        bx, by, bz = vertices[b]
        cx, cy, cz = vertices[c]
        ux, uy, uz = bx - ax, by - ay, bz - az
        vx, vy, vz = cx - ax, cy - ay, cz - az
        nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
        length = math.sqrt(nx * nx + ny * ny + nz * nz) or 1.0
        data.extend(struct.pack("<12fH", nx / length, ny / length, nz / length, ax, ay, az, bx, by, bz, cx, cy, cz, 0))
    path.write_bytes(data)


def find_inherited_profile(parent_name: str, child_path: Path) -> Path:
    normalized = parent_name if parent_name.endswith(".json") else f"{parent_name}.json"
    matches = [path for path in profile_paths if path.name == normalized]
    if not matches:
        raise FileNotFoundError(f"Could not resolve inherited Orca profile {parent_name!r} for {child_path}")
    child_parts = child_path.parts
    scored = []
    for match in matches:
        common = 0
        for left, right in zip(child_parts, match.parts):
            if left != right:
                break
            common += 1
        scored.append((common, -len(match.parts), match))
    scored.sort(reverse=True)
    return scored[0][2]


def load_profile(path: str, seen: set[Path] | None = None) -> dict:
    profile_path = root / path
    seen = set() if seen is None else seen
    if profile_path in seen:
        raise RuntimeError(f"Cycle while resolving Orca profile inheritance at {profile_path}")
    seen.add(profile_path)
    profile = json.loads(profile_path.read_text(encoding="utf-8"))
    inherits = profile.get("inherits")
    if not inherits:
        return profile
    merged: dict = {}
    for parent_name in (inherits if isinstance(inherits, list) else [inherits]):
        parent_path = find_inherited_profile(str(parent_name), profile_path)
        merged.update(load_profile(parent_path.relative_to(root).as_posix(), seen))
    merged.update(profile)
    return merged


write_cube(work / "u1_red_cube.stl", "u1_red_cube", 12)
write_cube(work / "u1_blue_cube.stl", "u1_blue_cube", 12)

machine = load_profile("vendor/orcaslicer/resources/profiles/Snapmaker/machine/Snapmaker U1 (0.4 nozzle).json")
process = load_profile("vendor/orcaslicer/resources/profiles/Snapmaker/process/0.20 Standard @Snapmaker U1 (0.4 nozzle).json")
filament_paths = [
    "vendor/orcaslicer/resources/profiles/Snapmaker/filament/Polymaker/PolyLite PLA @U1.json",
    "vendor/orcaslicer/resources/profiles/Snapmaker/filament/Polymaker/Panchroma PLA @U1.json",
]

machine["thumbnails"] = "128x128/PNG"
machine["thumbnails_format"] = "PNG"
machine["physical_extruder_map"] = machine.get("physical_extruder_map") or ["0", "1", "2", "3"]
machine["nozzle_volume"] = machine.get("nozzle_volume") or ["0", "0", "0", "0"]
machine["flush_multiplier"] = machine.get("flush_multiplier") or ["1", "1", "1", "1"]
(work / "machine.json").write_text(json.dumps(machine, indent=2, sort_keys=True) + "\n", encoding="utf-8")

process["filament_map_mode"] = "Manual"
process["filament_map"] = ["1", "2"]
(work / "process.json").write_text(json.dumps(process, indent=2, sort_keys=True) + "\n", encoding="utf-8")

for index, (path, color) in enumerate(zip(filament_paths, ("#F2754E", "#4EA3F2")), start=1):
    filament = load_profile(path)
    filament["filament_colour"] = [color]
    filament["filament_settings_id"] = [f"MobileSlicer Snapmaker U1 reference PLA {index}"]
    filament["filament_type"] = ["PLA"]
    filament["filament_diameter"] = ["1.75"]
    filament["filament_is_support"] = ["0"]
    filament["nozzle_temperature"] = ["220"]
    filament["nozzle_temperature_initial_layer"] = ["220"]
    filament["bed_temperature"] = ["60"]
    filament["bed_temperature_initial_layer"] = ["60"]
    (work / f"filament{index}.json").write_text(json.dumps(filament, indent=2, sort_keys=True) + "\n", encoding="utf-8")

assemble = {
    "plates": [{
        "plate_name": "Snapmaker U1 multi-nozzle",
        "need_arrange": False,
        "objects": [
            {
                "path": str((work / "u1_red_cube.stl").resolve()),
                "count": 1,
                "filaments": [1],
                "pos_x": [120],
                "pos_y": [135],
                "pos_z": [0],
                "assemble_index": [0],
            },
            {
                "path": str((work / "u1_blue_cube.stl").resolve()),
                "count": 1,
                "filaments": [2],
                "pos_x": [155],
                "pos_y": [135],
                "pos_z": [0],
                "assemble_index": [0],
            },
        ],
    }]
}
(work / "assemble-list.json").write_text(json.dumps(assemble, indent=2) + "\n", encoding="utf-8")
PY

PACKAGE_NAME="snapmaker-u1-multinozzle.gcode.3mf"
PACKAGE_PATH="$RUN_DIR/output/$PACKAGE_NAME"
LOG_PATH="$RUN_DIR/orca.log"

log "Artifacts: $RUN_DIR"
set +e
python3 - "$LOG_PATH" "${ORCA_ENV[@]}" "$ORCA_BIN" \
  --slice 0 \
  --outputdir "$RUN_DIR/output" \
  --export-3mf "$PACKAGE_NAME" \
  --load-settings "$RUN_DIR/work/machine.json;$RUN_DIR/work/process.json" \
  --load-filaments "$RUN_DIR/work/filament1.json;$RUN_DIR/work/filament2.json" \
  --allow-multicolor-oneplate \
  --load-assemble-list "$RUN_DIR/work/assemble-list.json" <<'PY'
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
  --min-plate-count 1 \
  --min-object-count 2 \
  --require-plate-names \
  --require-object-names \
  --require-filament-assignments \
  --require-project-thumbnails \
  --require-project-settings \
  --pretty > "$RUN_DIR/audit.json"

python3 "$ROOT_DIR/scripts/compare_orca_snapmaker_u1_multinozzle_gcode.py" \
  --reference "$PACKAGE_PATH" \
  --mobile-gcode "$PACKAGE_PATH" \
  --min-tools 2 > "$RUN_DIR/self-compare.json"

rm -rf "$REFERENCE_DIR"
mkdir -p "$REFERENCE_DIR"
cp "$PACKAGE_PATH" "$REFERENCE_DIR/$PACKAGE_NAME"
sed "s#$ROOT_DIR/##g" "$RUN_DIR/work/assemble-list.json" > "$REFERENCE_DIR/assemble-list.json"
cp "$RUN_DIR/audit.json" "$REFERENCE_DIR/audit.json"
cp "$RUN_DIR/self-compare.json" "$REFERENCE_DIR/self-compare.json"
cat > "$REFERENCE_DIR/README.md" <<EOF
# Snapmaker U1 Multi-Nozzle Orca Fixture

Generated by \`scripts/generate_orca_snapmaker_u1_multinozzle_fixture.sh\`.

- Orca binary: \`${ORCA_BIN#$ROOT_DIR/}\`
- Source artifact directory: \`${RUN_DIR#$ROOT_DIR/}\`
- Package: \`$PACKAGE_NAME\`

This fixture is a desktop Orca reference for Snapmaker U1 multi-nozzle
multi-filament slicing. It contains two objects assigned to two logical
filaments and a manual \`filament_map = 1,2\`, producing multi-tool G-code.

Use \`scripts/compare_orca_snapmaker_u1_multinozzle_gcode.py\` to compare a
MobileSlicer G-code file against this reference.
EOF

log "Generated Snapmaker U1 reference fixture: $REFERENCE_DIR"
