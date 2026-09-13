import {
  audioPartBytes,
  containsHanzi,
  containsHanziDeep,
  findAudioPart,
  isAudioPart,
  isNonEmptyString,
  isToneArray,
  isPlainObject,
  isStringArray,
} from './validate.js';

const MUSE_SPARK = 'meta/muse-spark-1.3-contributor';
const STT_MODEL = 'openai/gpt-4o-transcribe';

const TEXT_BODY_BYTES = 64 * 1024;
const TWO_CLIP_BODY_BYTES = 4 * 1024 * 1024;
const ONE_CLIP_BODY_BYTES = 2 * 1024 * 1024;
const MAX_AUDIO_PART_BYTES = 2 * 1024 * 1024;

const ITEM_TYPES = new Set(['word', 'phrase', 'minimalPair', 'dialogue']);
const RAYMOND_ROLES = new Set(['user', 'raymond']);

function fail(error, status = 400) {
  return { error, status };
}

function validateAudio(body, count) {
  const audio = body.audio;
  if (!Array.isArray(audio) || audio.length !== count) {
    return fail(`audio must contain exactly ${count === 1 ? 'one' : 'two'} input_audio parts`);
  }
  for (const part of audio) {
    if (!isAudioPart(part)) {
      return fail('audio parts must be input_audio content parts with base64 data and a supported format');
    }
    if (audioPartBytes(part) > MAX_AUDIO_PART_BYTES) {
      return fail('audio part too large', 413);
    }
  }
  return { audio };
}

function rejectAudio(body) {
  if (findAudioPart(body)) return fail('raw learner audio is not accepted by this workflow');
  return null;
}

function stringFields(output, required, optional = []) {
  if (!isPlainObject(output)) return null;
  const result = {};
  for (const key of required) {
    if (!isNonEmptyString(output[key])) return null;
    result[key] = output[key];
  }
  for (const key of optional) {
    const value = output[key];
    if (value === undefined || value === null || value === '') continue;
    if (!isNonEmptyString(value)) return null;
    result[key] = value;
  }
  if (containsHanziDeep(result)) return null;
  return result;
}

function turnFields(value) {
  if (!isPlainObject(value)) return null;
  if (!isNonEmptyString(value.pinyin) || containsHanzi(value.pinyin)) return null;
  if (!isNonEmptyString(value.meaning) || containsHanzi(value.meaning)) return null;
  if (!isToneArray(value.targetTones) || value.targetTones.length === 0) return null;
  return { pinyin: value.pinyin, meaning: value.meaning, targetTones: value.targetTones };
}

function localFields(value) {
  if (!isPlainObject(value)) return null;
  const fields = ['label', 'settingRole', 'personality', 'voiceProfile', 'pace'];
  const result = {};
  for (const field of fields) {
    if (!isNonEmptyString(value[field])) return null;
    result[field] = value[field];
  }
  if (containsHanziDeep(result)) return null;
  return result;
}

const WF1_PROMPT = `You are the pronunciation coach of an audio-first Mandarin learning app for beginners. The user message contains the expected phrase in pinyin with tone numbers, the expected tones, an optional measured acoustic evidence object, a reference audio clip, and the learner attempt clip. Compare the attempt with the reference and treat the evidence object as measured facts. Reply with a single JSON object with exactly these string fields: "weakestUnit" (the pinyin syllable or short unit that most needs work), "issue" (what differs from the reference), "tip" (one actionable instruction), "encouragement" (one short encouraging line), and "replayHint" (what to listen for on replay). Name Mandarin units in pinyin with tone numbers, for example ma2; write the rest in English. Never output Chinese characters. Coaching is advisory, never a grade or pass/fail.`;

const WF2_PROMPT = `You are the patient conversation partner of an audio-first Mandarin learning app for beginners. The user message contains the scenario, the conversation state with prior transcripts and corrections, a target difficulty, and the learner spoken turn. Understand the turn and continue the scenario naturally at the requested difficulty. Reply with a single JSON object: "replyText" (required, the partner reply), "gentleCorrection" (optional, at most one gentle correction), and "nextPrompt" (optional, a short prompt that keeps the exchange moving). Show Mandarin in pinyin with tone numbers and write the rest in English; never output Chinese characters. Corrections are gentle and optional.`;

