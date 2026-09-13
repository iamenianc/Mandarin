# 0002 - Phonetic aid: Hanyu Pinyin

- Status: accepted (amended by 0004 for Hangul; amended by 0011 as the taught pronunciation layer)
- Date: 2026-09-12

## Context

The app teaches listening and speaking only, but absolute beginners benefit from a
familiar phonetic spelling as a support while their ear develops. The two widely used
systems are Hanyu Pinyin and zhuyin (bopomofo). The app must choose what text to show
without drifting into teaching reading.

## Decision

- **Hanyu Pinyin** is the phonetic aid for every phrase, and the only one in v1.
- Treat pinyin as the **taught pronunciation layer**: always visible, explicitly taught
  as a sound map, and echoed in feedback - never as a standalone reading curriculum
  (ADR 0011).
- Do not display hanzi anywhere.
- Zhuyin (bopomofo) is out of scope for v1; revisit if there is demand.

## Consequences

- Content authoring stores a `pinyin` string per phrase; the data model carries it as
  a first-class field.
- Pronunciation feedback maps scores back onto pinyin syllables and tones.
- Beginners who cannot yet read pinyin are not blocked: the audio path never requires
  text, even though pinyin stays on screen.
- Alternative considered: zhuyin as an alternative aid (deferred to avoid doubling the
  authoring and feedback mapping work) and no text at all (purest, but too disorienting
  for many absolute beginners).
