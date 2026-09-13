# Roadmap

Status: draft. Milestones are sequential; each should leave the repo in a
coherent state. The throughline is audio-first: every milestone is judged on
listening and speaking, not on reading or writing.

## M0 - Planning (current)

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

## M1 - Project scaffold and audio foundation

- [ ] Create Android project (Kotlin, Compose, Gradle version catalog).
- [ ] Set up multi-module layout: `:app`, `:core:*`, `:feature:*` (ADR 0007).
- [ ] Define the `LearningModule` contract, `ContentItem`, and DI multibinding registry.
- [ ] Define the `:core:ai` workflow interfaces and one Worker endpoint per workflow
  (ADR 0009); the WF-1 request model includes the optional evidence field (ADR 0014).
- [ ] Microphone permission flow and audio capture/playback plumbing.
- [ ] CI and lint/format config.
- [ ] Add `build` / `lint` / `test` commands to `AGENTS.md`.
- [ ] Bundled reference clips and a Room schema.

Exit criteria: app builds, lists registered modules, plays a model clip, records the mic,
and plays it back.

## M2 - Tones, listening, vocabulary, and fundamentals modules

- [ ] Shared lesson/practice drill engine driven by `ContentItem`.
- [ ] **Tones module:** the four tones and the neutral tone, contours, tone pairs, and the
  tone-number notation; hear-and-name and production drills (beginner-first).
- [ ] **Listening module:** hear-and-respond drill loop with large, low-attention controls
  (spoken answers via WF-4; tap-only works offline).
- [ ] **Vocabulary module:** audio-first word/phrase lessons and practice over
  beginner/tourist/survival themes (pinyin always shown with tone numbers and taught as
  the pronunciation key, Hangul optional, no hanzi).
- [ ] **Fundamentals module:** syllable anatomy, the pinyin sound system (initials, finals,
  spelling conventions, tone numbers), tone sandhi, and read-aloud pinyin practice.
- [ ] Offline playback of bundled lessons.
- [ ] Ship the curated preloaded exercise set (`source = bundled`), fully usable offline.
- [ ] Session progress persisted locally.

Exit criteria: a learner can complete an offline session in any of the four modules.

## M3 - Speech and pronunciation feedback

- [ ] Speech capture with voice-activity detection (end-of-speech).
- [ ] Generate reference audio with Kokoro-82M from pinyin and bundle it per phrase (ADR 0006).
- [ ] **Acoustic-evidence spike (ADR 0014):** desktop extraction with openSMILE Python on
  Kokoro references and recorded attempts (including creaky tone 3 and noisy input);
  pipeline prototype (DTW alignment, speaker-relative normalization, tone comparison).
- [ ] **Extractor decision:** feature ablation (openSMILE breadth vs F0/voicing/loudness)
  and the grounded-vs-ungrounded WF-1 A/B (the adoption gate); Android integration only
  for the winning extractor; precompute reference evidence and syllable boundaries at
  content-build time; finalize the versioned evidence schema and WF-1 contract.
- [ ] **Speech and tones modules:** Worker-proxied reference-vs-attempt comparison via
  Muse Spark (WF-1, ADR 0005).
- [ ] Feedback maps coaching to pinyin syllables and tone numbers; model-vs-learner
  playback, retry loop, and prompt tuning.

Exit criteria: a learner speaks a phrase and gets specific, encouraging feedback in ~2s.

## M4 - AI conversation, the daily field loop, and Raymond

- [ ] Muse Spark understanding + Kokoro TTS conversation turn via the Worker (WF-2, WF-3).
- [ ] Scenario-based conversation lessons.
- [ ] Adaptive difficulty and gentle correction.
- [ ] **Raymond:** ask-anything Mandarin Q&A chat (WF-7), pinyin-only examples, bounded to
  Mandarin questions.
- [ ] **Endless practice:** runtime exercise generation (WF-8) with schema validation,
  deduplication, "extra" labeling, and offline fallback to bundled items.
- [ ] **Daily LAMP field loop:** script rehearsal (WF-9), a field mission with five
  simulated locals (WF-10 + WF-3), and debrief logging; bundled mission fallback offline.
- [ ] Latency budget met per `docs/01-requirements.md`.

Exit criteria: a learner holds a short, natural spoken exchange with the coach and
completes a full daily loop (rehearse, five locals, debrief).

## M5 - Progress, polish, and release

- [ ] Progress and recurring-problem reporting (audio-first) via WF-5, including debrief
  themes.
- [ ] Consent management and one-tap data deletion.
- [ ] Accessibility pass, performance, Play Store internal testing track.

## Backlog / deferred

- Additional learning modules beyond the initial set (e.g. conversation, numbers,
  scenario-specific modules) - additive via the module contract (ADR 0007).
- Accounts and cross-device sync.
- On-device inference (rejected for now - insufficient local compute).
