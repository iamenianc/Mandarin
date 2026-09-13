# Libraries and dependencies

Status: proposed. Inventory of third-party code the app and its build-time tooling are
expected to **import rather than write from scratch**, mapped to the stack in
`docs/02-architecture.md`. This records the choices and their constraints, not version
pins: the Android versions are fixed at M1 in a Gradle version catalog. ADR 0014 leaves
the tone extractor open; an ADR is added as each open decision closes.

## Principles

- Prefer established libraries and platform APIs over new code (AGENTS.md coding rules).
- One library per concern; avoid overlapping dependencies.
- Every dependency must fit the project's constraints: private single-user use,
  audio-first, cloud-only AI (ADR 0003, ADR 0014), no hanzi, Windows build host.
- Code that is genuinely a few dozen lines (DTW, tone comparison, WAV header) is written
  in-repo instead of depended on; see "Written in-repo".

Coordinates below are the expected Maven/Gradle artifact or package. "Option" means the
choice is not yet made and is tracked under "Open decisions".

## Android client

### Build and language

| Dependency | Purpose | Notes |
| --- | --- | --- |
| Kotlin stdlib | Language | |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Structured concurrency, ViewModel/Flow | |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | Worker payloads, evidence schema, content JSON | Kotlin serialization plugin |
| `org.jetbrains.kotlin.plugin.compose` | Compose compiler Gradle plugin | Required for Kotlin 2.0+; supersedes `androidx.compose.compiler:compiler` |
| `com.google.devtools.ksp` | Annotation processing | Backs Room and Hilt |

### UI

Compose artifacts are itemized in "Jetpack Compose inventory" below. The one non-Compose
UI dependency is microphone-permission UX:

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `com.google.accompanist:accompanist-permissions` | Microphone permission UX | Option; built-in activity result APIs may suffice |

### Persistence

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `androidx.room:room-runtime`, `room-ktx`, `room-compiler` | Local DB: lessons, attempts, progress, cached audio | SQLite; in-memory variant for tests |
| `androidx.datastore:datastore-preferences` | Prefs: goals, aid selection, consent flags | |

### Audio

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `androidx.media3:media3-exoplayer`, `media3-common` | Reference audio, learner playback, AI replies | Playback only; capture uses `AudioRecord` |
| `androidx.media3:media3-ui-compose` | Compose player controls (play/pause/seek, state) | Verified on Google Maven, 1.11.1 stable |
| `androidx.media3:media3-transformer` | PCM/WAV transcode for upload | Option; a WAV writer may replace it |
| WebRTC VAD via a maintained Android wrapper | End-of-speech detection | Option; DSP measurement, not on-device inference (ADR 0003) |
| `be.tarsos.dsp` TarsosDSP (or equivalent) | F0/YIN pitch tracking, filters | Option; GPL-family license - verify before any public distribution |
| openSMILE AAR (built from `progsrc/android-template`) | F0/voicing/loudness extraction for tone evidence | Option; audEERING Research License, private use only (docs/09, ADR 0014) |
| `com.microsoft.onnxruntime:onnxruntime-android` | Neural VAD (Silero) | Option; needs an ADR because ADR 0003 rejects on-device inference |

### Networking and AI plumbing

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `io.ktor:ktor-client-core`, `ktor-client-okhttp`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-websockets` | Worker HTTP + streaming conversation | Preferred; one library covers request/stream/JSON |
| `com.squareup.okhttp3:okhttp`, `okhttp-sse` | Alternative HTTP/WebSocket stack | Option; the engine under Ktor |
| `com.squareup.retrofit2:retrofit` | Alternative typed REST client | Option; only if Ktor is rejected |
| `com.networknt:json-schema-validator` (or Kotlin equivalent) | Validate WF output schemas before use | Option; guards WF-8/WF-9 generation |

### DI, async, logging

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `com.google.dagger:hilt-android`, `hilt-compiler` | DI; multibinding for the module registry | ADR 0007 |
| `androidx.hilt:hilt-navigation-compose` | Hilt-aware ViewModels in Compose | |
| `androidx.work:work-runtime-ktx` (+ `androidx.hilt:hilt-work`) | Background prefetch/generation, cleanup | Option; add only if a real background task exists |
| `com.jakewharton.timber:timber` | Logging | Option |
| Chucker, LeakCanary | Debugging HTTP and leaks | Debug-only, optional |

### Testing and tooling

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `junit:junit`, AndroidX `test.ext:junit`, `espresso-core`, Robolectric | Unit and instrumented tests | See testing strategy in docs/02 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | Deterministic coroutine tests | |
| `app.cash.turbine:turbine` | Flow assertions | |
| `io.mockk:mockk`, `com.google.truth:truth` | Mocking and assertions | |
| detekt, ktlint (or Spotless) | Lint and formatting | Required by M1; commands added to AGENTS.md |
| Gradle version catalog | Central dependency and version declarations | Built into Gradle |

## Jetpack Compose inventory

Verified 2026-09-13 against the Google Maven repository
(`https://dl.google.com/dl/android/maven2/`): the Compose BOM POM, per-artifact
`maven-metadata.xml`, and the AndroidX `master-index.xml`. Coordinates and versions below
are quoted from those files, not assumed. Re-verify before pinning at M1.

