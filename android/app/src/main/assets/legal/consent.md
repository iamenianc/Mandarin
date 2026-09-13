# Recording and AI consent

Version: 1. Status: draft. This text is shown before recordings are sent to the AI
provider. Consent is explicit and revocable at any time in settings
(`docs/02-architecture.md`).

## What the app records

The microphone is used to record the learner's speech for pronunciation practice, spoken
drill answers, conversation, and the field mission (FR-7). Listening drills and
repeat-after-audio playback need no microphone.

## What is sent, and when

- A recording is sent only to service a request the learner makes, through the Cloudflare
  Worker, to the AI provider (Muse Spark). The Worker holds the provider credentials; no
  key is on the device (ADR 0003).
- Only the workflows that must analyze audio receive it: pronunciation feedback (WF-1),
  conversation turn (WF-2), spoken-answer transcription (WF-4), and the field-mission
  local turn (WF-10) (`docs/08-ai-workflows.md`).
- Workflows that do not analyze audio - progress summary (WF-5), exercise generation
  (WF-8), and field-mission generation (WF-9) - receive only aggregated metadata and
  content context, never raw audio or full transcripts.
- For pronunciation feedback, the reference clip and the learner's attempt are sent
  together so the model can compare them (ADR 0005). The request may also include
  measured per-syllable tone evidence (ADR 0014).

## What the app does not do

- No account, profile, analytics, or monetization; the app is private and single-user
  (`docs/00-vision.md`).
- Recordings are not retained beyond local storage, and the Worker does not keep learner
  audio or transcripts beyond what is needed to serve the request (ADR 0003; NFR-4).
- Recordings are not used for model training by default (NFR-5). The provider's terms
  govern processing.

## Revoking consent and deleting data

Consent can be withdrawn in settings. Withdrawal stops the AI features that need audio;
offline listening drills and repeat-after-audio keep working. One-tap delete removes
recordings, transcripts, and cached AI audio (`docs/02-architecture.md`). See
`privacy-notice.md` and `data-inventory.md`.
