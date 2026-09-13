# Sample clips

Fixed, non-lesson audio that the app plays outside a drill: the welcome screen's sample
of what a session feels like (`docs/03-design.md`), and any onboarding or demo clip added
later. A sample clip is not tied to a `ContentItem` and carries no `targetTones`
requirement.

## Rules

- `category` is `sample` and `module` is `app`.
- Named by clip id, following `../naming-and-formats.md`.
- Mandarin speech follows the pinyin tone-number and no-hanzi rules (ADR 0002,
  ADR 0012); English narration is allowed.
- The welcome clip is a short excerpt, not a full lesson.
