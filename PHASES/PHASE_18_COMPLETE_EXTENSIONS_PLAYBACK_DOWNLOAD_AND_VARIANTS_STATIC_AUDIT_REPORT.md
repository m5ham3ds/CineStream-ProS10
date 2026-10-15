# CINESTREAM USER APP — PHASE 18
# COMPLETE READ-ONLY STATIC FORENSIC AUDIT REPORT:
# EXTENSIONS, SCRAPERS, PLAYBACK ENGINES, DOWNLOAD PIPELINES, AND MEDIA VARIANT FLOWS

**Execution Mode:** STRICT READ-ONLY FORENSIC AUDIT (No modifications to project source, no tests executed, no build, no emulator/network calls)  
**Audit Target:** CineStream User App Working Tree (`CineStream-ProS1-main`)  
**Package / App ID in Build:** `com.aistudio.cinestream.xyzabc` (Verified in `app/build.gradle.kts` and `app/google-services.json`)  
**Namespace:** `com.example`  
**Timestamp:** 2026-10-13  
**Phase:** 18  
**Audit Verdict:** **PASS WITH CRITICAL ARCHITECTURAL FINDINGS** (Codebase is structurally unified under Managed Extension Runtime and Orchestrators, but 6 specific architectural disconnects in variant persistence, download ID matching, and player cache keys were statically uncovered).

---

## 1. ملخص تنفيذي (Executive Summary)

تم تنفيذ تدقيق جنائي برمجي ساكن وشامل (Static Forensic Audit) لكافة مكونات مسارات الإضافات (Extensions)، والكاشطات البرمجية (Scrapers)، ومحركات التشغيل المضمنة والكاملة (Playback Engines)، وخطوط أنابيب التنزيل (Download Pipelines)، ونظام إدارة الأنماط الصوتية والمرئية (Media Variants: Dubbed, Subtitled, Anime).

التدقيق تم بصيغة **القراءة والفحص الثابت فقط (Strict Read-Only)** دون إجراء أي تعديل على كود التطبيق، ودون تشغيل مهام بناء Gradle أو اختبارات أو محاكيات أو إرسال طلبات scraping عبر الشبكة.

### أهم النتائج الإيجابية المحققة في الكود الحالي:
1. **هندسة الإضافات المدارة الموحدة (Managed Extension Runtime):** تم التحقق من وجود طبقة موحدة مركزية (`ManagedMediaOrchestrator`, `ManagedExtensionRegistry`, `ScraperRegistry`) تعزل مشغل الفيديو ونظام التنزيل والواجهات عزلاً تاماً عن تفاصيل الـ scraping و JavaScript و WebViews.
2. **منع الأكواد الديناميكية والتنفيذ غير الآمن (Zero Dynamic Loading / Reflection):** جميع الكاشطات الخمس (`qfilm`, `egydead`, `witanime`, `anime4up`, `animeblkom`) مسجلة استاتيكياً ومضمنة بالكامل داخل التطبيق عبر `ScraperRegistry`. لا يوجد أي تحميل ديناميكي لملفات dex أو jar أو أصناف خارجية عبر الشبكة.
3. **أمان تدفق التنزيلات ومقاومة هجمات المسار (Path Traversal Hardening):** فئة `MediaStorageUtils` تطبق تحققاً صارماً من الاحتواء (`verifyContained`) وتطهيراً دقيقاً لمعرفات الملفات (`sanitizeSegment`) لمنع تجاوز مسار المجلد الداخلي للتطبيق (`filesDir/media`).
4. **عزل تحديات Cloudflare و Turnstile:** فئة `InteractiveChallengeController` ومكون `InteractiveChallengeWebView` يوفران معالجة دقيقة مقيدة بمساحة مشغل الفيديو (16:9 أو كامل الشاشة) دون حجب بقية عناصر الشاشة، مع تخزين آمن لملفات تعريف الارتباط عبر `PerSiteSessionStore`.
5. **انتشار عناوين الدبلجة وتفضيل المزودين (Phase 17 Verification):** فئة `MediaDisplayTitleResolver` نجحت في فك الارتباط بين الهوية المرجعية لـ TMDB (`tmdbId`) وعنوان العرض المكتشف من المزود (`displayTitle`) في كل من نتائج البحث السريع (`SearchBarDropdown`) وشاشة البحث الكاملة (`SearchScreen`).

