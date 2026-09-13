# AGENTS.md

Repository-wide guidance for AI agents working in this project.

## Purpose

This repository holds a private, audio-first Android app that teaches beginners to listen
to and speak Mandarin Chinese, with AI-assisted speech and conversation coaching. Reading
and writing hanzi are out of scope; Hanyu Pinyin is always shown and Hangul is an optional
phonetic aid. It is a single-user, non-commercial hobby project for the author.

The repository began as a docs-first planning effort. The documentation under `docs/`
remains the source of truth for product, architecture, and roadmap decisions, and
application code now develops alongside it.

## Ground rules

- Keep design and planning artifacts under `docs/`.
- Record architectural decisions as ADRs in `docs/05-decisions/` using the existing template.
- Do not restrict the number or type of files created or added to the repository; create whatever files a task needs.
- Do not commit secrets, API keys, `local.properties`, keystores, or build output.

## Coding rules

- Do not reinvent the wheel; prefer established libraries and platform APIs over new code.
- Before writing code, check whether the repository already contains code that can be reused.
- Quality of life for human users is never optional.

## References

- **Android UI**: the official Jetpack Compose documentation,
  <https://developer.android.com/develop/ui/compose/documentation>, is the authoritative
  reference for Compose APIs, patterns, and best practices. Prefer it over memory, training
  data, or third-party snippets when deciding how to build a screen. Only the URL is
  recorded here; the environment may not be able to fetch the page, so the coding agent
  should open it directly when it needs the guidance.

## Conventions

- Markdown, sentence-case headings, one logical topic per file.
- Do not use the first person (`I`, `we`, `my`). Refer to "the user" or "the coding agent".
- ADR filenames: `NNNN-short-title.md` (zero-padded, sequential).
- Keep the `README.md` contents table in sync when adding top-level docs.

## Commits

- Commit finished work as part of the task; do not leave completed work uncommitted.
- Commit at logical boundaries, one coherent topic per commit, so the history is a
  useful record of how the project evolved.
- Write concise messages that state what changed and why.
- A change to these rules is itself a logical commit.

## Commands

- Initialize: `git init`
- No build, lint, or test commands exist yet. Add them here once the Android
  project is created.

## Do not commit

- Secrets, API keys, `local.properties`, or keystores.
- Build output (`build/`, `.gradle/`, `.idea/`).
