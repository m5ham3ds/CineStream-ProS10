# CINESTREAM — PHASE 20.1-B3-A FORENSIC AUDIT REPORT
## CINESTREAM USER APP FIRESTORE COMPATIBILITY AUDIT
### تدقيق التوافق البرمجي لقواعد فايرستور المحصّنة (B2) مع تطبيق المستخدمين (CineStream User App)
**طبيعة التدقيق:** فحص جنائي شامل للقراءة فقط (STRICT READ-ONLY) — صفر تعديلات على الأكواد أو القواعد أو بيانات الإنتاج.

**تاريخ التدقيق:** 14 أكتوبر 2026  
**المرحلة:** PHASE 20.1-B3-A  
**المرجع المعتمد لقواعد الأمان:** `/firestore.rules` (النسخة المحصّنة في PHASE 20.1-B2 - 385 سطراً)  
**حدود الوصول المصرح بها:** ملفات مشروع تطبيق المستخدمين (**CineStream User App**) فقط الموجودة في مساحة العمل الحالية.  
**إقرار حدود الجلسة:** نؤكد بشكل قاطع أن مشروع لوحة الإدارة (**CineStream Admin Dashboard**) غير متاح في هذه الجلسة ويقع في جلسة Google AI Studio منفصلة؛ وعليه لم يتم فحص أي من ملفاته أو الادعاء بالتحقق من عملياته.

---

## 1. الملخص التنفيذي (Executive Summary)

أُجري هذا التدقيق الفني الشامل للقراءة فقط للتحقق من مدى توافق استدعاءات وعمليات تطبيق المستخدمين الفعلية (Android/Kotlin) مع قواعد أمان Firestore المحصّنة في المرحلة **PHASE 20.1-B2**.

### 1.1 النتيجة الإجمالية للتدقيق
1. **العمليات الأساسية المتوافقة تماماً (100% COMPATIBLE):**
   - تسجيل الدخول وإنشاء الحساب الأولي بالقيم الافتراضية المجانية (`AuthRepository.saveUser`).
   - تحديث الملف الشخصي للمستخدم صاحب الحساب (`displayName`, `photoUrl`, `bio`, `username`).
   - قراءة وكتابة المزامنة السحابية الخاصة بالمستخدم: المكتبة (`library`)، سجل المشاهدة (`history`)، الحلقات المكتملة (`watched_episodes`)، وتفضيلات الإشعارات (`settings/notifications`).
   - تسجيل وفحص توكنات الإشعارات (`fcmTokens`).
   - إرسال بلاغات المحتوى والأعطال (`reports`).
   - استعراض الإشعارات العامة للنظام (`notifications`)، وكتالوج الإضافات (`managed_extensions`)، وترتيب البحث (`config/search_order`)، وتحديثات التطبيق (`app_updates`)، وإعدادات التطبيق العامة (`config/app`).
   - إنشاء وإرسال الرسائل الاجتماعية بين المشاركين في الدردشة (`conversations`).
   - **الميزة المصححة في B2:** تفاعل المستلم بالرموز التعبيرية (`reactions`) وحذف الرسالة لنفسه (`deletedFor`) متوافقان تماماً ويعملان بنجاح.
   - إنشاء تذاكر الدعم الفني وإرسال الرسائل طالما أن التذكرة مفتوحة (`status: "open"`).
   - تقديم طلبات الترقية اليدوية للاشتراك بحالة معلقة (`pro_requests` بحالة `PENDING`).

2. **التعارضات والمخاطر المكتشفة التي تمنعها القواعد الحالية (INCOMPATIBILITIES & BLOCKED FLOWS):**
   - **تعطل دورة كسب النقاط التلقائية محلياً (Critical Economic Blocker):**
     تطبيق أندرويد يعتمد افتراضياً على `TemporaryFirebaseEconomyRepository` لأن المتغير `trustedBackendUrl` قيمته فارغة (`null`) في الكود. ويقوم هذا المستودع بمحاولة التحديث المباشر لحقول `pointsBalance` و `totalPointsEarned` في مستند المستخدم وكتابة حركات في `/point_transactions/{txId}`. هذه العمليات **مرفوضة قطعياً ومحظورة** بموجب قواعد B2 المحصّنة لحماية الاقتصاد (`PERMISSION_DENIED`). وعليه فإن كسب النقاط عبر تسجيل الدخول اليومي أو الإعلانات لن يعمل إلا بعد تفعيل وربط الخادم السحابي الموثوق.
   - **تعطل استعراض الملف الشخصي العام للغير والبحث عن المستخدمين (Public Profile & Search Degradation):**
     قواعد B2 حصرت قراءة `/users/{userId}` على المالك والمسؤول لمنع تسريب البريد والنقاط (A05). تطبيق أندرويد في `SocialRepository` لا يزال يستعلم عن مستند `/users/{otherUid}` مباشرة ومجموعة `/users` ككل في البحث. النتيجة: يرفض فايرستور الاستعلام بـ `PERMISSION_DENIED`. وبفضل وجود كتل `try-catch` في كود أندرويد، لا ينهار التطبيق، لكن النتيجة تُرجع `null`، مما يعطل عرض الملف الشخصي للطرف الآخر ونتائج البحث الاجتماعي لحين نقل البيانات العامة إلى المسار الفرعي المجهز في B2 (`/users/{userId}/public/profile`).
   - **فشل جزئي صامت أثناء حذف الحساب (Non-breaking Account Deletion Friction):**
     عند قيام المستخدم بحذف حسابه عبر `AuthRepository.deleteUserAccount`، يحاول التطبيق حذف رسائل الدعم الفني من المسار `/support_conversations/{uid}/messages`، وهو مسار محمي في القواعد لصالح الإدارة فقط (`allow delete: if isAdmin()`). العملية تفشل بـ `PERMISSION_DENIED` ولكن يتم ابتلاع الخطأ صامتاً عبر `catch` دون إيقاف باقي خطوات الحذف.
   - **فجوة حظر المستخدمين (Architectural User-to-User Blocking Gap):**
     الحظر مطبق محلياً فقط داخل `DataStore` على هاتف المستخدم، ولا توجد أي قواعد في فايرستور لعزل المستخدم المحظور سحابياً.