### الثغرات والانقطاعات المعمارية المكتشفة (Critical Disconnects):
1. **[خلل جوهري] تصادم معرف المفضلة عند إضافة نفس العمل بنمطين مختلفين:** في `LibraryItem.kt` و `LibraryIdentity.kt`، المفتاح الأساسي في جدول Room هو `libraryId = "${contentType}_${tmdbId}"`. هذا المعرف لا يتضمن نمط النسخة (`variantMode`). في حال قام المستخدم بإضافة النسخة المترجمة والنسخة المدبلجة لنفس الفيلم إلى المفضلة، ستقوم إحداهما باستبدال الأخرى فوراً في جدول `library_items` وفي وثائق Firestore.
2. **[خلل جوهري] عدم تطابق معرف حلقة المسلسل في إيقاف/استئناف/إلغاء التنزيل:** في `DetailsScreens.kt` (السطر 1839)، يُرسل الأمر لخدمة `StreamDownloaderService` بمعرف `"${series.id}_${episode.id}"`. بينما يقوم منسق التنزيلات `UnifiedDownloadCoordinator` بتخزين التنزيل في جدول Room بمعرف مركب مشتق من `MediaStorageUtils.buildDownloadId(...)` بصيغة `"${series.id}_s${season}e${episode}_${mode}_${lang}"`. عند استدعاء `db.downloadDao().getItemById(fileId)` داخل الخدمة، تفشل العملية بصمت لأن المعرفين غير متطابقين.
3. **[خلل جوهري] سقوط نمط النسخة (Variant Mode) في شاشة المشغل الكامل:** في `PlayerViewModel.kt` (السطور 727-736)، عند استدعاء `managedOrchestrator.startPlaybackSession(...)`، لا يتم تمرير `variantMode` أو `audioLanguage`، وبالتالي يتم استخدام القيم الافتراضية (`subtitled` و `ar`). هذا يؤدي إلى تشغيل النسخة المترجمة عند التكبير حتى لو كان المستخدم قد بدأ المشاهدة كنسخة مدبلجة.
4. **[خلل جوهري] مفتاح كاش الدقة المكتملة في `ManagedMediaOrchestrator` يفتقر لنمط النسخة:** في `ManagedMediaOrchestrator.kt` (السطر 178)، يُبنى مفتاح `mediaIdentity` بصيغة `"$mediaId:${if (contentType.isMovie) "movie" else "series"}:$season:$episode"` دون تضمين `variantMode`، مما يسبب تداخلاً بين النسخة المدبلجة والمترجمة في كاش الدقة (`PlaybackResolutionCache`).
5. **[خلل تكاملي] فتح التحميلات المكتملة يسقط نمط التجربة:** في `AppNavigation.kt` (السطور 1199-1205)، عند النقر على عنصر تم تنزيله في `DownloadsScreen`، يتم التنقل إلى شاشة التفاصيل بمعامل `autoPlay = true` ولكن بدون تمرير `experienceMode = item.variantMode`، فيتم فتح العمل بنمط `STANDARD` دائماً.
6. **[عدم اتساق عزل الأنمي] كاشطات الأنمي تصرح بدعم الأفلام والمسلسلات العامة:** في `WitanimeScraper.kt` و `Anime4UpScraper.kt` و `AnimeBlkomScraper.kt`، تصرح الكاشطات بدعم `ContentType.MOVIE` و `ContentType.SERIES` في كودها الثابت وفي الكتالوج الافتراضي، مما يسمح لها نظرياً بالدخول في مرشح الأهلية لغير الأنمي ما لم يقم الأدمن بتقييدها عبر ترتيب البحث السحابي.