const WF5_PROMPT = `You are the progress summary writer of an audio-first Mandarin learning app. The user message contains aggregated practice metadata only: attempt counts, per-unit feedback themes, module and lesson ids, and debrief themes. Write a short, warm, spoken-style summary of the practice history and choose at most three recurring focus areas. Reply with a single JSON object: "summaryText" (string) and "focusAreas" (array of short strings). Use English and pinyin with tone numbers; never output Chinese characters. Raw audio and full transcripts are not available, so do not invent details.`;

const WF7_PROMPT = `You are Raymond, the always-available helper of an audio-first Mandarin learning app. Answer any question the learner has about the Mandarin language and its use: pronunciation, tones, pinyin conventions, meaning, usage, and light cultural context. Decline or redirect questions outside that scope in one short sentence instead of acting as a general assistant. Reply with a single JSON object: "answerText" (string), "examples" (array of objects with "pinyin" and "meaning"), and "followUps" (array of short strings). Show Mandarin in pinyin with tone numbers; never output Chinese characters.`;

const WF8_PROMPT = `You are the runtime exercise generator of an audio-first Mandarin learning app. The user message requests practice items for a module, an item type, a theme, target units, a difficulty, recurring feedback themes, and a count. Reply with a single JSON object: {"items": [{"type": "word|phrase|minimalPair|dialogue", "meaning": "...", "pinyin": "...", "hangul": "...", "targetTones": [1,2], "distractors": ["..."], "rationale": "..."}]}. "pinyin" carries tone numbers; "hangul" is optional Hangul script; "targetTones" lists the tone number of each syllable; "distractors" are plausible wrong answers; "rationale" is one short English sentence. Generate exactly the requested count and do not repeat bundled content. Never output Chinese characters.`;

const WF9_PROMPT = `You are the field mission generator of an audio-first Mandarin learning app. The user message contains a theme and module context, covered content, the learner level, recent debrief themes, and a requested exchange length. Produce one bite-sized functional exchange and the five simulated locals the learner will use it with. Reply with a single JSON object: {"script": [{"pinyin": "...", "meaning": "...", "targetTones": [1,2]}], "locals": [{"label": "...", "settingRole": "...", "personality": "...", "voiceProfile": "...", "pace": "..."}]}. The script is a short ordered list of spoken turns; "locals" has exactly five distinct entries. Show Mandarin in pinyin with tone numbers; never output Chinese characters.`;

const WF10_PROMPT = `You are role-playing exactly one simulated local person in the field mission of an audio-first Mandarin learning app. The user message contains the local persona, the mission script and goal, the conversation state, a target difficulty, and the learner spoken turn. Respond in character at the requested pace and comprehension level, as a member of the public, and never correct or coach the learner. Reply with a single JSON object: "replyText" (required), "understandingSignal" (optional, a short in-character signal that the learner was understood), and "nextLocalPrompt" (optional, the next thing the local says to keep the exchange going). Show Mandarin in pinyin with tone numbers and write the rest in English; never output Chinese characters.`;