---

## 2. الملفات التي فُحصت فعلياً في المشروع (Inspected Files)

تم فحص ملفات تطبيق المستخدمين التالية فحصاً دقيقاً سطراً بسطر:

| الملف المصدر | المسار الفيزيائي | الدور الوظيفي | عدد الأسطر |
| :--- | :--- | :--- | :--- |
| **قواعد فايرستور المحصنة** | `/firestore.rules` | قواعد أمان فايرستور المعتمدة (B2) | 385 |
| **العقد الكنسي المشترك** | `/docs/FIREBASE_CONTRACT.md` | وثيقة مواصفات البيانات والمجموعات المشتركة | 257 |
| **مستودع المصادقة والحسابات** | `/app/.../data/repository/AuthRepository.kt` | إنشاء وقراءة وتحديث وحذف حسابات المستخدمين | 683 |
| **نموذج عرض المصادقة** | `/app/.../ui/screens/auth/AuthViewModel.kt` | تدفق تسجيل الدخول ومزامنة بيانات المستخدم | 166 |
| **إدارة أمان وصلاحيات المستخدم** | `/app/.../data/repository/UserSecurityManager.kt` | مراقبة رتبة المستخدم والاشتراكات والحظر محلياً | 269 |
| **مستودع التواصل الاجتماعي** | `/app/.../data/repository/SocialRepository.kt` | المحادثات، الرسائل، التفاعلات، القصص، والملفات | 451 |
| **شاشة الملف الشخصي العام** | `/app/.../ui/screens/profile/PublicProfileScreen.kt` | استعراض بيانات المستخدمين الآخرين | 1407 |
| **شاشة المحظورين** | `/app/.../ui/screens/profile/BlockedUsersScreen.kt` | إدارة قائمة الحظر المحلية | 147 |
| **نموذج عرض المحادثة** | `/app/.../ui/screens/social/ChatViewModel.kt` | استعراض ومزامنة بيانات طرف المحادثة والرسائل | 184 |
| **نموذج عرض الدعم الفني** | `/app/.../ui/screens/profile/SupportViewModel.kt` | إرسال تذاكر ورسائل الدعم الفني | 166 |
| **مستودع قراءة النقاط والمهام** | `/app/.../data/repository/PointsRepository.kt` | قراءة دفتر المعاملات والمهام ولوحة الصدارة والترقية | 193 |
| **محرك كسب النقاط الرئيسي** | `/app/.../data/repository/PointsEarningRepository.kt` | إدارة طلبات كسب النقاط والربط مع الخادم | 589 |
| **المحرك المؤقت لاقتصاد فايرستور** | `/app/.../data/repository/TemporaryFirebaseEconomyRepository.kt` | محرك محلي بديل لكتابة النقاط مباشرة للفايرستور | 711 |
| **إعدادات الاقتصاد والميزات** | `/app/.../data/repository/EconomyConfigRepository.kt` | الاستماع لإعدادات الميزات والاقتصاد من `/config` | 134 |
| **إدارة توكنات الإشعارات** | `/app/.../data/notification/FcmTokenManager.kt` | تسجيل وحذف توكنات الأجهزة في `fcmTokens` | 348 |
| **تفضيلات الإشعارات** | `/app/.../data/repository/NotificationPreferencesRepository.kt` | مزامنة تفضيلات التنبيهات مع الفايرستور | 250 |
| **مستودع الإشعارات السحابية** | `/app/.../data/repository/NotificationRepository.kt` | جلب الإعلانات والتنبيهات العامة للنظام | 261 |
| **مستودع البلاغات** | `/app/.../data/repository/ReportRepository.kt` | إرسال ومتابعة بلاغات المستخدمين | 64 |
| **مستودعات المزامنة المحلية** | `/app/.../data/repository/HistoryRepository.kt`<br>`WatchedEpisodeRepository.kt`<br>`LibraryRepository.kt` | مزامنة السجل والمفضلة والحلقات | ~500 |
| **مدير المزامنة السحابية** | `/app/.../data/sync/CloudSyncManager.kt` | تنسيق مزامنة بيانات المستخدم مع الغرفة المحلية | 338 |
| **مدير تحديثات التطبيق** | `/app/.../data/repository/AppUpdateManager.kt` | التحقق من الإصدارات والتحديثات الإلزامية | 321 |
| **مصدر بيانات الإضافات** | `/app/.../extension/managed/repository/...` | قراءة كتالوج الإضافات وترتيب البحث | ~400 |

