/**
 * PHASE 20.1-B2 — FIRESTORE SECURITY HARDENING TEST SUITE
 * 
 * Target: /firestore.rules
 * Test runner: Node.js built-in test runner (node:test)
 * Framework: @firebase/rules-unit-testing
 * Execution environment: Local Firebase Firestore Emulator (127.0.0.1:8085)
 * 
 * Strict safety rules:
 * - Read-only against production (targets local demo project "demo-cinestream-test")
 * - Zero production mutations, zero deployments
 * - Validates HARDENED security rules (fixing A05, A09, B01, B02, C07, D04)
 * - Verifies all legitimate user & admin operations continue to succeed
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

  test("A05: [HARDENED PRIVACY] Cross-user sensitive data read: Authenticated user CANNOT read other user's private doc", async () => {
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
    // In hardened firestore.rules, `allow read: if isOwner(userId) || isAdmin();` protects private doc
    await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
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

  test("A09: [HARDENED SUBSCRIPTION] User CANNOT self-grant subscription directly", async () => {
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
    // In hardened firestore.rules, subscription fields are included in affectedKeys blocklist
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        isPremium: true,
        subscriptionTier: "PRO",
        subscriptionStatus: "ACTIVE",
        subscriptionExpiresAt: 4102444800000,
      })
    );
  });

  // NEW COVERAGE TESTS FOR SUITE A
  test("A10: [AUTHORIZED] User CAN read their own user document", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        email: "alice@test.com",
        displayName: "Alice",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  });

  test("A11: [AUTHORIZED] Admin CAN read any user's document", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "admin",
      });
      await context.firestore().collection("users").doc(BOB_UID).set({
        uid: BOB_UID,
        email: "bob_private@test.com",
        pointsBalance: 50000,
      });
    });

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(adminDb.collection("users").doc(BOB_UID).get());
  });

  test("A12: [AUTHORIZED] User CAN update legitimate non-sensitive profile fields", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        displayName: "Alice Initial",
        username: "alice123",
        bio: "Old bio",
        photoUrl: "https://res.cloudinary.com/old.jpg",
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("users").doc(ALICE_UID).update({
        displayName: "Alice Updated",
        username: "alice_new",
        bio: "Movie enthusiast & developer",
        photoUrl: "https://res.cloudinary.com/new.jpg",
      })
    );
  });

  test("A13: [AUTHORIZED] Admin CAN update subscription and limits for a user", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "admin",
      });
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        isPremium: false,
        subscriptionTier: "free",
      });
    });

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(
      adminDb.collection("users").doc(ALICE_UID).update({
        isPremium: true,
        subscriptionTier: "VIP",
        subscriptionStatus: "ACTIVE",
        subscriptionExpiresAt: 1999999999000,
      })
    );
  });
});

// =========================================================================
// SUITE B: POINTS & ECONOMY (/users/{userId}/..., /pro_requests, /reward_tasks)
// =========================================================================
describe("Suite B: Points & Economy System", () => {
  test("B01: [HARDENED ECONOMY] User CANNOT directly update pointsBalance or earned/spent", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        pointsBalance: 10,
        totalPointsEarned: 10,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // In hardened firestore.rules, points fields are in the affectedKeys blocklist
    await assertFails(
      aliceDb.collection("users").doc(ALICE_UID).update({
        pointsBalance: 1000000,
        totalPointsEarned: 1000000,
      })
    );
  });

  test("B02: [HARDENED LEDGER] User CANNOT insert unverified transaction into point_transactions", async () => {
    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // In hardened firestore.rules, write to point_transactions is strictly admin only
    await assertFails(
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

  test("B04: [SECURITY BOUND] Task Claim deletion: User CANNOT delete task_claims document", async () => {
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
    // Explicitly denied in hardened rules: allow delete: if isAdmin();
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

  // NEW COVERAGE TESTS FOR SUITE B
  test("B09: [AUTHORIZED] Admin CAN update pointsBalance on user profile", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "admin",
      });
      await context.firestore().collection("users").doc(ALICE_UID).set({
        uid: ALICE_UID,
        pointsBalance: 100,
      });
    });

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(
      adminDb.collection("users").doc(ALICE_UID).update({
        pointsBalance: 250,
      })
    );
  });

  test("B10: [AUTHORIZED] Admin CAN write to point_transactions ledger", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        enabled: true,
        role: "admin",
      });
    });

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(
      adminDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("point_transactions")
        .doc("tx_admin_001")
        .set({
          id: "tx_admin_001",
          userId: ALICE_UID,
          type: "BONUS_REWARD",
          amount: 500,
          balanceAfter: 600,
          createdAt: Date.now(),
        })
    );
  });

  test("B11: [SECURITY BOUND] User CANNOT update an existing task claim", async () => {
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
    await assertFails(
      aliceDb
        .collection("users")
        .doc(ALICE_UID)
        .collection("task_claims")
        .doc("task_daily_01")
        .update({
          claimedAt: 0,
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

  test("C07: [HARDENED FIX] Recipient CAN add reaction on sender's message", async () => {
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
          timestamp: 1000,
          reactions: {},
          deletedFor: [],
        });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    // In hardened firestore.rules, recipient participant CAN update reactions field
    await assertSucceeds(
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

  test("C08: [ARCHITECTURAL GAP] User-to-User blocking: Blocked user writes to Firestore (documented gap)", async () => {
    // In Android client, User A blocks User B in local DataStore (UserPreferencesRepository.BLOCKED_USERS).
    // In Firestore rules, there is no cross-user blocking document without architecture change.
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

  // NEW COVERAGE TESTS FOR SUITE C
  test("C09: [AUTHORIZED] Recipient CAN delete message for themselves (deletedFor)", async () => {
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
        .doc("msg_alice_todel")
        .set({
          id: "msg_alice_todel",
          senderId: ALICE_UID,
          text: "Delete me for Bob",
          timestamp: 1000,
          reactions: {},
          deletedFor: [],
        });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    await assertSucceeds(
      bobDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_todel")
        .update({
          deletedFor: [BOB_UID],
        })
    );
  });

  test("C10: [SECURITY BOUND] Recipient CANNOT modify message text, timestamp, or senderId", async () => {
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
        .doc("msg_alice_safe")
        .set({
          id: "msg_alice_safe",
          senderId: ALICE_UID,
          text: "Original immutable text",
          timestamp: 1000,
          reactions: {},
          deletedFor: [],
        });
    });

    const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
    // Tampering with text
    await assertFails(
      bobDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_safe")
        .update({
          text: "Bob hacked this text",
        })
    );

    // Tampering with senderId
    await assertFails(
      bobDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_safe")
        .update({
          senderId: BOB_UID,
        })
    );
  });

  test("C11: [AUTHORIZED] Original sender CAN edit message text and isEdited flag", async () => {
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
        .doc("msg_alice_edit")
        .set({
          id: "msg_alice_edit",
          senderId: ALICE_UID,
          text: "Original before edit",
          timestamp: 1000,
          isEdited: false,
          reactions: {},
          deletedFor: [],
        });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb
        .collection("conversations")
        .doc(CONV_ID)
        .collection("messages")
        .doc("msg_alice_edit")
        .update({
          text: "Edited by Alice",
          isEdited: true,
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
          senderRole: "admin",
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

  test("D04: [HARDENED STATUS] User CANNOT tamper with ticket status (e.g. from resolved to open)", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "resolved", // Admin previously closed/resolved the ticket
        createdAt: 1000,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    // In hardened firestore.rules, user cannot change status:
    // (!('status' in request.resource.data) || request.resource.data.status == resource.data.status)
    await assertFails(
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

  // NEW COVERAGE TESTS FOR SUITE D
  test("D06: [AUTHORIZED] User CAN update conversation metadata (lastMessage) when status remains open", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
        lastMessage: "Old message",
        lastMessageTime: 1000,
        createdAt: 1000,
      });
    });

    const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
    await assertSucceeds(
      aliceDb.collection("support_conversations").doc(ALICE_UID).update({
        lastMessage: "New message from user",
        lastMessageTime: 2000,
        status: "open", // Status unchanged
      })
    );
  });

  test("D07: [AUTHORIZED] Admin CAN update ticket status to resolved or in_progress", async () => {
    await seedData(async (context) => {
      await context.firestore().collection("support_conversations").doc(ALICE_UID).set({
        conversationId: ALICE_UID,
        userId: ALICE_UID,
        status: "open",
        createdAt: 1000,
      });
    });

    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertSucceeds(
      adminDb.collection("support_conversations").doc(ALICE_UID).update({
        status: "resolved",
      })
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
    await assertFails(adminDb.collection("config").doc("unexpected_unknown_key").get());
  });

  test("E07: [SECURITY BOUND] Deprecated /audit_logs path is completely forbidden", async () => {
    const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
    await assertFails(adminDb.collection("audit_logs").doc("legacy_log").get());
    await assertFails(
      adminDb.collection("audit_logs").doc("legacy_log").set({ test: true })
    );
  });
});
