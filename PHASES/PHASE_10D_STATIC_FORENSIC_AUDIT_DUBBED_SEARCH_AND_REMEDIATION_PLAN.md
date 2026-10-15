# CineStream Phase 10D — Static Forensic Audit & Dubbed Search Remediation Plan

## 1. Executive Summary & Audit Scope

**Phase 10D** was conducted as an exhaustive, strictly **Static Forensic Code and Architectural Audit** of the CineStream User Application (`com.aistudio.cinestream.xyzabc`).

### Objective
Diagnose and document the exact root causes of:
1. **Search & Dubbed Query Discovery Failures:** Missing Arabic dubbed results (`مدبلج`, `مدبلجة`, `دبلجة`), trailing whitespace bugs, space-bar input invalidation, and scraper title mismatches.
2. **Details Screen Architecture:** Reconciling the current single-screen toggle implementation against the approved architecture of two completely independent screens (`STANDARD` vs `ARABIC_DUBBED`).
3. **Elimination of Variant Switching:** Identifying all UI affordances, BottomSheets, FilterChips, player toggles, and fallback dialogs that allow switching between dubbed and subtitled versions.
4. **Strict Isolation of Dubbed Extensions:** Ensuring that in `ARABIC_DUBBED`, search, server discovery, stream extraction, playback orchestration, and downloads are restricted exclusively to eligible dubbed extensions (`arabicDubbed` group with `AudioCapability.ARABIC_DUB`), with zero fallback to subtitle providers.
5. **Accurate Error States & Failure Reporting:** Verifying that `NOT_FOUND` is not conflated with `NETWORK_ERROR`, `TIMEOUT`, `CHALLENGE`, or `UNSUPPORTED`, and ensuring terminal failure displays clear guidance without auto-switching.
6. **Anime vs Western Animation Disambiguation:** Auditing `MediaAnimationClassifier`, `ContentTypeResolver`, and TMDB mapping to ensure Western animation (e.g. Disney's *Moana*) is eligible for Arabic dubbing while Japanese anime is strictly subtitle-only.
7. **Genre Taxonomy & Language Synchronization:** Investigating why Arabic genre names persist when the application is set to English.
8. **Test Suite Static Review:** Evaluating existing regression tests from Phase 09C, 09B-B, 10B, and 10C without execution.
9. **Historical Phase Reconciliation:** Reconciling superseded decisions from Phase 09A through Phase 10C with the final approved design.
10. **Prioritized Remediation Blueprint:** Producing an actionable, phased repair plan for subsequent implementation phases.

---

## 2. Mandatory Restrictions & Execution Declarations

```text
================================================================================
VERIFICATION STATUS:
- STATIC_AUDIT_COMPLETE

EXPLICIT EXECUTION DECLARATIONS:
- BUILD NOT RUN — AS REQUESTED
- TESTS NOT RUN — AS REQUESTED
- PRODUCTION CODE NOT MODIFIED
- PHASE REPORTS NOT DELETED OR MOVED
================================================================================
```

| Mandatory Constraint | Status | Static Audit Verification |
|---|---|---|
| No Gradle execution | **ENFORCED** | Zero Gradle tasks, daemons, or compile steps invoked. |
| No Test execution | **ENFORCED** | Zero JUnit, Robolectric, AndroidTest, or Node.js tests executed. |
| No APK/AAB builds | **ENFORCED** | Zero artifacts generated; no build caches altered. |
| Single `/PHASES/` directory | **ENFORCED** | Report written directly to `/PHASES/`. Zero nested directories created. |
| Zero production code changes | **ENFORCED** | No Kotlin, XML, JSON, or Gradle files modified. |
| Package identity preservation | **ENFORCED** | Application ID remains `com.aistudio.cinestream.xyzabc`, namespace `com.example`. |

---

## 3. Inspected Repository Files

The static audit physically inspected the following core production and test files:

```text
Domain & Search:
├── app/src/main/java/com/example/domain/search/SearchTriggerParser.kt
├── app/src/main/java/com/example/domain/models/Media.kt
├── app/src/main/java/com/example/domain/models/VariantModels.kt
├── app/src/main/java/com/example/data/remote/TmdbApiService.kt
├── app/src/main/java/com/example/data/repository/TmdbMediaRepositoryImpl.kt
├── app/src/main/java/com/example/data/util/ContentTypeResolver.kt
├── app/src/main/java/com/example/data/util/MediaAnimationClassifier.kt
└── app/src/main/java/com/example/data/util/TmdbGenreHelper.kt

UI & Navigation:
├── app/src/main/java/com/example/navigation/Screen.kt
├── app/src/main/java/com/example/navigation/AppNavigation.kt
├── app/src/main/java/com/example/ui/screens/search/SearchViewModel.kt
├── app/src/main/java/com/example/ui/screens/search/SearchScreen.kt
├── app/src/main/java/com/example/ui/components/SearchBarDropdown.kt
├── app/src/main/java/com/example/ui/screens/details/DetailsScreens.kt
├── app/src/main/java/com/example/ui/screens/details/MovieDetailsViewModel.kt
├── app/src/main/java/com/example/ui/screens/details/SeriesDetailsViewModel.kt
├── app/src/main/java/com/example/ui/components/InlineDetailVideoPlayer.kt
├── app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt
└── app/src/main/java/com/example/ui/screens/player/PlayerViewModel.kt

Managed Extensions, Playback & Downloads:
├── app/src/main/java/com/example/extension/orchestrator/ManagedMediaOrchestrator.kt
├── app/src/main/java/com/example/extension/managed/usecase/SearchManagedExtensionsUseCase.kt
├── app/src/main/java/com/example/extension/managed/usecase/DiscoverManagedServersUseCase.kt
├── app/src/main/java/com/example/extension/managed/usecase/ManagedExtensionResolver.kt
├── app/src/main/java/com/example/extension/managed/searchorder/SearchOrder.kt
├── app/src/main/java/com/example/extension/managed/searchorder/ExtensionEligibilityFilter.kt
├── app/src/main/java/com/example/extension/managed/playback/PlaybackOrchestrator.kt
├── app/src/main/java/com/example/extension/managed/playback/PlaybackSession.kt
├── app/src/main/java/com/example/extension/managed/adapter/UnifiedDownloadCoordinator.kt
├── app/src/main/java/com/example/extension/managed/model/ExtractionModels.kt
├── app/src/main/java/com/example/data/model/DownloadItem.kt
├── app/src/main/java/com/example/ui/screens/player/ServerStateStore.kt
├── app/src/main/java/com/example/extension/managed/scraper/EgyDeadScraper.kt
└── app/src/main/java/com/example/extension/managed/scraper/QfilmScraper.kt

Preferences & App Lifecycle:
├── app/src/main/java/com/example/MainActivity.kt
├── app/src/main/java/com/example/data/repository/UserPreferencesRepository.kt
└── app/src/main/java/com/example/ui/screens/settings/SettingsScreen.kt

Test Files Audited Statically:
├── app/src/test/java/com/example/Phase10BDubbedAndTmdbIdentityTest.kt
├── app/src/test/java/com/example/Phase09CCrashRemediationAndVariantUITest.kt
├── app/src/test/java/com/example/extension/managed/Phase09BDubbedAndMultiProviderTest.kt
├── app/src/test/java/com/example/extension/managed/Phase09BAuditVerificationTest.kt
├── app/src/test/java/com/example/extension/managed/searchorder/SearchOrderAndEligibilityTest.kt
└── app/src/test/java/com/example/extension/managed/ManagedMediaOrchestratorTest.kt
```

---

## 4. Deep Forensic Analysis of the Seven Problem Domains

### Domain 1: Search for Dubbed Works & Whitespace Normalization

#### The Complete Traced Search Pathway
$$\text{Search Input (UI)} \xrightarrow{\text{onQueryChange}} \text{queryFlow (debounce 500ms)} \xrightarrow{\text{performSearch}} \text{SearchTriggerParser} \xrightarrow{\text{TMDB Search}} \text{searchMedia (Orchestrator)} \xrightarrow{\text{matchesMovie}} \text{Result Integration} \xrightarrow{\text{SearchScreen Card}}$$

#### Critical Forensic Discoveries:
1. **Trailing Space Discard Race Condition:**
   - In `SearchViewModel.kt:194`, `onQueryChange(query)` receives the raw text (e.g. `"Moana مدبلج "`). It updates `queryFlow.value = query` and sets `_uiState.update { it.copy(query = query) }`.
   - When the user presses the space-bar, `queryFlow` debounces and calls `performSearch(query)`.
   - Inside `performSearch(query)`:
     - Line 235: `if (searchGeneration != currentGen || _uiState.value.query != query) return@launch`
     - Line 249: `if (searchGeneration != currentGen || _uiState.value.query != query) return@launch`
     - Line 397: `if (current.query == query) { ... }`
   - If the user types a space while an in-flight network request for `"Moana مدبلج"` is executing, `_uiState.value.query` has become `"Moana مدبلج "`. Consequently, line 235 or 249 sees `_uiState.value.query != query` (`"Moana مدبلج "` $\neq$ `"Moana مدبلج"`), immediately **aborting and discarding** the valid search results that were already returning from TMDB or the scrapers!
2. **Untrimmed Query Propagation to Scrapers:**
   - In `SearchViewModel.kt:241`: `orchestrator.searchMedia(query)` is called with the raw, untrimmed `query`.
   - In `SearchManagedExtensionsUseCase.kt:183`: `titleQuery` from `SearchTriggerParser.parse(trimmedQuery).cleanQuery` is passed into `SearchRequest(query = titleQuery)`.
   - While `cleanQuery` is `"Moana"`, the query sent to EgyDead and QFilm is `"Moana"`.
3. **Cross-Language Scraper Title Mismatch (The Arabic vs English TMDB Divide):**
   - In `TmdbApiService.kt:184`: `searchMulti` has **NO** `language` query parameter. TMDB defaults to English, returning `movie.title = "Moana"` and `movie.originalTitle = "Moana"`.
   - When EgyDead or QFilm are queried, their Arabic titles are returned as: `"فيلم موانا مدبلج للعربية"` or `"موانا مدبلج"`.
   - In `SearchViewModel.kt:264-277`:
     ```kotlin
     fun matchesMovie(managedTitle: String, movie: Movie): Boolean {
         val mNorm = normalizeForMatch(managedTitle) // "موانا"
         val tNorm = normalizeForMatch(movie.title) // "moana"
         val oNorm = normalizeForMatch(movie.originalTitle) // "moana"
         val qNorm = normalizeForMatch(cleanQuery) // "moana"
         ...
     }
     ```
   - Because `mNorm` is Arabic (`"موانا"`) and `tNorm`/`oNorm`/`qNorm` are Latin characters (`"moana"`), **every single equality and containment check returns `false`!**
   - Result:
     - The canonical TMDB movie "Moana" fails to match the scraper item. Its `sourceCandidates` remains empty!
     - The scraper item `"فيلم موانا مدبلج"` is dumped into `standaloneManagedMovies` with an auto-generated id (`"managed_m_..."`), zero rating, empty synopsis, and missing TMDB metadata.
     - The user sees a broken or un-enriched item, or clicking the TMDB card yields zero sources.
4. **Scraper Results Conflation in Dubbed Search:**
   - In `SearchManagedExtensionsUseCase.kt:182-240`: When `isArabicDubbedIntent == true`, `SearchRequest(query = "Moana", isArabicDubbedIntent = true)` is sent to EgyDead.
   - EgyDead's search returns *both* `"فيلم Moana مترجم"` (subtitled) and `"فيلم Moana مدبلج"` (dubbed).
   - Neither `EgyDeadScraper` nor `SearchManagedExtensionsUseCase` filters the resulting items to verify that the returned post title actually contains `"مدبلج"`.
   - `SearchManagedExtensionsUseCase` wraps *all* returned items in `ProviderSourceCandidate(mode = MediaVariantMode.DUBBED)`.
   - If the subtitled post happens to be returned first by EgyDead, it is assigned as the primary dubbed source!

---

### Domain 2: Separation of Details Screens (STANDARD vs ARABIC_DUBBED)

#### Current Implementation vs Approved Architectural Contract

| Architectural Layer | Current Physical Code | Approved Architectural Contract |
|---|---|---|
| **Navigation Route** | Reuses `movie_details/{movieId}?experienceMode={experienceMode}` | Distinct destinations: Standard Movie Details vs Dubbed Movie Details |
| **Experience Parameter** | `experienceMode: String = "STANDARD"` passed as optional query param | Strongly-typed, immutable route context (`STANDARD` vs `ARABIC_DUBBED`) |
| **State Retention** | `selectedVariantMode` is mutable in-page Compose state | Immutable screen context derived strictly from navigation arguments |
| **Details Screen Composables** | Single `MovieDetailsScreen` containing an in-page toggle | Dedicated Standard screen vs Dedicated Dubbed screen (or single screen with immutable mode where all switching controls are stripped) |
| **Cache Partitioning** | `MovieDetailsViewModel.saveState(movieId)` and `MediaDetailsCacheManager` use raw `movieId` | Cache partitioned by `(movieId, experienceMode)` to eliminate cross-talk |

#### Evidence of Cache Cross-Talk:
- `MovieDetailsViewModel.kt:28-38`:
  ```kotlin
  private val cache = ConcurrentHashMap<String, MovieDetailsUiState>()
  fun getCachedState(movieId: String): MovieDetailsUiState? = cache[movieId]
  ```
  `movieId` has no experience mode prefix or suffix. When a user opens "Moana" from standard search, `cache["12445"]` is stored with standard sources. If the user then searches "Moana مدبلج" and opens the movie, `getCachedState("12445")` returns the standard state, poisoning the dubbed experience!

---

### Domain 3: Complete Elimination of Variant Switching Affordances

The user has explicitly mandated that **zero switching between dubbed and subtitled** shall be permitted inside the details screen or the player.

#### Audit of All Physical Switching Affordances in Current Codebase:

1. **`DetailsScreens.kt:582-600` (Movie FilterChips):**
   ```kotlin
   FilterChip(selected = selectedVariantMode == "subtitled", onClick = { selectedVariantMode = "subtitled" })
   FilterChip(selected = selectedVariantMode == "dubbed", onClick = { selectedVariantMode = "dubbed" })
   ```
   *Status:* Physical in-page toggle allowing users to switch variants. Must be removed.
2. **`DetailsScreens.kt:1452-1470` (Series FilterChips):**
   ```kotlin
   FilterChip(selected = effectiveMode == "subtitled", onClick = { selectedVariantMode = "subtitled" })
   FilterChip(selected = effectiveMode == "dubbed", onClick = { selectedVariantMode = "dubbed" })
   ```
   *Status:* Physical in-page toggle allowing users to switch series variants. Must be removed.
3. **`DetailsScreens.kt:654-662` & `1524-1532` ("Dubbed Unavailable" Switch Action):**
   ```kotlin
   TextButton(onClick = { selectedVariantMode = "subtitled" }) {
       Text(text = stringResource(R.string.switch_to_subtitled_action))
   }
   ```
   *Status:* Auto-recovery button that switches the screen context from dubbed to subtitled when dubbed sources are missing. Must be removed.
4. **`InlineDetailVideoPlayer.kt:1812-1816` (Clickable Player Badge):**
   ```kotlin
   Surface(modifier = Modifier.clickable {
       val newMode = if (isDubbed) "subtitled" else "dubbed"
       onToggleVariant(newMode)
   })
   ```
   *Status:* Clicking the badge on the inline player flips `selectedVariantMode`. Must be removed.
5. **`PlayerScreen.kt:1520-1530` (Standalone Player Bottom Action):**
   ```kotlin
   BottomAction(
       icon = Icons.Default.Translate,
       text = variantText,
       onClick = { showVariantSheet = true }
   )
   ```
   *Status:* Player control button that launches a variant selection sheet. Must be removed.
6. **`PlayerScreen.kt:1702-1760` (`showVariantSheet` BottomSheet):**
   Modal bottom sheet presenting "Subtitled Arabic" and "Dubbed Arabic" options with checkmarks, calling `viewModel.selectVariantMode("subtitled")` / `viewModel.selectVariantMode("dubbed")`.
   *Status:* Interactive bottom sheet switching variants inside the player. Must be removed.
7. **`PlayerViewModel.kt:177-190` (`selectVariantMode`):**
   ```kotlin
   fun selectVariantMode(newMode: String) {
       _uiState.update { it.copy(mode = newMode, isLoading = true, currentVideoUrl = null) }
       ...
   }
   ```
   *Status:* ViewModel mutation method supporting player mode switches. Must be eliminated.

---

### Domain 4: Strict Isolation of Dubbed Extensions Across All Layers

In `ARABIC_DUBBED`, the application must query **only** eligible dubbed extensions. Under no circumstances may a subtitle extension be invoked as a fallback.

#### Deep Code Audit of the Playback & Download Pipeline:

1. **`PlaybackOrchestrator.kt:111-128` (Candidate Resolution Failure):**
   ```kotlin
   // 1. Fetch canonical Search Order for requested content type
   val searchOrderIds = searchOrderRepository.getOrderForContentType(targetContentType)
   // For MOVIE, this returns listOf("akvam", "egydead", ...) from the standard 'movie' list!
   
   val initialCandidates = eligibilityFilter.filterEligibleExtensions(
       orderedExtensionIds = searchOrderIds,
       availableExtensions = allExtensions,
       targetContentType = targetContentType,
       capability = ScraperCapability.SERVER_DISCOVERY,
       requiredAudioCapability = null // OMITTED!
   )
   ```
   *Critical Finding:* `PlaybackOrchestrator` consumes `getOrderForContentType(targetContentType)`, which reads the standard subtitle list (`movie` or `series`). It **never** reads `arabicDubbed` from `SearchOrder`, and **never** passes `requiredAudioCapability = AudioCapability.ARABIC_DUB`.
   *Consequence:* When active playback begins, `PlaybackOrchestrator` executes the subtitle scrapers instead of the dubbed scrapers!
2. **`DiscoverManagedServersUseCase.kt:59-63` (Unconstrained Server Discovery):**
   ```kotlin
   val candidates = resolver.resolveEligibleExtensions(
       capability = ScraperCapability.SERVER_DISCOVERY,
       contentType = targetContentType,
       allowDeprecated = false
   )
   ```
   *Critical Finding:* Resolves all extensions by general priority without filtering for `AudioCapability.ARABIC_DUB`.
3. **`UnifiedDownloadCoordinator.kt` & `DownloadSource` (Context Erasure):**
   - `DownloadSource` (`ExtractionModels.kt:72-87`) does not possess `variantMode` or `audioLanguage` fields.
   - When a download is initiated, the dubbed context is lost, and `DownloadItem` defaults to `variantMode = "subtitled"`.

---

### Domain 5: Terminal Failure & Error Disambiguation

#### Error Categorization Contract vs Physical Code:

| Error Type | Scraper / Network Cause | Current Code Mapping | Required UX Behavior |
|---|---|---|---|
| `NOT_FOUND` | Provider completed search/extraction with 0 results | `CoverageStatus.NOT_FOUND` | Display "النسخة المدبلجة غير متوفرة لهذا العمل". No fallback to subtitle. |
| `NETWORK_ERROR` | `IOException`, `UnknownHostException`, `ConnectException` | `CoverageStatus.NETWORK_ERROR` | Display network error indicator with manual retry button. |
| `TIMEOUT` | Request exceeded timeout window | `CoverageStatus.TIMEOUT` | Display timeout notice with retry button. |
| `CHALLENGE` | Cloudflare / Turnstile challenge detected | `CoverageStatus.CHALLENGE` | Launch interactive challenge webview without switching mode. |
| `UNSUPPORTED` | Provider lacks search or stream extraction capability | `CoverageStatus.UNKNOWN_CAPABILITY` | Skip silently to next eligible dubbed provider. |

*Defect in Current UX:* In `DetailsScreens.kt:620-665`, when dubbed servers are empty (`isDubbedUnavailable`), the application displays a banner offering an explicit action to switch to subtitled (`switch_to_subtitled_action`). In the target architecture, this button must be eliminated; only a clean informative notice is displayed, and the user returns to search if they wish to choose the subtitled version.

---

### Domain 6: Anime vs Western Animation Disambiguation

#### Analysis of Classification Logic:
1. **`MediaAnimationClassifier.kt:20-46`:**
   ```kotlin
   fun isAnime(movie: Movie): Boolean {
       if (movie.genres.any { it.equals("Anime", ignoreCase = true) || it.equals("أنمي", ignoreCase = true) }) return true
       val isJapaneseIdentity = movie.language.equals("ja", ignoreCase = true) || movie.country.equals("JP", ignoreCase = true)
       val hasAnimationGenre = movie.genres.any { it.contains("Animation", ignoreCase = true) } || movie.genreIds.contains(16)
       if (hasAnimationGenre && isJapaneseIdentity) return true
       ...
       return false
   }
   ```
   - Correctly establishes that Western animation (e.g. *Moana*, *Frozen*, *Toy Story*) having `language = "en"` and `country = "US"` is **NOT** anime, allowing it to be dubbed.
2. **The Repository Mapping Bug in `TmdbMediaRepositoryImpl.kt:580-592`:**
   ```kotlin
   movies.add(Movie(
       id = item.id.toString(),
       title = item.title ?: "Unknown",
       ...
       genres = TmdbGenreHelper.getMovieGenreNames(rawGenreIds),
       runtime = 120,
       genreIds = rawGenreIds
       // CRITICAL DEFECT: language, country, originalTitle ARE NOT PASSED!
   ))
   ```
   - `Movie.language` defaults to `"en"`.
   - `Movie.country` defaults to `null`.
   - `Movie.originalTitle` defaults to `null`.
   - *Impact on Japanese Anime Movies:* When TMDB returns an actual Japanese anime movie (e.g. *Spirited Away*, *Your Name*), `item.originalLanguage` is `"ja"`. But because `TmdbMediaRepositoryImpl` fails to forward `originalLanguage` into `Movie(language = ...)`, the movie is created with `language = "en"`. As a result, `MediaAnimationClassifier.isAnime(movie)` fails to identify genuine anime movies, erroneously treating them as Western films!

---

### Domain 7: Genre Taxonomy & Localization Persistence

#### Why Arabic Genres Appear in the English Interface:
1. **Unpartitioned Cache Keys in `TmdbMediaRepositoryImpl.kt`:**
   - In `getCachedListFlow`:
     - Line 466: `getCachedListFlow("trending_movies")`
     - Line 471: `getCachedListFlow("popular_movies")`
     - Line 558: `getCachedListFlow("series_genre_$genreId")`
   - The cache key does **NOT** include the application language tag (`"trending_movies_ar"` vs `"trending_movies_en"`).
   - If the user previously ran the app in Arabic, `MediaListDiskCacheManager` saved the list to disk with Arabic genre strings.
   - When the user switches the app language to English and restarts, `MediaListDiskCacheManager.loadList(context, "trending_movies")` reads the cached disk file and emits Arabic genre names!
2. **Static String Persistence in `Movie.genres`:**
   - `Movie.genres` stores pre-computed string literals (`listOf("رسوم متحركة", "مغامرة")`).
   - In `DetailsScreens.kt:470, 498, 534`:
     ```kotlin
     Text("${movie.year} • ${movie.genres.take(3).joinToString(" • ")}")
     ```
   - The UI composables print `movie.genres` directly instead of resolving `movie.genreIds` via `TmdbGenreHelper.getMovieGenreNames(movie.genreIds, currentLocale)` at composition time.
3. **Missing Process-Level Locale Configuration in `SettingsScreen.kt:354-364`:**
   - When changing language, `SettingsScreen` updates DataStore and immediately forces a process restart (`Runtime.getRuntime().exit(0)`).
   - It does not invoke `AppCompatDelegate.setApplicationLocales` prior to exiting, nor does it invalidate the persistent disk caches (`MediaListDiskCacheManager` and `MediaDetailsCacheManager`).

---

## 5. Comprehensive Audit Findings Table

| Issue ID | Severity | File | Symbol | Static Evidence | Current Behavior | Expected Behavior | Root Cause | Confidence | Dependencies | Recommended Fix | Regression Risks |
|---|---|---|---|---|---|---|---|---|---|---|---|
| **ISS-10D-01** | **CRITICAL** | `SearchViewModel.kt` | `performSearch` / `onQueryChange` | Lines 194, 235, 249, 397: `_uiState.value.query != query` | Typing a trailing space aborts in-flight search results because raw string differs. | Whitespace normalized; trailing spaces reuse logical clean query key. | Lack of query normalization prior to coroutine state validation. | **HIGH** | `SearchScreen`, `SearchTriggerParser` | Normalize query with `.trim().replace(Regex("\\s+"), " ")` before dispatching and caching. | Low; must preserve search input text field cursor position. |
| **ISS-10D-02** | **CRITICAL** | `SearchViewModel.kt` | `matchesMovie` / `searchMulti` | Lines 264-277: `mNorm == tNorm`, `mNorm.contains(tNorm)` | English TMDB titles ("Moana") never match Arabic scraper titles ("موانا مدبلج"). | Scraper results successfully enrich canonical TMDB entries regardless of title language. | `searchMulti` omits Arabic language parameter; matcher lacks cross-lingual alias resolution. | **HIGH** | `TmdbApiService`, `TmdbMediaRepositoryImpl` | Pass localized language to `searchMulti`; match against both Arabic title and original title. | Low; requires dual-title indexing in search results. |
| **ISS-10D-03** | **CRITICAL** | `SearchManagedExtensionsUseCase.kt` | `executeArabicDubbedSearch` | Lines 182-240: queries EgyDead with stripped title; wraps all items in `DUBBED` | Scraper returns both sub and dub posts; subtitled posts can be erroneously marked as dubbed. | Only items whose title explicitly indicates dubbed content are accepted. | Scraper results not filtered for `مدبلج` keyword when `isArabicDubbedIntent == true`. | **HIGH** | `EgyDeadScraper`, `QfilmScraper` | Enforce that scraped items must contain dubbed markers before accepting them as dubbed candidates. | Medium; scrapers must consistently tag dubbed items. |
| **ISS-10D-04** | **CRITICAL** | `PlaybackOrchestrator.kt` | `orchestratePlayback` | Lines 111-128: `searchOrderRepository.getOrderForContentType(targetContentType)` | Uses standard subtitle `movie` list during playback; omits `requiredAudioCapability`. | In `ARABIC_DUBBED`, loads strictly from `arabicDubbed` search order with `ARABIC_DUB` capability. | `PlaybackOrchestrator` lacks awareness of experience mode and audio language. | **HIGH** | `PlaybackSession`, `SearchOrderRepository` | Pass `variantMode` and `audioLanguage` to `PlaybackSession`; query `getDubbedOrder` when mode is DUBBED. | Medium; must ensure fallback stays within dubbed group. |
| **ISS-10D-05** | **HIGH** | `DetailsScreens.kt` | `MovieDetailsScreen` / `SeriesDetailsScreen` | Lines 582-600, 1452-1470: `FilterChip(selected = selectedVariantMode == ...)` | Displays in-page FilterChips allowing user to toggle between subtitled and dubbed. | Zero in-page variant toggles; screen is strictly determined by navigation context. | Reusing single screen layout with mutable variant toggle state. | **HIGH** | `Screen`, `AppNavigation` | Remove FilterChips; bind screen mode immutably to `initialExperienceMode`. | Low; simplifies UI code. |
| **ISS-10D-06** | **HIGH** | `DetailsScreens.kt` | `dubbed_unavailable_banner_message` | Lines 654-662, 1524-1532: `TextButton(onClick = { selectedVariantMode = "subtitled" })` | Offers user action to switch from dubbed to subtitled when dubbed stream is missing. | Clean informative notice with zero switch action; user returns to search if desired. | Violation of independent experience design. | **HIGH** | `DetailsScreens.kt` | Remove `switch_to_subtitled_action` button; display informational message only. | Low; aligns with user requirement. |
| **ISS-10D-07** | **HIGH** | `PlayerScreen.kt` | `showVariantSheet` / `BottomAction` | Lines 1520-1530, 1702-1760: bottom action launches variant picker sheet | Player controls provide a BottomSheet allowing user to switch between sub and dub. | Zero variant switching in player; player reflects the launched experience only. | Legacy multi-variant player architecture from Phase 09C. | **HIGH** | `PlayerViewModel.kt` | Remove `showVariantSheet`, bottom action icon, and `selectVariantMode` method. | Low; cleaner player UI. |
| **ISS-10D-08** | **HIGH** | `InlineDetailVideoPlayer.kt` | Top bar badge | Lines 1812-1816: `clickable { onToggleVariant(newMode) }` | Clicking the variant badge on the inline player flips playback between sub and dub. | Badge is purely informational (non-clickable indicator). | Inline player retained toggle lambda. | **HIGH** | `DetailsScreens.kt` | Remove `clickable` modifier and `onToggleVariant` callback from inline player. | Low. |
| **ISS-10D-09** | **HIGH** | `TmdbMediaRepositoryImpl.kt` | `searchMulti` Movie mapping | Lines 580-592: omits `originalLanguage`, `originCountry`, `originalTitle` | Movie object created with `language = "en"`, causing anime movies to fail classification. | Movie preserves TMDB `originalLanguage` and country for accurate classifier evaluation. | Incomplete DTO-to-Domain mapping in `searchMulti`. | **HIGH** | `MediaAnimationClassifier` | Map `language = item.originalLanguage ?: "en"`, `country = item.originCountry?.firstOrNull()`. | Low; fixes anime movie classification. |
| **ISS-10D-10** | **HIGH** | `TmdbMediaRepositoryImpl.kt` | `getCachedListFlow` / `movieDetailsCache` | Lines 466, 471, 558: cache keys lack language (`"trending_movies"`) | Cached Arabic genre lists are emitted when application language is English. | Cache keys partitioned by language (`"trending_movies_en"` vs `"trending_movies_ar"`). | Unpartitioned cache keys across memory and disk caches. | **HIGH** | `MediaListDiskCacheManager`, `MediaDetailsCacheManager` | Incorporate active language code into all repository cache keys. | Low; prevents stale localized data. |
| **ISS-10D-11** | **MEDIUM** | `DetailsScreens.kt` | Movie/Series Header | Lines 470, 498, 534: `Text("${movie.genres.joinToString()}")` | Prints hardcoded genre string list directly without re-localizing for current locale. | Dynamically re-resolves `movie.genreIds` using `TmdbGenreHelper` at UI render time. | UI relies on pre-computed string literals rather than genre IDs. | **MEDIUM** | `TmdbGenreHelper` | Re-resolve genre names dynamically in Composable using `movie.genreIds`. | Low; ensures real-time localization. |
| **ISS-10D-12** | **MEDIUM** | `SearchBarDropdown.kt` | Quick Search click handler | Line 180: `onMovieClick(id)` does not pass `experienceMode` | Quick Search always navigates to `STANDARD`, losing dubbed intent. | Quick Search passes `targetMode` to navigation route. | Missing parameter forwarding in dropdown search. | **MEDIUM** | `AppNavigation.kt` | Pass `experienceMode = if (uiState.isDubbedIntent) "ARABIC_DUBBED" else "STANDARD"`. | Low; restores parity with full search. |
| **ISS-10D-13** | **MEDIUM** | `DiscoverManagedServersUseCase.kt` | `execute` | Lines 59-63: `resolveEligibleExtensions` without audio capability | Server discovery queries all extensions by priority without dubbed filtering. | When in dubbed mode, discovery queries strictly extensions with `ARABIC_DUB`. | Use case lacks awareness of audio variant mode. | **MEDIUM** | `ManagedExtensionResolver` | Pass `requiredAudioCapability` when discovery is executed in dubbed context. | Medium. |
| **ISS-10D-14** | **MEDIUM** | `UnifiedDownloadCoordinator.kt` | `download` / `DownloadSource` | `DownloadSource` lacks `variantMode` and `audioLanguage` fields | Download registrations lose their dubbed identity and revert to subtitled. | Downloads explicitly store `variantMode = "dubbed"` and `audioLanguage = "ar"`. | Incomplete download data model contract. | **MEDIUM** | `DownloadItem`, `StreamDownloaderService` | Add `variantMode` and `audioLanguage` to `DownloadSource` and forward to `DownloadItem`. | Low; ensures offline tracking integrity. |

---

## 6. Review of Test Suites & Historical Phase Reports

### A. Static Review of Key Test Suites (Without Execution)

1. **`Phase10BDubbedAndTmdbIdentityTest.kt`:**
   - *Scope:* Evaluates `SearchTriggerParser` for keywords (`مدبلج`, `مدبلجة`, `دبلجة`), bare keyword rejection, and Western animation classification.
   - *Static Limitation:* Uses local mock helper functions for `normalizeForMatch` rather than testing the real `SearchViewModel.performSearch` pipeline. Assumes English scraper titles (`"The Lion King مدبلج"`) and misses cross-lingual Arabic scraper matching.
2. **`Phase09CCrashRemediationAndVariantUITest.kt`:**
   - *Scope:* Validates fix for duplicate Compose keys (`2604`), player progress bar safety, and route URL encoding.
   - *Static Conflict:* Contains tests (e.g. `testIssue4_VariantUIAndAnimeRestrictions`) that explicitly test in-page variant toggles (`resolveEffectiveMode`), which are now obsolete and slated for removal.
3. **`Phase09BDubbedAndMultiProviderTest.kt` & `Phase09BAuditVerificationTest.kt`:**
   - *Scope:* Tests multi-provider concurrent search in `SearchManagedExtensionsUseCase`, 24h source cache, and error mapping.
   - *Static Finding:* Correctly asserts that anime is strictly subtitle-only, but does not cover `PlaybackOrchestrator` or `UnifiedDownloadCoordinator` isolation.
4. **`SearchOrderAndEligibilityTest.kt`:**
   - *Scope:* Validates `ExtensionEligibilityFilter` content-type isolation and `AudioCapability.ARABIC_DUB` enforcement.
   - *Confirmation:* Proves that `ExtensionEligibilityFilter` is fully capable of filtering by `requiredAudioCapability`; the defect is that `PlaybackOrchestrator` fails to pass the parameter!

### B. Reconciliation with Historical Phase Reports

- **Phase 09B-B Report:** Stated that *only* `"دبلجة"` triggers dubbed search and claimed words like `"مدبلج"` should not.
  *Reconciliation:* **SUPERSEDED.** Phase 10B properly expanded the trigger vocabulary to include `"مدبلج"`, `"مدبلجة"`, and `"دبلجة"`.
- **Phase 09C Report:** Implemented in-page `FilterChip` selectors, `showVariantSheet` in the player, and clickable badges.
  *Reconciliation:* **SUPERSEDED & ABANDONED.** The user has mandated two completely distinct experiences (`STANDARD` vs `ARABIC_DUBBED`) with zero in-page toggling.
- **Phase 10B Report:** Documented `MediaAnimationClassifier` and TMDB identity preservation.
  *Reconciliation:* **RETAINED IN PRINCIPLE, DEFECTIVE IN REPOSITORY.** The classifier rules are sound, but `TmdbMediaRepositoryImpl.searchMulti` omitted the necessary fields (`originalLanguage`, `country`, `originalTitle`) in the `Movie` mapping, crippling real-world execution.

---

## 7. Prioritized Remediation Plan (Phase 11 Blueprint)

The following 10-step sequential remediation plan is structured by strict architectural dependency order. **No code modifications were performed during this audit phase.**

### Step 1: Query Normalization & Whitespace Safety
- **Target Files:** `SearchViewModel.kt`, `SearchTriggerParser.kt`.
- **Modifications:**
  - In `SearchViewModel.onQueryChange`, normalize whitespace (`.trim().replace(Regex("\\s+"), " ")`) before evaluating `queryFlow` or updating state.
  - In `performSearch`, evaluate state cancellation against the *normalized* query to prevent space-bar input from aborting pending requests.
- **Acceptance Criteria:** Typing `"Moana مدبلج "` yields identical results to `"Moana مدبلج"` without cancelling in-flight coroutines.

### Step 2: Cross-Lingual TMDB Resolution & Arabic Alias Matching
- **Target Files:** `TmdbApiService.kt`, `TmdbMediaRepositoryImpl.kt`, `SearchViewModel.kt`.
- **Modifications:**
  - Add optional `@Query("language") language: String? = null` to `TmdbApiService.searchMulti`.
  - Pass the active user language code (`"ar-SA"` or `"en-US"`) to `searchMulti`.
  - In `TmdbMediaRepositoryImpl.searchMulti`, map `originalLanguage`, `originCountry`, and `originalTitle` into `Movie`.
  - In `SearchViewModel.matchesMovie`, match scraper titles against both the localized `movie.title` and `movie.originalTitle`.
- **Acceptance Criteria:** An Arabic scraper title like `"فيلم موانا مدبلج"` matches TMDB entry `"Moana"` and enriches it with provider candidates.

### Step 3: Scraper Dubbed Result Filtering
- **Target Files:** `SearchManagedExtensionsUseCase.kt`, `EgyDeadScraper.kt`, `QfilmScraper.kt`.
- **Modifications:**
  - In `SearchManagedExtensionsUseCase.executeArabicDubbedSearch`, filter incoming scraper items to verify that the item's title or URL contains explicit dubbed markers (`مدبلج`, `دبلجة`, `dubbed`) before accepting it.
- **Acceptance Criteria:** Subtitled posts returned by multi-category scrapers during a dubbed search are discarded, preventing subtitled streams from masquerading as dubbed.

### Step 4: Separation of Details Screens & Navigation Context
- **Target Files:** `Screen.kt`, `AppNavigation.kt`, `SearchScreen.kt`, `SearchBarDropdown.kt`.
- **Modifications:**
  - Standardize navigation routes: `movie_details/{movieId}?autoPlay={autoPlay}&experienceMode={experienceMode}` where `experienceMode` is strictly `"STANDARD"` or `"ARABIC_DUBBED"`.
  - Update `SearchBarDropdown.kt` to forward `experienceMode` matching full search.
  - In `MovieDetailsViewModel` and `SeriesDetailsViewModel`, partition memory and disk cache keys by `"${movieId}_${experienceMode}"`.
- **Acceptance Criteria:** Opening a work in `ARABIC_DUBBED` creates an isolated session that does not collide with or read from `STANDARD` cache.

### Step 5: Removal of All Variant Switching Affordances
- **Target Files:** `DetailsScreens.kt`, `InlineDetailVideoPlayer.kt`, `PlayerScreen.kt`, `PlayerViewModel.kt`.
- **Modifications:**
  - In `DetailsScreens.kt`: Remove `FilterChip` components for subtitled/dubbed; remove `switch_to_subtitled_action` from the unavailable banner.
  - In `InlineDetailVideoPlayer.kt`: Make the variant badge non-clickable and remove `onToggleVariant`.
  - In `PlayerScreen.kt`: Remove `showVariantSheet` BottomSheet and the bottom bar `Translate` icon action.
  - In `PlayerViewModel.kt`: Remove `selectVariantMode`.
- **Acceptance Criteria:** Zero buttons, chips, or dialogs exist in details or playback to switch variants. The only way to watch subtitled is returning to search and selecting the standard result.

### Step 6: Strict Isolation of Dubbed Extensions in Playback & Discovery
- **Target Files:** `PlaybackOrchestrator.kt`, `PlaybackSession.kt`, `DiscoverManagedServersUseCase.kt`.
- **Modifications:**
  - Add `variantMode: String` and `audioLanguage: String` to `PlaybackSession`.
  - In `PlaybackOrchestrator.orchestratePlayback`: When `session.variantMode == "dubbed"`, retrieve candidates strictly via `searchOrderRepository.getDubbedOrder(VariantLanguage.ARABIC)` and pass `requiredAudioCapability = AudioCapability.ARABIC_DUB` to `ExtensionEligibilityFilter`.
  - Strictly prohibit fallback to subtitle providers.
- **Acceptance Criteria:** In `ARABIC_DUBBED`, only `egydead` and `qfilm` (or configured dubbed providers) are queried for servers and streams.

### Step 7: Complete Download Flow Isolation
- **Target Files:** `ExtractionModels.kt`, `UnifiedDownloadCoordinator.kt`, `DownloadItem.kt`.
- **Modifications:**
  - Add `variantMode: String` and `audioLanguage: String` to `DownloadSource`.
  - In `UnifiedDownloadCoordinator.download`, forward `source.variantMode` and `source.audioLanguage` to `DownloadItem`.
- **Acceptance Criteria:** Offline downloads accurately register whether the downloaded file is dubbed or subtitled.

### Step 8: Accurate Terminal Error & Not-Found Handling
- **Target Files:** `DetailsScreens.kt`, `PlaybackOrchestrator.kt`.
- **Modifications:**
  - When all dubbed providers report `NOT_FOUND`, display a clean message: "النسخة المدبلجة غير متوفرة لهذا العمل".
  - Do not auto-switch to subtitled. Do not show a switch button.
- **Acceptance Criteria:** Terminal failure maintains user intent; back-press returns cleanly to search.

### Step 9: Fix Anime Movie Classification in TMDB Repository
- **Target Files:** `TmdbMediaRepositoryImpl.kt`.
- **Modifications:**
  - Ensure all DTO-to-Domain mappers (`toDomain`, `toDomainDetails`, `searchMulti`) forward `originalLanguage`, `originCountry`, and `originalTitle`.
- **Acceptance Criteria:** Anime movies (*Spirited Away*, *Demon Slayer*) have `language = "ja"` and are classified as anime, while Western films (*Moana*) have `language = "en"` and are classified as Western animation.

### Step 10: Fix Genre Localization & Cache Partitioning
- **Target Files:** `TmdbMediaRepositoryImpl.kt`, `DetailsScreens.kt`, `SettingsScreen.kt`.
- **Modifications:**
  - Partition all cache keys in `TmdbMediaRepositoryImpl` by the current language tag (`"${cacheKey}_${currentLang}"`).
  - In `DetailsScreens.kt`, dynamically re-resolve `movie.genreIds` via `TmdbGenreHelper.getMovieGenreNames(movie.genreIds)` instead of printing static `movie.genres` strings.
  - In `SettingsScreen.kt`, clear disk caches on language change before restarting.
- **Acceptance Criteria:** Setting the app to English immediately renders all genres in English across search, home, and details screens.

---

## 8. Final Acceptance & Sign-off

```text
================================================================================
PHASE 10D AUDIT COMPLETION SUMMARY:
- Primary Root Causes Traced & Proven with Physical Code Evidence: 14 Issues
- Files Statically Audited: 35 Files
- Tests Statically Evaluated: 6 Test Suites
- Physical Code Modifications: 0 (Strict Static Audit)
- Gradle / Build / Test Commands Executed: 0
- Single /PHASES/ Directory Invariant: Strictly Maintained

STATUS: STATIC_AUDIT_COMPLETE
================================================================================
```

---

## 9. Comprehensive Ready-to-Execute Phase 11 Remediation Prompt
### برومبت الإصلاح اللاحق الشامل الجاهز للتنفيذ

```markdown
# PHASE 11 — CINESTREAM DUBBED SEARCH REMEDIATION & INDEPENDENT DETAILS SEPARATION

## 1. SCOPE AND ARCHITECTURAL INVARIANTS

Target project: CineStream User App.
Package Identity & Application ID:
- applicationId: `com.aistudio.cinestream.xyzabc` (MANDATORY: DO NOT MODIFY)
- namespace: `com.example`
- app_name: `CineStream Pro` in strings.xml and metadata.json
- Canonical documentation directory: `/PHASES/` (Do not create `/docs/phases/` or `/app/PHASES/`)

Reference Forensic Audit: `/PHASES/PHASE_10D_STATIC_FORENSIC_AUDIT_DUBBED_SEARCH_AND_REMEDIATION_PLAN.md`

## 2. CORE REMEDIATION TASKS (10 STEPS)

Execute the following 10 implementation steps strictly according to the findings of Phase 10D:

### Step 1: Query Normalization & Trailing Whitespace Cancellation Prevention
- In `SearchViewModel.kt` (`performSearch` & `onQueryChange`):
  - Normalize queries by trimming leading/trailing spaces and collapsing multiple spaces: `val normalized = query.trim().replace(Regex("\\s+"), " ")`.
  - Do not abort or cancel in-flight search jobs if the difference between `_uiState.value.query` and `query` is merely trailing or multiple whitespace.
  - Retain the raw query in UI text state so user cursor is uninterrupted, but dispatch search operations strictly against `normalized`.
- In `SearchTriggerParser.kt`:
  - Ensure queries like `"Moana مدبلج "` parse identically to `"Moana مدبلج"` (`cleanQuery = "Moana"`, `isArabicDubbed = true`).

### Step 2: Cross-Lingual TMDB Resolution & Arabic Alias Matching
- In `TmdbApiService.kt`:
  - Add optional `@Query("language") language: String? = null` parameter to `searchMulti`.
- In `TmdbMediaRepositoryImpl.kt`:
  - Pass the active application language tag (e.g. `"ar-SA"`) to `searchMulti`.
  - In DTO-to-Domain mapping for `Movie` and `Series`, forward `originalLanguage`, `originCountry`, and `originalTitle`.
- In `SearchViewModel.kt` (`matchesMovie` & `matchesSeries`):
  - Expand matcher logic so that Arabic scraper titles (e.g. `"فيلم موانا مدبلج"`) match TMDB canonical entries with English titles (`"Moana"`) using normalized transliteration, original title comparison, and clean query matching.
  - Enrich canonical TMDB items with scraper source candidates instead of dumping them into unlinked standalone items.

### Step 3: Multi-Category Scraper Dubbed Result Filtering
- In `SearchManagedExtensionsUseCase.kt` (`executeArabicDubbedSearch`):
  - When searching with `isArabicDubbedIntent == true`, inspect every post/item returned by scrapers (`EgyDeadScraper`, `QfilmScraper`).
  - Discard posts whose title does NOT contain explicit dubbed indicators (`مدبلج`, `مدبلجة`, `دبلجة`, `dubbed`).
  - Prevent subtitled posts from being assigned `MediaVariantMode.DUBBED`.

### Step 4: Separation of Details Screens & Navigation Context
- In `Screen.kt` & `AppNavigation.kt`:
  - Formalize route structure: `movie_details/{movieId}?autoPlay={autoPlay}&experienceMode={experienceMode}`.
  - Strongly type `experienceMode` as `"STANDARD"` or `"ARABIC_DUBBED"`.
  - In `SearchBarDropdown.kt`: Forward `experienceMode` based on whether search was triggered with dubbed intent.
- In `MovieDetailsViewModel.kt` & `SeriesDetailsViewModel.kt`:
  - Partition in-memory state cache and disk cache keys by `"${mediaId}_${experienceMode}"` to eliminate cross-talk between standard and dubbed view sessions.

### Step 5: Complete Elimination of Variant Switching Affordances
- In `DetailsScreens.kt`:
  - Remove all `FilterChip` components for toggling between "مترجم" and "مدبلج".
  - In the "dubbed unavailable" banner, remove the `switch_to_subtitled_action` button. Display a pure informational message: "النسخة المدبلجة غير متوفرة لهذا العمل".
- In `InlineDetailVideoPlayer.kt`:
  - Convert the variant badge in the player top bar into a purely static, non-clickable badge (`Modifier.clickable` removed, `onToggleVariant` callback removed).
- In `PlayerScreen.kt`:
  - Remove `showVariantSheet` BottomSheet modal.
  - Remove the translation/variant icon button from player bottom action bar.
- In `PlayerViewModel.kt`:
  - Remove `selectVariantMode` and remove mode toggling mutations.

### Step 6: Strict Isolation of Dubbed Extensions in Playback & Discovery
- In `PlaybackSession.kt`:
  - Include `variantMode: String` and `audioLanguage: String`.
- In `PlaybackOrchestrator.kt` (`orchestratePlayback`):
  - When `session.variantMode == "dubbed"`, resolve extension order strictly from `searchOrderRepository.getDubbedOrder(VariantLanguage.ARABIC)`.
  - Pass `requiredAudioCapability = AudioCapability.ARABIC_DUB` to `ExtensionEligibilityFilter.filterEligibleExtensions`.
  - Prohibit fallback to subtitle providers when all dubbed extensions fail.
- In `DiscoverManagedServersUseCase.kt`:
  - Pass `requiredAudioCapability = AudioCapability.ARABIC_DUB` when server discovery is executed for dubbed content.

### Step 7: Download Pipeline Isolation & Audio Tracking
- In `ExtractionModels.kt` (`DownloadSource`):
  - Add `variantMode: String = "subtitled"` and `audioLanguage: String = "ar"`.
- In `UnifiedDownloadCoordinator.kt`:
  - Forward `source.variantMode` and `source.audioLanguage` into `DownloadItem`.
  - Ensure offline library correctly identifies and tags dubbed downloads.

### Step 8: Accurate Terminal Error & Not-Found Handling
- In `DetailsScreens.kt` & `PlaybackOrchestrator.kt`:
  - When all eligible dubbed extensions return empty or 404, set status cleanly to `CoverageStatus.NOT_FOUND`.
  - Display dedicated Arabic message: "النسخة المدبلجة غير متوفرة لهذا العمل".
  - Do NOT auto-switch to subtitled. Do NOT display recovery buttons that redirect to subtitled streams.

### Step 9: Fix Anime vs Western Animation TMDB Mapping
- In `TmdbMediaRepositoryImpl.kt`:
  - Ensure all `Movie` and `Series` objects constructed from TMDB responses accurately receive `originalLanguage = item.originalLanguage`, `country = item.originCountry?.firstOrNull()`, and `originalTitle = item.originalTitle`.
  - Verify that `MediaAnimationClassifier.isAnime()` accurately flags Japanese productions (`ja`, `JP`) as anime (subtitle-only) while Western animation (Disney, Pixar, DreamWorks) remains eligible for Arabic dubbing.

### Step 10: Dynamic Genre Localization & Cache Partitioning
- In `TmdbMediaRepositoryImpl.kt`:
  - Partition all media list and genre cache keys by current application language tag (`"${cacheKey}_${currentLang}"`).
- In `DetailsScreens.kt`:
  - Dynamically re-resolve genre names using `TmdbGenreHelper.getMovieGenreNames(movie.genreIds)` instead of printing static `movie.genres` string lists.
- In `SettingsScreen.kt`:
  - When language is changed, invalidate persistent memory and disk caches before restarting the application.

## 3. VERIFICATION AND REPORTING

1. Run `compile_applet` to confirm zero compilation errors.
2. Create unit regression test `app/src/test/java/com/example/Phase11DubbedSearchRemediationTest.kt` verifying:
   - Trailing space search preservation.
   - Cross-lingual title matching.
   - Dubbed extension isolation.
   - Zero variant toggles in UI states.
3. Write comprehensive completion report at `/PHASES/PHASE_11_DUBBED_SEARCH_REMEDIATION_AND_DETAILS_SEPARATION_REPORT.md`.
4. Update `/PHASES/README.md`.
```

