# Speech reference clips

Reference audio for the speech module: the clip a learner's attempt is compared against
in pronunciation feedback (WF-1, ADR 0005). The clip is sent with the attempt and the
measured acoustic evidence (`docs/02-architecture.md`).

## Clip set

- One reference per practiced phrase, matching the item the learner repeats.
- Phrase clips long enough to carry a full target tone sequence.
- Slow variants for early attempts, using the `-slow` suffix.

## Metadata

- `targetTones` is required: feedback names the weakest pinyin syllable and tone number.
- `evidenceRef` points at the precomputed acoustic evidence for the clip (ADR 0014).
- `pinyin` with tone numbers, optional `hangul`, and no hanzi (ADR 0002, ADR 0012).
