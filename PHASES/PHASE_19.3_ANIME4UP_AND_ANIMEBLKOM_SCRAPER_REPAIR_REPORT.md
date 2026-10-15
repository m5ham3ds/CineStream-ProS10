# CINESTREAM — PHASE 19.3 REPORT
# ANIME4UP & ANIMEBLKOM SCRAPER REPAIR

**Execution Date:** 2026-10-14  
**Status:** COMPLETED — ALL INVARIANTS, SCRAPER REPAIRS & UNIT TESTS VERIFIED  
**Build Result:** SUCCESS (`compile_applet` passed, unit test suite passed)  

---

## 1. ملخص المرحلة ونطاق التنفيذ (Scope & Summary)

تم إنجاز المرحلة **19.3** من مشروع **CineStream** بدقة وصرامة مع الالتزام بكافة حدود العزل:
- **المصادر المستهدفة بالإصلاح:**
  1. `Anime4Up` عبر الملف `Anime4UpScraper.kt`.
  2. `AnimeBlkom` عبر الملف `AnimeBlkomScraper.kt`.
- **المكونات المحمية تماماً (Zero Modifications / Strict Invariants):**
  - كاشطة `QFilm` لم يتم تعديلها مطلقاً وظلت ثابتة ومحمية.
  - كاشطة `EgyDead` (`EgyDeadScraper.kt`) لم تُمس وحافظت على نتائج المرحلة 19.2.
  - كاشطة `WitAnime` (`WitanimeScraper.kt`) لم تُمس وحافظت على نتائج المرحلة 19.2.
  - محرك البحث العام وتحديد نية الدبلجة وترتيب النتائج لم يتأثر.
  - نماذج وقواعد بيانات Room وDataStore وFirestore لم تُعدل.
  - إعدادات Firebase والواجهات الخارجية لم تتغير.
  - قاعدة المعرف المؤقت: لم يتم تغيير المعرف المؤقت أو اعتباره عيباً، وسيتم تثبيته في مرحلة الإنهاء النهائية.

---

## 2. الملفات المفحوصة والمعدلة (Inspected & Modified Files)

### الملفات المفحوصة:
- `app/src/main/java/com/example/extension/managed/scraper/Anime4UpScraper.kt`
- `app/src/main/java/com/example/extension/managed/scraper/AnimeBlkomScraper.kt`
- `app/src/main/java/com/example/extension/managed/model/SearchModels.kt`
- `app/src/main/java/com/example/extension/managed/model/MediaDetailsModels.kt`
- `app/src/main/java/com/example/extension/managed/web/TrustedEmbedHostPolicy.kt`
- `app/src/main/java/com/example/extension/managed/registry/ScraperRegistry.kt`
- `app/src/main/java/com/example/extension/orchestrator/ManagedMediaOrchestrator.kt`
- `app/src/test/java/com/example/extension/managed/scraper/EgyDeadAndWitAnimeScraperTest.kt`

### الملفات المعدلة:
1. `app/src/main/java/com/example/extension/managed/scraper/Anime4UpScraper.kt` (إصلاح البحث، البطاقات، البوسترات الكسولة، التصنيف، التفاصيل، الحلقات، السيرفرات).
2. `app/src/main/java/com/example/extension/managed/scraper/AnimeBlkomScraper.kt` (إصلاح البحث، محددات البطاقات ومنع التكرار، تنظيف العناوين، البوسترات الكسولة، تصنيف الأفلام، الحلقات، السيرفرات).
3. `app/src/main/java/com/example/extension/managed/web/TrustedEmbedHostPolicy.kt` (إضافة نطاقات Anime4Up وAnimeBlkom إلى قائمة المضيفين الموثوقين).
4. `app/src/test/java/com/example/extension/managed/scraper/Anime4UpAndAnimeBlkomScraperTest.kt` (إنشاء حزمة اختبارات شاملة بـ 14 اختباراً تغطي المصدرين وثوابت التكامل).

---

## 3. العيوب المكتشفة والإصلاحات المطبقة في Anime4Up

### العيوب السابقة المكتشفة:
1. **فقدان بوسترات التحميل الكسول (Lazy-loaded Posters Bug):** كان الكود يتحقق من `img.attr("src")` أولاً، وبما أن مواقع التحميل الكسول تضع صور SVG أو Base64 أو روابط نائبة (Placeholder) داخل `src`، كانت السمة غير فارغة مما يؤدي لتجاهل السمة الحقيقية في `data-src` أو `data-lazy-src` أو `data-original` أو `data-image`.
2. **تداخل المحددات وتكرار البطاقات (Nested Selector Collision):** كان محدد البطاقات يشمل الحاوية العامة `div.anime-card-container` مع العناصر الفرعية `div.anime-card-themex` في نفس الاستعلام، مما أدى لابتلاع بيانات بطاقات متعددة داخل بطاقة واحدة وتكرار النتائج.
3. **أسبقية استخراج العناوين:** كان الكود يقدم السمة `title` الموجودة في الوسم `<a>` على العنوان الرئيسي الموجود في عناصر الترويسة (`.anime-card-title`, `h3`)، مما قد يدمج نصوصاً إضافية في العنوان.
4. **تصنيف المحتوى للأفلام والحلقات:** لم يكن يدعم تصنيف الأفلام المعتمد على وسوم وشارات النوع (`.badge`, `.type`)، كما لم يكن يوفر حلقة بديلة في صفحة تفاصيل الأفلام لتشغيلها بسلاسة عبر مشغل الوسائط.
5. **محددات السيرفرات:** كانت محددات السيرفرات تقتصر على `data-watch` فقط وتتجاهل العناصر المعتمدة على `data-embed` أو `data-src` أو `data-url`.

