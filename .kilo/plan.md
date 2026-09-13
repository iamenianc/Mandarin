# Plan: drive LearnHuayu to production level

Status: in flight. Orchestrator-owned. Updated after every wave.

## Directive

Take the repository from the M0/M1 boundary (docs and content assets only) to a
production-level app: roadmap milestones M1-M5 complete, exit criteria met, CI green,
release build producible, docs consistent, all guardrails satisfied.

The orchestrator (this session) owns: roadmap/README/doc consistency, ADRs, wave
sequencing, reviews, and merges. Delegated work runs in Agent Manager worktrees.

## Loop exit condition

All of the following hold on `master`:

- M1: app builds; shows registered modules; plays a reference clip; records mic; plays back.
- M2: a learner can complete an offline session in each of tones, listening, vocabulary,
  fundamentals; curated bundled content ships; progress persists locally.
- M3: speak-and-repeat with measured tone evidence and Worker-proxied WF-1 feedback.
- M4: WF-2/WF-3 conversation, Raymond (WF-7), runtime generation (WF-8), and the daily
  LAMP field loop (WF-9, WF-10, WF-3) with offline fallback.
- M5: progress reporting (WF-5), consent + one-tap deletion, accessibility pass,
  release configuration (signed AAB path documented).
- `gradlew.bat build` and tests green locally; CI workflow present and coherent.
- `docs/04-roadmap.md` fully checked; all other docs consistent with shipped behavior.
- Guardrails: no hanzi, pinyin with tone numbers, no first person in docs, no secrets or
  build output committed, ADR numbering intact.

## Compliance (mandatory for every session)

Every delegated brief must require, before any work:

1. Read `AGENTS.md` and `conductor.md`.
2. Read `docs/00-vision.md`, `docs/01-requirements.md`, `docs/04-roadmap.md`, then the
   task-relevant docs and ADRs.
3. Obey the guardrails: no hanzi; pinyin always shown with tone numbers (ADR 0011, 0012);
   Hangul optional (ADR 0004); English/pinyin UI only; no first person in docs; ADR
   template and numbering; no secrets, `local.properties`, keystores, or build output.
4. Touch only the files the slice owns; orchestrator-owned files are off limits unless
   explicitly listed. Single-writer rule prevents merge conflicts.

Orchestrator-owned (sessions must not edit): `docs/04-roadmap.md`, `docs/02-architecture.md`,
`docs/08-ai-workflows.md`, `README.md`, `.kilo/plan.md`. Updates to these land from the
orchestrator after merges.

## Waves

| Wave | Sessions (worktrees) | Depends on |
| --- | --- | --- |
| 1 | Android scaffold; Worker WF endpoints; content corpus; Kokoro pipeline | none |
| 2 | `:core:data`, `:core:audio`, `:core:ai`, `:core:assessment`, `:core:ui`; content pipeline | wave 1 merged |
| 3a | Shared drill engine in `:app`; async lesson/practice specs in `:core:model` | wave 2.5 merged |
| 3b | M2 modules: home, tones, vocabulary, listening, fundamentals | wave 3a merged |
| 3c | M3 speech: WF-1 feedback and tone evidence in the speak-and-repeat drill | wave 3b merged |
| 3d | M4: field loop (WF-9, WF-10, WF-3), Raymond (WF-7), endless practice (WF-8) | wave 3c merged |
| 4 | Progress/reporting, consent + deletion, accessibility, release config | wave 3d merged |
| 5 | Full verification: build, lint, tests, release AAB; docs consistency pass | wave 4 merged |

Wave 1 (merged): Android scaffold; Worker WF endpoints; content corpus; Kokoro pipeline.
All four slices were reviewed, verified on their branches, and merged; master carries the
scaffold (M1 core), `api/` with 31/31 tests, the validated content corpus, and the Kokoro
generator with `--selftest` 15/15. Details are in the master notes below.

Wave 2 (merged 2026-09-13): all six Agent Manager worktree slices below were reviewed,
merged to `master` with `--no-ff`, and verified together. Merge commits: `cf61a11`
(data), `9b08057` (audio), `6bd77fb` (ai), `d958254` (assessment), `0ceafec` (ui),
`f601e69` (content assets). The first integrated run exposed one task-dependency gap:
`syncContentAssets` fed `app/build/generated/contentAssets` to the app lint tasks without
a declared dependency; `b36ca1c` adds it (case-insensitive `lint` task match). The
corrected `.\gradlew.bat :app:assembleDebug test lint spotlessCheck` run on the merged tree
ended BUILD SUCCESSFUL, so `master` is green and the debug APK is produced.

