#!/usr/bin/env node
// Dependency-free validator for the bundled content corpus under assets/content/.
//
// It checks every file for Han script, then validates the JSON manifests: required
// fields, known item types, pinyin tone numbers, globally unique ids, reference-audio
// naming (assets/audio/naming-and-formats.md), and meaning uniqueness within a module.
//
// Run from anywhere:  node assets/content/validate.mjs

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(here, '..', '..');
const audioReferenceRoot = path.join(repoRoot, 'assets', 'audio', 'reference');

const MODULES = ['tones', 'fundamentals', 'vocabulary', 'listening', 'speech'];
const TYPES = ['word', 'phrase', 'minimalPair', 'dialogue'];
const SOURCES = ['bundled'];
const LEVELS = ['beginner', 'tourist', 'survival'];
const KINDS = ['lesson', 'practice'];
const SPEAKERS = ['learner', 'partner'];

const HAN = /\p{Script=Han}/u;
const HANGUL = /^[\u1100-\u11FF\u3130-\u318F\uAC00-\uD7A3 ]+$/u;
const ID_RE = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const PINYIN_RE = /^[a-z\u00fc]+[1-5](?: [a-z\u00fc]+[1-5])*$/;
const AUDIO_RE = /^reference\/(tones|fundamentals|vocabulary|listening|speech)\/([a-z0-9]+(?:-[a-z0-9]+)*)\.(ogg|m4a|wav)$/;

const ITEM_KEYS = ['id', 'type', 'source', 'meaning', 'pinyin', 'targetTones', 'audioAssetRef', 'hangul', 'turns'];
const LESSON_KEYS = ['id', 'moduleId', 'title', 'level', 'topic', 'kind', 'contentItemIds'];
const MODULE_KEYS = ['id', 'title', 'theme', 'lessonIds'];

const errors = [];
const fail = (message) => errors.push(message);
const rel = (file) => path.relative(repoRoot, file).split(path.sep).join('/');
const isObject = (value) => value !== null && typeof value === 'object' && !Array.isArray(value);

function walk(dir) {
  const found = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) found.push(...walk(full));
    else found.push(full);
  }
  return found;
}

function parsePinyin(pinyin) {
  if (typeof pinyin !== 'string' || !PINYIN_RE.test(pinyin)) return null;
  return pinyin.split(' ').map((syllable) => Number(syllable[syllable.length - 1]));
}

function checkKeys(object, allowed, where) {
  for (const key of Object.keys(object)) {
    if (!allowed.includes(key)) fail(`${where}: unknown field "${key}"`);
  }
}

function checkRequired(object, keys, where) {
  for (const key of keys) {
    if (!(key in object)) fail(`${where}: missing required field "${key}"`);
  }
}

function checkTones(tones, pinyin, where) {
  const parsed = parsePinyin(pinyin);
  if (parsed === null) {
    fail(`${where}: pinyin "${pinyin}" must carry a tone number 1-5 on every syllable`);
    return;
  }
  if (!Array.isArray(tones) || tones.length !== parsed.length) {
    fail(`${where}: targetTones must list one tone per syllable (${parsed.length})`);
    return;
  }
  for (let i = 0; i < parsed.length; i += 1) {
    if (tones[i] !== parsed[i]) {
      fail(`${where}: targetTones[${i}] is ${tones[i]} but pinyin carries tone ${parsed[i]}`);
    }
  }
  for (const tone of tones) {
    if (!Number.isInteger(tone) || tone < 1 || tone > 5) {
      fail(`${where}: targetTones must be integers 1-5`);
    }
  }
}

// 1. Read every file once: scan for Han script, parse JSON.
const files = walk(here);
const documents = new Map();
for (const file of files) {
  const text = fs.readFileSync(file, 'utf8');
  if (HAN.test(text)) fail(`${rel(file)}: contains Han script characters`);
  if (file.endsWith('.json')) {
    try {
      documents.set(file, JSON.parse(text));
    } catch (error) {
      fail(`${rel(file)}: invalid JSON (${error.message})`);
    }
  }
}

const seenIds = new Map();
function claimId(id, where) {
  if (typeof id !== 'string' || !ID_RE.test(id)) {
    fail(`${where}: id "${id}" must be lowercase kebab-case`);
    return;
  }
  if (seenIds.has(id)) {
    fail(`${where}: duplicate id "${id}" (already used at ${seenIds.get(id)})`);
    return;
  }
  seenIds.set(id, where);
}

function load(file) {
  return documents.get(file);
}

