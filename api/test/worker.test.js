import test from 'node:test';
import assert from 'node:assert/strict';

import worker from '../worker.js';

const API_URL = 'https://api.example.com';
const CHAT_URL = 'https://openrouter.ai/api/v1/chat/completions';
const STT_URL = 'https://openrouter.ai/api/v1/audio/transcriptions';
const ENV = { OPENROUTER_API_KEY: 'test-key' };
const HANZI = '\u4f60\u597d';

function jsonResponse(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

function chatContent(output) {
  return jsonResponse({ choices: [{ message: { content: JSON.stringify(output) } }] });
}

function withFetch(handler) {
  const calls = [];
  const original = globalThis.fetch;
  globalThis.fetch = async (url, init = {}) => {
    const call = { url: String(url), init };
    calls.push(call);
    return handler(call, calls.length);
  };
  return {
    calls,
    restore() {
      globalThis.fetch = original;
    },
  };
}

function post(path, body, headers = { 'content-type': 'application/json' }) {
  return new Request(`${API_URL}${path}`, {
    method: 'POST',
    headers,
    body: typeof body === 'string' ? body : JSON.stringify(body),
  });
}

function get(path) {
  return new Request(`${API_URL}${path}`);
}

function audioPart(data = 'AAAA', format = 'wav') {
  return { type: 'input_audio', input_audio: { data, format } };
}

const SCRIPT_TURN = { pinyin: 'ni3 hao3', meaning: 'hello', targetTones: [3, 3] };
const LOCAL = {
  label: 'shopkeeper',
  settingRole: 'market stall',
  personality: 'cheerful',
  voiceProfile: 'warm',
  pace: 'slow',
};

test('GET / returns the documented health payload', async () => {
  const response = await worker.fetch(get('/'), ENV);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { service: 'learnhuayu-api', status: 'ok' });
});

test('OPTIONS returns CORS headers', async () => {
  const response = await worker.fetch(new Request(API_URL, { method: 'OPTIONS' }), ENV);
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('access-control-allow-origin'), '*');
  assert.equal(response.headers.get('access-control-allow-methods'), 'GET, POST, OPTIONS');
});

test('unknown routes return 404', async () => {
  const notFound = [
    get('/nope'),
    post('/v1/wf', {}),
    post('/v1/wf/unknown', {}),
    post('/v1/wf/content-authoring', {}),
    post('/v1/wf/', {}),
    get('/v1/wf/pronunciation-feedback'),
  ];
  for (const request of notFound) {
    const response = await worker.fetch(request, ENV);
    assert.equal(response.status, 404, `${request.method} ${new URL(request.url).pathname}`);
    assert.deepEqual(await response.json(), { error: 'not found', status: 404 });
  }
});

test('every runtime workflow slug is routed and rejects an empty body', async () => {
  const slugs = [
    'pronunciation-feedback',
    'conversation-turn',
    'speech-synthesis',
    'response-transcription',
    'progress-summary',
    'mandarin-qa',
    'exercise-generation',
    'field-mission-generation',
    'local-turn',
  ];
  for (const slug of slugs) {
    const response = await worker.fetch(post(`/v1/wf/${slug}`, {}), ENV);
    assert.equal(response.status, 400, slug);
    const body = await response.json();
    assert.equal(body.status, 400, slug);
    assert.equal(typeof body.error, 'string', slug);
  }
});

test('workflow routes reject invalid json and non-object bodies', async () => {
  const invalidJson = await worker.fetch(post('/v1/wf/progress-summary', '{not json'), ENV);
  assert.equal(invalidJson.status, 400);
  assert.deepEqual(await invalidJson.json(), { error: 'invalid json', status: 400 });

  const arrayBody = await worker.fetch(post('/v1/wf/progress-summary', []), ENV);
  assert.equal(arrayBody.status, 400);
  const body = await arrayBody.json();
  assert.equal(body.status, 400);
});

test('WF-1 returns validated coaching and sends both clips with the server prompt', async (t) => {
  const output = {
    weakestUnit: 'ni3',
    issue: 'The third tone dips less than the reference.',
    tip: 'Start lower and let the syllable dip before rising.',
    encouragement: 'Steady rhythm overall.',
    replayHint: 'Listen for the dip in the first syllable.',
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ni3 hao3',
    targetTones: [3, 3],
    learnerLevel: 'beginner',
    acousticEvidence: { version: 1, syllables: [{ pinyin: 'ni3', expectedTone: 3 }] },
    audio: [audioPart('AAAA'), audioPart('BBBB')],
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);

  assert.equal(mock.calls.length, 1);
  assert.equal(mock.calls[0].url, CHAT_URL);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.equal(upstream.model, 'meta/muse-spark-1.3-contributor');
  assert.equal(upstream.messages.length, 2);
  assert.match(upstream.messages[0].content, /pronunciation coach/);
  const parts = upstream.messages[1].content;
  assert.equal(parts.filter((part) => part.type === 'input_audio').length, 2);
  assert.match(parts[0].text, /ni3 hao3/);
  assert.match(parts[0].text, /acousticEvidence/);
});