The slice list (branch seeds, kept for the record):

1. `core/data` - Room schema, DataStore preferences, and a bundled-corpus repository that
   reads the APK asset convention `content/**` (JSON) and resolves `audioAssetRef` under
   `audio/**`; JVM/SQLite tests.
2. `core/audio` - Media3 playback wrapper, AudioRecord capture, a swappable end-of-speech
   VAD hook, and in-repo WAV helpers; JVM tests.
3. `core/ai` - typed WF-1..WF-5 and WF-7..WF-10 interfaces plus the Ktor Worker client
   (ktor-client-mock tests). Closes the M1 item: one endpoint per workflow (ADR 0009) and
   the optional WF-1 evidence field (ADR 0014). Prompts stay server-side.
4. `core/assessment` - deterministic F0/voicing/loudness extraction, DTW alignment,
   speaker-relative normalization, tone comparison, and a named-observation `ToneEvidence`
   model; the extractor choice remains gated by the M3 spike (ADR 0014).
5. `core/ui` - design system, `PinyinText` (tone numbers, never diacritics), optional
   Hangul aid line, audio drill controls, accessibility semantics.
6. `pipeline/content-assets` - Gradle tasks that validate the corpus and bundle
   `assets/content/**` and the reference/drill/sample audio into APK assets at build time
   (no committed copies).

Wave 2.5 (merged 2026-09-13): M1 audio demo in `:app` - microphone permission flow,
reference clip playback, and record/playback wiring against the merged core modules.
Worktree `app audio demo` (`wt-1789304417717-20`), branch `app/audio-demo`, session
`ses_f65255017ffeUGzYghOvT0d4UE`; merged as `e08111f`. Independent verification: the
diff is confined to `android/app/**`, no hanzi, `.\gradlew.bat :app:assembleDebug test
lint spotlessCheck` is green on the merged tree with 17/17 app unit tests, and the
debug APK packages `assets/audio/reference/tones/tone1-contour.wav`. The roadmap marks
M1 complete and M2 current.


Wave 3 refinement (2026-09-13): wave 2 left no shared drill engine and no path from a
module's lesson/practice to a runnable session; `LearningModule.lessons()/practices()`
is synchronous while the bundled corpus loads through suspend repositories. Wave 3 is
therefore sequenced as:

- 3a foundation and tracer bullet: a generic drill engine in `:app` (lesson browse,
  hear-and-name, listen-and-choose, speak-and-repeat over `ContentItem`s from
  `:core:data`), a `DrillMode` on `PracticeSpec`, and a suspend `LearningModule`
  contract in `:core:model`; the `:app` nav host gains a session route, `ModuleScreen`
  rows become runnable, and the `tones` module is wired end to end (bundled specs plus
  mode mapping) to prove the path. To be recorded as ADR 0018 after the interfaces
  settle in review.
- 3b remaining M2 modules: vocabulary, listening (tap-only offline), and fundamentals
  each map their bundled corpus to specs and modes, in parallel worktrees (distinct
  module directories, no shared files).
- 3c AI-supported drills: 3c-1 speech speak-and-repeat with WF-1 feedback and an
  evidence seam; 3c-1b measured tone evidence from the bundled clip, computed on device and
  cached; 3c-2 listening WF-4 spoken answers with the local matcher. WF-8 endless practice
  moved to 3d, where it shares the runtime-generation path with Raymond and the field
  mission.
- 3d M4: field loop (WF-9, WF-10, WF-3), Raymond (WF-7), and endless practice (WF-8).

Each sub-wave is fanned out only after the previous one merges, because 3a touches
`:app` and `:core:model` and every later slice touches at least one feature module.

3a (merged 2026-09-13): shared drill engine and tones tracer. Worktree `drill engine
tones` (`wt-1789304982862-21`), branch `app/drill-engine`, session
`ses_f651bd542ffe3WHzGboumTgxeY`; commits `6cba917` and `e612c54`; merged as `fd46173`.
Independent verification: the diff is confined to `:core:model`, the `ContentModule`
signatures, `:app`, `:feature:tones`, and the placeholder override signatures; no hanzi;
`:app:assembleDebug test lint spotlessCheck --rerun-tasks` is green on the branch with
every module at 0 failures (SessionViewModelTest 17, TonesLearningModuleTest 3,
LearningModuleContractTest 5) and again on the merged tree. The decision is recorded as
ADR 0018; wave 3b is unblocked.

