"""Asset-generation regression tests; no Minecraft installation required."""
import json
import struct
import subprocess
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class GeneratedAssetsTests(unittest.TestCase):
    def test_embedded_gunpack_and_preview_zip(self):
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp) / "resources"
            exported = Path(temp) / "H39-Healer-Gunpack.zip"
            generated = subprocess.run([
                sys.executable,
                str(ROOT / "tools/generate_h39_gunpack.py"),
                "--output", str(out),
                "--zip", str(exported)
            ], check=False, capture_output=True, text=True)
            self.assertEqual(
                generated.returncode, 0,
                "H-39 generator failed:\\n" + generated.stdout + "\\n" + generated.stderr
            )

            pack = out / "assets/tacz/custom/h39_healer"
            self.assertEqual(
                json.loads((pack / "gunpack.meta.json").read_text())["namespace"],
                "h39"
            )
            self.assertTrue((pack / "assets/h39/gunpack_info.json").is_file())

            gun = json.loads((pack / "assets/h39/geo_models/gun/h39_geo.json").read_text())
            bones = {bone["name"] for bone in gun["minecraft:geometry"][0]["bones"]}
            for i in range(1, 5):
                self.assertIn("medical_tube_" + str(i), bones)
            self.assertIn("muzzle_pos", bones)
            self.assertIn("scope_pos", bones)
            self.assertIn("grip_pos", bones)
            self.assertIn("laser_pos", bones)
            self.assertIn("muzzle_pos", bones)
            self.assertNotIn("scope", bones, "No integral scope should block optional optics")
            # TaCZ's camera is driven by viewpoint bone pivots. Zero-origin
            # placeholders made this rifle fill the sky and appear above head.
            named_bones = {bone["name"]: bone for bone in
                           gun["minecraft:geometry"][0]["bones"]}
            for view in ("camera", "idle_view", "iron_view"):
                pivot = named_bones[view]["pivot"]
                self.assertGreater(pivot[1], 8, view + " must be above receiver")
                self.assertGreater(pivot[2], 10, view + " must be behind receiver")
            self.assertGreater(named_bones["idle_view"]["pivot"][0], 2)
            self.assertEqual(named_bones["iron_view"]["pivot"][0], 0)
            self.assertEqual(named_bones["idle_view"]["parent"], "views")
            self.assertEqual(named_bones["iron_view"]["parent"], "views")
            self.assertEqual(named_bones["righthand"]["parent"], "gun_and_righthand")
            self.assertEqual(named_bones["lefthand"]["parent"], "gun_and_righthand")
            self.assertEqual(named_bones["righthand_pos"]["parent"], "righthand")
            self.assertEqual(named_bones["lefthand_pos"]["parent"], "lefthand")
            # Concept silhouette: narrower than previous 8-unit thick body,
            # with a long needle barrel and twin tapered nose fins.
            receiver = named_bones["main_body"]["cubes"]
            self.assertLessEqual(max(q["origin"][0]+q["size"][0] for q in receiver)-
                                 min(q["origin"][0] for q in receiver), 4.5)
            self.assertLess(named_bones["muzzle_pos"]["pivot"][2], -40)
            self.assertGreater(len(named_bones["upper_wing_left"]["cubes"]), 3)
            self.assertGreater(len(named_bones["upper_wing_right"]["cubes"]), 3)

            gun_data = json.loads((pack / "data/h39/data/guns/h39_data.json").read_text())
            self.assertEqual(gun_data["allow_attachment_types"],
                             ["scope", "laser", "grip", "muzzle"])
            self.assertNotIn("builtin_attachments", gun_data)
            display = json.loads((pack / "assets/h39/display/guns/h39_display.json").read_text())
            self.assertEqual(display.get("iron_zoom"), 1.0)
            self.assertFalse((pack / "data/h39/index/attachments/healer_optic.json").exists())
            self.assertFalse((pack / "assets/h39/display/attachments/healer_optic_display.json").exists())
            self.assertFalse((pack / "assets/h39/textures/attachment/uv/healer_optic.png").exists())
            self.assertIn("medical_cross_right", bones)
            ammo_display = json.loads(
                (pack / "assets/h39/display/ammo/medical_tube_display.json").read_text()
            )
            self.assertEqual(
                ammo_display["shell"]["model"], "h39:shell/empty_medical_tube"
            )
            shell_geo = pack / "assets/h39/geo_models/shell/empty_medical_tube.json"
            self.assertTrue(shell_geo.is_file(), "spent tube needs its own 3D model")

            animation = json.loads((pack / "assets/h39/animations/h39.animation.json").read_text())
            for name in ("static_idle", "shoot", "reload_empty", "reload_tactical",
                         "draw", "put_away", "inspect", "aim"):
                self.assertIn(name, animation["animations"])

            # TaCZ's Lua state machine must have a matching animation for
            # each count. Toggling bones is visual only, not another ammo store.
            display = json.loads((pack / "assets/h39/display/guns/h39_display.json").read_text())
            self.assertEqual(display["state_machine"], "h39:h39_state_machine")
            lua = (pack / "assets/h39/scripts/h39_state_machine.lua").read_text()
            self.assertIn("context:getAmmoCount()", lua)
            self.assertIn("PLAY_ONCE_HOLD", lua)
            self.assertIn("default.PARALLEL_TRACK_5", lua)
            self.assertIn("base_track_state = h39_base", lua)
            self.assertNotIn("static_track_top.value =", lua)
            self.assertNotIn("table.insert(states", lua)

            for visible in range(5):
                states = animation["animations"]["tubes_" + str(visible)]["bones"]
                for i in range(1, 5):
                    want = [1, 1, 1] if i > 4 - visible else [0, 0, 0]
                    self.assertEqual(states["medical_tube_" + str(i)]["scale"], want)

            textures = [
                "gun/uv/h39.png", "gun/slot/h39.png", "gun/hud/h39.png",
                "ammo/uv/medical_tube.png", "ammo/slot/medical_tube.png",
                "shell/uv/empty_medical_tube.png"
            ]
            for texture in textures:
                file = pack / "assets/h39/textures" / texture
                raw = file.read_bytes()
                self.assertTrue(raw.startswith(b"\x89PNG\r\n\x1a\n"), texture)
                w, h = struct.unpack(">II", raw[16:24])
                size = (256, 256) if "/uv/" in texture else (128, 128)
                self.assertEqual((w, h), size)

            # Original air-pressure shots and medical cartridge manipulation
            # must ship as Ogg/Vorbis, not just sound keys in display JSON.
            sound_ids = (
                "h39_shoot", "h39_shoot_3p", "h39_silence",
                "h39_silence_3p", "h39_reload_tactical",
                "h39_reload_empty", "h39_draw", "h39_put_away"
            )
            sound_dir = pack / "assets/h39/tacz_sounds/h39"
            for name in sound_ids:
                path = sound_dir / (name + ".ogg")
                self.assertTrue(path.is_file(), name)
                self.assertEqual(path.read_bytes()[:4], b"OggS", name)
            self.assertEqual(len(list(sound_dir.glob("*.ogg"))), len(sound_ids))
            sounds = display["sounds"]
            for name in sound_ids:
                self.assertIn("h39:h39/" + name, sounds.values())

            with zipfile.ZipFile(exported) as archive:
                names = set(archive.namelist())
                self.assertIn("gunpack.meta.json", names)
                self.assertIn("assets/h39/textures/gun/uv/h39.png", names)
                self.assertIn("assets/h39/geo_models/gun/h39_geo.json", names)
                self.assertIn("assets/h39/geo_models/shell/empty_medical_tube.json", names)
                self.assertIn("assets/h39/tacz_sounds/h39/h39_shoot.ogg", names)
                self.assertNotIn("assets/tacz/custom/h39_healer/gunpack.meta.json", names)

if __name__ == "__main__":
    unittest.main()
