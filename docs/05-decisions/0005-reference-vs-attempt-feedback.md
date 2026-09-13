# 0005 - Pronunciation feedback by comparing reference and attempt audio

- Status: accepted (amended by 0014 for measured tone evidence)
- Date: 2026-09-12
- Specified as: WF-1 in `docs/08-ai-workflows.md` (ADR 0009)

## Context

Beginners need fast, useful pronunciation and tone feedback, but the app does not need
scientific tone scoring. Building a calibrated GOP/F0 assessor is a large, ongoing
effort, and no off-the-shelf model on Qwen, OpenRouter, or DashScope performs calibrated
Mandarin tone scoring (see `docs/07-speech-assessment-models.md`). What modern multimodal
audio models *are* good at is comparing two clips and describing the difference in
natural language.

## Decision

For each phrase, give the learner comparative, AI-generated coaching rather than a score:

1. **Reference audio** - generated with Kokoro-82M from the phrase's pinyin and bundled
   per phrase (ADR 0006).
2. **Attempt audio** - the learner records themselves.
3. Send **both clips in one multimodal request** - two base64 `input_audio` parts plus a
   structured prompt (including the expected phrase) - via the Cloudflare Worker to
   Muse Spark, and ask it to compare tone, rhythm, and pronunciation and return concise,
   actionable feedback.
4. There is **no separate ASR stage**: Muse Spark transcribes and reasons about the audio
   in the same call.
5. Present the result as **coaching, not a grade**. It is advisory and may vary between
   runs; the product accepts this.

The model sits behind the Worker (ADR 0003) so it can be swapped. The intended choice is
**`meta/muse-spark-1.3-contributor`** via OpenRouter - multi-modal with a ~1M context. It
**accepts audio input** and is **assumed to understand Chinese** and reason about Mandarin
speech. Latency and cost have been judged acceptable, and the provider terms permit
sending learner recordings.

ADR 0014 amends this decision: the request also carries a compact, on-device measured
per-syllable tone-evidence object (F0/voicing/loudness, DTW alignment, speaker-relative
normalization, deterministic tone comparison). The output contract and the coaching-only
principle are unchanged.

## Consequences

- No calibrated tone score exists; feedback is qualitative, non-deterministic, and must
  never be shown as pass/fail or a numeric grade.
- The prompt is part of the product: it must produce short, specific, encouraging output
  and be versioned. Use low temperature, a fixed response schema, and the expected phrase
  as context.
- Cost and latency scale with audio duration, so attempts are short (target under ~15s)
  and reference audio analysis can be cached per phrase.
- Audio leaves the device, so explicit, revocable consent is required (NFR-4/NFR-5).
- Reference audio must exist for every phrase; Kokoro-82M generates it at build time.
- Alternative considered and deferred: a calibrated assessment API (SpeechSuper,
  Chivox, Azure Pronunciation Assessment) for validated scores. It can be added later
  without changing the UX contract.
