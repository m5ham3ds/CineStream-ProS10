# CINESTREAM — PHASE 20.1-B3-C1 COMPREHENSIVE AUDIT REPORT
# COMPREHENSIVE USER APP, FIREBASE & ADMIN-CONTROL CONTRACT AUDIT
## STRICT READ-ONLY FORENSIC REPORT — NO CODE CHANGES — NO DEPLOYMENT

**Audit Timestamp:** October 15, 2026  
**Audited Target:** CineStream Android User App (`com.example`)  
**Execution Context:** Cloud-hosted Android Build & Analysis Environment  
**Execution Mode:** STRICT READ-ONLY (`NO CODE CHANGES`, `NO DEPLOYMENT`)  
**Output Document:** `PHASE_20.1-B3-C1_COMPREHENSIVE_USER_APP_FIREBASE_ADMIN_CONTRACT_AUDIT.md`  

---

## 1. Audit Scope, Limitations & Confidence Boundaries

### 1.1 Scope of Inspected Codebase & Resources
This technical audit was conducted strictly against the local filesystem of the **CineStream User App** repository. The examined codebase includes:
- **Presentation & ViewModels:** `app/src/main/java/com/example/ui/` (Auth, Profile, Social, Support, Player, Details, Share, Banned, Maintenance, Extensions).
- **Data Repositories & Managers:** `app/src/main/java/com/example/data/` (`UserSecurityManager.kt`, `AuthRepository.kt`, `SocialRepository.kt`, `PointsRepository.kt`, `PointsEarningRepository.kt`, `TemporaryFirebaseEconomyRepository.kt`, `EconomyConfigRepository.kt`, `AppStartupManager.kt`, `AppUpdateManager.kt`, `NotificationRepository.kt`, `NotificationPreferencesRepository.kt`, `ReportRepository.kt`, `SessionManager.kt`, `FcmTokenManager.kt`, `CloudSyncManager.kt`, `LibraryRepository.kt`, `HistoryRepository.kt`, `WatchedEpisodeRepository.kt`, `DownloadManagerService.kt`).
- **Data Models:** `app/src/main/java/com/example/data/model/` (`UserRestrictions.kt`, `AppConfig.kt`, `EconomyConfig.kt`, `FeaturesConfig.kt`, `PointWallet.kt`, `PointTransaction.kt`, `ProRequest.kt`, `RewardTask.kt`, `LeaderboardEntry.kt`, `NotificationItem.kt`, `SupportMessage.kt`, `CanonicalSubscriptionTier.kt`, `SubscriptionNormalizer.kt`).
- **Extension & Scraper Framework:** `app/src/main/java/com/example/extension/` (`ManagedMediaOrchestrator.kt`, `FirebaseFirestoreManagedExtensionDataSource.kt`, `ManagedExtensionRealtimeSyncManager.kt`, `ManagedExtensionDto.kt`, `SearchOrderDataSource.kt`).
- **Android Services & Background Receivers:** `app/src/main/java/com/example/services/` (`AppFirebaseMessagingService.kt`), `app/src/main/java/com/example/utils/` (`StreamDownloaderService.kt`, `AndroidDownloader.kt`, `MediaStorageUtils.kt`, `P2pWifiDirectManager.kt`).
- **Security Rules & Configurations:** `/firestore.rules` (v2 cloud security rules, 403 lines), `/firebase.json` (local emulator configuration).
- **Architecture Documentation:** `/docs/FIREBASE_CONTRACT.md`, previous phase reports in `/PHASES/`.
- **Test Harnesses:** `tests/rules/firestore_security_regression.test.js` (49 unit/regression test cases executing against local Firestore Emulator).

### 1.2 Out-of-Scope / Unreachable Components (Strict Isolation)
1. **Admin Dashboard Repository:** The CineStream Admin Dashboard is hosted in a completely distinct Google AI Studio session. In strict compliance with instructions, **zero assumptions or unverified claims** are made regarding its private internal files, implementation details, or unreported behaviors. All Admin capabilities documented herein are deduced exclusively from how the User App expects, consumes, validates, or rejects Admin data structures and Firestore mutations.
2. **Production Firebase Cloud Project:** Zero network calls or provisioning actions were made to production Firebase. No production databases, credentials, or users were altered.
3. **Cloudflare Worker / External Backend:** The production trusted economy endpoint (`trustedBackendUrl`) is currently unprovisioned or external; its server-side execution code was not directly inspected.

### 1.3 Confidence Levels
- **Static Code Analysis & In-App Routing:** `HIGH CONFIDENCE (CONFIRMED)`. Verifiable via exact Kotlin source code lines.
- **Firestore Security Rules Enforcement:** `HIGH CONFIDENCE (CONFIRMED)`. Validated by direct line citations and verified live against the local Firestore Emulator.
- **Third-Party CDN / External Media Server Constraints:** `MEDIUM-HIGH CONFIDENCE (CONFIRMED ARCHITECTURAL GAP)`. Source code proves that video streams and file downloads originate from external scraper hosts (e.g., Anime4Up, EgyDead, Akwam) that do not evaluate CineStream Firebase auth tokens.

---

## 2. Executive Synthesis: The Six Core Technical Questions

### Question 1: What functions can the app owner currently control via the admin dashboard, and how does each action reflect inside the user app?
The app owner controls the following operational dimensions via Firestore writes:
1. **Account Ban & Suspension:** Admin sets `isBanned = true`, `isActive = false`, `banReason`, and optional `banExpiresAt` on `/users/{uid}`. User App (`UserSecurityManager`) listens in real time; `MainActivity` immediately tears down navigation and intercepts the user with `BannedScreen`.
2. **Granular Feature Restrictions:** Admin toggles boolean flags on `/users/{uid}`:
   - `canWatch = false` / `watchBan = true`: `PlayerScreen` and `InlineDetailVideoPlayer` pause ExoPlayer and render a lock overlay.
   - `canDownload = false` / `downloadBan = true`: Download buttons in `DetailsScreens`, `BatchDownloadSheet`, and `SmartDownloadQualityDialog` are locked; `StreamDownloaderService` rejects new download tasks.
   - `canChat = false` / `chatBan = true`: Social chat inputs are disabled in `ChatScreen` and `SocialViewModel`; Firestore security rules explicitly reject message writes with `PERMISSION_DENIED`.
   - `canStory = false` / `storyBan = true`: Story posting is blocked in UI and rejected by Firestore rules on `/stories/{storyId}`.
   - `canP2P = false` / `p2pBan = true`: `ShareScreen` locks local Wi-Fi Direct socket initialization with a restriction overlay.
   - `downloadLimit`: Caps the number of active downloads.
   - `allowedQuality`: Restricts maximum video playback and download resolution (e.g., capping at 720p or 1080p).
3. **Subscriptions & Entitlements:** Admin updates `subscriptionTier` (`PRO`, `VIP`), `isPremium`, `subscriptionStatus` (`ACTIVE`), and `subscriptionExpiresAt` on `/users/{uid}`. User App reflects ad removal (`isAdFree = true`) via `AdManager`.
4. **Points Wallet & Manual Adjustments:** Admin writes directly to `/users/{uid}.pointsBalance` or appends records to `/users/{uid}/point_transactions/{txId}`. User App displays the updated balance in `ProfileScreen` and `PointsEarningViewModel`.
5. **Reviewing & Resolving Pro Requests:** Admin reads `/pro_requests/{requestId}` and updates status (`APPROVED` or `REJECTED`). Upon approval, Admin updates `/users/{uid}` with subscription fields.
6. **Scheduled Maintenance Mode:** Admin sets `maintenanceEnabled = true`, `maintenanceTitle`, `maintenanceMessage` in `/config/app`. User App immediately intercepts all regular users with `MaintenanceScreen`. Admin accounts (`maintenanceAllowedRoles: ["admin", "superadmin"]`) are exempt and retain full access.
7. **Feature Flags & Toggles:** Admin modifies `/config/features` to set `subscriptions`, `points`, `dailyLogin`, `rewardedAds`, `tasks`, or `leaderboard` to `ACTIVE`, `DISABLED`, or `COMING_SOON`. User App dynamically hides or disables corresponding UI panels.
8. **Economy Parameters:** Admin modifies `/config/economy` (adjusting `dailyLoginRewards`, `rewardedAdPoints`, `rewardedAdDailyCap`, `rewardedAdCooldownSeconds`, `redemptionCosts`). User App dynamically updates reward calculations.
9. **Extension / Scraper Catalog & Priority:** Admin creates or updates `/managed_extensions/{extensionId}` (status `ACTIVE`/`DISABLED`, `baseUrl`, `priority`) and `/config/search_order`. User App dynamically re-orders and enables/disables content scrapers.
10. **Push Notifications & System Announcements:** Admin writes to `/notifications/{notificationId}` (with `target: "all"` or `target: "user"`, `targetUid`). User App syncs and displays local system notifications.
11. **App Version Enforcement:** Admin publishes update records in `/app_updates/{updateId}` or updates `/config/app.minimumVersionCode`. User App alerts the user with an optional or mandatory update modal.

### Question 2: What functions currently exist and work properly, and what functions are incomplete, partial, or unconstrained on the server?
- **Fully Functional & Server-Enforced:**
  - Account Ban (`/users/{uid}` ban listener + `MainActivity` lockout).
  - Social Chat and Stories restriction (`canChat`, `chatBan`, `canStory`, `storyBan` enforced both in UI and by Firestore rules).
  - Support ticket creation and messaging (`/support_conversations/{uid}` and subcollection `/messages/{msgId}` with role separation `senderRole: 'user'` vs `'admin'`).
  - Ticket status lockdown (User App cannot change ticket status from `resolved` to `open` or alter `userId`).
  - Pro subscription request workflow (`/pro_requests` creates `PENDING` request; client self-approval is rejected by Firestore rules).
  - Multi-device FCM Token management (`/users/{uid}/fcmTokens/{installationId}` with schema validation).
- **Client-Enforced Only (Architectural Gaps):**
  - **Watch & Download Restrictions:** `canWatch` and `canDownload` are strictly enforced by Android UI and services (`ExoPlayer`, `StreamDownloaderService`), but the third-party scraper hosts and media CDNs do not authenticate against Firebase. A modified APK could theoretically bypass client checks and stream media directly.
  - **Offline Playback Restriction:** `offlineDaysOverride` is defined and parsed in `UserSecurityManager`, but is NOT currently hooked into `DownloadsViewModel` or `PlayerScreen` for local unencrypted MP4 files on device storage.
  - **P2P Sharing Restriction:** Enforced at the UI layer in `ShareScreen.kt`, but if two physical devices are completely disconnected from the Internet, local Wi-Fi Direct sockets operate peer-to-peer without contacting Firestore.
  - **User-to-User Blocking:** Handled entirely in client-side `DataStore` (`UserPreferencesRepository.BLOCKED_USERS`). Firestore rules currently have no server-side record of peer blocks.
- **Incomplete / Requiring Trusted Backend:**
  - **Points Earning (Daily Login, Rewarded Ads, Tasks):** Client-side direct minting is intentionally blocked by hardened Firestore rules (`PERMISSION_DENIED`). A trusted backend (Cloudflare Worker or Cloud Functions) is required to execute atomic points minting using Firebase Admin SDK.

