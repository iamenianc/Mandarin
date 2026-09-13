#!/usr/bin/env python3
"""Generate bundled reference audio from pinyin with Kokoro-82M (WF-3, ADR 0006).

Reads a content manifest (``assets/content/**`` when present, or one or more
``--manifest`` paths), plans one deterministic reference clip per ContentItem,
validates the pinyin and tone numbers, and synthesizes 24 kHz mono WAV clips with a
pinned Kokoro Mandarin voice.

Modes:

- ``--dry-run``  validate inputs, compute every output path, and print the plan. No
  model and no network; Kokoro is imported lazily, so this runs offline.
- ``--selftest`` validate naming, the committed manifests against
  ``schema/track.schema.json``, the pinned voice registry, and 24 kHz mono
  expectations. No model and no network.
- ``--generate`` synthesize the planned clips into the generated output directory
  (``assets/audio/generated/`` by default), which is git-ignored.

Generated audio is never committed in bulk. Only scripts, manifests, docs, and
fixtures are committed; a generated clip is published to ``reference/<module>/`` by a
deliberate, reviewed step once it is accepted.
"""

from __future__ import annotations

import argparse
import array
import hashlib
import json
import re
import sys
import wave
from dataclasses import dataclass, field
from pathlib import Path

try:  # PyYAML is used to read and write the YAML manifests.
    import yaml
except ImportError as exc:  # pragma: no cover - environment guard
    raise SystemExit(
        "PyYAML is required to read the manifests: pip install pyyaml"
    ) from exc

AUDIO_ROOT = Path(__file__).resolve().parent.parent
SCHEMA_PATH = AUDIO_ROOT / "schema" / "track.schema.json"
TOP_MANIFEST_PATH = AUDIO_ROOT / "manifest.yaml"
REGISTRY_PATH = AUDIO_ROOT / "voices" / "registry.yaml"
DEFAULT_CONTENT_ROOT = AUDIO_ROOT.parent / "content"
DEFAULT_OUT_DIR = AUDIO_ROOT / "generated"
FIXTURE_DIR = AUDIO_ROOT / "pipeline" / "fixtures"

SAMPLE_RATE = 24_000
CHANNELS = 1
SAMPLE_WIDTH_BYTES = 2

ENGINE = "kokoro-82m"
LANG_CODE = "z"
DEFAULT_VOICE = "zf_xiaobei"
DEFAULT_CHECKPOINT = "hexgrad/Kokoro-82M@v1.0"

# Canonical order for the index and reference modules (docs/02-architecture.md).
MODULES = (
    "tones",
    "vocabulary",
    "listening",
    "speech",
    "fundamentals",
    "conversation",
    "numbers",
)
REFERENCE_MODULES = ("tones", "vocabulary", "listening", "speech", "fundamentals")
CONTENT_TYPES = ("phrase", "word", "minimalPair", "dialogue")

