# 0012 - Tone numbers and a dedicated tones module

- Status: accepted
- Date: 2026-09-13
- Amends: 0008-curriculum-scope.md

## Context

Tones are the hardest part of Mandarin for a beginner and the place where pinyin
notation matters most. The marks-vs-numbers question was left open (ADR 0002), and the
curriculum folds tones into the fundamentals module alongside syllable anatomy and tone
sandhi. With pinyin established as the taught pronunciation layer (ADR 0011), tone
notation and tone teaching need explicit decisions.

## Decision

- **Tones are written with numbers**, not diacritics: each syllable carries its tone
  number after it (e.g., `ni3 hao3`); the neutral tone is `5` (`ma5`). Tone numbers are
  the display convention everywhere: content, drills, feedback, Raymond, and generated
  items.
- **A dedicated `tones` module** teaches the four tones and the neutral tone, their
  contours, the tone-number notation, and tone pairs - to a beginner, ahead of words.
  It is the **recommended starting point** in the app's module order.
- The `fundamentals` module keeps the remaining theory: syllable anatomy (initial/final),
  the pinyin sound system (initials, finals, spelling conventions), tone sandhi, rhythm,
  and stress.
- Pronunciation feedback names tones by number ("tone 2 on the second syllable"), matching
  what is on screen.

## Consequences

- The open design question "pinyin tone marks vs. tone numbers" is resolved; vision,
  design, and requirements state numbers and drop the question.
- Content authoring, `ContentItem.pinyin`, `targetTones`, WF-1 output, and WF-8 items use
  tone numbers. No schema change: `targetTones` is already numeric.
- A new `:feature:tones` module ships in v1 through the existing module contract
  (ADR 0007): content plus a thin feature module, no shell changes.
- Pinyin-system teaching splits: tones module for the numbering convention and contours,
  fundamentals for initials, finals, and spelling; both serve ADR 0011.
- Alternatives considered: tone marks (rejected - diacritics are easy to miss and are
  less explicit about tone identity); tones kept inside fundamentals (rejected - tones
  need their own lessons and progression); both notations (rejected - inconsistent
  display, double authoring).
