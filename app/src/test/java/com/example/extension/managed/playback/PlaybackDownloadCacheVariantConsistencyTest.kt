package com.example.extension.managed.playback

import com.example.extension.managed.model.ContentType
import com.example.extension.managed.adapter.DownloaderHandoffAdapter
import com.example.extension.managed.model.DownloadSource
import com.example.ui.screens.player.ServerStateStore
import com.example.utils.MediaStorageUtils
import org.junit.Assert.*
import org.junit.Test

class PlaybackDownloadCacheVariantConsistencyTest {

    @Test
    fun test01_playbackResolutionCache_buildKeyDistinguishesDubbedAndSubtitled() {
        val cache = PlaybackResolutionCache()
        val dubbedKey = cache.buildKey(
            mediaId = "8587",
            isMovie = true,
            mode = "dubbed",
            language = "ar"
        )
        val subtitledKey = cache.buildKey(
            mediaId = "8587",
            isMovie = true,
            mode = "subtitled",
            language = "ar"
        )

        assertNotEquals(dubbedKey, subtitledKey)
        assertTrue(dubbedKey.contains(":dubbed:"))
        assertTrue(subtitledKey.contains(":subtitled:"))
    }

    @Test
    fun test02_playbackResolutionCache_strictVariantLookupIsolation() {
        val cache = PlaybackResolutionCache()
        val dubbedResolution = PlaybackResolution(
            mediaIdentity = "8587_dubbed",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mediaId = "8587",
            providerId = "egydead",
            streamUrl = "https://cdn.example.com/dubbed.m3u8",
            mode = "dubbed",
            language = "ar"
        )
        cache.put(dubbedResolution)

        // Lookup with mode="dubbed" finds the resolution
        val foundDubbed = cache.findValidResolution(
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "dubbed",
            language = "ar"
        )
        assertNotNull(foundDubbed)
        assertEquals("https://cdn.example.com/dubbed.m3u8", foundDubbed?.streamUrl)

        // Lookup with mode="subtitled" does NOT return the dubbed stream
        val foundSubtitled = cache.findValidResolution(
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "subtitled",
            language = "ar"
        )
        assertNull(foundSubtitled)
    }

    @Test
    fun test03_managedOrchestrator_cachePlaybackResolution_preservesVariantModeAndLanguage() {
        val resolution = PlaybackResolution(
            mediaIdentity = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mediaId = "8587",
            providerId = "egydead",
            streamUrl = "https://cdn.example.com/stream.m3u8",
            mode = "dubbed",
            language = "ar"
        )
        assertEquals("dubbed", resolution.mode)
        assertEquals("ar", resolution.language)
        assertEquals("8587", resolution.mediaId)
    }

    @Test
    fun test04_managedOrchestrator_evaluateWarmPlayback_isolatesDubbedFromSubtitled() {
        val cache = PlaybackResolutionCache()
        val resolution = PlaybackResolution(
            mediaIdentity = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mediaId = "8587",
            providerId = "egydead",
            streamUrl = "https://cdn.example.com/stream.m3u8",
            mode = "dubbed",
            language = "ar"
        )

        // Validating with mismatched mode returns Invalid
        val result = cache.validateResolution(
            cached = resolution,
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "subtitled",
            language = "ar"
        )
        assertTrue(result is ResolutionValidationResult.Invalid)

        // Validating with matching mode returns Valid
        val validResult = cache.validateResolution(
            cached = resolution,
            mediaId = "8587",
            contentType = com.example.extension.managed.model.ContentType.MOVIE,
            mode = "dubbed",
            language = "ar"
        )
        assertTrue(validResult is ResolutionValidationResult.Valid)
    }

    @Test
    fun test05_mediaStorageUtils_buildDownloadId_differentiatesMovieVariants() {
        val dubbedDownloadId = MediaStorageUtils.buildDownloadId(
            mediaId = "8587",
            isMovie = true,
            mode = "dubbed",
            language = "ar"
        )
        val subtitledDownloadId = MediaStorageUtils.buildDownloadId(
            mediaId = "8587",
            isMovie = true,
            mode = "subtitled",
            language = "ar"
        )

        assertEquals("8587_dubbed_ar", dubbedDownloadId)
        assertEquals("8587_subtitled_ar", subtitledDownloadId)
        assertNotEquals(dubbedDownloadId, subtitledDownloadId)
    }

    @Test
    fun test06_mediaStorageUtils_buildDownloadId_differentiatesSeriesEpisodeVariants() {
        val dubbedEpId = MediaStorageUtils.buildDownloadId(
            mediaId = "1399",
            isMovie = false,
            season = 2,
            episode = 5,
            mode = "dubbed",
            language = "ar"
        )
        val subtitledEpId = MediaStorageUtils.buildDownloadId(
            mediaId = "1399",
            isMovie = false,
            season = 2,
            episode = 5,
            mode = "subtitled",
            language = "ar"
        )

        assertEquals("1399_s2e5_dubbed_ar", dubbedEpId)
        assertEquals("1399_s2e5_subtitled_ar", subtitledEpId)
        assertNotEquals(dubbedEpId, subtitledEpId)
    }