This section answers "which artifacts and versions". For "how to use them" - APIs,
patterns, state, layout, accessibility - the authority is the official Jetpack Compose
documentation, <https://developer.android.com/develop/ui/compose/documentation> (see
AGENTS.md).

### Managed by the Compose BOM

`androidx.compose:compose-bom` (latest stable 2026.09.00) is a version-alignment BOM; it
produces no code. Pinning it lets the individual Compose libraries be declared without
versions. The BOM's managed set maps to: Compose UI/foundation/animation/runtime
**1.12.1**, Material 3 **1.4.0**, Material 3 adaptive **1.3.0**, Material icons
**1.7.8**.

Recommended for this project:

| Artifact | Purpose | Notes |
| --- | --- | --- |
| `androidx.compose.ui:ui` | Core composables, layout, input | |
| `androidx.compose.ui:ui-graphics` | `Canvas`, drawing, vector graphics | Tone-contour and pitch visualizations |
| `androidx.compose.ui:ui-text` | Text layout, typography | Pinyin + English text; no hanzi |
| `androidx.compose.ui:ui-unit` | Dp/IntOffset/TextUnit types | Transitive in practice |
| `androidx.compose.foundation:foundation` | `Lazy*`, gestures, scrolling | |
| `androidx.compose.foundation:foundation-layout` | `Box`/`Row`/`Column`, constraints | |
| `androidx.compose.animation:animation`, `animation-core` | Transitions and motion | Feedback and drill transitions |
| `androidx.compose.animation:animation-graphics` | Animated vector graphics | Optional; reinforcement visuals |
| `androidx.compose.material3:material3` | Material 3 components | Card in docs/02 |
| `androidx.compose.material3:material3-window-size-class` | Window size classes | Optional; phone-first, may be unused |
| `androidx.compose.runtime:runtime` | Composition runtime | Pulled transitively |
| `androidx.compose.ui:ui-tooling-preview` | `@Preview` annotations | Debug/source only |
| `androidx.compose.ui:ui-tooling` | Preview and layout inspector | `debugImplementation` |
| `androidx.compose.ui:ui-tooling-data` | Preview data | `debugImplementation`, optional |
| `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` | Compose UI tests | `androidTestImplementation` |
| `androidx.compose.material:material-icons-core` | Baseline icon set | Prefer core; add extended only if needed |
| `androidx.compose.material:material-icons-extended` | Full Material icon set | Optional; large dependency, R8 shrinks unused icons |

Deliberately skipped from the BOM:

| Artifact | Why skipped |
| --- | --- |
| `androidx.compose.material:material` | Material 2; the app uses Material 3 |
| `material-ripple` | Transitive through Material 3 |
| `androidx.compose.runtime:runtime-livedata` | The app uses coroutines/Flow, not LiveData |
| `androidx.compose.runtime:runtime-rxjava2`, `runtime-rxjava3` | No RxJava |
| `androidx.compose.runtime:runtime-tracing` | Optional perf tracing; add only if a trace is needed |
| `androidx.compose.runtime:runtime-retain` | New retain API; not needed for a phone app |
| `androidx.compose.ui:ui-viewbinding` | No View interop planned |
| `androidx.compose.material:material-navigation` | Legacy Material 2 navigation |
| `androidx.compose.material3.adaptive:*` | Adaptive/foldable layouts; phone-first, revisit if needed |
| `androidx.compose.material3:material3-adaptive-navigation-suite` | Same; adaptive navigation for large screens |

