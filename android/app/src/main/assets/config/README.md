# Configuration assets

App-level configuration defaults and constants bundled with `:app`. Files here are read
through `AssetManager`; they hold no secrets and no user preferences.

## Precedence

1. `defaults.json` supplies first-run and fallback values only. After the first write,
   DataStore is the source of truth (`docs/02-architecture.md`).
2. `worker.json` supplies Worker connection details. Provider credentials never appear
   here; the Worker is the trust boundary (ADR 0003).
3. `audio.json` holds pipeline constants shared by playback, capture, upload, and
   assessment (ADR 0006, ADR 0014).

## Rules

- No API keys, tokens, or account identifiers.
- No learning content. Authored lessons, examples, and reference audio live in the
  repository-root `assets/` tree (ADR 0007, ADR 0010).
- No structured Android resources. Themes, UI strings, and fonts belong under `res/`.
- Valid JSON without comments; status and provenance are string fields.

## Files

| File | Purpose |
| --- | --- |
| `defaults.json` | First-run and fallback values for user preferences |
| `worker.json` | Worker base URL, transport, and the per-workflow endpoint map |
| `audio.json` | Reference-audio, capture, upload, and playback constants |
