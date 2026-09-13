# Naming and formats

Conventions for every track under `assets/audio/`. See `schema/track.schema.json` for the
metadata shape and `manifest.yaml` for the category index.

## File naming

- A file stem is the id it serves, lowercased, with hyphens between words:
  `<content-id>.ogg` for a reference clip and `<drill-id>.ogg` for a drill track.
- Reference files sit under the module that owns the content:
  `reference/<module>/<content-id>.ogg`.
- The character set is ASCII `a-z`, `0-9`, and `-` only. No spaces, underscores,
  uppercase, diacritics, or hanzi. Tone numbers are ASCII digits and may appear in an id,
  but the canonical `pinyin` string also lives in metadata.
- Keep names stable once referenced. A rename requires updating every `audioAssetRef` and
  any precomputed evidence keyed to the clip.

## Variants

| Suffix | Meaning |
| --- | --- |
| `-slow` | Slowed playback of the same item, for early listening practice |
| `-<voice>` | The same item in an alternate voice, such as a second speaker in a dialogue |

## Formats

| Use | Format | Notes |
| --- | --- | --- |
| Pipeline source | WAV, PCM, 24 kHz mono | Matches Kokoro-82M output (ADR 0006); the format used for acoustic analysis |
| Bundled playback | OGG Opus (`.ogg`) | Preferred: small and supported by Media3/ExoPlayer |
| Bundled fallback | AAC in M4A (`.m4a`) | Used when an encoder constraint rules out Opus |

- One clip per item, at 24 kHz and mono, matching the generation output.
- Trim leading and trailing silence; keep natural inter-syllable pauses.
- Convert with `ffmpeg` in the build-time pipeline
  (`docs/10-libraries-and-dependencies.md`); the WAV source is never shipped.

## Provenance and determinism

Every generated clip records, in its manifest entry:

- `pinyin` with tone numbers as the synthesis input, plus optional `hangul`;
- the Kokoro voice id and pinned checkpoint;
- a `sourceHash` over the pinyin, voice, and checkpoint, so the same input maps to the
  same clip across rebuilds;
- the bundled `sha256` once the file exists.

A human recording, where used, records its origin and license in `provenance`.

## Validation checklist

1. Pinyin is present with tone numbers; no hanzi anywhere.
2. `sampleRate` is `24000` and `channels` is `1`.
3. The id matches the `ContentItem` or drill it serves and is unique in the manifest.
4. The file name matches the id and the recorded `format`.
5. `sha256` matches the bundled file.

## Worked example

`ni3 hao3`, documented in ADR 0012, as a reference entry:

```json
{
  "id": "vocab-ni3-hao3",
  "category": "reference",
  "module": "vocabulary",
  "contentType": "phrase",
  "source": "bundled",
  "file": "reference/vocabulary/vocab-ni3-hao3.ogg",
  "format": "ogg-opus",
  "sampleRate": 24000,
  "channels": 1,
  "pinyin": "ni3 hao3",
  "meaning": "hello",
  "targetTones": [3, 3]
}
```

The example is illustrative; the canonical field list is in `schema/README.md`.