### Compose-related artifacts outside the BOM

The BOM does not manage these; declare versions explicitly in the version catalog.

| Artifact | Coordinates | Version seen | Purpose |
| --- | --- | --- | --- |
| Compose host | `androidx.activity:activity-compose` | 1.13.0 stable | `setContent`, permission result APIs |
| Lifecycle state | `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 stable | `collectAsStateWithLifecycle` |
| Lifecycle ViewModel | `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 stable | `viewModel()` in Compose |
| Navigation | `androidx.navigation:navigation-compose` | 2.10.1 stable | Nav host and back stack |
| Hilt + Navigation | `androidx.hilt:hilt-navigation-compose` | 1.4.0 stable | `hiltViewModel()` in nav graphs |
| Media player UI | `androidx.media3:media3-ui-compose` | 1.11.1 stable | Compose controls bound to a Media3 `Player` |
| Constraint layout | `androidx.constraintlayout:constraintlayout-compose` | 1.1.2 stable | Optional; skip unless a screen needs it |
| Shapes | `androidx.graphics:graphics-shapes` | 1.1.0 stable | Optional; morphing shapes, not required |

Skipped Compose-family groups found in `master-index.xml`: `androidx.compose.remote*`
(projected/remote surfaces), `androidx.wear.compose*` (Wear OS), `androidx.xr.compose*`
(XR), `androidx.glance*` (app widgets), `androidx.paging:paging-compose` (no large
lists), `androidx.window` (large-screen sizing), `androidx.navigation3` and
`androidx.navigationevent` (next-gen navigation; track but do not adopt yet).

## Compose samples reference

Google's `android/compose-samples` repository is a first-party pattern source for Compose
UI. It is **reference material, not a project dependency**: the clone lives outside the
repository, nothing is imported or committed, and code is read for patterns rather than
copied (the samples carry their own `ASSETS_LICENSE` for fonts and images).

- Repository: <https://github.com/android/compose-samples>
- Cloned 2026-09-13 at commit `4c1fe75`, shallow, into
  `%LOCALAPPDATA%\Temp\kilo\compose-samples` (outside the repo).
- Re-clone with: `git clone --depth 1 https://github.com/android/compose-samples`.

### Sample-to-need mapping

| Sample | What it demonstrates | Use for this project |
| --- | --- | --- |
| **Jetcaster** | Podcast app: Media3 playback, Room, Hilt, Coil, WindowInsets, dynamic theming from artwork, Redux-style state, adaptive supporting pane | Primary reference for audio-first playback and the offline exercise DB. Files: `Jetcaster/.../util/DynamicTheming.kt`, `.../ui/home/Home.kt` |
| **Jetchat** | Chat UI: Material 3 theme, text input, animation, back handling, downloadable fonts | Reference for the Raymond chat surface. Files: `Jetchat/.../theme/Themes.kt`, `.../components/AnimatingFabContent.kt` |
| **JetNews** | Real-world architecture, navigation, Hilt, offline data, UI tests, Glance widget, window size classes | Reference for the shell/nav host, module launcher, and test layout. Files: `JetNews/.../ui/navigation/Navigator.kt`, `.../ui/interests/InterestsScreen.kt`, `.../ui/MainActivity.kt` |
| **Jetsnack** | Custom design system, custom layouts, animation, shared-element transitions | Reference for `:core:ui` design system and drill controls. Files: `Jetsnack/.../ui/theme/Theme.kt`, `.../ui/components/Grid.kt`, `.../ui/snackdetail/SnackDetail.kt` |
| **JetLagged** | Custom layouts and graphs drawn with `Canvas`/`Path` | Reference for tone-contour and pitch visualizations |
| **Reply** | Adaptive UI for phone/tablet/foldable, Material 3 theming, dynamic color, window size classes | Reference for M3 theming; adaptive layout is optional for a phone-first app. Files: `Reply/.../ui/theme/Theme.kt`, `.../ui/utils/WindowStateUtils.kt`, `.../ui/ReplyHomeViewModel.kt` |

