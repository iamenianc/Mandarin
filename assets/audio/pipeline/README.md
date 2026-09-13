# Content-build pipeline

How a track moves from authored content to a bundled clip. The pipeline runs on the
Windows host and is not shipped in the APK
(`docs/10-libraries-and-dependencies.md`).

## Steps

1. **Author.** WF-6 drafts lesson content for human review; reviewed content becomes an
   authored `ContentItem` with `pinyin`, optional `hangul`, `meaning`, and
   `targetTones` (`docs/08-ai-workflows.md`).
2. **Synthesize.** WF-3/Kokoro-82M generates a 24 kHz mono clip from the item's pinyin
   with the pinned voice (ADR 0006). `generate_reference_audio.py` performs this step.
3. **Trim and convert.** Trim leading and trailing silence, then convert the WAV source
   to OGG Opus with `ffmpeg` (`../naming-and-formats.md`).
4. **Analyze.** Precompute the acoustic evidence referenced by `evidenceRef`, keyed to
   the clip (ADR 0014, `docs/09-opensmile-acoustic-features.md`).
5. **Index.** Add the entry to the module manifest and record `sourceHash` and `sha256`
   (`../schema/track.schema.json`).
6. **Validate.** Check every entry against the schema and the naming rules before
   bundling.

## Reference-audio generator

`generate_reference_audio.py` reads a content manifest, plans one deterministic clip per
`ContentItem`, validates the pinyin and tone numbers, and synthesizes 24 kHz mono WAV
clips with a pinned Kokoro Mandarin voice. Kokoro is imported lazily, so `--dry-run` and
`--selftest` work offline and never touch the model.

### Setup

- Python 3.12 (the version on the build host).
- `pip install pyyaml` - manifest reading and writing.
- `pip install kokoro misaki[zh]` - Kokoro-82M and the Mandarin G2P. This pulls the
  PyTorch runtime and downloads `hexgrad/Kokoro-82M` on first use. Run it in a virtual
  environment if the host Python should stay clean.

### Content manifest

The generator reads `assets/content/**` by default, or any path passed with
`--manifest` (a file or a directory, repeatable). Accepted shapes:

- `{ "module": "<module>", "items": [ { ... }, ... ] }` - preferred;
- `[ { ... }, ... ]` - each item declares its own `module`;
- a single item object (an object with `pinyin` or `turns`).

An item uses the `ContentItem` fields (`docs/02-architecture.md`): `id`, `type`,
`pinyin` (tone numbers, neutral tone `5`), optional `meaning` and `hangul`, and
`targetTones`. Ids are lowercased and hyphenated; the file stem is the id. A `module`
comes from the item, its container, the path under the content root, or `--module`.
Dialogue items are voiced as one clip from their turns' pinyin, in order.

`pipeline/fixtures/` holds two example manifests: `content.sample.json` (vocabulary) and
`content.tones.json` (the five isolated tones, `ma1`-`ma5`). They validate the reader and
the tone path; they are fixtures, not app content.

### Usage

```
python assets/audio/pipeline/generate_reference_audio.py --selftest
python assets/audio/pipeline/generate_reference_audio.py --dry-run --manifest assets/audio/pipeline/fixtures
python assets/audio/pipeline/generate_reference_audio.py --generate --manifest assets/audio/pipeline/fixtures/content.tones.json
```

- `--dry-run` validates the pinyin inputs, checks the planned entries against
  `schema/track.schema.json`, computes every output path, and prints the plan. With no
  content manifest it reports zero clips and exits successfully.
- `--selftest` checks naming, the committed manifests against `track.schema.json`, the
  voice registry, and the 24 kHz mono expectation. It also reads the WAV header of every
  committed placeholder. No network and no model.
- `--generate` synthesizes the planned clips. `--limit N` bounds a smoke test; `--out`
  moves the output directory; `--voice` and `--checkpoint` override the registry.

### Output layout

| Path | Committed | Purpose |
| --- | --- | --- |
| `generated/<module>/<id>.wav` | No | Kokoro source clip, 24 kHz mono 16-bit PCM |
| `generated/tracks.yaml` | No | Manifest of generated clips, schema-valid, sorted by id |

`assets/audio/.gitignore` excludes `generated/`. Generated audio is never committed in
bulk; only scripts, manifests, docs, and fixtures are. Publishing a clip to
`reference/<module>/<id>.ogg` is a deliberate, reviewed step that also records `sha256`
in the module manifest.

The canonical target path and file stem follow `../naming-and-formats.md`:
`reference/<module>/<id>.ogg`, with the id lowercased and hyphenated.

### Voices and determinism

The pinned voice and checkpoint come from `../voices/registry.yaml`
(`defaultVoice`, `checkpoint`); a selected voice must be listed under `voices`. The
`sourceHash` is a SHA-256 over the engine, language code, voice, checkpoint, and pinyin,
so the same input always maps to the same source. `sha256` records the bytes of the
generated file.

### Environment caveat

`--selftest` and `--dry-run` need only Python and PyYAML. A real `--generate` run needs
the Kokoro package, the PyTorch runtime, and a one-time model download; without them the
script exits with `pipeline unavailable` and states the missing pieces. The placeholder
tone contours remain in `reference/tones/` until a generation run is validated and
published.

## Placeholder generation

No Mandarin TTS voice is available in the current environment, so
`generate-placeholder-tones.py` writes five synthetic tone-contour WAVs under
`../reference/tones/`. The script is deterministic, stdlib-only, and each generated file
is recorded with `placeholder: true` in `../reference/tones/tracks.yaml`. Replace the
files with Kokoro reference clips (ADR 0006) and delete the script once real synthesis is
possible.

## Not in this pipeline

- Runtime AI replies, which WF-3 synthesizes on demand and caches locally; they are
  never committed here.
- Learner recordings, which stay on device.
