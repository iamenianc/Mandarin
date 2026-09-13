# App assets

Bundled, app-level assets for the `:app` module (ADR 0007; `docs/02-architecture.md`).
This tree holds assets that are **general or miscellaneous**: configuration defaults,
legal and attribution texts, branding sources and specifications, and identity data. It
is read at runtime through Android's `AssetManager`.

## Scope

In scope:

- App configuration defaults and constants.
- Legal, consent, privacy, and attribution texts.
- Branding vector sources and their specifications.
- Identity and other general supporting data.

Out of scope (owned elsewhere; do not duplicate here):

- **Authored learning content.** Examples, samples, lessons, knowledge, and reference
  audio live in the repository-root `assets/` tree and are bundled per learning module
  (ADR 0007, ADR 0010). Do not duplicate those categories in this tree.
- **Structured Android resources.** Launcher icons, UI strings, fonts, themes, and other
  `res/` resources belong under `android/app/src/main/res/`. Android requires them there
  for density buckets, resource qualifiers, and translations, so they never belong in
  `assets/`. See `branding/README.md` for how brand sources map to `res/`.
- **Secrets and provider credentials.** API keys never ship in the APK; the Worker is the
  trust boundary (ADR 0003). Nothing in this tree may contain a key, token, or account
  identifier.

## Layout

| Path | Kind | Purpose |
| --- | --- | --- |
| `manifest.json` | index | Machine-readable index of every file in this tree |
| `config/` | configuration | App defaults, Worker endpoint map, audio constants |
| `legal/` | legal | Consent, privacy notice, data inventory, attribution, licences |
| `branding/` | branding | Brand sources, icon and typography specifications, palette |
| `misc/` | general | App identity and other general data |

## Conventions

- Data and prose use Markdown, JSON, YAML, or TXT. Brand artwork uses SVG, with PNG
  previews under `branding/preview/`.
- File names are lower kebab-case; JSON keys are lower camelCase.
- Markdown uses sentence-case headings and no first person (AGENTS.md).
- Every JSON file is valid without comments; status and provenance live in string fields
  or the folder README.
- `manifest.json` is updated whenever a file is added or removed.
- Binary assets are specified and indexed before they exist; where a real resource cannot
  be produced, a valid minimal file is created and its provenance documented.

## Related documentation

- `docs/02-architecture.md` - layering, repositories, preferences, and content/consent
  rules.
- `docs/05-decisions/0007-modular-learning-modules.md` - content is data, not code.
- `docs/05-decisions/0010-bundled-plus-generated-exercises.md` - bundled curated set plus
  validated generated items.
- `docs/06-pipeline.md` - the `android/` project root and the release pipeline.