---

## 2. جرد البنية البرمجية لمنظومة الإضافات والكاشطات (Extensions & Scrapers Inventory)

يتضمن تطبيق CineStream 5 كاشطات مضمنة ثابتة داخل حزمة `com.example.extension.managed.scraper`:

| اسم الكاشطة | المفتاح (Key) | الرابط الأساسي (Base URL) | الأنواع المدعومة بالكود | القدرات المصرح بها | المحركات المستخدمة |
|---|---|---|---|---|---|
| **QfilmScraper** | `qfilm` | `https://a.qfilm.tv` | `MOVIE`, `ANIME` | SEARCH, DETAILS, SERVER_DISCOVERY, VIDEO_EXTRACTION | Jsoup, Regex, SafeScraperBridge, WebExtractionEngine |
| **EgyDeadScraper** | `egydead` | `https://tv10.egydead.live` | `MOVIE`, `SERIES`, `ANIME` | SEARCH, DETAILS, EPISODES, SERVER_DISCOVERY, VIDEO_EXTRACTION | Jsoup (POST View=1), Headless WebView, StaticMediaExtractor |
| **WitanimeScraper** | `witanime` | `https://witanime.com` | `ANIME`, `SERIES`, `MOVIE` | SEARCH, DETAILS, EPISODES, SERVER_DISCOVERY, VIDEO_EXTRACTION | Jsoup, JSON `/sources`, 4shared embed regex |
| **Anime4UpScraper** | `anime4up` | `https://w1.anime4up.rest` | `ANIME`, `MOVIE`, `SERIES` | SEARCH, DETAILS, EPISODES, SERVER_DISCOVERY, VIDEO_EXTRACTION | Jsoup, `li[data-watch]`, Dynamic Embeds |
| **AnimeBlkomScraper** | `animeblkom` | `https://animeblkom.net` | `ANIME`, `MOVIE`, `SERIES` | SEARCH, DETAILS, EPISODES, SERVER_DISCOVERY, VIDEO_EXTRACTION | Jsoup, `/search?query=`, `[data-src]` iframes |

### 2.1 دورة حياة تسجيل وإدارة الإضافات:
* **التسجيل الثابت (`ScraperRegistry.kt`):** يحتوي على خريطة غير قابلة للتعديل تربط المفاتيح بالكائنات التنفيذية للكاشطات. لا يوجد استدعاء لـ `Class.forName` أو فحص للحزم وقت التشغيل.
* **كتالوج الإضافات المدارة (`ManagedExtensionRegistry.kt` / `ManagedExtensionRuntimeRegistry.kt`):**
  * يحتفظ بلقطة ثابتة `ExtensionRuntimeSnapshot` منشورة عبر `StateFlow`.
  * يتم تغذية السجل عند بدء التشغيل بنسخة LKG المخزنة محلياً في `SafeLocalMetadataCache` أو بالقيم الافتراضية المضمنة `BUNDLED_DEFAULT_EXTENSIONS`.
  * يستمع بشكل حي إلى تغييرات Firestore في `/managed_extensions` و `/config/search_order` عبر `ManagedExtensionRealtimeSyncManager`.
  * يرفض أي تحديث سحابي يحمل رقم مراجعة أقدم من الحالي (`Revision Protection`).
  * يحظر كلياً تنشيط أي إضافة حالتها `DISABLED` بواسطة المستخدم؛ سلطة الأدمن السحابية قطعية.

---

## 3. مسار البحث وتعدد المزودين وعزل الأنماط (Search & Multi-Provider Flow)

### 3.1 تدفق البحث الفعلي في الكود:

```
[مستخدم يكتب في شريط البحث: "دبلجة باتمان" أو "باتمان"]
                         │
                         ▼
             [SearchViewModel.onQueryChange]
                         │
                         ▼
        [SearchTriggerParser.parse(query)]
       (يتحقق من كلمة "دبلجة" ككلمة مستقلة فقط)
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
 [Intent: Regular Search]     [Intent: Arabic Dubbed Search]
         │                               │
         │ (توازي كامل)                  │ (توازي كامل)
         ├───────────────────────┐       ├────────────────────────┐
         ▼                       ▼       ▼                        ▼
 [TMDB multiSearch]     [Extension Search] [TMDB Catalog Search] [Arabic Dubbed Fan-Out]
 (نتائج فورية ومرجعية) (عبر Orchestrator) (نتائج TMDB للمطابقة)  (بحث في جميع المزودين المؤهلين)
         │                       │                                │
         │                       │                                ▼
         │                       │                 [SearchManagedExtensionsUseCase]
         │                       │                 (يعمل Fan-out لجميع المزودين غير المكتملين)
         │                       │                 (يحفظ المرشحين في SourceCoverageRepository)
         │                       │                                │
         └───────────────┬───────┘                                │
                         ▼                                        │
           [enrichMediaWithProviders] ◄───────────────────────────┘
           (يدمج مرشحي المزودين مع أعمال TMDB المرجعية)
                         │
                         ▼
       [MediaDisplayTitleResolver.resolveForMovie / Series]
       (يعرض العنوان العربي المكتشف للدبلجة، أو العنوان المرجعي للمترجم)
                         │
                         ▼
              [SearchUiState.movieResults / seriesResults]
```

### 3.2 فحص قواعد عزل الأنماط ومطابقة العناوين:
1. **قاعدة إطلاق بحث الدبلجة:** فئة `SearchTriggerParser` تفرض نمطاً صارماً: كلمة `دبلجة` وحدها ككلمة مستقلة هي ما يطلق عزم الدبلجة العربية. كلمات `مدبلج` أو `مدبلجة` أو `dubbed` لا تطلق عزم الدبلجة المنفصل، بل تعامل ككلمات بحث نصية عادية.
2. **عزل الأنمي عن الدبلجة:** في `SearchManagedExtensionsUseCase.kt` (السطور 60-65)، إذا كان `contentType == ContentType.ANIME` واحتوى الطلب على عزم دبلجة، يتم رفض الطلب فوراً برسالة خطأ صريحة: `"محتوى الأنمي مترجم فقط ولا تتوفر له دبلجة"`.
3. **التوازي وتجميع المرشحين (Fan-Out & Candidate Collection):** تم التحقق من أن الكود لا يتوقف عند أول مزود يعيد نتائج (كما كان في Phase 09A القديم)، بل يستعلم كافة المزودين المؤهلين بالتوازي عبر `deferredList = providersToQuery.map { async { runtime.search(...) } }` و `awaitAll()`، ويتم تجميع كافة المرشحين الصالحين في `ProviderSourceCandidate`.

---

## 4. مسار شاشة التفاصيل والتشغيل المضمن (Details & Playback Architecture)

### 4.1 عزل الذاكرة المؤقتة حسب نمط التجربة:
في `MovieDetailsViewModel` و `SeriesDetailsViewModel`، يتم تخزين الحالات وعزلها كلياً حسب المعامل `experienceMode`:
* المفتاح في كاش الشاشة: `"${movieId}_$experienceMode"` (مثال: `"1234_STANDARD"` مقابل `"1234_ARABIC_DUBBED"`).
* يُمنع صراحة السقوط في الكاش العادي عند وجود تجربة دبلجة (`experienceMode == "ARABIC_DUBBED"`).

### 4.2 تدفق التشغيل داخل المشغل المضمن (`InlineDetailVideoPlayer.kt`):
1. **التحقق من التنزيل المحلي:** إذا كان الفيلم أو الحلقة منزلة على الجهاز، يتم تشغيلها مباشرة عبر بروتوكول محلي:
   `url = "local_offline_file://$fileId"`
   ويقوم المشغل بقراءة الملف المحمي من مجلد `filesDir/media` باستخدام `MediaStorageUtils.findMediaFile`.