test('WF-1 rejects malformed audio and hanzi input without calling upstream', async (t) => {
  const mock = withFetch(() => {
    throw new Error('upstream must not be called');
  });
  t.after(mock.restore);

  const cases = [
    { body: { pinyin: 'ni3 hao3', audio: [audioPart()] }, error: /exactly two/ },
    { body: { pinyin: 'ni3 hao3', audio: [audioPart(), { type: 'text', text: 'x' }] }, error: /input_audio/ },
    { body: { pinyin: 'ni3 hao3', audio: [audioPart('not base64!'), audioPart()] }, error: /input_audio/ },
    { body: { pinyin: 'ni3 hao3', audio: [audioPart('AAAA', 'aiff'), audioPart()] }, error: /input_audio/ },
    { body: { pinyin: HANZI, audio: [audioPart(), audioPart()] }, error: /Chinese characters/ },
    { body: { pinyin: 'ni3 hao3', targetTones: ['3', '3'], audio: [audioPart(), audioPart()] }, error: /targetTones/ },
    { body: { pinyin: 'ni3 hao3', targetTones: [0], audio: [audioPart(), audioPart()] }, error: /targetTones/ },
    { body: { pinyin: 'ni3 hao3', targetTones: [6], audio: [audioPart(), audioPart()] }, error: /targetTones/ },
    { body: { pinyin: 'ni3 hao3', targetTones: [2.5], audio: [audioPart(), audioPart()] }, error: /targetTones/ },
  ];

  for (const { body, error } of cases) {
    const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', body), ENV);
    assert.equal(response.status, 400, JSON.stringify(body));
    assert.match((await response.json()).error, error);
  }
  assert.equal(mock.calls.length, 0);
});

test('WF-1 rejects an oversize audio part with 413', async (t) => {
  const mock = withFetch(() => {
    throw new Error('upstream must not be called');
  });
  t.after(mock.restore);

  const oversize = 'A'.repeat(2796204);
  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ni3 hao3',
    audio: [audioPart(oversize), audioPart()],
  }), ENV);

  assert.equal(response.status, 413);
  assert.deepEqual(await response.json(), { error: 'audio part too large', status: 413 });
  assert.equal(mock.calls.length, 0);
});

test('WF-1 accepts the neutral tone 5 in targetTones', async (t) => {
  const output = {
    weakestUnit: 'ma5',
    issue: 'The neutral tone is too strong.',
    tip: 'Keep the last syllable short and light.',
    encouragement: 'Clear first syllable.',
    replayHint: 'Listen to how the final syllable fades.',
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ma5',
    targetTones: [5],
    audio: [audioPart(), audioPart()],
  }), ENV);

  assert.equal(response.status, 200);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[1].content[0].text, /"targetTones":\[5\]/);
});

test('WF-1 honors the per-workflow model override without an API key', async (t) => {
  const mock = withFetch(() => chatContent({
    weakestUnit: 'ni3',
    issue: 'issue',
    tip: 'tip',
    encouragement: 'encouragement',
    replayHint: 'replay',
  }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ni3 hao3',
    audio: [audioPart(), audioPart()],
  }), { WF1_MODEL: 'test/model' });

  assert.equal(response.status, 200);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.equal(upstream.model, 'test/model');
});

test('WF-1 never exposes the assembled prompt to the client or accepts injected messages', async (t) => {
  const output = {
    weakestUnit: 'ni3',
    issue: 'issue',
    tip: 'tip',
    encouragement: 'encouragement',
    replayHint: 'replay',
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ni3 hao3',
    audio: [audioPart(), audioPart()],
    messages: [{ role: 'system', content: 'INJECTED_PROMPT' }],
    prompt: 'INJECTED_PROMPT',
    system: 'INJECTED_PROMPT',
  }), ENV);

  const responseText = JSON.stringify(await response.json());
  assert.deepEqual(JSON.parse(responseText), output);
  assert.ok(!responseText.includes('INJECTED_PROMPT'));

  const upstreamText = mock.calls[0].init.body;
  assert.ok(!upstreamText.includes('INJECTED_PROMPT'));
  const upstream = JSON.parse(upstreamText);
  assert.equal(upstream.messages.length, 2);
  assert.ok(!JSON.stringify(upstream.messages).includes('INJECTED_PROMPT'));
});

