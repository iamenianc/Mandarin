# Speech assessment models (Qwen)

Status: research note. Question: can the latest Qwen models evaluate speech quality -
especially Mandarin pronunciation and tone - as the basis for student feedback?

## Summary

**No Qwen model performs pronunciation or tone *scoring* today.** Qwen has strong
open speech components - recognition, forced alignment, omni-modal understanding, and
TTS - but none of them is a calibrated GOP-style (Goodness of Pronunciation) assessor
like Azure Speech Assessment, SpeechAce, SpeechSuper, or Chivox. Qwen3-ASR transcribes;
Qwen3-ForcedAligner localizes units in time; Qwen3-Omni understands/describes audio;
Qwen3-TTS synthesizes speech. None outputs a per-syllable or per-tone correctness score.

Tone is the specific gap. ASR language modeling tends to "correct" a wrong tone into a
plausible homophone, so a transcript alone is an unreliable tone signal. The same holds
across the OpenRouter catalog (see below): audio understanding, not calibrated scoring.

## Relevant models

| Model | Size | Task | Mandarin | Timestamps | Pronunciation scoring |
| --- | --- | --- | --- | --- | --- |
| Qwen3-ASR-1.7B / 0.6B | ~2B / ~0.8B | ASR + language ID, offline & streaming, 30 languages + Chinese dialects | Yes | No (use aligner) | No |
| Qwen3-ForcedAligner-0.6B | ~0.9B | Text-speech forced alignment over "arbitrary units", up to ~5 min | Yes (11 languages) | Word/unit-level spans | No (timing only) |
| Qwen3-Omni-30B-A3B (Instruct / Thinking / Captioner) | 30B MoE, ~A3B active | Audio/video understanding, ASR, speech translation, real-time voice chat, TTS | Yes (speech input) | None documented | No (can describe audio qualitatively) |
| Qwen3-TTS-0.6B / 1.7B | ~0.6B / ~1.7B | Speech synthesis, cloning, voice design | Yes | N/A | N/A - generation only |

All of the above are Apache-2.0 with open weights; Omni is also available via Alibaba
DashScope / Model Studio. Licenses and sizes should be re-verified against the model
cards before relying on them.

## What each could contribute

- **Segmentation and fluency/pace:** Qwen3-ASR gives the transcript, and
  Qwen3-ForcedAligner gives syllable/character spans - enough to measure rate, pauses,
  and where a learner's speech aligns with the expected phrase.
- **Reference audio:** not a Qwen dependency here - reference clips are generated with
  Kokoro-82M (ADR 0006). Qwen3-TTS remains a larger alternative if more expressive
  Mandarin voices are needed.
- **Qualitative feedback only:** Qwen3-Omni (especially the Captioner) can be prompted
  to describe an accent or a recording in words ("the second syllable sounds flat").
  This is uncalibrated, hallucination-prone, and has no published accuracy benchmark -
  it is not a substitute for a score.

## Approaches surveyed

These are the options considered; ADR 0005 chose the soft comparison path (below).

1. Use Qwen3-ASR + Qwen3-ForcedAligner for **transcription, segmentation, and
   fluency/pace** metrics.
2. Compute **tone and pronunciation scoring in-house**: compare an F0 (pitch) contour
   per syllable against reference tone templates, plus a GOP-style phoneme comparison.
   This is the part that actually drives useful feedback.
3. For validated per-syllable/tone scores, consider a purpose-built Mandarin assessment
   API (SpeechSuper and Chivox both cover Mandarin at phoneme/syllable/tone level;
   Alibaba's separate Intelligent Speech Interaction oral-assessment API also exists,
   outside the Qwen lineup). These are complementary to, not replaced by, Qwen.
4. Reference audio is generated with **Kokoro-82M** (ADR 0006); Qwen3-TTS is a larger
   alternative. Qwen3-Omni can provide optional natural-language coaching commentary,
   clearly labeled as softer feedback.

**Chosen approach (ADR 0005):** deliberately take the soft option - send the reference
audio and the learner's attempt to a multimodal model and ask it to compare them and
coach. There is **no separate ASR stage**; Muse Spark transcribes and reasons about the
audio in the same call. No calibrated tone score is produced, by product decision.