2. **الاستخراج التلقائي للبث الحي (`auto_extract://`):**
   * ينشئ جلسة تشغيل: `managedOrchestrator.startPlaybackSession(...)`.
   * يستدعي `managedOrchestrator.orchestratePlayback(...)` بمهلة إجمالية 120 ثانية.
   * بمجرد اكتشاف أول رابط صالح، يبدأ البث فوراً عبر `onFirstPlayableSource` دون انتظار بقية السيرفرات.
   * بالتوازي، يكتشف الجودات الإضافية عبر `onQualitiesDiscovered` ويحدث قائمة الجودات في `ServerStateStore`.
   * عند حدوث خطأ عابر في الشبكة (`PlaybackErrorClassifier.isTransientNetworkError`)، يستمر في محاولة التخزين المؤقت دون تدمير المشغل.

### 4.3 الكاش المعماري للجلسات في المشغل الكامل (`PlayerViewModel.kt`):
* المشغل الكامل يستقبل المعاملات عبر `SavedStateHandle` أو دوال التهيئة `initialize()`.
* **العيب المكتشف رقم 3:** في السطر 727 من `PlayerViewModel.kt`:
  ```kotlin
  val session = managedOrchestrator.startPlaybackSession(
      mediaId = state.mediaId,
      title = state.title,
      originalTitle = state.originalTitle,
      year = state.releaseYear,
      isMovie = state.isMovie,
      season = state.currentSeasonNumber,
      episode = state.currentEpisodeNumber,
      contentType = state.contentType
      // مفقود: variantMode = state.mode
      // مفقود: audioLanguage = state.audioLanguage
  )
  ```
  هذا الإسقاط يؤدي إلى بدء جلسة المشغل الكامل كـ `subtitled` بشكل افتراضي، مما يطلب ترتيب بحث الترجمة بدلاً من ترتيب بحث الدبلجة في المشغل الكامل.

---

## 5. خط أنابيب التنزيل والتخزين المحلي المفرغ (Download & Storage Pipeline)

### 5.1 مكونات دورة التنزيل:

```
[المستخدم ينقر على زر التحميل في تفاصيل العمل/الحلقة]
                         │
                         ▼
     [SmartDownloadQualityDialog / BatchDownloadSheet]
                         │ (يفحص القيود الأمنية UserSecurityManager)
                         │ (يفحص الكاش والجودات المتاحة)
                         ▼
        [UnifiedDownloadCoordinator.download()]
                         │ (يتحقق من الرابط ومنع النقرات المتكررة)
                         ▼
           [DownloaderHandoffAdapter]
           (يطهر الترويسات ويزيل كلياً ملفات تعريف الارتباط Cookies)
                         │
                         ▼
         [إدراج السجل في جدول download_items عبر Room]
                         │
                         ▼
         [تشغيل خدمة StreamDownloaderService في المقدمة]
                         │
         ┌───────────────┴───────────────┐
         ▼                               ▼
 [ملف مباشر .mp4 / .mkv]         [بث مجزأ .m3u8]
 تنزيل بقطع RandomAccessFile     تنزيل الأجزاء .ts بالتوازي (Semaphore=64)
                                 ودمجها في ملف فيديو نهائي
                         │
                         ▼
         [تخزين آمن في filesDir/media مع وجود .nomedia]
```

### 5.2 العيب المكتشف رقم 2 بالتفصيل (عدم تطابق معرفات التنزيل):
في `UnifiedDownloadCoordinator.kt` (السطور 215-219)، يتم بناء معرف التنزيل (`fileId` أو `taskId`) باستخدام:
```kotlin
val fileId = if (variantMode != null && audioLanguage != null) {
    MediaStorageUtils.buildDownloadId(mediaId, isMovie, seasonNumber, episodeNumber, variantMode, audioLanguage)
} else {
    if (isMovie) mediaId else "${mediaId}_${episodeId ?: "1_1"}"
}
```
فإذا كان المسلسل يحمل TMDB ID = `1399`، والحلقة 1 من الموسم 1، فإن `buildDownloadId` ينتج:
`"1399_s1e1_subtitled_ar"`
ويُحفظ في جدول `download_items` بهذا المفتاح الأساسي `id = "1399_s1e1_subtitled_ar"`.

