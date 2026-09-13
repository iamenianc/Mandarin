# Bundled content corpus

The curated, offline exercise set the app ships with (`source = bundled`, ADR 0010),
shaped by the data model in `docs/02-architecture.md` and the curriculum in ADR 0008.
Every module is data: a `Module`, ordered `Lesson` and `Practice` entries, and the
`ContentItem` objects they use. The app's drill engine drives all of it uniformly
(ADR 0007).

The corpus is derived from the authored material under `assets/lessons/`, `assets/knowledge/`,
and `assets/examples/`; it does not introduce a competing curriculum. Every lesson file
under `assets/lessons/` maps to one `Lesson` entry with several `ContentItem` objects, and
each module adds practice entries for repetition.

## Structure

| Path | Purpose |
| --- | --- |
| `index.json` | Top-level manifest: every module, its manifest paths, and its counts |
| `schema/content-item.schema.json` | Shape of one `ContentItem` |
| `schema/module.schema.json` | Shape of one `Module` |
| `schema/lesson.schema.json` | Shape of one `Lesson` or practice entry |
| `validate.mjs` | Dependency-free validator; run with `node assets/content/validate.mjs` |
| `tones/` | Tones module: `module.json`, `lessons.json`, `items.json`, `index.json` |
| `fundamentals/` | Fundamentals module, same four files |
| `vocabulary/` | Vocabulary module, same four files |
| `listening/` | Listening module, same four files |
| `speech/` | Speech module, same four files |

Each module folder holds exactly four files:

- `module.json` - the `Module`: `id`, `title`, `theme`, and ordered `lessonIds`.
- `lessons.json` - `{ "moduleId": ..., "lessons": [ ... ] }`, one object per lesson or
  practice entry, in module order.
- `items.json` - `{ "moduleId": ..., "items": [ ... ] }`, the module's `ContentItem`
  objects.
- `index.json` - the module manifest with the file names and the lesson and item counts.

A `Lesson` carries `id`, `moduleId`, `title`, `level`, `topic`, `kind`
(`lesson` or `practice`), and ordered `contentItemIds`. A `ContentItem` carries the
documented fields only: `id`, `type` (`word`, `phrase`, `minimalPair`, `dialogue`),
`source`, `meaning`, `pinyin`, `targetTones`, `audioAssetRef`, and optional `hangul` and
`turns`.

Practice is the same `Lesson` entity with `kind = "practice"`, so lessons and practice
share one engine and one schema, matching the `LearningModule` contract in
`docs/02-architecture.md`.

## Authoring rules

- No Han script appears in any file, id, or field (ADR 0002). A `hangul` field, when
  present, holds Hangul only (ADR 0004); it is omitted unless the mapping is genuinely
  correct.
- Pinyin is always present and carries a tone number 1-5 on every syllable; the neutral
  tone is `5`, as in `ma5` (ADR 0012). Tone numbers record the underlying tone, matching
  the pinyin authoring rule in `assets/knowledge/pronunciation/tone-numbers.md`; audio and
  feedback carry the surface form.
- `targetTones` lists one integer per pinyin syllable and repeats the tone numbers in
  `pinyin`.
- Meanings are English and are unique within a module, so a drill can present choices
  without an ambiguous gloss. The same meaning may appear in different modules, each with
  its own reference clip.
- An id is a globally unique lowercase kebab-case slug. Item ids are prefixed with their
  module (`tones-ma1`, `vocabulary-ni3-hao3`) so every id resolves across the corpus.
- `audioAssetRef` follows `assets/audio/naming-and-formats.md`: it is a path relative to
  `assets/audio/`, of the form `reference/<module>/<item-id>.(ogg|m4a|wav)`. Reference clips
  are generated at build time from the pinyin (ADR 0006) and owned by the audio workstream;
  the corpus records only the stable reference.
- The corpus is bundled, offline content: `source` is `bundled`. Generated items (WF-8)
  are runtime data and never committed (ADR 0010).

