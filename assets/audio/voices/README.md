# Voice configuration

The voices used to generate reference clips with Kokoro-82M (`lang_code='z'`) at build
time (ADR 0006). A voice is pinned, not chosen at runtime, so a rebuild produces the
same clip from the same pinyin.

## Rules

- Each generated track records its voice in `tts.voice`, with the pinned checkpoint in
  `tts.checkpoint` (`../schema/track.schema.json`).
- Voice ids come from the pinned Kokoro Mandarin set; `registry.yaml` lists the ids in
  use. No id is added until a clip uses it.
- A dialogue or field-mission clip may use more than one voice; each turn records its
  own voice.
- `LocalPersona.voiceProfile` maps to a voice id for field missions
  (`docs/02-architecture.md`).
- A voice is a property of a clip, never learner-selectable content.
