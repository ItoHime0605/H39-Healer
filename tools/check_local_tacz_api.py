#!/usr/bin/env python3
"""Check a user-owned TaCZ JAR without publishing or modifying it.

Usage:
  python tools/check_local_tacz_api.py /path/to/tacz.jar
Requires: JDK 21 (javap).
"""
import subprocess
import sys
from pathlib import Path

SIGNATURES = {
    "com.tacz.guns.api.event.common.GunReloadEvent": [
        "getEntity()", "getGunItemStack()", "getLogicalSide()",
        "net.neoforged.bus.api.ICancellableEvent",
    ],
    "com.tacz.guns.api.item.IGun": [
        "getIGunOrNull(", "getGunId(", "getCurrentAmmoCount(",
        "setCurrentAmmoCount(",
    ],
    "com.tacz.guns.api.item.IAmmo": [
        "getIAmmoOrNull(", "getAmmoId(",
    ],
}

def main() -> int:
    if len(sys.argv) != 2:
        print("Usage: python tools/check_local_tacz_api.py /path/to/tacz.jar")
        return 2
    jar = Path(sys.argv[1])
    if not jar.is_file():
        print(f"TaCZ JAR not found: {jar}")
        return 2
    missing = []
    for cls, tokens in SIGNATURES.items():
        result = subprocess.run(
            ["javap", "-classpath", str(jar), cls],
            text=True, capture_output=True, check=False,
        )
        if result.returncode:
            missing.append(f"{cls}: javap failed: {result.stderr.strip()}")
            continue
        for token in tokens:
            if token not in result.stdout:
                missing.append(f"{cls}: missing {token}")
    if missing:
        print("\n".join(missing))
        return 1
    print("TaCZ H-39 required API signatures verified.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
