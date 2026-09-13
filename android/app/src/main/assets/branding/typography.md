# Typography

Status: proposed. Typography rules for the minimal on-screen text. Fonts are structured
resources delivered through `res/font/` and are out of scope for this tree.

## Character coverage

- On-screen text is English plus pinyin. Tone is written as a number, so no tone-mark
  glyphs are required (ADR 0012).
- Hangul is optional and requires a font with Hangul coverage. The system default covers
  it, and any custom font must include it.
- Hanzi coverage is neither required nor used; no screen shows hanzi (FR-17).

## Roles

- Use the Material 3 type scale through the `:core:ui` theme. The app is phone-first and
  optimizes for low visual attention (`docs/02-architecture.md`, `docs/03-design.md`).
- Pinyin in drills is prominent and larger than its English gloss, so the sound map leads
  (ADR 0011).
- Feedback names a pinyin syllable and its tone number; render those inline without
  switching fonts.

## Accessibility

- Support system font scaling and TalkBack; avoid hard-coded sizes that break at large
  scale (NFR-7).
- Keep controls labelled and short; long-form text (legal, attribution) lives in assets
  and is scrollable.

## Fonts

No font is bundled in this tree. If a custom typeface is introduced, it is delivered
through `res/font/` as a structured resource and must cover Latin, digits, and Hangul,
and must not include hanzi.
