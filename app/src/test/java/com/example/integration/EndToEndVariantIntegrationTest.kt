package com.example.integration

import com.example.data.model.LibraryIdentity
import com.example.data.model.LibraryItem
import com.example.domain.models.*
import com.example.domain.search.DubbedSearchRankingEngine
import com.example.domain.search.MediaDisplayTitleResolver
import com.example.domain.search.SearchTriggerParser
import com.example.extension.managed.adapter.DownloaderHandoffAdapter
import com.example.extension.managed.model.DownloadSource
import com.example.extension.managed.playback.PlaybackResolution
import com.example.extension.managed.playback.PlaybackResolutionCache
import com.example.extension.managed.playback.ResolutionValidationResult
import com.example.ui.screens.player.ServerStateStore
import com.example.utils.MediaStorageUtils
import org.junit.Assert.*
import org.junit.Test

class EndToEndVariantIntegrationTest {

    @Test
    fun scenario1_dubbedVariantEndToEndFlow() {
        // Step 1: Search query with dubbed trigger
        val rawQuery = "فيلم إنسبشن مدبلج"
        val triggerResult = SearchTriggerParser.parse(rawQuery)
        assertTrue(triggerResult.isArabicDubbedIntent)
        assertEquals("فيلم إنسبشن", triggerResult.cleanQuery)

        // Step 2: Provider candidate matching
        val candidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "فيلم إنسبشن مدبلج للعربية",
            posterUrl = "https://egydead.link/poster_dub.jpg",
            sourcePageUrl = "https://egydead.link/m/8587",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED,
            audioLanguage = VariantLanguage.ARABIC
        )
        val movie = Movie(
            id = "8587",
            title = "Inception",
            originalTitle = "Inception",
            overview = "A thief enters the dreams of others.",
            posterUrl = "https://tmdb.org/inception.jpg",
            backdropUrl = "",
            year = 2010,
            rating = 8.8,
            genres = listOf("Sci-Fi", "Action"),
            runtime = 148,
            sourceCandidates = listOf(candidate)
        )

