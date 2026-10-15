package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ContentType
import com.example.data.model.LibraryIdentity
import com.example.data.model.LibraryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase 19.10 Targeted Crash Fix Regression Test Suite:
 * Validates Room DAO queries, legacy row mapping, schema migration, and identity invariants.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomLibraryIdentityCrashFixTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var libraryDao: LibraryDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        libraryDao = db.libraryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test01_standardRowLoadsSuccessfully() = runBlocking {
        val standardItem = LibraryItem.create(
            contentType = ContentType.MOVIE,
            tmdbId = "277834",
            title = "Rings (Subtitled)",
            posterUrl = "https://example.com/poster.jpg",
            variantMode = "subtitled"
        )
        libraryDao.insertItem(standardItem)

        val items = libraryDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("movie_277834", items[0].libraryId)
        assertEquals("277834", items[0].tmdbId)
        assertEquals("subtitled", items[0].variantMode)
        assertTrue(items[0].isMovie)
    }

    @Test
    fun test02_dubbedRowLoadsSuccessfully() = runBlocking {
        val dubbedItem = LibraryItem.create(
            contentType = ContentType.MOVIE,
            tmdbId = "277834",
            title = "فيلم رينجز مدبلج",
            posterUrl = "https://example.com/dub_poster.jpg",
            variantMode = "dubbed",
            providerId = "egydead"
        )
        libraryDao.insertItem(dubbedItem)

        val items = libraryDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("movie_277834_dubbed", items[0].libraryId)
        assertEquals("277834", items[0].tmdbId)
        assertEquals("dubbed", items[0].variantMode)
        assertTrue(items[0].isMovie)
    }

    @Test
    fun test03_legacyStandardRowNotReclassifiedAsDubbed() = runBlocking {
        // Direct SQL insert simulating legacy row with standard mode
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834', '277834', 'Original Movie', 'https://example.com/p.jpg', 'movie', 1, 'subtitled', '')
            """.trimIndent()
        )

        val items = libraryDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("movie_277834", items[0].libraryId)
        assertEquals("subtitled", items[0].variantMode)
        assertFalse(items[0].variantMode == "dubbed")
    }

    @Test
    fun test04_legacyDubbedRowRepairedWhenPersistedDataIdentifiesItAsDubbed() = runBlocking {
        // Direct SQL insert simulating the exact reported crash row:
        // libraryId = 'movie_277834' while variantMode = 'dubbed'
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834', '277834', 'فيلم رينجز مدبلج', 'https://example.com/p.jpg', 'movie', 1, 'dubbed', 'egydead')
            """.trimIndent()
        )

        // 1. Verify DAO query projects canonical ID on read without throwing Invariant violation
        val items = libraryDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("movie_277834_dubbed", items[0].libraryId)
        assertEquals("277834", items[0].tmdbId)
        assertEquals("dubbed", items[0].variantMode)

        // 2. Verify getItemById resolves with either legacy or canonical query
        val byCanonical = libraryDao.getItemById("movie_277834_dubbed")
        assertNotNull(byCanonical)
        assertEquals("movie_277834_dubbed", byCanonical?.libraryId)

        // 3. Verify transactional repair updates physical row
        AppDatabase.repairLegacyLibraryItems(db.openHelper.writableDatabase)
        val cursor = db.openHelper.readableDatabase.query("SELECT libraryId FROM library_items WHERE tmdbId = '277834'")
        assertTrue(cursor.moveToFirst())
        assertEquals("movie_277834_dubbed", cursor.getString(0))
        cursor.close()
    }

    @Test
    fun test05_ambiguousLegacyRecordFollowsDocumentedSafePolicy() = runBlocking {
        // Insert record with empty variantMode (legacy pre-v10 row)
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_12345', '12345', 'Old Ambiguous Movie', 'https://example.com/old.jpg', 'movie', 1, '', '')
            """.trimIndent()
        )

        val items = libraryDao.getAllItems().first()
        assertEquals(1, items.size)
        assertEquals("movie_12345", items[0].libraryId)
        assertEquals("subtitled", items[0].variantMode)
        assertNotEquals("dubbed", items[0].variantMode)
    }

    @Test
    fun test06_standardAndDubbedFavoritesRemainIndependent() = runBlocking {
        val sub = LibraryItem.create("movie", "277834", "Rings Original", "https://img/sub.jpg", "subtitled")
        val dub = LibraryItem.create("movie", "277834", "Rings Dubbed", "https://img/dub.jpg", "dubbed")

        libraryDao.insertItem(sub)
        libraryDao.insertItem(dub)

        val items = libraryDao.getAllItems().first()
        assertEquals(2, items.size)

        val subFound = libraryDao.getItemById("movie_277834")
        val dubFound = libraryDao.getItemById("movie_277834_dubbed")

        assertNotNull(subFound)
        assertNotNull(dubFound)
        assertEquals("subtitled", subFound?.variantMode)
        assertEquals("dubbed", dubFound?.variantMode)
    }

    @Test
    fun test07_addingOrDeletingOneVariantLeavesOtherUntouched() = runBlocking {
        val sub = LibraryItem.create("movie", "277834", "Rings Original", "https://img/sub.jpg", "subtitled")
        val dub = LibraryItem.create("movie", "277834", "Rings Dubbed", "https://img/dub.jpg", "dubbed")

        libraryDao.insertItem(sub)
        libraryDao.insertItem(dub)

        // Delete dubbed
        libraryDao.deleteByLibraryId("movie_277834_dubbed")

        val remaining = libraryDao.getAllItems().first()
        assertEquals(1, remaining.size)
        assertEquals("movie_277834", remaining[0].libraryId)
        assertEquals("subtitled", remaining[0].variantMode)
    }

    @Test
    fun test08_detailScreenLibraryObservationDoesNotThrowException() = runBlocking {
        // Seed database with legacy un-namespaced row that caused the reported crash
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834', '277834', 'Rings Dubbed', 'https://example.com/p.jpg', 'movie', 1, 'dubbed', 'egydead')
            """.trimIndent()
        )

        // Emulate DetailsScreens observation: libraryRepository.getLibraryItems().collectAsState()
        val observedList = try {
            libraryDao.getAllItems().first()
        } catch (e: Exception) {
            fail("Observation threw exception: ${e.message}")
            emptyList()
        }

        assertNotNull(observedList)
        assertEquals(1, observedList.size)
        assertEquals("movie_277834_dubbed", observedList[0].libraryId)
        assertEquals("dubbed", observedList[0].variantMode)
    }

    @Test
    fun test09_databaseMigrationAndRepairHandlesConflictsSafely() {
        // Pre-populate with both an un-namespaced legacy row and a canonical row for the same work
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834', '277834', 'Rings Legacy Dubbed', 'https://example.com/p1.jpg', 'movie', 1, 'dubbed', 'egydead')
            """.trimIndent()
        )
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834_dubbed', '277834', 'Rings Canonical Dubbed', 'https://example.com/p2.jpg', 'movie', 1, 'dubbed', 'egydead')
            """.trimIndent()
        )

        // Running repair must handle collision safely without UNIQUE constraint crash
        AppDatabase.repairLegacyLibraryItems(db.openHelper.writableDatabase)

        val cursor = db.openHelper.readableDatabase.query("SELECT libraryId, title FROM library_items WHERE tmdbId = '277834'")
        assertEquals(1, cursor.count)
        assertTrue(cursor.moveToFirst())
        assertEquals("movie_277834_dubbed", cursor.getString(0))
        cursor.close()
    }

    @Test
    fun test10_repairIsIdempotent() {
        db.openHelper.writableDatabase.execSQL(
            """
            INSERT INTO library_items (`libraryId`, `tmdbId`, `title`, `posterUrl`, `contentType`, `isMovie`, `variantMode`, `providerId`)
            VALUES ('movie_277834', '277834', 'Rings Dubbed', 'https://example.com/p.jpg', 'movie', 1, 'dubbed', 'egydead')
            """.trimIndent()
        )

        // Run repair 5 times repeatedly
        for (i in 1..5) {
            AppDatabase.repairLegacyLibraryItems(db.openHelper.writableDatabase)
        }

        val cursor = db.openHelper.readableDatabase.query("SELECT libraryId FROM library_items")
        assertEquals(1, cursor.count)
        assertTrue(cursor.moveToFirst())
        val finalId = cursor.getString(0)
        assertEquals("movie_277834_dubbed", finalId)
        assertFalse(finalId.contains("_dubbed_dubbed"))
        cursor.close()
    }
}
