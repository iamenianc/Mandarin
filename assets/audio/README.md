# Audio assets

Status: scaffold. This directory is created ahead of the first bundled clips; the
conventions below govern what is added.

Audio tracks that ship with the app: every sound the learner hears from bundled content.

## What belongs here

- **Reference audio** - clips for `ContentItem` phrases, words, minimal pairs, and
  dialogues, generated from pinyin with Kokoro-82M at build time (ADR 0006) and bundled
  so listening drills work offline.
- **Listening exercise tracks** - prompts and distractors used by the hear-and-identify
  and hear-and-respond drills.
- **Sample clips** - fixed audio such as the welcome screen's sample of what a session
  feels like (`docs/03-design.md`).

Learner recordings and runtime-cached AI audio do not belong here; they are local app
data, not repository assets (`docs/02-architecture.md`).

## File naming

- Name each file after the `ContentItem` id it serves, so `audioAssetRef` resolves
  directly: `<content-id>.ogg`.
- Use lowercase ASCII with hyphen-separated words; no spaces, underscores, or hanzi.
- Add a suffix for variants: `<content-id>-slow.ogg`, `<content-id>-<voice>.ogg`.
- Keep names stable once referenced; a rename requires updating every `audioAssetRef`
  and any precomputed evidence keyed to the clip.

## Preferred formats

| Use | Format | Notes |
| --- | --- | --- |
| Pipeline source | WAV, PCM, 24 kHz mono | Matches Kokoro-82M output (ADR 0006); resample only when required |
| Bundled playback | OGG Opus (`.ogg`), otherwise AAC in M4A | Compressed to keep the APK small; played with Media3/ExoPlayer (`docs/02-architecture.md`) |

- Keep one clip per phrase or prompt, with leading and trailing silence trimmed.
- Convert with `ffmpeg` in the build-time pipeline
  (`docs/10-libraries-and-dependencies.md`); only finished clips belong in this
  directory.
- Record the source and license of any human recording before adding it.
- Do not commit learner recordings, runtime-cached AI audio, or secrets.