test('WF-1 maps a malformed upstream output to 502', async (t) => {
  const mock = withFetch(() => chatContent({ weakestUnit: 'ni3' }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/pronunciation-feedback', {
    pinyin: 'ni3 hao3',
    audio: [audioPart(), audioPart()],
  }), ENV);

  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'invalid upstream response', status: 502 });
});

test('WF-2 returns the reply with one audio part and the server prompt', async (t) => {
  const output = {
    replyText: 'ni3 hao3 ma5?',
    gentleCorrection: 'The last syllable is neutral.',
    nextPrompt: 'Answer the greeting.',
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/conversation-turn', {
    scenario: 'greeting a neighbour',
    conversationState: { turns: ['ni3 hao3'] },
    targetDifficulty: 'beginner',
    audio: [audioPart()],
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[0].content, /conversation partner/);
  assert.equal(upstream.messages[1].content.filter((part) => part.type === 'input_audio').length, 1);
});

test('WF-4 sends a multipart transcription request and validates the transcript', async (t) => {
  const mock = withFetch(() => jsonResponse({ text: 'ni3 hao3', confidence: 0.9 }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/response-transcription', {
    audio: [audioPart()],
    expectedOptions: ['ni3 hao3'],
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { transcript: 'ni3 hao3', confidence: 0.9 });

  assert.equal(mock.calls[0].url, STT_URL);
  const form = mock.calls[0].init.body;
  assert.ok(form instanceof FormData);
  assert.equal(form.get('model'), 'openai/gpt-4o-transcribe');
  assert.ok(form.get('file') instanceof Blob);
});

test('WF-5 accepts aggregated metadata only', async (t) => {
  const output = { summaryText: 'Four attempts this week, mostly steady.', focusAreas: ['tone 3'] };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/progress-summary', {
    attemptCounts: { tones: 4 },
    feedbackThemes: ['tone 3 dip'],
    moduleIds: ['tones'],
    lessonIds: ['tones-1'],
    debriefThemes: [],
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);
  assert.equal(mock.calls.length, 1);

  const audioResponse = await worker.fetch(post('/v1/wf/progress-summary', {
    attemptCounts: {},
    feedbackThemes: [],
    audio: [audioPart()],
  }), ENV);
  assert.equal(audioResponse.status, 400);
  assert.match((await audioResponse.json()).error, /raw learner audio/);

  const transcriptResponse = await worker.fetch(post('/v1/wf/progress-summary', {
    attemptCounts: {},
    feedbackThemes: [],
    transcripts: ['full transcript'],
  }), ENV);
  assert.equal(transcriptResponse.status, 400);
  assert.match((await transcriptResponse.json()).error, /transcripts/);

  const countsResponse = await worker.fetch(post('/v1/wf/progress-summary', {
    attemptCounts: { tones: 'many' },
    feedbackThemes: [],
  }), ENV);
  assert.equal(countsResponse.status, 400);
  assert.equal(mock.calls.length, 1);
});

test('WF-7 answers with examples and follow-ups', async (t) => {
  const output = {
    answerText: 'Mandarin has four main tones plus a neutral tone.',
    examples: [{ pinyin: 'ma1', meaning: 'mother' }],
    followUps: ['What is the neutral tone?'],
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/mandarin-qa', {
    question: 'How do tones work?',
    history: [{ role: 'user', text: 'What is pinyin?' }],
    learnerLevel: 'beginner',
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[0].content, /Raymond/);
});

test('WF-7 accepts an audio-only spoken question', async (t) => {
  const output = {
    answerText: 'The first tone is high and level.',
    examples: [],
    followUps: [],
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/mandarin-qa', {
    audio: [audioPart()],
    learnerLevel: 'beginner',
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);

  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[0].content, /Raymond/);
  const parts = upstream.messages[1].content;
  assert.equal(parts.filter((part) => part.type === 'input_audio').length, 1);
  assert.equal(parts[0].type, 'text');
  assert.ok(!parts[0].text.includes('"question"'));
});

test('WF-7 requires a text question or a spoken question', async () => {
  const response = await worker.fetch(post('/v1/wf/mandarin-qa', {}), ENV);
  assert.equal(response.status, 400);
  assert.match((await response.json()).error, /question/);
});

