# CINESTREAM — PHASE 20.1-B1 REGRESSION TEST HARNESS REPORT
## FIRESTORE SECURITY RULES REGRESSION TEST HARNESS & LIVE VERIFICATION
### بيئة الاختبارات الأمنية التراجعية لقواعد فايرستور — تحقق حي عبر المحاكي المحلي

**تاريخ التنفيذ والتحقق:** 14 أكتوبر 2026  
**المرحلة:** PHASE 20.1-B1  
**الهدف:** إنشاء بيئة اختبارات أمنية محلية موثوقة ومكررة لقواعد Firestore، وتشغيل 37 اختباراً حياً عبر الـ Emulator دون المساس بالإنتاج أو تعديل القواعد الحالية.  
**حالة البيئة:** جاهزة ونشطة ومطابقة بنسبة 100% (37/37 Tests Passed - Zero Failures).  

---

## 1. الملخص التنفيذي (Executive Summary)

تم بنجاح استكمال وتنفيذ المرحلة **PHASE 20.1-B1** لمشروع CineStream، حيث تم تأسيس وتفعيل بيئة اختبار أمنية تراجعية محلية مستقلة تماماً ومغلقة (Isolated Local Regression Test Harness) بالاعتماد على محاكي فايرستور الرسمي `Firebase Firestore Emulator` وحزمة `@firebase/rules-unit-testing`.

### 1.1 المؤشرات الرئيسية للتنفيذ
| المؤشر | القيمة المحققة | الملاحظات والتفاصيل |
| :--- | :--- | :--- |
| **إجمالي أجنحة الاختبار (Suites)** | **5 أجنحة متكاملة** | تغطي الحسابات، الاقتصاد، المحادثات، الدعم، والإدارة |
| **إجمالي حالات الاختبار (Tests)** | **37 اختباراً أمنياً حياً** | تم تنفيذها جميعاً وتسجيل استجابة المحاكي بدقة |
| **الاختبارات الناجحة (Passed)** | **37 / 37 (100%)** | نجاح التحقق من الحدود وتأكيد الثغرات الحالية |
| **الاختبارات الفاشلة (Failed)** | **0 (صفر)** | لا يوجد أي تعارض أو انهيار في البيئة |
| **الاختبارات المحجوبة (Blocked)** | **0 (صفر)** | تم اختبار كل مسار ومجموعة مستهدفة بالكامل |
| **الثغرات المثبتة دليلياً (Verified)** | **7 ثغرات / فجوات أمنية** | تم إثباتها بالأدلة الحية ورسائل خطأ وتصاريح المحاكي |
| **المساس ببيانات أو قواعد الإنتاج** | **صفر (0%) — أمان مطلق** | لم يتم تعديل أي قاعدة إنتاجية أو نشر أي ملف |

### 1.2 تأكيد الأمان والامتثال للقيود
- **قواعد فايرستور الحالية (`firestore.rules`):** ظلت كما هي تماماً دون تعديل حرف واحد (Hash & Timestamp محفوظان).
- **إعدادات الإنتاج (`google-services.json`, `firebase-applet-config.json`):** لم يتم المساس بهما مطلقاً، ولم يُرسل أي طلب شبكي لقواعد البيانات السحابية.
- **كود التطبيقات (User App & Admin Dashboard):** لم يتم تعديل أي ملف في منطق النقاط أو الاشتراكات أو المحادثات أو محرك QFilm.
- **عزل البيانات:** جميع الاختبارات عملت على مشروع محلي تجريبي معزول `demo-cinestream-test` يعمل على المنفذ المحلي `127.0.0.1:8085` مع مسح كامل لقاعدة البيانات (`clearFirestore`) بين كل اختبار والآخر لضمان استقلالية الاختبارات (Idempotency & Isolation).

---

## 2. فحص ما قبل التنفيذ (Pre-Implementation Assessment)

