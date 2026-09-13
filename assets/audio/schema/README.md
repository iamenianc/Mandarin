# Track schema

Every track entry in a `tracks.yaml` manifest satisfies `track.schema.json`
(JSON Schema draft 2020-12). The build-time pipeline validates entries before bundling;
the Android build reads manifests at runtime, not the schema.

## Fields

| Field | Required | Notes |
| --- | --- | --- |
| `id` | Yes | Lowercase hyphenated slug; matches the file stem and the served item or drill id |
| `category` | Yes | `reference`, `drill`, or `sample` |
| `module` | Per category | Owning module, or `app` for a fixed sample clip |
| `contentType` | Reference (lexical) | `phrase`, `word`, `minimalPair`, or `dialogue`; omitted for tone contours |
| `source` | Reference | `bundled` or `generated` |
| `file` | Yes | Path relative to `assets/audio/` |
| `format` | Yes | `ogg-opus`, `m4a-aac`, or `wav-pcm` |
| `sampleRate` | Yes | Always `24000` (ADR 0006) |
| `channels` | Yes | Always `1` |
| `durationMs` | No | Measured clip length |
| `pinyin` | Speech | Tone numbers, for example `ni3 hao3`; never diacritics or hanzi (ADR 0012) |
| `hangul` | No | Optional phonetic aid (ADR 0004) |
| `meaning` | No | English gloss |
| `targetTones` | No | Tone number per syllable, `1`-`5` |
| `voice` | No | Convenience copy of `tts.voice` |
| `tts` | Generated | Engine, language code, pinned voice, and checkpoint |
| `sourceHash` | Generated | Hash over pinyin, voice, and checkpoint |
| `sha256` | Bundled | Content hash of the committed clip |
| `provenance` | Recorded or placeholder | `origin` is `generated`, `recorded`, or `placeholder`; license and note as applicable |
| `placeholder` | Placeholder | `true` marks a stand-in that must be replaced by real reference audio |
| `evidenceRef` | No | Precomputed acoustic evidence for the clip (ADR 0014) |
| `syllableBoundariesMs` | No | Syllable offsets used in DTW alignment |
| `notes` | No | Free-form author note |

## Example

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
  "durationMs": 1200,
  "pinyin": "ni3 hao3",
  "meaning": "hello",
  "targetTones": [3, 3],
  "voice": "zf_xiaobei",
  "tts": {
    "engine": "kokoro-82m",
    "langCode": "z",
    "voice": "zf_xiaobei",
    "checkpoint": "<pinned-revision>"
  },
  "sourceHash": "<hash>"
}
```

The voice id is an example drawn from the pinned Kokoro Mandarin set; a real entry
records whichever voice the clip was generated with.
