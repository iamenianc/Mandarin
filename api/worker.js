// LearnHuayu AI proxy - Cloudflare Worker
// Deploy: npx wrangler deploy
// Secret: npx wrangler secret put OPENROUTER_API_KEY
// Local:  copy .dev.vars.example to .dev.vars and fill in the key
//
// This is the initial LLM template: a health route plus a single bounded
// OpenRouter endpoint. Split it into one endpoint per workflow (docs/08) as
// the app grows; do not widen this endpoint to cover new tasks.

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

    return json({ error: 'not found' }, 404);
  },
};

async function chat(request, env) {
  const raw = await request.text();
  if (raw.length > MAX_BODY_BYTES) {
    return json({ error: 'payload too large' }, 413);
  }

  let body;
  try {
    body = JSON.parse(raw);
  } catch {
    return json({ error: 'invalid json' }, 400);
  }

  if (!Array.isArray(body.messages) || body.messages.length === 0) {
    return json({ error: 'messages must be a non-empty array' }, 400);
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

  const data = await upstream.json();
  return json({ content: data.choices?.[0]?.message?.content ?? null });
}

function json(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...CORS, 'Content-Type': 'application/json' },
  });
}