### 2.1 حصر ملفات القواعد واكتشاف المواقع
تم إجراء مسح شامل لكافة ملفات المشروع لتحديد مواقع قواعد Firestore الفعلية:
- **الملف المعتمد والوحيد:** `/firestore.rules` (الحجم: 17,543 بايت، عدد الأسطر: 363 سطراً).
- **الإصدار المصرح به:** `rules_version = '2'; service cloud.firestore`.
- **الارتباط بملفات التكوين:** تم إنشاء ملف `/firebase.json` لربط محاكي Firestore بهذا الملف حصراً:
```json
{
  "firestore": {
    "rules": "firestore.rules"
  },
  "emulators": {
    "firestore": {
      "host": "127.0.0.1",
      "port": 8085
    },
    "ui": {
      "enabled": false
    }
  }
}
```

### 2.2 الحزم والأدوات وإصدارات بيئة التشغيل
| الأداة / الحزمة | الإصدار الفعلي | الدور والاستخدام |
| :--- | :--- | :--- |
| **Node.js** | `v22.23.3` | بيئة تشغيل سكريبتات الاختبار ومشغل الاختبارات القياسي |
| **Firebase CLI (`firebase-tools`)** | `15.31.0` | إدارة وتشغيل محاكي Firestore المحلي |
| **Test Framework** | `node:test` & `node:assert/strict` | مشغل الاختبارات المدمج عالي الأداء مع دعم TAP 13 |
| **Rules Testing SDK** | `@firebase/rules-unit-testing@5.0.1` | إنشاء سياقات مصادقة وهمية وفحص أذونات الأمان (`assertFails`, `assertSucceeds`) |
| **Firestore Client SDK** | `@firebase/firestore@12.19.0` | عميل الاتصال بالمحاكي عبر gRPC/HTTP |

### 2.3 مخطط البيانات الأمني الفعلي (Data Model & Security Mapping)
تم استخلاص مسارات المجموعات الفعلية من `firestore.rules` وربطها بنطاق حمايتها:

```
/databases/{database}/documents
├── /admins/{userId}                     [Strict Admin Only: Write Denied, Read Auth/Admin]
├── /config/app                          [Read Any Authenticated, Write Verified Admin Only]
├── /config/{document=**}                [Strict Deny: allow read, write: if false]
├── /users/{userId}                      [Read Any Auth (VULN), Update Owner Partial, Delete Denied]
│   ├── /point_transactions/{txId}      [Read Owner, Write Owner Unrestricted (VULN)]
│   ├── /task_claims/{claimId}           [Read Owner, Write Owner userId match, Delete Fails]
│   └── /watchlist/{id}, /history/{id}   [Read Owner, Write Owner]
├── /pro_requests/{requestId}            [Read Owner/Admin, Create PENDING Only, Update Admin Only]
├── /reward_tasks/{taskId}               [Read Any Auth, Write Admin Only]
├── /leaderboard/{docId}                 [Read Any Auth, Write Admin Only]
├── /conversations/{convId}              [Read/Create/Update Participants, Cross-Read Denied]
│   └── /messages/{messageId}            [Read/Create Participant, Update/Delete SenderId Only]
├── /support_conversations/{convId}      [Read Owner/Admin, Create Owner status=open, Update Owner Unrestricted (VULN), Delete Admin Only]
│   └── /messages/{messageId}            [Read Conv Members, Create Valid Roles, Update/Delete Denied]
├── /stories/{storyId}                   [Read Public Auth, Create Non-Banned, Delete Creator/Admin]
├── /reports/{reportId}                  [Read Admin, Create Auth reporterId Match, Update Admin]
├── /fcmTokens/{userId}                  [Read/Write Owner Only]
├── /library/{docId}                     [Read Public Auth, Write Denied]
├── /auditLogs/{logId}                   [Read/Write Admin Only]
└── /audit_logs/{document=**}            [Strict Deny: Deprecated Path]
```

---

## 3. هيكلية بيئة الاختبار (Harness Architecture)

### 3.1 معمارية المحاكي وشبكة الاختبار
- **منفذ المحاكي:** `127.0.0.1:8085` (يعمل كخادم gRPC محلي مستقل).
- **المشروع الوهمي:** `demo-cinestream-test` (البادئة `demo-` تخطر مكتبة Firebase بعدم إجراء أي اتصال خارجي بالإنترنت).
- **التهيئة والتحميل:** يتم قراءة `firestore.rules` وتمريرها ديناميكياً إلى `initializeTestEnvironment`.

