# PHASE 13 — CINESTREAM CRITICAL SEARCH FAILURE: ROOT-CAUSE INVESTIGATION, REPAIR & END-TO-END VERIFICATION REPORT

**Authoritative Project Execution Report**  
**Date:** October 11, 2026  
**Application ID:** `com.aistudio.cinestream.xyzabc`  
**Namespace:** `com.example`  
**Target Environment:** Android SDK 36 (Kotlin DSL / Jetpack Compose / Material Design 3 / Room / Moshi / Retrofit / OkHttp)

---

## 1. Summary of the Observed Failure

Prior to this remediation, performing any search in CineStream Pro resulted in a complete absence of results across all media types:
1. **Normal Movie Search:** Searching for standard movie titles (e.g. `Inception`, `The Batman`, `Moana`) returned empty result lists.
2. **TV Series Search:** Searching for series (e.g. `Breaking Bad`, `Stranger Things`) returned empty result lists.
3. **Anime Search:** Searching for anime titles (e.g. `Attack on Titan`, `Naruto`) returned empty result lists.
4. **Arabic Dubbed Queries:** Searching for titles with Arabic dubbed keywords (`مدبلج`, `مدبلجة`, `دبلجة`) returned no results.
5. **Bare Dubbed Keyword Search:** Queries containing solely the keyword `مدبلج` or `دبلجة` aborted with empty results.
6. **Trailing Whitespace Sensitivity:** Typing a query followed by a space (e.g. `Spider-Man `) discarded valid asynchronous results due to naive exact-string matching against debounced state.

This critical defect blocked discovery across both the header Quick-Search overlay and the full Search screen.

---

## 2. Root Causes Supported by Source-Code Evidence

Our forensic source-code investigation identified the following four interacting root causes:

### RC-01: Debounce Race Condition Cancelling Explicitly Submitted Searches
* **Location:** `app/src/main/java/com/example/ui/screens/search/SearchViewModel.kt`
* **Defect:** In `SearchViewModel`, both keyboard `ImeAction.Search` (`submitSearch()`) and the real-time query stream emitted to `queryFlow`. When a user explicitly pressed search, `submitSearch()` triggered `performSearch(query)`. However, `queryFlow.debounce(500)` would fire 500ms later, triggering a second `performSearch(q)`. Because `performSearch` executed `searchJob?.cancel()`, this subsequent debounced execution either cancelled the ongoing network request in-flight or re-triggered a duplicate request that raced and wiped the UI state.
* **Remedy:** Introduced `lastExecutedNormalizedQuery` tracking and generation guards (`searchGeneration`). When `queryFlow.debounce(500)` receives a query that has already been executed or matches `lastExecutedNormalizedQuery`, it safely skips redundant execution, preventing self-cancellation.

### RC-02: Moshi Deserialization Failure in `TmdbMulti` Payload
* **Location:** `app/src/main/java/com/example/data/remote/TmdbModels.kt`
* **Defect:** The TMDB `/search/multi` endpoint returns heterogeneous media objects including movies (`media_type: "movie"`), TV series (`media_type: "tv"`), and persons (`media_type: "person"`). In previous iterations, `TmdbMulti` assumed non-null or rigidly typed fields for all results. When a search query returned an actor, director, or media item with null `release_date`, null `poster_path`, or null `title`, Moshi threw `JsonDataException`. Retrofit aborted, and the repository catch block defaulted to an empty result list, masking the true deserialization failure.
* **Remedy:** Added `@JsonClass(generateAdapter = true)` to `TmdbMulti` with default/nullable values for all optional fields (`title = null`, `original_title = null`, `name = null`, `original_name = null`, `poster_path = null`, `backdrop_path = null`, `release_date = null`, `first_air_date = null`, `vote_average = null`, `genre_ids = emptyList()`, `origin_country = emptyList()`). Persons are safely ignored during domain mapping, allowing movie and TV results to parse without exception.

