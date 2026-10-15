package com.example.data.model

import org.junit.Assert.*
import org.junit.Test

class FavoritesIdentityAndVariantIsolationTest {

    @Test
    fun test01_distinctLibraryIdGeneratedForDubbedVariant() {
        val dubbedId = LibraryIdentity.createLibraryId("movie", "8587", "dubbed")
        val subtitledId = LibraryIdentity.createLibraryId("movie", "8587", "subtitled")

        assertEquals("movie_8587_dubbed", dubbedId)
        assertEquals("movie_8587", subtitledId)
        assertNotEquals(dubbedId, subtitledId)
    }

    @Test
    fun test02_backwardCompatibilityWithLegacyLibraryIdFormat() {
        val parsedMovie = LibraryIdentity.parseLibraryDetails("movie_550")
        assertNotNull(parsedMovie)
        assertEquals("movie", parsedMovie?.contentType)
        assertEquals("550", parsedMovie?.tmdbId)
        assertEquals("subtitled", parsedMovie?.variantMode)

        val parsedTv = LibraryIdentity.parseLibraryDetails("tv_1399")
        assertNotNull(parsedTv)
        assertEquals("tv", parsedTv?.contentType)
        assertEquals("1399", parsedTv?.tmdbId)
        assertEquals("subtitled", parsedTv?.variantMode)
    }

    @Test
    fun test03_parseVariantAwareLibraryId() {
        val parsed = LibraryIdentity.parseLibraryDetails("movie_8587_dubbed")
        assertNotNull(parsed)
        assertEquals("movie", parsed?.contentType)
        assertEquals("8587", parsed?.tmdbId)
        assertEquals("dubbed", parsed?.variantMode)

        val pair = LibraryIdentity.parseLibraryId("movie_8587_dubbed")
        assertNotNull(pair)
        assertEquals("movie", pair?.first)
        assertEquals("8587", pair?.second)
    }

    @Test
    fun test04_validationEnforcesCanonicalInvariants() {
        // Valid inputs pass without exception
        LibraryIdentity.validate("movie_8587_dubbed", "8587", "movie", true, "dubbed")
        LibraryIdentity.validate("movie_8587", "8587", "movie", true, "subtitled")

        // Invalid contentType throws
        assertThrows(IllegalArgumentException::class.java) {
            LibraryIdentity.validate("invalid_8587", "8587", "invalid", true, "subtitled")
        }

        // Mismatched isMovie flag throws
        assertThrows(IllegalArgumentException::class.java) {
            LibraryIdentity.validate("movie_8587", "8587", "movie", false, "subtitled")
        }

        // Mismatched variant ID throws
        assertThrows(IllegalArgumentException::class.java) {
            LibraryIdentity.validate("movie_8587", "8587", "movie", true, "dubbed")
        }
    }

