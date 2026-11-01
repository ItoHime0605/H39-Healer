"""Validate H-39 prototype cross-references; no Minecraft runtime assumed."""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "gunpack"

class GunpackMetadataTests(unittest.TestCase):
    def load(self, path):
        return json.loads((PACK / path).read_text(encoding="utf-8"))

    def test_gun_index_namespace(self):
        gun = self.load("data/h39/index/guns/h39.json")
        self.assertEqual(gun["data"], "h39:h39_data")
        self.assertEqual(gun["display"], "h39:h39_display")

    def test_localized_keys(self):
        lang = self.load("assets/h39/lang/zh_tw.json")
        gun = self.load("data/h39/index/guns/h39.json")
        ammo = self.load("data/h39/index/ammo/medical_tube.json")
        for key in (gun["name"], gun["tooltip"], ammo["name"]):
            self.assertIn(key, lang)

    def test_not_claimed_playable(self):
        readme = (PACK / "README.md").read_text(encoding="utf-8")
        self.assertIn("不可視為可安裝槍包", readme)

if __name__ == "__main__":
    unittest.main()
