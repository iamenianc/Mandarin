// LearnHuayu AI proxy - Cloudflare Worker
// Deploy: npx wrangler deploy
// Secret: npx wrangler secret put OPENROUTER_API_KEY
// Local:  copy .dev.vars.example to .dev.vars and fill in the key
//
// One route per workflow (docs/08-ai-workflows.md, ADR 0009). Prompts and
// provider selection stay server-side (ADR 0003).

import { chatCompletion, proxySpeech, transcribeAudio } from './openrouter.js';
import { isPlainObject } from './validate.js';
import { workflowBySlug } from './workflows.js';

const OPENROUTER_URL = 'https://openrouter.ai/api/v1/chat/completions';
const DEFAULT_MODEL = 'openai/gpt-5.6-luna';
const MAX_BODY_BYTES = 64 * 1024;

const CORS = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type',
};

export default {
  async fetch(request, env) {
    const { pathname } = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, { headers: CORS });
    }

    if (request.method === 'GET' && pathname === '/') {
      return json({ service: 'learnhuayu-api', status: 'ok' });
    }

    if (request.method === 'POST' && pathname === '/v1/chat') {
      return chat(request, env);
    }

    if (request.method === 'POST' && pathname.startsWith('/v1/wf/')) {
      const workflow = workflowBySlug(pathname.slice('/v1/wf/'.length));
      if (!workflow) return json({ error: 'not found', status: 404 }, 404);
      return handleWorkflow(request, env, workflow);
    }

    return json({ error: 'not found', status: 404 }, 404);
  },
};

async function handleWorkflow(request, env, workflow) {
  const declared = Number(request.headers.get('content-length'));
  if (Number.isFinite(declared) && declared > workflow.maxBodyBytes) {
    return tooLarge();
  }

  const raw = await request.text();
  if (raw.length > workflow.maxBodyBytes) {
    return tooLarge();
  }

  let body;
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: 'invalid json', status: 400 }, 400);
  }

  if (!isPlainObject(body)) {
    return json({ error: 'body must be a JSON object', status: 400 }, 400);
  }

  const parsed = workflow.validateInput(body);
  if (parsed.error) {
    const status = parsed.status ?? 400;
    return json({ error: parsed.error, status }, status);
  }

  if (workflow.kind === 'tts') return handleSpeechSynthesis(env, workflow, parsed.value);
  if (workflow.kind === 'stt') return handleTranscription(env, workflow, parsed.value);
  return handleChat(env, workflow, parsed.value);
}

async function handleChat(env, workflow, value) {
  const result = await chatCompletion(env, {
    model: env[workflow.envModel] || workflow.model,
    messages: workflow.buildMessages(value),
    temperature: workflow.temperature,
    maxTokens: workflow.maxTokens,
    timeoutMs: workflow.timeoutMs,
  });

  if (!result.ok) return json({ error: result.error, status: result.status }, result.status);

  let output;
  try {
    output = JSON.parse(result.content);
  } catch {
    return invalidUpstream();
  }

  const validated = workflow.validateOutput(output);
  if (!validated) return invalidUpstream();
  return json(validated);
}

async function handleTranscription(env, workflow, value) {
  const result = await transcribeAudio(env, {
    model: env[workflow.envModel] || workflow.model,
    audio: value.audio[0],
    timeoutMs: workflow.timeoutMs,
  });

  if (!result.ok) return json({ error: result.error, status: result.status }, result.status);

  const output = { transcript: result.transcript };
  if (result.confidence !== undefined) output.confidence = result.confidence;
  if (result.matchedOptionId !== undefined) output.matchedOptionId = result.matchedOptionId;

  const validated = workflow.validateOutput(output);
  if (!validated) return invalidUpstream();
  return json(validated);
}

async function handleSpeechSynthesis(env, workflow, value) {
  const result = await proxySpeech(env, {
    input: value.input,
    voice: value.voice,
    language: value.language,
    timeoutMs: workflow.timeoutMs,
  });

  if (!result.ok) return json({ error: result.error, status: result.status }, result.status);

  return new Response(result.body, {
    status: 200,
    headers: { ...CORS, 'Content-Type': result.contentType },
  });
}

async function chat(request, env) {
  const raw = await request.text();
  if (raw.length > MAX_BODY_BYTES) {
    return json({ error: 'payload too large', status: 413 }, 413);
  }

  let body;
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: 'invalid json', status: 400 }, 400);
  }

  if (!isPlainObject(body)) {
    return json({ error: 'body must be a JSON object', status: 400 }, 400);
  }

  if (!Array.isArray(body.messages) || body.messages.length === 0) {
    return json({ error: 'messages must be a non-empty array', status: 400 }, 400);
  }

  const upstream = await fetch(OPENROUTER_URL, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
      'Content-Type': 'application/json',
      'X-Title': 'LearnHuayu',
    },
    body: JSON.stringify({
      model: body.model || env.OPENROUTER_MODEL || DEFAULT_MODEL,
      messages: body.messages,
      temperature: body.temperature ?? 0.7,
      max_tokens: body.max_tokens ?? 1024,
      response_format: { type: 'json_object' },
      provider: { data_collection: 'deny' },
    }),
  });

  if (!upstream.ok) {
    return json({ error: 'upstream error', status: upstream.status }, 502);
  }

  let data;
  try {
    data = await upstream.json();
  } catch {
    return json({ error: 'invalid upstream response', status: 502 }, 502);
  }
  return json({ content: data.choices?.[0]?.message?.content ?? null });
}

function tooLarge() {
  return json({ error: 'payload too large', status: 413 }, 413);
}

function invalidUpstream() {
  return json({ error: 'invalid upstream response', status: 502 }, 502);
}

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...CORS, 'Content-Type': 'application/json' },
  });
}
