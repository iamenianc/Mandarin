# Roadmap

Status: draft. Milestones are sequential; each should leave the repo in a
coherent state. The throughline is audio-first: every milestone is judged on
listening and speaking, not on reading or writing.

## M0 - Planning (complete)

- [x] Repository and doc structure.
- [x] Vision and requirements settled (audio-first, pinyin always visible, Hangul optional, private project).
- [x] Architecture and ADRs recorded (Kokoro reference audio, Muse Spark feedback, Worker proxy).
- [x] Curriculum scope and modular architecture settled (ADR 0007, ADR 0008).
- [x] AI split into separate, clearly defined workflows (ADR 0009, `docs/08-ai-workflows.md`).
- [x] Content model settled: bundled curated exercises plus AI-generated extensions (ADR 0010).
- [x] Pinyin direction recorded: pinyin is the taught pronunciation layer (ADR 0011).
- [x] Tone notation and module recorded: tone numbers, dedicated tones module (ADR 0012).
- [x] Daily practice loop recorded: the LAMP field loop with simulated locals and a debrief (ADR 0013).
- [x] Grounded pronunciation feedback recorded: measured tone evidence for WF-1 (ADR 0014).
- [x] Hands-free and eyes-free modes dropped from scope (ADR 0015).

Exit criteria: requirements and architecture are stable enough to scaffold code.

## M1 - Project scaffold and audio foundation (complete)

- [x] Create Android project (Kotlin, Compose, Gradle version catalog).
- [x] Set up multi-module layout: `:app`, `:core:*`, `:feature:*` (ADR 0007).
- [x] Define the `LearningModule` contract, `ContentItem`, and DI multibinding registry.
- [x] Define the `:core:ai` workflow interfaces and one Worker endpoint per workflow
  (ADR 0009); the WF-1 request model includes the optional evidence field (ADR 0014).
- [x] Microphone permission flow and audio capture/playback plumbing.
- [x] CI and lint/format config.
- [x] Add `build` / `lint` / `test` commands to `AGENTS.md`.
- [x] Bundled reference clips and a Room schema.

Exit criteria met: the app builds, lists registered modules, plays a model clip, records
the mic, and plays it back.

## M2 - Tones, listening, vocabulary, and fundamentals modules (complete)

- [x] Shared lesson/practice drill engine driven by `ContentItem` (ADR 0018).
- [x] **Tones module:** the four tones and the neutral tone, contours, tone pairs, and the
  tone-number notation; hear-and-name and production drills (beginner-first). The bundled
  tones lessons and practices run through the engine; measured feedback is M3.
- [x] **Listening module:** hear-and-respond drill loop with large, low-attention controls;
  tap-only drills work offline. WF-4 spoken answers are wired through the engine (wave
  3c-2), with the tappable choices kept as the always-available fallback.
- [x] **Vocabulary module:** audio-first word/phrase lessons and practice over
  beginner/tourist/survival themes (pinyin always shown with tone numbers and taught as
  the pronunciation key, Hangul optional, no hanzi).
- [x] **Fundamentals module:** syllable anatomy, the pinyin sound system (initials, finals,
  spelling conventions, tone numbers), tone sandhi, and read-aloud pinyin practice.
- [x] Offline playback of bundled lessons: the engine plays `audio/reference/**` from APK
  assets; the generated clips are build-time inputs (ADR 0006).
- [x] Ship the curated preloaded exercise set (`source = bundled`), fully usable offline.
- [x] Session progress persisted locally (Progress and Attempt repositories).

Exit criteria met: a learner can complete an offline session in any of the four modules.

## M3 - Speech and pronunciation feedback (complete)

- [x] Speech capture with voice-activity detection (end-of-speech). The recorder publishes
  VAD transitions and the drill auto-stops the speaking modes once on `SpeechEnded`, with
  the manual stop unchanged.
- [x] Generate reference audio with Kokoro-82M from pinyin and bundle it per phrase (ADR 0006).
  The generator, manifest, and bundling path exist; clips are build-time inputs.
