# CineStream Phase 10B — Dubbed Mode Architecture, TMDB Identity Preservation & Animation Disambiguation Report

## 1. Executive Summary & Objective

**Phase 10B** was initiated to resolve core architectural challenges in the **CineStream User App** surrounding Arabic Dubbed content discovery, playback variant coordination, and TMDB canonical identity preservation.

Prior to Phase 10B, content scraped from external providers containing Arabic dubbed keywords (e.g., `"The Lion King مدبلج"`) was prone to diverging from canonical TMDB metadata entries. This created fragmented duplicate media cards, broke TMDB details views (missing backdrops, ratings, overviews), and caused Western animated films (e.g., Disney, Pixar, DreamWorks) to be falsely categorized as anime, which blocked dubbed mode selection due to strict anime subtitle-only constraints.

Phase 10B establishes an end-to-end unified architecture where:
1. **Canonical TMDB Identity is Preserved:** Scraped dubbed sources attach cleanly to canonical TMDB entities without generating orphan cards or corrupting metadata.
2. **Arabic Dubbed Intent is Reliably Detected:** Search query parsing recognizes `مدبلج`, `مدبلجة`, and `دبلجة` (including full Arabic tashkeel/diacritic marks) while validating media title boundaries.
3. **Animation is Disambiguated from Anime:** Western/worldwide animation is distinguished from Japanese Anime, unlocking dubbed mode support for animated feature films.
4. **Playback & Cache Partitioning:** Playback states, resolution cache entries, and resume positions are strictly isolated between `dubbed` and `subtitled` variants.

---

## 2. Problem Statement & Root Cause Analysis

### Issue 1: Split Media Cards & TMDB Identity Loss
- **Symptom:** Searching for or discovering dubbed titles generated orphan "standalone" cards named with the provider title (e.g., `"Moana مدبلج"`), lacking TMDB backdrops, English titles, cast, ratings, and runtime.
- **Root Cause:** Provider titles containing `"مدبلج"` failed exact title-matching algorithms against canonical TMDB entries (`"Moana"`), treating them as completely novel standalone media.