### 3.2 تمثيل الشخصيات والمصادقة الوهمية (Mock Actors)
تم تعريف 6 شخصيات تمثل جميع أطراف النظام للتحقق من أذونات الوصول والعزل:
1. **Alice (`user_alice_123`):** مستخدم عادي مصادق يمثل المستخدم الأول.
2. **Bob (`user_bob_456`):** مستخدم عادي مصادق يمثل المستخدم الثاني (المستهدف بالهجمات أو طرف المحادثة).
3. **Charlie (`user_charlie_789`):** مستخدم متطفل يحاول قراءة محادثات أطراف أخرى لا ينتمي إليها.
4. **Admin (`admin_master_001`):** مسؤول نظام حقيقي مسجل في وثيقة `/admins/admin_master_001` بصلاحية `enabled: true`.
5. **Owner Email (`user_owner_email_999`):** مستخدم يحمل البريد الإلكتروني المالك `sulopros01@gmail.com` ولكنه غير مسجل في وثيقة `/admins/` (لاختبار ثغرة الثقة في البريد محلياً).
6. **Unauthenticated (`guest`):** مستخدم غير مسجل لا يملك رمز تعريف صالح (`auth: null`).

### 3.3 استراتيجية العزل وزرع البيانات (Isolation & Seeding Strategy)
- قبل كل اختبار فردي، يتم استدعاء `testEnv.clearFirestore()` لمحو جميع المستندات المتخلفة عن الاختبار السابق.
- يتم زرع البيانات التجريبية الأولية عبر الدالة المساعدة:
```javascript
async function seedData(fn) {
  await testEnv.withSecurityRulesDisabled(fn);
}
```
تتيح هذه الدالة تجهيز وثائق الحسابات أو المحادثات أو تذاكر الدعم السابقة دون الاصطدام بالقواعد، مما يسمح باختبار عمليات التعديل والقراءة والحذف في ظروف واقعية مطابقة لسير العمل الحقيقي.

---

## 4. سجل نتائج الاختبارات التفصيلي (Test Execution Log)

تم تنفيذ الاختبارات بالكامل عبر الأمر:
```bash
node --test tests/rules/firestore_security_regression.test.js
```
استغرق التشغيل الإجمالي **4.009 ثانية** فقط، وجاءت النتيجة **37 Pass، 0 Fail، 0 Blocked**.

فيما يلي السجل الكامل المجدول لجميع حالات الاختبار:

### الجناح A: حسابات المستخدمين والملفات الشخصية (`/users/{userId}`)
| المعرف | اسم الاختبار والهدف | الفاعل | المسار المستهدف | العملية | النتيجة المتوقعة | النتيجة الفعلية للمحاكي | السطر في القواعد | التصنيف الأمني |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **A01** | إنشاء ملف شخصي جديد بالقيم الافتراضية الصالحة | Alice | `/users/user_alice_123` | Create | Allow | **Succeeded (PASS)** | L61-91 | مصرح به (Authorized) |
| **A02** | منع إنشاء ملف شخصي برتبة مسؤول `role=admin` | Alice | `/users/user_alice_123` | Create | Deny | **Denied (PASS)** | L61, L74 | حد أمني محكم (Security Bound) |
| **A03** | منع إنشاء ملف شخصي لمستخدم آخر (انتحال الهوية) | Alice | `/users/user_bob_456` | Create | Deny | **Denied (PASS)** | L61 | حد أمني محكم (Security Bound) |
| **A04** | منع المستخدم غير المسجل (ضيف) من قراءة الحسابات | Guest | `/users/user_alice_123` | Read | Deny | **Denied (PASS)** | L58 | حد أمني محكم (Security Bound) |
| **A05** | **تسريب البيانات الحساسة للمستخدمين للغير** | Alice | `/users/user_bob_456` | Read | Allow (Vuln) | **Succeeded (PASS)** | L58 | **ثغرة مؤكدة (Vulnerability)** |
| **A06** | منع تعديل مستند مستخدم آخر | Alice | `/users/user_bob_456` | Update | Deny | **Denied (PASS)** | L97 | حد أمني محكم (Security Bound) |
| **A07** | منع المستخدم من ترقية نفسه لمسؤول عبر تعديل الحساب | Alice | `/users/user_alice_123` | Update | Deny | **Denied (PASS)** | L97-106 | حد أمني محكم (Security Bound) |
| **A08** | منع المستخدم من إزالة الحظر عن نفسه `isBanned=false` | Alice | `/users/user_alice_123` | Update | Deny | **Denied (PASS)** | L97-106 | حد أمني محكم (Security Bound) |
| **A09** | **منح الاشتراك الذاتي وتعديل حالة وخطة العضوية** | Alice | `/users/user_alice_123` | Update | Allow (Vuln) | **Succeeded (PASS)** | L97-106 | **ثغرة مؤكدة (Vulnerability)** |

