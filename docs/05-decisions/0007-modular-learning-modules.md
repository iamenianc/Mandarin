# 0007 - Modular learning modules

- Status: accepted
- Date: 2026-09-13

## Context

The first build teaches fundamentals, theory, and beginner/tourist/survival Mandarin,
starting with learning areas that include **tones**, **vocabulary**, **speech**,
**listening**, and **fundamentals**. More areas (conversation, numbers, scenarios) will
follow. `docs/02-architecture.md` previously deferred a multi-module split; the product
now requires that new learning areas be added without reworking the app shell or the
shared audio/AI pipeline.

## Decision

- Build the app as a **Gradle multi-module** project, one feature module per learning
  module, on top of shared `:core:*` modules.
- Define a **`LearningModule` contract** in `:core:model`. Each module declares an `id`,
  a `title`, its **lessons** (guided teaching) and its **practice** (reps). Modules are
  discovered via **DI multibinding**; the shell renders whatever the registry exposes.
- **Lessons and practice share one content/drill engine.** `ContentItem` carries a type
  discriminator (`phrase`, `word`, `minimalPair`, `dialogue`), so new item types extend
  content without new engines.
- **Content is data, not code.** Modules ship authored content (bundled JSON/assets) plus
  Kokoro-generated reference audio (ADR 0006). Adding a module means adding a feature
  module, its content, and one DI binding.
- The audio/AI pipeline stays in `:core:audio` and the `:core:ai` workflows (ADR 0009);
  feature modules consume them and never call providers directly.

Layout:

```
:app                shell, nav host, DI wiring, module registry
:core:model         LessonSpec, ContentItem, Attempt, Progress, LearningModule
:core:audio         Media3 playback, AudioRecord capture, VAD
:core:data          Room + DataStore repositories
:core:ai            AI workflow interfaces + schemas (WF-1…WF-6, ADR 0009)
:core:assessment    Pronunciation-feedback workflow client (WF-1, ADR 0005)
:core:ui            Compose design system, shared drill controls
:feature:home       module launcher + today's plan
:feature:tones      tone lessons + practice (beginner-first)
:feature:vocabulary vocab lessons + practice
:feature:listening  listening lessons + practice
:feature:speech     speech/pronunciation lessons + practice
:feature:fundamentals fundamentals lessons + practice
(future)            :feature:conversation, :feature:numbers
```

## Consequences

- New learning modules are additive: no shell, navigation, or audio changes required.
- Shared modules keep the audio path, feedback schema, and progress model consistent
  across modules, so feedback and reporting work uniformly.
- Multi-module adds Gradle and DI wiring complexity up front; accepted in exchange for
  extensibility.
- Content authoring and build-time audio generation become the main cost of a new module,
  not engineering.
- Alternative considered: a single module with internal feature packages (simpler to
  start, but does not enforce the boundaries the product requires) and a plugin system
  with runtime module loading (unnecessary for a single-user, statically bundled app).