### Question 3: What Firestore collections, fields, values, and states does the app actually use?
The User App interacts with 15 distinct Firestore collection paths, comprising root collections and subcollections. Full schemas, types, required/optional statuses, and timestamp encodings are detailed in Section 4.

### Question 4: What operations are performed directly by the user, and what operations must rely on a trusted backend?
- **Direct User Operations (Legitimate & Safe):**
  - Read own profile `/users/{uid}` and user subcollections (`library`, `history`, `watched_episodes`, `fcmTokens`).
  - Update personal non-sensitive profile attributes (`displayName`, `username`, `bio`, `photoUrl`).
  - Submit `PENDING` Pro subscription requests to `/pro_requests/{requestId}`.
  - Submit content/user reports to `/reports/{reportId}` (`status: "pending"`).
  - Create and participate in 1-on-1 social conversations `/conversations/{convId}` and `/messages/{msgId}`.
  - Add reactions and toggle `deletedFor` on messages.
  - Submit support inquiries to `/support_conversations/{uid}`.
  - Register FCM tokens under `/users/{uid}/fcmTokens/{installationId}`.
- **Operations Requiring a Trusted Backend / Admin SDK:**
  - Incrementing or decrementing `pointsBalance`, `totalPointsEarned`, `totalPointsSpent`.
  - Appending verified entries to `/users/{uid}/point_transactions/{txId}`.
  - Granting subscriptions, setting `isPremium = true`, `subscriptionTier`, `subscriptionExpiresAt`.
  - Authorizing Pro upgrade requests (updating `/pro_requests` to `APPROVED`).
  - Setting user account bans or granular restriction flags (`isBanned`, `canWatch`, `canDownload`, etc.).
  - Publishing global configurations (`/config/app`, `/config/features`, `/config/economy`, `/config/search_order`).
  - Publishing system announcements (`/notifications`).
  - Resolving or deleting support tickets.

### Question 5: How can current admin functions be preserved and secured without stripping legitimate owner powers?
Owner powers are preserved through dual-check architecture:
1. In `firestore.rules`, all administrative mutations are guarded by `isAdmin()`:
   ```javascript
   function isAdmin() {
     return isAuthenticated() && 
       exists(/databases/$(database)/documents/admins/$(request.auth.uid)) &&
       get(/databases/$(database)/documents/admins/$(request.auth.uid)).data.enabled == true;
   }
   ```
2. Client-side code (`UserSecurityManager.isCanonicalAdmin()`) recognizes both the Firestore document `/admins/{uid}.enabled == true` and the canonical owner identity (`sulopros01@gmail.com`).
3. To ensure legitimate owner operations succeed while blocking malicious users:
   - All admin writes to `/config/*`, `/admins/*`, `/managed_extensions/*`, `/reward_tasks/*`, `/auditLogs/*`, and `/notifications/*` require the verified `/admins/{uid}` document.
   - Non-admin client writes to sensitive fields (subscriptions, roles, points, bans) remain blocked via `affectedKeys` blocklists.

### Question 6: What variables, fields, and states are required to prepare for building a support communication page in the admin dashboard in the future?
Currently, the User App uses a simplified support contract:
- Path: `/support_conversations/{uid}` (conversation ID is the user UID)
- Status literal: strictly lowercase `"open"`
- Messages subcollection: `/support_conversations/{uid}/messages/{msgId}`
- Sender roles: strictly lowercase `"user"` or `"admin"`
A comprehensive, forward-compatible contract proposal covering priority, assignment, read states, and attachments is formulated in Section 14.

---

## 3. Comprehensive Feature Inventory

The following table catalogs every major Firebase- and administrative-dependent capability within the User App codebase:

| Feature ID | Feature Name | Implementation Status | Verification Status | Code Evidence (File & Line) | Enforcement Layer | Description & Operational Realities |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **FEAT-01** | Account Full Ban Interception | `IMPLEMENTED` | `CONFIRMED` | `MainActivity.kt:279-288`, `UserSecurityManager.kt:144-146`, `BannedScreen.kt:46-92` | Client UI & Routing + Rules | Real-time listener detects `isBanned` or `!isActive`. Immediately blocks navigation and renders `BannedScreen`. Rules also reject chat and story writes. |
| **FEAT-02** | Granular Watch Restriction | `IMPLEMENTED` | `CONFIRMED` | `UserSecurityManager.kt:147-148`, `PlayerScreen.kt:726-733`, `InlineDetailVideoPlayer.kt:1189-1196` | Client Player Logic | Pauses ExoPlayer, sets `playWhenReady = false`, displays lock screen overlay. External media CDN unauthenticated. |
| **FEAT-03** | Granular Download Restriction | `IMPLEMENTED` | `CONFIRMED` | `UserSecurityManager.kt:149-150`, `StreamDownloaderService.kt:190`, `AndroidDownloader.kt:37` | Client Service Layer | UI locks download buttons; downloader service rejects enqueue requests. Media CDN unauthenticated. |
| **FEAT-04** | Granular Social Chat Ban | `IMPLEMENTED` | `CONFIRMED` | `UserSecurityManager.kt:151-152`, `ChatScreen.kt:224`, `firestore.rules:337, 356` | Client UI + Firestore Rules | UI disables message compose; Firestore rules reject message creation with `PERMISSION_DENIED`. Verified by live test C06. |
| **FEAT-05** | Granular Story Posting Ban | `IMPLEMENTED` | `CONFIRMED` | `SocialViewModel.kt:117`, `firestore.rules:386-388` | Client UI + Firestore Rules | UI blocks story submission; Firestore rules reject document creation. |
| **FEAT-06** | Granular P2P Sharing Ban | `IMPLEMENTED` | `CONFIRMED` | `ShareScreen.kt:119-156`, `UserSecurityManager.kt:155-156` | Client UI & Socket Init | Renders blocking overlay, aborts Wi-Fi Direct socket server initialization. Offline transfers between offline devices bypass checks. |
| **FEAT-07** | Video Quality / Resolution Capping | `IMPLEMENTED` | `CONFIRMED` | `UserRestrictions.kt:105-113`, `SmartDownloadQualityDialog.kt:567` | Client Dialog Logic | Checks `allowedQuality` (e.g. 720p). Disables higher quality selectors. Decoupled from subscription tier. |
| **FEAT-08** | Download Item Count Limit | `IMPLEMENTED` | `CONFIRMED` | `UserRestrictions.kt:115-118`, `SmartDownloadQualityDialog.kt:567` | Client Dialog Logic | Evaluates `downloadLimit` against completed downloads. Blocks new downloads if limit reached. |
| **FEAT-09** | Pro Upgrade Request Submission | `IMPLEMENTED` | `CONFIRMED` | `PointsRepository.kt:166-191`, `SubscriptionScreen.kt:340`, `firestore.rules:235-248` | Client Repo + Firestore Rules | Client creates `PENDING` doc in `/pro_requests/{reqId}`. Rules reject self-approval. Verified by live tests B05 & B06. |
| **FEAT-10** | Subscription Entitlement (Ad Removal) | `IMPLEMENTED` | `CONFIRMED` | `AdManager.kt:43, 115`, `UserSecurityManager.kt:233`, `UserRestrictions.kt:78-83` | Client Ad Coordinator | `isAdFree` suppresses interstitial/rewarded ads. Strictly decoupled from video/download quality. |
| **FEAT-11** | Points Wallet Observation | `IMPLEMENTED` | `CONFIRMED` | `PointsRepository.kt:36-81`, `PointsEarningRepository.kt:69-101` | Client SnapshotListener | Real-time read-only observation of `/users/{uid}.pointsBalance` and subcollection `point_transactions`. |
| **FEAT-12** | Points Earning via Trusted Backend | `PARTIAL` | `CONFIRMED` | `PointsEarningRepository.kt:241-255`, `PointsEarningViewModel.kt:100` | External Backend | Dispatches requests to `/api/earn/*`. Returns controlled `BACKEND_REQUIRED` when backend unconfigured. |
| **FEAT-13** | Temporary Firebase Economy Mode | `INCONSISTENT` | `CONFIRMED` | `TemporaryFirebaseEconomyRepository.kt:67-140`, `firestore.rules:95-118` | Client Transaction (Blocked) | Client attempt to write points directly via Firestore transaction is blocked by hardened rules (`PERMISSION_DENIED`). |
| **FEAT-14** | Scheduled Maintenance Mode | `IMPLEMENTED` | `CONFIRMED` | `AppStartupManager.kt:46-86`, `MainActivity.kt:268-278`, `MaintenanceScreen.kt:40` | Client UI Interception | Reads `/config/app`. Intercepts non-admin users with full-screen `MaintenanceScreen`. Admins exempt. |
| **FEAT-15** | Feature Flags Management | `IMPLEMENTED` | `CONFIRMED` | `EconomyConfigRepository.kt:39-56`, `FeaturesConfig.kt:15-40` | Client SnapshotListener | Reads `/config/features`. Dynamically adjusts UI for `subscriptions`, `points`, `dailyLogin`, `tasks`, etc. |
| **FEAT-16** | Economy Configuration Management | `IMPLEMENTED` | `CONFIRMED` | `EconomyConfigRepository.kt:58-75`, `EconomyConfig.kt:12-40` | Client SnapshotListener | Reads `/config/economy`. Dynamically sets reward amounts, cooldowns, and redemption costs. |
| **FEAT-17** | Managed Extensions Catalog Sync | `IMPLEMENTED` | `CONFIRMED` | `FirebaseFirestoreManagedExtensionDataSource.kt:26-54`, `ManagedMediaOrchestrator.kt:589` | Client Engine | Reads `/managed_extensions`. Configures built-in scrapers with remote URLs, status, priority. |
| **FEAT-18** | Global Extension Search Ordering | `IMPLEMENTED` | `CONFIRMED` | `SearchOrderDataSource.kt:36-110`, `IntelligentDubbedSearchEngine.kt:45` | Client Search Engine | Reads `/config/search_order`. Orders extension execution order for movies, series, anime, dubbing. |
| **FEAT-19** | Multi-Device FCM Token Registration | `IMPLEMENTED` | `CONFIRMED` | `FcmTokenManager.kt:131-219`, `firestore.rules:169-207` | Client Service + Firestore Rules | Generates stable `installationId`, writes device metadata to `/users/{uid}/fcmTokens/{installationId}`. |
| **FEAT-20** | Push & In-App Notification Center | `IMPLEMENTED` | `CONFIRMED` | `NotificationRepository.kt:21-98`, `AppFirebaseMessagingService.kt:34-120` | Client Sync + FCM Service | Reads `/notifications`. Filters by targeting (`target: "all"` vs `"user"`), expiry, and user preferences. |
| **FEAT-21** | App Update Discovery & Enforcement | `IMPLEMENTED` | `CONFIRMED` | `AppUpdateManager.kt:58-140`, `AppStartupManager.kt:124-150` | Client Version Evaluator | Reads `/app_updates` and `/config/app`. Evaluates `versionCode`. Prompts optional or mandatory update modal. |
| **FEAT-22** | Support Communication Thread | `IMPLEMENTED` | `CONFIRMED` | `SupportViewModel.kt:48-134`, `firestore.rules:280-327` | Client UI + Firestore Rules | User sends messages under `/support_conversations/{uid}/messages`. Real-time sync with local Room database. |
| **FEAT-23** | User Content & Bug Reporting | `IMPLEMENTED` | `CONFIRMED` | `ReportRepository.kt:18-45`, `firestore.rules:269-277` | Client Repo + Firestore Rules | Writes report doc to `/reports/{reportId}` with `status: "pending"`. Read restricted to author and admin. |
| **FEAT-24** | Social 1-on-1 Chat Messaging | `IMPLEMENTED` | `CONFIRMED` | `SocialRepository.kt:126-295`, `firestore.rules:330-379` | Client Repo + Firestore Rules | Creates `/conversations/{convId}` and messages. Supports reactions, editing, and per-user message deletion. |
| **FEAT-25** | Ephemeral Stories | `IMPLEMENTED` | `CONFIRMED` | `SocialRepository.kt:114-124, 331-336`, `firestore.rules:382-390` | Client Repo + Firestore Rules | Uploads media via Cloudinary; writes story metadata to `/stories/{storyId}` with timestamp. |
| **FEAT-26** | User-to-User Local Blocking | `PARTIAL` | `CONFIRMED` | `UserPreferencesRepository.kt:37, 127-134` | Client DataStore Only | Blocks peer user locally on device. Zero Firestore sync; remote messages still accepted by Firestore rules. |
| **FEAT-27** | Account Deletion Flow | `IMPLEMENTED` | `CONFIRMED` | `AuthRepository.kt:560-667` | Client Flow + Firestore Rules | Deletes user library, history, FCM tokens, user doc, and Firebase Auth account. Swallows support doc deletion failure. |