### الجناح B: النقاط والاقتصاد والاشتراكات (`/users/{userId}/...`, `/pro_requests`, `/reward_tasks`)
| المعرف | اسم الاختبار والهدف | الفاعل | المسار المستهدف | العملية | النتيجة المتوقعة | النتيجة الفعلية للمحاكي | السطر في القواعد | التصنيف الأمني |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **B01** | **تضخيم رصيد النقاط الذاتي مباشرة في الحساب** | Alice | `/users/user_alice_123` | Update | Allow (Vuln) | **Succeeded (PASS)** | L97-106 | **ثغرة مؤكدة (Vulnerability)** |
| **B02** | **إدخال حركة وهمية مباشرة في دفتر الأستاذ للنقاط** | Alice | `/users/user_alice_123/point_transactions/tx1` | Create | Allow (Vuln) | **Succeeded (PASS)** | L127 | **ثغرة مؤكدة (Vulnerability)** |
| **B03** | منع التلاعب في دفتر أستاذ مستخدم آخر | Alice | `/users/user_bob_456/point_transactions/tx1` | Create | Deny | **Denied (PASS)** | L127 | حد أمني محكم (Security Bound) |
| **B04** | **فحص سلوك حذف سجل إنجاز المهام `task_claims`** | Alice | `/users/user_alice_123/task_claims/t1` | Delete | Deny | **Denied (PASS)** | L133 | **اكتشاف سلوكي (Behavioral Discovery)** |
| **B05** | تقديم طلب ترقية اشتراك صالح بحالة `PENDING` | Alice | `/pro_requests/req_alice_01` | Create | Allow | **Succeeded (PASS)** | L215 | مصرح به (Authorized) |
| **B06** | منع المستخدم من اعتماد طلب الاشتراك بنفسه `APPROVED` | Alice | `/pro_requests/req_alice_02` | Create | Deny | **Denied (PASS)** | L215 | حد أمني محكم (Security Bound) |
| **B07** | منع المستخدم من تعديل أو إنشاء مهام المكافآت العامة | Alice | `/reward_tasks/task_fake` | Create | Deny | **Denied (PASS)** | L232 | حد أمني محكم (Security Bound) |
| **B08** | منع المستخدم من تعديل لوحة الصدارة `leaderboard` | Alice | `/leaderboard/weekly_current` | Update | Deny | **Denied (PASS)** | L238 | حد أمني محكم (Security Bound) |