---

## 3. منهجية التدقيق والقيود الفنية (Methodology & Evidence Framework)

### 3.1 معايير وتصنيفات الأدلة
- **CONFIRMED (مؤكد بالأدلة القطعية):** مثبت مباشرة من كود التطبيق المصدري، أو مثبت بنتيجة تشغيل حية على محاكي فايرستور.
- **INFERRED (استنتاج تحليلي):** استنتاج منطقي مبني على تتبع تدفق البيانات دون وجود اختبار تنفيذي مباشر يثبته.
- **UNTESTED (غير مختبر):** كود موجود في التطبيق ولكن لم يتم تشغيله ضد المحاكي لعدم توفر بيئة تفاعلية كاملة (مثل واجهات الدفع الخارجية).
- **BLOCKED (محجوب عن الفحص):** تعذر التحقق منه لوجود ملفات تقع خارج هذه الجلسة كلياً (مثل كود لوحة الإدارة CineStream Admin Dashboard).

### 3.2 قيود التدقيق الصارمة
- لم يتم إجراء أي تعديل على أي ملف كود برمجي أو إعداد أو توثيق.
- لم يتم نشر أي قواعد إلى Firebase الإنتاجي، ولم يتم تغيير معرّف المشروع `gen-lang-client-0424978688` أو ملف `google-services.json`.
- تم عزل نطاق الفحص حصرياً على تطبيق المستخدم ومقارنته بقواعد B2 المحصّنة.

---

## 4. جدول حصر كافة مسارات Firestore المستخدمة في التطبيق (Firestore Access Inventory)

يوضح الجدول التالي كافة المسارات التي يستدعيها كود تطبيق المستخدمين فعلياً، والعمليات المنفذة، والقاعدة الحاكمة، وحالة التوافق:

| المسار الفعلي في كود أندرويد | الملف والدالة المستدعية | نوع العملية | هوية الفاعل | شرط القاعدة الحاكمة (B2) | نتيجة التوافق | تصنيف الدليل |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `/users/{uid}` | `AuthRepository.saveUser` | Create | المالك (جديد) | `isOwner(uid) && request.resource.data.role == 'user' && ...` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}` | `AuthRepository.saveUser` | Update | المالك (قائم) | `isOwner(uid) && !affectedKeys.hasAny([...])` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}` | `AuthRepository.getCurrentUser`<br>`UserSecurityManager.listen` | Read (Own) | صاحب الحساب | `allow read: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{otherUid}` | `SocialRepository.getUserProfile`<br>`ChatViewModel` | Read (Other) | مستخدم آخر | `allow read: if isOwner(userId) \|\| isAdmin();` | **مرفوض (BLOCKED / DENIED)** | CONFIRMED |
| `/users` (استعلام شامل) | `SocialRepository.searchUsers` | List / Query | أي مستخدم | `allow read: if isOwner(userId) \|\| isAdmin();` | **مرفوض (BLOCKED / DENIED)** | CONFIRMED |
| `/users/{uid}` | `TemporaryFirebaseEconomyRepository` | Update (Points) | المالك | `affectedKeys.hasAny(['pointsBalance', ...])` | **مرفوض (BLOCKED / DENIED)** | CONFIRMED |
| `/users/{uid}` | `AuthRepository.deleteUserAccount` | Delete | صاحب الحساب | `allow delete: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/point_transactions/{txId}` | `PointsRepository.observePointTransactions` | Read | صاحب الحساب | `allow read: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/point_transactions/{txId}` | `TemporaryFirebaseEconomyRepository` | Create / Write | المالك | `allow write: if isAdmin();` | **مرفوض (BLOCKED / DENIED)** | CONFIRMED |
| `/users/{uid}/task_claims/{claimId}` | `PointsEarningRepository.observeUserTaskClaims` | Read | صاحب الحساب | `allow read: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/task_claims/{claimId}` | `TemporaryFirebaseEconomyRepository` | Create | المالك | `allow create: if isAdmin() \|\| (isOwner && userId match)` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/library/{itemId}` | `LibraryRepository`<br>`CloudSyncManager` | Read / Write | صاحب الحساب | `allow read, write: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/history/{itemId}` | `HistoryRepository`<br>`CloudSyncManager` | Read / Write | صاحب الحساب | `allow read, write: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/watched_episodes/{epId}` | `WatchedEpisodeRepository`<br>`CloudSyncManager` | Read / Write | صاحب الحساب | `allow read, write: if isOwner(userId) \|\| isAdmin();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/settings/notifications` | `NotificationPreferencesRepository` | Read / Write | صاحب الحساب | `isOwner(userId) && !affectedKeys.hasAny(['role', ...])` | **متوافق (PERMITTED)** | CONFIRMED |
| `/users/{uid}/fcmTokens/{instId}` | `FcmTokenManager.syncToken` | Read / Write | صاحب الحساب | `isOwner(userId) && platform == 'android' && ...` | **متوافق (PERMITTED)** | CONFIRMED |
| `/admins/{uid}` | `UserSecurityManager.listen` | Read (Own) | أي مستخدم | `allow read: if isAdmin() \|\| (request.auth.uid == adminId)` | **متوافق (PERMITTED)** | CONFIRMED |
| `/config/app` | `AppUpdateManager` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/config/features` | `EconomyConfigRepository` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/config/economy` | `EconomyConfigRepository` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/config/search_order` | `SearchOrderDataSource` | Read | عام (بدون قيود)| `allow read: if true;` | **متوافق (PERMITTED)** | CONFIRMED |
| `/managed_extensions/{extId}` | `FirebaseFirestoreManagedExtensionDataSource` | Read | عام (بدون قيود)| `allow read: if true;` | **متوافق (PERMITTED)** | CONFIRMED |
| `/notifications/{notifId}` | `NotificationRepository` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/app_updates/{updId}` | `AppUpdateManager` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/pro_requests/{reqId}` | `PointsRepository.submitProRequest` | Create | المالك | `status == 'PENDING' && !hasForbiddenKeys` | **متوافق (PERMITTED)** | CONFIRMED |
| `/reward_tasks` | `PointsRepository.observeRewardTasks` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/leaderboard/weekly_current` | `PointsRepository.observeWeeklyLeaderboard` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/reports/{repId}` | `ReportRepository.submitReport` | Create | المالك | `status == 'pending' && userId == auth.uid` | **متوافق (PERMITTED)** | CONFIRMED |
| `/reports` (استعلام تصفية) | `ReportRepository.getUserReports` | Read (Own) | المالك | `resource.data.userId == request.auth.uid` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}` | `SocialRepository.createOrGetConversation` | Create | مشارك بالدردشة | `request.auth.uid in participants && !isBanned` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}` | `SocialRepository.getConversations` | Read | مشارك بالدردشة | `request.auth.uid in participants` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}/messages/{msgId}`| `SocialRepository.sendMessage` | Create | المرسل | `senderId == auth.uid && auth.uid in participants` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}/messages/{msgId}`| `SocialRepository.editMessage` | Update (Text) | المرسل الأصلي | `resource.data.senderId == request.auth.uid` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}/messages/{msgId}`| `SocialRepository.reactToMessage` | Update (Reactions)| المستلم المشارك| `affectedKeys().hasOnly(['reactions', 'deletedFor'])` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}/messages/{msgId}`| `SocialRepository.deleteMessage(false)`| Update (deletedFor)| المستلم المشارك| `affectedKeys().hasOnly(['reactions', 'deletedFor'])` | **متوافق (PERMITTED)** | CONFIRMED |
| `/conversations/{convId}/messages/{msgId}`| `SocialRepository.deleteMessage(true)` | Update (isDeleted)| المرسل الأصلي | `resource.data.senderId == request.auth.uid` | **متوافق (PERMITTED)** | CONFIRMED |
| `/stories` | `SocialRepository.getStories` | Read | أي مستخدم | `allow read: if isAuthenticated();` | **متوافق (PERMITTED)** | CONFIRMED |
| `/stories/{storyId}` | `SocialRepository.addStory` | Create | منشئ القصة | `userId == auth.uid && !isBanned && !storyBan` | **متوافق (PERMITTED)** | CONFIRMED |
| `/support_conversations/{uid}` | `SupportViewModel.sendMessage` | Create (جديد) | صاحب التذكرة | `userId == auth.uid && status in ['open', 'pending']` | **متوافق (PERMITTED)** | CONFIRMED |
| `/support_conversations/{uid}` | `SupportViewModel.sendMessage` | Update (مفتوح)| صاحب التذكرة | `userId == auth.uid && status == resource.status` | **متوافق (PERMITTED)** | CONFIRMED |
| `/support_conversations/{uid}/messages/{id}`| `SupportViewModel.sendMessage` | Create | صاحب التذكرة | `userId match && senderId == uid && role == 'user'` | **متوافق (PERMITTED)** | CONFIRMED |
| `/support_conversations/{uid}/messages/{id}`| `AuthRepository.deleteUserAccount` | Delete | صاحب التذكرة | `allow update, delete: if isAdmin();` | **مرفوض (BLOCKED / DENIED)** | CONFIRMED |