3b (merged 2026-09-13): listening, vocabulary, and fundamentals module slices, one worktree
each, all synced to `6d96c48` and confined to their own `:feature:*` directory. Merge
commits: `3ba3ec9` (listening, branch commit `4792d2a`), `9507582` (vocabulary, `36d9d9a`),
`a11a758` (fundamentals, `26447bd`). Each was verified independently before merge: diff
confined to its module, no hanzi, the module's unit tests re-run (3/3 each), and
`:app:assembleDebug` plus `spotlessCheck` green; the integrated
`:app:assembleDebug test lint spotlessCheck` on merged master is green. M2 is complete and
the roadmap marks it so; M3 is current.

Fundamentals recovery: that worker wedged mid-verification (its Kotlin compiler sat with a
flat CPU profile for ~17 minutes while the session waited). The session was stopped, the
hung Java processes were killed, and the conductor finished the slice: `spotlessApply`, the
scoped test (`:feature:fundamentals:testDebugUnitTest` 3/3), `:app:assembleDebug`, and
`spotlessCheck`, all with `--no-daemon`, then committed and merged. The slice's files were
complete on disk; only verification and commit were lost.

3c refinement (2026-09-13): speech needs WF-1 feedback and grounded tone evidence, but
reference clips publish as OGG (ADR 0006) and reference features with syllable boundaries
are not precomputed, so grounded evidence cannot be computed at runtime from the bundled
clips. 3c is sequenced as: 3c-1 speech speak-and-repeat with WF-1 feedback over the
reference and attempt clips, with an `AttemptEvidenceSource` seam that returns null until
reference features exist; 3c-1b precompute reference features and syllable boundaries at
content-build time and wire the seam; 3c-2 listening WF-4 spoken answers with the local
matcher and WF-8 endless practice. Only one 3c slice runs at a time because all three
extend the `:app` engine.

3c-1 (merged 2026-09-13): worktree `speech wf1` (`wt-1789318537609-25`), branch
`app/speech-feedback`, session `ses_f644ddaf7ffeeKNk1Vgm51u6an`, based on `1f640c0`;
commit `933db6f`; merged as `5f5751f`. Adds `DrillMode.SPEAK_AND_REPEAT_FEEDBACK`, wires
`:feature:speech` to the corpus, and gives the engine a coached feedback step: it reads the
reference asset bytes (`ReferenceClipReader`), sends reference plus attempt clips to WF-1,
and renders the coaching or the offline fallback. The measured-evidence seam
(`AttemptEvidenceSource`) returns null until reference features exist. Independent
verification: diff confined to `:app`, `:feature:speech`, and the one `DrillMode` value; no
hanzi; `:app:testDebugUnitTest` (SessionFeedbackTest 6, SessionViewModelTest 17) and
`:feature:speech:testDebugUnitTest` (3) re-run green, `:app:assembleDebug` and
`spotlessCheck` green, all with `--no-daemon`.

3c-2 (merged 2026-09-13): spoken answers via WF-4 in the listening drill, tap-only kept as
the always-available fallback. Worktree `listening wf4` (`wt-1789319842347-26`), branch
`app/listening-wf4`, session `ses_f6439f1feffeb52S1Skzod9bYf`, based on `1713742`; commits
`3bac8d9`, `cc7e3f9`, `f8dfc4b`; merged as `79b03f1`. Adds
`DrillMode.LISTEN_AND_ANSWER_SPOKEN`, maps the listening word practice to it, and gives the
engine a spoken-answer step that calls `ResponseTranscriptionWorkflow` and falls back to
tappable choices on any failure. Independent verification: diff confined to `:app`,
`:feature:listening`, and the one `DrillMode` value (11 files, +513/-14); no hanzi;
`:app:testDebugUnitTest` (49 tests, 0 failures) and `:feature:listening:testDebugUnitTest`
(3, 0) re-run green, `:app:assembleDebug` and `spotlessCheck` green on the branch; the
integrated `:app:assembleDebug test lint spotlessCheck` on merged master is green.

