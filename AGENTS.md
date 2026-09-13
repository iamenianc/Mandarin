# AGENTS.md

Repository-wide guidance for AI agents working in this project.

## Purpose

This is a **planning repository** for a private, audio-first Android app that teaches
beginners to listen to and speak Mandarin Chinese, with AI-assisted speech and
conversation coaching. Reading and writing hanzi are out of scope; Hanyu Pinyin is
always shown and Hangul is an optional phonetic aid. It is a single-user,
non-commercial hobby project for the author. Until the Android project is scaffolded,
work here is documentation and design only.

## Ground rules

- Do **not** add application code unless a task explicitly asks for it.
- Keep planning artifacts under `docs/`.
- Prefer editing existing docs over creating new ones; avoid duplicate documents.
- Record architectural decisions as ADRs in `docs/05-decisions/` using the existing template.
- Do not create new documentation files unless the task requires it.

## Conventions

- Markdown, sentence-case headings, one logical topic per file.
- Do not use the first person (`I`, `we`, `my`). Refer to "the user" or "the coding agent".
- ADR filenames: `NNNN-short-title.md` (zero-padded, sequential).
- Keep the `README.md` contents table in sync when adding top-level docs.

## Commands

- Initialize: `git init`
- No build, lint, or test commands exist yet. Add them here once the Android
  project is created.

## Do not commit

- Secrets, API keys, `local.properties`, or keystores.
- Build output (`build/`, `.gradle/`, `.idea/`).