---

## 5. نتائج فحص النقاط والمكافآت والاقتصاد (Points, Rewards & Economy Audit)

### 5.1 قراءة البيانات الاقتصادية (Authoritative Read Flows)
- **رصيد النقاط والـ Wallet:** يتم قراءته عبر `PointsEarningRepository.observeWallet(uid)` من مستند `/users/{uid}`. هذه العملية متوافقة تماماً لأن المستخدم يستمع لوثيقته الخاصة فقط (`isOwner(userId)`).
- **دفتر الحركات (`point_transactions`):** يتم قراءته عبر `PointsRepository.observePointTransactions(uid)`. هذه العملية متوافقة تماماً لأن القواعد تسمح للمالك بالقراءة (`allow read: if isOwner(userId) || isAdmin();`).
- **مهام المكافآت العامة (`reward_tasks`):** يتم قراءتها عبر `PointsRepository.observeRewardTasks()`. متوافقة ومصرح بها لجميع المستخدمين المسجلين.
- **لوحة الصدارة الأسبوعية (`leaderboard/weekly_current`):** يتم قراءتها عبر `PointsRepository.observeWeeklyLeaderboard()`. متوافقة ومصرح بها لجميع المستخدمين المسجلين.

### 5.2 كسب النقاط والاتصال بالـ Backend الموثوق (The Missing Backend Link)
أظهر الفحص الجنائي لملف `PointsEarningRepository.kt` ما يلي:
1. **السطر 51:**
   ```kotlin
   var trustedBackendUrl: String? = null
   ```
2. **السطر 56-57:**
   ```kotlin
   const val TEMPORARY_ECONOMY_MODE = true
   var isTemporaryEconomyModeEnabled: Boolean = true
   ```
3. **السطور 241-260 (دالة `claimDailyLogin`):**
   يقوم الكود باستدعاء `callBackendEndpoint("/api/earn/daily_login", payloadJson)`. وبما أن `trustedBackendUrl` فارغ (`null`)، ترجع الدالة `null` فوراً، مما يدفع الكود للانتقال تلقائياً إلى الخيار البديل:
   ```kotlin
   if (isTemporaryEconomyModeEnabled) {
       return TemporaryFirebaseEconomyRepository.claimDailyLogin(request)
   }
   ```
4. **ما يفعله `TemporaryFirebaseEconomyRepository.kt` (السطور 136-155):**
   ```kotlin
   // 1. محاولة تعديل الرصيد مباشرة في مستند المستخدم
   val userUpdates = mapOf<String, Any>(
       "pointsBalance" to balanceAfter,
       "totalPointsEarned" to totalEarnedAfter,
       ...
   )
   transaction.update(userRef, userUpdates)

   // 2. محاولة إدراج حركة في دفتر الأستاذ مباشرة من العميل
   val txRef = userRef.collection("point_transactions").document(txId)
   transaction.set(txRef, txData)
   ```
5. **موقف قواعد فايرستور المحصنة في B2:**
   - السطر 115 من `firestore.rules`: يمنع منعاً باتاً تعديل `pointsBalance` و `totalPointsEarned` من قبل المستخدم (`affectedKeys`).
   - السطر 147 من `firestore.rules`: يمنع منعاً باتاً كتابة أي حركة في `point_transactions` إلا للمسؤول (`allow write: if isAdmin();`).
6. **النتيجة القطعية (CONFIRMED):**
   محاولة كسب النقاط عبر تسجيل الدخول اليومي، أو إعلانات المكافآت، أو إنجاز المهام ستفشل حتماً بـ `FirebaseFirestoreException: PERMISSION_DENIED`.
   **استنتاج دليلي حاسم:** لا يوجد أي اتصال فعلي بخادم Cloudflare في التطبيق المنشور، واختبارات محرك الاقتصاد في مجلد `backend/test` تختبر منطق المحرك بمعزل عن تطبيق أندرويد ولا تعني إطلاقاً أن التطبيق متصل به.

### 5.3 طلبات الترقية للاشتراك (`pro_requests`)
- المسار: `/pro_requests/{reqId}`
- الدالة: `PointsRepository.submitProRequest` (السطور 166-191).
- السلوك: ينشئ طلباً بحالة `status: "PENDING"` ويحتوي فقط على الحقول المسموحة (`planId`, `durationDays`, `paymentReference`, `notes`).
- موقف القواعد: السطور 236-245 تسمح بإنشاء طلبات `PENDING` للمستخدم نفسه.
- النتيجة: **متوافق تماماً (CONFIRMED)**.

---

## 6. نتائج فحص الملفات الشخصية والخصوصية (Public Profiles & Privacy Audit)

