# Plan: clear the Node 20 deprecation warnings from CI

## Problem

Recent CI runs emit three warnings, all caused by action pins that target the deprecated
Node 20 runtime (GitHub forces them onto Node 24 and warns), plus an explicit
setup-java end-of-support notice:

- `android` job: `actions/checkout@v4`, `actions/setup-java@v4`,
  `android-actions/setup-android@v3`, `gradle/actions/setup-gradle@v4`
- `worker` job: `actions/checkout@v4`, `actions/setup-node@v4`
- `actions/setup-java@v4` is deprecated and no longer receives updates; the
  migration target is v5 or newer

The workflow also pins `node-version: "20"` for the worker job. Node 20 reached end
of life in April 2026, so the job currently tests on an unsupported runtime.

## Change

Edit `.github/workflows/ci.yml` only. Pin each action to the current major, verified
against the latest GitHub releases on 2026-09-13:

| Action | Current | Target (latest) |
| --- | --- | --- |
| `actions/checkout` | v4 | v7 (v7.0.1, 2026-07-20) |
| `actions/setup-java` | v4 | v6 (v6.0.1, 2026-09-09) |
| `android-actions/setup-android` | v3 | v4 (v4.0.1, 2026-04-04) |
| `gradle/actions/setup-gradle` | v4 | v6 (v6.3.0, 2026-08-02) |
| `actions/setup-node` | v4 | v7 (v7.0.0, 2026-07-14) |

Also change the worker job's `node-version` from `"20"` to `"24"` (current LTS,
matching the runtime GitHub already forces the actions onto).

The inputs the workflow uses (`distribution`, `java-version`, `node-version`) are
unchanged in the new majors, and no other workflow files exist.

Known side effects:

- `gradle/actions/setup-gradle@v6` bumps the cache protocol, so the first run after
  the change is a one-time cache miss (slower, still green).
- No effect on the Android build or the Cloudflare Worker runtime; only CI runner
  tooling changes.

Optional, recommended: add `.github/dependabot.yml` (`package-ecosystem: github-actions`,
weekly) so action pins stay current and this warning class does not silently return.

## Verification

1. Local, per `AGENTS.md`: `node --check api/worker.js` and `npm test` in `api/`.
2. Commit on `master` and push (master is currently in sync with `origin/master`),
   then confirm the CI run: all steps green, no deprecation warnings.
3. No Gradle checks are required for a workflow-only change.

## Execution notes

- Applied directly on `master`; no wave-2 slice owns `.github/`, so this cannot
  collide with the six active worktrees.
- One commit, message in repo style, for example:
  "Update CI action pins off the deprecated Node 20 runtimes".
- This plan file is session scaffolding: keep it out of the commit.
- Out of scope: SHA-pinning actions, changing the Worker's deployed runtime, any
  Android dependency upgrades.
