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
| 3 | Features: home, tones, vocabulary, listening, speech, fundamentals, field, raymond | wave 2 merged |
| 4 | Progress/reporting, consent + deletion, accessibility, release config | wave 3 merged |
| 5 | Full verification: build, lint, tests, release AAB; docs consistency pass | wave 4 merged |

Wave 1 in flight:

1. `scaffold/android-project` - Gradle multi-module scaffold per `docs/02-architecture.md`;
   all `:core:*` and `:feature:*` modules pre-registered with compiling stubs;
   `:core:model` contracts per the architecture doc; runnable Compose shell listing
   registered modules (FR-22); CI workflow per `docs/06-pipeline.md`; verified
   `build`/`lint`/`test` commands added to `AGENTS.md` (the one exception to
   orchestrator-owned files, for this session only). Status: takeover session
   `ses_f65676c3cffecVapS7gPS4c3a5` (worktree `scaffold-android-project-fix`) cherry-picked
   the checkpoint as `aa36a28` and is actively fixing the build (wrapper written 21:48).
   The original session in worktree `scaffold-android-project-18774aeda770df09`
   (`e841822`) is wedged and is not managed by this workspace, so it cannot be stopped or
   prompted from here; it needs cleanup in the Agent Manager UI, and its worktree is
   superseded by the takeover.
2. `worker/wf-endpoints` - one versioned endpoint per workflow per `docs/08-ai-workflows.md`
   and ADR 0003/0009; validation, per-workflow limits, tests; `api/README.md`. `api/` only.
   Reviewed: compliant; 31/31 tests pass. Merged in 6b8d3ec after the WF-7 audio-only fix
   and tone-number (1-5, ADR 0012) validation landed.
3. `assets/content-corpus` - curated bundled `ContentItem` corpus for the five modules plus
   schema and an offline validator, under `assets/content/**`. Merged: `node
   assets/content/validate.mjs` passes (tones 10 lessons/30 items, fundamentals 11/53,
   vocabulary 14/103, listening 4/13, speech 4/9); no Han script; all 26 files under
   `assets/content/`; author review of pinyin and tone choices remains open.
4. `assets/kokoro-pipeline` - build-time Kokoro reference-audio generation with offline
   `--dry-run`/`--selftest`, under `assets/audio/**` (ADR 0006). Merged: `--selftest`
   passes 15/15 and `--dry-run` plans the fixture clips; generated audio stays ignored.

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
removed per the conductor hygiene rule. The unmerged `assets-folder` worktree is kept for
the author's decision; the wedged original scaffold worktree needs UI cleanup.

Session conduct (conductor.md, updated in 203eab6): waits are capped at 60 seconds and the
loop ends its turn for peer replies; sessions are watched with evidence (activity, git
state, live process and log signals); a busy session with no writes, commits, or process
is wedged and is stopped rather than nudged; every new brief requires a completion report
as an Agent Manager peer reply (branch, commit SHAs, exact commands and results, open
questions), verified independently before merge; merged slices are stopped and their
worktrees removed.

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

## Validation of this loop

- Every wave leaves `master` coherent and green.
- No session edits a file another session owns; merges stay conflict-free.
- `.kilo/plan.md` reflects the current wave until the exit condition above is met.