        // Step 3: Display resolver provides Arabic dubbed title & provider poster
        val displayInfo = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)
        assertTrue(displayInfo.isConfirmedDubbed)
        assertEquals("فيلم إنسبشن مدبلج للعربية", displayInfo.displayTitle)
        assertEquals("https://egydead.link/poster_dub.jpg", displayInfo.displayPosterUrl)

        // Step 4: Add to library / favorites with isolated ID
        val libraryItem = LibraryItem.create(
            contentType = "movie",
            tmdbId = displayInfo.canonicalTmdbId,
            title = displayInfo.displayTitle,
            posterUrl = displayInfo.displayPosterUrl ?: "",
            variantMode = "dubbed",
            providerId = displayInfo.providerId ?: "egydead"
        )
        assertEquals("movie_8587_dubbed", libraryItem.libraryId)

        // Step 5: Playback resolution cache with mode & language
        val resolutionCache = PlaybackResolutionCache()
        val resolution = PlaybackResolution(
            mediaIdentity = libraryItem.libraryId,
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mediaId = "8587",
            providerId = "egydead",
            streamUrl = "https://cdn.example.com/inception_dubbed.m3u8",
            mode = "dubbed",
            language = "ar"
        )
        resolutionCache.put(resolution)

        val cached = resolutionCache.findValidResolution(
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "dubbed",
            language = "ar"
        )
        assertNotNull(cached)
        assertEquals("https://cdn.example.com/inception_dubbed.m3u8", cached?.streamUrl)

        // Step 6: Download item ID and storage path isolation
        val downloadId = MediaStorageUtils.buildDownloadId("8587", isMovie = true, mode = "dubbed", language = "ar")
        assertEquals("8587_dubbed_ar", downloadId)

        val downloadSource = DownloadSource(
            url = cached?.streamUrl ?: "",
            variantMode = "dubbed",
            audioLanguage = "ar"
        )
        val downloadInput = DownloaderHandoffAdapter.toDownloaderInput(downloadSource, downloadId, displayInfo.displayTitle)
        assertEquals("dubbed", downloadInput.variantMode)
        assertEquals("ar", downloadInput.audioLanguage)
    }

    @Test
    fun scenario2_subtitledVariantEndToEndFlow() {
        // Step 1: Search query without dubbed trigger
        val rawQuery = "Inception"
        val triggerResult = SearchTriggerParser.parse(rawQuery)
        assertFalse(triggerResult.isArabicDubbedIntent)

        // Step 2: TMDB movie
        val movie = Movie(
            id = "8587",
            title = "Inception",
            originalTitle = "Inception",
            overview = "A thief enters the dreams of others.",
            posterUrl = "https://tmdb.org/inception.jpg",
            backdropUrl = "",
            year = 2010,
            rating = 8.8,
            genres = listOf("Sci-Fi", "Action"),
            runtime = 148
        )

        // Step 3: Display resolution retains TMDB canonical details
        val displayInfo = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = false)
        assertFalse(displayInfo.isConfirmedDubbed)
        assertEquals("Inception", displayInfo.displayTitle)
        assertEquals("https://tmdb.org/inception.jpg", displayInfo.displayPosterUrl)

        // Step 4: Library ID has standard namespaced format
        val libraryItem = LibraryItem.create(
            contentType = "movie",
            tmdbId = "8587",
            title = displayInfo.displayTitle,
            posterUrl = displayInfo.displayPosterUrl ?: "",
            variantMode = "subtitled"
        )
        assertEquals("movie_8587", libraryItem.libraryId)

        // Step 5: Playback cache lookup requires subtitled mode
        val resolutionCache = PlaybackResolutionCache()
        val resolution = PlaybackResolution(
            mediaIdentity = libraryItem.libraryId,
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mediaId = "8587",
            providerId = "tmdb",
            streamUrl = "https://cdn.example.com/inception_sub.m3u8",
            mode = "subtitled",
            language = "ar"
        )
        resolutionCache.put(resolution)

        val found = resolutionCache.findValidResolution(
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "subtitled",
            language = "ar"
        )
        assertNotNull(found)
        assertEquals("https://cdn.example.com/inception_sub.m3u8", found?.streamUrl)

        // Step 6: Download ID
        val downloadId = MediaStorageUtils.buildDownloadId("8587", isMovie = true, mode = "subtitled", language = "ar")
        assertEquals("8587_subtitled_ar", downloadId)
    }

    @Test
    fun scenario3_coexistenceOfBothVariants() {
        val table = mutableMapOf<String, LibraryItem>()
        val dubbed = LibraryItem.create("movie", "8587", "إنسبشن مدبلج", "https://egydead.link/p.jpg", "dubbed")
        val subtitled = LibraryItem.create("movie", "8587", "Inception", "https://tmdb.org/p.jpg", "subtitled")

        // 1. Both coexist in library
        table[dubbed.libraryId] = dubbed
        table[subtitled.libraryId] = subtitled
        assertEquals(2, table.size)

        // 2. Deleting dubbed variant leaves subtitled completely untouched
        table.remove(dubbed.libraryId)
        assertEquals(1, table.size)
        assertTrue(table.containsKey("movie_8587"))

        // 3. ServerStateStore keys are completely isolated
        val keyDub = ServerStateStore.buildMediaKey("Inception", true, 1, 1, "dubbed", "ar")
        val keySub = ServerStateStore.buildMediaKey("Inception", true, 1, 1, "subtitled", "ar")
        assertNotEquals(keyDub, keySub)
    }

    @Test
    fun scenario4_backwardCompatibilityWithLegacyRecords() {
        // Legacy document map lacking variantMode
        val legacyDoc = mapOf<String, Any?>(
            "id" to "8587",
            "title" to "Inception",
            "posterUrl" to "https://tmdb.org/p.jpg",
            "isMovie" to true
        )
        val item = LibraryItem.fromFirestoreMap("8587", legacyDoc)
        assertNotNull(item)
        assertEquals("movie_8587", item?.libraryId)
        assertEquals("subtitled", item?.variantMode)

        // Backward-compatible file lookup method works with sanitized segments
        val safeSegment = MediaStorageUtils.sanitizeSegment("8587")
        assertEquals("8587", safeSegment)
    }

    @Test
    fun scenario5_seriesAndAnimeEndToEndVariantFlow() {
        val animeDubbed = LibraryItem.create("anime", "777", "Attack on Titan Dubbed", "https://img/dub.jpg", "dubbed")
        val animeSub = LibraryItem.create("anime", "777", "Attack on Titan Subtitled", "https://img/sub.jpg", "subtitled")

        assertEquals("anime_777_dubbed", animeDubbed.libraryId)
        assertEquals("anime_777", animeSub.libraryId)
        assertNotEquals(animeDubbed.libraryId, animeSub.libraryId)

        val dubbedEpDownloadId = MediaStorageUtils.buildDownloadId("777", isMovie = false, season = 2, episode = 5, mode = "dubbed", language = "ar")
        val subEpDownloadId = MediaStorageUtils.buildDownloadId("777", isMovie = false, season = 2, episode = 5, mode = "subtitled", language = "ar")

        assertEquals("777_s2e5_dubbed_ar", dubbedEpDownloadId)
        assertEquals("777_s2e5_subtitled_ar", subEpDownloadId)
        assertNotEquals(dubbedEpDownloadId, subEpDownloadId)
    }
}