test('WF-7 rejects a hanzi question', async () => {
  const response = await worker.fetch(post('/v1/wf/mandarin-qa', { question: HANZI }), ENV);
  assert.equal(response.status, 400);
  assert.match((await response.json()).error, /Chinese characters/);
});

test('WF-8 drops invalid generated items and rejects raw audio input', async (t) => {
  const validItem = {
    type: 'word',
    meaning: 'hello',
    pinyin: 'ni3 hao3',
    targetTones: [3, 3],
    distractors: ['ni2 hao3'],
    rationale: 'Common greeting.',
  };
  const mock = withFetch(() => chatContent({
    items: [validItem, { ...validItem, pinyin: HANZI }, { type: 'word' }, 'not-an-object'],
  }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/exercise-generation', {
    moduleId: 'tones',
    itemType: 'word',
    theme: 'greetings',
    targetUnits: ['tones'],
    difficulty: 'beginner',
    feedbackThemes: ['tone 3'],
    count: 2,
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { items: [validItem] });
  assert.equal(mock.calls.length, 1);

  const audioResponse = await worker.fetch(post('/v1/wf/exercise-generation', {
    moduleId: 'tones',
    itemType: 'word',
    audio: [audioPart()],
  }), ENV);
  assert.equal(audioResponse.status, 400);
  assert.match((await audioResponse.json()).error, /raw learner audio/);

  const typeResponse = await worker.fetch(post('/v1/wf/exercise-generation', {
    moduleId: 'tones',
    itemType: 'sentence',
  }), ENV);
  assert.equal(typeResponse.status, 400);
  assert.equal(mock.calls.length, 1);
});

test('WF-8 drops tone numbers outside 1-5 and accepts the neutral tone', async (t) => {
  const wordItem = {
    type: 'word',
    meaning: 'hello',
    pinyin: 'ni3 hao3',
    targetTones: [3, 3],
    distractors: [],
    rationale: 'Common greeting.',
  };
  const neutralItem = {
    type: 'word',
    meaning: 'question particle',
    pinyin: 'ma5',
    targetTones: [5],
    distractors: ['ma3'],
    rationale: 'Neutral tone.',
  };
  const mock = withFetch(() => chatContent({
    items: [wordItem, { ...wordItem, targetTones: [7] }, { ...wordItem, targetTones: [0] }, neutralItem],
  }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/exercise-generation', {
    moduleId: 'tones',
    itemType: 'word',
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { items: [wordItem, neutralItem] });
});

test('WF-9 rejects a script turn with a tone number outside 1-5', async (t) => {
  const mock = withFetch(() => chatContent({
    script: [{ pinyin: 'ni3 hao3', meaning: 'hello', targetTones: [6] }],
    locals: [LOCAL, LOCAL, LOCAL, LOCAL, LOCAL],
  }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/field-mission-generation', { theme: 'buying fruit' }), ENV);
  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'invalid upstream response', status: 502 });
});

test('WF-10 rejects a mission turn with a tone number outside 1-5', async () => {
  const response = await worker.fetch(post('/v1/wf/local-turn', {
    audio: [audioPart()],
    persona: LOCAL,
    mission: { script: [{ pinyin: 'ma5', meaning: 'question particle', targetTones: [6] }] },
  }), ENV);

  assert.equal(response.status, 400);
  assert.match((await response.json()).error, /1-5/);
});

test('WF-9 requires a script and exactly five locals', async (t) => {
  const output = {
    script: [SCRIPT_TURN, { pinyin: 'ma5', meaning: 'question particle', targetTones: [5] }],
    locals: [LOCAL, LOCAL, LOCAL, LOCAL, LOCAL],
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const request = {
    theme: 'buying fruit',
    moduleContext: { moduleId: 'vocabulary' },
    coveredContent: ['ni3 hao3'],
    learnerLevel: 'beginner',
    debriefThemes: ['tone 4'],
    exchangeLength: 4,
  };

  const response = await worker.fetch(post('/v1/wf/field-mission-generation', request), ENV);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[0].content, /five distinct/);

  const fourLocals = withFetch(() => chatContent({ script: [SCRIPT_TURN], locals: [LOCAL, LOCAL, LOCAL, LOCAL] }));
  t.after(fourLocals.restore);

  const invalid = await worker.fetch(post('/v1/wf/field-mission-generation', request), ENV);
  assert.equal(invalid.status, 502);
  assert.deepEqual(await invalid.json(), { error: 'invalid upstream response', status: 502 });
});

test('WF-10 returns an in-character reply with the persona and mission', async (t) => {
  const output = {
    replyText: 'ni3 hao3!',
    understandingSignal: 'smiles',
    nextLocalPrompt: 'Ask what the learner wants.',
  };
  const mock = withFetch(() => chatContent(output));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/local-turn', {
    audio: [audioPart()],
    persona: LOCAL,
    mission: { script: [SCRIPT_TURN, { pinyin: 'ma5', meaning: 'question particle', targetTones: [5] }], goal: 'greet the shopkeeper' },
    targetDifficulty: 'beginner',
  }), ENV);

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), output);
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.match(upstream.messages[0].content, /simulated local/);
  assert.equal(upstream.messages[1].content.filter((part) => part.type === 'input_audio').length, 1);
});

