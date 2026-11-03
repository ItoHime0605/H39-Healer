"""Verify H-39 covers all animation names used by TaCZ 1.1.8 Hotfix R7.

List extracted from the user-provided TaCZ JAR's
assets/tacz/scripts/default_state_machine.lua. No third-party source/JAR
is copied into this public repository.
"""
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "gunpack"

# Static calls to context:runAnimation() inside TaCZ default_state_machine.lua
TACZ_DEFAULT = {
    "aim", "aim_end", "aim_start", "bolt", "draw", "idle",
    "inspect", "inspect_empty", "melee_push", "melee_stock",
    "over_heat", "put_away", "reload_empty", "reload_tactical",
    "run", "run_end", "run_hold", "run_start", "shoot",
    "slide", "slide_back", "slide_idle", "static_bolt_caught",
    "static_idle", "static_over_heating", "walk_aiming",
    "walk_backward", "walk_forward", "walk_sideway",
}

# These are created through dynamic animation-name construction.
TACZ_DYNAMIC = {
    "melee_bayonet_1", "melee_bayonet_2", "melee_bayonet_3",
    "shoot_last",
}

class AnimationCompatibilityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        geo = json.loads((ROOT / "assets/h39/geo_models/gun/h39_geo.json").read_text(encoding="utf-8"))
        cls.geometry_bones = {bone["name"] for bone in geo["minecraft:geometry"][0]["bones"]}
        contents = json.loads((ROOT / "assets/h39/animations/h39.animation.json").read_text(encoding="utf-8"))
        cls.animations = contents["animations"]

    def test_required_tacz_defaults(self):
        missing = (TACZ_DEFAULT | TACZ_DYNAMIC) - set(self.animations)
        self.assertFalse(missing, "TaCZ may attempt to play missing animations: " + repr(sorted(missing)))

    def test_all_referenced_geometry_bones_exist(self):
        for name, data in self.animations.items():
            with self.subTest(name=name):
                self.assertFalse(set(data.get("bones", {})) - self.geometry_bones)

    def test_every_tube_visibility_stage(self):
        for count in range(5):
            with self.subTest(count=count):
                state = self.animations["tubes_" + str(count)]["bones"]
                for index in range(1, 5):
                    actual = state["medical_tube_" + str(index)]["scale"]
                    self.assertEqual(actual, [1, 1, 1] if index > 4 - count else [0, 0, 0])

    def test_no_recoil_physical_gun_kick(self):
        shoot = self.animations["shoot"]["bones"]["root"]
        self.assertTrue(all(
            value == [0, 0, 0] for value in shoot.get("rotation", {}).values()
        ))

if __name__ == "__main__":
    unittest.main()
