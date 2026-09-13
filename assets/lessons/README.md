# Lessons

Lesson materials for the audio-first curriculum: the guided teaching each learning
module offers alongside its practice (ADR 0007, ADR 0008). One file per lesson.
Lessons are heard and spoken; Mandarin appears as pinyin with tone numbers and never
as hanzi (ADR 0011, ADR 0012). Hangul, where present, is an optional display aid
(ADR 0004).

## Structure

Lessons are organised by level, then by module, the curriculum's teaching unit
(ADR 0007).

| Level | Modules | Focus |
| --- | --- | --- |
| `beginner/` | tones, fundamentals, vocabulary, listening, speech | The sound system first, then first words |
| `tourist/` | vocabulary | Travel themes: food, directions, transport, hotel, shopping |
| `survival/` | vocabulary | Getting help and being understood |

Modules are tones, vocabulary, listening, speech, and fundamentals, with conversation
and numbers planned (ADR 0007). Each level and module folder has a README that
describes its scope and sequence.

## Contents

| Folder | Lessons |
| --- | --- |
| `beginner/tones/` | First tone through tone pairs |
| `beginner/fundamentals/` | Syllable anatomy through tone sandhi |
| `beginner/vocabulary/` | Greetings, courtesy, numbers, time |
| `beginner/listening/` | Hear-the-tone and hear-the-word |
| `beginner/speech/` | Say-the-tone and say-the-word |
| `tourist/vocabulary/` | Food, directions, transport, hotel, shopping |
| `survival/vocabulary/` | Emergencies and repair phrases |

## What belongs here

- Lesson scripts and teaching sequences: the audio-first flow through one lesson's
  material.
- Word, phrase, tone, and pattern lists that belong to a single lesson.
- Lesson notes that record how the lesson is taught or how its reference audio is
  derived from pinyin at build time (ADR 0006).

## What does not belong here

- Standalone phrases, dialogues, and usage examples: `../examples/`.
- Reference material shared across lessons, such as grammar, vocabulary, and
  pronunciation notes: `../knowledge/`.
- Audio files of any kind: `../audio/`.
- File-format samples: `../samples/`.
- Generated practice items and learner data: runtime-only, never committed (ADR 0010).
- Product, architecture, and roadmap documents: `docs/`.
- Hanzi, and binary files of any kind.

## Naming

`<module>-<lesson-number>-<slug>.md`, for example `beginner/tones/tones-01-first-tone.md`.

- Module: the owning module's id, identical across all of its lessons.
- Lesson number: two digits, zero-padded from `01`. Numbers sequence a module across
  all levels, so the vocabulary module runs from its first beginner lesson through to
  its last survival lesson.
- Slug: short lowercase kebab-case derived from the lesson title.
- The file stem is the lesson id and is unique across the tree.

## Lesson format

- UTF-8 Markdown, opened by a level-1 sentence-case heading naming the lesson.
- Records the lesson's id, module, lesson number, level, and topic, matching the
  `Lesson` entity in `docs/02-architecture.md`.
- Lists the lesson's teaching items in order, as pinyin with tone numbers and
  meaning; no hanzi.
- Reference audio is generated at build time from the pinyin with Kokoro-82M and
  bundled under `assets/audio/` (ADR 0006).
- The lesson body teaches by ear; repetition belongs to practice.

Starter lesson content is illustrative and is reviewed by the author before it ships
(WF-6, ADR 0010). The exact schema is provisional until the content contract is
defined in M1 (`docs/04-roadmap.md`).
