/**
 * PHASE 20.1-B1 — FIRESTORE SECURITY REGRESSION TEST HARNESS
 * 
 * Target: /firestore.rules
 * Test runner: Node.js built-in test runner (node:test)
 * Framework: @firebase/rules-unit-testing
 * Execution environment: Local Firebase Firestore Emulator (127.0.0.1:8085)
 * 
 * Strict safety rules:
 * - Read-only against production (targets local demo project "demo-cinestream-test")
 * - Zero production mutations, zero deployments
 * - Accurately records whether current rules ALLOW (including vulnerabilities) or DENY
 */

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach, describe } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const PROJECT_ID = "demo-cinestream-test";
const EMULATOR_HOST = "127.0.0.1";
const EMULATOR_PORT = 8085;

// Test Actors
const ALICE_UID = "user_alice_123";
const BOB_UID = "user_bob_456";
const CHARLIE_UID = "user_charlie_789";
const ADMIN_UID = "admin_master_001";
const OWNER_EMAIL_UID = "user_owner_email_999"; // User with sulopros01@gmail.com but no /admins doc

let testEnv;

before(async () => {
  const rulesPath = path.resolve(__dirname, "../../firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");

  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: EMULATOR_HOST,
      port: EMULATOR_PORT,
      rules,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// Helper to seed data bypassing security rules
async function seedData(fn) {
  await testEnv.withSecurityRulesDisabled(fn);
}

// =========================================================================
// SUITE A: USER DATA & PROFILES (/users/{userId})
// =========================================================================
describe("Suite A: User Data & Profiles (/users/{userId})", () => {
  test("A01: [AUTHORIZED] User can create own initial profile with valid free defaults", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        email: "alice@test.com",
        displayName: "Alice",
        role: "user",
        isPremium: false,
        subscriptionTier: "free",
        plan: "free",
        planId: "free",
        durationDays: 0,
        subscriptionStatus: "none",
        pointsBalance: 0,
        totalPointsEarned: 0,
        totalPointsSpent: 0,
        isBanned: false,
        isActive: true,
        canWatch: true,
        canDownload: true,
        canChat: true,
        canStory: true,
        canP2P: true,
        createdAt: new Date(),
      })
    );
  });

  test("A02: [SECURITY BOUND] User CANNOT create initial profile with role=admin", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        email: "alice@test.com",
        role: "admin",
        isPremium: false,
      })
    );
  });

  test("A03: [SECURITY BOUND] User CANNOT create profile for another user (cross-user create)", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("users").doc(BOB_UID).set({
        uid: BOB_UID,
        email: "bob@test.com",
        role: "user",
        isPremium: false,
      })
    );
  });

  test("A04: [SECURITY BOUND] Unauthenticated user CANNOT read user profiles", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        email: "alice@test.com",
        displayName: "Alice",
      });
    });

    const unauthDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  });

  test("A05: [CONFIRMED VULNERABILITY] Cross-user sensitive data read: Authenticated user CAN read other user's full doc including points, email, ban status", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(BOB_UID).set({
        uid: BOB_UID,
        email: "bob_private@test.com",
        pointsBalance: 50000,
        subscriptionTier: "PRO",
        isBanned: true,
        banReason: "Suspicious behavior",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // In current firestore.rules:58, `allow read: if isAuthenticated();` exposes the entire document
    await assertSucceeds(aliceDb.collection("users").doc(BOB_UID).get());
  });

  test("A06: [SECURITY BOUND] User CANNOT update another user's document", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(BOB_UID).set({
        uid: BOB_UID,
        displayName: "Bob",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("users").doc(BOB_UID).update({
        displayName: "Bob Hacked",
      })
    );
  });

  test("A07: [SECURITY BOUND] User CANNOT update own role or admin fields", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        role: "user",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        role: "admin",
      })
    );
  });

  test("A08: [SECURITY BOUND] User CANNOT clear their own account bans", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        isBanned: true,
        banReason: "Policy violation",
        canChat: false,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        isBanned: false,
        canChat: true,
      })
    );
  });

  test("A09: [CONFIRMED VULNERABILITY] Self-Grant Subscription: Current rules ALLOW user to update isPremium, subscriptionTier, and subscriptionExpiresAt directly", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        isPremium: false,
        subscriptionTier: "free",
        subscriptionStatus: "none",
        subscriptionExpiresAt: 0,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // Vulnerability in firestore.rules:97-106: subscription keys omitted from affectedKeys blocklist
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).update({
        isPremium: true,
        subscriptionTier: "PRO",
        subscriptionStatus: "ACTIVE",
        subscriptionExpiresAt: 4102444800000, // Year 2100
      })
    );
  });
});

