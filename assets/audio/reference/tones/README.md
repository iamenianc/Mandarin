# Tones reference clips

Reference audio for the tones module: the four tones and the neutral tone, their
contours, the tone-number notation, and tone pairs. The module is taught to a beginner
ahead of words and is the recommended starting point (ADR 0012).

## Clip set

- Isolated tone contours, so each tone is heard on its own.
- Neutral-tone units, where the neutral tone is `5` (`ma5`).
- Tone pairs: two-syllable combinations that drill tone recognition in sequence.
- Contrast sets that place the same syllable under different tones.

## Metadata

- `targetTones` lists the expected tone number per syllable and is the primary field
  for this module.
- `pinyin` carries tone numbers (`ma1`, `ma2`, `ma3`, `ma4`, `ma5`); diacritics are
  never used (ADR 0012).
- Items are usually `word` or `minimalPair`; see `tracks.yaml`.