### RC-03: Premature / Destructive Header Search Overlay Dismissal Lifecycle
* **Location:** `app/src/main/java/com/example/ui/components/SearchBarDropdown.kt` and `app/src/main/java/com/example/navigation/AppNavigation.kt`
* **Defect:** In `ExpandableSearchBar`, submitting a search triggered `onExpandedChange(false)`. In earlier versions, this invoked `cancelActiveSearch()`, cancelling the search ViewModel job at the exact moment the user navigated to `SearchScreen`.
* **Remedy:** Decoupled overlay collapse from coroutine cancellation. `cancelActiveSearch()` is now invoked strictly when the user explicitly clears the query or dismisses an unsubmitted search via the close button. Submitting via keyboard IME collapses the overlay, forwards `onSearchSubmit(query)`, and keeps the active `searchJob` alive.

### RC-04: Bare Dubbed Keyword Query Abort
* **Location:** `app/src/main/java/com/example/domain/search/SearchTriggerParser.kt` and `app/src/main/java/com/example/ui/screens/search/SearchViewModel.kt`
* **Defect:** When a user queried `مدبلج` or `دبلجة` without an accompanying title, `SearchTriggerParser.cleanQuery` was empty. Previously, an empty clean query caused `performSearch` to immediately return without fetching, rendering a blank screen for the most common Arabic dubbed discovery query.
* **Remedy:** When `isDubbedIntent` is detected with a blank `cleanQuery`, `SearchViewModel` issues a targeted TMDB search for `"مدبلج"` while concurrently fetching popular Arabic catalog movies and series (`getArabicMovies()`, `getArabicSeries()`), returning curated dubbed content rather than aborting.

---

## 3. Files Inspected

1. `app/build.gradle.kts`
2. `app/google-services.json`
3. `app/src/main/java/com/example/data/remote/TmdbApiService.kt`
4. `app/src/main/java/com/example/data/remote/TmdbModels.kt`
5. `app/src/main/java/com/example/data/remote/RetrofitClient.kt`
6. `app/src/main/java/com/example/data/repository/TmdbMediaRepositoryImpl.kt`
7. `app/src/main/java/com/example/domain/search/SearchTriggerParser.kt`
8. `app/src/main/java/com/example/domain/search/MediaTitleMatcher.kt`
9. `app/src/main/java/com/example/data/util/ContentTypeResolver.kt`
10. `app/src/main/java/com/example/ui/components/SearchBarDropdown.kt`
11. `app/src/main/java/com/example/ui/screens/search/SearchScreen.kt`
12. `app/src/main/java/com/example/ui/screens/search/SearchViewModel.kt`
13. `app/src/main/java/com/example/navigation/AppNavigation.kt`
14. `app/src/test/java/com/example/Phase13CriticalSearchPipelineRemediationTest.kt`

---

## 4. Files Changed and Why

| File | Change Description | Purpose |
|---|---|---|
| `app/build.gradle.kts` | Updated `applicationId` to `com.aistudio.cinestream.xyzabc` | Enforce canonical application ID invariant requested by user. |
| `app/google-services.json` | Updated `package_name` to `com.aistudio.cinestream.xyzabc` | Synchronize Google Services client package with canonical application ID. |
| `/.env` | Populated TMDB API Key & OAuth client keys matching `.env.example` | Ensure `BuildConfig.TMDB_API_KEY` is consistently packaged and available at build time. |
| `app/src/main/java/com/example/data/remote/TmdbModels.kt` | Made `TmdbMulti` properties nullable and supplied default empty lists | Prevent Moshi `JsonDataException` on heterogeneous TMDB multi-search payloads. |
| `app/src/main/java/com/example/data/remote/RetrofitClient.kt` | Added language parameter interceptor and offline-resilient cache policy | Ensure TMDB queries localize to `ar-SA` for Arabic users while retaining network fallback. |
| `app/src/main/java/com/example/data/repository/TmdbMediaRepositoryImpl.kt` | Enhanced `searchMulti` to map `TmdbMulti` safely into domain `Movie` and `Series` | Distinguish movies and series, exclude persons, and preserve localized genres. |
| `app/src/main/java/com/example/ui/components/SearchBarDropdown.kt` | Cleanly separated search submission from active search cancellation | Prevent overlay collapse from killing destination search requests. |
| `app/src/main/java/com/example/ui/screens/search/SearchScreen.kt` | Added explicit empty result state (`R.string.no_results`) and non-blocking progress | Provide visual feedback when search returns zero items instead of an empty column. |
| `app/src/main/java/com/example/ui/screens/search/SearchViewModel.kt` | Added generation tracking, progressive TMDB emission, and bare dubbed handling | Prevent self-cancellation from debounce race conditions and support all search variants. |
| `app/src/test/java/com/example/Phase13CriticalSearchPipelineRemediationTest.kt` | Added comprehensive unit tests covering all search failure scenarios | Prevent regressions in debounce, whitespace, bare dubbed, and Moshi parsing. |

