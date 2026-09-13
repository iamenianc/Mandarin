# Listening reference clips

Reference audio for the listening module: the items a hear-and-identify or
hear-and-respond drill plays before the learner answers. Tap-only drills work offline;
spoken answers use WF-4 response transcription (`docs/08-ai-workflows.md`).

## Clip set

- Prompt clips for each listenable item, aligned with its `ContentItem`.
- Distractor-friendly sets, where items differ by a tone, an initial, or a final.
- Slow replays via the `-slow` suffix for early practice.

## Metadata

- `contentType` is `dialogue` for multi-turn clips and `word`, `phrase`, or
  `minimalPair` for single prompts.
- `pinyin` with tone numbers, optional `hangul`, and no hanzi (ADR 0002, ADR 0012).
- Drill-mechanic tracks that are not tied to a single item live in `../../drills/`.