---

## 4. Firestore Path Inventory & Actual Data Contract

The table below documents every single Firestore path utilized by the User App, its schema, data types, timestamp formats, and access permissions:

| # | Firestore Collection / Document Path | Purpose & Context | Actual Fields & Data Types | Req / Opt | Timestamp Format | Write Authority | Allowed Operations | Security Rules Citation | Code Locations |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `/admins/{adminId}` | Admin authorization directory | `uid` (String)<br>`enabled` (Boolean)<br>`role` (String) | Req<br>Req<br>Opt | N/A | Admin Only | `READ`, `WRITE` (Admin only) | `firestore.rules:21-24` | `UserSecurityManager.kt:58-70`, `tests/rules:21` |
| **2** | `/config/app` | Global app config, maintenance & update | `maintenanceEnabled` (Boolean)<br>`maintenanceTitle` (String)<br>`maintenanceMessage` (String)<br>`maintenanceAllowedRoles` (List&lt;String&gt;)<br>`minimumVersionCode` (Long)<br>`latestVersionCode` (Long)<br>`latestVersionName` (String)<br>`apkUrl` (String)<br>`apkSha256` (String)<br>`mandatoryUpdate` (Boolean)<br>`releaseNotes` (String)<br>`defaultOfflineDays` (Long)<br>`defaultForcedAds` (Long)<br>`providersJson` (String)<br>`updatedAt` (Long/Timestamp) | Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Unix ms / Timestamp | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:27-30` | `AppStartupManager.kt:48-64`, `AppConfig.kt:8-24` |
| **3** | `/config/search_order` | Extension search priority & dubbing | `movie` (List&lt;String&gt;)<br>`tv` (List&lt;String&gt;)<br>`series` (List&lt;String&gt;)<br>`anime` (List&lt;String&gt;)<br>`arabicDubbed` (List&lt;String&gt;)<br>`englishDubbed` (List&lt;String&gt;)<br>`dubbingConfig` (Map&lt;String, Any&gt;)<br>`updatedAt` (Timestamp/Long)<br>`updatedBy` (String) | Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp / Unix ms | Admin Only | `READ` (Public `true`)<br>`WRITE` (Admin only) | `firestore.rules:33-36` | `SearchOrderDataSource.kt:39-95` |
| **4** | `/config/features` | Remote feature flags | `subscriptions` (String)<br>`points` (String)<br>`dailyLogin` (String)<br>`rewardedAds` (String)<br>`tasks` (String)<br>`leaderboard` (String)<br>`disabledMessage` (String) | Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | N/A | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:39-42` | `EconomyConfigRepository.kt:41-53`, `FeaturesConfig.kt:15-30` |
| **5** | `/config/economy` | Points economy tuning | `redemptionCosts` (Map&lt;String, Long&gt;)<br>`dailyLoginRewards` (List&lt;Long&gt;)<br>`rewardedAdPoints` (Long)<br>`rewardedAdDailyCap` (Long)<br>`rewardedAdCooldownSeconds` (Long) | Opt<br>Opt<br>Opt<br>Opt<br>Opt | N/A | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:45-48` | `EconomyConfigRepository.kt:60-72`, `EconomyConfig.kt:12-30` |
| **6** | `/users/{userId}` | User profile, bans & entitlements | `uid` (String)<br>`email` (String)<br>`displayName` (String)<br>`username` (String)<br>`photoUrl` (String)<br>`bio` (String)<br>`role` (String)<br>`isPremium` (Boolean)<br>`subscriptionTier` (String)<br>`subscriptionStatus` (String)<br>`subscriptionExpiresAt` (Long/Timestamp)<br>`planId` (String)<br>`durationDays` (Int)<br>`isBanned` (Boolean)<br>`isActive` (Boolean)<br>`banReason` (String)<br>`banExpiresAt` (Long/Timestamp)<br>`canWatch` (Boolean)<br>`watchBan` (Boolean)<br>`canDownload` (Boolean)<br>`downloadBan` (Boolean)<br>`canChat` (Boolean)<br>`chatBan` (Boolean)<br>`canStory` (Boolean)<br>`storyBan` (Boolean)<br>`canP2P` (Boolean)<br>`p2pBan` (Boolean)<br>`allowedQuality` (String)<br>`downloadLimit` (Int)<br>`offlineDaysOverride` (Int)<br>`forcedAdsOverride` (Int)<br>`pointsBalance` (Long)<br>`totalPointsEarned` (Long)<br>`totalPointsSpent` (Long)<br>`dailyStreak` (Int)<br>`lastDailyLoginDate` (String 'YYYY-MM-DD')<br>`createdAt` (Timestamp/Long)<br>`updatedAt` (Timestamp) | Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp / Unix ms | Mixed (Owner: profile only; Admin/Backend: bans, tiers, points) | `READ` (Owner/Admin)<br>`CREATE` (Owner with safe defaults)<br>`UPDATE` (Owner profile only; Admin all)<br>`DELETE` (Owner/Admin) | `firestore.rules:56-122` | `UserSecurityManager.kt:73-178`, `AuthRepository.kt:250-310`, `UserRestrictions.kt:14-59` |
| **7** | `/users/{userId}/public/{docId}` | Safe public profile projection | `uid` (String)<br>`displayName` (String)<br>`username` (String)<br>`photoUrl` (String) | Req<br>Opt<br>Opt<br>Opt | N/A | Owner / Admin | `READ` (Auth users)<br>`WRITE` (Owner/Admin) | `firestore.rules:125-128` | Pre-configured in rules for architectural migration |
| **8** | `/users/{userId}/library/{itemId}` | User saved library items | `id` (String)<br>`title` (String)<br>`posterUrl` (String)<br>`mediaType` (String)<br>`addedAt` (Long) | Req<br>Req<br>Opt<br>Opt<br>Opt | Unix ms | Owner / Admin | `READ`, `WRITE` (Owner/Admin) | `firestore.rules:131-133` | `LibraryRepository.kt:17-45` |
| **9** | `/users/{userId}/history/{itemId}` | Playback history tracking | `id` (String)<br>`mediaId` (String)<br>`progress` (Long)<br>`duration` (Long)<br>`timestamp` (Long) | Req<br>Req<br>Opt<br>Opt<br>Opt | Unix ms | Owner / Admin | `READ`, `WRITE` (Owner/Admin) | `firestore.rules:135-137` | `HistoryRepository.kt:17-45` |
| **10** | `/users/{userId}/watched_episodes/{epId}` | Watched episodes index | `episodeId` (String)<br>`seriesId` (String)<br>`seasonNumber` (Int)<br>`episodeNumber` (Int)<br>`watchedAt` (Long) | Req<br>Req<br>Opt<br>Opt<br>Opt | Unix ms | Owner / Admin | `READ`, `WRITE` (Owner/Admin) | `firestore.rules:139-141` | `WatchedEpisodeRepository.kt:14-40` |
| **11** | `/users/{userId}/point_transactions/{txId}` | Points audit ledger | `id` (String)<br>`txId` (String)<br>`userId` (String)<br>`type` (String)<br>`amount` (Long)<br>`balanceBefore` (Long)<br>`balanceAfter` (Long)<br>`referenceId` (String)<br>`description` (String)<br>`createdAt` (Timestamp/Long) | Req<br>Opt<br>Req<br>Req<br>Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Req | Timestamp / Unix ms | Admin / Backend Only | `READ` (Owner/Admin)<br>`WRITE` (Admin only) | `firestore.rules:144-147` | `PointsRepository.kt:43-80` |
| **12** | `/users/{userId}/task_claims/{claimId}` | Reward task completion records | `taskId` (String)<br>`userId` (String)<br>`claimedAt` (Long/Timestamp) | Req<br>Req<br>Req | Unix ms / Timestamp | Owner (Create only) / Admin | `READ` (Owner/Admin)<br>`CREATE` (Owner/Admin)<br>`UPDATE`, `DELETE` (Admin only) | `firestore.rules:150-155` | `PointsEarningRepository.kt:204-220` |
| **13** | `/users/{userId}/settings/notifications` | User notification preferences | `moviesEnabled` (Boolean)<br>`seriesEnabled` (Boolean)<br>`animeEnabled` (Boolean)<br>`socialEnabled` (Boolean)<br>`systemEnabled` (Boolean)<br>`updatedAt` (Timestamp) | Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp | Owner / Admin | `READ`, `WRITE` (Owner/Admin with key restrictions) | `firestore.rules:158-162` | `NotificationPreferencesRepository.kt:30-80` |
| **14** | `/users/{userId}/fcmTokens/{installationId}` | Device-level FCM registrations | `token` (String)<br>`installationId` (String)<br>`platform` (String 'android')<br>`deviceModel` (String)<br>`osVersion` (String)<br>`appVersion` (String)<br>`isActive` (Boolean)<br>`createdAt` (Timestamp)<br>`updatedAt` (Timestamp)<br>`lastSeenAt` (Timestamp) | Req<br>Req<br>Req<br>Req<br>Req<br>Req<br>Req<br>Req<br>Req<br>Req | Timestamp (`serverTimestamp()`) | Owner / Admin | `READ`, `WRITE` (Owner/Admin with schema validation) | `firestore.rules:169-207` | `FcmTokenManager.kt:172-194`, `FcmTokenDocument.kt:14-44` |
| **15** | `/managed_extensions/{extensionId}` | Extension & scraper definitions | `id` (String)<br>`name` (String)<br>`description` (String)<br>`baseUrl` (String)<br>`iconUrl` (String)<br>`scraperKey` (String)<br>`definitionVersion` (Long)<br>`minAppVersionCode` (Long)<br>`runtimeApiVersion` (Long)<br>`priority` (Long)<br>`language` (String)<br>`contentTypes` (List&lt;String&gt;)<br>`capabilities` (List&lt;String&gt;)<br>`audioCapabilities` (List&lt;String&gt;)<br>`searchUrl` (String)<br>`enabled` (Boolean)<br>`status` (String 'ACTIVE'/'DISABLED')<br>`updatedAt` (Timestamp/Long) | Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp / Unix ms | Admin Only | `READ` (Public `true`)<br>`WRITE` (Admin only) | `firestore.rules:211-214` | `FirebaseFirestoreManagedExtensionDataSource.kt:38-49`, `ManagedExtensionDto.kt:12-31` |
| **16** | `/extensions/{extensionId}` | Legacy extension fallback catalog | Same fields as `/managed_extensions` | Opt | Timestamp / Unix ms | Admin Only | `READ` (Public `true`)<br>`WRITE` (Admin only) | `firestore.rules:217-220` | Retained for backward compatibility |
| **17** | `/notifications/{notificationId}` | Broadcast & targeted notifications | `title` (String)<br>`body` / `message` (String)<br>`imageUrl` (String)<br>`type` (String)<br>`event` (String)<br>`isAnime` (Boolean)<br>`target` / `targetType` (String 'all'/'user')<br>`targetUid` (String)<br>`isActive` (Boolean)<br>`expiresAt` (Timestamp/Long)<br>`createdAt` / `timestamp` (Timestamp/Long) | Req<br>Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Req | Timestamp / Unix ms | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:223-226` | `NotificationRepository.kt:29-65` |
| **18** | `/app_updates/{updateId}` | Application release catalog | `versionCode` (Long)<br>`version` / `versionName` (String)<br>`releaseNotes` (String)<br>`updateUrl` / `apkUrl` (String)<br>`minVersionCode` (Long)<br>`isMandatory` (Boolean)<br>`isActive` (Boolean)<br>`sha256` (String)<br>`signature` (String)<br>`publicKey` (String)<br>`createdAt` / `publishedAt` (Timestamp/Long) | Req<br>Req<br>Opt<br>Req<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp / Unix ms | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:229-232` | `AppUpdateManager.kt:62-140` |
| **19** | `/pro_requests/{requestId}` | Pro subscription upgrade requests | `requestId` (String)<br>`userId` (String)<br>`planId` (String)<br>`durationDays` (Int)<br>`status` (String 'PENDING')<br>`paymentReference` (String)<br>`notes` (String)<br>`createdAt` (Long) | Req<br>Req<br>Opt<br>Opt<br>Req<br>Opt<br>Opt<br>Req | Unix ms | User (Create `PENDING` only) / Admin | `READ` (Owner/Admin)<br>`CREATE` (Owner `PENDING`)<br>`UPDATE`, `DELETE` (Admin only) | `firestore.rules:235-248` | `PointsRepository.kt:166-191` |
| **20** | `/reward_tasks/{taskId}` | Task rewards catalog | `taskId` (String)<br>`title` (String)<br>`description` (String)<br>`rewardPoints` (Long)<br>`taskType` (String)<br>`actionUrl` (String)<br>`isActive` (Boolean)<br>`expiresAt` (Timestamp/Long) | Req<br>Req<br>Opt<br>Req<br>Opt<br>Opt<br>Opt<br>Opt | Timestamp / Unix ms | Admin Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:251-254` | `PointsRepository.kt:87-120` |
| **21** | `/leaderboard/weekly_current` | Current weekly point rankings | `rankings` (List&lt;Map&lt;String, Any&gt;&gt;)<br>Item: `userId`, `username`, `displayName`, `photoUrl`, `rank`, `weeklyEarnedPoints`, `reward`, `tier`, `isActive` | Req | N/A | Admin / Backend Only | `READ` (Auth users)<br>`WRITE` (Admin only) | `firestore.rules:257-260` | `PointsRepository.kt:127-158` |
| **22** | `/reports/{reportId}` | User moderation reports | `userId` (String)<br>`targetId` (String)<br>`type` (String)<br>`reason` (String)<br>`status` (String 'pending')<br>`createdAt` (Long/Timestamp)<br>`resolvedBy` (Null on create)<br>`resolvedAt` (Null on create) | Req<br>Opt<br>Opt<br>Opt<br>Req<br>Opt<br>Opt<br>Opt | Unix ms / Timestamp | User (Create only) / Admin | `READ` (Author/Admin)<br>`CREATE` (Author `pending`)<br>`UPDATE`, `DELETE` (Admin only) | `firestore.rules:269-277` | `ReportRepository.kt:18-45` |
| **23** | `/support_conversations/{convId}` | Support thread parent | `conversationId` (String = `user.uid`)<br>`userId` (String = `user.uid`)<br>`userEmail` (String)<br>`userName` (String)<br>`status` (String 'open')<br>`lastMessage` (String)<br>`lastMessageTime` (Long)<br>`updatedAt` (Timestamp) | Req<br>Req<br>Opt<br>Opt<br>Req<br>Opt<br>Opt<br>Opt | Unix ms & `serverTimestamp()` | User (Create/Update metadata) / Admin (All) | `READ` (Owner/Admin)<br>`CREATE` (Owner 'open'/'pending')<br>`UPDATE` (Owner metadata only; Admin all)<br>`DELETE` (Admin only) | `firestore.rules:280-305` | `SupportViewModel.kt:99-110` |
| **24** | `/support_conversations/{convId}/messages/{msgId}` | Support thread messages | `id` / `messageId` (String UUID)<br>`conversationId` (String)<br>`senderId` (String)<br>`senderRole` (String 'user'/'admin')<br>`text` (String)<br>`timestamp` (Long)<br>`createdAt` (Timestamp) | Req<br>Req<br>Req<br>Req<br>Req<br>Req<br>Opt | Unix ms & `serverTimestamp()` | User (`senderRole: 'user'`) / Admin (`senderRole: 'admin'`) | `READ` (Thread Owner/Admin)<br>`CREATE` (Owner 'user'; Admin 'admin')<br>`UPDATE`, `DELETE` (Admin only) | `firestore.rules:307-327` | `SupportViewModel.kt:112-123` |
| **25** | `/conversations/{convId}` | Social chat conversation | `id` (String)<br>`participants` (List&lt;String&gt;)<br>`participantNames` (Map&lt;String, String&gt;)<br>`lastMessage` (String)<br>`lastMessageTime` (Long)<br>`unreadCounts` (Map&lt;String, Int&gt;) | Req<br>Req<br>Opt<br>Opt<br>Opt<br>Opt | Unix ms | Conversation Participants / Admin | `READ`, `WRITE` (Participants/Admin) | `firestore.rules:330-346` | `SocialRepository.kt:126-152, 242-261` |
| **26** | `/conversations/{convId}/messages/{msgId}` | Social chat private messages | `id` (String)<br>`senderId` (String)<br>`text` (String)<br>`timestamp` (Long)<br>`isEdited` (Boolean)<br>`isDeleted` (Boolean)<br>`reactions` (Map&lt;String, String&gt;)<br>`deletedFor` (List&lt;String&gt;) | Req<br>Req<br>Req<br>Req<br>Opt<br>Opt<br>Opt<br>Opt | Unix ms | Sender (Text/Edit) / Recipient (Reactions/deletedFor only) | `READ` (Participants/Admin)<br>`CREATE` (Sender if unbanned)<br>`UPDATE` (Sender text/edit; Recipient reactions/deletedFor)<br>`DELETE` (Sender/Admin) | `firestore.rules:348-378` | `SocialRepository.kt:211-222, 263-295, 339-370` |
| **27** | `/stories/{storyId}` | Social ephemeral stories | `id` (String)<br>`userId` (String)<br>`imageUrl` (String)<br>`timestamp` (Long) | Req<br>Req<br>Req<br>Req | Unix ms | Author / Admin | `READ` (Auth users)<br>`CREATE` (Author if unbanned)<br>`UPDATE`, `DELETE` (Author/Admin) | `firestore.rules:382-390` | `SocialRepository.kt:114-124, 331-336` |
| **28** | `/auditLogs/{logId}` | Admin & security audit events | `action` (String)<br>`actorUid` (String)<br>`targetUid` (String)<br>`details` (Map&lt;String, Any&gt;)<br>`timestamp` (Timestamp) | Req<br>Req<br>Opt<br>Opt<br>Req | Timestamp | Admin Only | `READ`, `WRITE` (Admin only) | `firestore.rules:393-395` | `tests/rules:1124-1136` |

