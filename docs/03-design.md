# Design

Status: draft. This document covers UX flows and screen inventory, not visual
polish. The product is **audio and visual**: audio drives the lesson, visuals support
it, and every on-screen word is in English or pinyin. The eyes-free mode is an option,
not the default.

## Experience principles

- **Audio-first, visually supported.** Visuals reinforce what is heard; they never
  replace it or require reading a character.
- **One button when possible.** The primary interaction is talk (or tap once), not
  ten taps through text.
- **Fast, specific feedback.** Name the problem ("the second syllable sounds flat") and
  replay the reference, rather than showing a grade.
- **No hanzi, ever.** Every on-screen word is English or pinyin (with Hangul optional).
- **Pinyin is the pronunciation key.** Pinyin is always on screen and explicitly taught
  as the sound map (ADR 0011); Hangul can be added alongside it. Nothing is hidden or
  revealed on demand.
- **Encouraging under imperfection.** AI feedback is approximate; never punish a learner
  for the mic or for the model's uncertainty.
- **One shape for every module.** Each learning module offers lessons and practice through
  the same drill controls, so learning a new module costs nothing to the learner.
- **Never a dead end.** Running out of bundled exercises is not an end state: the learner
  can always continue with AI-generated examples or replay the curated set.
- **Practice transfers to the field.** Practice centers on using a whole exchange with
  different listeners (the daily loop), not isolated reps alone; the mission is where the
  learner finds out what survives contact.

## Core flows

### Onboarding
1. Welcome / value proposition, audio sample of what a session feels like.
2. Microphone permission with a clear explanation *before* the system prompt.
3. Set daily goal, session length, and whether Hangul is shown.
4. Consent for sending recordings to the AI provider, explained plainly.
5. The first tones lesson begins immediately.

### Listening drill (no speaking)
1. Play a short reference phrase or dialogue.
2. Learner responds by tapping a large choice (pinyin or meaning) or speaking a keyword.
3. Immediate audio feedback and the next item.