3c-1b (merged 2026-09-14): measured tone evidence behind the `AttemptEvidenceSource` seam.
Worktree `tone evidence` (`wt-1789331749091-27`), branch `app/tone-evidence`, session
`ses_f638443ebffe5J0MwBDi0xga60`, based on `7685e4f`; commits `222bf4f` and `2a41798`;
merged as `e503fdf`. Reference features are computed from the bundled clip on device and
cached per item: `SyllableAutoSegmenter` in `:core:assessment` recovers syllable spans from
the clip's energy contour, and `AssessmentAttemptEvidenceSource` in `:app` decodes the clip
(`ReferencePcmDecoder`: MediaExtractor/MediaCodec, or the WAV helper), precomputes, aligns,
classifies, and maps `ToneEvidence` to `:core:ai.AcousticEvidence`, returning null on any
failure so the audio-only fallback is preserved. Recorded as ADR 0019; the ADR 0014 oracle
stays a documented follow-up. Independent verification: diff confined to `:core:assessment`
and `:app` (8 files, +962/-11); no hanzi; `:core:assessment:testDebugUnitTest` and
`:app:testDebugUnitTest` re-run green (new `SyllableAutoSegmenterTest`,
`AssessmentAttemptEvidenceSourceTest`), plus `:app:assembleDebug`, `:app:lintDebug`, and
`spotlessCheck` on the branch; the integrated `:app:assembleDebug test lint spotlessCheck`
on merged master is green.

M3 remainder (2026-09-14): the end-of-speech VAD item is not yet wired into the drill.
`:core:audio` ships `EndOfSpeechDetector`/`EnergyZeroCrossingEndOfSpeechDetector` but
`AudioRecorder` does not expose frames or consume the detector, so the next slice adds an
end-of-speech hook to `:core:audio` and auto-stop in `:app`; M3 is complete after that.

VAD auto-stop (merged 2026-09-14): worktree `vad autostop` (`wt-1789332881433-28`), branch
`app/vad-autostop`, session `ses_f6372fcceffe70Iu5NFmMese4x`, based on `206c504`; commit
`d4399e2`; merged as `b2bd3a3`. `AudioRecorder` now publishes `vadEvents`, the
`AudioRecordRecorder` feeds an injected `EndOfSpeechDetector` and resets it per utterance,
and `SessionViewModel` auto-stops `SPEAK_AND_REPEAT`, `SPEAK_AND_REPEAT_FEEDBACK`, and
`LISTEN_AND_ANSWER_SPOKEN` exactly once on `SpeechEnded` through the same stop path as the
manual button. Independent verification: diff confined to `:core:audio` and `:app` (7
files, +440/-1); no hanzi; `:core:audio:testDebugUnitTest` and `:app:testDebugUnitTest`
green, plus `:app:assembleDebug`, `:app:lintDebug`, and `spotlessCheck` on the branch; the
integrated `:app:assembleDebug test lint spotlessCheck` on merged master is green (its
first attempt failed transiently and passed on re-run). M3 is complete; the roadmap marks
it so and M4 is current.

3d-1 (in flight 2026-09-14): feature-destination contract plus the Raymond chat (FR-25,
WF-7). Worktree `raymond chat` (`wt-1789333870141-29`), branch `app/raymond-feature`,
session `ses_f6363e5c9ffeghSiIV6QbuJoXf`, based on `f77b237`. Adds a `FeatureDestination`
contract in `:core:ui` that feature modules bind with Hilt `@IntoSet` and the shell renders
at `feature/{id}` and lists on Home, and implements Raymond in `:feature:raymond`: a typed
or voice question, WF-7 with accumulated history, pinyin examples and follow-up chips, and
a friendly offline message. Owns `:core:ui`, `:feature:raymond`, and `:app`.

Session IDs and worktree names are recorded in the Agent Manager overview; each brief
requires a completion report as a peer reply, with verification run independently before
merge.

Worktree gotcha (2026-09-13): Agent Manager seeds new worktrees from `origin/master`,
which lags local `master` because the loop commits locally and does not push. Before a
slice starts, its worktree branch is fast-forwarded to local `master`, and each brief
tells the worker to confirm `git log --oneline -1` matches `master` and to
`git merge --ff-only master` if `master` moves mid-slice. The wave-3a worktree was
corrected this way after its first session started on the stale base.

Wave 2 session IDs: `core/data` `ses_f655089b9ffeR7ZeraemxcxTRQ`; `core/audio`
`ses_f65508453ffedHgaory3GwMfiK`; `core/ai` `ses_f65507b60fferrHEX5IF5TMeBQ`;
`core/assessment` `ses_f655073d4ffeLsHBhFZK7aHctS`; `core/ui`
`ses_f65506bdaffezlnsIfxZ1LDyg0`; `pipeline/content-assets`
`ses_f655060bfffegGactmwp3viDbn`. Checkpoints are required before risky fixes; the
orchestrator verifies each branch's diff and checks before merging, one branch at a time.

