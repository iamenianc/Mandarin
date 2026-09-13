# Drill tracks

Audio used by drill mechanics rather than by a single `ContentItem`: prompt banks,
distractor sets, and instruction clips for the hear-and-identify and hear-and-respond
loops. A per-item reference clip lives in `../reference/`; a drill track is used when
the audio serves the exercise itself.

## Rules

- Named by drill id, following `../naming-and-formats.md`.
- `category` is `drill`; `module` names the module that runs the drill.
- The pinyin tone-number, optional-hangul, and no-hanzi rules still apply to Mandarin
  speech (ADR 0002, ADR 0004, ADR 0012).
- Tap-only drills remain playable offline; spoken answers use WF-4 response
  transcription (`docs/08-ai-workflows.md`).
