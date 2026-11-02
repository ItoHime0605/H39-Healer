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
assert gun["rpm"] == 180
assert gun["fire_mode"] == ["semi"]
assert gun["bullet"]["damage"] == 0.5
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
print("H39 candidate gun data and medical tube recipe: static checks passed")
print("WARNING: TaCZ has not been shown to consume one tube as five rounds.")
print("WARNING: referenced 3D model, texture, and animation are not packaged yet.")