// 2. Validate each module.
const loadedModules = new Map();
for (const moduleId of MODULES) {
  const moduleDir = path.join(here, moduleId);
  const moduleFile = path.join(moduleDir, 'module.json');
  const lessonsFile = path.join(moduleDir, 'lessons.json');
  const itemsFile = path.join(moduleDir, 'items.json');
  const indexFile = path.join(moduleDir, 'index.json');

  if (!fs.existsSync(moduleDir)) {
    fail(`modules: missing directory for "${moduleId}"`);
    continue;
  }
  if (!fs.existsSync(path.join(audioReferenceRoot, moduleId))) {
    fail(`modules: missing assets/audio/reference/${moduleId}/ for "${moduleId}"`);
  }

  const moduleDoc = load(moduleFile);
  const lessonsDoc = load(lessonsFile);
  const itemsDoc = load(itemsFile);
  const indexDoc = load(indexFile);
  if (!moduleDoc || !lessonsDoc || !itemsDoc || !indexDoc) {
    fail(`${moduleId}: could not read module.json, lessons.json, items.json, index.json`);
    continue;
  }

  // Module object.
  checkKeys(moduleDoc, MODULE_KEYS, `${rel(moduleFile)}`);
  checkRequired(moduleDoc, MODULE_KEYS, `${rel(moduleFile)}`);
  if (moduleDoc.id !== moduleId) fail(`${rel(moduleFile)}: id must equal "${moduleId}"`);
  claimId(moduleDoc.id, rel(moduleFile));

  // Content items.
  const items = itemsDoc.items;
  if (!Array.isArray(items)) {
    fail(`${rel(itemsFile)}: "items" must be an array`);
    continue;
  }
  if (itemsDoc.moduleId !== moduleId) fail(`${rel(itemsFile)}: moduleId must equal "${moduleId}"`);
  const itemIndex = new Map();
  const meanings = new Map();
  items.forEach((item, position) => {
    const where = `${rel(itemsFile)} items[${position}]`;
    if (!isObject(item)) {
      fail(`${where}: must be an object`);
      return;
    }
    checkKeys(item, ITEM_KEYS, where);
    checkRequired(item, ['id', 'type', 'source', 'meaning', 'pinyin', 'targetTones', 'audioAssetRef'], where);
    claimId(item.id, where);
    if (itemIndex.has(item.id)) fail(`${where}: duplicate item id "${item.id}"`);
    else itemIndex.set(item.id, item);

    if (!TYPES.includes(item.type)) fail(`${where}: unknown type "${item.type}"`);
    if (!SOURCES.includes(item.source)) fail(`${where}: source must be "bundled" for the shipped corpus`);
    if (typeof item.meaning !== 'string' || item.meaning.trim() === '') {
      fail(`${where}: meaning must be a non-empty English string`);
    } else {
      const key = item.meaning.trim().toLowerCase();
      if (meanings.has(key)) fail(`${where}: duplicate meaning "${item.meaning}" (also at ${meanings.get(key)})`);
      else meanings.set(key, where);
    }

    checkTones(item.targetTones, item.pinyin, where);

    if (typeof item.audioAssetRef !== 'string') {
      fail(`${where}: audioAssetRef must be a string`);
    } else {
      const match = AUDIO_RE.exec(item.audioAssetRef);
      if (!match) {
        fail(`${where}: audioAssetRef "${item.audioAssetRef}" must match reference/<module>/<id>.(ogg|m4a|wav)`);
      } else {
        if (match[1] !== moduleId) {
          fail(`${where}: audioAssetRef module "${match[1]}" must be the owning module "${moduleId}"`);
        }
        if (match[2] !== item.id) {
          fail(`${where}: audioAssetRef stem "${match[2]}" must equal the item id "${item.id}"`);
        }
      }
    }

    if ('hangul' in item && (typeof item.hangul !== 'string' || !HANGUL.test(item.hangul))) {
      fail(`${where}: hangul must contain Hangul syllables only (ADR 0004)`);
    }

    if (item.type === 'dialogue') {
      if (!Array.isArray(item.turns) || item.turns.length === 0) {
        fail(`${where}: a dialogue item requires a non-empty turns array`);
      } else {
        item.turns.forEach((turn, turnIndex) => {
          const turnWhere = `${where}.turns[${turnIndex}]`;
          if (!isObject(turn)) {
            fail(`${turnWhere}: must be an object`);
            return;
          }
          checkKeys(turn, ['speaker', 'pinyin', 'meaning', 'targetTones'], turnWhere);
          checkRequired(turn, ['speaker', 'pinyin', 'meaning', 'targetTones'], turnWhere);
          if (!SPEAKERS.includes(turn.speaker)) fail(`${turnWhere}: unknown speaker "${turn.speaker}"`);
          if (typeof turn.meaning !== 'string' || turn.meaning.trim() === '') {
            fail(`${turnWhere}: meaning must be a non-empty English string`);
          }
          checkTones(turn.targetTones, turn.pinyin, turnWhere);
        });
      }
    } else if ('turns' in item) {
      fail(`${where}: turns is only valid on a dialogue item`);
    }
  });

  // Lessons and practice.
  const lessons = lessonsDoc.lessons;
  if (!Array.isArray(lessons)) {
    fail(`${rel(lessonsFile)}: "lessons" must be an array`);
    continue;
  }
  if (lessonsDoc.moduleId !== moduleId) fail(`${rel(lessonsFile)}: moduleId must equal "${moduleId}"`);
  const lessonIndex = new Map();
  lessons.forEach((lesson, position) => {
    const where = `${rel(lessonsFile)} lessons[${position}]`;
    if (!isObject(lesson)) {
      fail(`${where}: must be an object`);
      return;
    }
    checkKeys(lesson, LESSON_KEYS, where);
    checkRequired(lesson, LESSON_KEYS, where);
    claimId(lesson.id, where);
    if (lessonIndex.has(lesson.id)) fail(`${where}: duplicate lesson id "${lesson.id}"`);
    else lessonIndex.set(lesson.id, lesson);

    if (lesson.moduleId !== moduleId) fail(`${where}: moduleId must equal "${moduleId}"`);
    if (!LEVELS.includes(lesson.level)) fail(`${where}: unknown level "${lesson.level}"`);
    if (!KINDS.includes(lesson.kind)) fail(`${where}: kind must be "lesson" or "practice"`);
    if (typeof lesson.title !== 'string' || lesson.title.trim() === '') fail(`${where}: title must be non-empty`);
    if (typeof lesson.topic !== 'string' || lesson.topic.trim() === '') fail(`${where}: topic must be non-empty`);

    if (!Array.isArray(lesson.contentItemIds) || lesson.contentItemIds.length === 0) {
      fail(`${where}: contentItemIds must be a non-empty array`);
      return;
    }
    for (const itemId of lesson.contentItemIds) {
      if (!itemIndex.has(itemId)) fail(`${where}: contentItemId "${itemId}" does not resolve in module "${moduleId}"`);
    }
  });

  // module.lessonIds mirrors lessons.json order exactly.
  if (!Array.isArray(moduleDoc.lessonIds)) {
    fail(`${rel(moduleFile)}: lessonIds must be an array`);
  } else {
    const declared = moduleDoc.lessonIds;
    const actual = lessons.map((lesson) => lesson.id);
    if (declared.length !== actual.length || declared.some((id, i) => id !== actual[i])) {
      fail(`${rel(moduleFile)}: lessonIds must list the lessons in lessons.json order`);
    }
    for (const id of declared) {
      if (!lessonIndex.has(id)) fail(`${rel(moduleFile)}: lessonIds entry "${id}" has no lesson`);
    }
  }

  // Per-module index counts.
  if (indexDoc.moduleId !== moduleId) fail(`${rel(indexFile)}: moduleId must equal "${moduleId}"`);
  if (indexDoc.lessonCount !== lessons.length) {
    fail(`${rel(indexFile)}: lessonCount ${indexDoc.lessonCount} must equal ${lessons.length}`);
  }
  if (indexDoc.itemCount !== items.length) {
    fail(`${rel(indexFile)}: itemCount ${indexDoc.itemCount} must equal ${items.length}`);
  }

  loadedModules.set(moduleId, {
    lessonCount: lessons.length,
    itemCount: items.length,
    itemIndex,
    lessonIndex,
  });
}

