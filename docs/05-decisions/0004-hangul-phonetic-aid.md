# 0004 - Add Hangul as a phonetic aid

- Status: accepted
- Date: 2026-09-12
- Amends: 0002-phonetic-aids.md

## Context

ADR 0002 made Hanyu Pinyin the only phonetic aid. The primary learner is a fluent
English speaker who already reads Hangul, and Hangul's phonemic letters map onto many
Mandarin sounds more directly than English-based romanization. Hangul can serve as a
second, more intuitive phonetic bridge for that learner.

## Decision

- Add **Hangul** as a phonetic aid alongside Hanyu Pinyin.
- Both aids are first-class display fields per phrase. **Hanyu Pinyin is always
  displayed**; Hangul is an optional aid the learner can enable, shown alongside pinyin.
- Hangul is **not taught**. It is assumed prior knowledge, used only to represent sound.
- Hangul is **not tone-marked** (it has no native tone notation); tone is carried by the
  pinyin and the audio.

## Consequences

- Content authoring must store both `pinyin` and `hangul` per phrase.
- Pinyin stays visible for everyone, so the screen stays text-light and audio-first.
- Pronunciation feedback may reference either representation.
- Alternative considered: pinyin-only (rejected - misses the learner's strongest bridge)
  and zhuyin (still out of scope for v1).
