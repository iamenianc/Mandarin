# Architecture

Status: proposed. Decisions that affect this document should be captured as ADRs
in `docs/05-decisions/` and linked here.

## Guiding principles

- **Audio-first, visually supported.** Audio drives the lesson; visuals reinforce it.
  There is no hanzi rendering, no handwriting, and no character-based navigation - all
  on-screen text is English or pinyin. Pinyin is the taught pronunciation layer
  (ADR 0011).
- **Fast feedback over exhaustive analysis.** Specific, encouraging coaching in ~1-2s
  beats a perfect score in 10s.
- **Cloud-only AI.** No on-device inference - local compute is insufficient. Listening
  drills and repeat-after-audio still work offline with bundled content. Deterministic
  on-device DSP for tone evidence (WF-1) is measurement, not inference, and is allowed
  (ADR 0014).
- **Single user.** Built for one person (the author); no accounts or profiles, no
  analytics.
- **Modular by construction.** Learning areas are feature modules behind one contract, so
  new modules are additive (ADR 0007).
- **Practice that transfers.** Daily practice centers on the LAMP field loop - rehearse
  one exchange, use it with five simulated locals, debrief - so drills feed real use
  (ADR 0013).

## Proposed stack

| Concern | Choice | Notes |
| --- | --- | --- |
| Language | Kotlin | |
| UI | Jetpack Compose | Declarative UI, Material 3; English + pinyin text only |
| Architecture | MVVM + unidirectional data flow | ViewModel + immutable UI state |
| Audio playback | Media3 / ExoPlayer | Reference audio, learner playback, AI replies |
| Audio capture | `AudioRecord` + raw PCM | Clipped per attempt; VAD for end-of-speech |
| Reference audio TTS | Kokoro-82M (Apache-2.0) | Build-time generation from pinyin; 24 kHz mono (ADR 0006) |
| Speech feedback (WF-1) | Worker-proxied `meta/muse-spark-1.3-contributor` | Reference + attempt audio + measured tone evidence in, coaching out (ADR 0005, ADR 0014) |
| Tone evidence (WF-1 input) | On-device DSP: F0/voicing/loudness + DTW alignment + tone comparison | Deterministic per-syllable observations; extractor chosen in the M3 spike (ADR 0014) |
| AI conversation (WF-2, WF-10) + speech synthesis (WF-3) | Worker-proxied Muse Spark + Kokoro TTS | Speech in -> understanding + reply text -> spoken reply; WF-10 keeps locals in character |
| Listening response (WF-4) | Worker-proxied STT endpoint | Transcript only; local matcher decides the answer |
| AI workflows | `:core:ai` typed workflows behind one Worker endpoint each | Separate prompt/schema/budget per task (ADR 0009) |
| Backend | Cloudflare Worker (`api/`) | Hides provider keys, rate limits, streams audio |
| Local DB | Room (SQLite) | Lessons, attempts, progress, cached audio |
| Prefs | DataStore | Goals, aid selection, consent flags |
| DI | Hilt | Modules registered by multibinding |
| Build | Gradle (Kotlin DSL) + version catalog | Multi-module; one feature module per learning module |

## Layering

```
UI (Compose)  ->  ViewModel  ->  Use case  ->  Repository
                                                   |-> Room / DataStore / assets (offline)
                                                   |-> Audio/AI clients -> Worker -> Muse Spark / Kokoro (online)
```

- **UI** renders immutable state and captures microphone/playback intents.
- **ViewModel** owns session state and turn-taking.
- **Use cases** hold the logic that matters: assessing an attempt, selecting the next
  drill, running a conversation turn.
- **Repositories** abstract local content vs. remote AI services.

## Learning modules

The app is built as a Gradle multi-module project so new learning areas are additive
(ADR 0007). Every module offers **lessons** (guided teaching) and **practice** (reps),
and is discovered at runtime through a small contract in `:core:model`.

```
:app                shell, nav host, DI wiring, module registry
:core:model         LessonSpec, ContentItem, Attempt, Progress, LearningModule
:core:audio         Media3 playback, AudioRecord capture, VAD
:core:data          Room + DataStore repositories
:core:ai            AI workflow interfaces + schemas (WF-1…WF-10, ADR 0009)
:core:assessment    Tone-evidence pipeline (extract, align, normalize, compare) + WF-1 client (ADR 0005, ADR 0014)
:core:ui            Compose design system, shared drill controls
:feature:home       module launcher + today's LAMP loop
:feature:tones      tone lessons + practice (beginner-first)
:feature:vocabulary vocab lessons + practice
:feature:listening  listening lessons + practice
:feature:speech     speech/pronunciation lessons + practice
:feature:fundamentals fundamentals lessons + practice
:feature:field      daily LAMP field loop (script, mission, debrief; WF-9, WF-10)
:feature:raymond    Raymond: ask-anything Mandarin Q&A (WF-7)
(future)            :feature:conversation, :feature:numbers
```