## Module coverage

| Module | Lessons and practice | Items | Lessons map to | Docs |
| --- | --- | --- | --- | --- |
| `tones` | 6 lessons + 4 practice | 30 | `assets/lessons/beginner/tones/` | FR-29, ADR 0012; contours, tone pairs, tone-number notation |
| `fundamentals` | 5 lessons + 6 practice | 53 | `assets/lessons/beginner/fundamentals/` | FR-20, FR-27, ADR 0011, ADR 0012; syllable anatomy, initials, finals, tone numbers, spelling conventions, tone sandhi |
| `vocabulary` | 11 lessons + 3 practice | 103 | `assets/lessons/{beginner,tourist,survival}/vocabulary/` | FR-21, ADR 0008; beginner, tourist, and survival themes |
| `listening` | 2 lessons + 2 practice | 13 | `assets/lessons/beginner/listening/` | FR-10; hear-the-tone and hear-the-word drills |
| `speech` | 2 lessons + 2 practice | 9 | `assets/lessons/beginner/speech/` | FR-19, WF-1, ADR 0005; say-the-tone and say-the-word drills |

The tones module leads and is the recommended starting point (ADR 0012). Modules are
registered through the contract in ADR 0007, so a new module is a new folder here plus a
thin feature module.

## Validation

`validate.mjs` is dependency-free Node (ESM). It scans every file for Han script, then
checks the JSON against the data model:

- required fields and known item types, levels, and kinds;
- pinyin present with tone numbers 1-5 on every syllable, matching `targetTones`;
- globally unique ids and unique meanings within a module;
- `audioAssetRef` matching `assets/audio/naming-and-formats.md` and the owning module;
- `module.lessonIds`, lesson `contentItemIds`, and the index manifests all consistent.

```
node assets/content/validate.mjs
```

The script prints a summary and exits zero on success; on failure it prints every error
and exits non-zero.

## Bundling into the APK

The corpus and the bundled audio ship inside the app. Gradle copies them into a
build-generated assets directory rather than committing a duplicate under `android/`.

| Task | Type | Purpose |
| --- | --- | --- |
| `:app:validateContent` | `Exec` | Runs `node assets/content/validate.mjs` from the repository root; a non-zero exit fails the build with the validator output. |
| `:app:syncContentAssets` | `Sync` | Copies the corpus and the bundled audio into `android/app/build/generated/contentAssets/`; it depends on `:app:validateContent`, so the copy is gated on validation passing. |

Wiring:

- `:app:preBuild` depends on `:app:validateContent`, so every build validates the corpus.
- The `main` source set adds `layout.buildDirectory.dir("generated/contentAssets")` as an
  asset directory, and the asset-merge tasks (`MergeSourceSetFolders`) depend on
  `:app:syncContentAssets`, so `:app:assembleDebug` and `:app:assembleRelease` sync the
  assets before packaging.
- The generated directory is build output under `android/app/build/` and is never
  committed.

### APK asset layout

| Repository path | APK asset path |
| --- | --- |
| `assets/content/**` | `assets/content/**` |
| `assets/audio/reference/**` | `assets/audio/reference/**` |
| `assets/audio/drills/**` | `assets/audio/drills/**` |
| `assets/audio/samples/**` | `assets/audio/samples/**` |

An item's `audioAssetRef` is relative to `assets/audio/`, so
`reference/tones/tones-ma1.wav` resolves at runtime to the APK asset
`assets/audio/reference/tones/tones-ma1.wav`.

## What does not belong here

- Han script in any form, and Hangul outside the optional `hangul` field.
- Generated exercises, learner attempts, transcripts, or recordings (ADR 0010).
- Audio files: reference clips live under `assets/audio/`.
- Lesson prose, knowledge notes, and examples: those stay in `assets/lessons/`,
  `assets/knowledge/`, and `assets/examples/`.