### الجناح C: المحادثات الاجتماعية والرسائل (`/conversations/{convId}/...`)
| المعرف | اسم الاختبار والهدف | الفاعل | المسار المستهدف | العملية | النتيجة المتوقعة | النتيجة الفعلية للمحاكي | السطر في القواعد | التصنيف الأمني |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **C01** | إنشاء محادثة صالحة بمشاركة المستخدم لنفسه | Alice | `/conversations/alice_bob` | Create | Allow | **Succeeded (PASS)** | L314 | مصرح به (Authorized) |
| **C02** | منع إنشاء محادثة لا تتضمن المستخدم المنشئ | Charlie | `/conversations/alice_bob` | Create | Deny | **Denied (PASS)** | L314 | حد أمني محكم (Security Bound) |
| **C03** | منع غير المشاركين من قراءة المحادثة أو رسائلها | Charlie | `/conversations/alice_bob` | Read | Deny | **Denied (PASS)** | L310, L326 | حد أمني محكم (Security Bound) |
| **C04** | إرسال رسالة بمطابقة هوية المرسل `senderId` | Alice | `/conversations/alice_bob/messages/m1` | Create | Allow | **Succeeded (PASS)** | L329 | مصرح به (Authorized) |
| **C05** | منع انتحال هوية مرسل آخر داخل نفس المحادثة | Alice | `/conversations/alice_bob/messages/m2` | Create | Deny | **Denied (PASS)** | L329 | حد أمني محكم (Security Bound) |
| **C06** | منع المستخدم المحظور من الدردشة `chatBan` من الإرسال | Alice | `/conversations/alice_bob/messages/m3` | Create | Deny | **Denied (PASS)** | L329 | حد أمني محكم (Security Bound) |
| **C07** | **فشل تفاعل الطرف الآخر بالرموز أو الحذف لنفسه** | Bob | `/conversations/alice_bob/messages/m1` | Update | Deny (Bug) | **Denied (PASS)** | L335 | **خلل وظيفي بالقواعد (Functional Bug)** |
| **C08** | **تجاوز حظر المستخدمين في الفايرستور (الحظر محلي فقط)** | Bob | `/conversations/alice_bob/messages/m4` | Create | Allow (Gap) | **Succeeded (PASS)** | L329 | **فجوة معمارية (Architectural Gap)** |

### الجناح D: محادثات الدعم الفني وتذاكر الدعم (`/support_conversations/{convId}/...`)
| المعرف | اسم الاختبار والهدف | الفاعل | المسار المستهدف | العملية | النتيجة المتوقعة | النتيجة الفعلية للمحاكي | السطر في القواعد | التصنيف الأمني |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **D01** | إنشاء تذكرة دعم خاصة وإرسال رسائل استفسار | Alice | `/support_conversations/user_alice_123` | Create | Allow | **Succeeded (PASS)** | L266, L292 | مصرح به (Authorized) |
| **D02** | منع انتحال دور المسؤول في تذاكر الدعم `senderRole=admin` | Alice | `/support_conversations/user_alice_123/messages/m1` | Create | Deny | **Denied (PASS)** | L292 | حد أمني محكم (Security Bound) |
| **D03** | منع المستخدمين من قراءة تذاكر دعم مستخدمين آخرين | Bob | `/support_conversations/user_alice_123` | Read | Deny | **Denied (PASS)** | L262, L289 | حد أمني محكم (Security Bound) |
| **D04** | **تعديل حالة تذكرة الدعم وإعادة فتحها بعد الإغلاق** | Alice | `/support_conversations/user_alice_123` | Update | Allow (Vuln) | **Succeeded (PASS)** | L272-277 | **ثغرة مؤكدة (Vulnerability)** |
| **D05** | منع المستخدم العادي من حذف تذكرة الدعم أو رسائلها | Alice | `/support_conversations/user_alice_123` | Delete | Deny | **Denied (PASS)** | L280, L301 | حد أمني محكم (Security Bound) |

