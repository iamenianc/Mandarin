# 0017 - Spotless with ktlint for formatting and static analysis

- Status: accepted
- Date: 2026-09-13

## Context

`docs/10-libraries-and-dependencies.md` requires detekt, ktlint, or Spotless at M1, with
the commands recorded in `AGENTS.md`. The scaffold added Android lint but no formatter, so
style is currently unenforced and the "lint/format config" M1 item is only half met.

## Decision

Use Spotless with its ktlint step for Kotlin sources, Gradle Kotlin scripts, and version
catalog files. `spotlessCheck` runs in CI and is added to `AGENTS.md`; `spotlessApply`
performs the fix. detekt is deferred: Android lint plus ktlint cover correctness and style
at this size, and detekt is added only if a rule it alone provides becomes necessary.

## Consequences

- The existing scaffold is reformatted once with `spotlessApply` before parallel slices
  begin, so later branches start from formatted code and avoid mass-conflict noise.
- CI gains `./gradlew spotlessCheck --no-daemon`; a formatting failure is a build failure.
- The Spotless plugin is added to the root build and pinned in the version catalog.
- Alternatives considered: ktlint alone (rejected - no Gradle-native caching or
  multi-language steps); detekt now (deferred - overlapping value, extra config surface).