export const WORKFLOWS = [
  {
    id: 'WF-1',
    name: 'Pronunciation feedback',
    slug: 'pronunciation-feedback',
    doc: 'docs/08-ai-workflows.md WF-1',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF1_MODEL',
    maxBodyBytes: TWO_CLIP_BODY_BYTES,
    temperature: 0.2,
    maxTokens: 400,
    timeoutMs: 12000,
    validateInput(body) {
      const audioCheck = validateAudio(body, 2);
      if (audioCheck.error) return audioCheck;
      if (!isNonEmptyString(body.pinyin)) return fail('pinyin must be a non-empty string');
      if (containsHanzi(body.pinyin)) return fail('pinyin must not contain Chinese characters');
      if (body.targetTones !== undefined && !isToneArray(body.targetTones)) {
        return fail('targetTones must be an array of integers between 1 and 5');
      }
      if (body.learnerLevel !== undefined && !isNonEmptyString(body.learnerLevel)) {
        return fail('learnerLevel must be a non-empty string');
      }
      if (body.acousticEvidence !== undefined) {
        if (!isPlainObject(body.acousticEvidence)) return fail('acousticEvidence must be an object');
        if (containsHanziDeep(body.acousticEvidence)) return fail('acousticEvidence must not contain Chinese characters');
        const syllables = body.acousticEvidence.syllables;
        if (syllables !== undefined && (!Array.isArray(syllables) || !syllables.every(isPlainObject))) {
          return fail('acousticEvidence.syllables must be an array of objects');
        }
      }
      return {
        value: {
          audio: audioCheck.audio,
          pinyin: body.pinyin,
          targetTones: body.targetTones ?? [],
          learnerLevel: body.learnerLevel ?? 'beginner',
          acousticEvidence: body.acousticEvidence,
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF1_PROMPT },
        {
          role: 'user',
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                pinyin: value.pinyin,
                targetTones: value.targetTones,
                learnerLevel: value.learnerLevel,
                acousticEvidence: value.acousticEvidence ?? null,
              }),
            },
            ...value.audio,
          ],
        },
      ];
    },
    validateOutput(output) {
      return stringFields(output, ['weakestUnit', 'issue', 'tip', 'encouragement', 'replayHint']);
    },
  },
  {
    id: 'WF-2',
    name: 'Conversation turn',
    slug: 'conversation-turn',
    doc: 'docs/08-ai-workflows.md WF-2',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF2_MODEL',
    maxBodyBytes: ONE_CLIP_BODY_BYTES,
    temperature: 0.6,
    maxTokens: 400,
    timeoutMs: 12000,
    validateInput(body) {
      const audioCheck = validateAudio(body, 1);
      if (audioCheck.error) return audioCheck;
      if (!isNonEmptyString(body.scenario) || containsHanzi(body.scenario)) {
        return fail('scenario must be a non-empty string without Chinese characters');
      }
      if (body.conversationState !== undefined && !isPlainObject(body.conversationState)) {
        return fail('conversationState must be an object');
      }
      if (body.targetDifficulty !== undefined && !isNonEmptyString(body.targetDifficulty)) {
        return fail('targetDifficulty must be a non-empty string');
      }
      return {
        value: {
          audio: audioCheck.audio,
          scenario: body.scenario,
          conversationState: body.conversationState,
          targetDifficulty: body.targetDifficulty,
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF2_PROMPT },
        {
          role: 'user',
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                scenario: value.scenario,
                conversationState: value.conversationState ?? null,
                targetDifficulty: value.targetDifficulty ?? null,
              }),
            },
            ...value.audio,
          ],
        },
      ];
    },
    validateOutput(output) {
      return stringFields(output, ['replyText'], ['gentleCorrection', 'nextPrompt']);
    },
  },
  {
    id: 'WF-3',
    name: 'Speech synthesis',
    slug: 'speech-synthesis',
    doc: 'docs/08-ai-workflows.md WF-3',
    kind: 'tts',
    model: null,
    envModel: null,
    maxBodyBytes: TEXT_BODY_BYTES,
    timeoutMs: 20000,
    validateInput(body) {
      const hasPinyin = body.pinyin !== undefined;
      const hasReplyText = body.replyText !== undefined;
      if (hasPinyin === hasReplyText) return fail('exactly one of pinyin or replyText is required');
      const text = hasPinyin ? body.pinyin : body.replyText;
      if (!isNonEmptyString(text)) return fail('the synthesis text must be a non-empty string');
      if (containsHanzi(text)) return fail('the synthesis text must not contain Chinese characters');
      if (body.voice !== undefined && !isNonEmptyString(body.voice)) return fail('voice must be a non-empty string');
      if (body.language !== undefined && !isNonEmptyString(body.language)) return fail('language must be a non-empty string');
      return {
        value: {
          input: text.trim(),
          source: hasPinyin ? 'pinyin' : 'replyText',
          voice: body.voice,
          language: body.language,
        },
      };
    },
  },
  {
    id: 'WF-4',
    name: 'Response transcription',
    slug: 'response-transcription',
    doc: 'docs/08-ai-workflows.md WF-4',
    kind: 'stt',
    model: STT_MODEL,
    envModel: 'WF4_MODEL',
    maxBodyBytes: ONE_CLIP_BODY_BYTES,
    timeoutMs: 20000,
    validateInput(body) {
      const audioCheck = validateAudio(body, 1);
      if (audioCheck.error) return audioCheck;
      if (body.expectedOptions !== undefined && !isStringArray(body.expectedOptions)) {
        return fail('expectedOptions must be an array of strings');
      }
      if (body.keywords !== undefined && !isStringArray(body.keywords)) {
        return fail('keywords must be an array of strings');
      }
      return {
        value: {
          audio: audioCheck.audio,
          expectedOptions: body.expectedOptions,
          keywords: body.keywords,
        },
      };
    },
    validateOutput(output) {
      if (!isPlainObject(output)) return null;
      if (!isNonEmptyString(output.transcript) || containsHanzi(output.transcript)) return null;
      const result = { transcript: output.transcript };
      if (output.confidence !== undefined) {
        if (typeof output.confidence !== 'number' || output.confidence < 0 || output.confidence > 1) return null;
        result.confidence = output.confidence;
      }
      if (output.matchedOptionId !== undefined) {
        if (!isNonEmptyString(output.matchedOptionId)) return null;
        result.matchedOptionId = output.matchedOptionId;
      }
      return result;
    },
  },
  {
    id: 'WF-5',
    name: 'Progress summary',
    slug: 'progress-summary',
    doc: 'docs/08-ai-workflows.md WF-5',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF5_MODEL',
    maxBodyBytes: TEXT_BODY_BYTES,
    temperature: 0.4,
    maxTokens: 500,
    timeoutMs: 20000,
    validateInput(body) {
      const audioError = rejectAudio(body);
      if (audioError) return audioError;
      if (body.transcripts !== undefined || body.transcript !== undefined) {
        return fail('full transcripts are not accepted by this workflow');
      }
      const counts = body.attemptCounts;
      if (!isPlainObject(counts) || !Object.values(counts).every((entry) => typeof entry === 'number' && Number.isFinite(entry) && entry >= 0)) {
        return fail('attemptCounts must be an object of non-negative numbers');
      }
      if (!isStringArray(body.feedbackThemes)) return fail('feedbackThemes must be an array of strings');
      if (body.moduleIds !== undefined && !isStringArray(body.moduleIds)) return fail('moduleIds must be an array of strings');
      if (body.lessonIds !== undefined && !isStringArray(body.lessonIds)) return fail('lessonIds must be an array of strings');
      if (body.debriefThemes !== undefined && !isStringArray(body.debriefThemes)) return fail('debriefThemes must be an array of strings');
      return {
        value: {
          attemptCounts: counts,
          feedbackThemes: body.feedbackThemes,
          moduleIds: body.moduleIds ?? [],
          lessonIds: body.lessonIds ?? [],
          debriefThemes: body.debriefThemes ?? [],
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF5_PROMPT },
        { role: 'user', content: JSON.stringify(value) },
      ];
    },
    validateOutput(output) {
      const base = stringFields(output, ['summaryText']);
      if (!base) return null;
      if (!Array.isArray(output.focusAreas) || !isStringArray(output.focusAreas)) return null;
      if (containsHanziDeep(output.focusAreas)) return null;
      return { ...base, focusAreas: output.focusAreas };
    },
  },
  {
    id: 'WF-7',
    name: 'Raymond - Mandarin Q&A',
    slug: 'mandarin-qa',
    doc: 'docs/08-ai-workflows.md WF-7',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF7_MODEL',
    maxBodyBytes: ONE_CLIP_BODY_BYTES,
    temperature: 0.5,
    maxTokens: 900,
    timeoutMs: 20000,
    validateInput(body) {
      let audio = [];
      if (body.audio !== undefined) {
        const audioCheck = validateAudio(body, 1);
        if (audioCheck.error) return audioCheck;
        audio = audioCheck.audio;
      }
      if (body.question !== undefined) {
        if (!isNonEmptyString(body.question) || containsHanzi(body.question)) {
          return fail('question must be a non-empty string without Chinese characters');
        }
      } else if (audio.length === 0) {
        return fail('a text question or exactly one spoken-question audio part is required');
      }
      if (body.history !== undefined) {
        if (!Array.isArray(body.history) || !body.history.every((entry) => isPlainObject(entry) && RAYMOND_ROLES.has(entry.role) && isNonEmptyString(entry.text))) {
          return fail('history must be an array of { role, text } entries with role user or raymond');
        }
      }
      if (body.learnerLevel !== undefined && !isNonEmptyString(body.learnerLevel)) {
        return fail('learnerLevel must be a non-empty string');
      }
      return {
        value: {
          audio,
          question: body.question,
          history: body.history,
          learnerLevel: body.learnerLevel,
        },
      };
    },
    buildMessages(value) {
      const metadata = { history: value.history ?? [], learnerLevel: value.learnerLevel ?? null };
      if (value.question !== undefined) metadata.question = value.question;
      return [
        { role: 'system', content: WF7_PROMPT },
        { role: 'user', content: [{ type: 'text', text: JSON.stringify(metadata) }].concat(value.audio) },
      ];
    },
    validateOutput(output) {
      const base = stringFields(output, ['answerText']);
      if (!base) return null;
      if (!Array.isArray(output.examples) || !output.examples.every((entry) => isPlainObject(entry) && isNonEmptyString(entry.pinyin) && isNonEmptyString(entry.meaning))) {
        return null;
      }
      if (!isStringArray(output.followUps)) return null;
      const result = {
        ...base,
        examples: output.examples.map((entry) => ({ pinyin: entry.pinyin, meaning: entry.meaning })),
        followUps: output.followUps,
      };
      if (containsHanziDeep(result)) return null;
      return result;
    },
  },
  {
    id: 'WF-8',
    name: 'Exercise generation (runtime)',
    slug: 'exercise-generation',
    doc: 'docs/08-ai-workflows.md WF-8',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF8_MODEL',
    maxBodyBytes: TEXT_BODY_BYTES,
    temperature: 0.7,
    maxTokens: 1500,
    timeoutMs: 25000,
    validateInput(body) {
      const audioError = rejectAudio(body);
      if (audioError) return audioError;
      if (!isNonEmptyString(body.moduleId)) return fail('moduleId must be a non-empty string');
      if (!ITEM_TYPES.has(body.itemType)) return fail('itemType must be one of word, phrase, minimalPair, dialogue');
      if (body.theme !== undefined && !isNonEmptyString(body.theme)) return fail('theme must be a non-empty string');
      if (body.targetUnits !== undefined && !isStringArray(body.targetUnits)) return fail('targetUnits must be an array of strings');
      if (body.difficulty !== undefined && !isNonEmptyString(body.difficulty)) return fail('difficulty must be a non-empty string');
      if (body.feedbackThemes !== undefined && !isStringArray(body.feedbackThemes)) return fail('feedbackThemes must be an array of strings');
      if (body.count !== undefined && !(Number.isInteger(body.count) && body.count > 0 && body.count <= 25)) {
        return fail('count must be an integer between 1 and 25');
      }
      return {
        value: {
          moduleId: body.moduleId,
          itemType: body.itemType,
          theme: body.theme,
          targetUnits: body.targetUnits ?? [],
          difficulty: body.difficulty ?? 'beginner',
          feedbackThemes: body.feedbackThemes ?? [],
          count: body.count ?? 5,
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF8_PROMPT },
        { role: 'user', content: JSON.stringify(value) },
      ];
    },
    validateOutput(output) {
      if (!isPlainObject(output) || !Array.isArray(output.items)) return null;
      const items = [];
      for (const item of output.items) {
        if (!isPlainObject(item)) continue;
        if (!ITEM_TYPES.has(item.type)) continue;
        if (!isNonEmptyString(item.meaning)) continue;
        if (!isNonEmptyString(item.pinyin) || containsHanzi(item.pinyin)) continue;
        if (item.hangul !== undefined && !isNonEmptyString(item.hangul)) continue;
        if (!isToneArray(item.targetTones) || item.targetTones.length === 0) continue;
        if (!isStringArray(item.distractors)) continue;
        if (!isNonEmptyString(item.rationale)) continue;
        const cleaned = {
          type: item.type,
          meaning: item.meaning,
          pinyin: item.pinyin,
          targetTones: item.targetTones,
          distractors: item.distractors,
          rationale: item.rationale,
        };
        if (item.hangul !== undefined) cleaned.hangul = item.hangul;
        if (containsHanziDeep(cleaned)) continue;
        items.push(cleaned);
      }
      return { items };
    },
  },
  {
    id: 'WF-9',
    name: 'Field mission generation',
    slug: 'field-mission-generation',
    doc: 'docs/08-ai-workflows.md WF-9',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF9_MODEL',
    maxBodyBytes: TEXT_BODY_BYTES,
    temperature: 0.7,
    maxTokens: 1500,
    timeoutMs: 25000,
    validateInput(body) {
      const audioError = rejectAudio(body);
      if (audioError) return audioError;
      if (!isNonEmptyString(body.theme) || containsHanzi(body.theme)) {
        return fail('theme must be a non-empty string without Chinese characters');
      }
      if (body.moduleContext !== undefined && !isNonEmptyString(body.moduleContext) && !isPlainObject(body.moduleContext)) {
        return fail('moduleContext must be a string or an object');
      }
      if (body.coveredContent !== undefined && !isStringArray(body.coveredContent)) return fail('coveredContent must be an array of strings');
      if (body.learnerLevel !== undefined && !isNonEmptyString(body.learnerLevel)) return fail('learnerLevel must be a non-empty string');
      if (body.debriefThemes !== undefined && !isStringArray(body.debriefThemes)) return fail('debriefThemes must be an array of strings');
      if (body.exchangeLength !== undefined && !(Number.isInteger(body.exchangeLength) && body.exchangeLength > 0 && body.exchangeLength <= 20)) {
        return fail('exchangeLength must be an integer between 1 and 20');
      }
      return {
        value: {
          theme: body.theme,
          moduleContext: body.moduleContext ?? null,
          coveredContent: body.coveredContent ?? [],
          learnerLevel: body.learnerLevel ?? 'beginner',
          debriefThemes: body.debriefThemes ?? [],
          exchangeLength: body.exchangeLength ?? 6,
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF9_PROMPT },
        { role: 'user', content: JSON.stringify(value) },
      ];
    },
    validateOutput(output) {
      if (!isPlainObject(output) || !Array.isArray(output.script) || output.script.length === 0) return null;
      const script = [];
      for (const turn of output.script) {
        const cleaned = turnFields(turn);
        if (!cleaned) return null;
        script.push(cleaned);
      }
      if (!Array.isArray(output.locals) || output.locals.length !== 5) return null;
      const locals = [];
      for (const local of output.locals) {
        const cleaned = localFields(local);
        if (!cleaned) return null;
        locals.push(cleaned);
      }
      return { script, locals };
    },
  },
  {
    id: 'WF-10',
    name: 'Local conversation turn',
    slug: 'local-turn',
    doc: 'docs/08-ai-workflows.md WF-10',
    kind: 'chat',
    model: MUSE_SPARK,
    envModel: 'WF10_MODEL',
    maxBodyBytes: ONE_CLIP_BODY_BYTES,
    temperature: 0.6,
    maxTokens: 400,
    timeoutMs: 12000,
    validateInput(body) {
      const audioCheck = validateAudio(body, 1);
      if (audioCheck.error) return audioCheck;
      const persona = localFields(body.persona);
      if (!persona) return fail('persona must include label, settingRole, personality, voiceProfile, and pace');
      if (!isPlainObject(body.mission) || !Array.isArray(body.mission.script) || body.mission.script.length === 0) {
        return fail('mission.script must be a non-empty array of turns');
      }
      const script = [];
      for (const turn of body.mission.script) {
        const cleaned = turnFields(turn);
        if (!cleaned) return fail('mission.script turns must include pinyin, meaning, and targetTones with integers 1-5');
        script.push(cleaned);
      }
      if (body.mission.goal !== undefined && !isNonEmptyString(body.mission.goal)) {
        return fail('mission.goal must be a non-empty string');
      }
      if (body.conversationState !== undefined && !isPlainObject(body.conversationState)) {
        return fail('conversationState must be an object');
      }
      if (body.targetDifficulty !== undefined && !isNonEmptyString(body.targetDifficulty)) {
        return fail('targetDifficulty must be a non-empty string');
      }
      if (containsHanziDeep({ persona, script })) {
        return fail('persona and mission script must not contain Chinese characters');
      }
      return {
        value: {
          audio: audioCheck.audio,
          persona,
          mission: { script, goal: body.mission.goal },
          conversationState: body.conversationState,
          targetDifficulty: body.targetDifficulty,
        },
      };
    },
    buildMessages(value) {
      return [
        { role: 'system', content: WF10_PROMPT },
        {
          role: 'user',
          content: [
            {
              type: 'text',
              text: JSON.stringify({
                persona: value.persona,
                mission: { script: value.mission.script, goal: value.mission.goal ?? null },
                conversationState: value.conversationState ?? null,
                targetDifficulty: value.targetDifficulty ?? null,
              }),
            },
            ...value.audio,
          ],
        },
      ];
    },
    validateOutput(output) {
      return stringFields(output, ['replyText'], ['understandingSignal', 'nextLocalPrompt']);
    },
  },
];

const BY_SLUG = new Map(WORKFLOWS.map((workflow) => [workflow.slug, workflow]));

export function workflowBySlug(slug) {
  return BY_SLUG.get(slug) ?? null;
}
