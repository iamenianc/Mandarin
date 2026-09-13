# Knowledge

Reference knowledge for the project: grammar notes, vocabulary lists, pronunciation
notes, and cultural notes. These files are authoring source material for lessons,
examples, AI prompts, and generated content. They are not learner-facing screens.

## Organization

| Folder | Contents |
| --- | --- |
| `pronunciation/` | Tones, syllable anatomy, the pinyin sound system, tone sandhi, rhythm |
| `grammar/` | Sentence patterns and the particles that hold them together |
| `vocabulary/` | Themed word and phrase lists for beginner, tourist, and survival Mandarin |
| `culture/` | Etiquette, customs, and context behind natural spoken exchanges |

Each folder has its own `README.md` index.

## Conventions

- Markdown only, with the `.md` extension, encoded as UTF-8.
- Lowercase kebab-case filenames: `tone-sandhi.md`, `food-and-drink.md`.
- One logical topic per file, per `AGENTS.md`.
- Sentence-case headings, and no first person (`I`, `we`, `my`).
- **Pinyin with tone numbers everywhere**, matching the app: each syllable carries its
  tone number after it (`ni3 hao3`), and the neutral tone is `5` (`ma5`) (ADR 0012).
- **No hanzi.** Every entry pairs pinyin with an English meaning, per the no-hanzi
  product invariant (ADR 0002). Hangul is an optional phonetic aid in the app and is not
  authored here.
- Tone numbers record the underlying tone. Where tone sandhi changes the spoken pitch,
  the note says so rather than rewriting the pinyin (see `pronunciation/tone-sandhi.md`).
- Filenames are stable references: rename only when the topic itself changes.

## Non-text resources

Diagrams live beside the notes they support and use the same kebab-case naming with an
`.svg` extension (`pronunciation/tone-contours.svg`). They are authored in this repository
with plain SVG text, carry no embedded raster images, and label pinyin with tone numbers.

No audio is authored here. Reference audio is generated from pinyin with Kokoro-82M at
build time and belongs to the content pipeline and `assets/audio/` (ADR 0006); knowledge
files cite the phrases that audio should cover rather than shipping clips.

## Sources

Content follows the curriculum themes in ADR 0008 and the pinyin and tone decisions in
ADR 0011 and ADR 0012. Examples are standard beginner Mandarin and are intended to be
checked by ear against reference audio; this folder is not a substitute for
native-speaker review.
