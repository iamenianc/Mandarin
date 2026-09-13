# LearnHuayu

**LearnHuayu** is a private Android app that teaches beginners to
**listen to and speak** Mandarin Chinese. It is audio-first but audiovisual: visuals
support what is heard, and every on-screen word is English or pinyin. AI-assisted speech
and conversation coaching. Reading and writing hanzi are out of scope; **Hanyu Pinyin is
always shown and explicitly taught as the pronunciation key** (ADR 0011), with tones
written as numbers (ADR 0012), and Hangul is an optional phonetic aid.

This is a non-commercial, single-user hobby project built by and for the author - no
accounts, profiles, analytics, or monetization.

Learning is organized as **modules** - beginning with a dedicated tones module, then
vocabulary, listening, speech, and fundamentals - each with lessons and practice, on an
architecture that lets new modules be added over time (see ADR 0007, ADR 0008). The app
ships with a curated set of preloaded exercises usable offline, and an AI flow extends
practice with generated examples without limit (ADR 0010). **Raymond** is an
always-available chat helper that answers any question about Mandarin (ADR 0009,
`docs/08-ai-workflows.md`). Daily practice is centered on the **Daily LAMP field loop**
(Language Acquisition Made Practical, ADR 0013): rehearse a bite-sized exchange, use it in
a field mission with five simulated AI locals, then debrief what tripped you up.

The repository began as a docs-first planning effort, and the material under `docs/`
remains the source of truth for product definition, architecture, and roadmap. Application
code now develops here alongside the docs.

## Contents

| Path | Purpose |
| --- | --- |
| `conductor.md` | Orchestrator playbook: how the coding agent plans, slices, delegates, and verifies work |
| `api/` | Cloudflare Worker AI proxy: routes, config, and secrets (see `docs/02-architecture.md`) |
| `docs/00-vision.md` | Why this app exists, target users, success criteria |
| `docs/01-requirements.md` | Functional and non-functional requirements |
| `docs/02-architecture.md` | Proposed technical architecture, stack, data model |
| `docs/03-design.md` | UX flows, screen inventory, visual direction |
| `docs/04-roadmap.md` | Milestones and phased delivery plan |
| `docs/05-decisions/` | Architecture Decision Records (ADRs) |
| `docs/06-pipeline.md` | Proposed build and deployment pipeline |
| `docs/07-speech-assessment-models.md` | Research note on Qwen models for speech assessment |
| `docs/08-ai-workflows.md` | Registry of separate, clearly defined AI workflows (WF-1…WF-10) |
| `docs/09-opensmile-acoustic-features.md` | Research note on openSMILE acoustic features for assessment (WF-1) |

## How to use

1. Start with `docs/00-vision.md` and `docs/01-requirements.md`.
2. Record significant technical choices as ADRs in `docs/05-decisions/`.
3. Application code develops in this repo alongside the docs (see `docs/04-roadmap.md`
   for the intended layout).

## Status

Moving from planning into implementation. See `docs/04-roadmap.md` for the current
milestone.
