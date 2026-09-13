# Lessons

Lesson materials for the audio-first curriculum: the guided teaching each learning
module offers alongside its practice (ADR 0007, ADR 0008). One file per lesson.
Lessons are heard and spoken; Mandarin appears as pinyin with tone numbers and never
as hanzi (ADR 0011, ADR 0012).

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

## Organisation

Lessons are organised by module: tones, vocabulary, listening, speech, and
fundamentals, with conversation and numbers planned (ADR 0007). Within a module,
lessons follow teaching order, encoded by the zero-padded lesson number so files sort
into the module sequence. Level and topic are attributes recorded inside each lesson
file, not folder levels, so lessons can be listed by level or theme without moving
files.

## Naming

`<module>-<lesson-number>-<slug>.md`, for example `tones-01-first-tone.md`.

- Module: the owning module's id, identical across all of its lessons.
- Lesson number: two digits, zero-padded from `01`, giving the sequence within the
  module.
- Slug: short lowercase kebab-case derived from the lesson title.
- The file stem is the lesson id.

## File format

- UTF-8 Markdown, opened by a level-1 sentence-case heading naming the lesson.
- Records the lesson's module, lesson number, level, and topic, matching the
  `Lesson` entity in `docs/02-architecture.md`.
- Lists the lesson's teaching items in order, as pinyin with tone numbers and
  meaning; no hanzi.
- References audio in `../audio/` by relative path, sharing the file stem.
- The format is demonstrated in `../samples/`.

The exact schema is provisional until the content contract is defined in M1
(`docs/04-roadmap.md`).
