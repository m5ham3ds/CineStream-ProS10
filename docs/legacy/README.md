# Legacy and Diagnostic Artifacts Archive

This directory archives historical diagnostic logs and temporary scripts retained for auditability and compliance with Phase 10A repository organization policies.

## Archived Artifacts

### 1. `firebase-debug.log`
- **Origin:** Generated during Phase 06/07 Firebase CLI environment synchronization and rule deployment testing.
- **Status:** Inactive diagnostic log.
- **Action:** Moved from root to `docs/legacy/` to eliminate root-level noise while preserving diagnostic history.

### 2. `tmp/test_init.gradle`
- **Origin:** Ephemeral initialization script used in Phase 07 Wave 1-2 to isolate legacy compilation candidates. All exclusions were formally removed in Phase 07 Unified Wave 5-7 (L-003 repaired).
- **Status:** Empty configuration hook (`allprojects {}`).
- **Action:** Retained in `tmp/` for backwards compatibility with legacy build commands referencing `tmp/test_init.gradle`.
