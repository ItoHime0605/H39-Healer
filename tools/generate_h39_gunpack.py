#!/usr/bin/env python3
"""Generate original H-39 TaCZ textures and stage the gunpack into mod resources.

Requires only Python 3 standard library. Does NOT copy third-party TaCZ assets.

Examples:
    python3 tools/generate_h39_gunpack.py --output build/generated/h39-resources
    python3 tools/generate_h39_gunpack.py --zip dist/H39-Healer-Gunpack.zip
"""
import argparse
import shutil
import struct
import zlib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "gunpack"
PACK_IN_MOD = Path("assets/tacz/custom/h39_healer")

def png_chunk(name, payload):
    return struct.pack(">I", len(payload)) + name + payload + struct.pack(
        ">I", zlib.crc32(name + payload) & 0xffffffff
    )

class Canvas:
    def __init__(self, width, height):
        self.w = width
        self.h = height
        self.data = bytearray(width * height * 4)

    def pixel(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            i = (y * self.w + x) * 4
            self.data[i:i + 4] = bytes(color)

    def rect(self, x, y, w, h, color):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.pixel(xx, yy, color)

    def line(self, x1, y1, x2, y2, color, thick=1):
        dx, dy = x2 - x1, y2 - y1
        steps = max(abs(dx), abs(dy), 1)
        for i in range(steps + 1):
            x = round(x1 + dx * i / steps)
            y = round(y1 + dy * i / steps)
            self.rect(x - thick // 2, y - thick // 2, thick, thick, color)

    def write(self, dest):
        dest.parent.mkdir(parents=True, exist_ok=True)
        scanlines = bytearray()
        for y in range(self.h):
            scanlines.append(0)
            start = y * self.w * 4
            scanlines.extend(self.data[start:start + self.w * 4])
        payload = bytearray(b"\x89PNG\r\n\x1a\n")
        payload += png_chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0))
        payload += png_chunk(b"IDAT", zlib.compress(scanlines, 9))
        payload += png_chunk(b"IEND", b"")
        dest.write_bytes(payload)

def atlas(path):
    """256px UV palette; cells start at (0,0), (88,0), (176,0), etc."""
    colors = [
        (228, 236, 235),  # white ceramic shell
        (138, 159, 163),  # brushed silver
        (59, 72, 79),    # dark barrel
        (66, 216, 115),  # healing liquid
        (206, 43, 57),   # medical red
        (28, 33, 38),    # black grip
        (150, 227, 199), # glossy glass
        (57, 121, 93),   # deep green
        (119, 142, 148), # grey mechanism
    ]
    pic = Canvas(256, 256)
    for index, (r, g, b) in enumerate(colors):
        x0, y0 = (index % 3) * 88, (index // 3) * 88
        for y in range(80):
            for x in range(80):
                n = ((x * 17 + y * 23 + index * 13) % 11) - 5
                shine = (12 if 6 <= x % 23 <= 9 else 0)
                side = -10 if x >= 72 or y >= 75 else 0
                rgb = tuple(max(0, min(255, c + n + shine + side)) for c in (r, g, b))
                pic.pixel(x0 + x, y0 + y, (*rgb, 255))
        for x in range(6, 75):
            pic.pixel(x0 + x, y0 + 4, (255, 255, 255, 135))
        for y in range(8, 73):
            pic.pixel(x0 + 4, y0 + y, (255, 255, 255, 100))
    pic.write(path)

def rifle_icon(path, hud=False):
    pic = Canvas(128, 128)
    shadow = (28, 42, 50, 255)
    metal = (228, 235, 235, 255)
    pale = (160, 186, 192, 255)
    green = (78, 224, 134, 255)
    red = (220, 59, 64, 255)
    # Long futuristic rifle from stock (left/down) to muzzle (right/up).
    pic.line(17, 88, 108, 43, shadow, 19)
    pic.line(17, 87, 108, 42, metal, 13)
    pic.line(34, 76, 103, 42, pale, 4)
    pic.line(83, 53, 118, 36, shadow, 5)
    pic.line(86, 52, 118, 36, metal, 2)
    pic.line(38, 81, 29, 112, shadow, 12)
    pic.line(39, 81, 32, 108, (60, 72, 79, 255), 7)
    pic.line(45, 64, 71, 50, shadow, 8)
    pic.line(46, 64, 71, 50, metal, 5)
    for k in range(4):
        x = 51 + k * 11
        y = 71 - k * 5
        pic.line(x, y, x + 7, y - 3, green, 4)
    pic.rect(25, 79, 11, 3, red)
    pic.rect(29, 75, 3, 11, red)
    if hud:
        pic.rect(10, 116, 108, 2, (86, 224, 145, 220))
    pic.write(path)

def tube_icon(path):
    pic = Canvas(128, 128)
    pic.line(28, 100, 92, 27, (21, 49, 50, 255), 24)
    pic.line(28, 100, 92, 27, (61, 203, 116, 255), 18)
    pic.line(34, 91, 87, 32, (177, 249, 195, 255), 5)
    pic.line(24, 103, 36, 90, (227, 237, 235, 255), 22)
    pic.line(86, 35, 98, 22, (227, 237, 235, 255), 22)
    pic.line(24, 103, 36, 90, (94, 119, 122, 255), 4)
    pic.line(86, 35, 98, 22, (94, 119, 122, 255), 4)
    pic.write(path)

def create_png_files(pack):
    assets = pack / "assets/h39/textures"
    atlas(assets / "gun/uv/h39.png")
    atlas(assets / "ammo/uv/medical_tube.png")
    atlas(assets / "shell/uv/empty_medical_tube.png")
    rifle_icon(assets / "gun/slot/h39.png")
    rifle_icon(assets / "gun/hud/h39.png", hud=True)
    tube_icon(assets / "ammo/slot/medical_tube.png")

def stage(target):
    pack = target / PACK_IN_MOD
    if pack.exists():
        shutil.rmtree(pack)
    shutil.copytree(SOURCE, pack)
    create_png_files(pack)
    # The directory should be a gunpack root, not contain an outer /gunpack folder.
    assert (pack / "gunpack.meta.json").is_file()
    return pack

def zip_pack(pack, dest):
    dest.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(dest, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for path in sorted(pack.rglob("*")):
            if path.is_file():
                z.write(path, path.relative_to(pack).as_posix())

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=ROOT / "build/generated/h39-resources")
    parser.add_argument("--zip", type=Path)
    args = parser.parse_args()
    pack = stage(args.output.resolve())
    if args.zip:
        zip_pack(pack, args.zip.resolve())
    files = [p for p in pack.rglob("*") if p.is_file()]
    print(f"Generated original H-39 gunpack: {len(files)} files under {pack}")

if __name__ == "__main__":
    main()