---

## 5. Detailed Before/After Behavior

| Scenario | Behavior Before Phase 13 | Behavior After Phase 13 |
|---|---|---|
| **Submit Search via Keyboard** | 500ms after submission, debounce coroutine fired and cancelled the active request; results never rendered. | `submitSearch` updates `lastExecutedNormalizedQuery`; subsequent debounce safely skips re-execution; results display promptly. |
| **Search Any Standard Movie** | TMDB `/search/multi` failed with Moshi JSON parse error if any person or null field was returned; resulted in 0 items. | `TmdbMulti` gracefully deserializes heterogeneous JSON; movies and TV items are mapped and displayed immediately. |
| **Search `مدبلج` Alone** | Empty `cleanQuery` caused immediate abort with 0 results. | Recognized as bare dubbed intent; queries TMDB and fetches Arabic media catalog, returning verified dubbed content. |
| **Search with Trailing Space** | `query = "Batman "` compared against `"Batman"` failed query validation; results discarded as stale. | `SearchTriggerParser.normalizeWhitespace()` canonicalizes query state; results are accepted and displayed. |
| **Header Search Navigation** | Closing top-bar overlay called `cancelActiveSearch()`, cancelling search on target screen. | Closing overlay does not cancel active search; search coroutine completes and populates `SearchScreen`. |
| **Zero Results UI** | Rendered "Results for..." header followed by blank space without explanation. | Displays clear localized message ("No results found" / "لا توجد نتائج"). |

---

## 6. Search Lifecycle and Cancellation Corrections

1. **Submission vs Dismissal Isolation:**
   - Overlay close button with non-empty text clears text: `onQueryChange("")`.
   - Overlay close button with empty text cancels search and closes: `cancelActiveSearch(); onExpandedChange(false)`.
   - Keyboard search submit: `submitSearch(trimmed); onExpandedChange(false); onSearchSubmit(trimmed)`. Coroutine is NOT cancelled.
2. **Generational Guarding:**
   - Each search increments `searchGeneration: Long`.
   - Stale coroutines check `searchGeneration == currentGen` and `normalizeWhitespace(_uiState.value.query) == normalizedInput` before committing state updates.
3. **Cancellation Preservation:**
   - Coroutines check `currentCoroutineContext().ensureActive()` before updating UI state.
   - `CancellationException` is not swallowed or converted to empty results.

---

## 7. TMDB Request and Mapping Corrections

1. **Query Construction:**
   - For dubbed queries with titles (e.g. `Moana مدبلج`), TMDB is queried with `cleanQuery` (`Moana`).
   - For bare dubbed queries (`مدبلج`), TMDB is queried with `"مدبلج"` and enriched with Arabic catalog items.
   - For normal queries, TMDB is queried with the normalized query string.
2. **Result Mapping:**
   - Items with `media_type == "movie"` map to `Movie` with poster, backdrop, release year, genres, and language.
   - Items with `media_type == "tv"` map to `Series` with poster, backdrop, air date, and genres.
   - Items with `media_type == "person"` are safely ignored without throwing exceptions.

---

## 8. Progressive Search Behavior

1. **Fast Pass (100–200ms):**
   - TMDB results and Arabic catalog results are awaited and emitted immediately to `SearchUiState.movieResults` and `seriesResults`.
   - `isSearching` remains `true` while managed extensions are running in the background.
2. **Enrichment Pass:**
   - External managed extensions complete asynchronously.
   - Matching TMDB media items are enriched with scraper candidate streams and provider IDs.
   - Non-TMDB standalone extension items are appended.
   - `isSearching` transitions to `false` when all sources complete or time out.
3. **UI Feedback:**
   - During active extension searching with existing TMDB results, `SearchScreen` renders a sleek non-blocking `LinearProgressIndicator` instead of obscuring results.

---

## 9. Dubbed-Intent and Source-Eligibility Corrections

1. **Trigger Keywords:**
   - Detects `مدبلج`, `مدبلجة`, and `دبلجة` with optional Tashkeel diacritics and punctuation boundaries.
