# 0019 - On-device reference tone features

- Status: accepted
- Date: 2026-09-14

## Context

ADR 0014 expects the small pure-Kotlin extractor (F0, voicing, loudness) to be used both in
the app and in a build-time task that precomputes reference features. Two constraints make
the build-time half impractical right now:

- The extractor lives in `:core:assessment`, an Android library. A Gradle task cannot host
  its classes without splitting the module or adding a JVM tool, and no other feature needs
  that split.
- A bundled reference clip is a single authored audio file with no syllable timings, and the
  content schema does not carry them, so reference features cannot be produced from content
  alone.

The speech drill already sends the reference and attempt clips to WF-1 with a seam
(`AttemptEvidenceSource`) that returns no measured evidence.

## Decision

- Compute reference features **on device** from the bundled reference clip and cache them per
  content item for the process lifetime.
- Recover syllable spans from the clip's own energy contour with a deterministic
  `SyllableAutoSegmenter`, using the expected tones from the item pinyin. Splitting lands on
  the deepest energy valleys; fewer voiced regions than syllables falls back to an even
  split.
- Decode the reference clip with a `ReferencePcmDecoder` seam: `MediaExtractor` +
  `MediaCodec` for OGG (Opus/Vorbis), MP3, and M4A, and the repository WAV helper for WAV.
- Run the existing `ToneAssessmentPipeline`, map `ToneEvidence` to the WF-1
  `AcousticEvidence` payload, and return `null` whenever any step is missing or fails, so
  the audio-only request and the offline fallback are unchanged.
- Keep the pure-Kotlin `BaselineAcousticFeatureExtractor` as the extractor. The openSMILE
  validation oracle, the feature ablation, and the grounded-vs-ungrounded WF-1 A/B remain
  open follow-ups per ADR 0014; auto-segmentation accuracy is unvalidated until then.

## Consequences

- No module split or build-tool host is needed, and measured evidence works offline.
- The first attempt on an item pays a decode and extraction cost; later attempts reuse the
  cached reference features.
- The seam is unchanged, so a content-build precompute can replace the on-device path later
  without touching the drill engine.
- Accuracy depends on energy-based segmentation, which is weakest on connected speech; the
  ADR 0014 oracle is the intended way to validate or replace it.
