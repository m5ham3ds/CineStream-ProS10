# CINESTREAM — PHASE 20.1-A2 FORENSIC VERIFICATION REPORT
## FIRESTORE RULES SECURITY & MULTI-USER ISOLATION VERIFICATION
### STRICT READ-ONLY AUDIT — ZERO PRODUCTION MUTATIONS

**تاريخ التدقيق والتحقق:** 14 أكتوبر 2026  
**المرجعية الأساسية:** متابعة تقرير `PHASE_20.1-A_FULL_FIREBASE_FORENSIC_AUDIT_REPORT.md` والتحقق المستقل من الأدلة الفيزيائية  
**حالة الحوكمة والامتثال:** **STRICT READ-ONLY (التزام تام بالتدقيق دون تعديل أي كود أو إعداد أو قاعدة بيانات أو نشر أي خدمة)**  
**مهندس الأمن والتدقيق:** مهندس أمان Firebase & Cloud Firestore  

---

## الفهرس العام للتقرير

1. [الملخص التنفيذي ومنهجية التدقيق الميداني](#1-الملخص-التنفيذي-ومنهجية-التدقيق-الميداني)
2. [الملفات البرمجية والإعدادات المفحوصة في مساحة العمل](#2-الملفات-البرمجية-والإعدادات-المفحوصة-في-مساحة-العمل)
3. [التحقق المستقل من ادعاءات تقرير المرحلة السابقة (PHASE 20.1-A)](#3-التحقق-المستقل-من-ادعاءات-تقرير-المرحلة-السابقة-phase-201-a)
4. [المهمة الأولى: المراجعة الشاملة لقواعد الأمان الفعلية (firestore.rules)](#4-المهمة-الأولى-المراجعة-الشاملة-لقواعد-الأمان-الفعلية-firestorerules)
5. [المهمة الثانية: التدقيق الأمني للمحادثات الاجتماعية والرسائل (Social Conversations)](#5-المهمة-الثانية-التدقيق-الأمني-للمحادثات-الاجتماعية-والرسائل-social-conversations)
6. [المهمة الثالثة: التدقيق الأمني لمحادثات الدعم الفني (Support Conversations)](#6-المهمة-الثالثة-التدقيق-الأمني-لمحادثات-الدعم-الفني-support-conversations)
7. [المهمة الرابعة: التدقيق الأمني للاقتصاد والاشتراكات والنقاط (Economy & Subscriptions)](#7-المهمة-الرابعة-التدقيق-الأمني-للاقتصاد-والاشتراكات-والنقاط-economy--subscriptions)
8. [المهمة الخامسة: تفكيك وتوفيق هوية Firebase وتعدد المشروعات](#8-المهمة-الخامسة-تفكيك-وتوفيق-هوية-firebase-وتعدد-المشروعات)
9. [مصفوفة الصلاحيات الشاملة لكافة الأطراف (Permissions Matrix)](#9-مصفوفة-الصلاحيات-الشاملة-لكافة-الأطراف-permissions-matrix)
10. [سجل المخاطر الأمنية المصنفة حسب الشدة (Security Risk Registry)](#10-سجل-المخاطر-الأمنية-المصنفة-حسب-الشدة-security-risk-registry)
11. [المهمة السادسة: مصفوفة الاختبارات الأمنية غير المدمرة (Security Test Matrix)](#11-المهمة-السادسة-مصفوفة-الاختبارات-الأمنية-غير-المدمرة-security-test-matrix)
12. [خارطة الإصلاحات الأمنية المقترحة لمرحلة PHASE 20.1-B (دون تطبيق)](#12-خارطة-الإصلاحات-الأمنية-المقترحة-لمرحلة-phase-201-b-دون-تطبيق)
13. [المتطلبات الإلزامية الواجب حسمها قبل بدء PHASE 20.1-B](#13-المتطلبات-الإلزامية-الواجب-حسمها-قبل-بدء-phase-201-b)
14. [قائمة التحقق الصارمة لإثبات عدم إجراء أي تعديل أو نشر (Verification Checklist)](#14-قائمة-التحقق-الصارمة-لإثبات-عدم-إجراء-أي-تعديل-أو-نشر-verification-checklist)

---

## 1. الملخص التنفيذي ومنهجية التدقيق الميداني

أُجري هذا التدقيق الأمني الميداني كفحص جنائي وتحليلي مستقل لقواعد أمان Cloud Firestore (`/firestore.rules`) وكود تطبيق Android (`app/src/main/`) والبنية الخلفية الموثوقة (`backend/`)، وذلك بهدف:
1. التحقق من صحة أو دحض الاستنتاجات السابقة الواردة في تقرير `PHASE_20.1-A_FULL_FIREBASE_FORENSIC_AUDIT_REPORT.md` بالرجوع المباشر للأسطر الفيزيائية.
2. فحص متانة العزل بين المستخدمين المتعددين (Multi-User Isolation).
3. كشف أي ثغرات تسمح بالترقية الذاتية أو تزوير النقاط أو انتحال الشخصيات أو انتهاك الخصوصية.
4. إثبات الحالة الحقيقية لمشاريع Firebase دون ادعاء اتصالات أو اختبارات غير منفذة.

### المنهجية المتبعة:
- **Strict Read-Only:** فحص الملفات عبر أدوات القراءة والبحث (`view_file`, `grep`) دون تعديل حرف واحد في الكود أو الإعدادات أو المستندات.
- **Evidence-Based Line Attribution:** ربط كل استنتاج برقم السطر والملف المصدر مباشرة.
- **Epistemic Separation:** الفصل الصارم بين القيمة المكتوبة في ملف، والقيمة المستنتجة من البناء، والقيمة المسجلة في السجلات القديمة، والاتصال الميداني المختبر.

---

## 2. الملفات البرمجية والإعدادات المفحوصة في مساحة العمل

تمت قراءة وفحص الملفات التالية سطراً بسطر:

| الملف الفيزيائي | عدد الأسطر | الغرض الأساسي والنطاق المفحوص |
| :--- | :--- | :--- |
| `/firestore.rules` | 364 | قواعد أمان فايرستور، الصلاحيات، الحقول المحمية، الدوال المساعدة. |
| `/app/google-services.json` | 27 | تهيئة عميل أندرويد لخدمات Google و Firebase. |
| `/firebase-applet-config.json` | 11 | تهيئة عميل الويب و AI Studio لخدمات Firebase. |
| `/.env.example` | 10 | المتغيرات البيئية ومفاتيح الويب المرجعية ومفاتيح Cloudinary. |
| `/backend/wrangler.toml` | 16 | تهيئة بيئة تشغيل Cloudflare Worker والمتغيرات البيئية للخادم الموثوق. |
| `/app/build.gradle.kts` | 143 | تبعيات أندرويد، إعدادات الحزمة، إعدادات التوقيع، وتفعيل الإضافات. |
| `/app/src/main/AndroidManifest.xml` | 134 | الأذونات المصرح بها، خدمات FCM، ومكونات النظام. |
| `/app/src/main/java/com/example/data/repository/SocialRepository.kt` | 451 | مسارات المحادثات الاجتماعية، الرسائل، التفاعلات، الحذف، والقصص. |
| `/app/src/main/java/com/example/ui/screens/social/SocialViewModel.kt` | 122 | إدارة حالة المحادثات وقوائم المستخدمين والقصص. |
| `/app/src/main/java/com/example/ui/screens/social/ChatViewModel.kt` | 120 | إرسال الرسائل، الوسائط، الصوتيات، والتعديل والتفاعل. |
| `/app/src/main/java/com/example/ui/screens/profile/BlockedUsersScreen.kt` | 232 | واجهة قائمة المستخدمين المحظورين وإلغاء الحظر. |
| `/app/src/main/java/com/example/data/repository/UserPreferencesRepository.kt` | 167 | إدارة التفضيلات المحلية عبر Jetpack DataStore (بما فيها قائمة المحظورين). |
| `/app/src/main/java/com/example/data/repository/UserSecurityManager.kt` | 269 | مراقبة وثيقة الأمان `/users/{uid}` و `/admins/{uid}` وصلاحيات الحظر والميزات. |
| `/app/src/main/java/com/example/ui/screens/profile/SupportViewModel.kt` | 166 | إدارة محادثة الدعم الفني بين المستخدم والإدارة ومزامنة الغرف والرسائل. |
| `/app/src/main/java/com/example/data/repository/TemporaryFirebaseEconomyRepository.kt` | 711 | منطق تنفيذ الاقتصاد المؤقت مباشرة من طرف العميل ضد فايرستور. |
| `/app/src/main/java/com/example/data/repository/PointsRepository.kt` | 193 | مراقبة سجل النقاط والمهام والمتصدرين وتقديم طلبات الترقية. |
| `/app/src/main/java/com/example/data/repository/PointsEarningRepository.kt` | 589 | محرك كسب النقاط وتوجيه العمليات بين الخادم الموثوق والوضع المؤقت. |
| `/app/src/main/java/com/example/data/repository/AuthRepository.kt` | 683 | دورة حياة المصادقة، حذف الحساب، ومزامنة الوثيقة التأسيسية. |
| `/app/src/main/java/com/example/ui/screens/auth/AuthViewModel.kt` | 366 | واجهات الدخول بجوجل والبريد الإلكتروني والتعامل مع الجلسات. |
| `/backend/src/index.ts` | 337 | مسارات ونقاط نهاية الخادم الموثوق (Cloudflare Worker). |
| `/backend/src/auth.ts` | 72 | فحص وتوثيق توكنات Firebase Auth في الخادم الموثوق. |
| `/backend/test/backend_suite.test.js` | 666 | اختبارات محرك الاقتصاد النظري في بيئة Node.js. |
| `/docs/legacy/firebase-debug.log` | 51 | سجلات تنفيذ Firebase CLI القديمة وأخطاء الصلاحيات 403. |
| `/PHASE_20.1-A_FULL_FIREBASE_FORENSIC_AUDIT_REPORT.md` | 256 | التقرير المرجعي السابق المطلوب تدقيق استنتاجاته. |

---

## 3. التحقق المستقل من ادعاءات تقرير المرحلة السابقة (PHASE 20.1-A)

بمقارنة الادعاءات الواردة في التقرير السابق بالأدلة الفعلية داخل مساحة العمل، تبيّن الآتي:

| بند الادعاء السابق | ما ورد في تقرير PHASE 20.1-A | الدليل الفعلي في الملفات | نتيجة التدقيق الجنائي | التفاصيل والسبب الفيزيائي |
| :--- | :--- | :--- | :--- | :--- |
| **اسم الحزمة (Package Name)** | زعم أن اسم الحزمة هو `com.aistudio.cinestream.xyzabc` (سطر 23، 34، 191) | `app/google-services.json:11` و `app/build.gradle.kts:17` يحددان: `com.aistudio.cinestream.csderp` | ❌ **مردود (REFUTED)** | خطأ في تقرير المرحلة السابقة؛ المعرف الفعلي للحزمة هو `csderp` وليس `xyzabc`. |
| **معرف مشروع الويب (Web Project ID)** | زعم أن معرف المشروع في `firebase-applet-config.json` هو `ai-studio-applet-webapp-bd7dd` (سطر 24) | `firebase-applet-config.json:2` يحتوي فعلياً على: `"projectId": "gen-lang-client-0424978688"` | ❌ **مردود (REFUTED)** | القيمة الفعلية المكتوبة في الملف تشير إلى مشروع سحابي آخر تماماً يبدأ بـ `gen-lang-client-`. |
| **حالة استدعاءات تسجيل الدخول المجهول** | زعم وجود 3 استدعاءات نشطة لـ `signInAnonymously()` في كود كوتلن (سطر 154-156) | البحث الدقيق عبر `grep` في `app/src/main/` أعاد **0 نتائج**؛ وتم تأكيد إلغائها في `FirebaseFirestoreManagedExtensionDataSource.kt:11-13` (F-009) | ❌ **مردود ومصحح (REFUTED)** | كود التطبيق نظيف تماماً من أي استدعاء لـ `signInAnonymously()`. |
| **أمان قواعد Firestore الكامل** | زعم أن القواعد الحالية شاملة ومحكمة وتوفر عزلاً تاماً (`PASS: SECURE & COMPREHENSIVE`) (سطر 251) | التدقيق المتعمق لـ `firestore.rules:97-106` كشف ثغرة ترقية ذاتية وتعديل رصيد النقاط من العميل مباشرة | ❌ **مردود جزئياً (QUALIFIED)** | أغفل التقرير السابق وجود ثغرات حرجة في وضع الاقتصاد المؤقت تسمح للمستخدم بتعديل اشتراكه ورصيده مباشرة. |
| **عزل وسائط التخزين بـ Cloudinary** | زعم أن Firebase Storage غير مستخدم إطلاقاً وأن الاعتماد 100% على Cloudinary (سطر 52-55، 144) | `app/build.gradle.kts:54` معطل، و `SocialRepository.kt:158` يستخدم Cloudinary MediaManager | ✅ **مؤكد 100% (CONFIRMED)** | مطابقة تامة؛ لا توجد أي أسرار مكشوفة في التخزين وتخزين الملفات بالكامل سحابي خارجي. |
| **غياب تطبيق الإدارة (Admin App)** | زعم أن لوحة الإدارة غير موجودة في مساحة العمل (سطر 38، 250) | تم إجراء مسح هيكلي كامل للمشروع؛ لا يوجد أي تطبيق أو كود خاص بلوحة تحكم الإدارة | ✅ **مؤكد 100% (CONFIRMED)** | لا وجود للوحة الإدارة في المستودع الحالي، والتعامل يتم فقط عبر عقود وقواعد فايرستور. |
| **تفعيل خدمة FCM في الأندرويد** | زعم أن FCM مفعل في كود التطبيق عبر `FcmTokenManager` ومحمي بالقواعد (سطر 50، 142) | موجود ومؤكد في `AndroidManifest.xml` و `FcmTokenManager.kt` ومحمي في `firestore.rules:148-186` | ✅ **مؤكد 100% (CONFIRMED)** | خدمة التوكنات مطبقة فعلياً ومحمية بقواعد تدقيق نوعية وصارمة من 38 سطراً. |

---

## 4. المهمة الأولى: المراجعة الشاملة لقواعد الأمان الفعلية (firestore.rules)

تمت قراءة وتحليل كافة الأسطر الـ 364 في ملف `/firestore.rules`:

### 4.1 تحليل الدوال المساعدة (Helper Functions)
- **`isAuthenticated()` (السطور 6-8):**
  ```javascript
  function isAuthenticated() {
    return request.auth != null;
  }
  ```
  *التقييم:* سليمة ومعيارية، تمنع الطلبات غير المسجلة من الوصول للمسارات المشروطة.
- **`isOwner(userId)` (السطور 10-12):**
  ```javascript
  function isOwner(userId) {
    return isAuthenticated() && request.auth.uid == userId;
  }
  ```
  *التقييم:* تحقق صارم من هوية المالك، تمنع انتحال معرفات المستخدمين الآخرين.
- **`isAdmin()` (السطور 14-18):**
  ```javascript
  function isAdmin() {
    return isAuthenticated() && 
      exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
      get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.enabled == true;
  }
  ```
  *التقييم الأمني:*
  1. التحقق خادم موثوق (Server-authoritative) يعتمد على وجود الوثيقة في `/admins/{uid}` مع اشتراط `enabled == true`.
  2. **ملاحظة أمنية دقيقة:** في كود الأندرويد (`UserSecurityManager.kt:37-43`)، يتم منح المستخدم العادي صفة الإدارة محلياً في الواجهة إذا تطابق بريده مع `sulopros01@gmail.com`. **ولكن في قواعد الأمان لا يوجد أي اعتبار للبريد الإلكتروني**؛ وبالتالي، إذا لم تكن وثيقة المسؤول موجودة في Firestore ومفعلة بـ `enabled == true`، فإن كافة عمليات الإدارة التي يحاول مالك هذا البريد إرسالها تُرفض فورياً بـ `PERMISSION_DENIED`.

### 4.2 فحص القواعد المتداخلة والحظر الشامل (Wildcard Denial)
- المسار `/config/{document=**}` (السطور 51-53): `allow read, write: if false;` يحظر أي مسارات إعدادات عشوائية أو قديمة.
- المسار `/audit_logs/{document=**}` (السطور 359-361): `allow read, write: if false;` يحظر المسار القديم غير المعتمد، مما يمنع تسرب أو حقن السجلات في مسار غير رسمي.

### 4.3 فحص تصعيد الصلاحيات (Admin Escalation Vulnerability Check)
- **وثائق المسؤولين `/admins/{adminId}` (السطور 21-24):**
  ```javascript
  match /admins/{adminId} {
    allow read: if isAdmin() || (isAuthenticated() && request.auth.uid == adminId);
    allow write: if isAdmin();
  }
  ```
  - المستخدم العادي لا يستطيع كتابة أو إنشاء أو تعديل أي وثيقة في `/admins/{adminId}` لأن الكتابة مشروطة بـ `isAdmin()`.
  - وبالتالي: **يستحيل على المستخدم العادي ترقية نفسه لمسؤول عبر هذه المجموعة.**
- **حقول الإدارة في وثيقة المستخدم `/users/{userId}`:**
  - عند الإنشاء (`create` سطر 63 و 85-93): يُشترط أن يكون `role == 'user'` مع حظر كلي لـ `admin` و `isAdmin`.
  - عند التعديل (`update` سطر 99-106): يتم فحص الحقول المعدلة عبر `affectedKeys().hasAny(...)`، ويتم حظر المساس بـ `role`, `admin`, `isAdmin` حظراً مطلقاً.

### 4.4 فحص المسارات العامة (Public Read Paths) والبيانات المكشوفة
1. **`/config/search_order` (السطور 33-36):**
   `allow read: if true;` — مبرر ومناسب لتمكين الضيوف من ترتيب كاشفات البحث دون تسجيل دخول. لا توجد أي بيانات حساسة مكشوفة (مصفوفة معرفات نصوص فقط).
2. **`/managed_extensions/{extensionId}` و `/extensions/{extensionId}` (السطور 190-199):**
   `allow read: if true;` — مبرر ومناسب لتمكين محرك التشغيل من جلب تعريفات الكاشفات للضيوف. البيانات المعروضة هي بيانات عامة لتشغيل الملحقات.
3. **`/users/{userId}` (السطور 56-59):**
   `allow read: if isAuthenticated();`
   ⚠️ **ثغرة خصوصية وتسريب بيانات (HIGH RISK):**
   - القاعدة تتيح لأي مستخدم مسجل الدخول قراءة وثيقة أي مستخدم آخر بالكامل!
   - بما أن وثيقة المستخدم تجمع بين البيانات العامة (الاسم، الصورة) والبيانات الحساسة (البريد الإلكتروني `email`، رصيد النقاط `pointsBalance`، تاريخ انتهاء الاشتراك `subscriptionExpiresAt`، حالة الحظر `isBanned`، وسبب الحظر `banReason`)، فإن هذا يسمح لأي مستخدم باستخراج هذه البيانات الحساسة لأي حساب آخر بمجرد معرفة الـ UID الخاص به.

---

## 5. المهمة الثانية: التدقيق الأمني للمحادثات الاجتماعية والرسائل (Social Conversations)

تمت مطابقة كود `SocialRepository.kt`, `SocialViewModel.kt`, `ChatViewModel.kt`, `BlockedUsersScreen.kt` مع القواعد في `firestore.rules:306-340`:

### 5.1 عزل المشاركين (Participant Isolation)
- **قراءة المحادثة `/conversations/{convId}` (السطور 307-310):**
  ```javascript
  allow read: if isAuthenticated() && (
    isAdmin() ||
    (resource != null && request.auth.uid in resource.data.participants)
  );
  ```
  *النتيجة المؤكدة:* مستخدم خارج قائمة `participants` **لا يستطيع قراءة وثيقة المحادثة**.
- **قراءة الرسائل `/conversations/{convId}/messages/{msgId}` (السطور 325-328):**
  ```javascript
  allow read: if isAuthenticated() && (
    isAdmin() ||
    request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants
  );
  ```
  *النتيجة المؤكدة:* مستخدم غير مشارك **لا يستطيع قراءة الرسائل نهائياً**.

### 5.2 منع انتحال معرف المرسل (SenderId Spoofing)
- **إنشاء رسالة (السطور 329-334):**
  ```javascript
  allow create: if isAuthenticated() &&
    request.resource.data.senderId == request.auth.uid &&
    (request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants) &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.canChat != false &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.chatBan != true &&
    get(/databases/$(database)/documents/users/$(request.auth.uid)).data.isBanned != true;
  ```
  *النتيجة المؤكدة:*
  1. يُشترط أن يكون `request.resource.data.senderId == request.auth.uid`؛ وبالتالي **يستحيل تزوير هوية المرسل**.
  2. يُشترط أن يكون صاحب المعرف موجوداً في قائمة المشاركين.
  3. تُفحص حالة الحظر من الدردشة (`canChat`, `chatBan`, `isBanned`) على مستوى قاعدة البيانات عند الإرسال.

### 5.3 تعديل وحذف الرسائل وفجوة التفاعل والحذف الذاتي
- **تعديل وحذف الرسائل (السطور 335-338):**
  ```javascript
  allow update, delete: if isAuthenticated() && (
    isAdmin() ||
    (resource != null && resource.data.senderId == request.auth.uid)
  );
  ```
  *النتيجة المؤكدة:*
  1. المستخدم أ لا يستطيع تعديل أو حذف رسالة المستخدم ب. هذا يوفر حماية تامة لسلامة المحتوى.
  2. ⚠️ **فجوة وظيفية مؤكدة (Functional Blockage):**
     في `SocialRepository.kt:366-384`، تحاول دالة `reactToMessage()` تحديث حقل `reactions` على رسالة الطرف الآخر لإضافة إيموجي، وتحاول دالة `deleteMessage(forEveryone = false)` (سطر 346-364) تحديث حقل `deletedFor` لإخفاء الرسالة لدى المستقبل.
     **بما أن القاعدة تشترط `resource.data.senderId == request.auth.uid` لأي عملية `update`**، فإن محاولة المستقبل إضافة تفاعل أو حذف الرسالة محلياً تفشل فورياً بخطأ `PERMISSION_DENIED`!

### 5.4 قيود الحظر بين المستخدمين (User Blocking Gap Analysis)
- **في الواجهة وتطبيق المستخدم:**
  - الحظر في `BlockedUsersScreen.kt:48` و `UserPreferencesRepository.kt:59, 127-139` يعتمد كلياً على **تفضيلات محلية في DataStore** على جهاز المستخدم (`BLOCKED_USERS`).
- **في قواعد الأمان فايرستور:**
  - **قواعد فايرستور لا تملك أي فكرة عن قوائم الحظر الشخصية بين المستخدمين.**
  - لا توجد مصفوفة `blockedUsers` في وثيقة المستخدم، ولا تتحقق قواعد `/conversations` أو `/messages` من الحظر الفردي.
- **النتيجة الميدانية:**
  إذا قام المستخدم أ بحظر المستخدم ب محلياً على هاتفه، فإن المستخدم ب **ما زال قادراً تقنياً على إرسال رسائل جديدة** إلى المحادثة المشتركة في Firestore، وتقبلها قواعد الأمان بنجاح! هذا يشكل فجوة حقيقية بين الواجهة وقواعد الأمان.

---

## 6. المهمة الثالثة: التدقيق الأمني لمحادثات الدعم الفني (Support Conversations)

تم فحص المسارين المستقلين في `firestore.rules:258-303` ومطابقتهما مع `SupportViewModel.kt:48-135`:
- `/support_conversations/{conversationId}`
- `/support_conversations/{conversationId}/messages/{messageId}`

### 6.1 منع انتحال دور المسؤول في الدعم (Admin Role Spoofing)
- في `firestore.rules:292-298`:
  ```javascript
  allow create: if isAuthenticated() && (
    isAdmin() || (
      get(/databases/$(database)/documents/support_conversations/$(conversationId)).data.userId == request.auth.uid &&
      request.resource.data.senderId == request.auth.uid &&
      request.resource.data.senderRole == 'user'
    )
  );
  ```
  *النتيجة المؤكدة:*
  1. إذا حاول مستخدم عادي إرسال رسالة تحمل `senderRole = "admin"`، فإن الشرط `request.resource.data.senderRole == 'user'` ينتفي، وشرط `isAdmin()` ينتفي؛ وبالتالي **تُرفض العملية فورياً**.
  2. يستحيل على المستخدم العادي تزوير رسائل الدعم الفني لتظهر وكأنها صادرة من فريق الإدارة.

### 6.2 عزل محادثات الدعم بين المستخدمين
- في `firestore.rules:261-263`:
  `allow read: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);`
- وفي الرسائل (سطر 285-288):
  `allow read: if isAdmin() || (isAuthenticated() && get(...).data.userId == request.auth.uid);`
  *النتيجة المؤكدة:* المستخدم العادي **لا يستطيع إطلاقاً قراءة تذاكر دعم المستخدمين الآخرين أو رسائلهم**.

### 6.3 ثغرة تعديل حالة التذكرة من قبل المستخدم (Ticket Status Tampering Vulnerability)
- في `firestore.rules:272-277`:
  ```javascript
  allow update: if isAdmin() || (
    isAuthenticated() &&
    resource.data.userId == request.auth.uid &&
    request.resource.data.userId == resource.data.userId &&
    (!('createdAt' in request.resource.data) || request.resource.data.createdAt == resource.data.createdAt)
  );
  ```
- ⚠️ **ثغرة أمنية وظيفية مؤكدة (MEDIUM RISK):**
  القاعدة تسمح للمستخدم المالك بتحديث وثيقة تذكرته، واشترطت فقط ثبات `userId` و `createdAt`، **لكنها لم تمنع تعديل حقل `status`**.
  وفي كود أندرويد (`SupportViewModel.kt:100-110`)، يقوم التطبيق عند كل رسالة جديدة يرسلها المستخدم بتنفيذ:
  ```kotlin
  val convData = mapOf(
      "conversationId" to user.uid,
      "userId" to user.uid,
      "status" to "open",
      ...
  )
  convRef.set(convData, SetOptions.merge())
  ```
  - إذا قام المسؤول بحل التذكرة وتغيير حالتها إلى `"resolved"` أو `"closed"`، فإن إرسال المستخدم لأي رسالة جديدة يعيد كتابة `status = "open"` مباشرة دون إذن الإدارة!
  - كما يمكن لأي مستخدم مخترق إرسال طلب تعديل مباشر وجعل حالة التذكرة أي نص يريده.

---

## 7. المهمة الرابعة: التدقيق الأمني للاقتصاد والاشتراكات والنقاط (Economy & Subscriptions)

تم فحص تدفق العمليات في:
- `/users/{userId}`
- `/users/{userId}/point_transactions/{txId}`
- `/users/{userId}/task_claims/{claimId}`
- `/pro_requests/{requestId}`
- `/reward_tasks/{taskId}`
- `/leaderboard/{docId}`

ومطابقتها مع كود `TemporaryFirebaseEconomyRepository.kt` و `PointsEarningRepository.kt` و `backend/src/`:

### 7.1 الثغرة الحرجة: الترقية الذاتية للاشتراك (Self-Granting Subscription Bypass)
- **الموقع في القواعد:** `/firestore.rules:97-106`:
  ```javascript
  // User updates in TEMPORARY ECONOMY MODE: Owner can update profile, economy, and subscription fields.
  allow update: if isAdmin() || (
    isOwner(userId) &&
    !request.resource.data.diff(resource.data).affectedKeys()
      .hasAny(['role', 'admin', 'isAdmin',
              'deviceLimit', 'maxDevices', 'allowedQuality', 'downloadLimit',
              'isActive', 'isBanned', 'banReason', 'banExpiresAt',
              'canWatch', 'canDownload', 'canChat', 'canStory', 'canP2P',
              'watchBan', 'downloadBan', 'chatBan', 'storyBan', 'p2pBan',
              'offlineDaysOverride', 'forcedAdsOverride'])
  );
  ```
- **التحليل الجنائي الدقيق:**
  الحقول التالية **غير موجودة إطلاقاً** في قائمة الحظر أعلاه:
  1. `isPremium`
  2. `subscriptionTier`
  3. `subscriptionStatus`
  4. `subscriptionExpiresAt`
  5. `plan` / `proPlan` / `proExpiresAt`
- **النتيجة المؤكدة (CRITICAL VULNERABILITY):**
  يستطيع أي مستخدم مسجل يملك أدوات تطوير أو نسخة معدلة إرسال استدعاء شبكي مباشر عبر Firestore SDK:
  ```kotlin
  db.collection("users").document(myUid).update(
      "isPremium", true,
      "subscriptionTier", "PRO",
      "subscriptionStatus", "ACTIVE",
      "subscriptionExpiresAt", 4102444800000L // سنة 2100
  )
  ```
  **وقواعد الأمان ستوافق على هذا التعديل فورياً بنجاح تام!**
  تسمية هذا الوضع في الكود بـ "TEMPORARY ECONOMY MODE" لا ينفي كونه ثغرة أمنية كبرى تسمح بتجاوز الدفع واستبدال النقاط والحصول على اشتراك دائم مجاناً.

### 7.2 ثغرة تزوير رصيد النقاط (Points Balance Inflation)
- في نفس القاعدة أعلاه (`firestore.rules:97-106`):
  الحقول `pointsBalance`, `totalPointsEarned`, `totalPointsSpent` غير محمية في `allow update`.
- **النتيجة المؤكدة (CRITICAL VULNERABILITY):**
  يستطيع أي مستخدم تعديل رصيده إلى مليون نقطة مباشرة:
  ```kotlin
  db.collection("users").document(myUid).update("pointsBalance", 1000000L)
  ```
  وستقبل القواعد الطلب دون الحاجة لإتمام أي مهمة أو مشاهدة إعلان أو تشغيل باك إند.

### 7.3 ثغرة تكرار المطالبة بالمهام عبر حذف وثيقة المطالبة (Task Re-Claiming)
- **الموقع في القواعد:** `/firestore.rules:131-134`:
  ```javascript
  match /task_claims/{claimId} {
    allow read: if isOwner(userId) || isAdmin();
    allow write: if isAdmin() || (isOwner(userId) && request.resource.data.userId == userId);
  }
  ```
- **التحليل الجنائي:**
  - في Firestore Rules، كلمة `allow write` تعني السماح بـ `create`, `update`, `delete`.
  - في كود أندرويد (`TemporaryFirebaseEconomyRepository.kt:268`)، يتم فحص منع التكرار عبر التأكد من وجود وثيقة في `task_claims`:
    `if (claimDoc.exists()) throw IllegalStateException("TASK_ALREADY_CLAIMED")`
  - **ولكن بما أن المستخدم يملك صلاحية `write` (بما فيها `delete`) على وثائق `task_claims` الخاصة به**، فإنه يستطيع ببساطة حذف وثيقة المطالبة، ثم استدعاء كود كسب المهمة مرة ثانية وثالثة وعاشرة!

### 7.4 تدقيق طلبات الترقية والمهام والمتصدرين
- **`/pro_requests/{requestId}` (السطور 214-227):**
  *تقييم محكم وسليم:* المستخدم يستطيع فقط إنشاء طلب بحالة `PENDING` مع حظر كامل لحقول الموافقة أو الترقية، والتعديل والحذف مقيد بالمسؤول فقط.
- **`/reward_tasks/{taskId}` (السطور 230-233):**
  *تقييم سليم:* قراءة فقط للمستخدم، والتعديل للإدارة فقط.
- **`/leaderboard/{docId}` (السطور 236-239):**
  *تقييم سليم:* قراءة فقط للمستخدم، ولا يمكن للمستخدم التلاعب بقوائم الشرف الأسبوعية.

### 7.5 حالة الباك إند الموثوق (Cloudflare Worker Status)
- في `backend/src/index.ts:32-110`:
  الخادم الموثوق يحتوي على محرك معاملات اقتصادي ممتاز يمنع التكرار ويفحص التوكنات، **ولكنه يستخدم مخزناً مؤقتاً في الذاكرة (`WorkerDataStore`) ولا يتصل بقاعدة Firestore الحقيقية عبر Firebase Admin SDK**.
- في كود أندرويد (`PointsEarningRepository.kt:51`):
  المتغير `trustedBackendUrl` قيمته الافتراضية `null`؛ مما يعني أن تطبيق الهاتف لا يتصل إطلاقاً بهذا الخادم، ويعتمد بنسبة 100% على الكود المؤقت غير الآمن في العميل.

---

## 8. المهمة الخامسة: تفكيك وتوفيق هوية Firebase وتعدد المشروعات

وفقاً للتوجيه الإلزامي بالفصل التام بين مستويات الإثبات، نلخص حقائق الهوية الميدانية كالتالي:

| المستوى | المصدر المفحوص | المعرف المسجل (`Project ID`) | رقم المشروع (`Project Number`) | اسم الحزمة (`Package Name`) | حالة التحقق الميداني والاتصال |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1. القيمة المكتوبة في ملف التهيئة** | `app/google-services.json` | `remixed-project-id` | `1234567890` | `com.aistudio.cinestream.csderp` | **قالب افتراضي ترقيعي (Mock Placeholder)** |
| **1. القيمة المكتوبة في ملف التهيئة** | `firebase-applet-config.json` | `gen-lang-client-0424978688` | `461424289527` | غير منطبق (Web Client) | **تهيئة ويب مستقلة** |
| **1. القيمة المكتوبة في ملف التهيئة** | `backend/wrangler.toml:13` | `ai-studio-applet-webapp-e138b` | غير متوفر | غير منطبق (Worker) | **معرف مستقل لخادم النقاط** |
| **1. القيمة المكتوبة في ملف التهيئة** | `.env.example:8` | غير محدد بالاسم | `979447256418` (مستخرج من Client ID) | غير منطبق | **معرف عميل OAuth رابع** |
| **2. الاستنتاج من إعدادات البناء** | `app/build.gradle.kts` | مشتق من `google-services.json` (`remixed-project-id`) | مشتق من `google-services.json` (`1234567890`) | `com.aistudio.cinestream.csderp` | **البناء يولد موارد تشير للمشروع الوهمي** |
| **3. المسجل في السجلات التشغيلية** | `docs/legacy/firebase-debug.log` | `455865098278` و `416678724956` | مسجلة كمعرفات مشاريع سابقة | غير متوفر | **فشل سابق بخطأ 403 API Disabled** |
| **4. الاتصال الميداني المختبر** | الاتصال المباشر بخدمة Firebase | **غير مؤكد / غير متصل إطلاقاً (UNVERIFIED / DISCONNECTED)** | غير متوفر | غير متوفر | **لا يوجد أي اتصال سحابي ناجح تم اختباره** |

### التوصيف الفني القاطع لهوية Firebase:
لا يوجد حالياً مشروع Firebase إنتاجي موحد أو متصل يربط بين تطبيق الأندرويد، وخادم Cloudflare، وعميل الويب. تطبيق أندرويد يحمل في ملفاته معرفاً وهمياً (`remixed-project-id`)، وأي استدعاء شبكي حقيقي لـ Firebase Auth أو Firestore سيفشل حتماً حتى يتم استبدال `google-services.json` بملف رسمي صادر من لوحة تحكم Firebase لمشروع تم تفعيله وتوثيق بصمات SHA-1 و SHA-256 الخاصة به.

---

## 9. مصفوفة الصلاحيات الشاملة لكافة الأطراف (Permissions Matrix)

تلخص المصفوفة التالية الصلاحيات الممنوحة لكل طرف وفقاً لملف `/firestore.rules` الحالي:

| المسار في Firestore | زائر غير مسجل (Guest) | مستخدم عادي (نفسه) | مستخدم عادي (حساب آخر) | مشارك بمحادثة | غير مشارك بمحادثة | مسؤول نظام (`isAdmin()`) | الباك إند (عبر توكن عميل) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `/admins/{uid}` | ❌ DENY | 👁️ READ (لحسابه فقط) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/config/app` | ❌ DENY | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/config/search_order` | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/config/features` | ❌ DENY | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/config/economy` | ❌ DENY | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/users/{userId}` | ❌ DENY | ✅ READ / CREATE / UPDATE* | 👁️ READ (مكشوف بالكامل!) | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | ✅ نفس صلاحية العميل |
| `/users/{userId}/library/**` | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/users/{userId}/history/**` | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/users/{userId}/watched_episodes/**` | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/users/{userId}/fcmTokens/**` | ❌ DENY | ✅ FULL (مع تدقيق صارم) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/users/{userId}/point_transactions/**` | ❌ DENY | ✅ FULL (دون تدقيق نوعي!) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/users/{userId}/task_claims/**` | ❌ DENY | ✅ FULL (بما فيه الحذف!) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/managed_extensions/**` | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/notifications/**` | ❌ DENY | 👁️ READ (الكل) | 👁️ READ (الكل) | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/pro_requests/**` | ❌ DENY | ✍️ CREATE (PENDING) / READ (له) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/reward_tasks/**` | ❌ DENY | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/leaderboard/**` | ❌ DENY | 👁️ READ | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/reports/**` | ❌ DENY | ✍️ CREATE (pending) / READ (له) | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/support_conversations/{cId}` | ❌ DENY | ✅ READ / CREATE / UPDATE* | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/support_conversations/{cId}/messages/**` | ❌ DENY | ✍️ CREATE (`senderRole=user`) / READ | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/conversations/{cId}` | ❌ DENY | ❌ DENY (إن لم يكن مشاركاً) | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/conversations/{cId}/messages/**` | ❌ DENY | ❌ DENY (إن لم يكن مشاركاً) | ❌ DENY | ✍️ CREATE / READ / UPDATE (رسائله) | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |
| `/stories/**` | ❌ DENY | ✅ READ / CREATE / DELETE (قصصه) | 👁️ READ | 👁️ READ | 👁️ READ | ✅ FULL READ/WRITE | 👁️ READ |
| `/auditLogs/**` | ❌ DENY | ❌ DENY | ❌ DENY | ❌ DENY | ❌ DENY | ✅ FULL READ/WRITE | ❌ DENY |

*\*تنبيه: التعديل خاضع للثغرات الموثقة في سجل المخاطر أدناه.*

---

## 10. سجل المخاطر الأمنية المصنفة حسب الشدة (Security Risk Registry)

| معرف الخطر | الشدة (Severity) | السطح المستهدف | الوصف الجنائي للثغرة | الأثر الميداني على النظام |
| :--- | :---: | :--- | :--- | :--- |
| **VULN-01** | 🔴 **CRITICAL** | `/users/{userId}` (`allow update`) | عدم تضمين حقول الاشتراك (`isPremium`, `subscriptionTier`, `subscriptionStatus`, `subscriptionExpiresAt`) في قائمة الحظر. | يستطيع أي مستخدم منح نفسه اشتراك PRO دائم مجاناً دون دفع أو استبدال نقاط عبر استدعاء برمجي مباشر. |
| **VULN-02** | 🔴 **CRITICAL** | `/users/{userId}` (`allow update`) | عدم تضمين حقول النقاط (`pointsBalance`, `totalPointsEarned`) في قائمة الحظر لقواعد التعديل. | يستطيع المستخدم تعديل رصيده لأي رقم يريده وتزوير الثروة المالية داخل التطبيق. |
| **VULN-03** | 🟠 **HIGH** | `/users/{userId}/task_claims` | استخدام `allow write` الذي يتيح حذف وثائق المطالبة للمستخدم المالك. | يستطيع المستخدم حذف وثيقة المطالبة بمهمة منجزة وتكرار المطالبة بنقاطها بلا نهاية. |
| **VULN-04** | 🟠 **HIGH** | `/users/{userId}` (`allow read`) | السماح لأي مستخدم مسجل بقراءة وثائق كافة المستخدمين بالكامل (`allow read: if isAuthenticated()`). | تسريب بيانات حساسة: البريد الإلكتروني، رصيد النقاط، انتهاء الاشتراك، وسجلات الحظر وأسبابه. |
| **VULN-05** | 🟠 **HIGH** | `/conversations/.../messages` | حظر تحديث الرسائل لغير كاتبها الأصلي (`resource.data.senderId == request.auth.uid`). | تعطل ميزات التفاعل بالإيموجي (`reactToMessage`) والحذف الذاتي لدى المستقبل (`deleteMessage for self`) مع ظهور خطأ أمني. |
| **VULN-06** | 🟡 **MEDIUM** | `/support_conversations` | غياب التحقق من حقل `status` عند تعديل وثيقة الدعم من قبل المستخدم المالك. | يقوم تطبيق المستخدم بإعادة فتح التذاكر المغلقة والمحلولة تلقائياً (`status = open`) عند كل رسالة جديدة، وإمكانية تلاعب المستخدم بحالة التذكرة. |
| **VULN-07** | 🟡 **MEDIUM** | حظر المستخدمين الاجتماعي | اقتصار حظر المستخدمين على DataStore المحلي وغيابه التام عن قواعد فايرستور. | يستطيع المستخدم المحظور الاستمرار في إرسال رسائل جديدة للمستخدم الذي قام بحظره عبر Firestore. |
| **VULN-08** | 🟢 **LOW** | `/users/{userId}/point_transactions` | غياب فحص سلامة البيانات (Schema validation) على سجل العمليات المالية الفرعي. | يستطيع المستخدم إدراج قيود مالية وهمية في سجله الشخصي دون تدقيق لقيمتها أو نوعها. |
| **VULN-09** | 🟢 **LOW** | `idempotency_records` | وجود المجموعة في إعدادات الباك إند (`wrangler.toml:15`) وغياب أي قاعدة لها في `firestore.rules`. | سيفشل الخادم الموثوق في كتابة سجلات منع التكرار إذا استخدم صلاحيات Firestore العادية بدلاً من حساب الخدمة. |

---

## 11. المهمة السادسة: مصفوفة الاختبارات الأمنية غير المدمرة (Security Test Matrix)

### 11.1 تدقيق حالة الاختبارات الحالية في المستودع
- **اختبارات قواعد Firestore الحالية:** **غير موجودة إطلاقاً (ZERO)**. لا يوجد ملف `firestore.test.js` ولا اختبارات `*RuleTest.kt`.
- **محاكي Firebase Emulator:** غير مشغّل حالياً في البيئة ولا توجد إعدادات `firebase.json` لربطه بالقواعد.
- **اختبارات الباك إند:** ملف `backend/test/backend_suite.test.js` يحتوي على 41 اختباراً لمحرك الذاكرة الوهمي (`TestEconomicEngine`)، وهي اختبارات Node.js داخلية لا تختبر قواعد أمان فايرستور إطلاقاً.

### 11.2 مصفوفة الاختبارات المقترحة (Proposed Rules Test Matrix)
تحدد هذه المصفوفة السيناريوهات الأمنية الواجب تنفيذها لاحقاً في محاكي فايرستور للتحقق من سلامة القواعد دون لمس بيئة الإنتاج:

| معرف الفحص | هوية الفاعل (Actor) | المسار المستهدف | نوع العملية | البيانات المرسلة / الحقول المعدلة | النتيجة المتوقعة | السند الأمني ورقم السطر في القواعد | حالة التنفيذ الميداني |
| :--- | :--- | :--- | :---: | :--- | :---: | :--- | :---: |
| **SEC-01** | زائر غير مسجل (Guest) | `/config/search_order` | Read | لا توجد | **ALLOW** | قراءة عامة للترتيب (`firestore.rules:34`) | 💡 مقترح (لم ينفذ) |
| **SEC-02** | زائر غير مسجل (Guest) | `/users/user_123` | Read | لا توجد | **DENY** | اشتراط المصادقة (`firestore.rules:58`) | 💡 مقترح (لم ينفذ) |
| **SEC-03** | مستخدم عادي (User A) | `/admins/user_A` | Create | `{"uid": "user_A", "enabled": true}` | **DENY** | الكتابة مشروطة بـ `isAdmin()` (`firestore.rules:23`) | 💡 مقترح (لم ينفذ) |
| **SEC-04** | مستخدم عادي (User A) | `/users/user_A` | Create | `{"uid": "user_A", "role": "admin"}` | **DENY** | اشتراط `role == 'user'` بالإنشاء (`firestore.rules:63`) | 💡 مقترح (لم ينفذ) |
| **SEC-05** | مستخدم عادي (User A) | `/users/user_A` | Update | `{"role": "admin"}` | **DENY** | حظر حقل `role` بالتعديل (`firestore.rules:100`) | 💡 مقترح (لم ينفذ) |
| **SEC-06** | مستخدم عادي (User A) | `/users/user_A` | Update | `{"isBanned": false, "canChat": true}` | **DENY** | حظر حقول الحظر (`firestore.rules:102-104`) | 💡 مقترح (لم ينفذ) |
| **SEC-07** | مستخدم عادي (User A) | `/users/user_A` | Update | `{"isPremium": true, "subscriptionTier": "PRO"}` | **ALLOW (ثغرة!)** | غير محظورة في القواعد الحالية (`firestore.rules:97-106`) | 💡 مقترح (لم ينفذ) |
| **SEC-08** | مستخدم عادي (User A) | `/users/user_A` | Update | `{"pointsBalance": 500000}` | **ALLOW (ثغرة!)** | غير محظورة في القواعد الحالية (`firestore.rules:97-106`) | 💡 مقترح (لم ينفذ) |
| **SEC-09** | مستخدم عادي (User A) | `/users/user_B` | Read | لا توجد | **ALLOW (تسريب!)** | قراءة الوثيقة عامة للمسجلين (`firestore.rules:58`) | 💡 مقترح (لم ينفذ) |
| **SEC-10** | مستخدم عادي (User A) | `/users/user_B` | Update | `{"displayName": "Hacked"}` | **DENY** | اشتراط `isOwner(user_B)` (`firestore.rules:98`) | 💡 مقترح (لم ينفذ) |
| **SEC-11** | مستخدم عادي (User A) | `/users/user_B/history/h1` | Read | لا توجد | **DENY** | اشتراط `isOwner(user_B)` (`firestore.rules:117`) | 💡 مقترح (لم ينفذ) |
| **SEC-12** | مستخدم عادي (User A) | `/users/user_A/task_claims/t1` | Delete | لا توجد | **ALLOW (ثغرة!)** | `allow write` تتيح الحذف للمالك (`firestore.rules:133`) | 💡 مقترح (لم ينفذ) |
| **SEC-13** | غير مشارك (User C) | `/conversations/A_B` | Read | لا توجد | **DENY** | اشتراط التواجد في `participants` (`firestore.rules:309`) | 💡 مقترح (لم ينفذ) |
| **SEC-14** | غير مشارك (User C) | `/conversations/A_B/messages/m1` | Create | `{"senderId": "user_C", "text": "Hi"}` | **DENY** | اشتراط التواجد في `participants` (`firestore.rules:331`) | 💡 مقترح (لم ينفذ) |
| **SEC-15** | مشارك (User A) | `/conversations/A_B/messages/m1` | Create | `{"senderId": "user_B", "text": "Spoof"}` | **DENY** | اشتراط `senderId == request.auth.uid` (`firestore.rules:330`) | 💡 مقترح (لم ينفذ) |
| **SEC-16** | مستخدم محظور من الدردشة | `/conversations/A_B/messages/m2` | Create | `{"senderId": "user_A", "text": "Spam"}` | **DENY** | فحص `canChat` و `chatBan` (`firestore.rules:332-333`) | 💡 مقترح (لم ينفذ) |
| **SEC-17** | مشارك (User B) | `/conversations/A_B/messages/m_A` | Update | `{"reactions": {"user_B": "❤️"}}` | **DENY (خلل!)** | اشتراط `senderId == auth.uid` للمسج (`firestore.rules:337`) | 💡 مقترح (لم ينفذ) |
| **SEC-18** | مستخدم عادي (User A) | `/support_conversations/user_B` | Read | لا توجد | **DENY** | اشتراط `userId == auth.uid` (`firestore.rules:262`) | 💡 مقترح (لم ينفذ) |
| **SEC-19** | مستخدم عادي (User A) | `/support_conversations/user_A/messages/m1` | Create | `{"senderId": "user_A", "senderRole": "admin"}` | **DENY** | اشتراط `senderRole == 'user'` للعميل (`firestore.rules:296`) | 💡 مقترح (لم ينفذ) |
| **SEC-20** | مستخدم عادي (User A) | `/support_conversations/user_A` | Update | `{"status": "open"}` على تذكرة محلولة | **ALLOW (خلل!)** | عدم حظر تعديل `status` (`firestore.rules:272-277`) | 💡 مقترح (لم ينفذ) |
| **SEC-21** | مستخدم عادي (User A) | `/pro_requests/req_1` | Create | `{"status": "APPROVED", "userId": "user_A"}` | **DENY** | اشتراط `status == 'PENDING'` (`firestore.rules:217`) | 💡 مقترح (لم ينفذ) |
| **SEC-22** | مستخدم عادي (User A) | `/auditLogs/log_1` | Read | لا توجد | **DENY** | مقيد بـ `isAdmin()` فقط (`firestore.rules:355`) | 💡 مقترح (لم ينفذ) |
| **SEC-23** | مستخدم عادي (User A) | `/audit_logs/log_1` | Read/Write | أي بيانات | **DENY** | حظر شامل للمسار القديم (`firestore.rules:360`) | 💡 مقترح (لم ينفذ) |
| **SEC-24** | مسؤول نظام مفعل | `/admins/user_new` | Create | `{"uid": "user_new", "enabled": true}` | **ALLOW** | يحقق دالة `isAdmin()` (`firestore.rules:23`) | 💡 مقترح (لم ينفذ) |
| **SEC-25** | مسؤول نظام مفعل | `/config/app` | Update | `{"maintenanceEnabled": true}` | **ALLOW** | يحقق دالة `isAdmin()` (`firestore.rules:29`) | 💡 مقترح (لم ينفذ) |

---

## 12. خارطة الإصلاحات الأمنية المقترحة لمرحلة PHASE 20.1-B (دون تطبيق)

*تنبيه: هذه مقترحات تصميمية وتوصيات هندسية فقط ولم يتم تطبيق أي سطر منها التزاماً بشرط التدقيق الصارم.*

### 12.1 إغلاق ثغرة الترقية وتزوير النقاط في وثيقة المستخدم
إعادة كتابة قاعدة التعديل في `/users/{userId}` لإدراج كافة حقول الاقتصاد والاشتراك في قائمة الحظر الصارم للمستخدم العادي:
```javascript
// مقترح لقاعدة التعديل في PHASE 20.1-B
allow update: if isAdmin() || (
  isOwner(userId) &&
  !request.resource.data.diff(resource.data).affectedKeys()
    .hasAny([
      'role', 'admin', 'isAdmin',
      'isPremium', 'subscriptionTier', 'subscriptionStatus', 'subscriptionExpiresAt',
      'plan', 'planId', 'durationDays', 'subscriptionSource', 'subscriptionReferenceId',
      'subscriptionStartedAt', 'proPlan', 'proExpiresAt', 'isPro',
      'pointsBalance', 'totalPointsEarned', 'totalPointsSpent',
      'deviceLimit', 'maxDevices', 'allowedQuality', 'downloadLimit',
      'isActive', 'isBanned', 'banReason', 'banExpiresAt',
      'canWatch', 'canDownload', 'canChat', 'canStory', 'canP2P',
      'watchBan', 'downloadBan', 'chatBan', 'storyBan', 'p2pBan',
      'offlineDaysOverride', 'forcedAdsOverride'
    ])
);
```

### 12.2 حماية مطالبات المهام وسجلات النقاط من التلاعب والحذف
تقييم العمليات في `task_claims` و `point_transactions` بحيث يُمنع الحذف أو التعديل من قبل العميل:
```javascript
// مقترح لقاعدة مطالبات المهام في PHASE 20.1-B
match /task_claims/{claimId} {
  allow read: if isOwner(userId) || isAdmin();
  allow create: if isOwner(userId) && request.resource.data.userId == userId;
  allow update, delete: if isAdmin(); // يمنع العميل من حذف المطالبة لتكرارها
}

match /point_transactions/{txId} {
  allow read: if isOwner(userId) || isAdmin();
  allow create: if isOwner(userId) && request.resource.data.userId == userId;
  allow update, delete: if isAdmin(); // سجل مالي ثابت لا يُعدل ولا يُحذف
}
```

### 12.3 معالجة التفاعل والحذف الذاتي في المحادثات الاجتماعية
السماح للمستقبل بتحديث حقلي `reactions` و `deletedFor` فقط دون المساس بنص الرسالة:
```javascript
// مقترح لمعالجة تفاعل وحذف الرسائل في PHASE 20.1-B
allow update: if isAuthenticated() && (
  isAdmin() ||
  // المالك الأصلي يستطيع تعديل النص وحذف الرسالة للجميع
  (resource.data.senderId == request.auth.uid && !request.resource.data.diff(resource.data).affectedKeys().hasAny(['senderId', 'timestamp'])) ||
  // الطرف الآخر يستطيع فقط إضافة تفاعل أو حذف لنفسه
  (request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.participants &&
   request.resource.data.diff(resource.data).affectedKeys().hasOnly(['reactions', 'deletedFor']))
);
```

### 12.4 حماية حالة تذاكر الدعم الفني من إعادة الفتح العبثي
منع العميل من تغيير حقل `status` عند التعديل وحصره بالإدارة فقط:
```javascript
// مقترح لقاعدة تعديل تذكرة الدعم في PHASE 20.1-B
allow update: if isAdmin() || (
  isAuthenticated() &&
  resource.data.userId == request.auth.uid &&
  request.resource.data.userId == resource.data.userId &&
  (!('createdAt' in request.resource.data) || request.resource.data.createdAt == resource.data.createdAt) &&
  !request.resource.data.diff(resource.data).affectedKeys().hasAny(['status', 'adminNotes', 'assignedTo'])
);
```

### 12.5 عزل البيانات الحساسة للمستخدمين لمنع التسريب
إنشاء مجموعة فرعية أو وثيقة منفصلة للملف الشخصي العام (`/users/{uid}/public/profile`) بحيث تكون البيانات العامة فقط قابلة للقراءة لجميع المسجلين، بينما تبقى بيانات الحساب الحساسة (البريد، النقاط، الاشتراكات، الحظر) مقتصرة بالقراءة على المالك والإدارة فقط:
`allow read: if isOwner(userId) || isAdmin();`

---

## 13. المتطلبات الإلزامية الواجب حسمها قبل بدء PHASE 20.1-B

قبل الشروع في كتابة أو نشر أي تعديل على القواعد في المرحلة القادمة، يجب حسم القرارات المعمارية التالية مع مالك النظام:

1. **حسم هوية مشروع Firebase الإنتاجي:**
   - استخراج ملف `google-services.json` الرسمي المربوط بالمشروع النهائي المعتمد وتنزيله لمجلد `app/`.
   - توحيد معرف المشروع عبر المكونات الثلاثة (تطبيق أندرويد، وعميل الويب، وخادم Cloudflare).
2. **حسم مصير الاقتصاد المؤقت (Temporary Economy Mode):**
   - هل سيستمر العمل بوضع الاقتصاد المؤقت المنفذ محلياً في العميل لفترة انتقالية؟
   - إذا كان الجواب نعم: هل يُقبل الخطر الأمني للترقية الذاتية أم يتم تطبيق حظر الترقية وحصرها بالباك إند فقط؟
   - التوصية الهندسية: **إغلاق إمكانية الترقية الذاتية وتعديل النقاط فورياً**، وحصر ترقية الاشتراكات وتوزيع النقاط بالباك إند الموثوق (`backend/`) عبر استدعاءات آمنة تتحقق من الهوية على الخادم.
3. **تأسيس وثيقة المسؤول الأول يدوياً في Firestore Console:**
   - المسار: `/admins/{owner_uid}`
   - البيانات: `{"uid": "OWNER_UID", "email": "sulopros01@gmail.com", "role": "superadmin", "enabled": true}`.
   - هذا الإجراء إلزامي لكي تعمل دوال `isAdmin()` بنجاح.
4. **تأسيس وثائق الإعدادات الافتراضية:**
   - إنشاء وثيقة `/config/app` و `/config/search_order` و `/config/features` و `/config/economy` لضمان عدم توقف التطبيق عند تفعيل القواعد الجديدة المقيدة.

---

## 14. قائمة التحقق الصارمة لإثبات عدم إجراء أي تعديل أو نشر (Verification Checklist)

تؤكد هذه القائمة الامتثال الكامل والتام لكافة القيود المفروضة في هذه المرحلة:

- [x] **لم يتم تعديل أي ملف برمجي في التطبيق (`app/src/` لم يتغير).**
- [x] **لم يتم تعديل ملف قواعد الأمان (`/firestore.rules` لم يتغير).**
- [x] **لم يتم تعديل ملفات التهيئة (`google-services.json`, `wrangler.toml`, `.env.example` لم تتغير).**
- [x] **لم يتم نشر أي شيء إلى Firebase (لم يتم استدعاء `deploy_firebase` أو `firebase deploy`).**
- [x] **لم يتم نشر أي شيء إلى Cloudflare (لم يتم تنفيذ أي أمر لـ `wrangler`).**
- [x] **لم يتم إنشاء أي مسؤول أو تعديل UID أو ترقية صلاحيات أي حساب على أي قاعدة بيانات.**
- [x] **لم يتم تغيير أسماء المجموعات أو الحقول في Firestore.**
- [x] **لم يتم ادعاء تنفيذ اختبارات ميدانية حية على محاكيات غير متوفرة.**
- [x] **تم توثيق كافة الأدلة بأرقام الأسطر والملفات المصدرية بدقة جنائية.**
- [x] **لم يتم البدء في المرحلة 20.1-B.**

---

**نهاية تقرير التدقيق الأمني الجنائي — تم رفع التقرير وتوثيق الأدلة بنجاح تمهيداً لقرارات المالك الهندسية.**
