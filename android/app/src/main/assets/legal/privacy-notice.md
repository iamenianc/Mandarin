# Privacy notice

Status: draft. LearnHuayu is a private, single-user app with no accounts, profiles,
analytics, or monetization (`docs/00-vision.md`; NFR-9).

## Data kept on the device

Preferences, attempts and feedback, conversation sessions and turns, field missions and
mission sessions, debrief entries, progress, recordings, and cached generated audio and
items are stored locally (Room and DataStore; `docs/02-architecture.md`). See
`data-inventory.md` for the full list.

## Data that leaves the device

- Data leaves the device only to service an AI request, through the Cloudflare Worker to
  the provider (ADR 0003).
- Each workflow receives only what its task requires. Raw learner audio goes only to
  WF-1, WF-2, WF-4, and WF-10; WF-5, WF-8, and WF-9 receive aggregated metadata and
  content context only (`docs/08-ai-workflows.md`; FR-24).
- Nothing leaves the device for offline listening drills, repeat-after-audio, or bundled
  content playback (NFR-3).

## Retention and deletion

- The Worker does not persist learner audio or transcripts beyond what is needed to serve
  the request (ADR 0003; NFR-4).
- One-tap delete removes recordings, transcripts, and cached AI audio; debrief entries
  and mission transcripts stay local (`docs/02-architecture.md`).

## Consent

Sending recordings requires explicit, revocable consent; see `consent.md`. Withdrawing
consent disables the AI features that need audio while leaving offline practice intact.

## Offline behaviour

The bundled curated set works offline, and AI features degrade to their documented
fallbacks (ADR 0010).
