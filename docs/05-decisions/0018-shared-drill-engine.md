# 0018 - Shared drill engine and drill modes

- Status: accepted
- Date: 2026-09-13

## Context

ADR 0007 defines one `LearningModule` contract whose modules declare lessons and practice,
with **lessons and practice sharing one content/drill engine**, and `ContentItem` as the
shared item type. The wave-2 implementation left two gaps:

- `LearningModule.lessons()` and `practices()` were synchronous, while the bundled corpus
  is read through suspend repositories (`:core:data`), so a module could not return the
  shipped content without blocking or duplicating the loader.
- Nothing connected a lesson or practice spec to a runnable drill: the shell listed spec
  titles but could not run them.

## Decision

- Add `DrillMode { LESSON, HEAR_AND_NAME, LISTEN_AND_CHOOSE, SPEAK_AND_REPEAT }` to
  `:core:model`, and carry it on `PracticeSpec.mode` (default `LISTEN_AND_CHOOSE`).
  Lessons always browse (`LESSON`); a practice declares how it is answered.
- Make `LearningModule.lessons()` and `practices()` **suspend**. The registry keeps only
  `id` and `title` synchronous; specs load asynchronously from the bundled corpus.
- Put the shared drill engine in the `:app` shell (`ui/session`). One route,
  `session/{moduleId}/{kind}/{specId}`, runs any spec: it resolves the spec, loads its
  `ContentItem`s, presents one item at a time, plays the reference clip, and drives the
  interaction from the spec's `DrillMode`. `ModuleScreen` rows navigate to it.
- Mode behavior:
  - `LESSON`: browse, auto-play/replay the reference, previous/next.
  - `HEAR_AND_NAME`: reference playback plus tone-number choices; the answer is the item's
    `targetTones` joined with `-` when longer than one (ADR 0012).
  - `LISTEN_AND_CHOOSE`: reference playback plus pinyin choices.
  - `SPEAK_AND_REPEAT`: reference playback, record, play the attempt back.
  Choice lists are a pure function of the spec's items (stable order, no randomness), so
  the interaction is testable and reproducible.
- Choice drills reveal the item's pinyin, Hangul aid, and meaning only after the learner
  answers, so the answer is not given away. Pinyin stays the only notation and is visible
  at every other step.
- Feature modules map their bundled corpus practices to modes in code; the content JSON
  gains no `mode` field yet. Adding a module means implementing the contract and mapping
  its practices.
- Persist progress uniformly: moving past an item upserts `Progress`; a speaking attempt
  also upserts an `Attempt` with the cached WAV reference.
- The generated reference clips are not committed (ADR 0006), so a missing asset degrades
  to a visible, non-fatal message and the learner can continue; `PlaybackStatus.Failed` and
  `RecorderState.Failed` surface as text. Shared `:core:audio` singletons are never
  released by a screen.

## Consequences

- A module's lesson or practice becomes runnable with no shell change and no new UI code;
  new interaction shapes are added as new `DrillMode` values handled once in the engine.
- Module-specific AI steps (WF-1 feedback, WF-4 spoken answers, WF-8 generation) extend
  the engine by mode rather than by per-module screens.
- The contract is now suspend, so every implementer loads specs asynchronously; the
  registry stays synchronous for module discovery.
- Hiding pinyin during a choice drill is a deliberate, narrow exception to "pinyin always
  visible" (ADR 0011): the pinyin is the answer being tested and is shown immediately after
  the tap.
- Moving the practice-to-mode mapping into the content schema remains possible later; it
  was kept in feature modules because authored content is not edited by feature slices.
