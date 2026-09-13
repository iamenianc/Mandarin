# Brand

## Name

**LearnHuayu** (`huayu` = the Chinese language, Mandarin). Written as one word with a
capital L and H: `LearnHuayu`.

## Product statement

A private, audio-first Android app that teaches beginners to listen to and speak
Mandarin Chinese, with pinyin always shown as the pronunciation key and AI-assisted
speech and conversation coaching (`docs/00-vision.md`).

## Voice

- Patient and encouraging; coaching names one specific thing to fix rather than showing
  a grade (`docs/03-design.md`).
- Plain English. Pinyin is the only Chinese text and always carries tone numbers
  (ADR 0011, ADR 0012).
- Never punitive about the microphone or the model's uncertainty (`docs/03-design.md`).

## Brand invariants

- No hanzi anywhere, including artwork, icons, and splash (FR-17; `docs/03-design.md`).
- Pinyin is shown with tone numbers, never tone marks (ADR 0012).
- Hangul is an optional aid, never a substitute for pinyin (FR-4, FR-5).
- The identity is audio-first: the mark is a tone contour, not a character.

## Mark

The mark is a single rounded stroke tracing a tone contour - a rise to a plateau, then a
fall - inside a rounded tile. It reads as a voice intonation curve at launcher size and
carries no text.

- Authoritative source: `source/brand-mark.svg`.
- Clear space: at least the stroke width on all sides.
- Minimum size: 24 px for the stroke detail; below that, use the tile.
- Do not add text or hanzi, rotate the contour, apply effects, or recolour the stroke
  outside `palette.json`.
- Icon production follows `icon-spec.md`.

## Palette and type

See `palette.json` and `typography.md`.
