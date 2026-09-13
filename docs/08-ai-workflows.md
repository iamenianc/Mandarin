# AI workflows

Status: proposed. Each AI task is a **separate, named workflow** with its own prompt,
model, typed input, fixed output schema, latency budget, and fallback (ADR 0009).
Workflows never share a prompt. They run behind the Cloudflare Worker (ADR 0003), which
exposes one versioned endpoint per workflow and owns provider selection.

## Principles

- **One task, one workflow.** Pronunciation feedback is not conversation; a summary is not
  a transcription. Do not overload a workflow.
- **Pinyin is the language of pronunciation.** Pronunciation-related output names pinyin
  syllables and tone numbers (`ma2`); no workflow ever emits hanzi (ADR 0011, ADR 0012).
- **Minimum necessary input.** A workflow receives only what its task requires. Raw learner
  audio goes only to workflows that must analyze it; summaries get aggregated metadata.
- **Fixed output schema.** Every workflow returns a typed, validated structure so the app
  never parses free-form text.
- **Per-workflow budgets.** Each workflow has its own latency and cost budget tied to the
  NFR it serves.
- **Graceful fallback.** Every workflow names the offline or scripted behavior used when
  the Worker or provider is unavailable.

## Workflow registry

| ID | Workflow | Module(s) | Model | Trigger | Latency |
| --- | --- | --- | --- | --- | --- |
| WF-1 | Pronunciation feedback | Speech, Tones, Vocabulary | Muse Spark (audio in) | Learner finishes an attempt | NFR-1 (~2s) |
| WF-2 | Conversation turn | Conversation | Muse Spark (audio in) | Learner finishes a turn | NFR-2 (~3s) |
| WF-3 | Speech synthesis | All (audio), Conversation | Kokoro-82M | Reference clip needed; AI reply ready | Playback-bound |
| WF-4 | Response transcription | Listening | STT endpoint (Whisper / GPT-4o-transcribe / Qwen3-ASR) | Learner answers a drill by speaking | ~1–2s |
| WF-5 | Progress summary | Progress | Muse Spark (text only) | Session or week ends | Off critical path |
| WF-6 | Content authoring (build-time) | None (build) | Text LLM | Author drafts a module's content | N/A |
| WF-7 | Raymond - Mandarin Q&A | Raymond | Muse Spark | Learner asks a question about Mandarin | ~2–3s |
| WF-8 | Exercise generation (runtime) | Tones, Vocabulary, Listening, Fundamentals | Muse Spark | Bundled set exhausted or learner wants more | ~2–4s |
| WF-9 | Field mission generation | Field loop (all module content) | Muse Spark | Daily loop starts; learner wants a new mission | ~2–4s |
| WF-10 | Local conversation turn | Field loop | Muse Spark (audio in) | Learner finishes a turn with a local | NFR-2 (~3s) |

## WF-1 - Pronunciation feedback

- **Purpose:** compare the learner's attempt with the reference and return short,
  encouraging coaching.
- **Input:** reference clip (cached per phrase), learner attempt clip, expected
  `pinyin` + `targetTones`, learner level, and `acousticEvidence` - a compact, versioned
  per-syllable object (`expectedTone`, observed direction/span, onset/offset levels,
  voicing ratio, duration, relative loudness, note) measured deterministically on-device
  (ADR 0014; shape in `docs/09-opensmile-acoustic-features.md`).
- **Output schema:** `{ weakestUnit, issue, tip, encouragement, replayHint }` (pinyin
  syllables with tone numbers only; never hanzi); unchanged by ADR 0014.
- **Notes:** two base64 `input_audio` parts plus a structured prompt (ADR 0005); no ASR
  stage; low temperature; advisory only. The model receives named per-syllable
  observations, never raw F0 frames; the evidence schema is versioned with the workflow
  and validated like any payload. If extraction fails, the request degrades to the
  audio-only input.
- **Fallback:** replay reference and attempt side by side with a stored generic tip.

## WF-2 - Conversation turn

- **Purpose:** understand the learner's spoken turn and produce the coach's next reply.
- **Input:** scenario, conversation state (prior transcripts and corrections), learner
  audio, target difficulty.
- **Output schema:** `{ replyText, gentleCorrection?, nextPrompt? }`.
- **Notes:** streaming preferred to hit NFR-2; corrections are gentle and optional; reply
  text is then voiced by WF-3.
- **Fallback:** a scripted scenario line so the exchange keeps moving offline.

## WF-3 - Speech synthesis

- **Purpose:** turn pinyin text into reference and reply audio.
- **Input:** `pinyin` with tone numbers (for reference clips) or `replyText` (for WF-2 and
  WF-10 replies); voice and language config.
- **Output:** 24 kHz mono audio.
- **Notes:** reference clips are generated **at build time** from pinyin and bundled, so
  they need no network (ADR 0006); runtime AI replies are synthesized via the Worker.
- **Fallback:** reference clips are always local; if a reply cannot be synthesized, show
  the reply text (English/pinyin) and continue.

## WF-4 - Response transcription

- **Purpose:** interpret a spoken answer to a listening drill.
- **Input:** short learner clip, the drill's expected options/keywords.
- **Output schema:** `{ transcript, matchedOptionId?, confidence }`.
- **Notes:** the model returns a transcript; the app's local matcher decides the answer so
  drills still work when only transcription is available. Distinct from WF-1: no
  reference comparison, no coaching.
