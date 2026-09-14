# Worker API

The Cloudflare Worker in `api/` is the trust boundary for AI calls (ADR 0003). It keeps
provider credentials out of the app, validates and sizes every request per workflow, and
assembles every prompt server-side. Workflow registry and contracts live in
`docs/08-ai-workflows.md`; the route form and the initial `/v1/chat` template are
documented in `docs/02-architecture.md`.

## Routes

One versioned POST endpoint per runtime workflow (ADR 0009). Slugs map to the workflow
names in `docs/08-ai-workflows.md`; `/v1/wf/local-turn` follows the pipeline diagram in
`docs/02-architecture.md`.

| Method | Path | Workflow | Request fields | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/` | - | - | `{ "service": "learnhuayu-api", "status": "ok" }` |
| `POST` | `/v1/chat` | template | `{ messages, model?, temperature?, max_tokens? }` | `{ "content": "<string>" }` |
| `POST` | `/v1/wf/pronunciation-feedback` | WF-1 | `pinyin`, `targetTones?`, `learnerLevel?`, `acousticEvidence?`, `audio` (two `input_audio` parts) | `{ weakestUnit, issue, tip, encouragement, replayHint }` |
| `POST` | `/v1/wf/conversation-turn` | WF-2 | `scenario`, `conversationState?`, `targetDifficulty?`, `audio` (one part) | `{ replyText, gentleCorrection?, nextPrompt? }` |
| `POST` | `/v1/wf/speech-synthesis` | WF-3 | exactly one of `pinyin` or `replyText`, plus `voice?`, `language?` | 24 kHz mono audio, or `501` when unconfigured |
| `POST` | `/v1/wf/response-transcription` | WF-4 | `audio` (one part), `expectedOptions?`, `keywords?` | `{ transcript, matchedOptionId?, confidence }` |
| `POST` | `/v1/wf/progress-summary` | WF-5 | `attemptCounts`, `feedbackThemes`, `moduleIds?`, `lessonIds?`, `debriefThemes?` | `{ summaryText, focusAreas }` |
| `POST` | `/v1/wf/mandarin-qa` | WF-7 | `question` or `audio` (one part, spoken question), `history?`, `learnerLevel?` | `{ answerText, examples, followUps }` |
| `POST` | `/v1/wf/exercise-generation` | WF-8 | `moduleId`, `itemType`, `theme?`, `targetUnits?`, `difficulty?`, `feedbackThemes?`, `count?` | `{ items }` |
| `POST` | `/v1/wf/field-mission-generation` | WF-9 | `theme`, `moduleContext?`, `coveredContent?`, `learnerLevel?`, `debriefThemes?`, `exchangeLength?` | `{ script, locals }` (exactly five locals) |
| `POST` | `/v1/wf/local-turn` | WF-10 | `persona`, `mission`, `conversationState?`, `targetDifficulty?`, `audio` (one part) | `{ replyText, understandingSignal?, nextLocalPrompt? }` |
| `OPTIONS` | any | - | - | CORS preflight; all origins allowed |

WF-6 (content authoring) has **no route**: `docs/08-ai-workflows.md` defines it as
build-time authoring that never runs in the app. Prompts are assembled in
`api/workflows.js` per workflow and are never accepted from a client or returned in a
response (ADR 0003, ADR 0009). Prompts, output schemas, and fallbacks are per workflow;
a fallback is the app's offline behavior, so a Worker failure returns an error and the
app degrades on its own.

### Audio input

WF-1, WF-2, WF-4, and WF-10 (and the optional spoken question for WF-7) receive audio as
base64 `input_audio` content parts, the shape OpenRouter uses
(`{ "type": "input_audio", "input_audio": { "data": "<base64>", "format": "wav" } }`).
Supported formats: `wav`, `mp3`, `m4a`, `ogg`, `webm`, `flac`. WF-1 requires exactly two
parts (reference, then attempt); WF-2, WF-4, and WF-10 require exactly one; WF-7 accepts
either a text question or exactly one spoken-question part.
WF-5, WF-8, and WF-9 receive aggregated metadata and content context only (FR-24): the
Worker rejects any `input_audio` part in those requests, and WF-5 also rejects
`transcript`/`transcripts`.

### Validation and limits

| Workflow | Request body cap | Audio limits |
| --- | --- | --- |
| WF-1 | 6 MiB | two parts, 2 MiB decoded each |
| WF-2, WF-4, WF-7, WF-10 | 3 MiB | one part, 2 MiB decoded |
| WF-3, WF-5, WF-8, WF-9 | 64 KiB | audio not accepted |

Body caps are sized above the base64 expansion of the audio budget: a 2 MiB
decoded part encodes to ~2.8 MiB (4 chars per 3 bytes plus the JSON envelope),
so one-part workflows cap at 3 MiB and the two-part WF-1 caps at 6 MiB.

Pinyin fields must not contain Chinese characters; text metadata fields may carry
pinyin with tone numbers (`ma2`) or English, but never hanzi. Outputs are validated
against the documented schema, and trailing-coaching fields are dropped where the
schema marks them optional. Numeric tone arrays are checked as integers 1-5 (the
neutral tone is `5`) wherever the schema carries them (ADR 0012): WF-1 `targetTones`,
WF-8 item `targetTones`, WF-9 script turns, and WF-10 mission script turns. The pinyin
strings themselves are not pattern-checked server-side, because content validation owns
pinyin spelling. No-hanzi is enforced on every free-text input field
(WF-1 `pinyin`, `learnerLevel`, `acousticEvidence`; WF-2 `scenario`,
`conversationState`, `targetDifficulty`; WF-3 synthesis text, `voice`, `language`;
WF-4 `expectedOptions`, `keywords`; WF-5 `feedbackThemes`, `moduleIds`, `lessonIds`,
`debriefThemes`; WF-7 `question`, `history`, `learnerLevel`; WF-8 `moduleId`, `theme`,
`targetUnits`, `difficulty`, `feedbackThemes`; WF-9 `theme`, `moduleContext`,
`coveredContent`, `learnerLevel`, `debriefThemes`; WF-10 `persona`, mission script
turns, `mission.goal`, `conversationState`, `targetDifficulty`) and on output.
WF-1 rejects an out-of-range tone array with `400`; WF-8 drops the invalid item per
`docs/08-ai-workflows.md`; WF-9 and WF-10 fail the request.

### Errors

| Status | Body | Cause |
| --- | --- | --- |
| `400` | `{ "error": "<message>", "status": 400 }` | invalid JSON, non-object body, or schema violation |
| `413` | `{ "error": "payload too large", "status": 413 }` or `{ "error": "audio part too large", "status": 413 }` | body or audio part over the workflow cap |
| `502` | `{ "error": "upstream error", "status": 502 }` | provider call failed, timed out, or returned a non-2xx status |
| `502` | `{ "error": "invalid upstream response", "status": 502 }` | provider body unparseable or output failed schema validation |
| `501` | `{ "error": "speech synthesis provider not configured", "status": 501 }` | WF-3 called without `TTS_PROVIDER_URL` |
| `404` | `{ "error": "not found", "status": 404 }` | unknown path or workflow slug |

All workflow error bodies carry the `status` field. `/v1/chat` follows the same
shape: oversize bodies return `413` with `{ "error": "payload too large", "status":
413 }`, upstream failures return `{ "error": "upstream error", "status": <upstream
code> }`, and an unparseable upstream body returns `{ "error": "invalid upstream
response", "status": 502 }`.

## Configuration

Non-secret vars live in `wrangler.jsonc`; secrets are set with
`npx wrangler secret put <NAME>` (never committed).

| Variable | Kind | Purpose |
| --- | --- | --- |
| `OPENROUTER_API_KEY` | secret | OpenRouter credential for all chat and STT calls |
| `OPENROUTER_MODEL` | var | default model for `/v1/chat` only |
| `WF1_MODEL`, `WF2_MODEL`, `WF4_MODEL`, `WF5_MODEL`, `WF7_MODEL`, `WF8_MODEL`, `WF9_MODEL`, `WF10_MODEL` | var | optional per-workflow model overrides |
| `TTS_PROVIDER_URL` | var | WF-3 speech-synthesis provider endpoint; unset returns `501` |
| `TTS_PROVIDER_API_KEY` | secret | optional bearer token for the WF-3 provider |

Default models:

| Workflow | Default | Source |
| --- | --- | --- |
| WF-1, WF-2, WF-5, WF-7, WF-8, WF-9, WF-10 | `meta/muse-spark-1.3-contributor` | ADR 0005, `docs/07-speech-assessment-models.md` |
| WF-4 | `openai/gpt-4o-transcribe` | STT options named in `docs/07-speech-assessment-models.md` |
| WF-3 | configured provider only | `docs/08-ai-workflows.md` WF-3 |

## Local development

```powershell
Copy-Item .dev.vars.example .dev.vars   # then fill in OPENROUTER_API_KEY
npm run dev                             # npx wrangler dev
npm run deploy                          # npx wrangler deploy
npm test                                # node --test, offline
```

`.dev.vars` and `.wrangler/` are git-ignored and must not be committed. `npm test` runs
the `node:test` suite with `fetch` mocked and needs no API key.

## Known gaps

- **WF-3 cannot run Kokoro in the Worker.** No runtime synthesis provider is documented
  in `docs/08-ai-workflows.md` or `docs/06-pipeline.md`, so the route is a bounded proxy
  to `TTS_PROVIDER_URL` and returns `501` until that var is set. The provider request
  contract used here matches `api/openrouter.js` exactly: `POST` to `TTS_PROVIDER_URL`
  with JSON `{ "input": "<pinyin or replyText>", "voice?", "language?" }` and an
  `Authorization: Bearer <TTS_PROVIDER_API_KEY>` header only when that secret is set;
  the response must carry an `audio/*` content type and a non-empty body. `voice` and
  `language` are forwarded only when the client supplies them. Build-time reference
  clips stay bundled (ADR 0006).
- **No streaming.** `docs/02-architecture.md` prefers WebSocket streaming for conversation
  latency (NFR-2); all routes are unary JSON requests. WF-2 and WF-10 return a single
  JSON reply per request and emit no server-sent events, chunks, or socket frames; that
  unary shape is intentional until a streaming ADR lands.
- **No rate limiting yet.** ADR 0003 makes the Worker the rate-limiting boundary; only
  payload caps are implemented. A per-IP in-memory throttle was evaluated and deferred:
  Worker isolates have no shared memory across instances or regions, so a `Map` would
  throttle one isolate while leaving every other isolate unthrottled, and the single-user
  hobby scope carries no abuse pressure. Rate limiting stays deferred until cross-instance
  state (Durable Objects or KV) is adopted; see the proposed ADR text in the hardening
  completion report. Until then, senders stay bounded by the per-workflow body and audio
  caps documented above.
- **WF-4 model and confidence.** The default STT model id follows the options named in
  `docs/07-speech-assessment-models.md`; confirm it against the provider catalog before
  deployment. `confidence` and `matchedOptionId` are returned only when the provider
  supplies them, and answer matching stays on-device per `docs/08-ai-workflows.md` WF-4.
- **No prompt-version field.** Prompts are per-workflow and server-side, but the response
  does not echo a prompt version.
