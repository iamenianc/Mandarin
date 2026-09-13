# 0006 - Reference audio from Kokoro-82M

- Status: accepted
- Date: 2026-09-12

## Context

Every phrase needs reference audio for listening drills and for the
reference-vs-attempt comparison in ADR 0005. Native-speaker recordings give the best
quality but are slow and costly to source at volume. A permissively licensed,
lightweight TTS that can run at build time is needed.

## Decision

Generate reference audio with **`hexgrad/Kokoro-82M`**:

- 82M-parameter TTS (StyleTTS 2 + ISTFTNet), **Apache-2.0**, v1.0 (2025-01-27).
- 8 languages / 54 voices; **Mandarin via `lang_code='z'`** with the `misaki[zh]` G2P
  package. Output is 24 kHz mono.
- **Input is pinyin** - the G2P accepts pinyin directly, so no hidden hanzi source is
  needed. This matches the app's content model.
- Reference clips are **generated at build time and bundled** with lessons so listening
  drills work offline.
- Kokoro is also the TTS voice for AI conversation replies.

## Consequences

- Tiny footprint (82M) and build-time generation keep the app self-contained and offline
  for the core experience; on-device inference is not pursued (ADR 0003).
- Apache-2.0 is safe for a private project and would allow sharing later.
- Pinyin input removes the earlier risk of needing a hidden hanzi authoring field.
- Kokoro's Mandarin tone quality has been judged excellent; native recordings are not
  required.
- Checkpoints/voices must be pinned by hash for reproducible builds.
- Alternative considered: native recordings (best quality, more effort), Qwen3-TTS
  (larger, more expressive), or a cloud TTS API (cost, keys, network).

Sources: https://huggingface.co/hexgrad/Kokoro-82M, https://github.com/hexgrad/kokoro