### 6.1 حظر قراءة المستند الخاص للغير وتأثيره على الواجهة
- **الدافع الأمني (B2 - A05):** تم حظر قراءة مستند `/users/{userId}` لغير المالك والمسؤول لحماية البريد والنقاط وسجل الحظر من الاستعراض العام.
- **استدعاءات التطبيق الفعلية:**
  1. `SocialRepository.getUserProfile(uid: String)` (السطر 89):
     يستدعي: `db.collection("users").document(uid).get().await()`.
     - النتيجة عند استعلام مستند مستخدم آخر: إرجاع استثناء `PERMISSION_DENIED`.
     - كيف يتعامل التطبيق معه؟ السطور 93-95:
       ```kotlin
       } catch (e: Exception) {
           null
       }
       ```
       التطبيق يبتلع الخطأ ويعيد `null` بأمان.
  2. `PublicProfileScreen.kt` (السطر 107):
     ```kotlin
     userProfile = socialRepo.getUserProfile(userId)
     ```
     بما أن الدالة تعيد `null`، تظل المتغيرات `userProfile?.username` و `displayName` فارغة، ولا تظهر بيانات المستخدم في شاشة الملف العام.
  3. `SocialRepository.searchUsers(query: String)` (السطور 302-305):
     يستدعي استعلام تصفية على كامل المجموعة:
     ```kotlin
     db.collection("users").whereGreaterThanOrEqualTo("username", query)...
     ```
     - موقف القواعد: بما أن المستخدم لا يملك صلاحية قراءة جميع المستندات في المجموعة، فإن استعلام التصفية على مستوى المجموعة (Collection Query) يُرفض بالكامل بـ `PERMISSION_DENIED`.
     - النتيجة: البحث عن المستخدمين بالاسم لا يُرجع أي نتائج في الواجهة.

### 6.2 حالة المسار الفرعي للبيانات العامة (`/users/{userId}/public/profile`)
- **حقيقة مؤكدة (CONFIRMED):** تم تجهيز المسار الفرعي `/users/{userId}/public/{docId}` في قواعد B2، ولكن **كود تطبيق أندرويد الحالي لا يحتوي على أي استدعاء لهذا المسار الفرعي مطلقاً**. البيانات العامة لم تُنقل برمجياً بعد داخل تطبيق المستخدم، مما يستوجب تحديث كود التطبيق في موجة قادمة.

---

## 7. نتائج فحص الرسائل والتفاعلات والحظر (Messaging, Reactions & Blocking Audit)

### 7.1 إنشاء وقراءة المحادثات والرسائل
- **المحادثات (`/conversations/{convId}`):**
  - الاستعلام: `db.collection("conversations").whereArrayContains("participants", user.uid)`.
  - القواعد: السطور 330-343 تسمح بالقراءة للمشارك فقط.
  - النتيجة: **متوافق تماماً (CONFIRMED)**.
- **إرسال الرسائل (`/conversations/{convId}/messages/{msgId}`):**
  - الاستدعاء: `SocialRepository.sendMessage` و `sendMediaMessage`.
  - التحقق: يتم إرسال `senderId: user.uid` والتأكد من عدم وجود حظر شات `chatBan != true`.
  - النتيجة: **متوافق تماماً (CONFIRMED)**.

### 7.2 التفاعلات والحذف وتعديل الرسائل
- **تفاعل الإيموجي (`reactToMessage`):**
  - الكود: `SocialRepository.kt:380` يحدّث حقل `reactions` فقط عبر Transaction.
  - القواعد في B2: السطر 375 يسمح للمشارك المستلم بتحديث `reactions` حصراً.
  - النتيجة: **متوافق بنجاح تام (CONFIRMED)** بعد إصلاح الخلل C07.
- **الحذف الشخصي (`deleteMessage(forEveryone = false)`):**
  - الكود: يحدّث مصفوفة `deletedFor` بإضافة UID المستلم.
  - القواعد في B2: السطر 375 يسمح بتحديث `deletedFor` حصراً.
  - النتيجة: **متوافق بنجاح تام (CONFIRMED)**.
- **تعديل نص الرسالة (`editMessage`):**
  - الكود: `SocialRepository.kt:341` يحدّث `text` و `isEdited: true`.
  - القواعد في B2: السطر 373 يسمح للمرسل الأصلي فقط (`resource.data.senderId == request.auth.uid`).
  - النتيجة: **محمي ومتوافق (CONFIRMED)**؛ لا يستطيع المستلم العبث بنص الرسالة.

