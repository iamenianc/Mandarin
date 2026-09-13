# Assets

Authored learning content for the app: example phrases and dialogues, lesson
material, reference knowledge, and the reference audio that accompanies them.
The app bundles these assets so listening and speaking practice works offline.

## Structure

| Path | Purpose |
| --- | --- |
| `examples/` | Example phrases, dialogues, and usage examples |
| `samples/` | Small sample materials that demonstrate the expected file formats |
| `lessons/` | Lesson materials |
| `knowledge/` | Reference knowledge: grammar, vocabulary, cultural notes |
| `audio/` | Reference audio tracks |

Each folder has its own README describing what belongs there and its file naming
conventions; follow the folder README when adding assets.

## Conventions

- Text assets are UTF-8 Markdown.
- File names are lowercase kebab-case.
- Mandarin text always appears with Hanyu Pinyin, with tones written as numbers
  (ADR 0011, ADR 0012); no asset contains hanzi (ADR 0002).
- A text asset and its companion audio share the same file stem.
