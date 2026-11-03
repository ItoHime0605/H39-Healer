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
            subprocess.run([
                sys.executable,
                str(ROOT / "tools/generate_h39_gunpack.py"),
                "--output", str(out),
                "--zip", str(exported)
            ], check=True, capture_output=True, text=True)

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
            self.assertIn("medical_cross_right", bones)

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
            for visible in range(5):
                states = animation["animations"]["tubes_" + str(visible)]["bones"]
                for i in range(1, 5):
                    want = [1, 1, 1] if i <= visible else [0, 0, 0]
                    self.assertEqual(states["medical_tube_" + str(i)]["scale"], want)

            textures = [
                "gun/uv/h39.png", "gun/slot/h39.png", "gun/hud/h39.png",
                "ammo/uv/medical_tube.png", "ammo/slot/medical_tube.png"
            ]
            for texture in textures:
                file = pack / "assets/h39/textures" / texture
                raw = file.read_bytes()
                self.assertTrue(raw.startswith(b"\x89PNG\r\n\x1a\n"), texture)
                w, h = struct.unpack(">II", raw[16:24])
                size = (256, 256) if "/uv/" in texture else (128, 128)
                self.assertEqual((w, h), size)

            with zipfile.ZipFile(exported) as archive:
                names = set(archive.namelist())
                self.assertIn("gunpack.meta.json", names)
                self.assertIn("assets/h39/textures/gun/uv/h39.png", names)
                self.assertIn("assets/h39/geo_models/gun/h39_geo.json", names)
                self.assertNotIn("assets/tacz/custom/h39_healer/gunpack.meta.json", names)

if __name__ == "__main__":
    unittest.main()
