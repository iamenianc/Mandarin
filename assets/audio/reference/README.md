# Reference clips

Canonical audio for each `ContentItem`, generated at build time from its pinyin with
Kokoro-82M and bundled so drills work offline (ADR 0006). A reference clip is the
listening target for a drill and the comparison baseline for pronunciation feedback
(WF-1, ADR 0005).

## Layout

| Path | Module |
| --- | --- |
| `tones/` | Tone units, contours, and tone pairs |
| `vocabulary/` | Themed words and phrases |
| `listening/` | Items played by hear-and-identify and hear-and-respond drills |
| `speech/` | Phrases compared against a learner attempt |
| `fundamentals/` | Syllables, minimal pairs, sandhi, and read-aloud items |

Each module folder holds its own `tracks.yaml`; `../manifest.yaml` indexes them all.

## Rules

- One canonical clip per `ContentItem`, named after its id (`../naming-and-formats.md`).
- Every entry carries `pinyin` with tone numbers; `hangul` is optional; no hanzi
  (ADR 0002, ADR 0004, ADR 0012).
- `contentType` uses the shared discriminator: `phrase`, `word`, `minimalPair`, or
  `dialogue` (`docs/02-architecture.md`).
- Clips are 24 kHz mono (ADR 0006) and bundled as OGG Opus for playback.
- Slow variants use the `-slow` suffix; alternate voices use the `-<voice>` suffix.
