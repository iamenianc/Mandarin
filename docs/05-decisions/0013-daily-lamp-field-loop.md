# 0013 - The Daily LAMP field loop

- Status: accepted
- Date: 2026-09-13

## Context

Practice so far is described as module drill loops - hear-and-respond, speak and repeat,
scenario conversation. Drills build individual skills, but they do not by themselves make
a beginner use a whole exchange with another person, which is where real conversations
usually expose the gap. LAMP (**Language Acquisition Made Practical**) is a field method:
learn one bite-sized exchange, use it with several different native speakers the same day,
then debrief what was not understood. This project is single-user and cannot guarantee
native-speaker sessions every day, but it can simulate the field with AI voices.

## Decision

- Practice is centered on the **Daily LAMP field loop**, three phases in order:
  1. **Script Rehearsal** - the app generates (or selects) one bite-sized functional
     exchange for the day; the learner rehearses it.
  2. **Field Mission** - the learner speaks the exchange with **five simulated AI locals**,
     in-app voice personas, one after another.
  3. **Debrief** - the learner logs unfamiliar words and tone breakdowns from the mission.
- The loop keeps LAMP's core moves - one usable exchange, immediate use with several
  different listeners, and a debrief - with the field simulated by AI because that is what
  a private, single-user app can provide on demand.
- Each of the five locals is a **distinct character** (setting role, personality, voice,
  pace) and **responds in character**. A local never coaches or corrects; correction stays
  in rehearsal (WF-1) and the debrief. Varied listeners are the point of the mission: the
  learner practices coping with speech variation, not reciting to one partner.
- The loop is **cross-module practice**: scripts are drawn from covered content and themes
  (tones, vocabulary, listening, speech, fundamentals). It ships no lesson content of its
  own and does not change the module contract (ADR 0007).
- **Debrief logging is structured and local**: words and syllables are logged as pinyin
  with tone numbers (ADR 0012), no hanzi, and become recurring themes for progress (WF-5)
  and for later generation (WF-8, WF-9). Debrief notes themselves stay on device; only
  aggregated themes leave it.
- Missions run **in-app only**; no real-world outing or human interlocutor is required.
  Offline, the loop falls back to a bundled script and a bundled persona set with scripted
  local lines.

## Consequences

- Two new AI workflows: **WF-9** (field mission generation: the script plus five locals)
  and **WF-10** (in-character local turn; no correction). WF-3 voices the locals' replies.
- New local data: missions, local personas, mission sessions and turns, and debrief
  entries.
- A new feature module, `:feature:field`, owns the daily loop shell; learning modules
  supply the content it draws on.
- Requirements and design gain the loop (FR-30…FR-34, a core flow, and three screens);
  WF-5 and WF-9 consume debrief themes.
- The loop becomes the default practice path on Home; module lessons and drill practice
  remain.
- Alternatives considered: real-world missions with human locals (rejected - not
  guaranteed, not private, and beyond what the app can run); one adaptive coach speaking
  the five turns (rejected - one partner trains reacting to a single voice, not to
  variation); locals correcting during the mission (rejected - breaks the field illusion
  and the flow; the debrief owns reflection); making the loop a learning module (rejected -
  it orchestrates module content rather than teaching new content).