---

## 5. Admin-Control Matrix

This matrix correlates each administrative action available to the owner with its Firestore representation, User App reflection, and the layer of security enforcement:

| Admin Control Action | Target Path & Fields Mutated | User App Real-time Reflection | Primary Enforcement Layer | Server Security Guarantee | Failure / Bypass Analysis |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Ban Account Fully** | `/users/{uid}`:<br>`isBanned = true`<br>`isActive = false`<br>`banReason = "..."`<br>`banExpiresAt = <timestamp>` | Real-time listener in `UserSecurityManager` fires; `MainActivity` swaps navigation with `BannedScreen`. Session cleared on logout. | **Client UI & Navigation Interception + Firestore Rules** | `CONFIRMED`. Firestore rules check `data.isBanned != true` for chat/stories. Rules also reject banned user updates. | If client APK is hacked to bypass `BannedScreen`, third-party video CDN streams might still load, but Firestore-backed features (chat, support, profile) are blocked by rules. |
| **Unban Account** | `/users/{uid}`:<br>`isBanned = false`<br>`isActive = true`<br>`banReason = ""` | `BannedScreen` check button refreshes security; screen dismisses and `AppNavigation` is restored immediately. | **Client UI & Security State** | `CONFIRMED`. Restores full read/write permissions for user. | None. Operational and verified. |
| **Ban Video Watching** | `/users/{uid}`:<br>`canWatch = false`<br>`watchBan = true` | `PlayerScreen` & `InlineDetailVideoPlayer` pause ExoPlayer and show lock overlay. `HeroCarousel` locks Play button. | **Client Player Logic Only** | `CLIENT-ONLY GAP`. Third-party scrapers/CDNs have no token gating. | Modifying the APK code removes the ExoPlayer pause call; video would stream because CDN does not check Firestore. |
| **Ban Media Downloading** | `/users/{uid}`:<br>`canDownload = false`<br>`downloadBan = true` | Download buttons locked; `StreamDownloaderService` and `AndroidDownloader` reject download tasks. | **Client Service & UI Logic** | `CLIENT-ONLY GAP`. Direct HTTP video links are not gated by Firebase. | Hacked APK could bypass client downloader check. |
| **Ban Chat / Social Messaging** | `/users/{uid}`:<br>`canChat = false`<br>`chatBan = true` | UI disables message compose button in `ChatScreen`. | **Client UI + Firestore Security Rules** | `SERVER-ENFORCED`. Firestore rules evaluate `data.canChat != false && data.chatBan != true`. Rejects writes with `PERMISSION_DENIED`. | Tamper-proof on server. Verified live in test C06. |
| **Ban Social Stories** | `/users/{uid}`:<br>`canStory = false`<br>`storyBan = true` | UI disables Add Story button in `SocialScreen`. | **Client UI + Firestore Security Rules** | `SERVER-ENFORCED`. Firestore rules evaluate `data.canStory != false && data.storyBan != true` on `/stories/{storyId}`. | Tamper-proof on server. |
| **Ban P2P / Offline Sharing** | `/users/{uid}`:<br>`canP2P = false`<br>`p2pBan = true` | `ShareScreen` renders blocking overlay; socket service initialization aborted. | **Client UI & Socket Coordinator** | `CLIENT-ONLY GAP`. Wi-Fi Direct socket protocol has no cloud gate. | Completely offline phones not connected to Firestore will not receive the ban event. |
| **Set Video Quality Cap** | `/users/{uid}`:<br>`allowedQuality = "720p"` | `SmartDownloadQualityDialog` disables resolutions above 720p; `UserRestrictions.isQualityAllowed()` caps ExoPlayer tracks. | **Client Dialog & Player Track Selector** | `CLIENT-ONLY`. Video CDNs provide raw multi-bitrate streams without user authentication. | Client-side restriction only. |
| **Set Download Item Cap** | `/users/{uid}`:<br>`downloadLimit = 5` | `SmartDownloadQualityDialog` disables download buttons once completed download count reaches 5. | **Client Dialog & Room Database Check** | `CLIENT-ONLY`. Room database stores local downloads. | Client-side restriction only. |
| **Grant / Revoke Subscription** | `/users/{uid}`:<br>`subscriptionTier = "PRO"`<br>`isPremium = true`<br>`subscriptionStatus = "ACTIVE"`<br>`subscriptionExpiresAt = ...` | `UserSecurityManager` recalculates `isAdFree = true`. `AdManager` suppresses all banner and rewarded ads. | **Client Ad Manager + Firestore Rules** | `SERVER-ENFORCED`. Rules block users from updating subscription fields (`affectedKeys`). Only Admin can update. | Verified live in tests A09 & A13. |
| **Adjust Points Balance** | `/users/{uid}`:<br>`pointsBalance = <newVal>` | `ProfileScreen` and `PointsEarningViewModel` immediately reflect new balance. | **Client Wallet Listener + Firestore Rules** | `SERVER-ENFORCED`. Rules block client from updating `pointsBalance`. Only Admin can update. | Verified live in tests B01 & B09. |
| **Record Points Transaction** | `/users/{uid}/point_transactions/{txId}`:<br>Payload with `type`, `amount`, `balanceAfter` | Displays in user transaction history list in `PointsHistoryDialog`. | **Client Ledger Listener + Firestore Rules** | `SERVER-ENFORCED`. Rules restrict write on `point_transactions` to `isAdmin()`. | Verified live in tests B02 & B10. |
| **Approve / Reject Pro Request** | `/pro_requests/{reqId}`:<br>`status = "APPROVED"` / `"REJECTED"` | Admin updates request status; Admin updates `/users/{uid}` with subscription tier. User gets ad-free experience. | **Firestore Rules + Admin Dashboard** | `SERVER-ENFORCED`. Rules block client from updating or approving requests. | Tamper-proof on server. Verified in test B06. |
| **Toggle Scheduled Maintenance** | `/config/app`:<br>`maintenanceEnabled = true`<br>`maintenanceTitle = "..."`<br>`maintenanceMessage = "..."` | `MainActivity` displays full-screen `MaintenanceScreen`. Regular users cannot proceed. Admins bypass. | **Client UI Interception** | `CLIENT-ONLY GAP`. Firestore reads are still permitted under security rules for authenticated users. | While regular users are blocked by UI, their auth token can still read Firestore docs unless rules also check maintenance. |
| **Toggle Feature Flags** | `/config/features`:<br>`subscriptions = "DISABLED"`<br>`points = "DISABLED"` | UI disables subscription/points screens and shows `disabledMessage`. | **Client UI Dynamic Layout** | `CLIENT-ONLY`. Rules do not dynamically check feature flags. | Purely UI presentation gating. |
| **Update Economy Tuning** | `/config/economy`:<br>`dailyLoginRewards = [...]`<br>`rewardedAdPoints = 20` | `PointsEarningViewModel` updates daily claim reward ladder and ad reward amounts. | **Client Calculation** | `CLIENT-DEPENDENT`. In temporary economy mode, calculation occurs in client; in trusted backend mode, backend enforces. | In temporary mode, client-calculated amounts are sent. |
| **Enable / Disable Extension** | `/managed_extensions/{extId}`:<br>`enabled = false`<br>`status = "DISABLED"` | `ManagedExtensionRealtimeSyncManager` updates local cache; `ManagedMediaOrchestrator` excludes scraper from search. | **Client Orchestrator** | `PUBLIC READ`. Client consumes remote config. | Scrapers are built into APK; disabling in Firestore removes them from search results dynamically. |
| **Reorder Global Extensions** | `/config/search_order`:<br>`movie = ["egydead", "anime4up"]`<br>`dubbingConfig = {...}` | `IntelligentDubbedSearchEngine` searches extensions in exact specified priority order. | **Client Search Engine** | `PUBLIC READ`. Client strictly respects order. | Operational and verified in search engine tests. |
| **Publish System Notification** | `/notifications/{notifId}`:<br>`title`, `body`, `type`, `target` | `NotificationRepository` syncs document, filters preferences, deduplicates, and posts Android system notification. | **Client Sync + System Tray** | `SERVER-ENFORCED WRITES`. Only Admin can write to `/notifications`. | Verified live in test E04. |
| **Publish App Update** | `/app_updates/{updateId}`:<br>`versionCode = 15`<br>`isMandatory = true` | `AppUpdateManager` displays update prompt dialog on startup or settings check. | **Client Version Comparator** | `SERVER-ENFORCED WRITES`. Only Admin can write update records. | Verified in update manager unit tests. |
| **Respond to Support Ticket** | `/support_conversations/{uid}/messages/{msgId}`:<br>`senderRole = "admin"`, `text = "..."` | `SupportViewModel` receives message in real time, saves to Room `supportDao`, displays in chat thread. | **Client Real-time Listener + Firestore Rules** | `SERVER-ENFORCED`. Rules enforce that only Admin can write `senderRole == 'admin'`. Normal users spoofing are blocked. | Verified live in test D02. |

