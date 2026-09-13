# Content-build pipeline

How a track moves from authored content to a bundled clip. The pipeline runs on the
Windows host and is not shipped in the APK
(`docs/10-libraries-and-dependencies.md`).

## Steps

1. **Author.** WF-6 drafts lesson content for human review; reviewed content becomes an
   authored `ContentItem` with `pinyin`, optional `hangul`, `meaning`, and
   `targetTones` (`docs/08-ai-workflows.md`).
2. **Synthesize.** WF-3/Kokoro-82M generates a 24 kHz mono clip from the item's pinyin
   with the pinned voice (ADR 0006).
3. **Trim and convert.** Trim leading and trailing silence, then convert the WAV source
   to OGG Opus with `ffmpeg` (`../naming-and-formats.md`).
4. **Analyze.** Precompute the acoustic evidence referenced by `evidenceRef`, keyed to
   the clip (ADR 0014, `docs/09-opensmile-acoustic-features.md`).
5. **Index.** Add the entry to the module manifest and record `sourceHash` and `sha256`
   (`../schema/track.schema.json`).
6. **Validate.** Check every entry against the schema and the naming rules before
   bundling.

## Not in this pipeline

- Runtime AI replies, which WF-3 synthesizes on demand and caches locally; they are
  never committed here.
- Learner recordings, which stay on device.
