#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android-app"
APK_PATH="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.mobileslicer"
MAIN_ACTIVITY="com.mobileslicer/.MainActivity"
AUTOMATION_ACTION="com.mobileslicer.action.AUTOMATE_SLICE"
DEFAULT_SERIAL="RFCYA01ANVE"
BENCHY_AUTOMATION_CONFIG='{"bed_width_mm":270,"bed_depth_mm":270,"max_height_mm":256,"nozzle_diameter":0.4000000059604645,"filament_diameter":1.75,"filament_type":"PLA","filament_max_volumetric_speed":50,"nozzle_temperature_initial_layer":210,"nozzle_temperature":210,"bed_temperature_initial_layer":60,"bed_temperature":60,"cooling_baseline":100,"close_fan_the_first_x_layers":1,"layer_height":0.20000000298023224,"first_layer_height":0.20000000298023224,"first_layer_print_speed":10,"first_layer_infill_speed":22.5,"initial_layer_travel_speed_percent":50,"slow_down_layers":0,"outer_wall_speed":30,"inner_wall_speed":30,"top_surface_speed":30,"travel_speed":120,"outer_wall_acceleration":500,"inner_wall_acceleration":10000,"top_surface_acceleration":500,"sparse_infill_acceleration":500,"bridge_speed":10,"small_perimeter_speed":15,"small_perimeter_threshold":0,"sparse_infill_speed":300,"internal_solid_infill_speed":30,"gap_infill_speed":15,"top_shell_layers":4,"bottom_shell_layers":3,"seam_position":"aligned","precise_outer_wall":true,"only_one_wall_top":true,"top_surface_pattern":"monotonicline","sparse_infill_density":15,"sparse_infill_pattern":"grid","wall_loops":2,"print_speed_baseline":60,"skirts":2,"brim_width":0}'

usage() {
  cat <<'USAGE'
Usage:
  scripts/verify_android.sh unit
  scripts/verify_android.sh lint
  scripts/verify_android.sh stubs
  scripts/verify_android.sh apk
  scripts/verify_android.sh release
  scripts/verify_android.sh local
  scripts/verify_android.sh install [serial]
  scripts/verify_android.sh device [serial]
  scripts/verify_android.sh device-automation [serial]
  scripts/verify_android.sh profile-ui [serial]
  scripts/verify_android.sh benchy <local-stl-path> [serial]
  scripts/verify_android.sh all [serial]

Modes:
  unit    Run JVM debug unit tests.
  lint    Run Android lint for the debug variant.
  stubs   Validate the Android Orca native stub inventory.
  apk     Build the debug APK.
  release Run release compile/lint/R8 checks. Builds the signed release APK
          when release signing credentials are configured.
  local   Run stub inventory validation, lint, unit tests, and build the debug APK.
  install Build and install the debug APK on a connected device.
  device  Alias for install; no UI automation, log pulls, or runtime probing.
  device-automation
          Build, install, cold-launch, and assert the process stays alive with
          an empty crash buffer. Requires MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1.
  profile-ui
          Build, install, cold-launch, and assert the process stays alive with
          an empty crash buffer. Requires MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1.
  benchy  Build, install, stage an STL app-private, and run automation slicing.
          Requires MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1.
  all     Run local checks and install the debug APK.

Device serial defaults to $ANDROID_SERIAL, then RFCYA01ANVE.
USAGE
}

log() {
  printf '[verify_android] %s\n' "$*"
}

fail() {
  printf '[verify_android] ERROR: %s\n' "$*" >&2
  exit 1
}

adb_bin() {
  if [[ -x "$ROOT_DIR/tools/adb" ]]; then
    printf '%s\n' "$ROOT_DIR/tools/adb"
  elif command -v adb >/dev/null 2>&1; then
    command -v adb
  else
    fail "adb not found. Expected $ROOT_DIR/tools/adb or adb on PATH."
  fi
}

device_serial() {
  local requested="${1:-}"
  if [[ -n "$requested" ]]; then
    printf '%s\n' "$requested"
  elif [[ -n "${ANDROID_SERIAL:-}" ]]; then
    printf '%s\n' "$ANDROID_SERIAL"
  else
    printf '%s\n' "$DEFAULT_SERIAL"
  fi
}

gradle() {
  (cd "$ANDROID_DIR" && ./gradlew "$@")
}

run_unit() {
  log "Running JVM debug unit tests"
  gradle testDebugUnitTest
}

run_lint() {
  log "Running Android debug lint"
  gradle lintDebug
}

run_stub_inventory() {
  log "Validating Android Orca stub inventory"
  "$ROOT_DIR/scripts/verify_stub_inventory.py"
}

build_apk() {
  log "Building debug APK"
  gradle assembleDebug
  [[ -f "$APK_PATH" ]] || fail "APK missing after build: $APK_PATH"
  log "APK ready: $APK_PATH"
}