---

## 6. User Restriction Matrix

This matrix provides an exhaustive, forensic breakdown of every account restriction mechanism supported by the CineStream User App:

| Restriction Category | Exact Firestore Fields & Types | Affected Application Layers | Client Enforcement Mechanism | Server / Rules Enforcement Mechanism | Behavior When Lifted / Unbanned | Expiration & Reason Support |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Full Account Ban** | `isBanned`: Boolean<br>`isActive`: Boolean<br>`banReason`: String<br>`banExpiresAt`: Long / Timestamp | `MainActivity`, Navigation, Session, Social, Stories | `MainActivity:279` completely removes `AppNavigation` from Compose hierarchy and renders `BannedScreen`. Session sign-out allowed. | Firestore security rules reject `/conversations` creation, message creation, story creation, and user doc updates. | When `isBanned` becomes false, `BannedScreen` check button triggers `refreshUserSecurity()`, dismisses overlay, and restores navigation instantly. | `banReason` displayed in red banner on `BannedScreen`. `banExpiresAt` formatted as `yyyy/MM/dd HH:mm`. Ban auto-expires if `now > banExpiresAt`. |
| **Community & Social Restrictions** | `canComment`: Boolean<br>`canUpload`: Boolean<br>`canRequest`: Boolean | Comments, Media Upload, Content Requests | Checked via `UserSecurityManager.canComment()`, `canUpload()`, `canRequest()`. Disables comment inputs and request forms. | Declared in user document schema. Currently enforced via client gating. | Re-enabling fields in `/users/{uid}` immediately un-greys interactive UI buttons via real-time StateFlow. | No explicit expiration field for sub-permissions; governed by account ban expiration or manual admin toggle. |
| **Messaging Ban** | `canChat`: Boolean (default true)<br>`chatBan`: Boolean (default false) | Social Chat (`ChatScreen`, `SocialViewModel`, `ChatViewModel`) | `ChatScreen:224` disables send button and displays warning toast: "Chat is restricted for your account". `SocialViewModel:107` aborts `sendMessage()`. | `firestore.rules:337, 356`: `get(/databases/.../users/$(auth.uid)).data.canChat != false && data.chatBan != true`. Rejects writes with `PERMISSION_DENIED`. | Real-time snapshot listener on `/users/{uid}` updates `UserRestrictions.isChatAllowed`, immediately restoring compose input. | Covered by general account suspension reason, or toggled independently by admin. |
| **Story Posting Ban** | `canStory`: Boolean (default true)<br>`storyBan`: Boolean (default false) | Social Stories (`SocialScreen`, `SocialViewModel`) | `SocialViewModel:117` returns immediately if `!UserSecurityManager.canPostStory()`. UI hides Add Story FAB. | `firestore.rules:386-388`: Evaluates `canStory != false && storyBan != true` before allowing write to `/stories/{storyId}`. | Setting `canStory = true` restores story creation button in `SocialScreen`. | Managed directly via admin toggle. |
| **Watching Ban** | `canWatch`: Boolean (default true)<br>`watchBan`: Boolean (default false) | Media Playback (`PlayerScreen`, `InlineDetailVideoPlayer`, `HeroCarousel`) | `PlayerScreen:726-733` pauses ExoPlayer (`playWhenReady = false`), clears video surface, and displays full-screen black lock overlay. | `CLIENT-ONLY GAP`. Third-party media scraper hosts (e.g. Anime4Up, EgyDead) do not check CineStream Firebase Auth. | Restoring `canWatch = true` removes player lock overlay and resumes normal playback controls. | Reason displayed inside player overlay if provided. |
| **Download Ban** | `canDownload`: Boolean (default true)<br>`downloadBan`: Boolean (default false)<br>`downloadLimit`: Int (optional) | Media Downloader (`DetailsScreens`, `SmartDownloadQualityDialog`, `BatchDownloadSheet`, `StreamDownloaderService`, `AndroidDownloader`) | UI disables Download buttons. `StreamDownloaderService:190` and `AndroidDownloader:37` check `!UserSecurityManager.canDownload()` before queuing download job. | `CLIENT-ONLY GAP`. Video file URLs are direct HTTP links from third-party hosts. | Restoring `canDownload = true` re-enables download icons and dialog options. | Toast notification: "Downloading is restricted for your account". |
| **Offline Playback Restriction** | `offlineDaysOverride`: Int (optional)<br>`defaultOfflineDays`: Long (in `/config/app`) | Downloaded Storage & Offline Player | `UserSecurityManager.getOfflineDaysLimit()` parses override (or default 2 days). **Architectural Finding:** Not currently hooked to reject playback in `DownloadsScreen`. | `CLIENT-ONLY GAP`. Downloaded media files are standard unencrypted files on storage. | Re-enabling allows online sync. | Not actively enforced on existing downloaded files. |
| **P2P / Offline Sharing Ban** | `canP2P`: Boolean (default true)<br>`p2pBan`: Boolean (default false) | Direct Sharing (`ShareScreen`, `P2pWifiDirectManager`) | `ShareScreen:119-156` checks `!restrictions.isP2PAllowed`. Renders full-screen block message and aborts Wi-Fi Direct socket server initialization. | `CLIENT-ONLY GAP`. Local Wi-Fi Direct socket communication on port 8888 does not consult Firestore while disconnected from the Internet. | Restoring `canP2P = true` re-enables Wi-Fi Direct discovery and file transfer controls. | Admin message displayed: "P2P sharing has been disabled for this account by administration". |