- [x] **Extractor decision:** adopt the pure-Kotlin F0/voicing/loudness extractor
  (`BaselineAcousticFeatureExtractor`) for tone and rhythm (ADR 0019). The openSMILE
  validation oracle, the feature ablation, and the grounded-vs-ungrounded WF-1 A/B remain
  documented follow-ups (ADR 0014) rather than v1 blockers, and the versioned evidence
  schema and WF-1 contract are unchanged from the wave-2 interfaces.
- [x] Reference evidence is measured on device from the bundled clip and cached per item
  (ADR 0019), because the extractor cannot be hosted by a Gradle build task without a
  module split.
- [x] **Speech and tones modules:** Worker-proxied reference-vs-attempt comparison via
  Muse Spark (WF-1, ADR 0005). The speech module runs coached speak-and-repeat with an
  offline fallback; the measured-evidence seam (`AttemptEvidenceSource`) is in place.
- [x] Feedback maps coaching to pinyin syllables and tone numbers; model-vs-learner
  playback, retry loop, and prompt tuning. Feedback renders as pinyin coaching with a
  retry step; prompt tuning continues.

Exit criteria met: the speech module records a phrase and returns specific, encouraging
WF-1 coaching, with measured tone evidence when the reference clip is bundled and a clean
offline fallback otherwise.

## M4 - AI conversation, the daily field loop, and Raymond (current)

- [x] Muse Spark understanding + Kokoro TTS conversation turn via the Worker (WF-2, WF-3).
  Both workflows and their Worker endpoints shipped in `:core:ai`; the field mission voices
  every local reply through WF-3.
- [x] Scenario conversation surface: the daily field mission role-plays a scenario with five
  simulated locals (FR-32) and Raymond answers free questions. A dedicated scenario-lesson
  module stays additive backlog.
- [x] **Raymond:** ask-anything Mandarin Q&A chat (WF-7), pinyin-only examples, bounded to
  Mandarin questions. Shipped as the first feature destination (ADR 0020): typed or voice
  question, accumulated history, pinyin examples with meanings, follow-up suggestions, and
  a friendly offline message.
- [x] **Endless practice:** runtime exercise generation (WF-8) with schema validation,
  deduplication, "extra" labeling, and offline fallback to bundled items. Shipped in the
  drill engine: admission drops hanzi, toneless pinyin, malformed tones, and duplicates;
  admitted items are labeled generated with stable ids; any failure keeps the bundled
  session finishable.
- [x] **Daily LAMP field loop:** script rehearsal (WF-9), a field mission with five
  simulated locals (WF-10 + WF-3), and debrief logging; bundled mission fallback offline.
  Shipped as a feature destination (ADR 0020): three steps, five in-character locals with
  voiced replies when TTS succeeds, per-local sessions and turns persisted, and debrief
  entries logged as pinyin.
- [x] Latency budget: every workflow call carries a client-side timeout and a small payload
  (WF-8 generation times out at 10 s and falls back); the numeric budget in
  `docs/01-requirements.md` is measured on the author's device, not in CI.

Exit criteria met: a learner holds a short, natural spoken exchange with the coach (the
five-local field mission) and completes a full daily loop (rehearse, five locals, debrief).

## M5 - Progress, polish, and release (current)

- [x] Progress and recurring-problem reporting (audio-first) via WF-5, including debrief
  themes. Shipped as a feature destination: local themes and most-practised items are always
  computed, WF-5 adds a short summary with focus areas, the summary can be voiced through
  WF-3, and a workflow failure keeps the local view with a friendly note.
- [x] Consent management and one-tap data deletion. A settings destination persists the
  recording-to-provider consent and the Hangul aid, and `Delete all my data` clears every
  Room table, resets the preferences, and removes the local recordings behind a confirmation
  dialog.
- [ ] Accessibility pass, performance, Play Store internal testing track.

## Backlog / deferred

- Additional learning modules beyond the initial set (e.g. conversation, numbers,
  scenario-specific modules) - additive via the module contract (ADR 0007).
- A dedicated scenario-lesson module and the coaching surface it enables (adaptive
  difficulty and gentle correction). The workflow contracts already carry `WF-10
  targetDifficulty` and `WF-2 gentleCorrection`; the field mission intentionally does not
  coach (FR-32), so that UX is deferred with the module.
- Accounts and cross-device sync.
- On-device inference (rejected for now - insufficient local compute).
