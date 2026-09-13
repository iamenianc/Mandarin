#!/usr/bin/env python3
"""Generate placeholder tone-contour clips for the tones module.

No Mandarin TTS voice is available in this environment, so these files stand in for
Kokoro-generated reference audio. Each clip is a synthetic pitch contour that shows the
shape of a tone; it is not a human voice and not a pronunciation model. Replace the
files and their manifest entries once the build-time pipeline can synthesize real
reference audio (ADR 0006).

Output: 24 kHz mono 16-bit PCM WAV, one clip per tone 1-5, written to
assets/audio/reference/tones/.
"""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 24_000
AMPLITUDE = 0.35
ATTACK_S = 0.02
RELEASE_S = 0.04


def _sweep(frequency, duration_s):
    """Return PCM samples for a contour given frequency(t) in Hz."""
    total = int(SAMPLE_RATE * duration_s)
    attack = int(SAMPLE_RATE * ATTACK_S)
    release = int(SAMPLE_RATE * RELEASE_S)
    phase = 0.0
    frames = []
    for index in range(total):
        t = index / SAMPLE_RATE
        phase += 2.0 * math.pi * frequency(t) / SAMPLE_RATE
        gain = 1.0
        if index < attack:
            gain = index / attack
        elif index > total - release:
            gain = (total - index) / release
        value = AMPLITUDE * gain * math.sin(phase)
        frames.append(struct.pack("<h", int(max(-1.0, min(1.0, value)) * 32767)))
    return b"".join(frames)


def _level(start_hz, end_hz, duration_s):
    return lambda t: start_hz + (end_hz - start_hz) * (t / duration_s)


def _dip(start_hz, low_hz, end_hz, duration_s):
    breakpoint = duration_s * 0.4

    def frequency(t):
        if t < breakpoint:
            return start_hz + (low_hz - start_hz) * (t / breakpoint)
        return low_hz + (end_hz - low_hz) * ((t - breakpoint) / (duration_s - breakpoint))

    return frequency


TONES = {
    # tone number -> (frequency contour, duration in seconds)
    1: (_level(220.0, 220.0, 0.70), 0.70),
    2: (_level(170.0, 260.0, 0.70), 0.70),
    3: (_dip(170.0, 130.0, 210.0, 0.80), 0.80),
    4: (_level(280.0, 160.0, 0.70), 0.70),
    5: (_level(175.0, 175.0, 0.35), 0.35),
}


def main():
    out_dir = Path(__file__).resolve().parent.parent / "reference" / "tones"
    out_dir.mkdir(parents=True, exist_ok=True)
    for tone, (frequency, duration_s) in TONES.items():
        path = out_dir / f"tone{tone}-contour.wav"
        with wave.open(str(path), "wb") as handle:
            handle.setnchannels(1)
            handle.setsampwidth(2)
            handle.setframerate(SAMPLE_RATE)
            handle.writeframes(_sweep(frequency, duration_s))
        print(path.as_posix())


if __name__ == "__main__":
    main()
