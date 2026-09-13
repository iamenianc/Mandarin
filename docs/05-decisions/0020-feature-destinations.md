# 0020 - Feature destinations contributed by feature modules

- Status: accepted
- Date: 2026-09-14

## Context

The shell hosts Home, the audio check, the module list, and drill sessions. ADR 0018 makes a
module's lessons and practice runnable through one shared engine, but some features are not
lessons or practice at all: Raymond is a chat surface, and the daily field loop is a
multi-step mission. Those need their own screens without the shell knowing their internals,
and without a shell change for every new feature.

## Decision

- `:core:ui` exposes a `FeatureDestination` contract: a stable `id`, an English or pinyin
  `title`, and `@Composable fun Content(onBack: () -> Unit)`. The module stays a plain
  design system: no Hilt, no app knowledge (ADR 0007).
- A feature module implements the contract and binds it with Hilt `@IntoSet`. The shell
  declares the multibound `Set<FeatureDestination>`, lists each destination on Home, and
  renders it on the shell-owned `feature/{id}` route with a back action.
- Feature modules own their own Compose and Hilt setup; the shell provides the navigation
  host and the Home surface only.

## Consequences

- A standalone feature is additive: implement the contract, bind it, and it appears on Home
  and routes with no shell edit.
- Learning modules keep using the drill engine; the contract is only for surfaces that are
  not lessons or practice.
- The contract is Compose-shaped, so it lives in `:core:ui` rather than `:core:model`, which
  stays dependency-free.
- The shell remains the single navigation host, so destinations cannot nest their own
  sibling graph outside `feature/{id}`; a future need for that is a new decision.
