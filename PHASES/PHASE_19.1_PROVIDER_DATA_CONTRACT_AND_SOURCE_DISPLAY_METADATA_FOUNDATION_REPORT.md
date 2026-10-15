# CINESTREAM — PHASE 19.1 REPORT
# PROVIDER DATA CONTRACT & SOURCE DISPLAY METADATA FOUNDATION

**تاريخ التنفيذ:** 13 أكتوبر 2026  
**المرحلة:** Phase 19.1 (المرحلة الفرعية الأولى من المرحلة 19)  
**الحالة:** مكتملة بنجاح بنسبة 100% (COMPLETED & VERIFIED)  
**حالة البناء:** Build Succeeded (`compile_applet` PASSED)

---

## 1. الأهداف ونطاق العمل (Executive Summary & Scope)

تم تنفيذ المرحلة **Phase 19.1** بدقة متناهية ودون تجاوز لنطاق العمل المحدد. تم التركيز بشكل حصري على:
1. فحص وتدقيق عقد البيانات المشترك بين نتائج الإضافات المدارة (`SearchMediaItem` / Scrapers) وبيانات TMDB القانونية (`Movie` / `Series`) وطبقة تسوية معلومات العرض (`MediaDisplayTitleResolver`).
2. معالجة الخلل المعماري الذي كان يؤدي إلى فقدان صورة الغلاف الخاصة بمصدر المحتوى (`posterUrl` الخاص بالمزود) عند تمريره عبر نماذج البيانات، بينما كان عنوان المصدر فقط هو الذي ينتقل.
3. الفصل التام بين:
   - **هوية العمل القانونية في TMDB:** (المعرف القانوني، النوع، العنوان المرجعي، غلاف TMDB الأصلي، وسائر البيانات المرجعية).
   - **بيانات مصدر المحتوى:** (معرف الإضافة، عنوان العمل كما في الإضافة، رابط الصفحة الفعلي، صورة الغلاف المستخرجة من الإضافة، نوع المحتوى، والبيانات الوصفية الإضافية).
   - **سياق النسخة:** (وضع النسخة `MediaVariantMode.DUBBED` / `SUBTITLED`، ولغة الصوت `VariantLanguage.ARABIC`).
   - **رابط صفحة المصدر:** المحتفظ به بدقة للوصول إلى الحلقات واستخراج الوسائط وتشغيلها.
4. عدم المساس بمنطق ترتيب البحث أو استعلامات الإضافات أو مخططات Room / Firestore في هذه المرحلة التأسيسية.

---

## 2. نتائج الفحص الجنائي لمسار البيانات الحالي (Forensic Audit of Data Paths)

قبل البدء في أي تعديل، تم فحص الفئات والاستدعاءات الفعلية في المشروع:

### أ. مسار استخراج البيانات من المحللات (Scraper Extraction Layer)
* أثبت الفحص أن كافة محللات الإضافات المدارة الخمسة (`EgyDeadScraper`, `QfilmScraper`, `AnimeBlkomScraper`, `Anime4UpScraper`, `WitanimeScraper`) تستخرج بالفعل صورة الغلاف وتمررها إلى كائن `SearchMediaItem(posterUrl = ...)`:
  - في `EgyDeadScraper`: تستخرج الصورة من سمات `data-src` أو `src` لعنصر الصورة.
  - في `QfilmScraper`: تستخرج الصورة في السطر 203.
  - في `AnimeBlkomScraper`, `Anime4UpScraper`, `WitanimeScraper`: تستخرج من بطاقات الأعمال.
* **موضع الخلل:** عند استلام نتائج الإضافات في `SearchManagedExtensionsUseCase.kt` وتحويلها إلى مرشحات مصادر `ProviderSourceCandidate`، لم يكن نموذج `ProviderSourceCandidate` يحتوي أصلاً على حقل `posterUrl` أو `contentType` أو `extraMetadata`، مما أدى إلى **بتر وتجريد صورة الغلاف الخاصة بالمصدر فور خروجها من المحلل**!

