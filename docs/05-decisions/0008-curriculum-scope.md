# 0008 - Curriculum scope

- Status: accepted (amended by 0012 for tone numbers and a dedicated tones module)
- Date: 2026-09-13

## Context

The app must decide *what* it teaches first and how each learning area relates to the
existing audio-first constraint. An earlier draft of `docs/03-design.md` said there was
"deliberately no vocabulary browser," which conflicts with the new vocabulary module.
The risk is drifting back toward reading/writing drills.

## Decision

- The initial build teaches **first principles, fundamentals, and theory** (how Mandarin
  sound works) plus **beginner / tourist / survival Mandarin**.
- **Tones:** the four tones and the neutral tone, their contours, the tone-number
  notation, and tone pairs - a dedicated module taught to a beginner ahead of words
  (ADR 0012).
- **Fundamentals and theory:** syllable anatomy (initial / final), the pinyin sound
  system (initials, finals, spelling conventions), tone sandhi, rhythm and stress -
  taught **by ear**, with pinyin explicitly taught and always shown as the pronunciation
  key (ADR 0011).
- **Themes:** greetings and courtesy, numbers and money, ordering food, directions and
  transport, hotel, shopping, time, emergencies.
- **Vocabulary is audio-first.** Words and phrases are taught as sound plus meaning and
  practiced by hearing and saying them. This is **not** a hanzi/character browser: no
  hanzi is displayed (ADR 0002), pinyin is always shown and taught as the pronunciation
  key (ADR 0011), Hangul stays optional (ADR 0004).
- **No calibrated scoring.** Pronunciation and tone feedback remains soft AI coaching per
  ADR 0005; no pass/fail.

## Consequences

- Scope is now organized around **modules** (tones, vocabulary, listening, speech, and
  fundamentals), each with **lessons and practice** (ADR 0007).
- The blanket "no vocabulary" statement is replaced by a narrower, still-binding rule:
  **no hanzi and no character-based reading curricula**; audio vocabulary is in scope.
- Content authoring gains a thematic curriculum to build against, but the no-hanzi and
  audio-first invariants are unchanged.
- Alternative considered: keeping vocabulary out entirely (rejected - high-frequency
  vocabulary is a prerequisite for survival listening and speech) and a reading-based
  vocabulary browser (rejected - violates the audio-first, no-hanzi product).
