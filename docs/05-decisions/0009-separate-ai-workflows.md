# 0009 - Separate, clearly defined AI workflows

- Status: accepted
- Date: 2026-09-13

## Context

AI models are used for several different jobs - pronunciation feedback, conversation,
speech synthesis, listening-response transcription, progress summaries, and build-time
content authoring. These jobs differ in inputs, expected output shape, latency budget,
consent exposure, and failure behavior. Using one generic "ask the model" path would mix
prompts, leak inappropriate context between tasks (e.g. raw learner audio into a summary),
and make each job impossible to tune or version independently.

## Decision

- Every AI task is a **named workflow** with its own **prompt, model, typed input, fixed
  output schema, latency budget, and failure/fallback**. Workflows never share a prompt.
- Workflows are declared and documented in `docs/08-ai-workflows.md`; each has a stable id
  (`WF-1`…).
- Workflows are implemented in a shared **`:core:ai`** module as typed interfaces; feature
  modules depend on the workflow, never on a provider. Provider selection stays inside the
  Worker.
- The Worker exposes **one versioned endpoint per workflow** (e.g.
  `/v1/wf/pronunciation-feedback`), so prompts and schemas are server-side and each
  workflow can be revised, rate-limited, and model-routed independently (ADR 0003).
- A workflow only receives the inputs it needs. Raw learner audio is sent only to the
  workflows that must analyze it (pronunciation feedback, response transcription,
  conversation turn); summaries receive aggregated metadata, not audio.
- Adding a new AI task means adding a new workflow, not extending an existing one.

## Consequences

- Prompts are versioned per workflow and can be tuned without regressing other tasks.
- Latency and cost budgets are per workflow, matching the NFRs they serve.
- Failure is isolated: one workflow falling back (offline drill, scripted line, canned
  summary) does not affect the others.
- More explicit surface area: each workflow needs its own schema, tests, and Worker route.
- Alternative considered: a single general-purpose assistant path (simpler to build, but
  couples unrelated tasks and prevents independent tuning) and per-feature ad-hoc provider
  calls (rejected - duplicates trust-boundary and schema handling).