```kotlin
interface LearningModule {
    val id: String
    val title: String
    fun lessons(): List<LessonSpec>      // guided teaching
    fun practices(): List<PracticeSpec>  // reps
}
```

- Modules are contributed by Hilt multibinding; the shell renders whatever the registry
  exposes, so adding a module needs no shell or navigation changes.
- **Lessons and practice share one content/drill engine.** `ContentItem` carries a type
  discriminator (`phrase`, `word`, `minimalPair`, `dialogue`) so new item types extend
  content without new engines.
- **Two content sources** (ADR 0010): a curated set is **bundled** and works offline; WF-8
  can **generate** extra items at runtime. `ContentItem.source` (`bundled` / `generated`)
  lets the UI label and the drill engine treat them uniformly.
- Feature modules consume `:core:audio` and the `:core:ai` workflows; they never call
  providers directly. Content is authored as data with bundled Kokoro reference audio
  (ADR 0006).
- **Raymond** (`:feature:raymond`) is a feature module but **not** a `LearningModule`: it
  ships no lessons or practice and instead provides a persistent "ask anything about
  Mandarin" surface backed by WF-7.
- **The daily LAMP field loop** (`:feature:field`, ADR 0013) is also a feature module but
  **not** a `LearningModule`: it ships no lesson content of its own and orchestrates
  practice across modules - a generated script, a mission with five simulated locals
  (WF-9, WF-10, WF-3), and a debrief whose themes feed progress and later generation.

## AI workflows

Each AI task is a **separate, named workflow** with its own prompt, model, typed input,
fixed output schema, latency budget, and fallback (ADR 0009). Workflows are implemented in
`:core:ai` as typed interfaces; feature modules depend on a workflow, never on a provider.
The Worker exposes **one versioned endpoint per workflow**, owns provider selection, and
keeps prompts server-side (ADR 0003). The full registry is in `docs/08-ai-workflows.md`.

| ID | Workflow | Model | Serves |
| --- | --- | --- | --- |
| WF-1 | Pronunciation feedback | Muse Spark | Speech, Tones, Vocabulary |
| WF-2 | Conversation turn | Muse Spark | Conversation |
| WF-3 | Speech synthesis | Kokoro-82M | All audio |
| WF-4 | Response transcription | STT endpoint | Listening |
| WF-5 | Progress summary | Muse Spark (text only) | Progress |
| WF-6 | Content authoring (build-time) | Text LLM | Authoring (not shipped) |
| WF-7 | Raymond - Mandarin Q&A | Muse Spark | Raymond |
| WF-8 | Exercise generation (runtime) | Muse Spark (text only) | Tones, Vocabulary, Listening, Fundamentals |
| WF-9 | Field mission generation | Muse Spark | Field loop |
| WF-10 | Local conversation turn | Muse Spark (audio in) | Field loop |

Rules: no shared prompts; minimum necessary input (raw learner audio goes only to WF-1,
WF-2, WF-4, WF-10, and spoken questions to WF-7; WF-5, WF-8, and WF-9 get aggregated
metadata and content context only); each workflow fails and falls back independently, so
an unavailable model degrades one feature rather than the app.

## Audio and AI pipeline

### Listening drill (WF-4 for spoken answers)
```
bundled audio -> Media3 -> learner response (tap / speak) -> local check -> next
                                   \-> WF-4 response transcription (speak only)
```

### Speak and repeat - pronunciation (WF-1)
```
mic -> VAD + PCM buffer -> on-device evidence (F0/voicing/loudness -> DTW align -> normalize -> tone compare)
                                     \-> Worker /v1/wf/pronunciation-feedback -> Muse Spark (reference + attempt + evidence) -> coaching text -> UI
```

### AI conversation turn (WF-2 + WF-3)
```
mic -> VAD -> Worker /v1/wf/conversation-turn -> Muse Spark (understand + reply text)
                                                        |
                                              WF-3 speech synthesis -> playback
                    ^                                     |
                    +--------- conversation state ---------+
```

### Field mission (WF-9 + WF-10 + WF-3)
```
WF-9 mission generation -> script + five locals -> rehearsal and mission state
mic -> VAD -> Worker /v1/wf/local-turn -> Muse Spark (persona + mission state) -> reply text
                                                       |
                                             WF-3 speech synthesis -> playback
```

The Worker is the trust boundary: provider API keys never ship in the APK, and the
Worker enforces rate limits and payload sizes per workflow. Streaming (WebSocket) is
preferred for conversation to hit the latency targets in `docs/01-requirements.md`.

### Audio handling

