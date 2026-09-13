# Tone numbers

The app writes tones as numbers, not diacritics (ADR 0012). Each syllable carries its
tone number directly after it, and the neutral tone is `5`.

| Tone | Number | Example | Meaning |
| --- | --- | --- | --- |
| First | `1` | `ma1` | mother |
| Second | `2` | `ma2` | hemp |
| Third | `3` | `ma3` | horse |
| Fourth | `4` | `ma4` | scold |
| Neutral | `5` | `ma5` | question particle |

## Rules

- Write the number after the syllable, with no space: `ni3`, `hao3`, `shi4`.
- Write the neutral tone as `5`, never omitted: `de5`, `le5`, `ma5`.
- Keep numbers on every syllable of a multi-syllable word: `xie4xie5`, `dui4bu5qi3`.
- Record the underlying tone in `ContentItem.pinyin` and `targetTones`. Keep numbers
  unchanged when tone sandhi alters the spoken contour, and let feedback and audio carry
  the surface form (see `tone-sandhi.md`).
- Write fixed neutral-tone syllables as `5` (`de5`, `le5`, `ma5`, `zi5`). In reduplicated
  words the second syllable is usually neutral in speech, so it is written `5`
  (`xie4xie5`, `ma1ma5`). When a syllable keeps a clear tone, write that tone.

## Why numbers

Numbers are explicit about tone identity, survive plain-text authoring and schema
validation, and let feedback name a tone directly ("tone 2 on the second syllable")
(ADR 0012). Diacritics are easy to miss and are harder to validate in generated content.
