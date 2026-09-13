# Legal and attribution assets

User-facing legal texts bundled with the app: consent, privacy, data inventory,
attribution, and licence references. These are long-form documents read from assets. The
short UI strings that label them (for example a settings row title) belong under `res/`
and are out of scope.

## Display

- Consent is shown before the microphone or recordings are used, and is explicit and
  revocable (`docs/02-architecture.md`; NFR-4, NFR-5).
- The privacy notice and data inventory back the settings screen and one-tap data
  deletion.
- Attribution and licence references satisfy the notice obligations of the dependencies
  listed in `docs/10-libraries-and-dependencies.md`.

## Rules

- Plain language; no first person (AGENTS.md).
- No hanzi. Pinyin appears with tone numbers where a Chinese term is needed (ADR 0012).
- Do not restate provider terms that are not recorded in the docs; point to the provider
  instead.
- `licenses.json` is the machine-readable index and `attribution.md` is its readable
  form. Update both when a dependency is added or pinned.
- Consent text is versioned; `config/defaults.json` records the accepted version.

## Files

| File | Purpose |
| --- | --- |
| `consent.md` | Recording and AI consent text (version 1) |
| `privacy-notice.md` | Plain-language privacy notice |
| `data-inventory.md` | What is stored, where, and what leaves the device |
| `attribution.md` | Readable third-party attribution list |
| `licenses.json` | Machine-readable licence inventory |
| `licenses/` | Full licence texts referenced by the inventory |