Scope change (2026-09-13): the author dropped hands-free and eyes-free session modes.
Recorded as ADR 0015; FR-13/FR-14 marked Won't (v1); the vision criterion, both roadmap
items, and the design-doc mention are removed; the design worktree session was stopped
before producing changes.

Master notes: the sibling cleanup commit 909e063 (leftover eyes-free references) was
reviewed and is valid. The Worker slice is merged; `docs/02-architecture.md` now points at
`api/README.md` for the implemented routes. Orchestrator re-verified on `f374ffc`: the
`api/` tree is identical to the reviewed head `b49f8f4`, and `npm test` passes 31/31.
Kokoro is merged; `--selftest` passes 15/15 on master. The merged wave-1 worktrees
(worker, content corpus, Kokoro) and the stale empty `assets-*` worktrees were stopped and
removed per the conductor hygiene rule. No `assets-folder` worktree exists on disk or in
the Agent Manager overview at takeover, so there is nothing to keep for the author.

Formatting config closed on master as `f08f8f1`: Spotless + ktlint 1.4.1 for Kotlin and
Gradle scripts, with ktlint's `function-naming` rule ignoring `@Composable` names
(ADR 0017); `spotlessCheck` and the Worker tests are wired into CI; `AGENTS.md` records
`spotlessCheck`/`spotlessApply`; the version catalog gained the wave-2 dependency set and
the serialization plugin. Orchestrator verification on master: `.\gradlew.bat
:app:assembleDebug test lint spotlessCheck` ended BUILD SUCCESSFUL with the debug APK
present; `spotlessApply` changed import order only. The superseded straggler worktree
`scaffold-android-project-18774aeda770df09` held no unique content (on disk only
lint-cache jars; its branch's only delta was a stale `eyesFreeMode` preference superseded
by ADR 0015) and was removed; its branch `scaffold/android-project` is kept only as an
inert ref.

Session conduct (conductor.md, updated in 203eab6): waits are capped at 60 seconds and the
loop ends its turn for peer replies; sessions are watched with evidence (activity, git
state, live process and log signals); a busy session with no writes, commits, or process
is wedged and is stopped rather than nudged; every new brief requires a completion report
as an Agent Manager peer reply (branch, commit SHAs, exact commands and results, open
questions), verified independently before merge; merged slices are stopped and their
worktrees removed. Recovery on this machine (2026-09-13): sample a build process's CPU
twice - a flat profile means blocked, not compiling; Gradle daemons can also keep the
console open after `BUILD SUCCESSFUL`, so conductor-run verification uses `--no-daemon`
and reads the JUnit XML for results. An integrated run can also fail transiently (seen on
`lintAnalyzeDebugUnitTest` and after the VAD merge) and pass on re-run: re-run the failing
task alone before treating it as real.

## Merge protocol

After each wave:

1. Inspect each finished worktree: `git status`, log, diff versus `master`.
2. Verify the slice's acceptance points against the task and the docs.
3. Run its checks in the worktree (build, tests) where the environment allows.
4. Merge to `master` (user-confirmed), one branch at a time, subtrees disjoint.
5. Update roadmap/README/doc consistency on `master`; commit.

## Open questions (non-blocking)

- Play Store publication itself needs the author's developer account; "production level"
  means release-ready, not published.
- WF-3 (Kokoro TTS) provider path: the Worker cannot run Kokoro; the worker session must
  follow `docs/08-ai-workflows.md` and record any contract gap in `api/README.md`.
- M3 extractor choice is gated by the ADR 0014 spike; the pipeline ships with the
  pure-Kotlin default and the gate documented.
- Wave 2.5 tests cover the audio-check wiring at the JVM level; real microphone capture
  and ExoPlayer asset/local playback have not been exercised on a device or emulator.
  The first on-device run of the audio check is the remaining M1 risk and is folded into
  the wave 5 verification pass.
- Reference-clip naming: the corpus `audioAssetRef` values and the Kokoro generator's
  output filenames have not been reconciled against a real generated set, and a clean
  checkout has no reference `.ogg`, so drills degrade to the non-fatal message until the
  generator runs. Wave 5 verifies that the generator output matches the corpus refs before
  the release build.

## Validation of this loop

- Every wave leaves `master` coherent and green.
- No session edits a file another session owns; merges stay conflict-free.
- `.kilo/plan.md` reflects the current wave until the exit condition above is met.