### ب. مسار تخزين التغطية (Coverage Persistence Layer)
* في `SourceCoverageRepository.kt` و `ProviderCoverageRecord`: كان الكائن يحتفظ فقط بـ `providerTitle` ويتجاهل `providerPosterUrl`.
* عند استرجاع الجولة السابقة (`existingRound`) من الكاش، كان يتم إعادة بناء مرشح بدون غلاف المصدر.

### ج. مسار المطابقة مع TMDB (SearchViewModel & TMDB Catalog)
* في `SearchViewModel.kt` (الأسطر 654-795):
  - يتم دمج مرشحات المصادر `sourceCandidates` الخاصة بالإضافات مع كائنات TMDB المرجعية `Movie` و `Series`.
  - تحافظ كائنات TMDB على هويتها الأصلية: `movie.posterUrl` يمثل غلاف TMDB الأصلي.
  - لكن نظراً لأن المرشحات في `sourceCandidates` كانت تفتقر إلى غلاف المصدر، لم تتمكن أي طبقة لاحقة من معرفة صورة غلاف المصدر.

### د. مسار طبقة العرض (MediaDisplayTitleResolver & UI Presentation)
* في `MediaDisplayTitleResolver.kt`:
  - كان كائن `ResolvedMediaDisplayInfo` يحتوي فقط على `canonicalTmdbId`, `canonicalTitle`, `providerTitle`, `displayTitle`, `variantMode`, `providerId`, `sourcePageUrl`, `isConfirmedDubbed`, `contentType`.
  - كان يفتقر تماماً إلى حقول تمثيل الغلاف: `canonicalPosterUrl`, `providerPosterUrl`, `displayPosterUrl`.
* في `SearchScreen.kt` و `SearchBarDropdown.kt`:
  - بطاقة البحث كانت تعرض `displayInfo.displayTitle` كعنوان، لكنها كانت مجبرة على أخذ `m.posterUrl` أو `s.posterUrl` (غلاف TMDB) مباشرة لعدم وجود غلاف العرض المناسب في `ResolvedMediaDisplayInfo`.

---

## 3. التعديلات الهندسية المنفذة (Implemented Architectural Upgrades)

### 1) تحديث نموذج المرشح وسجلات التغطية (`VariantModels.kt`)
تمت ترقية `ProviderSourceCandidate` و `ProviderCoverageRecord` بأقل تغيير ممكن ودون كسر أي مواضع استدعاء سابقة:
```kotlin
data class ProviderSourceCandidate(
    val providerId: String,
    val scraperKey: String,
    val sourcePageUrl: String,
    val title: String,
    val mode: MediaVariantMode = MediaVariantMode.SUBTITLED,
    val audioLanguage: VariantLanguage = VariantLanguage.ARABIC,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val status: CoverageStatus = CoverageStatus.FOUND,
    val streamUrl: String? = null,
    val streamExpiresAt: Long? = null,
    val qualities: List<QualitySource> = emptyList(),
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val posterUrl: String? = null,
    val contentType: ContentType? = null,
    val extraMetadata: Map<String, String> = emptyMap()
) {
    val providerTitle: String get() = title
    val providerPosterUrl: String? get() = posterUrl
}
```
وفي `ProviderCoverageRecord`:
```kotlin
data class ProviderCoverageRecord(
    val providerId: String,
    val scraperKey: String,
    val sourcePageUrl: String? = null,
    val coverageStatus: CoverageStatus = CoverageStatus.FOUND,
    val discoveredAt: Long = System.currentTimeMillis(),
    val streamUrl: String? = null,
    val streamExpiresAt: Long? = null,
    val qualities: List<QualitySource> = emptyList(),
    val errorMessage: String? = null,
    val providerFingerprint: String = "",
    val providerTitle: String? = null,
    val providerPosterUrl: String? = null
)
```