// =========================================================================
// SUITE B: POINTS & ECONOMY (/users/{userId}/..., /pro_requests, /reward_tasks)
// =========================================================================
describe("Suite B: Points & Economy System", () => {
  test("B01: [CONFIRMED VULNERABILITY] Balance Inflation: Current rules ALLOW user to directly update pointsBalance", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        pointsBalance: 10,
        totalPointsEarned: 10,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // Vulnerability in firestore.rules:97-106: pointsBalance is omitted from affectedKeys blocklist
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).update({
        pointsBalance: 1000000,
        totalPointsEarned: 1000000,
      })
    );
  });

  test("B02: [CONFIRMED VULNERABILITY] Arbitrary Ledger Insertion: User can insert unverified transaction into point_transactions", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("point_transactions")
        .doc("tx_fake_001")
        .set({
          id: "tx_fake_001",
          userId: ALICE_UID,
          type: "FREE_MINT",
          amount: 50000,
          balanceAfter: 50000,
          createdAt: Date.now(),
        })
    );
  });

  test("B03: [SECURITY BOUND] Cross-user ledger isolation: User CANNOT write to another user's point_transactions", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb
        .collection("users")
        .doc(BOB_UID)
        .collection("point_transactions")
        .doc("tx_cross")
        .set({
          userId: BOB_UID,
          amount: 100,
        })
    );
  });

  test("B04: [SECURITY BOUND / BEHAVIORAL DISCOVERY] Task Claim deletion: User deleting task_claims document", async () => {
    await seedData(async (context) => {
      await context
        .firestore()
        .collection("users")
        .doc(ALICE_UID)
        .collection("task_claims")
        .doc("task_daily_01")
        .set({
          taskId: "task_daily_01",
          userId: ALICE_UID,
          claimedAt: Date.now(),
        });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // In firestore.rules:133: `allow write: if isAdmin() || (isOwner(userId) && request.resource.data.userId == userId);`
    // On DELETE, request.resource.data is undefined in Firestore Rules, causing evaluation to fail fast!
    // Therefore, deletion is DENIED, preventing task-claim reset via direct document deletion!
    await assertFails(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("task_claims")
        .doc("task_daily_01")
        .delete()
    );
  });

  test("B05: [AUTHORIZED] User can submit a legitimate PENDING pro_request", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("pro_requests").doc("req_alice_01").set({
        requestId: "req_alice_01",
        userId: ALICE_UID,
        planId: "pro_30d",
        durationDays: 30,
        status: "PENDING",
        notes: "Please upgrade",
      })
    );
  });

  test("B06: [SECURITY BOUND] User CANNOT self-approve pro_request (status=APPROVED)", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("pro_requests").doc("req_alice_02").set({
        requestId: "req_alice_02",
        userId: ALICE_UID,
        status: "APPROVED",
        isPremium: true,
      })
    );
  });

  test("B07: [SECURITY BOUND] User CANNOT modify or create reward_tasks", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("reward_tasks").doc("task_fake").set({
        taskId: "task_fake",
        rewardPoints: 10000,
        isActive: true,
      })
    );
  });

  test("B08: [SECURITY BOUND] User CANNOT modify leaderboard documents", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("leaderboard").doc("weekly_current").set({
        rankings: [],
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("leaderboard").doc("weekly_current").update({
        rankings: [{ userId: ALICE_UID, rank: 1, weeklyEarnedPoints: 99999 }],
      })
    );
  });
});