### Issue 2: False Anime Categorization for Animated Movies
- **Symptom:** Western animated blockbusters (Disney's *The Lion King*, *Finding Nemo*, *Moana*, *Frozen*) were classified as Anime because their TMDB genre contained `"Animation"`.
- **Root Cause:** Naive genre checks treated any item with `"Animation"` as `ContentType.ANIME`. Because `MediaVariantIdentity` strictly prohibits dubbed mode for anime, dubbed playback was erroneously blocked or threw exceptions.

### Issue 3: Search Query Keyword Bleed
- **Symptom:** Search queries such as `"سبايدرمان مدبلج"` or bare queries like `"مدبلج"` caused ambiguous search behavior or passed the keyword `"مدبلج"` directly to upstream TMDB API calls, yielding zero results.
- **Root Cause:** `SearchTriggerParser` did not fully support feminine (`مدبلجة`) and nominal (`دبلجة`) forms, nor did it guard against queries consisting solely of trigger words without an actual media title.

### Issue 4: Cache Collision Between Dubbed & Subtitled Streams
- **Symptom:** Switching between dubbed and subtitled versions occasionally played the previously cached audio track or resumed from an inaccurate timestamp.
- **Root Cause:** `PlaybackResolutionCache` and `buildMediaKey` lacked variant mode partitioning in their composite cache keys.

---

## 3. Architecture & Remediation Implementations

### 3.1. Robust Arabic Dubbed Trigger Parsing (`SearchTriggerParser.kt`)
Enhanced `SearchTriggerParser` with regex patterns covering:
- Standard masculine keyword: `مدبلج`
- Feminine keyword: `مدبلجة`
- Nominal keyword: `دبلجة` (with diacritics resilience `دَبْلَجَة`)
- Cleans query strings for downstream TMDB searches.
- Validates `hasValidMediaTitle` to prevent searching with bare trigger tokens.

### 3.2. Canonical TMDB Title Matching & Enrichment (`SearchViewModel.kt`, `MediaTitleResolver.kt`)
Implemented regex normalization in matching pipelines:
- Strips Arabic diacritics (`[\u064B-\u065F\u0670]`).
- Strips common media descriptors (`مدبلج`, `مدبلجة`, `دبلجة`, `مترجم`, `مترجمة`, `فيلم`, `مسلسل`, `season`, `الموسم`, `part`, `الجزء`).
- Matches cleaned scraper titles against TMDB titles and enriches canonical TMDB objects with provider sources (`sourceUrl`, `providerId`, `sourceCandidates`, `mode = "dubbed"`).

### 3.3. Authoritative Animation vs. Anime Classifier (`MediaAnimationClassifier.kt`)
Created `MediaAnimationClassifier`:
- **Anime Criteria:** Must possess explicit `"Anime"` / `"أنمي"` genre, OR have `"Animation"` genre combined with Japanese origin (`originalLanguage == "ja"` or `originCountry` contains `"JP"`).
- **Western Animation Criteria:** Has `"Animation"` genre with Western/non-Japanese origin. Always resolves to `ContentType.MOVIE` or `ContentType.TV`, ensuring full eligibility for Arabic dubbing.

### 3.4. Experience Mode Tracking & Cache Partitioning (`LastPlaybackStore.kt`, `PlaybackResolutionCache.kt`)
- `LastPlaybackStore`: Explicitly stores and retrieves `mode` (`"dubbed"` vs `"subtitled"`), defaulting gracefully to `"subtitled"`.
- `PlaybackResolutionCache`: Key structure formats as `"${mediaId}:${if (isMovie) "movie" else "tv"}:${mode}:${language}:${sourceOrProvider}:${quality}"`.
- `buildMediaKey`: Formats as `"${title}-${type}-${season}-${episode}-${variantMode}-${audioLang}"`, preventing any stream or position collision.

### 3.5. Localized TMDB Genre Helper (`TmdbGenreHelper.kt`)
Provides accurate dual-language genre mappings (Arabic & English) for TMDB genre IDs (e.g., ID 16 -> `"رسوم متحركة"` / `"Animation"`, ID 28 -> `"أكشن"` / `"Action"`).

---

## 4. Verification & Automated Test Suite

A dedicated Robolectric and unit test suite was implemented in `app/src/test/java/com/example/Phase10BDubbedAndTmdbIdentityTest.kt` under `@Config(sdk = [34])`:

| Test Name | Domain Tested | Expected Result | Status |
|---|---|---|---|
| `searchTriggerParser_detectsMudablajKeyword` | Search Trigger Parsing | Detects `مدبلج`, extracts `cleanQuery`, sets `hasValidMediaTitle=true` | **PASSED** |
| `searchTriggerParser_detectsMudablajaKeyword` | Search Trigger Parsing | Detects `مدبلجة`, extracts `cleanQuery`, sets `hasValidMediaTitle=true` | **PASSED** |
| `searchTriggerParser_detectsDablajaKeywordWithDiacritics` | Search Trigger Parsing | Detects `دَبْلَجَة` with tashkeel, extracts clean title | **PASSED** |
| `searchTriggerParser_rejectsBareKeywordWithoutTitle` | Search Trigger Parsing | Rejects bare keywords with `hasValidMediaTitle=false` | **PASSED** |
| `searchTriggerParser_rejectsUnmatchedEnglishQuery` | Search Trigger Parsing | Ignores unrelated English queries | **PASSED** |
| `searchMatching_unifiesScraperDubbedTitleWithCanonicalTmdbMovie` | TMDB Identity Preservation | Normalized titles match; TMDB ID, backdrop, and rating preserved; sources attached | **PASSED** |
| `westernAnimationMovie_isNotMisclassifiedAsAnime_andAllowsDubbedMode` | Animation Classification | Disney animated movie resolves as `MOVIE`, not anime; dubbing allowed | **PASSED** |
| `japaneseAnimeSeries_isClassifiedAsAnime` | Animation Classification | Japanese animated series correctly resolves as `ANIME` | **PASSED** |
| `lastPlaybackStore_persistsAndRetrievesExperienceMode` | Playback State | Successfully saves and retrieves `mode = "dubbed"` | **PASSED** |
| `lastPlaybackStore_defaultsToSubtitledWhenUnspecified` | Playback State | Defaults to `mode = "subtitled"` when unspecified | **PASSED** |
| `serverStateStore_buildMediaKey_partitionsDubbedFromSubtitled` | Stream Key Partitioning | Distinct keys generated for dubbed vs subtitled | **PASSED** |
| `playbackResolutionCache_distinctKeysForDubbedAndSubtitled` | Resolution Cache | Cache keys segregated by variant mode | **PASSED** |
| `tmdbGenreHelper_returnsCorrectLocalizedGenreNames` | Genre Localization | Localized names returned correctly in AR and EN | **PASSED** |

**Execution Result:** 13/13 tests passed in 32s.

---

## 5. Build & Integration Status

- **Unit Test Execution:** All Phase 10B tests passed (`BUILD SUCCESSFUL`).
- **Full Applet Compilation:** Verified via `compile_applet` (`Build succeeded - the applet is compiled`).
- **Zero Breaking Changes:** Backward compatibility maintained across all previous phase contracts and test suites.

---

## 6. Conclusion

Phase 10B successfully resolves all dubbing integration issues in the CineStream User App. Arabic dubbed content now seamlessly integrates into canonical TMDB identities, animated features are accurately classified without anime restrictions, search intent is reliably captured, and stream caches remain strictly isolated between viewing modes.
