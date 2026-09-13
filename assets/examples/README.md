# Examples

Example phrases, dialogues, and usage examples that show how vocabulary and
grammar patterns are used in context. Entries are standalone teaching material;
material tied to a single lesson belongs in `../lessons/`, and reusable drill
items that carry ids and audio belong in the app's curated content set.

## Structure

| Path | Purpose |
| --- | --- |
| `index.md` | Themed index of every example in this folder |
| `index.json` | Machine-readable index for authoring and review tools |
| `<theme>-<slug>.md` | Phrase sets, grouped by curriculum theme |
| `<topic>-<slug>-dialogue.md` | Dialogues with speaker turns |
| `fundamentals-<slug>.md` | Sound-system examples: tones, tone pairs, syllables |

Phrase sets cover the themes in ADR 0008. Sound-system examples cover the tone
notation in ADR 0012 and the syllable anatomy in ADR 0008.

## Naming

`<topic>-<slug>.md`, for example `greetings-introduction-dialogue.md`. Related
files share a topic prefix. Notes about the level or lesson an example supports
belong in the file, not in the file name. A companion recording, when present,
lives under `../audio/` and shares the file stem.

## Format

A phrase set is a Markdown table with one row per example:

| pinyin | meaning | note |
| --- | --- | --- |
| `ni3 hao3` | hello | Works at any time of day. |

A dialogue lists speaker turns in order:

| speaker | pinyin | meaning |
| --- | --- | --- |
| A | `ni3 hao3` | hello |

- Mandarin text appears as Hanyu Pinyin with tone numbers; no hanzi appears
  (ADR 0002, ADR 0012).
- Tone numbers record the spoken surface tone, including common tone sandhi
  (`ni2 hao3`, `bu2 yao4`, `yi4 bei1`).
- Hangul is an optional display aid shown alongside pinyin and is not tone-marked
  (ADR 0004).
- Diagrams and other non-text files share the file stem of the example they
  support; `fundamentals-tone-contours.svg` is hand-authored and uses no external
  assets.
- Examples carry no audio of their own. Reference clips are generated with
  Kokoro-82M and live in `../audio/` (ADR 0006).

## Related

- `../lessons/` holds material tied to a single lesson.
- `../samples/` holds format references, not teaching content.
- `../knowledge/` holds grammar and cultural notes that explain the patterns the
  examples use.