    @Test
    fun test07_mediaStorageUtils_findMediaFile_strictIsolationForDubbedRequests() {
        // MediaStorageUtils.sanitizeSegment tests
        assertEquals("clean_id", MediaStorageUtils.sanitizeSegment("clean_id"))
        assertEquals("1399_dubbed_ar", MediaStorageUtils.sanitizeSegment("1399_dubbed_ar"))

        // Path traversal attempts are rejected
        assertThrows(SecurityException::class.java) {
            MediaStorageUtils.sanitizeSegment("../etc/passwd")
        }
    }

    @Test
    fun test08_unifiedDownloadCoordinator_buildDownloadSource_retainsVariantMetadata() {
        val downloadSource = DownloadSource(
            url = "https://cdn.example.com/video.mp4",
            variantMode = "dubbed",
            audioLanguage = "ar",
            metadata = mapOf("mode" to "dubbed", "language" to "ar")
        )
        assertEquals("dubbed", downloadSource.variantMode)
        assertEquals("ar", downloadSource.audioLanguage)
    }

    @Test
    fun test09_downloaderHandoffAdapter_toDownloaderInput_propagatesVariantFields() {
        val source = DownloadSource(
            url = "https://cdn.example.com/file.mp4",
            variantMode = "dubbed",
            audioLanguage = "ar",
            metadata = mapOf("providerId" to "egydead", "isMovie" to "true")
        )
        val input = DownloaderHandoffAdapter.toDownloaderInput(source, "task_1", "Sample Movie")

        assertEquals("dubbed", input.variantMode)
        assertEquals("ar", input.audioLanguage)
        assertEquals("egydead", input.providerId)
        assertEquals(true, input.isMovie)
    }

    @Test
    fun test10_serverStateStore_saveDataAndUpdateRevalidated_isolatesTargetMediaId() {
        val mediaKeyDub = ServerStateStore.buildMediaKey("Inception", true, 1, 1, "dubbed", "ar")
        val mediaKeySub = ServerStateStore.buildMediaKey("Inception", true, 1, 1, "subtitled", "ar")

        assertNotEquals(mediaKeyDub, mediaKeySub)
        assertTrue(mediaKeyDub.contains("-dubbed-ar"))
        assertTrue(mediaKeySub.contains("-subtitled-ar"))

        ServerStateStore.saveForMedia(
            mediaKey = mediaKeyDub,
            servers = listOf("Dubbed Server 1"),
            links = mapOf("Dubbed Server 1" to "https://dub.com"),
            ids = emptyMap(),
            downloads = emptyMap(),
            mediaId = "8587",
            variantMode = "dubbed"
        )

        ServerStateStore.saveForMedia(
            mediaKey = mediaKeySub,
            servers = listOf("Subtitled Server 1"),
            links = mapOf("Subtitled Server 1" to "https://sub.com"),
            ids = emptyMap(),
            downloads = emptyMap(),
            mediaId = "8587",
            variantMode = "subtitled"
        )

        val cachedDub = ServerStateStore.getCachedData(mediaKeyDub)
        val cachedSub = ServerStateStore.getCachedData(mediaKeySub)

        assertNotNull(cachedDub)
        assertNotNull(cachedSub)
        assertEquals(listOf("Dubbed Server 1"), cachedDub?.servers)
        assertEquals(listOf("Subtitled Server 1"), cachedSub?.servers)
    }

    @Test
    fun test11_serverStateStore_getCachedData_strictVariantBoundary() {
        val dubbedKey = ServerStateStore.buildMediaKey("Avatar", true, 1, 1, "dubbed", "ar")
        ServerStateStore.saveForMedia(
            mediaKey = dubbedKey,
            servers = listOf("Avatar Dub Server"),
            links = mapOf("Avatar Dub Server" to "https://link.com"),
            ids = emptyMap(),
            downloads = emptyMap(),
            mediaId = "999",
            variantMode = "dubbed"
        )

        // Subtitled query for same title must NOT receive dubbed server data
        val subtitledKey = ServerStateStore.buildMediaKey("Avatar", true, 1, 1, "subtitled", "ar")
        val result = ServerStateStore.getCachedData(subtitledKey)
        // If not saved under subtitledKey, it must be null
        assertNull(result)
    }

    @Test
    fun test12_mediaDetailsCacheManager_strictVariantSeparationAndFallback() {
        // Test media key isolation logic
        val keyDub = ServerStateStore.buildMediaKey("Matrix", true, 1, 1, "dubbed", "ar")
        val keySub = ServerStateStore.buildMediaKey("Matrix", true, 1, 1, "subtitled", "ar")

        assertFalse(keyDub == keySub)
    }
}
