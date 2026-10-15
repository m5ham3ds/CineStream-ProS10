# CINESTREAM — PHASE 19.2 REPORT
# EGYDEAD & WITANIME SCRAPER REPAIR

**Execution Date:** 2026-10-14  
**Status:** COMPLETE — ALL INVARIANTS & UNIT TESTS VERIFIED  
**Canonical Application ID:** `com.aistudio.cinestream.xyzabc`  

---

## 1. النطاق والهدف (Scope & Objectives)

تم تنفيذ المرحلة **19.2** من مشروع **CineStream** بدقة وفقاً للمتطلبات المحددة:
- **المصادر المشمولة بالإصلاح:**
  1. `EgyDead` (`EgyDeadScraper.kt`)
  2. `WitAnime` (`WitanimeScraper.kt`)
- **المصادر المحمية والمستثناة تماماً:**
  - `QFilm`: لم يتم تعديل تنفيذها أو إعداداتها (`https://a.qfilm.tv`).
  - `Anime4Up` و `AnimeBlkom`: تم الحفاظ عليهما دون أي تغيير للمرحلة 19.3.
- **الحفاظ على عقود المرحلة 19.1:**
  - الحفاظ التام على نموذج `SearchMediaItem` واستقبال `posterUrl` / `providerPosterUrl`، و `title` / `providerTitle`، و `url` / `sourcePageUrl`، و `extraMetadata`.
  - الحفاظ التام على هوية TMDB القانونية، مفاتيح المفضلة، وسجلات التنزيل.
- **ضبط هوية التطبيق (Application ID Invariant):**
  - تم التحقق من وضبط `applicationId = "com.aistudio.cinestream.xyzabc"` في `app/build.gradle.kts` و `"package_name": "com.aistudio.cinestream.xyzabc"` في `app/google-services.json` وفقاً لتوجيهات المستخدم الصريحة.

---

## 2. الملفات المفحوصة والمعدلة (Inspected & Modified Files)

### الملفات المعدلة:
1. `app/src/main/java/com/example/extension/managed/scraper/EgyDeadScraper.kt`
   - إصلاح عقد البحث واستخدام الجذر `/?s=` في الصفحة الأولى وتوليد الصفحات اللاحقة بالنمط السليم.
   - تحسين محددات استخراج البطاقات (`ul.posts-list li.movieItem`, `li.movieItem`, `.movieItem`).
   - استخراج بوسترات التحميل الكسول (`data-src`, `data-lazy-src`, `data-original`, `src`) مع منع الصور النائبة والبيانات المشفرة.
   - دعم المسارات المتنوعة للأعمال دون اشتراط مسار `/movie/` حصراً، والتمييز الدقيق بين الأفلام والمسلسلات والأنمي.
   - حل الروابط النسبية والمطلقة بدون تكرار النطاق الأساسي.

2. `app/src/main/java/com/example/extension/managed/scraper/WitanimeScraper.kt`
   - تصحيح عقد البحث والانتقال الكامل إلى `/search?q={query}` بدلاً من النمط القديم.
   - دعم كامل لمسارات الأفلام `/movie/{slug}` وتصنيفها كـ `ContentType.MOVIE` مع عدم حصر النتائج على `/anime/`.
   - استخراج العناوين النظيفة دون دمج التقييمات أو سنوات الإصدار أو شارات الحالة.
   - معالجة نطاق الصور المنفصل (`https://images.witanime.site/posters/`) دون تكرار النطاق الأساسي.
   - استخراج الحلقات بدقة وربط أرقام الحلقات والمواسم بصورة سليمة.

3. `app/src/main/java/com/example/extension/managed/web/TrustedEmbedHostPolicy.kt`
   - تحديث النطاقات الموثوقة لتشمل `tv10.egydead.live`, `egydead.live`, `egydead.to`, `witanime.site`, `witanime.com`, `4shared.com`، مع الحفاظ على قواعد المضيفات الآمنة ومنع الشبكات المحلية أو عناوين IP الخاصة.

4. `app/src/main/java/com/example/extension/orchestrator/ManagedMediaOrchestrator.kt`
   - ضبط الإعدادات المجمعة الافتراضية للنطاقات (`https://tv10.egydead.live` و `https://witanime.site`).
   - الحفاظ التام على بقية الإضافات (`QFilm`, `Anime4Up`, `AnimeBlkom`).

5. `app/src/main/java/com/example/extension/managed/registry/ScraperRegistry.kt`
   - تسجيل الكاشطات الثابتة الموثوقة بصورة مستقلة ومجردة.

6. `app/build.gradle.kts`
   - ضبط `applicationId = "com.aistudio.cinestream.xyzabc"`.

7. `app/google-services.json`
   - ضبط `package_name = "com.aistudio.cinestream.xyzabc"`.

8. `app/src/test/java/com/example/extension/managed/scraper/EgyDeadAndWitAnimeScraperTest.kt`
   - مجموعة اختبارات شاملة تغطي 13 سيناريو لاختبار عقود البحث واستخراج البطاقات، ومعالجة الصور الكسولة، والتمييز بين الأفلام والمسلسلات، والحفاظ على ثوابت QFilm وAnime4Up وAnimeBlkom.

---

## 3. تفاصيل إصلاح كاشطة EgyDead