// 3. Top-level index consistency.
const topIndex = load(path.join(here, 'index.json'));
if (!topIndex || !Array.isArray(topIndex.modules)) {
  fail('index.json: "modules" must be an array');
} else {
  const declaredIds = topIndex.modules.map((entry) => entry.id);
  if (declaredIds.length !== MODULES.length || declaredIds.some((id, i) => id !== MODULES[i])) {
    fail(`index.json: modules must list ${MODULES.join(', ')} in order`);
  }
  topIndex.modules.forEach((entry, position) => {
    const where = `index.json modules[${position}]`;
    if (!isObject(entry)) {
      fail(`${where}: must be an object`);
      return;
    }
    const loaded = loadedModules.get(entry.id);
    if (!loaded) {
      fail(`${where}: module "${entry.id}" was not loaded`);
      return;
    }
    if (entry.lessonCount !== loaded.lessonCount) fail(`${where}: lessonCount does not match module data`);
    if (entry.itemCount !== loaded.itemCount) fail(`${where}: itemCount does not match module data`);
    if (typeof entry.path !== 'string' || !fs.existsSync(path.join(here, entry.path))) {
      fail(`${where}: path "${entry.path}" does not exist`);
    }
    if (typeof entry.index !== 'string' || !fs.existsSync(path.join(here, entry.index))) {
      fail(`${where}: index "${entry.index}" does not exist`);
    }
  });
}

// 4. Report.
if (errors.length > 0) {
  console.error(`Content validation failed with ${errors.length} error(s):`);
  for (const error of errors) console.error(`  - ${error}`);
  process.exitCode = 1;
} else {
  const summary = MODULES.map((id) => {
    const module = loadedModules.get(id);
    return `${id} (${module.lessonCount} lessons, ${module.itemCount} items)`;
  }).join(', ');
  console.log(`Content validation passed: ${summary}.`);
}