// =========================================================================
// SUITE C: SOCIAL CONVERSATIONS & MESSAGING (/conversations/{convId}/...)
// =========================================================================
describe("Suite C: Social Conversations & Messaging", () => {
  const CONV_ID = `${ALICE_UID}_${BOB_UID}`;

  beforeEach(async () => {
    // Seed participant user docs with clean permissions
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        canChat: true,
        chatBan: false,
        isBanned: false,
      });
      await context.firestore().collection("users").doc(BOB_UID).set({
        uid: BOB_UID,
        canChat: true,
        chatBan: false,
        isBanned: false,
      });
      await context.firestore().collection("users").doc(CHARLIE_UID).set({
        uid: CHARLIE_UID,
        canChat: true,
        chatBan: false,
        isBanned: false,
      });
    });
  });

  test("C01: [AUTHORIZED] Participant can create conversation document with themselves included", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
        lastMessage: "Hello Bob",
        lastMessageTime: Date.now(),
      })
    );
  });

  test("C02: [SECURITY BOUND] Non-participant CANNOT create conversation they are not in", async () => {
    const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
    await assertFails(
      charlieDb.collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID], // Charlie is NOT in participants
        lastMessage: "Eavesdropping",
      })
    );
  });

  test("C03: [SECURITY BOUND] Non-participant CANNOT read conversation document or messages", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
      await context
        .firestore()
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_1")
        .set({
          id: "msg_1",
          senderId: ALICE_UID,
          text: "Secret message",
        });
    });

    const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
    await assertFails(charlieDb.collection("conversations").doc(CONV_ID).get());
    await assertFails(
      charlieDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_1")
        .get()
    );
  });

  test("C04: [AUTHORIZED] Participant can send message with their own senderId", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_1")
        .set({
          id: "msg_alice_1",
          senderId: ALICE_UID,
          text: "Hi Bob!",
          timestamp: Date.now(),
        })
    );
  });

  test("C05: [SECURITY BOUND] Sender spoofing: Participant CANNOT forge another user's senderId", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_spoofed")
        .set({
          id: "msg_spoofed",
          senderId: BOB_UID, // Alice tries to pretend to be Bob
          text: "I confess to everything",
        })
    );
  });

  test("C06: [SECURITY BOUND] Chat-banned user CANNOT send messages", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).update({
        canChat: false,
        chatBan: true,
      });
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_banned")
        .set({
          id: "msg_banned",
          senderId: ALICE_UID,
          text: "I am banned but trying to chat",
        })
    );
  });

  test("C07: [CONFIRMED FUNCTIONAL BUG] Recipient CANNOT add reaction or delete for self on sender's message", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
      await context
        .firestore()
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_orig")
        .set({
          id: "msg_alice_orig",
          senderId: ALICE_UID,
          text: "Original from Alice",
          reactions: {},
          deletedFor: [],
        });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    // In SocialRepository.kt:366-384, Bob tries to react: msgRef.update("reactions", { bob: "❤️" })
    // Current firestore.rules:337 strictly requires: resource.data.senderId == request.auth.uid
    // Since senderId is Alice, Bob's reaction update FAILS!
    await assertFails(
      bobDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_orig")
        .update({
          reactions: { [BOB_UID]: "❤️" },
        })
    );
  });

  test("C08: [ARCHITECTURAL GAP] User-to-User blocking: Blocked user can still write to Firestore conversation", async () => {
    // In Android client, User A blocks User B in local DataStore (UserPreferencesRepository.BLOCKED_USERS).
    // In Firestore rules, there is no check for user-to-user blocking.
    // The test confirms that Firestore allows Bob to write even if Alice blocked him locally on her phone.
    await seedData(async (context) => {
      await context.firestore().collection("conversations").doc(CONV_ID).set({
        id: CONV_ID,
        participants: [ALICE_UID, BOB_UID],
      });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    await assertSucceeds(
      bobDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_from_blocked")
        .set({
          id: "msg_from_blocked",
          senderId: BOB_UID,
          text: "I am blocked in UI but Firestore accepts my write",
          timestamp: Date.now(),
        })
    );
  });
});

