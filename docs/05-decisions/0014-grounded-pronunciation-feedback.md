# 0014 - Ground pronunciation feedback with measured tone evidence

- Status: accepted
- Date: 2026-09-13
- Amends: 0005-reference-vs-attempt-feedback.md

## Context

ADR 0005 compares the reference clip with the learner's attempt in one multimodal request
and returns coaching. That works for qualitative, segmental feedback, but it asks the
model to judge tone and prosody from raw audio - an inference that speech-LLM benchmarks
show is unreliable (MSPB, Interspeech 2025; PitchBench, 2026; see
`docs/09-opensmile-acoustic-features.md`). Tone is the specific gap: the model can
describe a contour, but it does not measure one, and the app already knows the expected
tone of every pinyin syllable.

Deterministic DSP measures exactly the missing signal: F0, voicing, and loudness. What
has been missing is a pipeline that turns those measurements into per-syllable evidence
the coaching prompt can trust.

## Decision

WF-1's input gains a compact, versioned **`acousticEvidence`** object, computed
deterministically **on-device**, alongside the two audio clips:

1. **Extract** F0, voicing probability, and loudness contours from the attempt (and from
   the reference at content-build time).
2. **Align** the attempt to the reference with dynamic time warping (DTW) over the
   contours, yielding per-syllable spans. A forced aligner is a fallback only, never the
   default: it adds a model call to the critical path.
3. **Normalize** pitch to a speaker-relative five-level scale (the T-value method) so the
   comparison is relative to the learner's own range.
4. **Compare** each syllable's normalized contour against its expected tone (1: level
   high; 2: rising; 3: dip; 4: falling) and emit named observations: direction, span,
   onset/offset levels, voicing ratio, duration, and loudness relative to the rest of the
   phrase.

Each syllable in the evidence object carries its expected tone plus those observations -
no raw frame arrays. The LLM's role narrows to selecting the weakest unit and phrasing
coaching from facts it is given; it does not measure and does not receive contour dumps.
Audio is still sent so the model keeps segmental context. The WF-1 output schema and the
coaching-only contract are unchanged (ADR 0005): coaching, never a grade.

**Extractor.** The default expectation is a small pure-Kotlin extractor (F0, voicing,
loudness) with no NDK dependency, used both in the app and in a build-time task that
precomputes reference features. openSMILE's desktop tooling is the validation oracle
during the M3 spike. The openSMILE Android AAR is adopted only if the spike's feature
ablation shows that its additional features (voice quality, formants, spectral detail)
materially improve feedback; it carries a native build and maintenance burden and the
audEERING Research License, which permits personal, non-commercial use but requires a
commercial license for use in any product - locking the project to private use
(`docs/09`, options A-D).

**Validation gate.** The M3 spike runs a desktop extraction pass, a pipeline prototype,
and an A/B of WF-1 with and without the evidence object (fixed prompt and model), plus
the feature ablation. If the grounded variant is not measurably better, this decision is
superseded and WF-1 reverts to audio-only comparison.

## Consequences

- Amends ADR 0005: same comparison concept and output contract, richer input.
- Feedback becomes more specific where it was weakest (tone), and deterministic
  measurements ground the coaching text; the model cannot invent a contour.
- Tone-error history becomes deterministic data, which could later feed WF-5 progress
  themes (separate decision).
- Extraction is milliseconds of DSP, not AI inference: NFR-1 is unaffected, the
  cloud-only AI principle (ADR 0003) still holds, and evidence can be computed offline -
  only the coaching call needs the network.
- Extraction failure or a slow device degrades gracefully to the audio-only request.
- Derived features are voice-derived data sent in the same request as the audio;
  the NFR-4/NFR-5 posture is unchanged.
- The evidence schema is versioned with WF-1 and validated like any workflow payload.
- Alternatives considered: prompting the LLM harder over raw audio (rejected - the
  benchmarks show the perception layer is the limit); openSMILE in the Worker via Wasm
  (rejected - no official build, extra decode and latency for no accuracy gain); sending
  raw contour frames to the LLM (rejected - unreliable numeric reasoning); on-device
  fine-tuning (rejected - cloud-only, no training pipeline); a calibrated assessment API
  (still deferred, ADR 0005).

Sources: `docs/09-opensmile-acoustic-features.md`; ADR 0005; ADR 0006;
`docs/07-speech-assessment-models.md`.
