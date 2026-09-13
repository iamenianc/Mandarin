# Vision

## Problem

A beginner who wants to *speak and understand* Mandarin usually can't get there
from mainstream apps. Those apps are built around reading and writing: characters,
flashcards, stroke order, translating sentences on a screen. A learner can rack up
hundreds of characters and still freeze the moment someone actually talks to them.
The skills that matter most early on - hearing tones, producing them, and holding a
simple spoken exchange - are the ones least practiced.

## Product

An Android app for absolute beginners that teaches **listening and speaking only**.
It is audio-first: every lesson is heard and spoken, not read. **Understanding pinyin
is crucial** - it is how a word's pronunciation is written down - so the app teaches
the pinyin sound system explicitly: the learner can read how any word should sound, not
just repeat what was played. It uses AI-assisted coaching - a multimodal model that
compares the learner's speech with reference audio - to give fast, specific feedback on
pronunciation, tones, and conversational responses.

Reading and writing Chinese are explicitly out of scope. The app never requires the
learner to read hanzi or produce characters to make progress.

The initial build teaches **first principles, fundamentals, and theory** - how Mandarin
sound works: tones and tone contours, syllable anatomy (initial / final / tone), the
pinyin sound system (initials, finals, spelling conventions, tone numbers), tone sandhi,
and rhythm - alongside **beginner, tourist, and survival Mandarin**. It is organized as
**learning modules** (tones, vocabulary, listening, speech, and fundamentals), each
offering **lessons** (guided teaching) and **practice** (reps); the **tones module
leads**, teaching the four tones and the neutral tone to a beginner ahead of words. The
app is built so new modules can be added without reworking the shell or the
audio/feedback pipeline (ADR 0007).

Practice is centered on the **Daily LAMP field loop** (Language Acquisition Made
Practical, ADR 0013): the app generates a bite-sized functional exchange for **script
rehearsal**, the learner uses it in a **field mission** with five simulated AI locals -
each a different voice and personality - and then **debriefs** by logging the words and
tone breakdowns that tripped them up.

**This is a private, single-user, non-commercial project** built by and for the author:
an absolute-beginner Mandarin learner who speaks and reads English and reads Hangul.
There are no accounts, no profiles, no analytics, and no monetization.

## Target users

- **The user:** the author - an absolute beginner in Mandarin who speaks and reads
  English and reads Hangul. The app is designed for this one person; there are no other
  user types and no profiles.

## Value proposition

- Learn by listening and speaking; no hanzi required.
- Grounded in fundamentals and theory: understand how tones and syllables work, and how
  pinyin spells them, by ear.
- **Tones come first**: a dedicated module teaches the four tones and the neutral tone to
  a beginner ahead of vocabulary.
- Beginner, tourist, and survival Mandarin that is useful immediately.
- **Pinyin is the pronunciation key**: always visible, explicitly taught, and echoed in
  feedback, so the learner can read how any word should sound; **Hangul optional** (the
  user reads Hangul).
- The product is **audio and visual**: visuals support the ear, but every on-screen word
  is in English or pinyin.
- Fast, specific feedback that compares the learner's attempt to the reference audio.
- AI conversation practice: speak with a patient partner that adapts to the learner.
- **Daily LAMP field loop**: rehearse one useful exchange, use it with five different
  simulated locals, then log what tripped you up - practice that moves from drills to real
  conversation.
- Short, focused audio sessions that fit a commute or a coffee break.
- **Modular**: new learning modules can be added over time without disrupting what
  exists.

## Success criteria

- A learner can hold a short spoken exchange on a covered topic without prompting.
- The user runs the daily loop consistently and can hold the day's exchange with all
  five locals by the end of the mission.
- The user finds themselves using practiced exchanges outside the app.
- A learner can hear and produce the four tones and basic syllable sounds by ear, and
  name tones by number.
- Pinyin works as a portable pronunciation key: the learner can read a new pinyin word
  aloud with the right sounds and tones before hearing it.
- A learner can complete a session in eyes-free mode (audio-only) when desired.
- Feedback arrives fast enough to feel like a conversation, not a test.
- The user sticks with it.
- Adding a new learning module requires content and a thin feature module, not shell
  changes.

## Non-goals

- Teaching hanzi, stroke order, or handwriting.
- Any character-based reading or vocabulary browsing; vocabulary is audio-first.
- Pinyin reading fluency as an end in itself; pinyin is taught as the pronunciation map
  (ADR 0011), not as a reading curriculum.
- Teaching Hangul; it is assumed prior knowledge, used only as a phonetic aid.
- Passing a written exam or HSK reading sections.
- Becoming a general-purpose translator.
- Replacing real human conversation: the field is simulated with AI locals; the app does
  not arrange real-world practice partners.
- Accounts, sync, analytics, or any monetization.

## Open questions

- Hands-free conversation: wake word vs. push-to-talk.
- How the five locals vary by default (role, pace, politeness) and how much they change
  between missions.
- How to log a debrief word the learner cannot type: tap it in the mission transcript vs.
  speak it.