- **Fallback:** tap-only drills, which are always available offline.

## WF-5 - Progress summary

- **Purpose:** describe practice history and recurring feedback themes in a short spoken
  summary.
- **Input:** aggregated metadata only - attempt counts, per-unit feedback themes, module
  and lesson ids, and mission debrief themes. **No raw audio, no full transcripts.**
- **Output schema:** `{ summaryText, focusAreas[] }`, then voiced by WF-3.
- **Notes:** off the critical path; may run on session end or on demand.
- **Fallback:** a canned summary rendered from the same counts without the model.

## WF-6 - Content authoring (build-time)

- **Purpose:** help the author draft lessons, phrase lists, glosses, and example items.
- **Input:** author brief and theme; no learner data.
- **Output:** draft content for **human review** before it is committed to a module.
- **Notes:** runs at authoring time, never in the app; its output is reviewed and then
  treated as authored data, with reference audio generated by WF-3.
- **Fallback:** hand-authored content.

## WF-7 - Raymond (Mandarin Q&A)

- **Purpose:** answer **any question the learner has about Mandarin** - pronunciation,
  tones, pinyin conventions, what a word or phrase means, how it is used, and light
  cultural context. Raymond is a named, always-available helper, not a lesson or a
  role-play.
- **Input:** the learner's question (English or pinyin text; spoken questions accepted),
  recent Q&A history, and the learner's level.
- **Output schema:** `{ answerText, examples[] (pinyin + meaning), followUps[] }`; answers
  may be voiced by WF-3.
- **Notes:**
  - **Bounded scope:** Raymond answers about the Mandarin language and its use. It declines
    or redirects questions outside that scope rather than acting as a general assistant.
  - **Pinyin only:** examples are pinyin plus meaning; **never hanzi** (ADR 0002).
  - **Distinct from WF-2:** WF-2 is a scenario role-play that drives a lesson forward;
    WF-7 answers the learner's own questions in a free-form chat. They do not share a
    prompt or conversation state.
  - No mandatory audio: Raymond receives the learner's text unless they choose to speak.
- **Fallback:** an offline note explaining Raymond needs a connection, plus the bundled
  fundamentals content that covers the same ground.

## WF-8 - Exercise generation (runtime)

- **Purpose:** extend or continue a lesson with fresh examples when the bundled set is
  exhausted or the learner wants more - making practice effectively limitless. This is the
  runtime counterpart to build-time authoring (WF-6).
- **Input:** module id, requested item type (`word` / `phrase` / `minimalPair` /
  `dialogue`), theme, target units (initials/finals/tones), difficulty, the learner's
  recurring feedback themes, and a requested count.
- **Output schema:** `{ items[] }`, where each item is
  `{ type, meaning, pinyin, hangul?, targetTones, distractors[], rationale }`.
- **Validation before use:** schema-valid, `pinyin` present with tone numbers, **no hanzi**,
  known item type, deduplicated against bundled and previously generated content, and
  matching the requested module/theme/difficulty. Invalid items are dropped.
- **Notes:**
  - Generated items are **labeled as extra**, not curated; the learner can always return to
    the bundled set. They may be cached locally for replay offline.
  - Reference audio is produced by WF-3.
  - Distinct from WF-6: WF-8 never edits or replaces the shipped content set.
- **Fallback:** repeat and vary bundled items; no generation without a connection.

## WF-9 - Field mission generation

- **Purpose:** produce one day's bite-sized functional exchange plus the five locals the
  learner will use it with (ADR 0013).
- **Input:** theme and module context, covered content, learner level, recent debrief
  themes, requested exchange length.
- **Output schema:** `{ script, locals[] }`: the script is a short ordered list of turns
  (`pinyin` + `meaning` + `targetTones`); `locals` has exactly five entries (`label`,
  `settingRole`, `personality`, `voiceProfile`, `pace`).
- **Validation before use:** same bar as WF-8 - schema-valid, pinyin with tone numbers,
  **no hanzi**, on-theme and level-appropriate, five distinct locals, deduplicated against
  bundled scripts; invalid generations are dropped.
- **Notes:** bundled mission scripts remain the offline fallback. WF-8 stays item-level
  exercise generation; WF-9 generates a complete mission. Reference audio comes from WF-3.
- **Fallback:** a bundled script and bundled persona set.

## WF-10 - Local conversation turn

- **Purpose:** respond as one simulated local so the learner can hold the day's exchange
  with varied listeners.
- **Input:** local persona, mission script and goal, conversation state, learner audio,
  target difficulty (pace and comprehension).
- **Output schema:** `{ replyText, understandingSignal?, nextLocalPrompt? }` (pinyin and
  English only; never hanzi).
- **Notes:** the local stays in character and **does not correct** - coaching stays in WF-1
  and the debrief; reply text is voiced by WF-3. Distinct from WF-2: WF-2 is a coaching
  conversation that drives a lesson forward; WF-10 role-plays a member of the public.
- **Fallback:** scripted local lines from the bundled mission script.

## Adding a workflow

1. Give it an id, purpose, inputs, fixed schema, budget, and fallback.
2. Add it to the registry above and to `docs/02-architecture.md`.
3. Implement it behind `:core:ai` and add its Worker endpoint; do not extend an existing
   workflow to cover a new task.