### Dependency findings from the samples

The samples' `gradle/libs.versions.toml` files corroborate several choices and add a few
candidates. They are duplicated from a global catalog and lag the newest releases, so
Google Maven remains the version authority.

Corroborated as sane stable coordinates: `androidx.activity:activity-compose` 1.13.0,
`androidx.lifecycle:lifecycle-{runtime,viewmodel}-compose` 2.11.0,
`androidx.hilt:hilt-navigation-compose` 1.4.0, `androidx.room:room-*` 2.8.4,
`androidx.media3:media3-ui-compose` 1.11.0, `com.google.accompanist:accompanist-permissions`
0.37.3, Spotless 8.9.0.

Additions worth evaluating:

| Candidate | Purpose | Notes |
| --- | --- | --- |
| `org.jetbrains.kotlinx:kotlinx-collections-immutable` | Immutable collections for UI state | Supports the immutable-UI-state rule in docs/02 |
| `androidx.core:core-splashscreen` | Branded startup with the Compose theme | Optional quality-of-life |
| `io.github.takahirom.roborazzi:roborazzi`, `roborazzi-compose`, `roborazzi-junit-rule` | Screenshot/UI regression tests on the JVM | Optional; useful once drill screens stabilize |
| `com.google.android.material:material` | View-system Material theme backing the Compose theme | Only if an XML theme or splash needs it |

Not adopted, with reasons: `androidx.navigation3` and `lifecycle-viewmodel-navigation3`
(still alpha in the samples, matching the "track, do not adopt yet" note above);
`androidx.palette` (dynamic theming from artwork, and there is no artwork);
`com.google.android.horologist:*` (Wear audio UI), `androidx.wear.compose:*`,
`androidx.tv:*`, `com.google.maps.android:*`, `com.rometools:rome` (all sample-specific
targets, not this app's concerns).

## Build-time content pipeline

Runs on the Windows host to author bundled content and precompute reference evidence.
Not shipped in the APK.

| Dependency | Purpose | Notes |
| --- | --- | --- |
| Python | Pipeline language | |
| `kokoro`, `misaki[zh]` | Reference-audio TTS from pinyin | Kokoro Apache-2.0 (ADR 0006) |
| `pypinyin`, `cn2an`, `jieba` | Chinese G2P and number normalization for TTS text | Only for synthesis input; no hanzi ships |
| `soundfile`, `numpy`, `librosa` | Audio I/O, resampling, analysis | 24 kHz mono (ADR 0006) |
| `opensmile` (Python) | Reference F0/voicing/loudness precompute | audEERING Research License (docs/09) |
| `fastdtw` or `scipy` | DTW alignment in the spike | Prototype only; runtime DTW is written in-repo |
| `ffmpeg` | Format conversion | Optional |

## Cloudflare Worker (`api/`)

| Dependency | Purpose | Notes |
| --- | --- | --- |
| `wrangler` | Local dev and deploy | Already used by `api/package.json` |
| `hono` | Routing and middleware | Option; current `worker.js` routes by hand |
| `zod` | Request/response validation | Option; currently validated manually |
| OpenRouter via `fetch` | Provider access | OpenAI-compatible; no provider SDK or key in the APK (ADR 0003) |

## Written in-repo

Small, deterministic pieces kept as first-party code instead of dependencies:

- DTW alignment, speaker-relative normalization, and tone comparison (docs/09).
- WAV/PCM framing for upload if `media3-transformer` is not used.
- Pinyin tone-number parsing and display formatting (ADR 0012).
- The `ContentItem`/drill engine and the `LearningModule` registry contract (ADR 0007).
- Prompt assembly and the workflow-to-endpoint mapping glue (ADR 0009).

## Open decisions

- HTTP/streaming client: Ktor vs OkHttp/Retrofit.
- VAD: WebRTC (DSP) vs Silero (neural; would need an ADR exception to ADR 0003).
- Tone extractor: openSMILE vs TarsosDSP vs minimal Kotlin F0/loudness (M3 spike, ADR 0014).
- WF output schema validation: library vs hand-written parsers.
- Whether WorkManager, image loading, or debug-only tooling is needed at all.
- Exact Android artifact for the WebRTC VAD wrapper (to confirm during M1).
