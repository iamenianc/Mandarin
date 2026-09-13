# 0015 - Drop hands-free and eyes-free session modes

- Status: accepted
- Date: 2026-09-13
- Amends: 0011-pinyin-pronunciation-layer.md

## Context

Earlier planning assumed two Should-priority interface modes: an eyes-free / audio-only
way to complete a full session (FR-13, a vision success criterion, and an M5 roadmap
item) and hands-free, turn-based conversation with the AI coach (FR-14). Neither is
needed: sessions are guided on screen, audio-first describes what is learned rather than
an interface without visuals, and conversation practice stays tap-to-talk. Keeping the
modes would force extra turn-taking, barge-in, and cue design into every drill for a mode
that will not be used.

## Decision

Drop both modes from scope. No hands-free or eyes-free design work is done; the
requirements rows are marked Won't (v1) with this ADR as the reason; the vision success
criterion and both roadmap items are removed; the design doc no longer presents an
eyes-free option. This is a removal, not a deferral: no later version is promised these
modes.

## Consequences

- Session and turn-taking design stays deliberately simple: one primary control, VAD for
  end of speech, and spoken feedback; no wake word, barge-in, or auto-advance UX.
- Accessibility remains governed by NFR-7 (TalkBack labels, dynamic type and volume);
  dropping these modes does not lower that bar.
- Audio-only mentions that describe WF-1's degraded request or WF-2's audio input are
  unaffected.
- Alternatives considered: keep an audio-only mode (rejected - not needed); defer both
  modes to the backlog (rejected - the constraint is dropped, not postponed).