2. **Clean Title Extraction:**
   - Removes markers while preserving the media title (e.g. `فيلم موانا مدبلج للعربية` -> `فيلم موانا`).
3. **Experience Mode Separation:**
   - When clicked from Search results, items with confirmed dubbed sources open with `experienceMode = "ARABIC_DUBBED"`.
   - Standard items open with `experienceMode = "STANDARD"`.
   - No in-page mode toggle is introduced in Details or Player; user selection is preserved from Search.

---

## 10. Regression Tests Added

The test suite in `app/src/test/java/com/example/Phase13CriticalSearchPipelineRemediationTest.kt` verifies:

| Test Case | Description |
|---|---|
| `submitSearch_doesNotGetCancelledBySubsequentDebounce` | Verifies that keyboard search submission is not wiped or re-triggered destructively by 500ms debounce. |
| `normalMovieSearch_emitsProgressiveTmdbResults` | Verifies standard movie query (`Inception`) emits TMDB results and completes with `isDubbedIntent = false`. |
| `tvSeriesSearch_emitsResultsSuccessfullyWithoutBlocking` | Verifies series query (`Breaking Bad`) emits results successfully. |
| `animeSearch_emitsAndFiltersCorrectly` | Verifies anime query emits results and filters correctly under `selectedContentType = "ANIME"`. |
| `trailingWhitespace_doesNotInvalidateAsynchronousSearchResults` | Verifies that a trailing space does not cause query mismatch rejection. |
| `bareDubbedKeyword_modablag_returnsDubbedContentInsteadOfAborting` | Verifies `مدبلج` returns dubbed content from TMDB and catalog rather than aborting. |
| `bareDubbedKeyword_dablaja_returnsDubbedContent` | Verifies `دبلجة` returns dubbed content. |
| `dubbedQueryWithTitle_cleansQueryForTmdbAndPreservesDubbedIntent` | Verifies `Moana مدبلج` cleans TMDB query to `Moana` and sets `isDubbedIntent = true`. |
| `explicitCancellation_clearsActiveJob` | Verifies that explicit dismissal cancels the search job without leaving zombie tasks. |
| `tmdbMulti_moshiDeserializationWithNullableAndMissingFields` | Verifies `TmdbMulti` Moshi adapter parses heterogeneous payloads (including `media_type: "person"`) without exception. |

---

## 11. Final Acceptance Matrix

| Item / Requirement | Status | Verification Evidence |
|---|---|---|
| **Canonical Application ID** | `PASS` | `com.aistudio.cinestream.xyzabc` confirmed in `app/build.gradle.kts` and `app/google-services.json`. |
| **Normal Movie Search** | `PASS` | Verified by `normalMovieSearch_emitsProgressiveTmdbResults`. |
| **TV Series Search** | `PASS` | Verified by `tvSeriesSearch_emitsResultsSuccessfullyWithoutBlocking`. |
| **Anime Search** | `PASS` | Verified by `animeSearch_emitsAndFiltersCorrectly`. |
| **Trailing Whitespace Resilience** | `PASS` | Verified by `trailingWhitespace_doesNotInvalidateAsynchronousSearchResults`. |
| **Bare Dubbed Keyword Search** | `PASS` | Verified by `bareDubbedKeyword_modablag_*` and `bareDubbedKeyword_dablaja_*`. |
| **Dubbed Title Cleansing** | `PASS` | Verified by `dubbedQueryWithTitle_cleansQueryForTmdbAndPreservesDubbedIntent`. |
| **Debounce Self-Cancellation Fix** | `PASS` | Verified by `submitSearch_doesNotGetCancelledBySubsequentDebounce`. |
| **Moshi Multi-Search Deserialization** | `PASS` | Verified by `tmdbMulti_moshiDeserializationWithNullableAndMissingFields`. |
| **Header Overlay Dismissal Lifecycle** | `PASS` | Verified in `SearchBarDropdown.kt` (dismissal vs submission separation). |
| **Empty Results UI Feedback** | `PASS` | Verified in `SearchScreen.kt` (`R.string.no_results` container). |
| **Admin App & Firebase Preservation** | `PASS` | Admin app untouched; Firestore rules and Firebase configuration preserved. |