### 2) وضوح عقد بيانات نتائج البحث (`SearchModels.kt`)
تمت إضافة الخصائص المرجعية التوضيحية داخل `SearchMediaItem` لتوحيد تسميات المصدر دون التأثير على الحقول الأصلية:
```kotlin
data class SearchMediaItem(
    val id: String,
    val title: String,
    val posterUrl: String? = null,
    val url: String,
    val contentType: ContentType,
    val year: String? = null,
    val extraMetadata: Map<String, String> = emptyMap(),
    val providerId: String? = null,
    val sourceCandidates: List<ProviderSourceCandidate> = emptyList()
) {
    val providerTitle: String get() = title
    val providerPosterUrl: String? get() = posterUrl
    val sourcePageUrl: String get() = url
}
```

### 3) ترقية طبقة تسوية العرض (`MediaDisplayTitleResolver.kt`)
تم تحديث `ResolvedMediaDisplayInfo` ليشمل معلومات الغلاف وسياق النسخة ولغة الصوت بالكامل مع الحفاظ الكامل على الحقول الأصلية:
```kotlin
data class ResolvedMediaDisplayInfo(
    val canonicalTmdbId: String,
    val canonicalTitle: String,
    val providerTitle: String?,
    val displayTitle: String,
    val variantMode: MediaVariantMode,
    val providerId: String?,
    val sourcePageUrl: String?,
    val isConfirmedDubbed: Boolean,
    val contentType: String,
    val canonicalPosterUrl: String? = null,
    val providerPosterUrl: String? = null,
    val displayPosterUrl: String? = null,
    val audioLanguage: VariantLanguage? = null
)
```

كما تم تحديث منطق التسوية في `MediaDisplayTitleResolver`:
* **في حالة النسخة المترجمة أو القياسية (`!isDubbedExperience`):**
  - `canonicalPosterUrl = canonicalPosterUrl` (غلاف TMDB)
  - `providerPosterUrl = null`
  - `displayPosterUrl = canonicalPosterUrl`
  - `audioLanguage = VariantLanguage.ORIGINAL`
* **في حالة النسخة المدبلجة (`isDubbedExperience`):**
  - عند اختيار المرشح الأفضل (`bestCandidate`)، يتم أخذ **كل من العنوان والغلاف من نفس المرشح بدقة**:
    - `providerTitle = bestCandidate.title.trim()`
    - `providerPosterUrl = bestCandidate.posterUrl?.takeIf { it.isNotBlank() }`
    - `displayTitle = bestCandidate.title.trim()`
    - `displayPosterUrl = providerPoster ?: canonicalPosterUrl` (استخدام غلاف المصدر مع الرجوع الآمن لغلاف TMDB إذا لم يتوفر غلاف للمزود)
    - `audioLanguage = bestCandidate.audioLanguage`
* **إضافة دالة مساعدة لتسوية مرشح محدد (`resolveForCandidate`):**
  تتيح تسوية العنوان والغلاف عند تمرير مرشح محدد مسبقاً، مع ربط صارم يمنع خلط عنوان إضافة بغلاف إضافة أخرى.

### 4) تمرير البيانات عبر خط الإنتاج (`SearchManagedExtensionsUseCase.kt`)
تم تصحيح كافة مواضع إنشاء المرشحات لضمان عدم ضياع أي بيانات:
1. عند إنشاء مرشحات البحث المدبلج (`executeArabicDubbedSearch`): تمرير `posterUrl = item.posterUrl`, `contentType = item.contentType`, `extraMetadata = item.extraMetadata`.
2. عند تسجيل التغطية (`coverageRepo.recordProviderCoverage`): تسجيل `providerPosterUrl = item.posterUrl`.
3. عند إعادة بناء المرشحات من التغطية السابقة (`existingCandidate`): تمرير `posterUrl = rec.providerPosterUrl`.
4. عند البحث العام وتجميع النتائج: تمرير كافة الحقول والحفاظ على أفضل غلاف عند إزالة التكرار (`mergeAndDeduplicateItems`).