بينما في `DetailsScreens.kt` (السطر 1839)، عند نقر المستخدم على زر الإيقاف أو الاستئناف للحلقة:
```kotlin
val intent = Intent(context, StreamDownloaderService::class.java).apply {
    action = if (downloadItem.isPaused) "RESUME" else "PAUSE"
    putExtra("id", "${series.id}_${episode.id}") // يرسل مثلاً "1399_1"
}
```
وعند وصول الأمر إلى `StreamDownloaderService.kt`:
```kotlin
val item = db.downloadDao().getItemById(fileId) // يبحث عن "1399_1" فلا يجد شيئاً!
```
النتيجة الحتمية في الكود الحالي: لا يستجيب التنزيل لأوامر الإيقاف والاستئناف من شاشة التفاصيل للحلقات بسبب عدم تطابق صيغة المعرف.

---

## 6. جدول مقارنة ادعاءات التقارير السابقة بالواقع الفعلي للكود (Discrepancy Matrix)

| الادعاء في تقارير المراحل السابقة | الحقيقة البرمجية المثبتة في الكود الحالي | الدليل من الكود (Source Evidence) |
|---|---|---|
| **Phase 06.5:** تم دمج مزودي VidApi كأحد كاشطات النظام في `ScraperRegistry`. | **غير صحيح.** لا يوجد أي أثر لكود أو فئة تدعى `VidApi` في كامل المشروع. الكاشطات المضمنة هي 5 فقط (`qfilm, egydead, witanime, anime4up, animeblkom`). | فحص `ScraperRegistry.kt` ونتائج البحث الشامل في مساحة العمل. |
| **Phase 09A:** إضافات الأنمي معزولة حصرياً للأنمي ولا يمكن ترشيحها للأفلام والمسلسلات. | **صحيح وظيفياً في الواجهة، ولكنه غير مقيد في الكاشطة نفسها.** كاشطات `witanime` و `anime4up` و `animeblkom` تصرح بدعم `ContentType.MOVIE` و `ContentType.SERIES` في كودها. العزل يتم في الواجهة وترتيب البحث فقط. | السطور 42-45 في `WitanimeScraper.kt` والسطور 38-42 في `Anime4UpScraper.kt`. |
| **Phase 10B:** الاحتفاظ الكامل بهوية TMDB المرجعية كمعرف وحيد للتفاصيل. | **صحيح ومثبت.** شاشات التفاصيل تعتمد كلياً على `tmdbId` الرقمي وتستخدم كاش TMDB، ومطابقة العناوين تتم عبر `MediaTitleMatcher`. | `MovieDetailsViewModel.kt` و `SeriesDetailsViewModel.kt`. |
| **Phase 16:** ترتيب بحث الدبلجة محكوم بسلطة الأدمن السحابية ولا يعاد ترتيبه محلياً. | **صحيح ومثبت.** فئة `SearchOrderRepository.getDubbedOrder()` تقرأ مصفوفة `arabicDubbed` وتحافظ على ترتيبها الصارم دون إعادة فرز بالأولويات المحلية. | `SearchOrderRepository.kt` و `ExtensionEligibilityFilter.kt`. |
| **Phase 17:** تم إصلاح حفظ المفضلة للنسخ المدبلجة ونقل عناوين العرض لشاشة البحث. | **صحيح، مع وجود ثغرة تصادم المفتاح.** تم إضافة أعمدة `variantMode` و `providerId` لقاعدة البيانات، وعناوين الدبلجة تظهر بنجاح عبر `MediaDisplayTitleResolver`، ولكن المفتاح الأساسي `libraryId` لا يميز بين النسخة المدبلجة والمترجمة لنفس العمل. | `LibraryItem.kt` السطور 8-19، و `AppDatabase.kt` (Migration 10_11). |