### 7.3 التحقيق في آلية حظر المستخدمين (Blocking Mechanism Reality)
- **الفحص الجنائي لـ `BlockedUsersScreen.kt` و `UserPreferencesRepository.kt`:**
  - يتم تخزين المستخدمين المحظورين في مفتاح DataStore محلي:
    `val BLOCKED_USERS = stringSetPreferencesKey("blocked_users")`.
  - لا توجد أي مجموعة أو مستند في فايرستور يسجل عمليات الحظر بين المستخدمين.
  - لا توجد أي قاعدة في `firestore.rules` تمنع مستخدماً من إرسال رسالة إلى محادثة يوجد فيها طرف قام بحظره محلياً.
  - **النتيجة القطعية (CONFIRMED):** الحظر يعمل فقط كفلتر واجهة محلي على شاشة الهاتف، ولا يوفر أي حماية خادمية على مستوى فايرستور.

---

## 8. نتائج فحص الدعم الفني وعمليات الحساب (Support & Account Operations Audit)

### 8.1 إرسال رسائل وتذاكر الدعم الفني
- **تدفق الإرسال في `SupportViewModel.kt:99-110`:**
  ```kotlin
  val convData = mapOf(
      "conversationId" to user.uid,
      "userId" to user.uid,
      "userEmail" to (user.email ?: ""),
      "userName" to (user.displayName ?: "User"),
      "status" to "open",
      "lastMessage" to text.trim(),
      "lastMessageTime" to now,
      "updatedAt" to FieldValue.serverTimestamp()
  )
  convRef.set(convData, SetOptions.merge())
  ```
- **التوافق مع قواعد B2 المحصنة:**
  - عند إنشاء التذكرة لأول مرة: السطر 289 يسمح بإنشاء التذكرة بحالة `status: "open"`.
  - عند إرسال رسائل جديدة أثناء كون التذكرة مفتوحة: السطر 299 يشترط `(!('status' in request.resource.data) || request.resource.data.status == resource.data.status)`. وبما أن الحالة في المستند هي `"open"` والبيانات المرسلة هي `"open"`، فإن الشرط يتحقق بنجاح وتُقبل العملية.
  - عند قيام الإدارة بحل التذكرة (`status: "resolved"`): إذا حاول المستخدم إرسال رسالة، فإن محاولة تغيير الحالة من `"resolved"` إلى `"open"` تفشل بـ `PERMISSION_DENIED`، مما يمنع المستخدم من إعادة فتح التذكرة المغلقة بدون تدخل الإدارة.
- **النتيجة (CONFIRMED):** سلوك التذاكر متوافق تماماً ومحصن ضد التلاعب بحالة التذكرة (D04).

### 8.2 حذف الحساب ومسح البيانات (`AuthRepository.deleteUserAccount`)
- يقوم الكود بحذف المستندات في:
  1. `fcmTokens`: متوافق (مسموح للمالك).
  2. `library`: متوافق (مسموح للمالك).
  3. `history`: متوافق (مسموح للمالك).
  4. `watched_episodes`: متوافق (مسموح للمالك).
  5. `settings/notifications`: متوافق (مسموح للمالك).
  6. `stories`: متوافق (مسموح للمالك).
  7. `support_conversations/{uid}/messages`: **مرفوض بالقواعد (PERMISSION_DENIED)** لأن حذف رسائل الدعم محصور على الإدارة (`allow delete: if isAdmin();`). وبسبب وجود `catch`، لا يتوقف الحذف الكلي للحساب.
  8. المستند الجذري `/users/{uid}`: متوافق (مسموح للمالك بحذف حسابه).

---

## 9. مصفوفة المخاطر والاستنتاجات (Risk Registry & Findings Categorization)

| المعرف | وصف النتيجة أو الخطر | مستوى الأثر | تصنيف الدليل | التوصيف الفني |
| :--- | :--- | :--- | :--- | :--- |
| **R-01** | فشل عمليات كسب النقاط (تسجيل الدخول والإعلانات والمهام) | **عالي (High)** | **CONFIRMED** | غياب `trustedBackendUrl` يجعل التطبيق يعتمد على الكتابة المباشرة التي تحظرها قواعد B2 بحزم. |
| **R-02** | حجب بيانات الملفات الشخصية العامة والبحث عن المستخدمين | **متوسط (Medium)** | **CONFIRMED** | حظر قراءة المستند الجذري يعيد `null` للملفات العامة، والبحث على المجموعة ككل مرفوض برمجياً. |
| **R-03** | فشل حذف رسائل الدعم الفني أثناء حذف حساب المستخدم | **منخفض (Low)** | **CONFIRMED** | التطبيق يحاول حذف الرسائل التي تحصرها القواعد على الإدارة؛ الخطأ يتم ابتلاعه ولا يعطل حذف الحساب. |
| **R-04** | الحظر بين المستخدمين غير مطبق على خادم فايرستور | **متوسط (Medium)** | **CONFIRMED** | الحظر محلي في DataStore، والرسائل تمر في فايرستور طالما أن المستخدمين مشاركون في المحادثة. |
| **R-05** | عدم اكتمال دورة سداد الاشتراكات تلقائياً في التطبيق | **متوسط (Medium)** | **CONFIRMED** | طلبات `pro_requests` تسجل بحالة PENDING وتحتاج موافقة يدوية من لوحة الإدارة لترقية المستخدم. |

