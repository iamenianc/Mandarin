# Knowledge

Reference knowledge for the project: the notes and lists that lessons, examples, and AI
workflows draw from when authoring and generating Mandarin content. Files here are
source material for the project, not learner-facing screens.

## What belongs here

- Grammar notes: sentence structure, particles, word order, and usage patterns.
- Vocabulary lists: themed word and phrase lists with pinyin and tone numbers.
- Pronunciation notes: tones, tone sandhi, syllable anatomy (initial / final / tone),
  pinyin spelling conventions, and rhythm.
- Cultural notes: etiquette, customs, and context that shape natural spoken exchanges.

## What does not belong here

- Guided teaching material and lesson scripts: `assets/lessons/`.
- Practice items and example sentences: `assets/examples/`.
- Audio files of any kind: `assets/audio/`.
- Product, architecture, and roadmap documents: `docs/`.
- Binary files of any kind.

## Naming conventions

- Markdown only, with the `.md` extension.
- Lowercase kebab-case filenames: `tone-sandhi.md`, `food-vocabulary.md`.
- Start names with a category label so files sort and scan by kind: `grammar-…`,
  `vocab-…`, `pronunciation-…`, `culture-…`.
- One logical topic per file, per `AGENTS.md`.
- Treat filenames as stable references: rename only when the topic itself changes.

## File format

- Open with a single `#` heading naming the topic, in sentence case.
- Follow the heading with a short summary of what the file covers.
- Keep pinyin and tones consistent with the app: tones are written as numbers (ADR 0012).