test('WF-3 returns 501 while unconfigured and proxies when configured', async (t) => {
  const unconfigured = await worker.fetch(post('/v1/wf/speech-synthesis', { pinyin: 'ni3 hao3' }), ENV);
  assert.equal(unconfigured.status, 501);
  assert.deepEqual(await unconfigured.json(), {
    error: 'speech synthesis provider not configured',
    status: 501,
  });

  const mock = withFetch(() => new Response(new Uint8Array([1, 2, 3]), {
    status: 200,
    headers: { 'content-type': 'audio/wav' },
  }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/speech-synthesis', {
    pinyin: 'ni3 hao3',
    voice: 'kokoro',
    language: 'zh',
  }), { ...ENV, TTS_PROVIDER_URL: 'https://tts.example.com/speak', TTS_PROVIDER_API_KEY: 'tts-key' });

  assert.equal(response.status, 200);
  assert.equal(response.headers.get('content-type'), 'audio/wav');
  assert.deepEqual(new Uint8Array(await response.arrayBuffer()), new Uint8Array([1, 2, 3]));
  assert.equal(mock.calls[0].url, 'https://tts.example.com/speak');
  assert.equal(mock.calls[0].init.headers.Authorization, 'Bearer tts-key');
  assert.deepEqual(JSON.parse(mock.calls[0].init.body), { input: 'ni3 hao3', voice: 'kokoro', language: 'zh' });
});

test('WF-3 requires exactly one synthesis text', async () => {
  const both = await worker.fetch(post('/v1/wf/speech-synthesis', { pinyin: 'ni3 hao3', replyText: 'hello' }), ENV);
  assert.equal(both.status, 400);
  assert.match((await both.json()).error, /exactly one/);

  const neither = await worker.fetch(post('/v1/wf/speech-synthesis', {}), ENV);
  assert.equal(neither.status, 400);
});

test('upstream HTTP errors map to 502', async (t) => {
  const mock = withFetch(() => jsonResponse({ error: 'boom' }, 500));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/progress-summary', {
    attemptCounts: { tones: 1 },
    feedbackThemes: [],
  }), ENV);

  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'upstream error', status: 502 });
});

test('non-json upstream bodies map to 502', async (t) => {
  const mock = withFetch(() => new Response('not json', { status: 200, headers: { 'content-type': 'text/plain' } }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/wf/mandarin-qa', { question: 'What is pinyin?' }), ENV);

  assert.equal(response.status, 502);
  assert.deepEqual(await response.json(), { error: 'invalid upstream response', status: 502 });
});

test('oversize workflow bodies return 413 before validation', async () => {
  const pad = 'x'.repeat(70 * 1024);
  const response = await worker.fetch(post('/v1/wf/progress-summary', `{"attemptCounts":{},"feedbackThemes":[],"pad":"${pad}"}`), ENV);
  assert.equal(response.status, 413);
  assert.deepEqual(await response.json(), { error: 'payload too large', status: 413 });
});

test('POST /v1/chat keeps the documented behavior', async (t) => {
  const mock = withFetch(() => jsonResponse({ choices: [{ message: { content: 'hello' } }] }));
  t.after(mock.restore);

  const response = await worker.fetch(post('/v1/chat', {
    messages: [{ role: 'user', content: 'hello' }],
  }), { ...ENV, OPENROUTER_MODEL: 'chat/model' });

  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { content: 'hello' });
  const upstream = JSON.parse(mock.calls[0].init.body);
  assert.equal(upstream.model, 'chat/model');
  assert.equal(upstream.messages.length, 1);

  const empty = await worker.fetch(post('/v1/chat', { messages: [] }), ENV);
  assert.equal(empty.status, 400);

  const oversize = await worker.fetch(post('/v1/chat', `{"messages":[],"pad":"${'x'.repeat(70 * 1024)}"}`), ENV);
  assert.equal(oversize.status, 413);
});