ID_RE = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
FILE_RE = re.compile(r"^[a-z0-9/_-]+\.(ogg|m4a|wav)$")
SHA256_RE = re.compile(r"^[a-f0-9]{64}$")
TONE_TOKEN_RE = re.compile(r"^[a-z]+[1-5]$")
NON_ASCII_RE = re.compile(r"[^\x00-\x7f]")
HANZI_RE = re.compile("[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
ASCII_ID_CHARS_RE = re.compile(r"^[a-z0-9-]+$")


class ContentError(ValueError):
    """A content item is missing or violates the pinyin/id conventions."""


class PipelineUnavailable(RuntimeError):
    """Kokoro (or one of its runtime dependencies) is not installed."""


@dataclass(frozen=True)
class ContentItem:
    """A curated item to voice, derived from a ContentItem-style record."""

    id: str
    module: str
    content_type: str
    pinyin: str
    meaning: str | None = None
    hangul: str | None = None
    target_tones: tuple[int, ...] = ()
    source: str = "bundled"
    origin_path: str = ""


@dataclass(frozen=True)
class PlannedTrack:
    """A deterministic clip plan: the bundled target and the generated source."""

    item: ContentItem
    bundled_path: str
    generated_path: Path
    voice: str
    checkpoint: str
    source_hash: str


@dataclass
class Report:
    """Accumulates selftest check results."""

    checks: list[tuple[str, bool, str]] = field(default_factory=list)

    def add(self, name: str, ok: bool, detail: str = "") -> None:
        self.checks.append((name, ok, detail))

    @property
    def failed(self) -> int:
        return sum(1 for _, ok, _ in self.checks if not ok)

    def print(self) -> None:
        for name, ok, detail in self.checks:
            status = "PASS" if ok else "FAIL"
            suffix = f" - {detail}" if detail else ""
            print(f"[{status}] {name}{suffix}")


# --------------------------------------------------------------------------- #
# Small helpers
# --------------------------------------------------------------------------- #


def load_yaml(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        return yaml.safe_load(handle)


def dump_yaml(path: Path, data, header: str | None = None) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    body = yaml.safe_dump(data, sort_keys=False, allow_unicode=True, width=100)
    text = f"{header}\n{body}" if header else body
    path.write_text(text, encoding="utf-8", newline="\n")


def slugify(value: str) -> str:
    """Lowercase a name and hyphenate word boundaries into a valid track id."""
    cleaned = re.sub(r"[^a-z0-9]+", "-", value.lower())
    return cleaned.strip("-")


def sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def ascii_safe(text: str) -> str:
    """Render text with ASCII escapes so console encoding never breaks a run."""
    return text.encode("ascii", "backslashreplace").decode("ascii")


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def source_hash(pinyin: str, voice: str, checkpoint: str) -> str:
    payload = "|".join([ENGINE, LANG_CODE, voice, checkpoint, pinyin])
    return sha256_bytes(payload.encode("utf-8"))


def validate_pinyin(pinyin: str) -> list[str]:
    """Return the list of problems with a pinyin string; empty means valid."""
    errors: list[str] = []
    if not pinyin or not pinyin.strip():
        return ["pinyin is empty"]
    if HANZI_RE.search(pinyin):
        errors.append("pinyin contains hanzi")
    if NON_ASCII_RE.search(pinyin):
        errors.append("pinyin must be ASCII (no diacritics)")
    for token in pinyin.split():
        if not TONE_TOKEN_RE.match(token):
            errors.append(
                f"pinyin token {token!r} must be lowercase letters followed by "
                "tone number 1-5"
            )
    return errors


def parse_pinyin(pinyin: str) -> tuple[list[str], tuple[int, ...]]:
    errors = validate_pinyin(pinyin)
    if errors:
        raise ContentError("; ".join(errors))
    tokens = pinyin.split()
    tones = tuple(int(token[-1]) for token in tokens)
    return tokens, tones


def validate_audio_header(path: Path) -> list[str]:
    """Check that a WAV file is 24 kHz mono 16-bit PCM."""
    errors: list[str] = []
    try:
        with wave.open(str(path), "rb") as handle:
            if handle.getframerate() != SAMPLE_RATE:
                errors.append(f"sample rate is {handle.getframerate()}, expected {SAMPLE_RATE}")
            if handle.getnchannels() != CHANNELS:
                errors.append(f"channels is {handle.getnchannels()}, expected {CHANNELS}")
            if handle.getsampwidth() != SAMPLE_WIDTH_BYTES:
                errors.append(
                    f"sample width is {handle.getsampwidth()} bytes, "
                    f"expected {SAMPLE_WIDTH_BYTES}"
                )
    except (wave.Error, OSError) as exc:
        errors.append(f"not a readable WAV file: {exc}")
    return errors


# --------------------------------------------------------------------------- #
# JSON Schema subset validator (constraints come from track.schema.json)
# --------------------------------------------------------------------------- #


def _matches_type(value, expected: str) -> bool:
    if expected == "object":
        return isinstance(value, dict)
    if expected == "array":
        return isinstance(value, list)
    if expected == "string":
        return isinstance(value, str)
    if expected == "integer":
        return isinstance(value, int) and not isinstance(value, bool)
    if expected == "number":
        return isinstance(value, (int, float)) and not isinstance(value, bool)
    if expected == "boolean":
        return isinstance(value, bool)
    return True


def validate_instance(instance, schema: dict, path: str = "$") -> list[str]:
    """Validate against the JSON Schema keywords used by track.schema.json."""
    errors: list[str] = []

    if "type" in schema:
        expected = schema["type"]
        types = expected if isinstance(expected, list) else [expected]
        if not any(_matches_type(instance, name) for name in types):
            return [f"{path}: expected type {types}, got {type(instance).__name__}"]

    if "const" in schema and instance != schema["const"]:
        errors.append(f"{path}: expected {schema['const']!r}, got {instance!r}")

    if "enum" in schema and instance not in schema["enum"]:
        errors.append(f"{path}: {instance!r} is not one of {schema['enum']}")

    if isinstance(instance, str):
        if "pattern" in schema and not re.search(schema["pattern"], instance):
            errors.append(f"{path}: {instance!r} does not match {schema['pattern']!r}")

    if isinstance(instance, (int, float)) and not isinstance(instance, bool):
        if "minimum" in schema and instance < schema["minimum"]:
            errors.append(f"{path}: {instance} is below minimum {schema['minimum']}")
        if "maximum" in schema and instance > schema["maximum"]:
            errors.append(f"{path}: {instance} is above maximum {schema['maximum']}")

    if isinstance(instance, list) and "items" in schema:
        for index, element in enumerate(instance):
            errors.extend(validate_instance(element, schema["items"], f"{path}[{index}]"))

    if isinstance(instance, dict):
        for required in schema.get("required", []):
            if required not in instance:
                errors.append(f"{path}: missing required property {required!r}")
        properties = schema.get("properties", {})
        additional = schema.get("additionalProperties", True)
        for key, value in instance.items():
            if key in properties:
                errors.extend(validate_instance(value, properties[key], f"{path}.{key}"))
            elif additional is False:
                errors.append(f"{path}: additional property {key!r} is not allowed")

    return errors


# --------------------------------------------------------------------------- #
# Content manifest loading
# --------------------------------------------------------------------------- #


def discover_manifest_files(paths: list[Path], content_root: Path) -> list[Path]:
    files: list[Path] = []
    for path in paths:
        if path.is_dir():
            files.extend(sorted(p for p in path.rglob("*.json") if p.is_file()))
        elif path.is_file():
            files.append(path)
    if not paths and content_root.is_dir():
        files.extend(sorted(p for p in content_root.rglob("*.json") if p.is_file()))
    # De-duplicate while keeping the deterministic discovery order.
    seen: set[str] = set()
    unique: list[Path] = []
    for path in files:
        key = str(path.resolve())
        if key not in seen:
            seen.add(key)
            unique.append(path)
    return unique


def infer_module_from_path(path: Path, content_root: Path) -> str | None:
    try:
        relative = path.resolve().relative_to(content_root.resolve())
    except ValueError:
        return None
    for part in relative.parts[:-1]:
        if part in MODULES:
            return part
    return None


def _iter_raw_items(doc, content_root: Path, path: Path):
    """Yield (raw_item, module_hint) from the accepted manifest shapes."""
    path_hint = infer_module_from_path(path, content_root)
    if isinstance(doc, list):
        for item in doc:
            yield item, path_hint
        return
    if not isinstance(doc, dict):
        raise ContentError(f"{path.as_posix()}: unsupported manifest root")

    container_module = doc.get("module") or doc.get("moduleId") or path_hint
    if isinstance(doc.get("items"), list):
        for item in doc["items"]:
            yield item, container_module
        return
    if isinstance(doc.get("tracks"), list):
        # A track manifest is not a content manifest; ignore it.
        return
    if "pinyin" in doc or "turns" in doc:
        yield doc, container_module
        return
    # Lesson/module/dialogue wrappers without direct pinyin are not voiceable here.
    if "contentItemIds" in doc or "lessonIds" in doc:
        return
    if "id" in doc and "type" in doc:
        yield doc, container_module


def _module_from_id(module_id: str | None) -> str | None:
    if not module_id:
        return None
    for module in MODULES:
        if module in module_id:
            return module
    return None


def coerce_item(raw, module_hint: str | None, cli_module: str | None, path: Path) -> ContentItem:
    if not isinstance(raw, dict):
        raise ContentError(f"{path.as_posix()}: content item is not an object")
    item_id = raw.get("id")
    if not item_id:
        raise ContentError(f"{path.as_posix()}: content item is missing 'id'")
    item_id = str(item_id)

    module = (
        raw.get("module")
        or _module_from_id(raw.get("moduleId"))
        or module_hint
        or cli_module
    )
    if module not in MODULES:
        raise ContentError(
            f"{path.as_posix()}: item {item_id!r} has no valid module "
            f"(got {module!r}); set 'module' or pass --module"
        )

    content_type = raw.get("type") or "phrase"
    if content_type not in CONTENT_TYPES:
        raise ContentError(
            f"{path.as_posix()}: item {item_id!r} has unknown type {content_type!r}"
        )

    if "turns" in raw:
        turns = raw.get("turns") or []
        pinyin = " ".join(str(turn.get("pinyin", "")).strip() for turn in turns).strip()
        declared_tones = None
    else:
        pinyin = str(raw.get("pinyin", "")).strip()
        declared_tones = raw.get("targetTones")

    declared_tones = raw.get("targetTones", declared_tones)
    _tokens, tones = parse_pinyin(pinyin)
    if declared_tones is not None:
        declared = tuple(int(value) for value in declared_tones)
        if declared != tones:
            raise ContentError(
                f"{path.as_posix()}: item {item_id!r} targetTones {declared} do not "
                f"match pinyin {pinyin!r} -> {tones}"
            )

    if not ID_RE.match(item_id):
        item_id = slugify(item_id)
    if not ID_RE.match(item_id):
        raise ContentError(
            f"{path.as_posix()}: item id {raw.get('id')!r} is not a lowercase "
            "hyphenated slug"
        )

    return ContentItem(
        id=item_id,
        module=module,
        content_type=content_type,
        pinyin=pinyin,
        meaning=raw.get("meaning"),
        hangul=raw.get("hangul"),
        target_tones=tones,
        source=str(raw.get("source", "bundled")),
        origin_path=path.as_posix(),
    )


def load_content_items(
    manifest_paths: list[Path],
    content_root: Path,
    cli_module: str | None,
) -> list[ContentItem]:
    items: list[ContentItem] = []
    for path in discover_manifest_files(manifest_paths, content_root):
        try:
            doc = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as exc:
            raise ContentError(f"{path.as_posix()}: cannot read JSON: {exc}") from exc
        for raw, module_hint in _iter_raw_items(doc, content_root, path):
            items.append(coerce_item(raw, module_hint, cli_module, path))
    items.sort(key=lambda item: (item.module, item.id))
    return items


# --------------------------------------------------------------------------- #
# Voice registry
# --------------------------------------------------------------------------- #


def resolve_voice(registry: dict, cli_voice: str | None) -> str:
    voices = registry.get("voices") or []
    if cli_voice:
        return cli_voice
    if registry.get("defaultVoice"):
        return registry["defaultVoice"]
    if voices:
        return voices[0]
    return DEFAULT_VOICE


def resolve_checkpoint(registry: dict, cli_checkpoint: str | None) -> str:
    if cli_checkpoint:
        return cli_checkpoint
    return registry.get("checkpoint") or DEFAULT_CHECKPOINT


def checkpoint_repo(checkpoint: str) -> str:
    return checkpoint.split("@", 1)[0]


def checkpoint_revision(checkpoint: str) -> str | None:
    parts = checkpoint.split("@", 1)
    return parts[1] if len(parts) == 2 and parts[1] else None


def voice_registry_problems(registry: dict) -> list[str]:
    problems: list[str] = []
    voices = registry.get("voices") or []
    if not isinstance(voices, list) or not voices:
        problems.append("registry.voices is empty; pin at least one voice id")
    default_voice = registry.get("defaultVoice")
    if default_voice and default_voice not in voices:
        problems.append(f"defaultVoice {default_voice!r} is not listed in voices")
    return problems


# --------------------------------------------------------------------------- #
# Planning and generation
# --------------------------------------------------------------------------- #


def plan_tracks(
    items: list[ContentItem],
    voice: str,
    checkpoint: str,
    out_dir: Path,
) -> list[PlannedTrack]:
    plans: list[PlannedTrack] = []
    for item in items:
        generated_path = out_dir / item.module / f"{item.id}.wav"
        plans.append(
            PlannedTrack(
                item=item,
                bundled_path=f"reference/{item.module}/{item.id}.ogg",
                generated_path=generated_path,
                voice=voice,
                checkpoint=checkpoint,
                source_hash=source_hash(item.pinyin, voice, checkpoint),
            )
        )
    return plans


def _load_kokoro_pipeline(voice: str, checkpoint: str):
    """Import Kokoro lazily so dry-run and selftest never need the model."""
    try:
        from kokoro import KPipeline
    except ImportError as exc:
        raise PipelineUnavailable(
            "Kokoro is not installed. Install the build-time dependencies with "
            "`pip install kokoro misaki[zh]` (see pipeline/README.md)."
        ) from exc

    repo = checkpoint_repo(checkpoint)
    revision = checkpoint_revision(checkpoint)
    attempts = [
        {"lang_code": LANG_CODE, "repo_id": repo, "revision": revision},
        {"lang_code": LANG_CODE, "repo_id": repo},
    ]
    last_error: Exception | None = None
    for kwargs in attempts:
        try:
            return KPipeline(**kwargs)
        except TypeError as exc:  # older kokoro builds lack `revision`
            last_error = exc
            continue
    raise PipelineUnavailable(f"could not construct Kokoro pipeline: {last_error}")


def _flatten_audio(audio) -> list[float]:
    if hasattr(audio, "detach"):
        audio = audio.detach()
    if hasattr(audio, "cpu"):
        audio = audio.cpu()
    if hasattr(audio, "tolist"):
        audio = audio.tolist()
    flat: list[float] = []
    _collect_floats(audio, flat)
    return flat


def _collect_floats(value, out: list[float]) -> None:
    if isinstance(value, (list, tuple)):
        for element in value:
            _collect_floats(element, out)
    elif isinstance(value, (int, float)):
        out.append(float(value))


def synthesize(pipeline, pinyin: str, voice: str) -> list[float]:
    generator = pipeline(pinyin, voice=voice, speed=1.0)
    samples: list[float] = []
    for chunk in generator:
        # Kokoro 0.9.x yields a Result with `.audio`; older builds yield a tuple.
        if getattr(chunk, "audio", None) is not None:
            audio = chunk.audio
        elif isinstance(chunk, (tuple, list)):
            audio = chunk[-1]
        else:
            audio = chunk
        samples.extend(_flatten_audio(audio))
    if not samples:
        raise PipelineUnavailable(f"Kokoro returned no audio for {pinyin!r}")
    return samples


def write_wav(path: Path, samples: list[float]) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    pcm = array.array("h", (max(-32768, min(32767, int(round(value * 32767)))) for value in samples))
    if sys.byteorder == "big":
        pcm.byteswap()
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(CHANNELS)
        handle.setsampwidth(SAMPLE_WIDTH_BYTES)
        handle.setframerate(SAMPLE_RATE)
        handle.writeframes(pcm.tobytes())
    return round(1000 * len(samples) / SAMPLE_RATE)


def build_track_entry(plan: PlannedTrack, duration_ms: int, sha256: str | None) -> dict:
    item = plan.item
    entry: dict = {
        "id": item.id,
        "category": "reference",
        "module": item.module,
        "contentType": item.content_type,
        "source": item.source,
        "file": plan.generated_path.name,
        "format": "wav-pcm",
        "sampleRate": SAMPLE_RATE,
        "channels": CHANNELS,
        "durationMs": duration_ms,
        "pinyin": item.pinyin,
    }
    if item.hangul:
        entry["hangul"] = item.hangul
    if item.meaning:
        entry["meaning"] = item.meaning
    entry["targetTones"] = list(item.target_tones)
    entry["voice"] = plan.voice
    entry["tts"] = {
        "engine": ENGINE,
        "langCode": LANG_CODE,
        "voice": plan.voice,
        "checkpoint": plan.checkpoint,
    }
    entry["sourceHash"] = plan.source_hash
    if sha256:
        entry["sha256"] = sha256
    entry["provenance"] = {
        "origin": "generated",
        "license": "Apache-2.0",
        "note": (
            "Kokoro-82M reference audio generated from pinyin; working output under "
            "generated/ until published to reference/."
        ),
    }
    return entry


def relative_to_audio_root(path: Path) -> str:
    try:
        return path.resolve().relative_to(AUDIO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


GENERATED_HEADER = (
    "# Generated reference-clip manifest. Build output; not committed.\n"
    "# Written by pipeline/generate_reference_audio.py. "
    "A single entry is described by ../schema/track.schema.json."
)


def write_generated_manifest(entries: list[dict], out_dir: Path) -> Path:
    target = out_dir / "tracks.yaml"
    document = {
        "version": 1,
        "category": "reference",
        "generated": True,
        "tracks": sorted(entries, key=lambda entry: entry["id"]),
    }
    # `file` is rewritten relative to assets/audio for portability.
    for entry in document["tracks"]:
        entry["file"] = relative_to_audio_root(out_dir / entry["module"] / f"{entry['id']}.wav")
    dump_yaml(target, document, GENERATED_HEADER)
    return target


def ensure_top_index() -> bool:
    """Keep manifest.yaml valid and deterministically ordered; rewrite only if needed."""
    document = load_yaml(TOP_MANIFEST_PATH)
    manifests = document.get("manifests") or []
    canonical_order = list(REFERENCE_MODULES) + ["drill", "sample"]
    by_module = {entry.get("module", entry.get("category")): entry for entry in manifests}

    ordered = []
    for key in canonical_order:
        if key in by_module:
            ordered.append(by_module[key])
    for entry in manifests:
        if entry not in ordered:
            ordered.append(entry)
    if ordered == manifests:
        return False
    document["manifests"] = ordered
    header = (
        "# Index of every track manifest under assets/audio/.\n"
        "# A single entry is described by schema/track.schema.json."
    )
    dump_yaml(TOP_MANIFEST_PATH, document, header)
    return True


# --------------------------------------------------------------------------- #
# Modes
# --------------------------------------------------------------------------- #


def print_plan(plans: list[PlannedTrack], voice: str, checkpoint: str) -> None:
    print(f"engine: {ENGINE}  langCode: {LANG_CODE}  voice: {voice}")
    print(f"checkpoint: {checkpoint}")
    print(f"planned clips: {len(plans)}")
    if not plans:
        print(
            "No content manifest found. Supply --manifest PATH, or author "
            "assets/content/**. The fixture at pipeline/fixtures/ demonstrates the "
            "format."
        )
        return
    for plan in plans:
        item = plan.item
        tones = ",".join(str(tone) for tone in item.target_tones)
        print(
            f"- {item.module}/{item.id}: pinyin={item.pinyin!r} tones=[{tones}] "
            f"-> {plan.bundled_path} "
            f"(source: {relative_to_audio_root(plan.generated_path)}, "
            f"sourceHash={plan.source_hash[:12]}...)"
        )


def run_dry_run(args) -> int:
    registry = load_yaml(REGISTRY_PATH)
    voice = resolve_voice(registry, args.voice)
    checkpoint = resolve_checkpoint(registry, args.checkpoint)
    out_dir = Path(args.out).resolve()

    items = load_content_items(
        [Path(path) for path in args.manifest],
        Path(args.content_root).resolve(),
        args.module,
    )
    plans = plan_tracks(items, voice, checkpoint, out_dir)

    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    problems: list[str] = []
    for plan in plans:
        entry = build_track_entry(plan, duration_ms=0, sha256=None)
        entry["file"] = plan.bundled_path
        entry["durationMs"] = None
        candidate = {key: value for key, value in entry.items() if value is not None}
        candidate["format"] = "ogg-opus"
        for problem in validate_instance(candidate, schema):
            problems.append(f"{plan.item.id}: {problem}")

    print_plan(plans, voice, checkpoint)
    registry_problems = voice_registry_problems(registry)
    if voice not in (registry.get("voices") or []):
        registry_problems.append(f"selected voice {voice!r} is not in registry.yaml")
    if registry_problems:
        print("registry warnings:")
        for problem in registry_problems:
            print(f"  - {problem}")
    if problems:
        print("schema problems:")
        for problem in problems:
            print(f"  - {problem}")
        return 2
    print("dry-run OK")
    return 0


def run_generate(args) -> int:
    registry = load_yaml(REGISTRY_PATH)
    voice = resolve_voice(registry, args.voice)
    checkpoint = resolve_checkpoint(registry, args.checkpoint)
    out_dir = Path(args.out).resolve()

    items = load_content_items(
        [Path(path) for path in args.manifest],
        Path(args.content_root).resolve(),
        args.module,
    )
    plans = plan_tracks(items, voice, checkpoint, out_dir)
    if not plans:
        print(
            "No content manifest found; nothing to generate. Supply --manifest PATH "
            "or author assets/content/**."
        )
        return 0
    if args.limit:
        plans = plans[: args.limit]

    # Fail fast before touching the model or the filesystem.
    pipeline = _load_kokoro_pipeline(voice, checkpoint)

    entries: list[dict] = []
    for index, plan in enumerate(plans, start=1):
        print(f"[{index}/{len(plans)}] {plan.item.id} <- {plan.item.pinyin!r}")
        samples = synthesize(pipeline, plan.item.pinyin, plan.voice)
        duration_ms = write_wav(plan.generated_path, samples)
        entries.append(
            build_track_entry(plan, duration_ms, sha256_file(plan.generated_path))
        )
    manifest_path = write_generated_manifest(entries, out_dir)
    changed = ensure_top_index()
    print(f"wrote {len(entries)} clips under {relative_to_audio_root(out_dir)}/")
    print(f"wrote {relative_to_audio_root(manifest_path)}")
    if changed:
        print("reordered assets/audio/manifest.yaml into canonical order")
    return 0


# --------------------------------------------------------------------------- #
# Selftest
# --------------------------------------------------------------------------- #


def check_naming(report: Report) -> None:
    valid = {
        "id": "phrase-ni3-hao3",
        "module": "vocabulary",
        "pinyin": "ni3 hao3",
        "tones": (3, 3),
    }
    item = ContentItem(
        id=valid["id"],
        module=valid["module"],
        content_type="phrase",
        pinyin=valid["pinyin"],
        target_tones=valid["tones"],
    )
    plan = plan_tracks([item], DEFAULT_VOICE, DEFAULT_CHECKPOINT, DEFAULT_OUT_DIR)[0]
    ok = (
        ID_RE.match(item.id) is not None
        and ASCII_ID_CHARS_RE.match(item.id) is not None
        and plan.bundled_path == "reference/vocabulary/phrase-ni3-hao3.ogg"
        and FILE_RE.match(plan.bundled_path) is not None
        and plan.generated_path.name == "phrase-ni3-hao3.wav"
    )
    report.add(
        "naming: deterministic reference path and file stem",
        ok,
        f"{plan.bundled_path} / {plan.generated_path.name}",
    )

    cases = {
        "ni3 hao3": True,
        "ma5": True,
        "n\u01d0 ha\u01ceo": False,  # tone marks, not numbers
        "\u4f60\u597d": False,  # hanzi
        "ni3hao3": False,  # no syllable boundary
        "ni hao": False,  # missing tone number
        "ni6": False,  # out-of-range tone
    }
    for text, expected in cases.items():
        problems = validate_pinyin(text)
        passed = (not problems) == expected
        label = "valid" if expected else "rejected"
        report.add(
            f"pinyin: {label} {ascii_safe(text)!r}",
            passed,
            "" if passed else f"problems={problems}",
        )


def check_schema_definition(report: Report) -> None:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    ok = (
        schema.get("properties", {}).get("sampleRate", {}).get("const") == SAMPLE_RATE
        and schema.get("properties", {}).get("channels", {}).get("const") == CHANNELS
    )
    report.add(
        "schema: track.schema.json pins 24 kHz mono",
        ok,
        "sampleRate=24000 channels=1",
    )


def check_committed_manifests(report: Report) -> None:
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    index = load_yaml(TOP_MANIFEST_PATH)
    manifests = index.get("manifests") or []

    listed_paths = [entry.get("path") for entry in manifests]
    report.add(
        "manifest.yaml: every indexed manifest exists",
        all((AUDIO_ROOT / path).is_file() for path in listed_paths if path),
        ", ".join(str(path) for path in listed_paths),
    )

    seen_ids: set[str] = set()
    all_valid = True
    details: list[str] = []
    for entry in manifests:
        manifest_path = AUDIO_ROOT / entry["path"]
        if not manifest_path.is_file():
            continue
        manifest = load_yaml(manifest_path)
        if manifest.get("category") != entry.get("category"):
            all_valid = False
            details.append(f"{entry['path']}: category mismatch")
        if entry.get("module") and manifest.get("module") != entry.get("module"):
            all_valid = False
            details.append(f"{entry['path']}: module mismatch")
        tracks = manifest.get("tracks")
        if tracks is None:
            all_valid = False
            details.append(f"{entry['path']}: missing tracks list")
            continue
        ids = [track.get("id") for track in tracks]
        if ids != sorted(ids):
            all_valid = False
            details.append(f"{entry['path']}: ids are not deterministically sorted")
        for track in tracks:
            track_id = track.get("id")
            if track_id in seen_ids:
                all_valid = False
                details.append(f"duplicate id {track_id!r}")
            seen_ids.add(track_id)
            for problem in validate_instance(track, schema):
                all_valid = False
                details.append(f"{track_id}: {problem}")
            for problem in validate_pinyin(track.get("pinyin", "")):
                all_valid = False
                details.append(f"{track_id}: {problem}")
            file_path = AUDIO_ROOT / track.get("file", "")
            if track.get("placeholder"):
                if not file_path.is_file():
                    all_valid = False
                    details.append(f"{track_id}: placeholder file is missing")
                if (track.get("provenance") or {}).get("origin") != "placeholder":
                    all_valid = False
                    details.append(f"{track_id}: placeholder lacks provenance.origin")
            if file_path.suffix == ".wav" and file_path.is_file():
                header_problems = validate_audio_header(file_path)
                if header_problems:
                    all_valid = False
                    details.append(f"{track_id}: {'; '.join(header_problems)}")
            recorded_sha = track.get("sha256")
            if recorded_sha and file_path.is_file():
                if sha256_file(file_path) != recorded_sha:
                    all_valid = False
                    details.append(f"{track_id}: sha256 does not match the file")
    report.add(
        "manifests: committed tracks satisfy track.schema.json, naming, 24 kHz mono",
        all_valid,
        "; ".join(details[:6]),
    )


def check_generated_manifest(report: Report, out_dir: Path) -> None:
    path = out_dir / "tracks.yaml"
    if not path.is_file():
        report.add(
            "generated manifest: optional build output",
            True,
            f"{relative_to_audio_root(path)} not present (expected before a generation run)",
        )
        return
    schema = json.loads(SCHEMA_PATH.read_text(encoding="utf-8"))
    manifest = load_yaml(path)
    problems: list[str] = []
    for track in manifest.get("tracks") or []:
        for problem in validate_instance(track, schema):
            problems.append(f"{track.get('id')}: {problem}")
    report.add(
        "generated manifest: tracks satisfy track.schema.json",
        not problems,
        "; ".join(problems[:6]),
    )


def check_registry(report: Report) -> None:
    registry = load_yaml(REGISTRY_PATH)
    problems = voice_registry_problems(registry)
    report.add(
        "registry: pinned voice configuration",
        not problems,
        "; ".join(problems),
    )

    used: set[str] = set()
    index = load_yaml(TOP_MANIFEST_PATH)
    for entry in index.get("manifests") or []:
        manifest_path = AUDIO_ROOT / entry["path"]
        if manifest_path.is_file():
            for track in load_yaml(manifest_path).get("tracks") or []:
                voice = (track.get("tts") or {}).get("voice")
                if voice:
                    used.add(voice)
    unregistered = sorted(voice for voice in used if voice not in (registry.get("voices") or []))
    report.add(
        "registry: every track voice is pinned",
        not unregistered,
        ", ".join(unregistered),
    )


def check_fixture(report: Report) -> None:
    if not FIXTURE_DIR.is_dir():
        report.add("fixtures: sample plan", True, "no fixtures directory")
        return
    try:
        items = load_content_items([FIXTURE_DIR], DEFAULT_CONTENT_ROOT, None)
        plans = plan_tracks(items, DEFAULT_VOICE, DEFAULT_CHECKPOINT, DEFAULT_OUT_DIR)
    except ContentError as exc:
        report.add("fixtures: sample plan", False, str(exc))
        return
    ids = [plan.item.id for plan in plans]
    ordering = [(plan.item.module, plan.item.id) for plan in plans]
    ok = (
        bool(items)
        and len(ids) == len(set(ids))
        and ordering == sorted(set(ordering))
    )
    report.add(
        "fixtures: content plans validate, unique, and deterministically ordered",
        ok,
        f"{len(plans)} items",
    )


def run_selftest(args) -> int:
    report = Report()
    check_naming(report)
    check_schema_definition(report)
    check_committed_manifests(report)
    check_registry(report)
    check_fixture(report)
    check_generated_manifest(report, Path(args.out).resolve())

    report.print()
    if report.failed:
        print(f"selftest FAILED: {report.failed} check(s)")
        return 2
    print(f"selftest OK: {len(report.checks)} checks")
    return 0


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Generate 24 kHz mono reference audio from pinyin with Kokoro-82M "
            "(ADR 0006)."
        )
    )
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument(
        "--dry-run",
        action="store_true",
        help="validate and print the plan; no model, no network (default)",
    )
    modes.add_argument(
        "--selftest",
        action="store_true",
        help="validate naming, manifests, schema, and 24 kHz mono expectations",
    )
    modes.add_argument(
        "--generate",
        action="store_true",
        help="synthesize clips into the generated output directory",
    )
    parser.add_argument(
        "--manifest",
        action="append",
        default=[],
        metavar="PATH",
        help="content manifest file or directory (repeatable); defaults to assets/content",
    )
    parser.add_argument(
        "--content-root",
        default=str(DEFAULT_CONTENT_ROOT),
        help="directory scanned when no --manifest is given",
    )
    parser.add_argument(
        "--module",
        choices=MODULES,
        default=None,
        help="default module for content items that do not declare one",
    )
    parser.add_argument("--voice", default=None, help="override the pinned voice id")
    parser.add_argument(
        "--checkpoint", default=None, help="override the pinned Kokoro checkpoint"
    )
    parser.add_argument(
        "--out",
        default=str(DEFAULT_OUT_DIR),
        help="generated output directory (git-ignored)",
    )
    parser.add_argument(
        "--limit", type=int, default=0, help="generate at most N clips (smoke tests)"
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding="utf-8", errors="replace")
        except (AttributeError, ValueError):
            pass
    args = build_parser().parse_args(argv)
    try:
        if args.selftest:
            return run_selftest(args)
        if args.generate:
            return run_generate(args)
        return run_dry_run(args)
    except ContentError as exc:
        print(f"content error: {exc}", file=sys.stderr)
        return 2
    except PipelineUnavailable as exc:
        print(f"pipeline unavailable: {exc}", file=sys.stderr)
        return 3


if __name__ == "__main__":
    raise SystemExit(main())