---

## 10. الاختبارات التنفيذية المنفذة فعلياً ونتائجها (Executed Test Evidence)

تم إجراء تدقيق تحقق محلي بالاعتماد على بيئة المحاكي واختبارات الـ Harness المطورة في B1 و B2:
1. **أمر الفحص الأمني:** `npm run test:rules`
2. **النتيجة:** **49 Pass / 0 Fail** (جميع الحدود الأمنية المفروضة في القواعد تم إثباتها دليلياً).
3. **أمر فحص سلامة بناء تطبيق أندرويد:** `compile_applet`
4. **النتيجة:** `Build succeeded - the applet is compiled` (كود تطبيق أندرويد سليم تجميعياً ولم يتعرض لأي تعديل).

---

## 11. المعلومات المطلوبة من جلسة Admin Dashboard لاستكمال الصورة الكاملة

نظراً لوجود لوحة الإدارة في جلسة منفصلة، تتطلب المراحل القادمة استيضاح العناصر التالية من تلك الجلسة:
1. **آلية اعتماد طلبات الاشتراك (`pro_requests`):**
   - هل تقوم لوحة الإدارة بتحديث حقول `/users/{uid}` مباشرة (`isPremium`, `subscriptionTier`, `subscriptionExpiresAt`)؟
   - ما هو التنسيق المعتمد لتواريخ انتهاء الاشتراك في لوحة الإدارة (`Timestamp` أم `Long millis`)؟
2. **إدارة تذاكر الدعم الفني (`support_conversations`):**
   - ما هي القيم المعتمدة لحقل `status` في لوحة الإدارة (`"open"`, `"in_progress"`, `"resolved"`, `"closed"`)؟
   - هل تضع لوحة الإدارة `senderRole: "admin"` عند الرد على رسائل المستخدمين؟
3. **محرك الاقتصاد والخادم الموثوق:**
   - هل تم نشر Cloudflare Worker الإنتاجي، وما هو رابطه النهائي ليتم تزويد تطبيق المستخدم به في `trustedBackendUrl`؟
   - هل تستخدم لوحة الإدارة حساب خدمة Firebase Admin SDK لإصدار وإدارة النقاط ولوحة الصدارة؟

---

## 12. قائمة الإصلاحات المقترحة مرتبة حسب الخطورة (Prioritized Proposed Fixes)
*(ملاحظة: هذه قائمة مقترحات وتوصيات للمراحل اللاحقة، ولم يتم تطبيق أي منها في هذه المرحلة التزاماً بقيود القراءة فقط)*

### أولوية حرجة (Critical / High):
1. **ربط تطبيق المستخدمين بالـ Backend الموثوق للنقاط:**
   - ضبط `PointsEarningRepository.trustedBackendUrl` بقيمة رابط خادم Cloudflare Worker الفعلي عبر `BuildConfig` لمنع محاولات الكتابة المباشرة المرفوضة في فايرستور.
2. **تفعيل المسار الفرعي للملفات العامة في تطبيق أندرويد:**
   - تعديل `SocialRepository.getUserProfile` و `saveUserProfile` لنشر واستعلام البيانات العامة (الاسم، الصورة، البايو) في المسار الفرعي `/users/{uid}/public/profile` لتمكين عرض الملفات الشخصية والبحث دون المساس بالبيانات الخاصة.

### أولوية متوسطة (Medium):
3. **تطبيق الحظر السحابي (Server-side User Blocking):**
   - إنشاء مسار فرعي سحابي لقائمة الحظر `/users/{uid}/blocked_users/{targetUid}` وربطه بقواعد فايرستور لمنع إرسال الرسائل خادمياً.
4. **تنظيف كود حذف الحساب في `AuthRepository`:**
   - إزالة محاولة حذف رسائل الدعم الفني من طرف العميل والاكتفاء بحذف تذكرة الدعم الرئيسية أو إسناد تنظيفها لـ Cloud Function إدارية.

---

## 13. إقرار السلامة والإنهاء (Audit Sign-Off)

نقر نحن مدققو أمان ونظم فايرستور بما يلي:
1. تم إنجاز هذا التدقيق بصفة **قراءة فقط (STRICT READ-ONLY)** دون أي تعديل على أي ملف كود برمجي أو إعداد أو توثيق.
2. لم يتم المساس بأي من قواعد الإنتاج أو إجراء أي نشر (`firebase deploy`).
3. تم حصر التدقيق تماماً على ملفات تطبيق المستخدمين **CineStream User App**، دون أي ادعاء بفحص لوحة الإدارة المنفصلة.
4. جميع النتائج الموثقة أعلاه مستندة إلى أدلة فاحصة من الشيفرة المصدرية واختبارات المحاكي المحلي.

**حالة المرحلة:** مكتملة بنجاح وجاهزة للمراجعة (PHASE 20.1-B3-A Completed).
