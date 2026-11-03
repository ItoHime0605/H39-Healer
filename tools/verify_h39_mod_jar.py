#!/usr/bin/env python3
"""Verify a compiled NeoForge H-39 JAR is actually a one-JAR TaCZ gunpack.

This checks packaging, NOT gameplay or resource loading at runtime.
Usage: python3 tools/verify_h39_mod_jar.py build/libs/h39medical-0.1.8-alpha.jar
"""
from __future__ import annotations
import re
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = "assets/tacz/custom/h39_healer/"
REQUIRED = {
    "META-INF/neoforge.mods.toml",
    "tw/h39/compat/H39MedicalCompat.class",
    "tw/h39/compat/H39RuntimeDiagnostics.class",
    "tw/h39/compat/H39MedicalHitHandler.class",
    "tw/h39/compat/H39TaCZAmmoEvents.class",
    "tw/h39/compat/H39ReloadPlan.class",
    "tw/h39/compat/H39ReloadTiming.class",
    BASE + "gunpack.meta.json",
    BASE + "data/h39/index/guns/h39.json",
    BASE + "data/h39/data/guns/h39_data.json",
    BASE + "data/h39/index/ammo/medical_tube.json",
    BASE + "data/h39/recipe/gun/h39.json",
    BASE + "data/h39/recipe/ammo/medical_tube.json",
    BASE + "data/h39/tacz_tags/attachments/allow_attachments/h39.json",
    BASE + "assets/h39/display/guns/h39_display.json",
    BASE + "assets/h39/display/ammo/medical_tube_display.json",
    BASE + "assets/h39/geo_models/gun/h39_geo.json",
    BASE + "assets/h39/geo_models/ammo/medical_tube.json",
    BASE + "assets/h39/geo_models/shell/empty_medical_tube.json",
    BASE + "assets/h39/animations/h39.animation.json",
    BASE + "assets/h39/scripts/h39_state_machine.lua",
    BASE + "assets/h39/textures/gun/uv/h39.png",
    BASE + "assets/h39/textures/gun/slot/h39.png",
    BASE + "assets/h39/textures/gun/hud/h39.png",
    BASE + "assets/h39/textures/ammo/uv/medical_tube.png",
    BASE + "assets/h39/textures/ammo/slot/medical_tube.png",
    BASE + "assets/h39/textures/shell/uv/empty_medical_tube.png",
    BASE + "assets/h39/lang/zh_tw.json",
    BASE + "assets/h39/lang/en_us.json",
}
for name in (
    "h39_shoot", "h39_shoot_3p", "h39_silence",
    "h39_silence_3p", "h39_reload_tactical",
    "h39_reload_empty", "h39_draw", "h39_put_away",
):
    REQUIRED.add(BASE + "assets/h39/tacz_sounds/h39/" + name + ".ogg")

def verify(path: Path):
    if not path.is_file():
        raise AssertionError(f"Compiled JAR missing: {path}")
    with zipfile.ZipFile(path) as archive:
        files = set(archive.namelist())
        missing = sorted(REQUIRED - files)
        if missing:
            raise AssertionError("Missing H-39 JAR entries:\n" + "\n".join(missing))
        if any(name.startswith("com/tacz/guns/") for name in files):
            raise AssertionError("Unexpected third-party TaCZ classes in H-39 JAR")
        meta = archive.read("META-INF/neoforge.mods.toml").decode("utf-8")
        expected = re.search(r"(?m)^mod_version=(.+)$",
                             (ROOT / "gradle.properties").read_text(encoding="utf-8"))
        if not expected:
            raise AssertionError("No project mod_version found")
        if f'version="{expected.group(1).strip()}"' not in meta:
            raise AssertionError("Built mod metadata version does not match gradle.properties")
        for name in files:
            if name.startswith(BASE + "assets/h39/tacz_sounds/h39/") and name.endswith(".ogg"):
                if not archive.read(name).startswith(b"OggS"):
                    raise AssertionError("Not an Ogg Vorbis stream: " + name)
    return len(files)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: python3 tools/verify_h39_mod_jar.py <built.jar>")
    path = Path(sys.argv[1])
    try:
        count = verify(path)
    except (AssertionError, OSError, zipfile.BadZipFile) as exc:
        sys.exit("H-39 packaged JAR FAILED: " + str(exc))
    print(f"H-39 one-JAR resource verification passed: {path} ({count} entries)")
