# Data inventory

Status: draft. What the app stores, where it lives, and whether it leaves the device.
Grounded in the data model and consent rules in `docs/02-architecture.md`, the workflow
input rules in `docs/08-ai-workflows.md`, and NFR-4/NFR-5.

| Data | Storage | Audio | Leaves device | Recipient | Deletion |
| --- | --- | --- | --- | --- | --- |
| Preferences and consent flags | DataStore | no | no | - | settings reset |
| Consent record (version accepted) | DataStore | no | no | - | settings reset |
| Reference audio (curated) | bundled with content | yes | no (already in the APK) | - | not applicable |
| Learner recordings (attempts, turns) | local storage | yes | only to service a request | WF-1, WF-2, WF-4, WF-10 | one-tap delete |
| Attempts and feedback text | Room | no | no (feedback request only) | - | with the attempt/session |
| Conversation sessions and turns | Room | turns may reference audio | audio to WF-2; reply text generated | WF-2 | delete session |
| Field missions and local personas | bundled or Room (generated) | no | content context to WF-9 | WF-9 | delete mission |
| Mission sessions and turns | Room | yes (mission audio) | audio to WF-10 | WF-10 | delete session |
| Debrief entries | Room | no | aggregated themes only | WF-5, WF-9 | one-tap delete |
| Progress | Room | no | aggregated metadata only | WF-5 | one-tap delete |
| Generated exercises (cached) | Room / local | no | generated via WF-8 | WF-8 | clear cache |
| Cached AI audio (replies, generated references) | local cache | yes | generated via WF-3 | WF-3 | one-tap delete |

## Notes

- No entity carries hanzi; pinyin strings carry tone numbers (ADR 0012).
- Raw learner audio is sent only to workflows that must analyze it (FR-24); summaries and
  generation receive aggregated metadata.
- The Worker does not persist learner audio or transcripts beyond serving the request
  (ADR 0003).
