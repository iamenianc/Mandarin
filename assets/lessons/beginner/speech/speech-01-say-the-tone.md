# Say the tone

| Field | Value |
| --- | --- |
| id | speech-01-say-the-tone |
| module | speech |
| lesson | 01 |
| level | beginner |
| topic | tone production |

The reference tone is played, the learner repeats it, and the app returns soft
feedback that names the tone it heard and the tone expected. The lesson teaches one
tone at a time so the feedback is about a single contour.

## Items

| # | pinyin | target tone |
| --- | --- | --- |
| 1 | ma1 | 1 |
| 2 | ma2 | 2 |
| 3 | ma3 | 3 |
| 4 | ma4 | 4 |
| 5 | ma5 | 5 |

## Notes

- The reference and the learner's attempt are replayed together, never scored
  (ADR 0005).
- If the mic or the model is uncertain, feedback stays encouraging and the item can
  be retried.
- Reference audio is generated at build time from the pinyin above and bundled under
  `assets/audio/` (ADR 0006).