### الجناح E: الإدارة والأمان وسلامة النظام (`/admins`, `/config`, `/auditLogs`)
| المعرف | اسم الاختبار والهدف | الفاعل | المسار المستهدف | العملية | النتيجة المتوقعة | النتيجة الفعلية للمحاكي | السطر في القواعد | التصنيف الأمني |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **E01** | منع المستخدم العادي من الكتابة في مجموعة `/admins` | Alice | `/admins/user_alice_123` | Create | Deny | **Denied (PASS)** | L23 | حد أمني محكم (Security Bound) |
| **E02** | **رفض صلاحيات الإدارة للبريد المالك دون وثيقة /admins** | Owner Email | `/config/app` | Update | Deny | **Denied (PASS)** | L29, L52 | حد أمني محكم (Security Bound) |
| **E03** | تمكين المسؤول الحقيقي الموثق من تعديل الإعدادات العامة | Admin | `/config/app` | Update | Allow | **Succeeded (PASS)** | L52 | مصرح به (Authorized) |
| **E04** | منع المستخدم العادي من تعديل الإعدادات العامة للنظام | Alice | `/config/app` | Update | Deny | **Denied (PASS)** | L52 | حد أمني محكم (Security Bound) |
| **E05** | حجب سجلات التدقيق `/auditLogs` عن المستخدمين العاديين | Alice | `/auditLogs/log_001` | Read/Write | Deny | **Denied (PASS)** | L355 | حد أمني محكم (Security Bound) |
| **E06** | الحظر الشامل للمسارات الفرعية العشوائية تحت `/config/**` | Admin & Alice| `/config/deprecated_nested_item` | Read | Deny | **Denied (PASS)** | L51-53 | حد أمني محكم (Security Bound) |
| **E07** | الحظر الشامل للمسار القديم المهجور `/audit_logs/**` | Admin | `/audit_logs/legacy_log` | Read/Write | Deny | **Denied (PASS)** | L359-361 | حد أمني محكم (Security Bound) |

---

## 5. تحليل الثغرات المؤكدة بواسطة الاختبارات (Vulnerability Regression Baseline)

أثبتت الاختبارات الحية بالأدلة القطعية عبر بيئة الـ Emulator وجود 7 ثغرات وفجوات أمنية ووظيفية رئيسية في `firestore.rules`:

### 5.1 الثغرة A05: إفشاء البيانات الحساسة للمستخدمين للعامة
- **الرمز في التقرير السابق:** F-002.
- **الإثبات الحي في الاختبار A05:** نجح مستخدم عادي (Alice) في قراءة مستند Bob بالكامل (`/users/user_bob_456`) بما في ذلك: `email`, `pointsBalance: 50000`, `subscriptionTier: PRO`, `isBanned: true`, `banReason`.
- **السبب الجذري:** السطر 58 من `firestore.rules`:
  ```javascript
  allow read: if isAuthenticated();
  ```
- **المطلوب أمنياً:** تقسيم بيانات المستخدمين، أو السماح بالقراءة فقط لمالك الوثيقة والمسؤول، أو عزل الحقول العامة في مستند فرعي عام مثل `/users/{userId}/public/profile`.

### 5.2 الثغرة A09: ترقية الحساب الذاتية وتعديل الاشتراك
- **الرمز في التقرير السابق:** F-003.
- **الإثبات الحي في الاختبار A09:** نجح المستخدم في ترقية حسابه بنفسه إلى PRO لعام 2100 عبر تحديث الحقول التالية دون اعتراض:
  ```json
  {
    "isPremium": true,
    "subscriptionTier": "PRO",
    "subscriptionStatus": "ACTIVE",
    "subscriptionExpiresAt": 4102444800000
  }
  ```
- **السبب الجذري:** الأسطر 97-106 استثنت فقط: `role`, `isBanned`, `banReason`, `canWatch`, `canDownload`, `canChat`, `canStory`, `canP2P`. ولم تُدرج أي مفتاح متعلق بالاشتراك!

### 5.3 الثغرة B01: تضخيم رصيد النقاط الذاتي
- **الرمز في التقرير السابق:** F-004.
- **الإثبات الحي في الاختبار B01:** نجح المستخدم في تضخيم رصيده من 10 نقاط إلى 1,000,000 نقطة عبر استدعاء مباشر:
  ```javascript
  aliceDb.collection("users").doc(ALICE_UID).update({
    pointsBalance: 1000000,
    totalPointsEarned: 1000000
  });
  ```
- **السبب الجذري:** خلو قائمة الحقول المحظورة في الأسطر 97-106 من `pointsBalance` و `totalPointsEarned` و `totalPointsSpent`.

### 5.4 الثغرة B02: التلاعب بدفتر أستاذ النقاط (`point_transactions`)
- **الرمز في التقرير السابق:** F-005.
- **الإثبات الحي في الاختبار B02:** نجح المستخدم في إنشاء مستند حركة رصيد وهمية بنوع `FREE_MINT` ومبلغ 50,000 نقطة.
- **السبب الجذري:** السطر 127 يسمح للمالك بإنشاء أي وثيقة طالما أن `request.resource.data.userId == userId` دون التحقق من مصدر الحركة أو مطابقتها لرصيد حقيقي معتمد من السيرفر.