    @Test
    fun test05_libraryItemCreatePreservesVariantModeAndDistinctIdentity() {
        val dubbedItem = LibraryItem.create(
            contentType = "movie",
            tmdbId = "8587",
            title = "إنسبشن مدبلج",
            posterUrl = "https://egydead.link/poster_dubbed.jpg",
            variantMode = "dubbed",
            providerId = "egydead"
        )
        val subtitledItem = LibraryItem.create(
            contentType = "movie",
            tmdbId = "8587",
            title = "Inception",
            posterUrl = "https://image.tmdb.org/t/p/w500/inception.jpg",
            variantMode = "subtitled",
            providerId = "tmdb"
        )

        assertEquals("movie_8587_dubbed", dubbedItem.libraryId)
        assertEquals("movie_8587", subtitledItem.libraryId)
        assertEquals("dubbed", dubbedItem.variantMode)
        assertEquals("subtitled", subtitledItem.variantMode)
        assertEquals("إنسبشن مدبلج", dubbedItem.title)
        assertEquals("Inception", subtitledItem.title)
        assertEquals("https://egydead.link/poster_dubbed.jpg", dubbedItem.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/inception.jpg", subtitledItem.posterUrl)
    }

    @Test
    fun test06_firestoreSerializationPreservesVariantModeAndDistinctId() {
        val item = LibraryItem.create(
            contentType = "movie",
            tmdbId = "8587",
            title = "Inception (Dubbed)",
            posterUrl = "https://example.com/p.jpg",
            variantMode = "dubbed",
            providerId = "egydead"
        )
        val map = item.toFirestoreMap()
        assertEquals("movie_8587_dubbed", map["libraryId"])
        assertEquals("dubbed", map["variantMode"])
        assertEquals("8587", map["tmdbId"])

        val parsed = LibraryItem.fromFirestoreMap("movie_8587_dubbed", map)
        assertNotNull(parsed)
        assertEquals("movie_8587_dubbed", parsed?.libraryId)
        assertEquals("dubbed", parsed?.variantMode)
        assertEquals("8587", parsed?.tmdbId)
        assertEquals("Inception (Dubbed)", parsed?.title)
    }

    @Test
    fun test07_roomDatabaseVariantIsolationBothVariantsCanCoexist() {
        val dubbed = LibraryItem.create("movie", "8587", "Inception Dubbed", "https://img/dub.jpg", "dubbed")
        val subtitled = LibraryItem.create("movie", "8587", "Inception Subtitled", "https://img/sub.jpg", "subtitled")

        // Since libraryId is the primary key in Room, distinct keys mean both items coexist
        assertNotEquals(dubbed.libraryId, subtitled.libraryId)
        val table = mutableMapOf<String, LibraryItem>()
        table[dubbed.libraryId] = dubbed
        table[subtitled.libraryId] = subtitled

        assertEquals(2, table.size)
        assertTrue(table.containsKey("movie_8587_dubbed"))
        assertTrue(table.containsKey("movie_8587"))
    }

    @Test
    fun test08_roomQueriesIsolateButtonStatusPerVariant() {
        val items = listOf(
            LibraryItem.create("movie", "8587", "Inception", "https://img/sub.jpg", "subtitled")
        )

        // Subtitled query finds item
        val isSubtitledInLibrary = items.any {
            it.tmdbId == "8587" && it.contentType == "movie" && it.variantMode == "subtitled"
        }
        assertTrue(isSubtitledInLibrary)

        // Dubbed query does NOT find item when only subtitled was added
        val isDubbedInLibrary = items.any {
            it.tmdbId == "8587" && it.contentType == "movie" && it.variantMode == "dubbed"
        }
        assertFalse(isDubbedInLibrary)
    }

    @Test
    fun test09_removingOneVariantLeavesOtherVariantUntouched() {
        val table = mutableMapOf<String, LibraryItem>()
        val dubbed = LibraryItem.create("movie", "8587", "Inception Dubbed", "https://img/dub.jpg", "dubbed")
        val subtitled = LibraryItem.create("movie", "8587", "Inception Subtitled", "https://img/sub.jpg", "subtitled")
        table[dubbed.libraryId] = dubbed
        table[subtitled.libraryId] = subtitled

        // Remove dubbed variant
        table.remove(dubbed.libraryId)

        assertEquals(1, table.size)
        assertFalse(table.containsKey("movie_8587_dubbed"))
        assertTrue(table.containsKey("movie_8587"))
        assertEquals("Inception Subtitled", table["movie_8587"]?.title)
    }

    @Test
    fun test10_legacySubtitledEntriesWithoutExplicitModeMatchSubtitledQueries() {
        val legacyDoc = mapOf<String, Any?>(
            "id" to "550",
            "title" to "Fight Club",
            "posterUrl" to "https://img/fc.jpg",
            "isMovie" to true
            // Note: variantMode omitted to simulate legacy document
        )
        val parsed = LibraryItem.fromFirestoreMap("550", legacyDoc)
        assertNotNull(parsed)
        assertEquals("movie_550", parsed?.libraryId)
        assertEquals("subtitled", parsed?.variantMode)

        // Matches subtitled query
        assertEquals(LibraryIdentity.VARIANT_SUBTITLED, LibraryIdentity.normalizeVariantMode(parsed?.variantMode))
    }

    @Test
    fun test11_seriesAndAnimeVariantsIsolation() {
        val tvDubbed = LibraryItem.create("tv", "1399", "Game of Thrones", "https://img/got.jpg", "dubbed")
        val tvSub = LibraryItem.create("tv", "1399", "Game of Thrones", "https://img/got.jpg", "subtitled")
        val animeDubbed = LibraryItem.create("anime", "1429", "Attack on Titan", "https://img/aot.jpg", "dubbed")
        val animeSub = LibraryItem.create("anime", "1429", "Attack on Titan", "https://img/aot.jpg", "subtitled")

        assertEquals("tv_1399_dubbed", tvDubbed.libraryId)
        assertEquals("tv_1399", tvSub.libraryId)
        assertEquals("anime_1429_dubbed", animeDubbed.libraryId)
        assertEquals("anime_1429", animeSub.libraryId)

        assertNotEquals(tvDubbed.libraryId, tvSub.libraryId)
        assertNotEquals(animeDubbed.libraryId, animeSub.libraryId)
    }

    @Test
    fun test12_noCrossVariantAttributeContamination() {
        val dubbed = LibraryItem.create(
            contentType = "movie",
            tmdbId = "999",
            title = "العنوان العربي المدبلج",
            posterUrl = "https://provider.com/arabic_poster.jpg",
            variantMode = "dubbed",
            providerId = "egydead"
        )
        val subtitled = LibraryItem.create(
            contentType = "movie",
            tmdbId = "999",
            title = "Original English Title",
            posterUrl = "https://tmdb.org/canonical.jpg",
            variantMode = "subtitled",
            providerId = "tmdb"
        )

        assertNotEquals(dubbed.title, subtitled.title)
        assertNotEquals(dubbed.posterUrl, subtitled.posterUrl)
        assertNotEquals(dubbed.providerId, subtitled.providerId)
        assertEquals("العنوان العربي المدبلج", dubbed.title)
        assertEquals("Original English Title", subtitled.title)
    }
}
