# Requirements

Status: draft. Requirements use `FR-` (functional) and `NFR-` (non-functional)
identifiers so they can be referenced from ADRs and issues. The guiding constraint
is **audio-first: listening and speaking only**. Phonetic aids are the one allowed
text channel, and **pinyin is taught, not just shown**: understanding it is the
learner's key to pronunciation (ADR 0011). The learner is never required to read hanzi
or write characters.

## Functional requirements

| ID | Requirement | Priority |
| --- | --- | --- |
| FR-1 | The app shall present short guided audio lessons for beginners. | Must |
| FR-2 | The app shall play reference audio for each phrase (Kokoro-generated), replayable at any time. | Must |
| FR-3 | The app shall always display Hanyu Pinyin for each phrase with tone numbers, aligned with its audio (ADR 0012). | Must |
| FR-4 | The app shall offer Hangul as an optional phonetic aid (the user reads Hangul), remembered across sessions. | Should |
| FR-5 | The app shall display pinyin at all times; Hangul, when enabled, appears alongside it. | Must |
| FR-6 | The app shall support zhuyin (bopomofo) as an alternative phonetic aid. | Won't (v1) |
| FR-7 | The app shall record the learner's speech via the device microphone. | Must |
| FR-8 | The app shall send the reference and the attempt to a multimodal model, together with measured per-syllable tone evidence, and return specific, encouraging feedback on pronunciation, tone, and rhythm (ADR 0014). | Must |
| FR-9 | The app shall let the learner play back their recording next to the reference audio. | Must |
| FR-10 | The app shall run listening drills that require no speaking (hear and respond). | Must |
| FR-11 | The app shall provide AI-assisted conversation practice that adapts to the learner. | Must |
| FR-12 | The app shall track practice history and surface recurring feedback themes. | Should |
| FR-13 | The app shall offer an eyes-free / audio-only mode for a full session. | Should |
| FR-14 | The app shall support hands-free, turn-based conversation with the AI coach. | Should |
| FR-15 | The app shall bundle a starter set of lessons usable offline (no AI, no network). | Should |
| FR-16 | The app shall sync progress across devices via an account. | Won't (v1) |
| FR-17 | Every on-screen word shall be in English or pinyin; no hanzi is ever displayed. | Must |
| FR-18 | The app shall present learning as **modules**, each offering **lessons** and **practice**. | Must |
| FR-19 | The app shall ship an initial set of modules: **tones, vocabulary, listening, speech, and fundamentals**. | Must |
| FR-20 | The app shall teach **fundamentals and theory** (syllable anatomy, pinyin sound system with tone numbers, tone sandhi, rhythm) by ear. | Must |
| FR-21 | The app shall provide **audio-first vocabulary** lessons and practice over beginner/tourist/survival themes; no hanzi and no character browsing. | Must |
| FR-22 | The app shall discover and register modules so a new module can be added without changes to the app shell, navigation, or audio/AI pipeline. | Must |
| FR-23 | Each AI task shall be a separate, clearly defined workflow with its own prompt, model, typed input, fixed output schema, latency budget, and fallback (ADR 0009). | Must |
| FR-24 | A workflow shall receive only the inputs its task requires; raw learner audio shall not be sent to workflows that do not analyze audio (e.g. progress summaries). | Must |
| FR-25 | The app shall provide **Raymond**, a general chat workflow that answers any question the learner has about Mandarin (pronunciation, tones, meaning, usage, light culture), with pinyin examples and no hanzi. | Must |
| FR-26 | The app shall ship a curated set of preloaded exercises usable offline, and an AI flow shall generate additional validated examples on demand so practice can continue without limit. | Must |
| FR-27 | The app shall explicitly teach the pinyin sound system (initials, finals, tone numbers, spelling conventions, tone sandhi in context) as the learner's key to pronunciation (ADR 0011). | Must |
| FR-28 | The app shall let the learner practice pinyin-to-sound mapping: reading shown pinyin aloud against reference audio, and choosing the pinyin that matches what is heard. | Must |
| FR-29 | The app shall ship a dedicated **tones** module, taught to a beginner ahead of words, covering the four tones and the neutral tone, their contours, tone pairs, and the tone-number notation, with discrimination and production practice (ADR 0012). | Must |
| FR-30 | The app shall organize daily practice as the **Daily LAMP field loop**: Script Rehearsal, Field Mission, Debrief (ADR 0013). | Must |
| FR-31 | The app shall generate (or, offline, select from bundled content) one bite-sized functional exchange for script rehearsal, with reference audio and pinyin. | Must |
| FR-32 | The Field Mission shall have the learner speak the exchange with five simulated AI locals in turn - in-app voice personas, each distinct, responding in character and not coaching - with no real-world interlocutor required. | Must |
| FR-33 | The Debrief shall let the learner log unfamiliar words and tone breakdowns from the mission, stored as pinyin with tone numbers and no hanzi. | Must |
| FR-34 | Debrief entries shall surface as recurring themes in progress and feed later mission and exercise generation. | Should |

## Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-1 | Feedback latency: end of speech to feedback under ~2 seconds typical. |
| NFR-2 | Conversation turn latency: learner stops speaking to AI reply under ~3 seconds. |
| NFR-3 | Core listening drills and repeat-after-audio work fully offline. |
| NFR-4 | Voice recordings are sent to the AI provider only to service the request and are not retained by the app beyond local storage; one-tap delete exists. |
| NFR-5 | AI processing is permitted to receive learner recordings under the chosen provider terms; no training use by default. |
| NFR-6 | Robust in noisy environments; clear guidance and graceful failure when audio is unusable. |
| NFR-7 | Accessible: TalkBack labels, dynamic volume/font scaling for the (minimal) UI text. |
| NFR-8 | Support Android 8.0 (API 26) and above. |
| NFR-9 | No account required; everything works locally except AI calls. |
| NFR-10 | AI-generated exercises and field mission scripts are schema-validated, pinyin-only (no hanzi), deduplicated, and labeled as extra; invalid items are dropped and the app falls back to bundled content. |

## Out of scope

- Reading or writing hanzi; stroke order; handwriting input.
- Teaching Hangul; Hangul is used only as a phonetic aid for learners who already read it.
- Zhuyin (bopomofo) support; Hanyu Pinyin and Hangul are the phonetic aids in v1.
- Reading pinyin as an end in itself (a transliteration or reading-fluency curriculum);
  pinyin is taught as the pronunciation map (ADR 0011).
- Written quizzes, grammar text, or any character-based (hanzi) vocabulary browsing or
  reading curriculum; vocabulary is audio-first, heard and spoken, with pinyin as the
  always-visible pronunciation key.
- A separate ASR pipeline for pronunciation feedback or conversation; those workflows send
  audio to the multimodal model (ADR 0005). Listening drills that accept a spoken answer
  use the response-transcription workflow (WF-4) and a local matcher. Deterministic
  on-device tone measurement (DSP and DTW alignment) is not an ASR pipeline (ADR 0014).
- On-device speech/AI inference - cloud-only, since local compute is insufficient.
- Accounts, cross-device sync, multi-user profiles, analytics, social features, and monetization.
- Chinese-language UI localization (English UI only).
