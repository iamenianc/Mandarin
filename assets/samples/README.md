# Samples

Small, representative materials that demonstrate the formats and structure used by
the project's content. A sample is a minimal, self-contained illustration: enough to
show the shape of a file, not enough to teach from. Samples are reference material
for authoring and review; the app never loads them at runtime.

## Contents

| File | Demonstrates |
| --- | --- |
| `content/module.example.json` | A learning module and its ordered lessons. |
| `content/lesson.example.json` | A lesson and its ordered content items. |
| `content/phrase.example.json` | A `phrase` content item, with the optional Hangul field. |
| `content/word.example.json` | A `word` content item. |
| `content/dialogue.example.json` | A `dialogue` content item with ordered turns. |
| `audio/audio-track.example.json` | Reference-audio track metadata (ADR 0006). |
| `mission/field-mission.example.json` | A bundled daily field mission and its five locals. |
| `mission/local-persona.example.json` | One simulated local persona. |
| `mission/debrief-entry.example.json` | A debrief entry logged after a mission. |
| `progress/attempt.example.json` | A stored learner attempt and its feedback. |
| `progress/progress.example.json` | Per-item practice progress and feedback themes. |
| `workflow/wf-1-request.example.json` | WF-1 pronunciation-feedback input, with evidence. |
| `workflow/wf-1-response.example.json` | WF-1 coaching output. |
| `workflow/wf-8-response.example.json` | WF-8 generated practice items. |
| `authoring/lesson-outline.example.md` | An authoring outline for human review (WF-6). |

## What belongs here

- One minimal sample per content format, following the data model in
  `docs/02-architecture.md` and the workflow schemas in `docs/08-ai-workflows.md`.
- Samples that show fields, identifiers, and relationships: a module's lesson order, a
  lesson's items, a dialogue's turns, a mission's script and personas.
- Sample audio-track metadata for a generated reference clip: pinyin with tone numbers,
  target tones, duration, sample rate, and voice provenance (ADR 0006).
- A text stand-in for audio. Audio is referenced by path or shown as a placeholder
  token; the sample never carries the audio itself.

## What does not belong here

- Audio files and other binaries. Audio belongs in `assets/audio/`; samples describe
  it in text.
- Real lesson content or complete teaching material. Those belong in
  `assets/lessons/` or in the feature module that owns them.
- Generated or exported output, runtime data, or anything the app produces.
- Secrets, API keys, or provider credentials.

## Naming conventions

- Lowercase kebab-case, with a descriptive topic and the `.example` marker before the
  extension: `content/lesson.example.json`, `audio/audio-track.example.json`.
  The marker identifies a file as illustrative, in the spirit of
  `api/.dev.vars.example`.
- The file name names the entity or format it shows, in the singular
  (`lesson`, not `lessons`). Folders group related samples; the file name may repeat
  the folder when that keeps the format obvious.
- Do not encode dates, versions, or personal names in file names.
- Text files are UTF-8 with LF line endings and no trailing spaces.

## Content rules

- Pinyin carries tone numbers and no hanzi appears anywhere (ADR 0011, ADR 0012).
  Hangul stays optional and, when present, follows ADR 0004. Meanings and labels are
  English.
- Identifiers and values are visibly placeholders, so a sample cannot be mistaken for
  shippable content or collide with real content ids.
- Field names follow the documented model. Where a format is not yet settled, the
  sample shows a proposed shape and is updated or removed when the schema is decided.
- Keep each sample as small as possible: the fewest fields and items that still show
  the format.

## Related documents

- `docs/02-architecture.md` - data model and content pipeline.
- `docs/08-ai-workflows.md` - workflow registry and fixed output schemas.
- `docs/05-decisions/0004-hangul-phonetic-aid.md` - optional Hangul field.
- `docs/05-decisions/0006-kokoro-reference-audio.md` - reference-audio generation and format.
- `docs/05-decisions/0008-curriculum-scope.md` - themes and modules.
- `docs/05-decisions/0010-bundled-plus-generated-exercises.md` - bundled and generated content.
- `docs/05-decisions/0011-pinyin-pronunciation-layer.md` - pinyin as the pronunciation key.
- `docs/05-decisions/0012-tone-numbers-and-tones-module.md` - tone-number notation.
- `docs/05-decisions/0013-daily-lamp-field-loop.md` - the daily field loop.
