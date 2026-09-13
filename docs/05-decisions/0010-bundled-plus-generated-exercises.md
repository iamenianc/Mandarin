# 0010 - Bundled exercises plus AI-generated extensions

- Status: accepted
- Date: 2026-09-13

## Context

A fixed lesson set is finite: the learner can finish the bundled exercises and then run
out of practice, and a curated set cannot cover every word, tone combination, or scenario
the learner asks for. The app must be useful immediately and offline, yet able to continue
indefinitely. Unbounded AI content, however, risks drifting from the product: hanzi,
invalid pinyin, duplicate items, or off-theme examples.

## Decision

- The app **ships with a preloaded set of curated exercises**, bundled and fully usable
  offline. This is the default and the fallback.
- An **AI expansion flow** (WF-8, runtime exercise generation) lets the learner extend or
  continue with new examples on demand - effectively limitless practice.
- Generated items are **validated before use**: fixed output schema, pinyin required,
  **no hanzi**, item type from the known set, deduplicated against existing content, and
  matching the requested module, theme, and difficulty.
- Generated items are **labeled as extra** and are **not** treated as curated content; the
  learner can always fall back to bundled exercises.
- Reference audio for generated items is produced by WF-3 (speech synthesis).
- WF-8 is distinct from WF-6 (build-time content authoring, which is human-reviewed and
  becomes committed content). WF-8 runs at runtime and never edits the shipped set.

## Consequences

- The app is useful offline from first launch, and has no ceiling on practice volume
  online.
- Two content sources must coexist: curated (bundled, versioned) and generated (runtime).
  The content model gains a `source` discriminator, and generated items may be cached
  locally.
- Validation and deduplication are load-bearing: without them, AI output would corrupt the
  learning experience. Malformed generations are dropped and fall back to bundled items.
- Product invariants (no hanzi, pinyin always shown, audio-first) are enforced on generated
  content, not assumed.
- Alternative considered: relying solely on AI generation (rejected - no offline use,
  quality and cost variability) and relying solely on bundled content (rejected - finite,
  and cannot respond to what the learner wants to practice).