---

## 7. الجرد الأمني الشامل وإدارة الشهادات وحماية المسار (Security & Compliance)

1. **إدارة الشهادات ومعرف الحزمة:**
   * تم التحقق من أن معرف التطبيق في ملف `app/build.gradle.kts` هو:
     `applicationId = "com.aistudio.cinestream.xyzabc"`
   * تم التحقق من تطابقه الكامل في `app/google-services.json`:
     `"package_name": "com.aistudio.cinestream.xyzabc"`
   * إعدادات التوقيع (`signingConfigs`) وملفات `debug.keystore` سليمة وغير ممسوسة.
2. **بروتوكولات الأمان في الـ WebView:**
   * فئة `ControlledWebViewEngine`:
     * ترفض كلياً الروابط غير المشفرة `http://` وتلزم استخدام `https://`.
     * تفحص النطاقات المستهدفة عبر `TrustedEmbedHostPolicy.isNavigationAllowed` وتمنع التنقل إلى الشبكات الخاصة أو المحلية (`localhost`, `127.0.0.1`, `192.168.x.x`).
     * تفكك وتلغي الـ WebView بالكامل عند إلغاء العملية (`cleanup()` تستدعي `destroy()` وتزيل الـ JavascriptInterfaces).
3. **أمان ترويسات التنزيل:**
   * فئة `DownloaderHandoffAdapter` تجرد وتستبعد بشكل صريح أي ترويسات تتضمن `Cookie` أو `Set-Cookie` قبل تسليم طلب التنزيل لـ `AndroidDownloader` أو تخزينه في قاعدة بيانات Room.

---

## 8. خلاصة التدقيق والتوصيات للمراحل القادمة (Audit Conclusions & Roadmap)

### الخلاصة الفنية:
منظومة CineStream في وضعها الحالي تتمتع ببنية معمارية قوية ومتطورة في طبقة الإضافات المدارة، مع تطبيق ممتاز لمبادئ عدم تسريب الجلسات وعدم التحميل الديناميكي، وتحقيق فصل نظيف بين بيانات TMDB المرجعية ومصادر المزودين المكتشفة.

### مصفوفة الإصلاحات الهندسية المطلوبة مستقبلاً (Phase 19 Remediation Target):
1. **تصحيح معرف المفضلة المركب:** تعديل `LibraryIdentity.createLibraryId` ليشمل نمط النسخة (مثال: `"${contentType}_${tmdbId}_${variantMode}"`) مع إضافة هجرة غير مدمرة (Migration 11_12) في Room لتمكين المستخدم من حفظ النسختين المترجمة والمدبلجة لنفس العمل دون استبدال إحداهما بالأخرى.
2. **توحيد معرفات مهام التنزيل للحلقات:** توحيد صياغة المعرف المرسل في `DetailsScreens.kt` للحلقات ليتطابق مع المعرف المنشأ في `UnifiedDownloadCoordinator` عبر استخدام دالة موحدة مثل `MediaStorageUtils.buildDownloadId(...)`.
3. **تمرير معاملات النمط في المشغل الكامل:** تعديل استدعاء `startPlaybackSession` في `PlayerViewModel.kt` ليمرر `variantMode = state.mode` و `audioLanguage = state.audioLanguage`.
4. **تضمين نمط النسخة في كاش دقة المشغل المكتملة:** تعديل دالة `cachePlaybackResolution` في `ManagedMediaOrchestrator.kt` لتمرير `mode` و `language` وبناء `mediaIdentity` مميز للنسخ المدبلجة.
5. **تمرير نمط النسخة عند فتح المحتوى المنزّل:** تعديل استدعاء `onItemClick` في `DownloadsScreen.kt` و `AppNavigation.kt` ليمرر `experienceMode = item.variantMode`.

---
**نهاية تقرير التدقيق الجنائي الساكن — المرحلة 18 (PHASE 18 STATIC AUDIT COMPLETE)**