build_release_apk() {
  local signing_configured=0
  if [[ -n "${MOBILE_SLICER_RELEASE_STORE_FILE:-}" &&
        -n "${MOBILE_SLICER_RELEASE_STORE_PASSWORD:-}" &&
        -n "${MOBILE_SLICER_RELEASE_KEY_ALIAS:-}" &&
        -n "${MOBILE_SLICER_RELEASE_KEY_PASSWORD:-}" ]]; then
    signing_configured=1
  elif [[ -f "$ANDROID_DIR/release-signing.properties" ]]; then
    signing_configured=1
  fi

  if [[ "$signing_configured" == "1" ]]; then
    log "Building signed release APK"
    gradle :app:assembleRelease
    local release_apk="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"
    [[ -f "$release_apk" ]] || fail "Release APK missing after build: $release_apk"
    log "Release APK ready: $release_apk"
  else
    log "Release signing not configured; running release compile, lint, and R8 checks without packaging."
    gradle :app:compileReleaseKotlin :app:lintVitalRelease :app:minifyReleaseWithR8
  fi
}

adb_device() {
  local serial="$1"
  shift
  local adb
  adb="$(adb_bin)"
  "$adb" -s "$serial" "$@"
}

require_device_automation() {
  case "${MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION:-}" in
    1|true|TRUE|yes|YES)
      ;;
    *)
      fail "Device automation requires MOBILE_SLICER_ALLOW_DEVICE_AUTOMATION=1. Use 'install'/'device' for delivery-only device verification."
      ;;
  esac
}

require_device() {
  local serial="$1"
  log "Checking device $serial"
  adb_device "$serial" get-state >/dev/null
}

install_apk() {
  local serial="$1"
  build_apk
  require_device "$serial"
  log "Installing debug APK on $serial"
  adb_device "$serial" install -r "$APK_PATH"
}

launch_app() {
  local serial="$1"
  log "Cold-launching $PACKAGE_NAME on $serial"
  adb_device "$serial" shell am force-stop "$PACKAGE_NAME"
  adb_device "$serial" logcat -c
  adb_device "$serial" shell am start -W -n "$MAIN_ACTIVITY"
  log "Recent app logs"
  adb_device "$serial" logcat -d -v brief | grep -E 'MobileSlicer|AndroidRuntime|FATAL EXCEPTION' || true
}

assert_no_crash_after_launch() {
  local serial="$1"
  sleep 4
  local crash_log
  crash_log="$(adb_device "$serial" logcat -b crash -d -t 200)"
  if [[ -n "$crash_log" ]]; then
    printf '%s\n' "$crash_log" >&2
    fail "Crash log buffer is not empty after launch."
  fi
  local pid
  pid="$(adb_device "$serial" shell pidof "$PACKAGE_NAME" || true)"
  [[ -n "$pid" ]] || fail "$PACKAGE_NAME is not running after launch."
  log "$PACKAGE_NAME running with pid $pid and clean crash buffer"
}

dump_ui_xml() {
  local serial="$1"
  adb_device "$serial" shell uiautomator dump /sdcard/mobileslicer-window.xml >/dev/null
  adb_device "$serial" shell cat /sdcard/mobileslicer-window.xml
}

ui_text_bounds() {
  local serial="$1"
  local text="$2"
  local xml
  xml="$(dump_ui_xml "$serial")"
  XML_PAYLOAD="$xml" python3 - "$text" <<'PY'
import os, re, sys, xml.etree.ElementTree as ET
target = sys.argv[1]
root = ET.fromstring(os.environ["XML_PAYLOAD"])
for node in root.iter("node"):
    if node.attrib.get("text") == target or node.attrib.get("content-desc") == target:
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
            sys.exit(0)
sys.exit(1)
PY
}

tap_text() {
  local serial="$1"
  local text="$2"
  local coords
  coords="$(ui_text_bounds "$serial" "$text")" || fail "Unable to find UI text: $text"
  log "Tapping '$text'"
  adb_device "$serial" shell input tap $coords
  sleep 1
}

assert_text_visible() {
  local serial="$1"
  local text="$2"
  ui_text_bounds "$serial" "$text" >/dev/null || fail "Expected UI text is not visible: $text"
  log "Verified UI text: $text"
}

ensure_home_visible() {
  local serial="$1"
  for _ in 1 2 3 4; do
    if ui_text_bounds "$serial" "Mobile Slicer" >/dev/null && ui_text_bounds "$serial" "Open Profiles" >/dev/null; then
      return 0
    fi
    adb_device "$serial" shell input keyevent KEYCODE_BACK
    sleep 1
  done
  ui_text_bounds "$serial" "Mobile Slicer" >/dev/null && ui_text_bounds "$serial" "Open Profiles" >/dev/null || fail "Unable to return to app home with Open Profiles visible."
}

