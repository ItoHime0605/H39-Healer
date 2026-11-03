#!/usr/bin/env python3
"""Static checks for candidate TaCZ data, not a game compatibility test."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "gunpack"
gun = json.loads((root / "data/h39/data/guns/h39_data.json").read_text(encoding="utf-8"))
recipe = json.loads((root / "data/h39/recipe/ammo/medical_tube.json").read_text(encoding="utf-8"))
display = json.loads((root / "assets/h39/display/guns/h39_display.json").read_text(encoding="utf-8"))

assert gun["ammo"] == "h39:medical_tube"
assert gun["ammo_amount"] == 20
# Pneumatic H-39 must use one unified ammo count; closed bolt tracks
# an extra chamber round separately and breaks the 20/20 HUD/tube display.
assert gun["bolt"] == "open_bolt"
assert gun["rpm"] == 180
assert gun["fire_mode"] == ["semi"]
assert gun["bullet"]["damage"] == 0.5
# TaCZ R7 default pack comparison: M4A1 speed=270, M700 speed=380.
# Medical pneumatic projectile is deliberately slower, but persists long
# enough to remain useful for mid-to-long-range teammate support.
assert gun["bullet"]["speed"] == 200
assert gun["bullet"]["life"] == 1.5
assert gun["bullet"]["gravity"] == 0.03
assert 0 < gun["bullet"]["gravity"] < 0.15  # Slight arc, not a straight hitscan projectile
assert gun["bullet"]["speed"] < 270
# Precision medical darts must fly along the crosshair, without TaCZ's
# old 5.5-degree standing / 6-degree moving random dispersion.
assert set(gun["inaccuracy"]) == {"stand", "move", "sneak", "lie", "aim"}
assert all(value == 0 for value in gun["inaccuracy"].values())


assert gun["bullet"]["extra_damage"]["damage_adjust"][0]["distance"] == "infinite"
assert recipe["result"]["id"] == gun["ammo"]
assert recipe["result"]["count"] == 1
materials = {entry["item"]["item"]: entry["count"] for entry in recipe["materials"]}
assert materials == {
    "meds_and_herbs:aloe_leaf": 5,
    "meds_and_herbs:chamomile": 5,
    "meds_and_herbs:plantago_leaf": 5,
    "meds_and_herbs:glass_tube": 2,
    "minecraft:glistering_melon_slice": 5,
    "minecraft:iron_ingot": 1,
}
assert display["model"] == "h39:gun/h39_geo"
assert display["iron_zoom"] == 1.0
assert "builtin_attachments" not in gun
assert gun["allow_attachment_types"] == ["scope", "laser", "grip", "muzzle"]
assert "extended_mag" not in gun["allow_attachment_types"]
assert "ammo_mod" not in gun["allow_attachment_types"]
print("H39 candidate gun data and medical tube recipe: static checks passed")
print("WARNING: TaCZ has not been shown to consume one tube as five rounds.")
print("INFO: original 3D model, texture generator and animation resources are present; runtime mounting is not yet verified.")
