# Samples

Small, representative materials that demonstrate the formats and structure used by
the project's content. A sample is a minimal, self-contained illustration: enough to
show the shape of a file, not enough to teach from. Samples are reference material
for authoring and review; the app never loads them at runtime.

## What belongs here

- One minimal sample per content format, following the data model in
  `docs/02-architecture.md` (`ContentItem`, `Lesson`, `FieldMission`, and similar).
- Sample lesson entries that show the fields, identifiers, and relationships of a
  lesson and its items.
- Sample audio-track metadata for a reference clip: pinyin with tone numbers, target
  tones, duration, sample rate, and voice provenance (ADR 0006).
- Sample field-mission scripts with per-turn pinyin, meaning, and target tones.

## What does not belong here

- Audio files and other binaries. Audio belongs in `assets/audio/`; samples describe
  it in text.
- Real lesson content or complete teaching material. Those belong in
  `assets/lessons/` or in the feature module that owns them.
- Generated or exported output, runtime data, or anything the app produces.
- Secrets, API keys, or provider credentials.

## Naming conventions

- Lowercase kebab-case with a descriptive topic, for example
  `lesson-entry.example.json` or `audio-track.example.json`.
- The `.example` marker before the extension identifies a file as illustrative, in
  the spirit of `api/.dev.vars.example`.
- One format and one topic per file; name the format the file demonstrates, using
  the singular form (`lesson-entry`, not `lessons`).
- Do not encode dates, versions, or personal names in file names.
- Text files are UTF-8 with LF line endings and no trailing spaces.

## Content rules

- Pinyin carries tone numbers and no hanzi appears anywhere (ADR 0011, ADR 0012).
  Hangul stays optional and, when present, follows ADR 0004. Meanings and labels are
  English.
- Identifiers and values are visibly placeholders, so a sample cannot be mistaken
  for shippable content or collide with real content ids.
- Field names and structure match the documented model exactly. When the model
  changes, the affected sample changes or is removed in the same commit.
- Keep each sample as small as possible: the fewest fields and items that still show
  the format.

## Related documents

- `docs/02-architecture.md` - proposed stack, data model, and content pipeline.
- `docs/05-decisions/0004-hangul-phonetic-aid.md` - optional Hangul field.
- `docs/05-decisions/0006-kokoro-reference-audio.md` - reference-audio generation and format.
- `docs/05-decisions/0010-bundled-plus-generated-exercises.md` - bundled and generated content.
- `docs/05-decisions/0011-pinyin-pronunciation-layer.md` - pinyin as the pronunciation key.
- `docs/05-decisions/0012-tone-numbers-and-tones-module.md` - tone-number notation.
