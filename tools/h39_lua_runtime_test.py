#!/usr/bin/env python3
"""Exercise H39 state-machine callbacks on LuaJ and default Lua from TaCZ R7.

Read the user's or public TaCZ JAR. Third-party libraries are extracted only
to a temporary directory; none are committed with the H-39 project.
"""
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT = "assets/tacz/custom/tacz_default_gun/assets/tacz/scripts/default_state_machine.lua"
CORE = "META-INF/jarjar/luaj-core-3.0.8-figura.jar"
JSE = "META-INF/jarjar/luaj-jse-3.0.8-figura.jar"


def check(tacz: Path):
    if not tacz.is_file():
        raise FileNotFoundError("Missing TaCZ JAR: " + str(tacz))
    with tempfile.TemporaryDirectory(prefix="h39-lua-") as root:
        tmp = Path(root)
        with zipfile.ZipFile(tacz) as z:
            for item in (DEFAULT, CORE, JSE):
                if item not in z.namelist():
                    raise AssertionError("Missing TaCZ LuaJ dependency: " + item)
                (tmp / item.rsplit("/", 1)[-1]).write_bytes(z.read(item))
        separator = ";" if sys.platform == "win32" else ":"
        cp = separator.join(str(tmp / p.rsplit("/", 1)[-1]) for p in (CORE, JSE))
        cmd = [
            "java", "-cp", cp, "lua",
            str(ROOT / "tools/h39_luaj_integration.lua"),
            str(tmp / "default_state_machine.lua"),
            str(ROOT / "gunpack/assets/h39/scripts/h39_state_machine.lua"),
        ]
        result = subprocess.run(cmd, text=True, capture_output=True, check=False)
        print(result.stdout, end="")
        # The bundled LuaJ CLI can print a LuaError yet return zero!
        if (result.returncode or result.stderr.strip()
                or "PASS: native TaCZ LuaJ" not in result.stdout):
            raise AssertionError("TaCZ LuaJ test failed:\n" + result.stderr + result.stdout)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: python tools/h39_lua_runtime_test.py <tacz-r7.jar>")
    try:
        check(Path(sys.argv[1]))
    except (AssertionError, OSError, zipfile.BadZipFile) as exc:
        sys.exit(str(exc))
