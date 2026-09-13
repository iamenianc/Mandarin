# 0016 - Ktor and kotlinx.serialization for the Worker client

- Status: accepted
- Date: 2026-09-13

## Context

`docs/10-libraries-and-dependencies.md` left the HTTP and JSON stack open (Ktor versus
OkHttp/Retrofit). `:core:ai` must call the Worker's unary JSON workflow endpoints from M1,
and `docs/02-architecture.md` prefers WebSocket streaming for conversation latency later
(NFR-2). The Worker payloads, the evidence schema, and the bundled content JSON all need
one serialization model.

## Decision

Use one Kotlin stack: Ktor client (`ktor-client-core`, `ktor-client-okhttp`,
`ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`,
`ktor-client-websockets`) with `kotlinx-serialization-json`, applied through the Kotlin
serialization plugin. Worker DTOs and the evidence schema are `@Serializable` types in
`:core:ai`; the app never talks to a provider directly (ADR 0003). Ktor's `MockEngine`
backs the unit tests.

## Consequences

- One library covers HTTP, JSON negotiation, streaming, and test doubles; `retrofit2` and
  a direct `okhttp` stack are not adopted.
- The Kotlin serialization Gradle plugin and the Ktor artifacts are added to the version
  catalog; every module that serializes applies the plugin.
- Wire models stay internal to `:core:ai`; feature modules see workflow-level interfaces,
  not transport types.
- Streaming conversation (WF-2/WF-3) stays unary until M4; the WebSocket client is
  present but unused, which `docs/10` already anticipated.
- Alternatives considered: OkHttp/Retrofit (rejected - a second client and converter for
  no gain); hand-rolled JSON (rejected - the payloads are structured and schema-checked).
