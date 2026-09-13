# 0001 - Record architecture decisions

- Status: accepted
- Date: 2026-09-12

## Context

The project needs a lightweight, durable way to capture significant technical
decisions while the app is still being planned, so future contributors
understand why choices were made.

## Decision

Record architecture decisions as ADRs in `docs/05-decisions/`, one file per
decision, named `NNNN-short-title.md` with a zero-padded sequential number.
Use the template below.

## Consequences

- Decisions are versioned with the docs and reviewable in pull requests.
- Superseded decisions are marked as such rather than deleted.
- Minor or easily reversible choices do not need an ADR.

## Template

```markdown
# NNNN - Short title

- Status: proposed | accepted | superseded by NNNN
- Date: YYYY-MM-DD

## Context

What is the problem or force that requires a decision?

## Decision

The decision, stated plainly.

## Consequences

What becomes easier or harder as a result. Include alternatives considered.
```
