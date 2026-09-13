export const OPENROUTER_CHAT_URL = 'https://openrouter.ai/api/v1/chat/completions';
export const OPENROUTER_STT_URL = 'https://openrouter.ai/api/v1/audio/transcriptions';

const UPSTREAM_ERROR = { error: 'upstream error', status: 502 };
const INVALID_UPSTREAM = { error: 'invalid upstream response', status: 502 };

const AUDIO_MIME = {
  wav: 'audio/wav',
  mp3: 'audio/mpeg',
  m4a: 'audio/mp4',
  ogg: 'audio/ogg',
  webm: 'audio/webm',
  flac: 'audio/flac',
};

function providerHeaders(env, withJsonBody) {
  const headers = {
    Authorization: `Bearer ${env.OPENROUTER_API_KEY}`,
    'X-Title': 'LearnHuayu',
  };
  if (withJsonBody) headers['Content-Type'] = 'application/json';
  return headers;
}

function base64ToBytes(value) {
  const binary = atob(value);
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  return bytes;
}

export async function chatCompletion(env, { model, messages, temperature, maxTokens, timeoutMs }) {
  let response;
  try {
    response = await fetch(OPENROUTER_CHAT_URL, {
      method: 'POST',
      headers: providerHeaders(env, true),
      body: JSON.stringify({
        model,
        messages,
        temperature,
        max_tokens: maxTokens,
        response_format: { type: 'json_object' },
        provider: { data_collection: 'deny' },
      }),
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch {
    return { ...UPSTREAM_ERROR };
  }

  if (!response.ok) return { ...UPSTREAM_ERROR };

  let data;
  try {
    data = await response.json();
  } catch {
    return { ...INVALID_UPSTREAM };
  }

  const content = data?.choices?.[0]?.message?.content;
  if (typeof content !== 'string' || content.trim() === '') return { ...INVALID_UPSTREAM };
  return { ok: true, content };
}

export async function transcribeAudio(env, { model, audio, timeoutMs }) {
  const format = audio.input_audio.format;
  const form = new FormData();
  form.append('model', model);
  form.append('file', new Blob([base64ToBytes(audio.input_audio.data)], { type: AUDIO_MIME[format] }), `audio.${format}`);

  let response;
  try {
    response = await fetch(OPENROUTER_STT_URL, {
      method: 'POST',
      headers: providerHeaders(env, false),
      body: form,
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch {
    return { ...UPSTREAM_ERROR };
  }

  if (!response.ok) return { ...UPSTREAM_ERROR };

  let data;
  try {
    data = await response.json();
  } catch {
    return { ...INVALID_UPSTREAM };
  }

  const transcript = data?.transcript ?? data?.text;
  if (typeof transcript !== 'string' || transcript.trim() === '') return { ...INVALID_UPSTREAM };

  const result = { ok: true, transcript: transcript.trim() };
  if (typeof data.confidence === 'number' && data.confidence >= 0 && data.confidence <= 1) {
    result.confidence = data.confidence;
  }
  if (typeof data.matchedOptionId === 'string' && data.matchedOptionId !== '') {
    result.matchedOptionId = data.matchedOptionId;
  }
  return result;
}

export async function proxySpeech(env, { input, voice, language, timeoutMs }) {
  const url = env.TTS_PROVIDER_URL;
  if (typeof url !== 'string' || url.trim() === '') {
    return { ok: false, unconfigured: true, error: 'speech synthesis provider not configured', status: 501 };
  }

  const headers = { 'Content-Type': 'application/json' };
  if (typeof env.TTS_PROVIDER_API_KEY === 'string' && env.TTS_PROVIDER_API_KEY !== '') {
    headers.Authorization = `Bearer ${env.TTS_PROVIDER_API_KEY}`;
  }

  const payload = { input };
  if (voice !== undefined) payload.voice = voice;
  if (language !== undefined) payload.language = language;

  let response;
  try {
    response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(payload),
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch {
    return { ...UPSTREAM_ERROR };
  }

  if (!response.ok) return { ...UPSTREAM_ERROR };

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.startsWith('audio/')) return { ...INVALID_UPSTREAM };

  const body = await response.arrayBuffer();
  if (body.byteLength === 0) return { ...INVALID_UPSTREAM };
  return { ok: true, body, contentType };
}
