# Plan: ground WF-1 with measured tone evidence

Status: implemented. Docs-only changes; no application code yet (AGENTS.md: planning repo).

## Directive

`docs/09-opensmile-acoustic-features.md` found that WF-1's accuracy is limited by asking a
multimodal LLM to judge pitch from raw audio, and recommended measured per-syllable
evidence as the fix. This plan turns that into decisions: accept the architecture and the
WF-1 input change now (ADR 0014), and put the extractor choice and the accuracy claim
behind a cheap, ordered M3 validation gate before any native dependency is adopted.

## Recommendation

1. **Ground WF-1, keep the contract.** WF-1 keeps sending both audio clips and keeps its
   coaching-only output schema (ADR 0005); its input gains a compact, versioned
   per-syllable `acousticEvidence` object. The LLM never measures and never receives raw
   contours - only named observations (direction, span, onset/offset, voicing, duration,
   relative loudness, note).
2. **Deterministic pipeline on-device:** extract F0/voicing/loudness -> DTW-align the
   attempt against the reference -> normalize to a speaker-relative five-level scale
   (T-value method) -> compare against the expected tone -> emit the evidence object.
   DTW first; a forced aligner stays an optional fallback, not the default (no extra
   model call on the critical path).
3. **Validation order (cheap first).** Doc 09 builds the Android AAR before the accuracy
   claim is tested; reorder so the expensive native work happens only for the winning
   arm:
   1. Desktop extraction spike (openSMILE Python) on Kokoro reference clips and recorded
      attempts - including creaky tone 3 and noisy input.
   2. Pipeline prototype (DTW + normalization + tone comparison); sanity-check against a
      human listener.
   3. A/B: the same attempts through WF-1 with and without the evidence object, fixed
      prompt and model. This is the adoption gate.
   4. Feature ablation: full openSMILE breadth vs F0/voicing/loudness alone.
   5. Android integration only for the winner; measure native/library size and latency.
4. **Extractor default.** The evidence object needs only F0/voicing/loudness, so the
   default expectation is a small pure-Kotlin extractor (no NDK; one implementation used
   both in the app and in a build-time task that precomputes reference features). Adopt
   the openSMILE AAR only if the ablation shows its extra features - voice quality,
   formants, spectral detail - materially improve feedback; that choice also locks the
   project to private use under the audEERING Research License. This refines doc 09's
   "openSMILE preferred" recommendation: the preference is conditional on the ablation.
5. **Reference data at build time.** Kokoro gives no timestamps: precompute reference
   evidence and syllable boundaries at content-build time for the curated set (small,
   one-time author review); generated items align by DTW, confirmed in the spike.
6. **Degrade safely.** Extraction failure or a slow device falls back to today's
   audio-only WF-1 request. On-device DSP is deterministic and off the AI budget, so
   NFR-1 is unaffected.
7. **Rejected:** openSMILE in the Worker (Wasm; option C in doc 09), raw feature dumps in
   the prompt, forced alignment on the default path.

## Decision to record (ADR 0014)

New `docs/05-decisions/0014-grounded-pronunciation-feedback.md` (template per 0001):

- Status: **accepted**. The direction is decided now; the M3 gate is written into the
  consequences, and a failed A/B supersedes the ADR.
- **Context:** ADR 0005 chose soft, uncalibrated comparison; speech-LLM benchmarks (MSPB,
  PitchBench) show pitch/prosody judgment from raw audio is unreliable; the fix is
  measurement, not more prompt engineering (doc 09).
- **Decision:** WF-1 input gains versioned, per-syllable measured evidence, computed
  deterministically on-device; audio still sent; output schema and coaching-only contract
  unchanged; extractor and exact schema selected by the M3 spike (decision rule above).
- **Consequences:** amends ADR 0005; the openSMILE AAR path carries the private-use
  license lock and native build burden; NFR-4/NFR-5 unchanged (derived voice data, same
  request); deterministic tone history could later feed WF-5 themes (separate decision);
  superseded if the M3 A/B shows no measurable gain.
- **Amends:** 0005-reference-vs-attempt-feedback.md.

## Planned file changes

1. `docs/05-decisions/0014-grounded-pronunciation-feedback.md` (new) - as above.
2. `docs/05-decisions/0005-reference-vs-attempt-feedback.md` - status line: accepted
   (amended by 0014); one sentence pointing forward.
3. `docs/08-ai-workflows.md` - WF-1 input gains `acousticEvidence` (sketch per doc 09,
   schema versioned with the workflow); Notes: on-device deterministic evidence, named
   observations only, no raw contours, audio still sent; fallback degrades to today's
   audio-only request.
4. `docs/02-architecture.md` - stack row for the evidence pipeline; `:core:assessment`
   owns extraction, alignment, normalization, and tone comparison; WF-1 pipeline diagram;
   audio-handling note (deterministic DSP, not on-device AI inference - consistent with
   ADR 0003); deferred list gains "openSMILE in the Worker (Wasm)".
5. `docs/01-requirements.md` - FR-8 gains the measured-evidence clause; confirm the
   no-separate-ASR out-of-scope line still holds (DTW alignment is not ASR).
6. `docs/04-roadmap.md` - M0 checked line for ADR 0014; M1 note that the WF-1 request
   model includes the optional evidence field; M3 adds the five validation tasks and the
   finalize-schema task.
7. `docs/07-speech-assessment-models.md` - one sentence in the chosen-approach paragraph
   pointing to ADR 0014 (cross-link already present).
8. `docs/09-opensmile-acoustic-features.md` - status line: direction adopted (ADR 0014);
   extractor deferred to the M3 spike. Research body unchanged.
9. `README.md` - no change needed (09 already listed; table stays in sync).
10. `docs/03-design.md` - no change needed (feedback UX wording is unchanged).

## Open questions (non-blocking)

- Reference syllable boundaries: build-time precomputation with author review is the
  proposal; confirm in the spike whether DTW alone suffices.
- Octave errors and tone-3 creak are the known pitch-tracking failure modes to test.
- Whether deterministic tone-error history should feed WF-5 progress themes later.
- What triggers the A/B "measurably better" threshold: proposed on a fixed attempt set,
  grounded feedback names the correct weak tone more often than ungrounded, judged by the
  author.

## Validation

- ADR numbering sequential, 0001 template followed, cross-references resolve (0005
  amended by 0014; 0003, 0006, 0009, 0011, 0012 consistent).
- No application code; all changes under `docs/`; no new build/lint/test commands.
- No hanzi anywhere; pinyin with tone numbers in all schemas (ADR 0012).
