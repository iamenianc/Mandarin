# Licence texts

Full licence texts referenced by `../licenses.json` and `../attribution.md`.

## Convention

- One file per licence id, named after the SPDX id (`apache-2.0.txt`) or a descriptive
  name when there is no SPDX id (`audeering-research-license.txt`).
- Keep the canonical wording unchanged. Attribution notices get a separate
  `.NOTICE.md`; never edit the licence body.
- Add a text only for an adopted, pinned dependency. Option dependencies keep a
  placeholder entry in `../licenses.json` until adoption.

## Present

| File | Licence | Source | Used by |
| --- | --- | --- | --- |
| `apache-2.0.txt` | Apache License 2.0 | https://www.apache.org/licenses/LICENSE-2.0.txt | AndroidX, Media3, Room, Hilt, Kotlin, Ktor, Kokoro-82M |

## Pending

| Id | Reason |
| --- | --- |
| `audeering-research` | openSMILE is an option (`docs/09`); add on adoption. |
| `gpl` | TarsosDSP is an option with a GPL-family licence; add only if adopted, after reviewing distribution impact. |
