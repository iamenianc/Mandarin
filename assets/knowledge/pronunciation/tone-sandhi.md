# Tone sandhi

Tones change in connected speech. The display convention is to keep the underlying tone
number and let audio and feedback carry the surface form, so the same syllable stays
recognizable across contexts.

## Third-tone sandhi

A third tone followed by another third tone is spoken as a second tone, while the written
number stays `3`.

| Written | Spoken | Meaning |
| --- | --- | --- |
| `ni3 hao3` | ni2 hao3 | hello |
| `hen3 hao3` | hen2 hao3 | very good |
| `hen3 zao3` | hen2 zao3 | very early |
| `wo3 hen3 hao3` | wo2 hen2 hao3 | I am well |

Rules of thumb:

- Sandhi applies within a phrase or rhythm group, not across a pause.
- In a run of three third tones, the first two usually become second tone:
  `wo3 hen3 hao3` -> wo2 hen2 hao3.
- A third tone before any other tone becomes a half third: low and falling, with the
  final rise dropped. It is still written `3`.

## `bu4` sandhi

`bu4` is spoken as second tone before a fourth tone, and stays fourth tone elsewhere.

| Written | Spoken | Meaning |
| --- | --- | --- |
| `bu4 shi4` | bu2 shi4 | is not |
| `bu4 ke4 qi5` | bu2 ke4 qi5 | you're welcome |
| `bu4 hao3` | bu4 hao3 | not good |
| `bu4 mang2` | bu4 mang2 | not busy |

## `yi1` sandhi

`yi1` changes with the tone that follows when it means "one" in a counting phrase.

| Context | Spoken | Example | Meaning |
| --- | --- | --- | --- |
| before tone 4 | `yi2` | `yi1 ge4` -> yi2 ge4 | one (of something) |
| before tones 1, 2, 3 | `yi4` | `yi1 tian1` -> yi4 tian1 | one day |
| before tones 1, 2, 3 | `yi4` | `yi1 nian2` -> yi4 nian2 | one year |
| before tones 1, 2, 3 | `yi4` | `yi1 ben3` -> yi4 ben3 | one (book) |
| counting or final position | `yi1` | `yi1, er4, san1` | one, two, three |

`yi1` stays first tone in ordinals and dates such as `di4 yi1` (first) and `yi1 yue4`
(January).

## Neutralization

Many second syllables lose their full tone in everyday speech and become neutral:

| Written | Common speech | Meaning |
| --- | --- | --- |
| `xie4 xie5` | xie4 xie5 | thanks |
| `ma1 ma5` | ma1 ma5 | mother |
| `peng2 you5` | peng2 you5 | friend |
| `dong1 xi5` | dong1 xi5 | thing |

## Authoring notes

- Keep `ContentItem.pinyin` and `targetTones` in underlying tones; record the surface form
  only when it matters for a specific lesson.
- When a generated item is validated, check that sandhi cases are written with underlying
  tones and that any explanatory note names the sandhi rule.
- Pronunciation feedback names the surface tone the learner produced and the underlying
  tone expected, so the note explains the mismatch rather than contradicting the screen.