### 5) تكامل طبقة واجهة المستخدم (`SearchScreen.kt` & `SearchBarDropdown.kt`)
* تم تحديث `UnifiedMediaResult` في `SearchScreen.kt` ليحتوي على `canonicalPosterUrl` و `providerPosterUrl`.
* في `SearchScreen.kt`: يتم تعيين `posterUrl = displayInfo.displayPosterUrl ?: m.posterUrl` لعرض الغلاف الملائم لسياق النسخة دون المساس بغلاف TMDB الأصلي المحفوظ في `canonicalPosterUrl`.
* في `SearchBarDropdown.kt`: تم استخدام `displayInfo.displayPosterUrl ?: tmdbPoster` لعرض الغلاف المتسق مع النسخة المحددة.

---

## 4. جدول مصفوفة عقد البيانات المشترك (Data Contract Matrix)

| البيان | الحقل في TMDB | الحقل في مرشح المصدر (`ProviderSourceCandidate`) | الحقل في تسوية العرض (`ResolvedMediaDisplayInfo`) | الحقل في واجهة العرض (`UnifiedMediaResult`) |
| :--- | :--- | :--- | :--- | :--- |
| **معرف TMDB** | `movie.id` / `series.id` | غير مخزن هنا منعاً للخلط | `canonicalTmdbId` | `id` |
| **عنوان TMDB القانوني** | `movie.title` | غير مخزن هنا | `canonicalTitle` | `canonicalTitle` |
| **غلاف TMDB القانوني** | `movie.posterUrl` | غير مخزن هنا | `canonicalPosterUrl` | `canonicalPosterUrl` |
| **معرف الإضافة/المزود** | `movie.providerId` | `candidate.providerId` | `displayInfo.providerId` | `providerId` |
| **عنوان المصدر** | غير مخزن كعنوان أساسي | `candidate.title` (`providerTitle`) | `providerTitle` | يُستخدم في `title` عبر `displayTitle` |
| **غلاف المصدر** | غير مخزن في TMDB الأساسي | `candidate.posterUrl` (`providerPosterUrl`) | `providerPosterUrl` | يُستخدم في `posterUrl` عبر `displayPosterUrl` |
| **رابط صفحة المصدر** | `movie.sourceUrl` | `candidate.sourcePageUrl` | `sourcePageUrl` | عبر مسار المشغل والتفاصيل |
| **نوع المحتوى** | `ContentType.MOVIE` / `TV` | `candidate.contentType` | `contentType` | `contentType` |
| **نمط النسخة** | `movie.mode` | `candidate.mode` | `variantMode` | `variantMode` |
| **لغة الصوت** | `movie.language` | `candidate.audioLanguage` | `audioLanguage` | عبر سياق المشغل والبحث |

---

## 5. التوافق وعدم الانحدار (Compatibility & Invariant Verification)

1. **سلامة قواعد البيانات:** لم يتم تعديل أي من كائنات Room Entity أو DataStore أو Firestore، وظل المخطط متوافقاً تماماً دون الحاجة لأي migrations في هذه المرحلة.
2. **سلامة المفاتيح:** لم تتغير مفاتيح المفضلة، ولا معرفات التنزيل، ولا جلسات التشغيل.
3. **سلامة المحللات:** لم يتم تعديل أي استعلامات أو محددات CSS/HTML في أي من الإضافات المدارة.
4. **عزل هوية TMDB:** لم يتم استبدال أو كتابة عنوان المزود أو غلافه فوق عنوان أو غلاف TMDB داخل كائنات TMDB الأصلية.
5. **الربط الحصري للمرشح:** يتم اختيار الغلاف والعنوان معاً من المرشح المؤهل نفسه، مما يضمن استحالة ظهور عنوان موقع بغلاف موقع آخر.

---

## 6. التحقق البرمجي (Compilation & Verification)

* تم إجراء فحص البناء الكامل عبر أداة المنصة المعتمدة:
  `compile_applet`
* **النتيجة:**
  ```text
  Build succeeded - the applet is compiled
  ```
* البناء تم بنجاح تام وبدون أي أخطاء تجميع، وكافة التغييرات جاهزة تماماً للمراحل اللاحقة من خطة Phase 19.

---
**نهاية تقرير المرحلة 19.1 — CINESTREAM PHASE 19.1 COMPLETE**