---

## 7. Notifications Matrix

The User App implements a multi-tier notification architecture encompassing Cloud Messaging (FCM), Firestore announcement collections, and local Room caching:

| Notification Type | Delivery Channel | Trigger / Source Path | Document Fields & Schemas | Targeting & Addressing | Deduplication & Filtering Mechanism |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Global System Announcement** | Firestore Poll / Real-time Listener | `/notifications/{notificationId}` | `title` (String)<br>`body` / `message` (String)<br>`imageUrl` (String, opt)<br>`type` (String, e.g. "info", "system")<br>`event` (String, opt)<br>`isActive` (Boolean)<br>`expiresAt` (Timestamp/Long)<br>`createdAt` (Timestamp/Long) | Broadcast: `target: "all"` or omitted. All active app instances receive document. | `NotificationDeduplicator` caches doc IDs in DataStore. `NotificationPreferencesRepository` filters out categories disabled by user. Inserted into Room `notificationDao`. |
| **User-Targeted Cloud Notification** | Firestore Poll / Real-time Listener | `/notifications/{notificationId}` | Same schema as global, plus:<br>`target: "user"`<br>`targetUid: "<UID>"` | Targeted: Document contains `targetUid`. `NotificationRepository:125` ignores document if `currentUid != targetUid`. | Ignored by other users' devices during collection scan. Deduplicated and saved to Room. |
| **Push Notification (FCM Background)** | Firebase Cloud Messaging (Google Play Services) | External FCM Server Payload via `AppFirebaseMessagingService` | RemoteMessage data payload:<br>`title`, `body`<br>`type` (movies, series, anime, social, system)<br>`event`, `mediaId`, `mediaType`<br>`imageUrl` | Delivered directly to device registration token stored under `/users/{uid}/fcmTokens/{installationId}`. | `AppFirebaseMessagingService:70-110` checks `NotificationCategoryResolver.isNotificationAllowed()`. Suppresses tray alert if category disabled in preferences. |
| **Multi-Device Token Synchronization** | Client Background Service (`FcmTokenManager`) | `/users/{uid}/fcmTokens/{installationId}` | `token` (String)<br>`installationId` (UUID)<br>`platform` ("android")<br>`deviceModel` (String)<br>`osVersion` (String)<br>`appVersion` (String)<br>`isActive` (Boolean)<br>`createdAt` (Timestamp)<br>`updatedAt` (Timestamp)<br>`lastSeenAt` (Timestamp) | Associated with current authenticated user `uid`. Installation ID ensures each physical device has a dedicated document. | Stable `installationId` stored in DataStore. On sign-out, token is marked inactive or dissociated to prevent cross-account notification leaks. |
| **In-App Notification Center UI** | Local Room Database (`notificationDao`) | Room Table: `notifications` populated by `NotificationRepository` | `id` (String)<br>`title` (String)<br>`message` (String)<br>`timestamp` (Long)<br>`isRead` (Boolean)<br>`imageUrl` (String)<br>`type` (String) | User's local on-device inbox. Accessible from Home top bar. | Read status toggled locally (`isRead = true`). Badge count driven by Room `getUnreadCount()`. |

---

## 8. Global Configuration & Maintenance Mode Matrix

The User App consumes global configurations from distinct Firestore documents under `/config/*`:

| Configuration Domain | Firestore Target Path | Fields, Types & Defaults | User App Consumption Mechanism | Admin Exemption Logic | Fallback & Resilience |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **App Startup & Maintenance** | `/config/app` | `maintenanceEnabled`: Boolean (false)<br>`maintenanceTitle`: String ("Under Scheduled Maintenance")<br>`maintenanceMessage`: String ("...")<br>`maintenanceAllowedRoles`: List&lt;String&gt; (["admin", "superadmin"])<br>`defaultOfflineDays`: Long (2)<br>`defaultForcedAds`: Long (5)<br>`minimumVersionCode`: Long (1)<br>`latestVersionCode`: Long (1)<br>`apkUrl`: String ("") | `AppStartupManager.checkMaintenanceStatus()` attempts `Source.SERVER` fetch. Updates `_maintenanceFlow`. If active, `MainActivity` displays `MaintenanceScreen`. | **Exemption Active:** `AppStartupManager.kt:72-73` evaluates `isUserExemptFromMaintenance()`. Returns true if `UserSecurityManager.isAdmin()` or user role is in `maintenanceAllowedRoles`. | If offline or Firestore read fails, falls back to cached preferences in DataStore (`app_startup_prefs`). Default is maintenance disabled. |
| **Feature Flags** | `/config/features` | `subscriptions`: FeatureState (`ACTIVE`, `DISABLED`, `COMING_SOON`)<br>`points`: FeatureState<br>`dailyLogin`: FeatureState<br>`rewardedAds`: FeatureState<br>`tasks`: FeatureState<br>`leaderboard`: FeatureState<br>`disabledMessage`: String ("هذه الميزة غير متوفرة حالياً") | `EconomyConfigRepository.startListening()` attaches snapshot listener. Emits `FeaturesConfig`. UI components check feature state before rendering tabs or claim buttons. | None. Feature flags apply globally to all users viewing the feature. | Defaults to all features `ACTIVE` if document is missing or read fails. |
| **Economy Tuning** | `/config/economy` | `redemptionCosts`: Map&lt;String, Long&gt;<br>`dailyLoginRewards`: List&lt;Long&gt; (e.g. [10, 20, 30, ...])<br>`rewardedAdPoints`: Long (15)<br>`rewardedAdDailyCap`: Long (5)<br>`rewardedAdCooldownSeconds`: Long (300) | `EconomyConfigRepository.startListening()` attaches snapshot listener. Emits `EconomyConfig`. Feeds `PointsEarningViewModel` reward timers and daily claim ladders. | None. Configures uniform economic rules. | Defaults to hardcoded fallback constants (`dailyLoginRewards = [10, 20, ...]`, `rewardedAdPoints = 15`) on error. |
| **Search Order & Dubbing** | `/config/search_order` | `movie`: List&lt;String&gt;<br>`tv`: List&lt;String&gt;<br>`series`: List&lt;String&gt;<br>`anime`: List&lt;String&gt;<br>`arabicDubbed`: List&lt;String&gt;<br>`englishDubbed`: List&lt;String&gt;<br>`dubbingConfig`: Map&lt;String, Any&gt;<br>`updatedAt`: Long/Timestamp | `SearchOrderDataSource.fetchSearchOrder()` reads document. Feeds `IntelligentDubbedSearchEngine` and `ManagedMediaOrchestrator` to sort scraper execution priority. | None. Affects all client media searches. | Falls back to default hardcoded scraper priority if document missing. |
| **Deprecated Config Paths** | `/config/{document=**}` | Any legacy or arbitrary document key | N/A | N/A | Hardened Firestore rules explicitly block all reads and writes to wildcard config sub-paths: `allow read, write: if false;`. |

---

## 9. Extensions & Scrapers Detailed Inventory

### 9.1 Forensic Discovery: What Are "Extensions" in CineStream?
In the CineStream codebase, "Extensions" are **NOT** dynamic APK plugins, third-party APKs, or dynamically loaded `.dex`/`.jar` code (which would violate Google Play Developer Program Policies on Dynamic Code Loading).  
Instead, Extensions are **statically compiled Kotlin media scrapers and site-specific extraction engines** residing in `com.example.extension.managed.scraper.*` (e.g., `Anime4UpScraper`, `EgyDeadScraper`, `WitAnimeScraper`, `AnimeBlkomScraper`, `AkwamScraper`).  

Firestore's `/managed_extensions/{extensionId}` collection acts as a **remote operational control plane**: it supplies base URLs, search endpoints, priorities, enabled/disabled toggles, and metadata flags for each statically compiled scraper engine.

### 9.2 Extensions Inventory Table

| Extension ID (`scraperKey`) | Primary Base URL | Supported Content Types | Remote Control Parameters (`/managed_extensions/{id}`) | Priority & Fallback Order | Health & Failure Handling | Can Owner Alter Without Releasing New App? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`anime4up`** | `https://anime4up.cx` | `["ANIME", "SERIES", "MOVIE"]` | `enabled`: Boolean<br>`status`: "ACTIVE"/"DISABLED"<br>`baseUrl`: String<br>`searchUrl`: String<br>`priority`: Long<br>`definitionVersion`: Long | Defined in `/config/search_order.anime` and `arabicDubbed`. | If site changes domain or blocks requests, scraper reports failure to `ManagedMediaOrchestrator`. Orchestrator fails over to next scraper. | **YES (Domain & Enablement Only):** Owner can update `baseUrl`, `searchUrl`, `priority`, or set `enabled = false`. **NO (HTML DOM Parsing):** If site changes HTML layout, app requires code update. |
| **`egydead`** | `https://egydead.live` | `["MOVIE", "SERIES", "ANIME"]` | `enabled`: Boolean<br>`status`: "ACTIVE"/"DISABLED"<br>`baseUrl`: String<br>`searchUrl`: String<br>`priority`: Long | Configured in `/config/search_order.movie` and `series`. | Evaluated in parallel or sequential search cascades. Failure triggers fallback. | **YES (Domain & Enablement Only)**. HTML structural changes require app release. |
| **`witanime`** | `https://witanime.pics` | `["ANIME", "MOVIE"]` | Same schema | Configured in `/config/search_order.anime`. | Handled by `EgyDeadAndWitAnimeScraperTest`. | **YES (Domain & Enablement Only)**. |
| **`animeblkom`** | `https://animeblkom.net` | `["ANIME"]` | Same schema | Configured in `/config/search_order.anime`. | Handled by `Anime4UpAndAnimeBlkomScraperTest`. | **YES (Domain & Enablement Only)**. |
| **`akwam`** | `https://akwam.to` | `["MOVIE", "SERIES"]` | Same schema | Configured in `/config/search_order.movie`. | Handled by media orchestrator fallback chain. | **YES (Domain & Enablement Only)**. |

---

## 10. Update Management Matrix

The application provides dual-source checking for application updates:

| Update Source Path | Evaluated Fields & Types | Version Comparison Logic | Mandatory Update Behavior | Integrity & Security Verification | Missing Capabilities & Future Admin Requirements |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Dedicated Catalog:** `/app_updates/{updateId}` | `versionCode`: Long<br>`versionName`: String<br>`releaseNotes`: String<br>`downloadUrl` / `apkUrl`: String<br>`minVersionCode`: Long<br>`isMandatory`: Boolean<br>`isActive`: Boolean<br>`sha256`: String<br>`signature`: String<br>`publicKey`: String | Numerical comparison: `targetCode > currentCode`. Mandatory if `isMandatory == true` OR if `currentCode < minVersionCode`. | `isMandatory = true` prevents dismissal of update dialog; user must tap update URL to proceed. | Document supports `sha256` checksum, `signature`, and `publicKey` fields to prevent rogue APK installation. | Needs centralized creation tool in Admin Dashboard to publish release objects with SHA256 hashes. |
| **Global Config Fallback:** `/config/app` | `latestVersionCode`: Long<br>`latestVersionName`: String<br>`minimumVersionCode`: Long<br>`mandatoryUpdate`: Boolean<br>`apkUrl`: String<br>`apkSha256`: String<br>`releaseNotes`: String | Evaluated during startup check in `AppStartupManager.kt:140-155`. | If `currentCode < minCode` or `mandatoryUpdate == true`, user is blocked from entering app until update is accepted. | Uses `apkSha256` string match. | Inconsistency: Having two update sources (`/config/app` and `/app_updates`) creates potential drift if admin updates one but not the other. |