run_install_only() {
  local serial="$1"
  install_apk "$serial"
}

run_device_automation_smoke() {
  local serial="$1"
  require_device_automation
  install_apk "$serial"
  launch_app "$serial"
  assert_no_crash_after_launch "$serial"
}

run_profile_ui_smoke() {
  local serial="$1"
  require_device_automation
  install_apk "$serial"
  launch_app "$serial"
  ensure_home_visible "$serial"
  tap_text "$serial" "Open Profiles"
  assert_text_visible "$serial" "Profiles"
  tap_text "$serial" "Process"
  assert_text_visible "$serial" "Process Profiles"
  tap_text "$serial" "Edit / Rename"
  assert_text_visible "$serial" "Quality"
  assert_text_visible "$serial" "Layer height"
  tap_text "$serial" "Strength"
  assert_text_visible "$serial" "Top/bottom shells"
  adb_device "$serial" shell input keyevent KEYCODE_BACK
  sleep 1
  assert_no_crash_after_launch "$serial"
}

stage_app_private_file() {
  local serial="$1"
  local local_path="$2"
  local basename
  basename="$(basename "$local_path")"
  local tmp_path="/data/local/tmp/mobileslicer-$basename"
  local app_path="/data/data/$PACKAGE_NAME/files/automation/$basename"

  [[ -f "$local_path" ]] || fail "STL not found: $local_path"
  log "Staging $local_path to app-private storage on $serial"
  adb_device "$serial" push "$local_path" "$tmp_path" >/dev/null
  adb_device "$serial" shell run-as "$PACKAGE_NAME" mkdir -p files/automation
  adb_device "$serial" shell run-as "$PACKAGE_NAME" cp "$tmp_path" "files/automation/$basename"
  adb_device "$serial" shell rm -f "$tmp_path"
  printf '%s\n' "$app_path"
}

wait_for_status() {
  local serial="$1"
  local status_path="$2"
  local attempts="${3:-120}"
  local delay_seconds="${4:-1}"

  for _ in $(seq 1 "$attempts"); do
    if adb_device "$serial" shell run-as "$PACKAGE_NAME" test -f "$status_path"; then
      adb_device "$serial" shell run-as "$PACKAGE_NAME" cat "$status_path"
      return 0
    fi
    sleep "$delay_seconds"
  done
  fail "Timed out waiting for automation status: $status_path"
}

run_benchy() {
  local local_stl="$1"
  local serial="$2"
  require_device_automation
  install_apk "$serial"

  local app_model_path
  app_model_path="$(stage_app_private_file "$serial" "$local_stl" | tail -n 1)"
  local stamp
  stamp="$(date +%Y%m%d-%H%M%S)"
  local output_path="/data/data/$PACKAGE_NAME/files/automation/benchy-$stamp.gcode"
  local status_path="$output_path.status.txt"

  log "Running automation slice on $serial"
  adb_device "$serial" shell run-as "$PACKAGE_NAME" rm -f "$output_path" "$status_path"
  adb_device "$serial" shell am force-stop "$PACKAGE_NAME"
  adb_device "$serial" logcat -c
  adb_device "$serial" shell "CONFIG='$BENCHY_AUTOMATION_CONFIG'; am start -W \
    -a '$AUTOMATION_ACTION' \
    -n '$MAIN_ACTIVITY' \
    --es automation_model_path '$app_model_path' \
    --es automation_output_path '$output_path' \
    --es automation_status_path '$status_path' \
    --es automation_config_json \"\$CONFIG\""

  log "Automation status"
  wait_for_status "$serial" "$status_path"
  log "Output file"
  adb_device "$serial" shell run-as "$PACKAGE_NAME" ls -lh "$output_path"
}

mode="${1:-}"
case "$mode" in
  unit)
    run_unit
    ;;
  lint)
    run_lint
    ;;
  stubs)
    run_stub_inventory
    ;;
  apk)
    build_apk
    ;;
  release)
    build_release_apk
    ;;
  local)
    run_stub_inventory
    run_lint
    run_unit
    build_apk
    ;;
  install)
    run_install_only "$(device_serial "${2:-}")"
    ;;
  device)
    run_install_only "$(device_serial "${2:-}")"
    ;;
  device-automation)
    run_device_automation_smoke "$(device_serial "${2:-}")"
    ;;
  profile-ui)
    run_profile_ui_smoke "$(device_serial "${2:-}")"
    ;;
  benchy)
    [[ $# -ge 2 ]] || {
      usage
      fail "benchy mode requires a local STL path."
    }
    run_benchy "$2" "$(device_serial "${3:-}")"
    ;;
  all)
    run_stub_inventory
    run_lint
    run_unit
    build_release_apk
    install_apk "$(device_serial "${2:-}")"
    ;;
  -h|--help|help|"")
    usage
    ;;
  *)
    usage
    fail "unknown mode: $mode"
    ;;
esac
