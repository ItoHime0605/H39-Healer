#!/usr/bin/env python3
"""Compare H39's gunpack structure against a user-supplied TaCZ JAR.

Does not upload, modify or extract proprietary assets.
Usage: python tools/check_tacz_gunpack_schema.py /path/to/tacz.jar
"""
import json
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "gunpack"
PREFIX = "assets/tacz/custom/tacz_default_gun/"
CASES = (
    ("data/h39/index/guns/h39.json", "data/tacz/index/guns/m700.json"),
    ("data/h39/index/ammo/medical_tube.json", "data/tacz/index/ammo/308.json"),
    ("data/h39/recipe/ammo/medical_tube.json", "data/tacz/recipe/ammo/308.json"),
    ("data/h39/data/guns/h39_data.json", "data/tacz/data/guns/m700_data.json"),
    ("assets/h39/display/guns/h39_display.json", "assets/tacz/display/guns/m700_display.json"),
)

def main():
    if len(sys.argv) != 2:
        print("Usage: python tools/check_tacz_gunpack_schema.py /path/to/tacz.jar")
        return 2
    errors = []
    with zipfile.ZipFile(sys.argv[1]) as archive:
        for local, reference in CASES:
            ours = json.loads((ROOT / local).read_text(encoding="utf-8"))
            # Some official display/data files contain JSON5-style comments.
            # Compare key names with a comment-tolerant lightweight scanner.
            raw = archive.read(PREFIX + reference).decode("utf-8-sig")
            import re
            official_keys = set(re.findall(r'^\s*"([^"]+)"\s*:', raw, re.MULTILINE))
            for key in ours:
                if key not in official_keys:
                    errors.append(f"{local}: unsupported candidate key {key!r}")
            print(f"checked {local} against {reference}")
        # State machine identifiers are referenced by built-in gun display JSON.
        display = json.loads((ROOT / CASES[-1][0]).read_text(encoding="utf-8"))
        target = display.get("state_machine")
        if target == "h39:h39_state_machine":
            script = ROOT / "assets/h39/scripts/h39_state_machine.lua"
            if not script.is_file():
                errors.append("Missing H-39 custom Lua state machine")
            else:
                script_text = script.read_text(encoding="utf-8")
                if 'require("tacz_default_state_machine")' not in script_text:
                    errors.append("H-39 script does not inherit TaCZ default state machine")
        else:
            found = any(
                target.encode("utf-8") in archive.read(name)
                for name in archive.namelist()
                if name.startswith(PREFIX + "assets/tacz/display/guns/")
                and name.endswith(".json")
            )
            if not found:
                errors.append(f"Unreferenced TaCZ state machine: {target}")
    if errors:
        print("\n".join(errors))
        return 1
    print("H-39 candidate top-level keys and state machine match TaCZ examples.")
    print("This is a static schema comparison, not a Minecraft loading test.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
