const HANZI = /[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]/;
const BASE64 = /^[A-Za-z0-9+/]+={0,2}$/;

export const AUDIO_FORMATS = new Set(['wav', 'mp3', 'm4a', 'ogg', 'webm', 'flac']);

export function isPlainObject(value) {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function isNonEmptyString(value) {
  return typeof value === 'string' && value.trim() !== '';
}

export function isStringArray(value) {
  return Array.isArray(value) && value.every(isNonEmptyString);
}

export function isNumberArray(value) {
  return Array.isArray(value) && value.every((entry) => typeof entry === 'number' && Number.isFinite(entry));
}

export function containsHanzi(value) {
  return typeof value === 'string' && HANZI.test(value);
}

export function containsHanziDeep(value) {
  if (typeof value === 'string') return HANZI.test(value);
  if (Array.isArray(value)) return value.some(containsHanziDeep);
  if (isPlainObject(value)) return Object.values(value).some(containsHanziDeep);
  return false;
}

export function isBase64(value) {
  return typeof value === 'string' && value.length > 0 && value.length % 4 === 0 && BASE64.test(value);
}

export function isAudioPart(value) {
  if (!isPlainObject(value) || value.type !== 'input_audio') return false;
  const part = value.input_audio;
  return isPlainObject(part)
    && isBase64(part.data)
    && isNonEmptyString(part.format)
    && AUDIO_FORMATS.has(part.format);
}

export function audioPartBytes(part) {
  return Math.ceil((part.input_audio.data.length * 3) / 4);
}

export function findAudioPart(value) {
  if (Array.isArray(value)) return value.some(findAudioPart);
  if (!isPlainObject(value)) return false;
  if (value.type === 'input_audio' || 'input_audio' in value) return true;
  return Object.values(value).some(findAudioPart);
}