### تفاصيل الإصلاحات:
- **عقد البحث:**
  - النمط المعتمد: `GET /?s={URL_ENCODED_QUERY}` للصفحة الأولى، و `GET /page/{page}/?s={URL_ENCODED_QUERY}` للصفحات اللاحقة مع تشفير UTF-8 التام.
  - معالجة الاستعلامات الفارغة بأمان وإرجاع قائمة فارغة دون طلب شبكي غير مجدٍ.
- **استخراج البطاقات:**
  - حصر استخراج البطاقات على العناصر الفردية: `div.anime-card-themex, div.anime-card, div.anime-item, article.anime-card, div.post-entry`.
  - تقديم فحص عناصر العنوان المباشرة (`.anime-card-title, .anime-title, .title, h3, h2, h1`) مع تنظيف الفواصل والزوائد (`substringBefore(" – ")`).
- **معالجة الصور والبوسترات:**
  - فحص الخصائص التتابعية: `data-src`، ثم `data-lazy-src`، ثم `data-original`، ثم `data-image`، ثم `src`.
  - الرفض الصارم للصور النائبة والقيم التي تبدأ بـ `data:image` أو تحتوي على كلمة `placeholder`.
  - عدم مضاعفة النطاق الأساسي إذا كان رابط الصورة مطلقاً بالفعل.
- **تصنيف المحتوى والأفلام:**
  - فحص الرابط (`/movie/`) وشارات النوع وعنوان العمل لتحديد `ContentType.MOVIE` أو `ContentType.ANIME`.
  - في صفحة تفاصيل الأفلام، عند عدم وجود قائمة حلقات، يتم توليد حلقة افتراضية برقم 1 تمثل الفيلم لضمان استمرار المشغل واستخراج السيرفرات.
- **السيرفرات والتشغيل:**
  - توسيع محددات السيرفرات لتشمل `data-watch`, `data-embed`, `data-src`, `data-url`.
  - الحفاظ على فك تشفير Base64 الآمن وفرض بروتوكول HTTPS.

---

## 4. العيوب المكتشفة والإصلاحات المطبقة في AnimeBlkom

### العيوب السابقة المكتشفة:
1. **تكرار وتداخل محددات Bootstrap:** كان محدد البحث يدمج فئات الأعمدة المتجاورة في Bootstrap (`div.col-md-3, div.col-sm-4, div.col-xs-6, div.item, a[href*='/anime/']`) في نفس الاستعلام، مما أدى لمطابقة نفس العنصر عدة مرات وحدوث تكرار شديد للنتائج.
2. **فقدان روابط الأفلام في البحث:** كان الكود يشترط احتواء الرابط على `/anime/` صراحة ويستبعد أي نتيجة برابط `/movie/`.
3. **تلوث العنوان بالتقييمات والسنوات:** عند اللجوء للنص العام للبطاقة كان يتم دمج سنة الإصدار والتقييم وشارات الحالة في نص العنوان.
4. **فقدان البوسترات الكسولة:** الاعتماد على `img.attr("src")` أولاً أدى لفقدان الملصقات الحقيقية المخزنة في `data-original` و `data-src`.
5. **تقييد الحلقات والسيرفرات:** كانت دالة الحلقات تشترط احتواء الرابط على `/watch/` وتتجاهل روابط الحلقات بنمط `/episode/`، كما كانت السيرفرات تبحث عن `data-src` فقط وتتجاهل بقية السمات المضمنة.

### تفاصيل الإصلاحات:
- **عقد البحث:**
  - النمط المعتمد: `GET /search?query={URL_ENCODED_QUERY}` للصفحة الأولى، و `GET /search?query={URL_ENCODED_QUERY}&page={page}` للصفحات اللاحقة.
- **استخراج البطاقات ومنع التكرار:**
  - حصر استخراج البطاقات على الحاويات المنطقية: `.anime-card, .search-results .item, .content .item, div.anime-item, div.poster-item, div.item`.
  - استخراج العناوين النظيفة من عناصر الاسم والترويسة المخصصة وتجريد أي شارات مدمجة.
  - السماح بمسارات `/movie/` إلى جانب `/anime/`.
- **معالجة الصور والبوسترات:**
  - فحص تتابعي: `data-src`، `data-original`، `data-lazy-src`، `src` مع استبعاد `data:image` و `placeholder`.