### Speak and repeat
1. Play the reference phrase.
2. Learner taps once and repeats it.
3. App records, detects end of speech, and sends reference + attempt for feedback.
4. Feedback names the weakest sound by its pinyin syllable and tone number ("the second
   syllable sounds flat - it should be tone 2"), and replays reference and learner together.
5. Learner can retry, or move on.

### Tones (lessons and practice, beginner-first)
1. **Lesson:** one tone at a time - the four tones and the neutral tone - heard, described
   as a contour, and labelled with its tone number (`ma1`, `ma2`, `ma3`, `ma4`, `ma5`).
2. **Practice:** hear-and-name-the-tone drills, tone pairs (which numbers did you hear?),
   and say-and-repeat with tone-number feedback.
3. The tones module is the recommended starting point and shares the standard drill
   controls used by every module.

### Vocabulary (lessons and practice)
1. **Lesson:** a themed set of high-frequency words or phrases; each plays audio with
   pinyin (and Hangul if enabled) and an English meaning.
2. **Practice:** hear-and-identify the word from audio (choose the matching pinyin or
   meaning), then say-and-repeat it.
3. No hanzi and no character matching; words are learned as sound plus pinyin plus
   meaning.

### Fundamentals and theory (lessons and practice)
1. **Lesson:** a spoken explanation of one idea - syllable anatomy (initial/final), tone
   sandhi, rhythm - with clear audio examples. Pinyin-system lessons cover initials,
   finals, spelling conventions, and tone-number notation (ADR 0011, ADR 0012).
2. **Practice:** discrimination drills (which sound did you hear?), pinyin-to-sound
   practice (read shown pinyin aloud; choose the pinyin that matches what was heard), and
   short imitation.
3. Theory is always heard, never read as grammar text.

### AI conversation
1. Choose a scenario (greeting, ordering, directions).
2. Coach speaks; learner responds with the mic.
3. Coach adapts difficulty, corrects gently, and keeps the exchange going.
4. Session summary highlights what was understood and what to practice.

### Daily LAMP field loop (ADR 0013)
1. **Script Rehearsal:** the app presents one bite-sized functional exchange for the day
   (WF-9; a bundled script offline) - a few turns with pinyin and reference audio. The
   learner hears each line and says it back with feedback (WF-1).
2. **Field Mission:** the learner speaks the exchange in turn with five simulated locals
   (WF-10, voiced by WF-3). Each local has a different role, personality, voice, and pace,
   and responds in character; they never correct. Progress shows which local is next
   ("local 3 of 5"), and every turn can be replayed afterward.
3. **Debrief:** back in the app, the exchange is shown in pinyin; the learner marks the
   words they did not catch and the syllables whose tones broke down, by tapping the
   transcript or speaking the word. Entries are stored as pinyin with tone numbers
   (ADR 0012) and become progress themes and input to the next script.
4. Locals are role-play, not coaches: the coaching conversation (WF-2) and pronunciation
   feedback (WF-1) stay in lessons and rehearsal; corrections surface in rehearsal or
   debrief, never mid-mission.

### Endless practice (AI-extended)
1. At the end of a bundled set, practice offers "more like this".
2. WF-8 generates validated, on-theme items; they appear labeled as extra, with the same
   drill controls and reference audio.
3. The learner can keep going indefinitely, or return to the curated set.
4. Offline, the same control repeats and varies bundled items instead.

### Raymond (ask anything about Mandarin)
1. Available from Home at any time; keeps a running chat history.
2. The learner asks by typing (English or pinyin) or by speaking.
3. Raymond answers about pronunciation, tones, pinyin conventions, meaning, usage, and
   light culture, with pinyin examples (voiced on request). It redirects questions
   outside Mandarin.
4. Answers never show hanzi; the learner can replay any voiced example.

### Progress
- Practice history and recurring feedback themes (e.g. "second tone", "final -ng").
- Presented mostly as audio summaries, with compact visual support.

## Screen inventory (v1)

| Screen | Purpose |
| --- | --- |
| Onboarding | Value prop, mic permission, goal + consent |
| Home | Module launcher and today's LAMP loop |
| Module | A module's lessons and practice list |
| Raymond | Ask-anything Mandarin Q&A chat |
| Tones | Tone lesson and practice (beginner-first) |
| Vocabulary | Word/phrase lesson and practice |
| Fundamentals | Syllable and pinyin-system lesson and practice |
| Listening drill | Hear-and-respond loop |
| Speak and repeat | Record, compare, retry |
| Script rehearsal | Rehearse today's exchange before the mission |
| Field mission | Speak the exchange with five simulated locals |
| Debrief | Log unfamiliar words and tone breakdowns |
| Conversation | Turn-based AI voice practice |
| Session summary | Spoken results and next focus |
| Progress | Practice history and feedback themes |
| Settings | Goal, Hangul toggle, consent, data deletion |

Vocabulary is **audio-first**: words are heard and spoken, shown with pinyin - always
visible and taught as the pronunciation key (ADR 0011) - plus meaning (and optional
Hangul). There is deliberately **no hanzi browser and no character-based drill** (ADR 0008).

## Interaction notes

- Large, reachable controls; usable one-handed.
- Support system dark theme from day one, but optimize for low visual attention.
- Pinyin is always visible during drills; Hangul appears only if enabled.
- Tones are displayed as numbers (`ni3 hao3`) everywhere; feedback quotes the same
  numbers.
- Feedback names pinyin syllables and tone numbers, so coaching maps back to the pinyin
  on screen.
- Mic states must be unmistakable: listening, processing, speaking, muted.
- Handle interruption (calls, other audio) without losing session state.
- Always allow replay of the reference; never lose the learner's recording before
  they've heard it.
- The field mission shows which local is next and never shows hanzi; locals' speech and
  the learner's own turns are replayable.
- Debrief logging starts from the mission transcript: tap a word or syllable to mark it,
  or speak it - the learner never has to type pinyin.
- Back navigation during a session confirms before discarding progress.

## Open design questions

- Hands-free conversation: wake word vs. push-to-talk.
- How to keep AI feedback varied and encouraging rather than repetitive.
- How the five locals vary by default (role, pace, politeness) and whether personas are
  generated per mission or drawn from a bundled pool.
- How guided the debrief should be (suggested trouble spots) versus learner-driven.
