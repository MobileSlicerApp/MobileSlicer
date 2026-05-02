#!/usr/bin/env python3
"""Compare an Orca desktop G-code file with a MobileSlicer G-code file.

The goal is parity triage, not byte-for-byte diffing. The script reports
structural differences that usually identify the underlying mismatch:
configuration key drift, command vocabulary, feature counts, filament totals,
layer markers, and emitted XY bounds.
"""

from __future__ import annotations

import argparse
import collections
import json
import math
import re
from pathlib import Path


MOVE_RE = re.compile(r"^(?P<cmd>[GMT]\d+)\b")
WORD_RE = re.compile(r"([XYZE])([-+]?(?:\d+(?:\.\d*)?|\.\d+))")
FEATURE_RE = re.compile(r"^;\s*(?:TYPE|FEATURE):\s*(.+?)\s*$")
FILAMENT_RE = re.compile(r"^;\s*filament used \[(?P<unit>[^\]]+)\]\s*=\s*(?P<value>[-+]?\d+(?:\.\d+)?)")


def read_lines(path: Path) -> list[str]:
    return path.read_text(errors="replace").splitlines()


def config_block(lines: list[str]) -> dict[str, list[str]]:
    result: dict[str, list[str]] = collections.defaultdict(list)
    in_block = False
    for line in lines:
        if line == "; CONFIG_BLOCK_START":
            in_block = True
            continue
        if line == "; CONFIG_BLOCK_END":
            break
        if not in_block or not line.startswith(";"):
            continue
        body = line[1:].strip()
        if " = " not in body:
            continue
        key, value = body.split(" = ", 1)
        result[key].append(value)
    return dict(result)


def command_counts(lines: list[str]) -> collections.Counter[str]:
    counts: collections.Counter[str] = collections.Counter()
    for line in lines:
        match = MOVE_RE.match(line)
        if match:
            counts[match.group("cmd")] += 1
    return counts


def feature_counts(lines: list[str]) -> collections.Counter[str]:
    counts: collections.Counter[str] = collections.Counter()
    for line in lines:
        match = FEATURE_RE.match(line)
        if match:
            counts[match.group(1)] += 1
    return counts


def layer_count(lines: list[str]) -> int:
    markers = 0
    for line in lines:
        if line.startswith(";LAYER_CHANGE") or line.startswith("; CHANGE_LAYER"):
            markers += 1
    return markers


def filament_totals(lines: list[str]) -> dict[str, float]:
    totals: dict[str, float] = {}
    for line in lines:
        match = FILAMENT_RE.match(line)
        if match:
            totals[match.group("unit")] = float(match.group("value"))
    return totals


def xy_bounds(lines: list[str]) -> dict[str, float | None]:
    min_x = math.inf
    max_x = -math.inf
    min_y = math.inf
    max_y = -math.inf
    found = False
    for line in lines:
        if not line.startswith(("G0 ", "G1 ")):
            continue
        words = dict(WORD_RE.findall(line))
        if "X" not in words or "Y" not in words:
            continue
        x = float(words["X"])
        y = float(words["Y"])
        min_x = min(min_x, x)
        max_x = max(max_x, x)
        min_y = min(min_y, y)
        max_y = max(max_y, y)
        found = True
    if not found:
        return {"min_x": None, "max_x": None, "min_y": None, "max_y": None}
    return {"min_x": min_x, "max_x": max_x, "min_y": min_y, "max_y": max_y}


def changed_config(left: dict[str, list[str]], right: dict[str, list[str]]) -> list[dict[str, object]]:
    rows: list[dict[str, object]] = []
    for key in sorted(set(left) | set(right)):
        left_values = left.get(key)
        right_values = right.get(key)
        if left_values != right_values:
            rows.append({"key": key, "orca": left_values, "mobile": right_values})
    return rows


def counter_delta(left: collections.Counter[str], right: collections.Counter[str]) -> dict[str, dict[str, int]]:
    delta: dict[str, dict[str, int]] = {}
    for key in sorted(set(left) | set(right)):
        if left[key] != right[key]:
            delta[key] = {"orca": left[key], "mobile": right[key]}
    return delta


def build_report(orca_path: Path, mobile_path: Path) -> dict[str, object]:
    orca_lines = read_lines(orca_path)
    mobile_lines = read_lines(mobile_path)
    return {
        "files": {
            "orca": str(orca_path),
            "mobile": str(mobile_path),
        },
        "line_counts": {
            "orca": len(orca_lines),
            "mobile": len(mobile_lines),
        },
        "layers": {
            "orca": layer_count(orca_lines),
            "mobile": layer_count(mobile_lines),
        },
        "filament_totals": {
            "orca": filament_totals(orca_lines),
            "mobile": filament_totals(mobile_lines),
        },
        "xy_bounds": {
            "orca": xy_bounds(orca_lines),
            "mobile": xy_bounds(mobile_lines),
        },
        "config_differences": changed_config(config_block(orca_lines), config_block(mobile_lines)),
        "command_count_differences": counter_delta(command_counts(orca_lines), command_counts(mobile_lines)),
        "feature_count_differences": counter_delta(feature_counts(orca_lines), feature_counts(mobile_lines)),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare Orca and MobileSlicer G-code parity signals.")
    parser.add_argument("orca_gcode", type=Path)
    parser.add_argument("mobile_gcode", type=Path)
    parser.add_argument("--json", action="store_true", help="Emit the full report as JSON.")
    args = parser.parse_args()

    report = build_report(args.orca_gcode, args.mobile_gcode)
    if args.json:
        print(json.dumps(report, indent=2, sort_keys=True))
        return 0

    print(f"Orca:   {report['files']['orca']}")
    print(f"Mobile: {report['files']['mobile']}")
    print(f"Lines:  Orca {report['line_counts']['orca']} / Mobile {report['line_counts']['mobile']}")
    print(f"Layers: Orca {report['layers']['orca']} / Mobile {report['layers']['mobile']}")
    print(f"Filament: Orca {report['filament_totals']['orca']} / Mobile {report['filament_totals']['mobile']}")
    print(f"XY bounds: Orca {report['xy_bounds']['orca']} / Mobile {report['xy_bounds']['mobile']}")
    print(f"Config differences: {len(report['config_differences'])}")
    for row in report["config_differences"][:40]:
        print(f"  {row['key']}: Orca={row['orca']} Mobile={row['mobile']}")
    print(f"Command count differences: {report['command_count_differences']}")
    print(f"Feature count differences: {report['feature_count_differences']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
