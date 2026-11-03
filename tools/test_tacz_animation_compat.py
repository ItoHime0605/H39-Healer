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

    def test_spent_tube_ejection_only_after_single_shot(self):
        script = (ROOT / "assets/h39/scripts/h39_state_machine.lua").read_text(encoding="utf-8")
        # Guard against a model jumping several rounds during login/sync.
        self.assertIn("rounds == state.last_rounds - 1", script)
        self.assertIn('state_per_context = setmetatable({}, {__mode = "k"})', script)
        self.assertIn("state_per_context[context] = nil", script)
        self.assertNotIn("rounds < state.last_rounds then", script)
        self.assertNotIn("local last_rounds = -1", script)

        def count(rounds):
            return (rounds + 4) // 5

        def eject(previous, current):
            return previous >= 0 and current == previous - 1 and count(current) < count(previous)

        fired = [(before, before - 1) for before in range(20, 0, -1)]
        self.assertEqual([before for before, after in fired if eject(before, after)], [16, 11, 6, 1])
        for before, after in [(-1, 0), (0, 20), (10, 20), (20, 10), (15, 0), (19, 16)]:
            self.assertFalse(eject(before, after), f"unexpected tube ejection {before}->{after}")

    def test_no_recoil_physical_gun_kick(self):
        shoot = self.animations["shoot"]["bones"]["root"]
        self.assertTrue(all(
            value == [0, 0, 0] for value in shoot.get("rotation", {}).values()
        ))

if __name__ == "__main__":
    unittest.main()