- **تصنيف المحتوى والتفاصيل:**
  - التعرف الدقيق على الأفلام وتصنيفها كـ `ContentType.MOVIE` مع توفير حلقة افتراضية عند الحاجة.
  - دعم روابط الحلقات بنمطي `/watch/` و `/episode/` مع استخراج أرقام الحلقات بدقة دون الاعتماد على الترتيب العشوائي.
- **السيرفرات والتشغيل:**
  - دعم عناصر `data-src`, `data-embed`, `data-watch`, `data-url` وإطارات iframe الموثوقة مع فرض HTTPS.

---

## 5. الحفاظ على عقد بيانات المرحلة 19.1 (Provider Data Contract Preservation)

تم التحقق من تطابق الكاشطتين مع عقد البيانات الخاص بالمرحلة 19.1:
- `title` / `providerTitle`: ينبع حصراً من الموقع الخاص بالمصدر ولا يتم التعديل عليه ببيانات TMDB.
- `posterUrl` / `providerPosterUrl`: يتم تمرير ملصق المصدر المستخرج بشكل نظيف ومستقل.
- `url` / `sourcePageUrl`: يتم حفظ الرابط الأصلي الكامل لصفحة العمل.
- `contentType`: تصنيف منضبط (`MOVIE` أو `ANIME` أو `SERIES`).
- `extraMetadata`: نقل البيانات الإضافية المتاحة (كالتقييم، السنة، شارة النوع) دون فقدان.

---

## 6. نتائج الاختبارات والتحقق (Test & Build Verification)

### اختبارات المرحلة 19.3 (`Anime4UpAndAnimeBlkomScraperTest`):
تم تشغيل حزمة الاختبارات المستهدفة عبر Gradle:
- **عدد الاختبارات المنفذة:** 14 اختباراً.
- **الناجحة:** 14.
- **الفاشلة:** 0.
- **الأخطاء:** 0.
- **المتخطاة:** 0.

قائمة الاختبارات المنفذة:
1. `testAnime4Up_searchUrlConstructionAndArabicEncoding`: PASS
2. `testAnime4Up_cardExtraction_titlePosterUrlMapping`: PASS
3. `testAnime4Up_lazyLoadedPoster_rejectionOfPlaceholderAndDataImage`: PASS
4. `testAnime4Up_relativeAndAbsoluteUrls_noDoubleDomainPrepend`: PASS
5. `testAnime4Up_contentClassification_movieVsAnime`: PASS
6. `testAnime4Up_detailsAndEpisodesParsing`: PASS
7. `testAnime4Up_serversDiscovery_dataWatchAndBase64Decoding`: PASS
8. `testAnimeBlkom_searchUrlConstructionAndArabicEncoding`: PASS
9. `testAnimeBlkom_cardExtraction_titleCleanWithoutConcatenatedBadges`: PASS
10. `testAnimeBlkom_movieClassificationAndEpisodeFallback`: PASS
11. `testAnimeBlkom_episodesParsing_watchAndEpisodeUrls`: PASS
12. `testAnimeBlkom_serversDiscovery_dataSrcAndIframe`: PASS
13. `testIntegration_searchMediaItemContractPreserved`: PASS
14. `testInvariants_qFilmAndEgyDeadAndWitAnimeIntact`: PASS

### الاختبارات الشاملة للمراحل السابقة (`EgyDeadAndWitAnimeScraperTest`):
تم تشغيل اختبارات المرحلة 19.2 بالتوازي للتأكد من عدم حدوث أي انحدار (Regression):
- **النتيجة:** 13 اختباراً ناجحاً بنسبة 100% (0 failures).
- **إجمالي اختبارات الكاشطات الناجحة في المشروع:** **27 اختباراً موثقاً**.

### التحقق من بناء التطبيق (`compile_applet`):
- تم تنفيذ فحص بناء التطبيق الكامل بنجاح:
  `Build succeeded - the applet is compiled`

---

## 7. حالة الفحص الحي والقيود (Live Verification & Limitations)

- **حالة الفحص الحي:** تم الاعتماد على عينات وهياكل HTML ممثلة ومطابقة للواقع (Fixtures) تم اختبارها برمجياً دون إجراء اتصالات شبكية حية خارجية بالمواقع الفعلية لتجنب القيود الشبكية وتحديات Cloudflare داخل بيئة الحاويات. لم يتم الادعاء بإجراء اتصالات شبكية حية حقيقية.
- **القيود المتبقية:** في حال قيام أي من الموقعين بتحديث حماية Cloudflare (Turnstile / Managed Challenge)، فإن التطبيق يعتمد على منظومة `InteractiveChallengeWebView` و `CloudflareChallengeDetector` القائمة في المشروع لمعالجة التحديات التفاعلية وفقاً للسياسات الأمنية المعتمدة.

---

## 8. شرط التوقف (Stop Condition)

تم اكتمال المرحلة **19.3** بنجاح. وفقاً للتعليمات:
- تم التوقف فوراً وعدم الانتقال تلقائياً للمرحلة التالية.
- لم يتم المساس بمحرك البحث المدبلج أو المفضلة أو التنزيلات.
- تم تعليق تعديل المعرف المؤقت لمرحلة التثبيت النهائية.