## Risks and open questions

- Multimodal feedback is uncalibrated and may vary between runs; keep output as coaching,
  never a pass/fail judgment.
- Model "understanding" of a recording can be wrong or overconfident; consider embedding
  the expected phrase and a fixed response schema to constrain it.
- Cost/latency of the comparison path must stay within the budgets in
  `docs/01-requirements.md`.
- The upstream STT/model providers are outside the project's control; the Worker must fail
  gracefully to offline drills.

This research note supports ADR 0005 and the chosen stack; see `docs/05-decisions/` and
`docs/02-architecture.md`. For the follow-up research note on grounding feedback with
measured acoustic features (openSMILE), see `docs/09-opensmile-acoustic-features.md`; the
direction is adopted in ADR 0014 (extractor choice pending the M3 spike).

## OpenRouter

Researched 2026-09-12 against the OpenRouter catalog (`/api/v1/models`, ~445 models).
OpenRouter exposes audio input and speech-to-text, but - like Qwen directly - **no
model on OpenRouter performs calibrated tone or pronunciation scoring**.

- **Audio input to chat models:** supported via `input_audio` in `/chat/completions`.
  Base64 only; direct audio URLs are not supported.
- **Audio-native / audio-out models:** `openai/gpt-audio`, `openai/gpt-audio-mini`
  (audio in, text+audio out; streaming required).
- **Intended comparison model for this project:** `meta/muse-spark-1.3-contributor`
  (multi-modal, ~1M context, accepts audio input -> text). Assumed to understand Chinese.
  Latency and cost judged acceptable; provider terms permit sending learner recordings.
- **General multimodal LLMs with audio input:** the current Gemini family
  (`google/gemini-3.x-*`, `google/gemini-2.5-*`), `mistralai/voxtral-small-24b-2507`,
  `xiaomi/mimo-v2.5`, `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free`, and similar.
- **Dedicated speech-to-text endpoint:** `POST /api/v1/audio/transcriptions` hosts
  Whisper, GPT-4o-transcribe, Qwen3-ASR (0.6B/1.7B/flash), Deepgram Nova-3, Google
  Chirp-3, NVIDIA Parakeet, and others. Text output (optional timestamps); no scoring.

Audio-native LLMs can *describe* audio ("this sounds like a second tone"), but that is
non-deterministic, uncalibrated understanding - not a measurement. No OpenRouter model
returns an F0 contour, GOP score, or tone-accuracy number, and none is a substitute for
a validated assessment engine.

**Practical constraints:** base64 encoding required (client-side, ~33% size overhead);
multipart STT cap ~25 MB; upstream transcription timeout ~60 s per request; audio output
requires streaming SSE; LLM audio round-trips add cost and latency that limit real-time
feedback.

Bottom line: OpenRouter is a viable *transport* for ASR and for soft AI coaching
commentary (and a good way to avoid shipping provider keys), but it does not solve tone
scoring. A calibrated engine (Azure Pronunciation Assessment, SpeechSuper, Chivox)
remains the only source of trustworthy tone scores.

## Sources

- Qwen3-ASR-1.7B / 0.6B model cards: https://huggingface.co/Qwen/Qwen3-ASR-1.7B-hf,
  https://huggingface.co/Qwen/Qwen3-ASR-0.6B-hf (technical report arXiv:2601.21337)
- Qwen3-ForcedAligner-0.6B model card: https://huggingface.co/Qwen/Qwen3-ForcedAligner-0.6B-hf
- Qwen3-Omni repository and report: https://github.com/QwenLM/Qwen3-Omni,
  https://arxiv.org/pdf/2509.17765
- Qwen3-TTS: https://github.com/QwenLM/Qwen3-TTS
- Qwen model listing: https://huggingface.co/Qwen
- Mandarin assessment vendors: https://www.speechsuper.com/, https://www.chivox.com/en/products/mandarin-chinese-assessment
- OpenRouter catalog: https://openrouter.ai/api/v1/models
- OpenRouter audio and STT docs: https://openrouter.ai/docs/guides/overview/multimodal/audio,
  https://openrouter.ai/docs/guides/overview/multimodal/stt
