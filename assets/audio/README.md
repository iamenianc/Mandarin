# Audio assets

Status: scaffold. The organisation, conventions, and manifests below are in place ahead
of the first generated clips.

Bundled audio for the app: every track that ships and plays offline, together with the
metadata that indexes it. Reference clips are generated at build time from pinyin
(ADR 0006) and bundled, so listening drills and repeat-after-audio work without a
network.

## Layout

| Path | Purpose |
| --- | --- |
| `naming-and-formats.md` | File naming rules and preferred audio formats |
| `manifest.yaml` | Index of every category manifest under this folder |
| `schema/` | JSON Schema and field reference for a track entry |
| `reference/` | One canonical reference clip per `ContentItem`, by module |
| `drills/` | Listening-exercise prompt and distractor tracks |
| `samples/` | Fixed app clips, such as the welcome sample |
| `voices/` | Voice configuration used to generate reference clips |
| `pipeline/` | How a track is generated, converted, indexed, and verified |

## Invariants

- Pinyin is always present and always uses tone numbers (`ni3 hao3`), never diacritics
  (ADR 0012).
- Hangul is optional and never required for a track to be valid (ADR 0004).
- No hanzi appears in a file name, a manifest, or track metadata (ADR 0002).
- A track is keyed by the id it serves: a reference clip uses its `ContentItem` id
  (`docs/02-architecture.md`).
- Audio is never gated on text; every track is playable without reading.

## What does not belong here

- Binary audio. This folder scaffolds the organisation; clips are produced by the
  build-time pipeline and are not committed until the pipeline exists.
- Learner recordings and runtime AI audio, which are local app data and never
  repository assets.
- Lesson, vocabulary, or knowledge content, which lives in the sibling asset folders.

## Map to the docs

| Topic | Source |
| --- | --- |
| Reference audio from pinyin, 24 kHz mono | ADR 0006 |
| Content model (`ContentItem`, `audioAssetRef`, `pinyin`, `hangul`, `targetTones`) | `docs/02-architecture.md` |
| Learning modules and curriculum | ADR 0007, ADR 0008 |
| Tone numbers | ADR 0012 |
| Speech synthesis (WF-3) and content authoring (WF-6) | `docs/08-ai-workflows.md` |
| Build-time audio dependencies | `docs/10-libraries-and-dependencies.md` |
