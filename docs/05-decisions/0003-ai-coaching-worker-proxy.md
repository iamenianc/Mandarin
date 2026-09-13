# 0003 - AI via a Cloudflare Worker proxy

- Status: accepted
- Date: 2026-09-12
- Amended by: 0009-separate-ai-workflows.md (one versioned endpoint per workflow;
  listening-response transcription is a distinct workflow, WF-4)

## Context

Pronunciation feedback and AI conversation practice need multimodal audio understanding
and text-to-speech with low latency. Running the provider directly from the app would
ship API keys in the APK and expose them to abuse. Running it on-device would avoid that
but local compute is insufficient for the models the app relies on.

## Decision

- Route **multimodal audio understanding (Muse Spark)**, the conversation turn, and
  **text-to-speech (Kokoro)** through a **Cloudflare Worker** in `api/`.
- There is no separate speech-recognition stage; all audio input goes to Muse Spark.
- The Worker is the trust boundary: it holds provider keys, enforces rate limits, caps
  payload sizes, and streams audio where possible (WebSocket) to hit the feedback and
  turn-latency budgets.
- The app talks to the Worker over HTTPS/WebSocket with no provider credentials on the
  device.
- Offline listening drills and repeat-after-audio remain fully functional without the
  Worker; only AI coaching depends on it.

## Consequences

- AI features require a network connection and depend on provider availability and
  cost; the app must degrade gracefully to offline drills.
- The Worker must not persist learner audio or transcripts beyond what is needed to
  serve a request; consent for sending recordings is explicit and revocable
  (NFR-4/NFR-5).
- Deploying the Worker is part of every release (see `docs/06-pipeline.md`).
- Alternative considered: on-device inference - **rejected**, local compute is
  insufficient. Direct-from-device provider calls rejected: key exposure.