---

## 11. Support Current Contract (User App Implementation)

This section documents the actual, exact implementation of Support Communications currently running in the CineStream User App:

### 11.1 Support Thread Architecture
- **Thread Parent Path:** `/support_conversations/{userId}`  
  *Note:* The document ID is always the user's Auth UID (`user.uid`). There is exactly one thread per user.
- **Messages Subcollection:** `/support_conversations/{userId}/messages/{messageId}`  
  *Note:* Each message is stored as an individual document with a UUID.
- **Local Room Database Mirroring:** Messages are mirrored in SQLite table `support_messages` via `SupportDao` (`SupportViewModel.kt:30`).

### 11.2 Actual Document Schemas & Exact String Literals
1. **Thread Parent Document (`/support_conversations/{userId}`):**
   ```json
   {
     "conversationId": "string (user.uid)",
     "userId": "string (user.uid)",
     "userEmail": "string",
     "userName": "string",
     "status": "open",
     "lastMessage": "string",
     "lastMessageTime": 1729000000000,
     "updatedAt": "FieldValue.serverTimestamp()"
   }
   ```
   *Strict Casing Fact:* In `SupportViewModel.kt:105`, the status is written literally as lowercase `"open"`.
2. **Message Document (`/support_conversations/{userId}/messages/{messageId}`):**
   ```json
   {
     "id": "string (UUID)",
     "messageId": "string (UUID)",
     "conversationId": "string (user.uid)",
     "senderId": "string (user.uid)",
     "senderRole": "user",
     "text": "string",
     "timestamp": 1729000000000,
     "createdAt": "FieldValue.serverTimestamp()"
   }
   ```
   *Strict Casing Fact:* User App messages set `senderRole: "user"`. Inbound admin messages are evaluated with `senderRole == "admin"`.

### 11.3 Firestore Security Rules Constraints
- **Read:** User can read their own thread; Admin can read all threads (`firestore.rules:282, 309`).
- **Create Thread:** User can create thread with `status: 'open'` or `'pending'` (`firestore.rules:287-290`).
- **Update Thread (Hardened in B2):** User CANNOT alter `userId` or change `status`. If `status` is provided by user, it MUST match existing status (`status: "open" == "open"`). Only Admin can change status (e.g. to `"resolved"`, `"in_progress"`). Verified live in test D04.
- **Send Message:** User can create message only if `senderRole == 'user'` and `senderId == auth.uid`. Admin sends messages with `senderRole == 'admin'`. Normal users spoofing admin role are rejected (`PERMISSION_DENIED`). Verified live in test D02.
- **Delete Thread / Messages:** Blocked for regular users; allowed only for Admin (`firestore.rules:304, 325`).

### 11.4 Identified Gaps in Current User App Support
- **No Image or Media Attachments:** `SupportViewModel` only supports plain text (`text: String`).
- **No Unread Message Badge on Thread:** No `unreadCountUser` or `unreadCountAdmin` field exists.
- **No Ticket Category / Subject:** All inquiries go into a single unstructured chat thread.
- **Guest Fallback:** If user is unauthenticated, `SupportViewModel` simulates a local canned bot response (stored in Room only) and never contacts Firestore.

---

## 12. PROPOSED SUPPORT ADMIN CONTRACT — NOT YET IMPLEMENTED

> [!IMPORTANT]
> **DISCLAIMER:** The fields, structures, and schemas in this section are **PROPOSALS FOR FUTURE IMPLEMENTATION**. They DO NOT currently exist in the User App or Firestore database. They are documented here to serve as the design specification for the upcoming Admin Dashboard support page.

To transform CineStream support into an enterprise-grade ticketing and chat system, the following contract extensions are proposed:

### 12.1 Proposed Thread Parent Extension (`/support_conversations/{conversationId}`)

| Proposed Field | Data Type | Required / Optional | Read Access | Write Access | Purpose & Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `conversationId` | String | Required | User, Admin | System / User | Existing: Unique identifier (currently `user.uid`). |
| `userId` | String | Required | User, Admin | System / User | Existing: Identifies ticket author. |
| `userEmail` | String | Optional | User, Admin | User / Admin | Existing: Contact email. |
| `userName` | String | Optional | User, Admin | User / Admin | Existing: User display name. |
| `subject` | String | Optional | User, Admin | User (on create) | **NEW:** Brief summary or topic of issue (e.g., "Playback Error on EgyDead"). |
| `category` | String (Enum) | Required | User, Admin | User (on create), Admin | **NEW:** Categorization (`"BILLING"`, `"PLAYBACK"`, `"ACCOUNT"`, `"BUG_REPORT"`, `"GENERAL"`). |
| `priority` | String (Enum) | Required | User, Admin | Admin Only | **NEW:** Ticket priority (`"LOW"`, `"NORMAL"`, `"HIGH"`, `"URGENT"`). Defaults to `"NORMAL"`. |
| `status` | String (Enum) | Required | User, Admin | Admin (User can only set `'open'` on create) | **PROPOSAL TO FORMALIZE:** Canonical lowercase string: `'open'`, `'in_progress'`, `'waiting_on_user'`, `'resolved'`, `'closed'`. |
| `assignedAdminId` | String | Optional | Admin Only | Admin Only | **NEW:** UID of support agent assigned to handle this ticket. |
| `assignedAdminName`| String | Optional | User, Admin | Admin Only | **NEW:** Display name of assigned support agent shown to user. |
| `unreadCountUser` | Int | Required | User, Admin | Admin (increments), User (resets to 0) | **NEW:** Number of unread admin messages for the user. Powers top bar notification badge. |
| `unreadCountAdmin`| Int | Required | Admin Only | User (increments), Admin (resets to 0) | **NEW:** Number of unread user messages for support staff. Powers Admin Dashboard badge. |
| `lastMessage` | String | Optional | User, Admin | User, Admin | Existing: Preview snippet of the most recent message. |
| `lastMessageSender`| String | Optional | User, Admin | User, Admin | **NEW:** `'user'` or `'admin'`. Helps dashboard filter threads awaiting response. |
| `lastMessageTime` | Long / Timestamp | Required | User, Admin | User, Admin | Existing: Timestamp of latest message. Used for thread sorting. |
| `createdAt` | Timestamp | Required | User, Admin | User (on create) | Existing: Creation timestamp. |
| `updatedAt` | Timestamp | Required | User, Admin | User, Admin | Existing: Last activity timestamp. |
| `resolvedAt` | Timestamp | Optional | User, Admin | Admin Only | **NEW:** Timestamp when ticket status was changed to `'resolved'`. |
| `closedAt` | Timestamp | Optional | User, Admin | Admin Only | **NEW:** Timestamp when ticket was permanently closed. |

### 12.2 Proposed Message Extension (`/support_conversations/{conversationId}/messages/{messageId}`)

| Proposed Field | Data Type | Required / Optional | Read Access | Write Access | Purpose & Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `id` / `messageId` | String (UUID) | Required | User, Admin | Author | Existing: Unique message document ID. |
| `conversationId` | String | Required | User, Admin | Author | Existing: Parent thread ID. |
| `senderId` | String | Required | User, Admin | Author | Existing: UID of the author. |
| `senderRole` | String (Enum) | Required | User, Admin | Author (Enforced by rules) | Existing: Strictly `'user'` or `'admin'`. (Future: `'system'` for automated bot events). |
| `senderName` | String | Optional | User, Admin | Author | **NEW:** Name of author displayed in bubble. |
| `text` | String | Required | User, Admin | Author | Existing: Text content of message. |
| `attachments` | List&lt;Map&gt; | Optional | User, Admin | Author | **NEW:** List of image/log attachments: `[{"url": "...", "type": "image/jpeg", "name": "screenshot.png"}]`. |
| `isInternalNote` | Boolean | Optional | Admin Only | Admin Only | **NEW:** If true, visible ONLY to admins in dashboard (internal staff discussion on ticket). |
| `timestamp` | Long | Required | User, Admin | Author | Existing: Client Unix timestamp in millis. |
| `createdAt` | Timestamp | Required | User, Admin | Author | Existing: Server timestamp. |

---

## 13. Security Findings & Risk Prioritization

| Priority | Finding ID | Severity & Vulnerability Title | Exact Code Evidence | Impact & Exploitability Analysis | Concrete Remediation Recommendation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **P1** | **SEC-01** | **Client-Side-Only Watch & Download Restrictions** | `PlayerScreen.kt:726-733`, `StreamDownloaderService.kt:190` | Third-party media scrapers and video CDNs (EgyDead, Anime4Up, Akwam) do not evaluate Firebase auth or ban tokens. A modified APK can comment out the ExoPlayer pause call and stream/download videos despite an active admin ban. | In a future architecture phase, route media streaming requests through a lightweight authenticated proxy or signed streaming worker if strict DRM-like ban enforcement is required. |
| **P1** | **SEC-02** | **Offline Playback Expiration Not Hooked** | `UserSecurityManager.kt:265-267`, `DownloadsScreen.kt:150-250` | `offlineDaysOverride` exists and is parsed, but downloaded files are stored as plain MP4/MKV in `/storage/emulated/0/...` without playback expiration logic. Offline users can play existing downloads indefinitely even after ban. | Integrate an offline license verification check in `DownloadsViewModel` comparing local system clock and last online token receipt against `offlineDaysLimit`. |
| **P1** | **SEC-03** | **Peer-to-Peer Wi-Fi Direct Ban Ineffective Offline** | `ShareScreen.kt:119-156`, `P2pWifiDirectManager.kt:150` | P2P sharing restriction is gated by Compose UI. When two devices are offline in the field, local Wi-Fi Direct socket transfers on port 8888 operate peer-to-peer without contacting Firestore to learn of new bans. | Embed a signed peer-attestation challenge during the initial socket handshake requiring a cached, unexpired online attestation ticket before accepting file bytes. |
| **P2** | **SEC-04** | **Direct User Search Query Incompatibility with Hardened Rules** | `SocialRepository.kt:302-305`, `firestore.rules:58` | `SocialRepository.searchUsers()` executes a range query against `/users`. Because `/users/{userId}` read rule is hardened to `isOwner(userId) \|\| isAdmin()` (to protect private emails and balances), search queries fail with `PERMISSION_DENIED`. | Deploy a dedicated public projection subcollection `/users/{userId}/public/profile` or Algolia/Cloudflare search worker returning only sanitized public fields (`username`, `displayName`, `photoUrl`). |
| **P2** | **SEC-05** | **User-to-User Blocking Not Stored in Cloud** | `UserPreferencesRepository.kt:37, 127` | Peer blocking is saved exclusively in Android DataStore on the local device. Firestore rules do not know about peer blocks; blocked users can still write messages into shared `/conversations/{convId}` documents. | Create a cloud subcollection `/users/{uid}/blocked_users/{targetUid}` and add an `exists()` check in `firestore.rules` on message creation. |
| **P2** | **SEC-06** | **Dual Update Management Configuration Drift** | `AppConfig.kt:8-24`, `AppUpdateManager.kt:58-140` | Update parameters are maintained in two separate places: `/config/app` (monolithic) and `/app_updates/{updateId}` (catalog). Updating one without the other can cause inconsistent update prompts across app startup and settings. | Standardize Admin Dashboard to update both paths atomically, or deprecate `/config/app` update fields in favor of `/app_updates`. |
| **P3** | **SEC-07** | **Account Deletion Swallows Support Conversation Deletion Failure** | `AuthRepository.kt:595-601`, `firestore.rules:304` | During user account deletion, `AuthRepository` attempts to delete `/support_conversations/{uid}/messages`. Hardened rules deny delete to users (`allow delete: if isAdmin()`). The exception is caught and swallowed. | Intended behavior for audit trail preservation, but client should document this partial deletion state explicitly rather than silently catching. |
| **P3** | **SEC-08** | **Temporary Economy Mode Client Minting Incompatibility** | `TemporaryFirebaseEconomyRepository.kt:101-140`, `firestore.rules:95-118` | Hardened rules in B2 correctly blocked client writes to `pointsBalance` and `point_transactions`. Running in temporary economy mode causes client transaction failure. | Retain hardened rules; route all economy operations through `PointsEarningRepository.trustedBackendUrl` backed by a Cloudflare Worker or Cloud Function. |

