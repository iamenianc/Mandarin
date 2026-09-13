# Icon specification

Status: proposed. Applies to the launcher icon, the themed (monochrome) icon, and the
splash mark. Sources are in `source/`; the corresponding `res/` files are produced from
them and are out of scope for this tree (see `asset-manifest.json`).

## Adaptive launcher icon

Android adaptive icons use a 108 x 108 dp viewport with separate layers:

| Layer | Viewport | Source | Target (`res/`) |
| --- | --- | --- | --- |
| Background | 108 x 108 dp | `source/launcher-background.svg` | `values/ic_launcher_background.xml` (colour) |
| Foreground | 108 x 108 dp | `source/launcher-foreground.svg` | `drawable/ic_launcher_foreground.xml` |
| Monochrome | 108 x 108 dp | `source/launcher-monochrome.svg` | `drawable/ic_launcher_monochrome.xml` |

- Safe zone: keep content inside the centre 72 x 72 dp; for round masks keep it inside a
  66 dp diameter circle.
- The foreground contour is centred in the safe zone; the background is a flat colour.
- The monochrome layer uses a single colour (the path silhouette) and is tinted by the
  system for themed icons on Android 13 and above.

## Density exports

Production generates PNG fallbacks for legacy launchers:

| Density | Size |
| --- | --- |
| mdpi | 108 x 108 px |
| hdpi | 162 x 162 px |
| xhdpi | 216 x 216 px |
| xxhdpi | 324 x 324 px |
| xxxhdpi | 432 x 432 px |

Provide both square (`ic_launcher`) and round (`ic_launcher_round`) variants; the round
icon is masked from the same artwork.

## Splash

The splash shows the brand mark on the background colour through `core-splashscreen`
(candidate in `docs/10-libraries-and-dependencies.md`); the window icon uses the
monochrome source. Exact theme wiring is an M1 implementation detail.

## Rules

- No text and no hanzi in any icon (FR-17).
- One concept: the tone contour. Do not add letters, flags, or character glyphs.
- Verify foreground contrast against the background in both light and dark launcher
  contexts.

## Planned res/ derivatives

The launcher icon, monochrome layer, and splash drawable are `res/` resources and are
tracked outside this tree. This folder holds only the sources and their specifications,
so the two trees stay merge-disjoint.
