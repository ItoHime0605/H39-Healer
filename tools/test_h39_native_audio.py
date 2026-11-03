"""Check that gunshot audio uses the root NeoForge sound registry, not the optional
TaCZ dynamic gunpack sound loader. Gameplay still requires client verification.
"""
import json
import math
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
from generate_h39_sounds import RATE, synthesize

class NativeShotAudioTests(unittest.TestCase):
    def test_server_verified_shot_sound(self):
        code = (ROOT / "src/main/java/tw/h39/compat/H39PneumaticAudio.java").read_text()
        self.assertIn("onServerGunShot(GunShootEvent event)", code)
        self.assertIn("EventPriority.LOWEST", code)
        self.assertIn("event.getLogicalSide() != LogicalSide.SERVER", code)
        self.assertIn("event.isCanceled()", code)
        self.assertIn("level.playSound(null,", code)
        self.assertIn("PNEUMATIC_SHOT.get()", code)
        self.assertNotIn("GunFireEvent event", code)

    def test_native_registration_metadata_and_localization(self):
        data=json.loads((ROOT / "src/main/resources/assets/h39medical/sounds.json").read_text())
        self.assertEqual(data["pneumatic_shot"]["sounds"][0]["name"],
                         "h39medical:pneumatic_shot")
        self.assertEqual(data["pneumatic_shot"]["subtitle"],
                         "subtitles.h39medical.pneumatic_shot")
        for name in ("zh_tw","en_us"):
            lang=json.loads((ROOT / ("src/main/resources/assets/h39medical/lang/" + name + ".json")).read_text())
            self.assertTrue(lang["subtitles.h39medical.pneumatic_shot"])

    def test_pneumatic_pop_whoosh_is_audible(self):
        samples=synthesize("shot", 0.48, 0)
        self.assertEqual(len(samples), round(0.48*RATE))
        rms=lambda seq: math.sqrt(sum(s*s for s in seq)/len(seq))/32768
        pop=samples[:int(.05*RATE)]
        whoosh=samples[int(.05*RATE):int(.25*RATE)]
        self.assertGreater(rms(pop), .10, "Impact pop should be clearly audible")
        self.assertGreater(rms(whoosh), .06, "Air release should be clearly audible")
        self.assertLess(max(abs(s) for s in samples), 32768)

if __name__ == "__main__":
    unittest.main()
