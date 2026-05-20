#!/usr/bin/env python3
"""Compare MobileSlicer Snapmaker U1 multi-nozzle G-code with an Orca reference."""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path


TOOL_RE = re.compile(r"^T(\d+)\b", re.MULTILINE)
PRIME_TOWER_RE = re.compile(r"^;\s*TYPE:\s*Prime tower\s*$", re.IGNORECASE | re.MULTILINE)
THUMBNAIL_RE = re.compile(r"^;\s*thumbnail begin\s+(\d+)x(\d+)\s+(\d+)\s*$", re.IGNORECASE | re.MULTILINE)


def read_reference_gcode(path: Path) -> str:
    if path.suffix.lower() == ".gcode":
        return path.read_text(encoding="utf-8", errors="replace")
    with zipfile.ZipFile(path) as zf:
        candidates = sorted(
            name for name in zf.namelist()
            if re.fullmatch(r"Metadata/plate_\d+\.gcode", name)
        )
        if not candidates:
            raise ValueError(f"{path} does not contain Metadata/plate_*.gcode")
        return zf.read(candidates[0]).decode("utf-8", errors="replace")


def gcode_summary(text: str) -> dict[str, object]:
    tools = sorted({int(match.group(1)) for match in TOOL_RE.finditer(text)})
    scalar_fields: dict[str, str] = {}
    for line in text.splitlines():
        key, separator, value = line.partition("=")
        if separator != "=":
            continue
        key = key.removeprefix(";").strip()
        scalar_fields[key] = value.strip()
    thumbnails = [
        {"width": int(match.group(1)), "height": int(match.group(2)), "bytes": int(match.group(3))}
        for match in THUMBNAIL_RE.finditer(text)
    ]
    return {
        "tool_ids": tools,
        "tool_count": len(tools),
        "has_toolchange": len(tools) > 1,
        "prime_tower_feature_count": len(PRIME_TOWER_RE.findall(text)),
        "has_prime_tower_geometry": bool(PRIME_TOWER_RE.search(text)),
        "thumbnail_count": len(thumbnails),
        "thumbnails": thumbnails,
        "filament": scalar_fields.get("filament", ""),
        "filament_colour": scalar_fields.get("filament_colour", ""),
        "filament_colour_count": len([part for part in scalar_fields.get("filament_colour", "").split(";") if part.strip()]),
        "filament_map": scalar_fields.get("filament_map", ""),
        "nozzle_diameter": scalar_fields.get("nozzle_diameter", ""),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reference", required=True, type=Path, help="Desktop Orca .gcode.3mf or .gcode reference")
    parser.add_argument("--mobile-gcode", required=True, type=Path, help="MobileSlicer G-code to compare")
    parser.add_argument("--min-tools", type=int, default=2)
    parser.add_argument("--require-prime-tower", action="store_true")
    parser.add_argument("--require-thumbnails", action="store_true")
    parser.add_argument("--require-reference-filament-map", action="store_true")
    args = parser.parse_args()

    reference_text = read_reference_gcode(args.reference)
    mobile_text = read_reference_gcode(args.mobile_gcode)
    reference = gcode_summary(reference_text)
    mobile = gcode_summary(mobile_text)
    report = {"reference": reference, "mobile": mobile}
    print(json.dumps(report, indent=2, sort_keys=True))

    reference_tools = set(reference["tool_ids"])
    mobile_tools = set(mobile["tool_ids"])
    if len(reference_tools) < args.min_tools:
        print(f"reference does not prove multi-tool output: {sorted(reference_tools)}", file=sys.stderr)
        return 2
    if len(mobile_tools) < args.min_tools:
        print(f"mobile output is not multi-tool: {sorted(mobile_tools)}", file=sys.stderr)
        return 1
    if not reference_tools.issubset(mobile_tools):
        print(
            f"mobile output is missing reference tools: {sorted(reference_tools - mobile_tools)}",
            file=sys.stderr,
        )
        return 1
    if args.require_prime_tower and not mobile["has_prime_tower_geometry"]:
        print("mobile output is missing ;TYPE:Prime tower geometry", file=sys.stderr)
        return 1
    if args.require_thumbnails and int(mobile["thumbnail_count"]) <= 0:
        print("mobile output is missing embedded thumbnail blocks", file=sys.stderr)
        return 1
    if args.require_reference_filament_map:
        reference_map = str(reference["filament_map"]).strip()
        mobile_map = str(mobile["filament_map"]).strip()
        if not reference_map:
            print("reference output has no filament_map to compare", file=sys.stderr)
            return 2
        if mobile_map != reference_map:
            print(f"mobile filament_map {mobile_map!r} does not match reference {reference_map!r}", file=sys.stderr)
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
