#!/usr/bin/env python3
"""Deterministically synthesize ORIGINAL non-musical H-39 pneumatic sound effects.

Only Python stdlib + ffmpeg's Ogg Vorbis encoder are required.
No third-party audio is embedded or sampled.
"""
import argparse
import math
import random
import shutil
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

RATE = 22050
TAU = 2 * math.pi

def _noise(seed):
    return random.Random(3905 + seed)

def synthesize(kind, seconds, seed=0):
    rng = _noise(seed)
    samples = []
    air_filtered = 0.0
    for i in range(int(RATE * seconds)):
        t = i / RATE
        n = rng.uniform(-1.0, 1.0)
        if kind == "shot":
            # Audible compressed-air "POMF-shyooo":
            # 0-45 ms: heavy pneumatic valve pop (not a gunpowder explosion).
            # 30-440 ms: pressurized air rush and descending narrow whistle.
            pop = math.exp(-t * 46.0) * (
                0.80 * math.sin(TAU * (130 * t - 45 * t * t))
                + 0.40 * math.sin(TAU * 290 * t))
            after = max(0.0, t - 0.030)
            air_filtered = 0.58 * air_filtered + 0.42 * n
            air_envelope = (1.0 - math.exp(-after * 175.0)) * math.exp(-after * 10.5)
            rush = air_envelope * (0.62 * n + 0.38 * air_filtered)
            chirp = math.sin(TAU * (1250 * after - 760 * after * after))
            whistle = air_envelope * chirp * 0.35
            val = 0.60 * pop + 0.85 * rush + whistle
        elif kind == "remote_shot":
            # Less attack, more audible air plume for third-person playback.
            pop = math.exp(-t * 42) * math.sin(TAU * 134 * t)
            hiss = math.exp(-t * 17) * n
            val = 0.27 * hiss + 0.20 * pop
        elif kind == "quiet_shot":
            val = 0.20 * math.exp(-t * 30) * n + 0.13 * math.exp(-t * 42) * math.sin(TAU * 250 * t)
        elif kind == "reload":
            # Two small medical-cartridge clamp clicks and valve pressure release.
            def click(t0):
                z = t - t0
                return math.exp(-z * 115) * math.sin(TAU * 1700 * z) if z >= 0 else 0
            hiss_start = 0.21 + seed * 0.015
            hiss = math.exp(-(t - hiss_start) * 30) * n if t >= hiss_start else 0
            val = 0.34 * click(0.04) + 0.25 * click(0.18) + 0.16 * hiss
            if seed == 1:
                val += 0.31 * click(0.38)
        else: # draw, stow
            val = (0.25 * math.sin(TAU * 1100 * t) + 0.19 * n) * math.exp(-t * 37)
        # Gentle 5-ms fades suppress pops at clip boundary.
        fade = min(1.0, t / 0.005, max(0, (seconds - t) / 0.005))
        v = max(-0.95, min(0.95, val * fade))
        samples.append(int(v * 32767))
    return samples

def write_ogg(path, kind, duration, seed=0):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "h39.wav"
        with wave.open(str(wav), "wb") as writer:
            writer.setnchannels(1)
            writer.setsampwidth(2)
            writer.setframerate(RATE)
            samples = synthesize(kind, duration, seed)
            writer.writeframes(struct.pack("<" + "h" * len(samples), *samples))
        command = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
                   "-i", str(wav), "-c:a", "libvorbis", "-q:a", "4", str(path)]
        subprocess.run(command, check=True)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    if not shutil.which("ffmpeg"):
        raise SystemExit("ffmpeg is required to generate original H-39 Ogg sounds")
    names = {
        "h39_shoot": ("shot", 0.48, 0),
        "h39_shoot_3p": ("remote_shot", 0.3, 2),
        "h39_silence": ("quiet_shot", 0.22, 1),
        "h39_silence_3p": ("quiet_shot", 0.3, 3),
        "h39_reload_tactical": ("reload", 0.55, 0),
        "h39_reload_empty": ("reload", 0.78, 1),
        "h39_draw": ("mechanical", 0.15, 2),
        "h39_put_away": ("mechanical", 0.16, 4),
    }
    for name, (kind, duration, seed) in names.items():
        write_ogg(args.output_dir / (name + ".ogg"), kind, duration, seed)
    print(f"Generated {len(names)} original pneumatic H-39 Ogg/Vorbis effects")

if __name__ == "__main__":
    main()
