# Third-party attribution

Status: draft. Readable attribution list for the components recorded in
`docs/10-libraries-and-dependencies.md`. The machine-readable form is `licenses.json`;
full licence texts live in `licenses/`.

Licence values marked "to verify" are the expected licence for that dependency and must
be confirmed against the pinned artifact at M1. Components listed as options are not
shipped unless adopted.

## Shipped with the app

| Component | Use | Licence | Notes |
| --- | --- | --- | --- |
| AndroidX / Jetpack Compose | UI, lifecycle, navigation | Apache-2.0 (to verify) | `docs/10` |
| Media3 / ExoPlayer | Audio playback | Apache-2.0 (to verify) | `docs/10` |
| Room | Local database | Apache-2.0 (to verify) | `docs/10` |
| DataStore | Preferences | Apache-2.0 (to verify) | `docs/10` |
| Hilt / Dagger | Dependency injection | Apache-2.0 (to verify) | `docs/10` |
| Kotlin, kotlinx-coroutines, kotlinx-serialization | Language and runtime | Apache-2.0 (to verify) | `docs/10` |
| Ktor or OkHttp | HTTP and WebSocket | Apache-2.0 (to verify) | Option pending the M1 decision |
| Kokoro-82M reference clips | Bundled reference audio | Apache-2.0 | Generated at build time (ADR 0006) |
| openSMILE (option) | Tone evidence extraction | audEERING Research License | Private, non-commercial use only; blocks public or paid distribution (`docs/09`) |
| TarsosDSP (option) | Pitch tracking | GPL-family | Verify before any public distribution (`docs/10`) |

## Build-time only, not shipped

| Component | Use | Licence | Notes |
| --- | --- | --- | --- |
| Kokoro-82M and `misaki[zh]` | Reference-audio generation | Apache-2.0 | Checkpoints and voices pinned by hash (ADR 0006) |
| `pypinyin`, `cn2an`, `jieba` | G2P and number normalization for synthesis | to verify | Synthesis input only; no hanzi ships |
| `soundfile`, `numpy`, `librosa` | Audio I/O and analysis | to verify | |
| `ffmpeg` (optional) | Format conversion | to verify | |

## Worker-side and online services

| Service | Use | Terms |
| --- | --- | --- |
| Cloudflare Workers | API host | Cloudflare terms (`api/wrangler.jsonc`) |
| OpenRouter / Muse Spark | Multimodal and text models | Provider terms; the Worker holds the key (ADR 0003) |
| STT endpoint (WF-4) | Response transcription | Provider terms |

The app delegates provider selection to the Worker, and no provider credentials ship
(ADR 0003). Attribution for provider models is governed by the provider's terms rather
than an open-source licence.
