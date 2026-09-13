# Branding assets

Brand sources and specifications for LearnHuayu. Vector sources live under `source/`;
raster previews live under `preview/`. This folder records the brand so the launcher
icon, splash, and in-app chrome can be produced consistently.

## Boundary with res/

Android requires launcher icons, splash drawables, themes, UI strings, and fonts to be
**structured resources under `android/app/src/main/res/`** (density buckets, adaptive
icon XML, `values/` strings). Those derivatives are out of scope for the assets tree and
must not be duplicated here. `asset-manifest.json` maps each source to its intended
`res/` target so the handoff is explicit.

## Binary assets and provenance

- `source/*.svg` is the authoritative artwork: hand-authored vector sources with no
  third-party content and no hanzi.
- `preview/*.png` are raster renders of the same geometry, produced offline for review
  and documentation (supersampled, then downsampled). They are not runtime resources;
  the `res/` derivatives are generated from the SVG sources.
- No fonts are bundled here. A custom typeface, if any, is delivered through
  `res/font/`; the system font covers English, pinyin, and Hangul.

## Files

| File | Purpose |
| --- | --- |
| `brand.md` | Name, product statement, voice, invariants, and mark usage |
| `icon-spec.md` | Adaptive icon, density exports, and splash specification |
| `typography.md` | Character coverage, type roles, and accessibility rules |
| `palette.json` | Proposed colour tokens and semantic states |
| `asset-manifest.json` | Brand asset index and `res/` target mapping |
| `source/brand-mark.svg` | Authoritative brand mark |
| `source/launcher-foreground.svg` | Adaptive icon foreground layer |
| `source/launcher-background.svg` | Adaptive icon background layer |
| `source/launcher-monochrome.svg` | Themed-icon monochrome layer |
| `preview/brand-mark-512.png` | Brand mark preview |
| `preview/launcher-square-192.png` | Square launcher preview |
| `preview/launcher-round-192.png` | Round launcher preview |
