#!/usr/bin/env python3
import tempfile
import unittest
from pathlib import Path

from generate_orca_printer_assets import copy_profile_asset, safe_asset_name, write_thumbnail


class GenerateOrcaPrinterAssetsTest(unittest.TestCase):
    def test_safe_asset_name_normalizes_profile_names(self):
        self.assertEqual("bambu_lab_p1s_0_4_nozzle", safe_asset_name("Bambu Lab P1S 0.4 nozzle"))
        self.assertEqual("flashforge_guider_2s", safe_asset_name("Flashforge/Guider 2S"))

    def test_copy_profile_asset_creates_parent_directories(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source.bin"
            destination = root / "nested" / "assets" / "copied.bin"
            source.write_bytes(b"profile asset")

            copied_path = copy_profile_asset(source, destination)

            self.assertEqual(str(destination).replace("\\", "/"), copied_path)
            self.assertEqual(b"profile asset", destination.read_bytes())

    def test_write_thumbnail_creates_parent_directories_before_fallback_copy(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            source = root / "source.png"
            destination = root / "covers" / "thumbnail.png"
            source.write_bytes(b"not a real png but valid fallback bytes")

            write_thumbnail(source, destination)

            self.assertEqual(source.read_bytes(), destination.read_bytes())


if __name__ == "__main__":
    unittest.main()
