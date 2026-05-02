#!/usr/bin/env python3
import json
import sys
from pathlib import Path


REQUIRED_FILE_FIELDS = {
    "path",
    "classification",
    "allowedProductScope",
    "blockedProductAreas",
    "symbols",
}

ALLOWED_CLASSIFICATIONS = {
    "compatibility_shims",
    "unsupported_feature_gates",
    "mixed_print_compatibility_layer",
}


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    inventory_path = repo_root / "engine-wrapper/orca-android-libslic3r/stub_inventory.json"
    inventory = json.loads(inventory_path.read_text(encoding="utf-8"))

    if inventory.get("schemaVersion") != 1:
        print("stub inventory schemaVersion must be 1", file=sys.stderr)
        return 1

    files = inventory.get("files")
    if not isinstance(files, list) or not files:
        print("stub inventory must contain at least one file entry", file=sys.stderr)
        return 1

    errors = []
    seen_paths = set()
    inventoried_paths = set()
    for index, entry in enumerate(files):
        missing = REQUIRED_FILE_FIELDS - set(entry)
        if missing:
            errors.append(f"entry {index} missing fields: {', '.join(sorted(missing))}")
            continue

        path = entry["path"]
        inventoried_paths.add(path)
        if path in seen_paths:
            errors.append(f"duplicate inventory path: {path}")
        seen_paths.add(path)

        source_path = repo_root / path
        if not source_path.is_file():
            errors.append(f"inventory path does not exist: {path}")
            continue

        text = source_path.read_text(encoding="utf-8", errors="replace")

        classification = entry["classification"]
        if classification not in ALLOWED_CLASSIFICATIONS:
            errors.append(f"{path} classification is not recognized: {classification}")

        allowed_scope = entry["allowedProductScope"]
        if not isinstance(allowed_scope, str) or not allowed_scope.strip():
            errors.append(f"{path} allowedProductScope must be a non-empty string")

        for field in ("blockedProductAreas", "symbols"):
            values = entry[field]
            if not isinstance(values, list) or not values:
                errors.append(f"{path} field {field} must be a non-empty list")
                continue
            if any(not isinstance(value, str) or not value.strip() for value in values):
                errors.append(f"{path} field {field} contains an empty/non-string value")

        for symbol in entry.get("symbols", []):
            token = symbol.rsplit("::", 1)[-1]
            if token not in text:
                errors.append(f"{path} does not contain listed symbol token: {symbol}")

    stub_dir = repo_root / "engine-wrapper/orca-android-libslic3r"
    source_stub_files = sorted(stub_dir.glob("*stubs.cpp"))
    source_stub_paths = {
        str(path.relative_to(repo_root))
        for path in source_stub_files
    }
    for path in sorted(source_stub_paths - inventoried_paths):
        errors.append(f"stub source is missing from inventory: {path}")

    cmake_path = stub_dir / "CMakeLists.txt"
    cmake_text = cmake_path.read_text(encoding="utf-8", errors="replace")
    linked_stub_paths = {
        str(path.relative_to(repo_root))
        for path in source_stub_files
        if path.name in cmake_text
    }
    for path in sorted(linked_stub_paths - inventoried_paths):
        errors.append(f"CMake-linked stub source is missing from inventory: {path}")
    for path in sorted(inventoried_paths - linked_stub_paths):
        errors.append(f"inventory path is not referenced by Android Orca CMake target: {path}")

    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1

    print(f"Verified {len(files)} Android Orca stub inventory entries.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