// =========================================================================
// SUITE D: SUPPORT CONVERSATIONS (/support_conversations/{convId}/...)
// =========================================================================
describe("Suite D: Support Conversations System", () => {
  beforeEach(async () => {
    // Seed admin doc
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "admin",
      });
    });
  });

  test("D01: [AUTHORIZED] User can create their own support conversation and send user messages", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
        lastMessage: "Help needed",
      })
    );

    await assertSucceeds(
      aliceDb
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("sup_msg_1")
        .set({
          id: "sup_msg_1",
          conversationId: ALICE_UID,
          senderId: ALICE_UID,
          senderRole: "user",
          text: "My video player is lagging",
        })
    );
  });

  test("D02: [SECURITY BOUND] User CANNOT spoof admin role in support messages (senderRole=admin)", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("sup_fake_admin")
        .set({
          id: "sup_fake_admin",
          conversationId: ALICE_UID,
          senderId: ALICE_UID,
          senderRole: "admin", // Forged role!
          text: "This is official admin speaking",
        })
    );
  });

  test("D03: [SECURITY BOUND] User CANNOT read other users' support conversations or messages", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
      });
      await context
        .firestore()
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("sup_alice_msg")
        .set({
          id: "sup_alice_msg",
          conversationId: ALICE_UID,
          senderId: ALICE_UID,
          senderRole: "user",
          text: "Confidential support ticket",
        });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    await assertFails(bobDb.collection("support_conversations").doc(ALICE_UID).get());
    await assertFails(
      bobDb
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("sup_alice_msg")
        .get()
    );
  });

  test("D04: [CONFIRMED VULNERABILITY] Support Ticket Status Tampering: Current rules ALLOW user to update ticket status", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "resolved", // Admin previously closed/resolved the ticket
        createdAt: 1000,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // Vulnerability in firestore.rules:272-277: status is not restricted on update!
    // In SupportViewModel.kt:110, convRef.set(..., SetOptions.merge()) resets status to "open" on every message!
    await assertSucceeds(
      aliceDb.collection("support_conversations").doc(ALICE_UID).update({
        status: "open",
      })
    );
  });

  test("D05: [SECURITY BOUND] Normal user CANNOT delete support conversation or messages", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
      });
      await context
        .firestore()
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("msg_del")
        .set({
          id: "msg_del",
          conversationId: ALICE_UID,
          senderId: ALICE_UID,
          senderRole: "user",
          text: "Delete me",
        });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(aliceDb.collection("support_conversations").doc(ALICE_UID).delete());
    await assertFails(
      aliceDb
        .collection("support_conversations")
        .doc(ALICE_UID)
        .collection("messages")
        .doc("msg_del")
        .delete()
    );
  });
});

// =========================================================================
// SUITE E: ADMIN PRIVILEGES & SYSTEM INTEGRITY (/admins, /config, /auditLogs)
// =========================================================================
describe("Suite E: Admin Privileges & System Integrity", () => {
  beforeEach(async () => {
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "superadmin",
      });
    });
  });

  test("E01: [SECURITY BOUND] Normal user CANNOT write to /admins collection", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("admins").doc(ALICE_UID).set({
        uid: ALICE_UID,
        enabled: true,
        role: "admin",
      })
    );
  });

  test("E02: [SECURITY BOUND] User with owner email (sulopros01@gmail.com) CANNOT perform admin actions if /admins doc missing", async () => {
    // In Android client UserSecurityManager.kt:41, OWNER_EMAIL is recognized locally in UI.
    // But Firestore rules strictly check exists(/databases/$(database)/documents/admins/$(request.auth.uid)).
    // This test proves that Firestore rules do NOT trust email alone!
    const ownerEmailDb = testEnv
      .authenticatedContext(OWNER_EMAIL_UID, { email: "sulopros01@gmail.com" })
      .firestore();

    await assertFails(
      ownerEmailDb.collection("config").doc("app").set({
        maintenanceEnabled: true,
      })
    );
  });

  test("E03: [AUTHORIZED] Real verified Admin can update system configuration (/config/app)", async () => {
    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(
      adminDb.collection("config").doc("app").set({
        maintenanceEnabled: false,
        latestVersionCode: 10,
      })
    );
  });

  test("E04: [SECURITY BOUND] Normal user CANNOT modify system configuration (/config/app)", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(
      aliceDb.collection("config").doc("app").set({
        maintenanceEnabled: true,
      })
    );
  });

  test("E05: [SECURITY BOUND] Normal user CANNOT read or write audit logs (/auditLogs)", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("auditLogs").doc("log_001").set({
        action: "BAN_USER",
        actorUid: ADMIN_UID,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(aliceDb.collection("auditLogs").doc("log_001").get());
    await assertFails(
      aliceDb.collection("auditLogs").doc("log_fake").set({
        action: "FAKE_LOG",
      })
    );
  });

  test("E06: [SECURITY BOUND] Deprecated config wildcards are completely forbidden (/config/{document=**})", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertFails(aliceDb.collection("config").doc("deprecated_nested_item").get());

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    // Rule in firestore.rules:51-53 explicitly forbids all access to unknown config paths even for admins:
    // `match /config/{document=**} { allow read, write: if false; }`
    await assertFails(adminDb.collection("config").doc("unexpected_unknown_key").get());
  });

  test("E07: [SECURITY BOUND] Deprecated /audit_logs path is completely forbidden", async () => {
    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    // Rule in firestore.rules:359-361 explicitly forbids deprecated path:
    // `match /audit_logs/{document=**} { allow read, write: if false; }`
    await assertFails(adminDb.collection("audit_logs").doc("legacy_log").get());
    await assertFails(
      adminDb.collection("audit_logs").doc("legacy_log").set({ test: true })
    );
  });
});
