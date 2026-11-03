"""Regression checks for complete H-39 TaCZ gunpack references and recipes."""
import json
import unittest
from pathlib import Path

PACK = Path(__file__).resolve().parents[1] / "gunpack"
APPROVED = {
    "tacz:sight_srs_02", "tacz:scope_acog_ta31",
    "tacz:scope_qmk152", "tacz:scope_1873_6x",
    "tacz:laser_compact", "tacz:grip_magpul_afg_2",
    "tacz:muzzle_silencer_phantom_s1",
}
def load(path):
    return json.loads((PACK / path).read_text(encoding="utf-8"))

class H39PackIntegrityTests(unittest.TestCase):
    def test_main_index_references_resolve(self):
        gun = load("data/h39/index/guns/h39.json")
        namespace, value = gun["data"].split(":", 1)
        self.assertEqual(namespace, "h39")
        self.assertTrue((PACK / f"data/{namespace}/data/guns/{value}.json").is_file())
        ns, display_id = gun["display"].split(":", 1)
        self.assertTrue((PACK / f"assets/{ns}/display/guns/{display_id}.json").is_file())
        data = load(f"data/{namespace}/data/guns/{value}.json")
        ammo_ns, ammo_id = data["ammo"].split(":", 1)
        ammo = load(f"data/{ammo_ns}/index/ammo/{ammo_id}.json")
        display_ns, display_id = ammo["display"].split(":", 1)
        self.assertTrue((PACK / f"assets/{display_ns}/display/ammo/{display_id}.json").is_file())

    def test_gun_smith_recipes(self):
        gun_recipe = load("data/h39/recipe/gun/h39.json")
        tube_recipe = load("data/h39/recipe/ammo/medical_tube.json")
        self.assertEqual(gun_recipe["type"], "tacz:gun_smith_table_crafting")
        self.assertEqual(tube_recipe["type"], gun_recipe["type"])
        self.assertEqual(gun_recipe["result"], {"type":"gun","id":"h39:h39"})
        self.assertEqual(tube_recipe["result"]["id"], "h39:medical_tube")
        for entry in gun_recipe["materials"] + tube_recipe["materials"]:
            item = entry["item"]
            self.assertEqual(len(item), 1)
            self.assertTrue(item.get("tag") or item.get("item"))
            self.assertGreater(entry.get("count",1), 0)
            self.assertLessEqual(entry.get("count",1), 64)

    def test_exact_attachment_whitelist(self):
        gun = load("data/h39/data/guns/h39_data.json")
        approved = load("data/h39/tacz_tags/attachments/allow_attachments/h39.json")
        self.assertIsInstance(approved, list)
        self.assertEqual(set(approved), APPROVED)
        self.assertEqual(len(approved), len(APPROVED))
        self.assertFalse(any(a.startswith("#") for a in approved))
        self.assertFalse(any("extended_mag" in a or "ammo_mod" in a for a in approved))
        # Keep TaCZ's type filter as an additional guard.
        self.assertEqual(set(gun["allow_attachment_types"]),
                         {"scope", "laser", "grip", "muzzle"})
        self.assertNotIn("builtin_attachments", gun)

    def test_localized_metadata(self):
        zh = load("assets/h39/lang/zh_tw.json")
        en = load("assets/h39/lang/en_us.json")
        gun = load("data/h39/index/guns/h39.json")
        tube = load("data/h39/index/ammo/medical_tube.json")
        info = load("assets/h39/gunpack_info.json")
        for key in (gun["name"],gun["tooltip"],tube["name"],info["name"],info["desc"]):
            self.assertTrue(zh.get(key), "Missing zh_TW key "+key)
            self.assertTrue(en.get(key), "Missing en_US key "+key)

if __name__ == "__main__":
    unittest.main()