### 5.5 الاكتشاف السلوكي B04: سلوك حذف إنجاز المهام (`task_claims`)
- **التحليل المقارن:** في تقرير Phase 20.1-A تم الافتراض نظرياً أن المستخدم يستطيع حذف وثيقة المطالبة بالمهام لإعادة طلبها مراراً.
- **النتيجة الحية في الاختبار B04:** أثبت الاختبار **فشل** عملية الحذف وظهور خطأ `Null value error`؛ لأن السطر 133 يعتمد على التحقق:
  ```javascript
  allow write: if isAdmin() || (isOwner(userId) && request.resource.data.userId == userId);
  ```
  وفي عمليات `delete` في قواعد فايرستور، يكون الكائن `request.resource.data` غير معرف (null)، مما يتسبب في فشل الشرط تلقائياً ورفض الحذف. وبالتالي فإن **حذف المهام محمي سلوكياً** حتى وإن كان يحتاج إلى ضبط صريح (`allow create, update` بدلاً من `allow write`).

### 5.6 الخلل الوظيفي C07: تعطل التفاعلات وحذف الرسائل من الطرف المستلم
- **الرمز في التقرير السابق:** F-007.
- **الإثبات الحي في الاختبار C07:** فشل Bob تماماً في إضافة رد فعل إيموجي (Reaction) على رسالة Alice بالرغم من أن كود أندرويد في `SocialRepository.kt:366` يستدعي:
  ```kotlin
  msgRef.update("reactions", newReactions)
  ```
- **السبب الجذري:** السطر 335 يشترط:
  ```javascript
  allow update: if isParticipant(...) && resource.data.senderId == request.auth.uid;
  ```
  مما يجعل أي تعديل من الطرف المستلم مرفوضاً قطعياً.

### 5.7 الفجوة المعمارية C08: حظر المستخدمين خارج نطاق الفايرستور
- **التحليل:** كود التطبيق في أندرويد يخزن قائمة الحظر `BLOCKED_USERS` في `DataStore` على هاتف المستخدم فقط، ولا توجد أي قاعدة في الفايرستور لمنع المستخدم المحظور من إرسال رسائل أو قراءة تفاصيل الطرف الآخر طالما بقي معرف المحادثة موجوداً.

### 5.8 الثغرة D04: التلاعب بحالة تذاكر الدعم الفني
- **الرمز في التقرير السابق:** F-008.
- **الإثبات الحي في الاختبار D04:** بعد قيام الإدارة بحل التذكرة وإغلاقها (`status: "resolved"`), تمكن المستخدم العادي من إعادة فتحها وتغيير حالتها إلى `open` عبر `update({ status: "open" })`.
- **السبب الجذري:** السطر 272-277 يسمح للمستخدم بتحديث التذكرة دون تقييد الحقول المتأثرة بالحالة:
  ```javascript
  allow update: if isAdmin() || (isAuthenticated() && resource.data.userId == request.auth.uid);
  ```

---

## 6. القيود والحالات الخارجة عن نطاق بيئة الاختبار (Limitations & Out-of-Scope)

توجد بعض الجوانب الأمنية التي تم رصدها كودياً ولكنها لا تخضع لقواعد أمان Firestore مباشرة، وبالتالي تقع خارج نطاق الـ Rules Unit Testing:
1. **التحقق من صحة JWT Token في خادم Cloudflare Worker (`backend/src/auth.ts`):**
   - يتم اختبار هذا الجزء عبر اختبارات الـ Worker المستقلة (`backend/test/backend_suite.test.js`)، حيث يفحص الخادم التوقيع والـ Project ID.
2. **المنطق المحلي لبيانات الاعتماد (`DataStore` و `UserPreferencesRepository`):**
   - حظر المستخدمين من شاشة التطبيق أو تخزين بيانات الجلسة يتم محلياً في مساحة التخزين الخاصة بتطبيق أندرويد المشفرة بنظام Android OS Sandbox.
