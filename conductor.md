# Conductor

Playbook for the orchestrating agent. `AGENTS.md` holds the rules; this file holds the
operating procedure for planning, slicing, delegating, and verifying work in this repo.

## Read first

Before planning any work, read, in order:

1. `AGENTS.md` - ground rules and doc conventions (authoritative; this file defers to it).
2. `docs/00-vision.md` and `docs/01-requirements.md` - what the product is and is not.
3. `docs/04-roadmap.md` - the current milestone and its exit criteria.
4. `.kilo/plan.md` - the in-flight plan, if any.
5. `docs/02-architecture.md` and `docs/05-decisions/` - settled structure and decisions.

Treat `docs/04-roadmap.md` as the source of truth for status; do not trust any status line
elsewhere, including this file.

## Current shape of the repo

- **Docs and code together.** The repo began docs-first; `docs/` remains the source of
  truth for product and architecture decisions, and application code now develops alongside
  it. Unless a task explicitly asks for code, the deliverable is a documentation change.
- **No build, lint, or test commands exist yet.** Do not invent or claim one. M1 adds them
  to `AGENTS.md`; run them only once present.
- The stack, module layout, and AI workflow registry are proposals in
  `docs/02-architecture.md` and `docs/08-ai-workflows.md` until scaffolded into code.

## Operating mode

- Default to editing existing docs. Create a new file only when a task requires it, and
  then update the `README.md` contents table.
- Record decisions as ADRs in `docs/05-decisions/NNNN-short-title.md`, following the
  `0001` template. When a decision changes an earlier one, amend the earlier ADR's status
  line and cross-link both directions.
- Keep the vision, requirements, architecture, roadmap, and workflow registry consistent
  with each other. A change to one usually implies a check of the others.

## Slicing work

- Cut **vertical slices** that each leave the repo coherent, mirroring how milestones are
  written. A slice that only half-updates a decision is not sliceable.
- One delegable unit is a **single coherent change set** with explicit acceptance: the
  files it touches, the cross-references it adds, and the convention checks it satisfies.
- Sequence slices so that shared documents are touched once. `README.md`,
  `docs/02-architecture.md`, `docs/04-roadmap.md`, and `docs/08-ai-workflows.md` are
  collision hotspots; land them in one slice or one serialized order rather than fanning
  out parallel edits.

## Delegating

- **Research and exploration** - use a read-only subagent (`explore`) to map docs, find
  cross-references, or answer "where is X decided". Keep the result; do not duplicate it.
- **Independent doc slices** - use Agent Manager worktree sessions, one worktree per
  independent slice. Group alternate versions of the same slice with `versions`.
- **Consequential and cross-cutting work stays with the orchestrator**: new or amended
  ADRs, vision/requirements wording, the README table, and any final consistency pass.
- Do not fan out work that edits the same file in parallel. Serialize it or keep it local.
- Give each delegated session the grounding it needs (relevant doc paths and ADR numbers)
  and a definition of done it can check without re-reading the whole repo.

## Guardrails

Every slice must satisfy all of these before it is done:

- No hanzi anywhere. Pinyin is always shown and taught as the pronunciation key (ADR 0011),
  with tones written as numbers (ADR 0012). Hangul stays optional (ADR 0004).
- No first person in docs. Refer to "the user" or "the coding agent".
- ADR filenames are zero-padded and sequential; the `0001` template is followed.
- Cross-references resolve, including amended-by and amends links.
- New top-level docs are added to the `README.md` contents table.
- Nothing secret or generated is committed: no API keys, `local.properties`, keystores,
  `build/`, `.gradle/`, or `.idea/`.
- No out-of-scope claims: reading and writing hanzi remain out of scope; accounts, sync,
  and monetization remain deferred unless an ADR says otherwise.

## Verifying

Before declaring a slice complete:

1. Re-read the task and confirm each acceptance point is met by a specific change.
2. Confirm the guardrails above, mechanically where possible (grep for hanzi and for first
   person; check ADR numbering and links).
3. Confirm the affected docs still agree with `docs/04-roadmap.md` and the ADR set.
4. State what changed, what was verified, and what remains open. Mark any non-blocking
   questions explicitly rather than guessing.

## Handoff

- Leave the repo in a coherent state at the end of every slice; do not leave a decision
  half-recorded.
- For in-flight work, keep `.kilo/plan.md` current: directive, recommendation, planned file
  changes, open questions, and validation.
- When passing work to another agent, capture the plan, the affected paths, and the open
  questions rather than the conversation.
