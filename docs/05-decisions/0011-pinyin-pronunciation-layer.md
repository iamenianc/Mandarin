# 0011 - Pinyin as the pronunciation layer

- Status: accepted
- Date: 2026-09-13
- Amends: 0002-phonetic-aids.md

## Context

Understanding pinyin is crucial: it is how the learner knows how a word should be
pronounced. The app already displays pinyin for every phrase (ADR 0002), but the docs
describe it as passive support - "secondary to the audio", "toggleable" - and the
curriculum treats the pinyin sound system as one topic among many. If pinyin stays
passive, the learner depends on audio playback for every pronunciation question and
never gains a portable key to how words sound.

## Decision

- Pinyin is the app's **pronunciation layer**: explicitly taught, always visible, and
  echoed in every drill and every piece of feedback.
- **Taught explicitly.** The tones module (ADR 0012) teaches the tone numbers and
  contours; fundamentals covers initials, finals, spelling conventions, and tone sandhi in
  context. Practice includes reading shown pinyin aloud against reference audio and
  mapping heard audio back to pinyin.
- **Always visible.** Pinyin is displayed for every phrase and target in every module;
  Hangul (optional, ADR 0004) appears alongside it. Audio is never gated on text.
- **Feedback speaks pinyin.** AI coaching names the weakest pinyin syllable and tone
  ("second tone", "final -ng"), never characters.
- **Still not a reading curriculum.** No pinyin paragraph/text reading for its own sake,
  no hanzi anywhere (ADR 0002), and eyes-free mode remains possible.

## Consequences

- The learner can look at new pinyin and know how it should sound - a durable skill that
  works outside the app.
- Requirements add explicit pinyin-teaching and read-aloud practice FRs; the fundamentals
  module gains pinyin-system lessons; design, roadmap, and AI workflows reference pinyin
  in every module.
- `ContentItem.pinyin` is already first-class (ADR 0002), so no data-model change;
  prompts and feedback schemas must express pronunciation units in pinyin syllables and
  tones.
- The tone-notation question is resolved by ADR 0012: tones are written as numbers, and
  taught in a dedicated tones module.
- Alternatives considered: keep pinyin passive (rejected - no portable pronunciation
  key); a pinyin reading/transliteration curriculum (rejected - reading fluency is not
  the goal); Hangul as the taught system (rejected - pinyin is the standard and is
  tone-marked).