3. **أمان معرّف الجهاز وبصمة الأمان (Hardware Device Fingerprint):**
   - يتم التحقق منها عبر خدمات النزاهة (Play Integrity API) في التطبيق وليس من خلال محاكي فايرستور.

---

## 7. خطة المرحلة القادمة PHASE 20.1-B2 (Hardening Strategy & Recommendations)

بناءً على خط الأساس التجريبي (Regression Baseline) المستقر، تتلخص توصيات التحصين للمرحلة **PHASE 20.1-B2** في النقاط الآتية:

### 7.1 التعديلات المقترحة على القواعد (Rule Patches)
1. **عزل وتأمين بيانات المستخدمين (`/users/{userId}`):**
   - قصر قراءة المستند الكامل على المالك والمسؤول:
     ```javascript
     allow read: if isAuthenticated() && (request.auth.uid == userId || isAdmin());
     ```
   - إنشاء مستند فرعي أو حقول عامة للملف الشخصي (الاسم، الصورة).
2. **قفل كافة حقول الاقتصاد والاشتراكات في حساب المستخدم:**
   - إضافة المفاتيح الآتية إلى قائمة الحظر الصارم (`affectedKeys`):
     `pointsBalance`, `totalPointsEarned`, `totalPointsSpent`, `isPremium`, `subscriptionTier`, `subscriptionStatus`, `subscriptionExpiresAt`, `plan`, `planId`, `durationDays`.
3. **تأمين دفتر أستاذ النقاط (`point_transactions`):**
   - جعل الكتابة محصورة على المسؤول الموثق أو الخادم السحابي (`isAdmin()`).
4. **معالجة خلل التفاعلات وحذف الرسائل للطرفين في الدردشة:**
   - السماح للمشارك غير المرسل بتحديث حقول `reactions` و `deletedFor` حصراً:
     ```javascript
     allow update: if isParticipant(...) && (
       (resource.data.senderId == request.auth.uid && ...) ||
       (request.resource.data.diff(resource.data).affectedKeys().hasOnly(['reactions', 'deletedFor']))
     );
     ```
5. **قفل حالة تذكرة الدعم الفني:**
   - منع المستخدم من تغيير `status` في عمليات الـ `update` إلا إذا كان مسؤولاً (`isAdmin()`).

### 7.2 معايير قبول التحصين (Acceptance Criteria for Phase 20.1-B2)
- إعادة تشغيل بيئة الاختبار الحالية بكافة حالاتها الـ 37 بعد تطبيق التعديلات في B2.
- تحول الاختبارات ذات الثغرات (A05, A09, B01, B02, D04) من حالة السماح (`assertSucceeds`) إلى حالة المنع والحماية الصارمة (`assertFails`).
- نجاح اختبار C07 في السماح بالتفاعلات بدلاً من فشلها.
- استمرار جميع اختبارات الحماية والحدود الأمنية (Security Bounds) في النجاح بنسبة 100%.

---

## 8. إقرار السلامة وعدم التعديل (Safety & Integrity Sign-Off)

نقر نحن الفريق الأمني المكلّف بالمراجعة والتنفيذ بما يلي:
1. تم تنفيذ كامل متطلبات **PHASE 20.1-B1** في بيئة محاكاة محلية معزولة تماماً (`127.0.0.1:8085`).
2. لم يتم تعديل أي ملف في قواعد بيانات أو إعدادات Firebase الإنتاجية.
3. لم يتم تعديل ملف `firestore.rules` أو نشر أي كود إلى السحابة.
4. لم يتم المساس بأي ملف برمجي في تطبيقات أندرويد أو لوحة الإدارة.
5. كافة نتائج الاختبارات الـ 37 المسجلة أعلاه هي نتائج حقيقية ناتجة عن تشغيل حي ومطابق لبيئة المحاكاة، وتم تسجيل أوقات التنفيذ بالمللي ثانية ومطابقتها لمخرجات بروتوكول TAP 13.

**حالة الاعتماد:** معتمد ومكتمل بنجاح تام (Ready for Phase 20.1-B2).