1. **عقد البحث (Search Contract):**
   - **النطاق:** `https://tv10.egydead.live`
   - **الصفحة الأولى:** `GET /?s={URL_ENCODED_QUERY}`
   - **الصفحات اللاحقة:** `GET /page/{page}/?s={URL_ENCODED_QUERY}`
   - تم التخلص من النمط غير المتوافق، وتشفير الاستعلام العربي بواسطة `URLEncoder.encode(..., "UTF-8")` مع منع تكرار الوسيط `s`.

2. **محددات بطاقات البحث:**
   - المحددات المعتمدة: `div.search-page div.catHolder ul.posts-list li.movieItem, div.catHolder ul.posts-list li.movieItem, ul.posts-list li.movieItem, li.movieItem, .movieItem`.
   - استخراج العنوان: قراءة السمة `title` من الرابط أو العنصر `h1.BottomTitle, .BottomTitle, h2, .title`.
   - استخراج الغلاف: التحقق التتابعي من `data-src`، ثم `data-lazy-src`، ثم `data-original`، ثم `src`، مع استبعاد `data:image` و `placeholder`.

3. **نوع المحتوى والمسارات:**
   - عدم رفض الأعمال التي لا تحتوي على مسار `/movie/`؛ معالجة المسارات المباشرة ومسارات `/season/` و `/serie/`.
   - كشف الأعمال المدبلجة بدقة عبر الكلمات الدلالية وتمريرها في `extraMetadata["audio"] = "dubbed"`.
   - عدم تصنيف صفحات المسلسلات كأفلام، واستخراج الحلقات والمواسم المرتبطة.

---

## 4. تفاصيل إصلاح كاشطة WitAnime

1. **عقد البحث (Search Contract):**
   - **النطاق:** `https://witanime.site`
   - **صيغة الطلب:** `GET /search?q={URL_ENCODED_QUERY}` (للصفحة الأولى)، و `/search?q={URL_ENCODED_QUERY}&page={page}` (للصفحات اللاحقة).
   - إزالة النمط القديم `search_param=animes&s=`.

2. **دعم مسارات الأفلام وتصنيفها:**
   - شمل مسارات `/movie/{slug}` إلى جانب `/anime/{slug}`.
   - تصنيف الأفلام كـ `ContentType.MOVIE` دون إجبارها على تصنيف `ANIME`، مع توليد حلقة افتراضية تمثل الفيلم في صفحة التفاصيل.

3. **سلامة العناوين والصور:**
   - فصل شارات التقييم والسنة والنوع عن اسم العمل.
   - دعم استخراج الملصقات المستضافة على النطاق الخارجي `https://images.witanime.site/posters/` دون إضافة نطاق الموقع الأساسي أمامه.

4. **السيرفرات والتشغيل:**
   - التحليل المزدوج: فحص نقطة النهاية `/sources` لقراءة خوادم 4shared مع إخفاء وتأمين الرموز البرمجية (Tokens) بالكامل، مع وجود آلية احتياطية لفحص شجرة DOM للروابط المضمنة وإطارات iframe الموثوقة.

---

## 5. نتائج الاختبارات (Verification & Test Execution)

تم تنفيذ حزمة الاختبارات `com.example.extension.managed.scraper.EgyDeadAndWitAnimeScraperTest` بنجاح:
```
BUILD SUCCESSFUL in 5m 44s
31 actionable tasks: 20 executed, 11 up-to-date
```
تقرير XML للاختبارات (`TEST-com.example.extension.managed.scraper.EgyDeadAndWitAnimeScraperTest.xml`):
- عدد الاختبارات: **13**
- الاختبارات المتجاوزة: **13**
- الفشل (Failures): **0**
- الأخطاء (Errors): **0**
- التخطي (Skipped): **0**

### قائمة الاختبارات المنفذة:
1. `testEgyDead_searchFirstPageUsesRootPathWithS`: نجح.
2. `testEgyDead_cardExtractionTitleUrlPosterFromSameCard`: نجح.
3. `testEgyDead_relativeAndAbsoluteUrlsHandled`: نجح.
4. `testEgyDead_diversePathsNotRejectedWithoutMovieSlug`: نجح.
5. `testEgyDead_seriesNotClassifiedAsMovieWithoutEvidence`: نجح.
6. `testEgyDead_lazyImagesDoNotYieldEmptyOrPlaceholderPosters`: نجح.
7. `testWitAnime_searchUsesSearchWithQ`: نجح.
8. `testWitAnime_moviePathsAreNotExcludedAndAreClassifiedAsMovie`: نجح.
9. `testWitAnime_titleDoesNotConcatenateBadgesOrRatings`: نجح.
10. `testWitAnime_separateImagesDomainHandledWithoutPrependingBaseUrl`: نجح.
11. `testWitAnime_episodesParsedWithCorrectNumbersAndUrls`: نجح.
12. `testIntegration_searchMediaItemCarriesSourcePosterAndMetadata`: نجح.
13. `testIntegration_qFilmAndAnime4UpAndAnimeBlkomUnmodified`: نجح.

---

## 6. التأكيد على الثوابت (Invariants Confirmation)

- [x] بقاء `QFilm` دون أي تعديل في الكود أو الإعدادات.
- [x] بقاء `Anime4Up` و `AnimeBlkom` دون أي تعديل للمرحلة 19.3.
- [x] ثبات عقد بيانات المرحلة 19.1 وتمرير الملصقات والعناوين والروابط الأصلية.
- [x] تطبيق وحفظ اسم الحزمة القانوني `com.aistudio.cinestream.xyzabc`.
- [x] عدم بدء المرحلة 19.3 تلقائياً.