---

## 14. Runtime Test Evidence & Emulator Verification

All security rules and access constraints documented in this audit were verified live against the local Firebase Firestore Emulator:

### 14.1 Emulator Test Harness Execution
- **Command:**
  ```bash
  firebase emulators:exec --only firestore "node --test tests/rules/firestore_security_regression.test.js"
  ```
- **Execution Date:** October 15, 2026 (13:04:28Z)
- **Host & Port:** `127.0.0.1:8085` (Local isolated test environment)
- **Target Rules File:** `/firestore.rules` (v2 cloud security rules, 403 lines)
- **Execution Summary:**
  - **Total Test Suites:** 5 suites
  - **Total Test Cases:** 49 tests
  - **Passed:** **49 (100% PASS)**
  - **Failed:** **0**
  - **Cancelled / Skipped:** **0**
  - **Total Duration:** 14,234.98 ms

### 14.2 Suite-by-Suite Test Evidence Breakdown
1. **Suite A: User Data & Profiles (`/users/{userId}`) — 13 Tests (13 Passed):**
   - `A01`: Owner can create profile with safe free defaults (`PASS`).
   - `A02`: User cannot create profile with `role: "admin"` (`PASS` - `PERMISSION_DENIED`).
   - `A03`: User cannot create profile for another user (`PASS` - `PERMISSION_DENIED`).
   - `A04`: Unauthenticated user cannot read profiles (`PASS` - `PERMISSION_DENIED`).
   - `A05`: Cross-user sensitive data read blocked for normal users (`PASS` - `PERMISSION_DENIED`).
   - `A06`: User cannot update another user's document (`PASS` - `PERMISSION_DENIED`).
   - `A07`: User cannot self-grant admin role (`PASS` - `PERMISSION_DENIED`).
   - `A08`: User cannot clear own account bans (`PASS` - `PERMISSION_DENIED`).
   - `A09`: User cannot self-grant subscriptions (`PASS` - `PERMISSION_DENIED`).
   - `A10`: User can read own private user document (`PASS`).
   - `A11`: Verified Admin can read any user document (`PASS`).
   - `A12`: User can update legitimate profile fields (displayName, bio, photoUrl) (`PASS`).
   - `A13`: Admin can update subscription tiers and limits for users (`PASS`).
2. **Suite B: Points & Economy System — 11 Tests (11 Passed):**
   - `B01`: User cannot directly update `pointsBalance` (`PASS` - `PERMISSION_DENIED`).
   - `B02`: User cannot write to `point_transactions` ledger (`PASS` - `PERMISSION_DENIED`).
   - `B03`: Cross-user ledger isolation enforced (`PASS` - `PERMISSION_DENIED`).
   - `B04`: User cannot delete task claims (`PASS` - `PERMISSION_DENIED`).
   - `B05`: User can submit legitimate `PENDING` pro request (`PASS`).
   - `B06`: User cannot self-approve pro request (`PASS` - `PERMISSION_DENIED`).
   - `B07`: User cannot modify reward tasks catalog (`PASS` - `PERMISSION_DENIED`).
   - `B08`: User cannot tamper with leaderboard documents (`PASS` - `PERMISSION_DENIED`).
   - `B09`: Admin can adjust user points balance (`PASS`).
   - `B10`: Admin can record ledger transactions (`PASS`).
   - `B11`: User cannot update existing task claim (`PASS` - `PERMISSION_DENIED`).
3. **Suite C: Social Conversations & Messaging — 11 Tests (11 Passed):**
   - `C01`: Participant can create conversation document (`PASS`).
   - `C02`: Non-participant cannot create conversation (`PASS` - `PERMISSION_DENIED`).
   - `C03`: Non-participant cannot read conversation messages (`PASS` - `PERMISSION_DENIED`).
   - `C04`: Participant can send message matching sender ID (`PASS`).
   - `C05`: Participant cannot forge another user's sender ID (`PASS` - `PERMISSION_DENIED`).
   - `C06`: Chat-banned user cannot send messages (`PASS` - `PERMISSION_DENIED`).
   - `C07`: Recipient can add reaction to message (`PASS`).
   - `C08`: Documented peer-blocking gap: blocked user writes to Firestore (`PASS - Architectural Gap Verified`).
   - `C09`: Recipient can delete message for themselves via `deletedFor` (`PASS`).
   - `C10`: Recipient cannot tamper with message text or sender ID (`PASS` - `PERMISSION_DENIED`).
   - `C11`: Original sender can edit message text (`PASS`).
4. **Suite D: Support Conversations System — 7 Tests (7 Passed):**
   - `D01`: User can create support conversation and send user messages (`PASS`).
   - `D02`: User cannot spoof admin role (`senderRole: "admin"`) in support messages (`PASS` - `PERMISSION_DENIED`).
   - `D03`: User cannot read other users' support threads (`PASS` - `PERMISSION_DENIED`).
   - `D04`: User cannot tamper with ticket status (e.g. from resolved to open) (`PASS` - `PERMISSION_DENIED`).
   - `D05`: Normal user cannot delete support conversation or messages (`PASS` - `PERMISSION_DENIED`).
   - `D06`: User can update thread metadata while status remains open (`PASS`).
   - `D07`: Admin can update ticket status to resolved or in_progress (`PASS`).
5. **Suite E: Admin Privileges & System Integrity — 7 Tests (7 Passed):**
   - `E01`: Normal user cannot write to `/admins` collection (`PASS` - `PERMISSION_DENIED`).
   - `E02`: User with owner email cannot perform admin actions without `/admins` doc (`PASS` - `PERMISSION_DENIED`).
   - `E03`: Verified Admin can update system configuration in `/config/app` (`PASS`).
   - `E04`: Normal user cannot modify system configuration (`PASS` - `PERMISSION_DENIED`).
   - `E05`: Normal user cannot read or write audit logs in `/auditLogs` (`PASS` - `PERMISSION_DENIED`).
   - `E06`: Wildcard config sub-paths are completely blocked (`PASS` - `PERMISSION_DENIED`).
   - `E07`: Deprecated `/audit_logs` collection is completely forbidden (`PASS` - `PERMISSION_DENIED`).

### 14.3 Android Applet Compilation Evidence
- **Tool:** `compile_applet`
- **Result:** `Build succeeded - the applet is compiled`
- **Confirmation:** Zero compiler errors, zero unresolved symbols across all repositories, models, and ViewModels.

---

## 15. Admin Audit Handoff & Discovery Checklist

When initiating the upcoming audit of the **CineStream Admin Dashboard** in its dedicated session, the following technical items must be verified against the Admin codebase:

| Item # | Inspection Question for Admin Dashboard | Corresponding User App Contract | Risk / Drift to Prevent |
| :--- | :--- | :--- | :--- |
| **H-01** | Does the Admin Dashboard update `/users/{uid}` using exact canonical field names? | `isBanned`, `isActive`, `banReason`, `canWatch`, `watchBan`, `canDownload`, `downloadBan`, `canChat`, `chatBan`, `canStory`, `storyBan`, `canP2P`, `p2pBan`, `subscriptionTier`, `isPremium`, `subscriptionStatus`, `pointsBalance` | If Admin writes `banned` instead of `isBanned`, or `pro` instead of `isPremium`, the User App will not detect the change. |
| **H-02** | What literal casing does Admin write for `status` in `/support_conversations`? | User App strictly expects lowercase: `"open"`, `"in_progress"`, `"resolved"` | If Admin writes uppercase `"OPEN"` or `"RESOLVED"`, `SupportViewModel.kt:105` which writes `"open"` will cause a mismatch and trigger Firestore rule rejection. |
| **H-03** | How does Admin authenticate support message replies? | `/support_conversations/{uid}/messages/{msgId}` requires `senderRole: "admin"` | If Admin omits `senderRole` or sends `"staff"`, rules reject the write with `PERMISSION_DENIED`. |
| **H-04** | Does Admin update both `/config/app` and `/app_updates` when publishing releases? | User App startup checks `/config/app`; update dialog queries `/app_updates` | Updating only `/app_updates` leaves the startup check on older version metadata. |
| **H-05** | How does Admin approve `/pro_requests`? | User App expects Admin to mark request `status: "APPROVED"`, and atomically write `subscriptionTier: "PRO"` and `isPremium: true` to `/users/{uid}` | If Admin only updates the `/pro_requests` doc without updating `/users/{uid}`, user never receives ad-free entitlement. |
| **H-06** | How does Admin handle points adjustments? | User App listens to `/users/{uid}.pointsBalance` and `/users/{uid}/point_transactions` | Admin must write both the new balance and the audit transaction doc atomically to maintain ledger integrity. |
| **H-07** | Does Admin write `/config/search_order` in the format expected by the Dubbed Search Engine? | Expects `movie`, `tv`, `series`, `anime`, `arabicDubbed`, `englishDubbed` as string lists of extension IDs | Incorrect JSON structure will break the User App search cascade. |
| **H-08** | How is the Admin user itself authenticated in Firestore? | User App rules require a document at `/admins/{adminUid}` with `enabled: true` | If Admin relies only on email or custom claims without the `/admins` document, all admin writes fail. |

---

## 16. Audit Sign-Off & Status Declaration

**Final Operational Status:**
`COMPREHENSIVE READ-ONLY AUDIT — REPORT ONLY — NO CODE CHANGES — NO DEPLOYMENT`

- **Source Code Modified:** NONE (0 files).
- **Rules Modified:** NONE (0 files).
- **Production State:** UNTOUCHED.
- **QFilm & Video Scrapers:** UNTOUCHED.
- **Deliverable:** Fully synthesized in this document.

*End of Forensic Report.*