Pronunciation feedback and conversation have **no separate ASR stage** (WF-1, WF-2): the
multimodal model transcribes and reasons about the audio in one call. The one exception is
the listening drill's spoken answer, which uses the transcription workflow (WF-4) and a
local matcher. For pronunciation feedback the app sends the reference clip and the
learner's attempt (two base64 `input_audio` parts) with a structured prompt and receives
concise coaching. See ADR 0005, ADR 0009, and `docs/07-speech-assessment-models.md`. This
is soft, non-deterministic feedback - not a score - and is accepted by product decision.

WF-1 is grounded by measured evidence (ADR 0014): a deterministic on-device step extracts
F0, voicing, and loudness, aligns the attempt to the reference by DTW, normalizes to a
speaker-relative scale, and compares each syllable against its expected tone. Only named
per-syllable observations are sent to the model - never raw contours - and reference
features are precomputed at content-build time. This is DSP, not on-device AI inference
(ADR 0003). If extraction fails, the request degrades to the audio-only form, so the
soft-feedback behavior above still holds. See `docs/09-opensmile-acoustic-features.md`.

Reference audio is generated with Kokoro-82M (ADR 0006) at build time from the phrase's
pinyin and bundled; Kokoro is also the TTS voice for AI replies (WF-3).

## Data model (draft)

| Entity | Key fields |
| --- | --- |
| `Module` | id, title, theme, ordered `Lesson` ids |
| `Lesson` | id, moduleId, title, level, topic, ordered `ContentItem` ids |
| `ContentItem` | id, type (`phrase` / `word` / `minimalPair` / `dialogue`), source (`bundled` / `generated`), meaning, audioAssetRef, pinyin (tone numbers), hangul (optional), targetTones |
| `Attempt` | id, contentItemId, recordedAt, userAudioRef, transcript, feedbackText |
| `ConversationSession` | id, lessonId, startedAt, endedAt, turnCount, summary |
| `Turn` | id, sessionId, speaker (user/ai), audioRef, transcript, feedback |
| `FieldMission` | id, date, theme, script turns (pinyin + meaning + targetTones), source (`bundled` / `generated`), ordered `LocalPersona` ids |
| `LocalPersona` | id, label, settingRole, personality, voiceProfile, pace |
| `MissionSession` | id, missionId, personaId, startedAt, endedAt, turnCount, outcome |
| `MissionTurn` | id, sessionId, speaker (learner / local), audioRef, transcript |
| `DebriefEntry` | id, missionId, pinyin, kind (`unfamiliarWord` / `toneBreakdown`), note, createdAt |
| `RaymondMessage` | id, role (user/raymond), text, audioRef (optional), createdAt |
| `Progress` | contentItemId, timesPracticed, lastPracticedAt, feedbackThemes |

Records by internal id and audio. **No hanzi field.** Phonetic aids are first-class
display fields - `pinyin` (always shown) and optional `hangul` - and they exist to teach
pronunciation: pinyin is the taught sound map (ADR 0011), never drilled as hanzi reading.
Pinyin strings carry tone numbers (ADR 0012). Zhuyin is out of scope.
`ContentItem` is the shared unit across all modules, so a lesson or drill in any module
is built from the same content and drill engine (ADR 0007, ADR 0008). Debrief entries
carry pinyin with tone numbers (ADR 0012) and no hanzi; raw notes stay on device, and only
aggregated themes reach WF-5 and WF-9.

## Content and consent

- The curated exercise set is **bundled**, so listening drills and repeat-after-audio play
  without a network (ADR 0010).
- **Generated** exercises (WF-8) extend or continue practice on demand, are validated, are
  labeled as extra, and may be cached locally; without a network the learner falls back to
  the bundled set.
- Reference audio is bundled for curated items; generated items are voiced via WF-3.
- Recordings are stored locally and sent to the AI provider only to service a request
  (Muse Spark). Consent is explicit and revocable.
- Debrief entries and mission transcripts stay local; only aggregated debrief themes are
  sent to WF-5 and WF-9, and mission audio only to WF-10.
- Provide one-tap delete for recordings, transcripts, and cached AI audio.

## Testing strategy

- Unit tests for drill selection, feedback-schema parsing, and prompt assembly.
- Mission flow tests: five-local progression, interruption/resume, and debrief
  persistence.
- Repository tests against an in-memory Room database.
- Instrumented tests for the mic/playback lifecycle (permissions, interruption).
- Latency budget tests with recorded samples against the Worker.

## Deferred / rejected

- On-device speech/AI inference: **rejected** - insufficient local compute (ADR 0003).
- Runtime-loaded plugin modules: **rejected** - modules are compile-time feature modules
  (ADR 0007). AI-generated *content* is data consumed by the drill engine, not a module
  (ADR 0010).
- Real-world field missions with human locals: not required - the field is simulated
  (ADR 0013).
- openSMILE tone extraction in the Worker via WebAssembly: **rejected** - no official
  build, single-threaded startup limits, and extra decode/latency for no accuracy gain
  (`docs/09`).
- Accounts and cross-device sync.
