package com.example.domain.search

import com.example.domain.models.*
import com.example.extension.managed.contract.BaseSiteScraper
import com.example.extension.managed.scraper.QfilmScraper
import org.junit.Assert.*
import org.junit.Test

class TmdbMatchingAndDisplayPolicyTest {

    private fun createMovie(
        id: String = "8587",
        title: String = "The Lion King",
        originalTitle: String = "The Lion King",
        year: Int = 2019,
        posterUrl: String = "https://image.tmdb.org/t/p/w500/lion_king.jpg",
        candidates: List<ProviderSourceCandidate> = emptyList()
    ): Movie {
        return Movie(
            id = id,
            title = title,
            originalTitle = originalTitle,
            overview = "Lion prince Simba flees his kingdom.",
            posterUrl = posterUrl,
            backdropUrl = "https://image.tmdb.org/t/p/w500/backdrop.jpg",
            year = year,
            rating = 7.1,
            genres = listOf("Animation", "Family"),
            runtime = 118,
            sourceCandidates = candidates
        )
    }

    @Test
    fun test01_canonicalIdentityPreservedWhenSourceTitleDiffers() {
        val candidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "فيلم الأسد الملك مدبلج للعربية",
            posterUrl = "https://egydead.link/p.jpg",
            sourcePageUrl = "https://egydead.link/m/123",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val movie = createMovie(candidates = listOf(candidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)

        assertEquals("8587", resolved.canonicalTmdbId)
        assertEquals("The Lion King", resolved.canonicalTitle)
        assertEquals("فيلم الأسد الملك مدبلج للعربية", resolved.providerTitle)
    }

    @Test
    fun test02_confirmedDubbedCandidateProvidesDisplayTitleAndPoster() {
        val candidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "الأسد الملك مدبلج",
            posterUrl = "https://egydead.link/poster_dub.jpg",
            sourcePageUrl = "https://egydead.link/m/123",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val movie = createMovie(candidates = listOf(candidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)

        assertTrue(resolved.isConfirmedDubbed)
        assertEquals("الأسد الملك مدبلج", resolved.displayTitle)
        assertEquals("https://egydead.link/poster_dub.jpg", resolved.displayPosterUrl)
    }

    @Test
    fun test03_missingSourcePosterFallsBackToCanonicalPosterForDisplayOnly() {
        val candidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "الأسد الملك مدبلج",
            posterUrl = null, // Missing source poster
            sourcePageUrl = "https://egydead.link/m/123",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val movie = createMovie(candidates = listOf(candidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)

        assertNull(resolved.providerPosterUrl)
        assertEquals("https://image.tmdb.org/t/p/w500/lion_king.jpg", resolved.displayPosterUrl)
    }

    @Test
    fun test04_missingSourceTitleFallsBackToCanonicalTitleForDisplayOnly() {
        val candidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "", // Missing source title
            posterUrl = "https://egydead.link/poster.jpg",
            sourcePageUrl = "https://egydead.link/m/123",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val movie = createMovie(candidates = listOf(candidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)

        assertEquals("The Lion King", resolved.displayTitle)
        assertNull(resolved.providerTitle)
    }

    @Test
    fun test05_unverifiedDubbedCandidateDoesNotOverrideDisplayTitleOrPoster() {
        val unverifiedCandidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "الأسد الملك مدبلج",
            posterUrl = "https://egydead.link/poster.jpg",
            sourcePageUrl = "", // Blank source page URL = NOT verified
            status = CoverageStatus.NOT_FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val movie = createMovie(candidates = listOf(unverifiedCandidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = true)

        assertFalse(resolved.isConfirmedDubbed)
        assertEquals("The Lion King", resolved.displayTitle)
        assertEquals("https://image.tmdb.org/t/p/w500/lion_king.jpg", resolved.displayPosterUrl)
    }

    @Test
    fun test06_subtitledCandidateNeverOverridesDisplayTitle() {
        val subCandidate = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "فيلم الأسد الملك مترجم",
            posterUrl = "https://egydead.link/sub_poster.jpg",
            sourcePageUrl = "https://egydead.link/m/123",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.SUBTITLED
        )
        val movie = createMovie(candidates = listOf(subCandidate))
        val resolved = MediaDisplayTitleResolver.resolveForMovie(movie, isDubbedIntent = false)

        assertFalse(resolved.isConfirmedDubbed)
        assertEquals("The Lion King", resolved.displayTitle)
    }

    @Test
    fun test07_preferredProviderHonoredInTieBreaking() {
        val c1 = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "EgyDead Title",
            posterUrl = "https://egydead/p.jpg",
            sourcePageUrl = "https://egydead/1",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val c2 = ProviderSourceCandidate(
            providerId = "witanime",
            scraperKey = "witanime",
            title = "WitAnime Title",
            posterUrl = "https://wit/p.jpg",
            sourcePageUrl = "https://wit/1",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )

        val best = MediaDisplayTitleResolver.selectBestDubbedCandidate(
            candidates = listOf(c1, c2),
            dubbedOrder = emptyList(),
            preferredProviderId = "witanime"
        )
        assertEquals("witanime", best?.providerId)
    }

    @Test
    fun test08_authoritativeDubbedOrderHonored() {
        val c1 = ProviderSourceCandidate(
            providerId = "animeblkom",
            scraperKey = "animeblkom",
            title = "Blkom Title",
            posterUrl = "https://b/p.jpg",
            sourcePageUrl = "https://b/1",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )
        val c2 = ProviderSourceCandidate(
            providerId = "egydead",
            scraperKey = "egydead",
            title = "EgyDead Title",
            posterUrl = "https://e/p.jpg",
            sourcePageUrl = "https://e/1",
            status = CoverageStatus.FOUND,
            mode = MediaVariantMode.DUBBED
        )

        val order = listOf("egydead", "animeblkom")
        val best = MediaDisplayTitleResolver.selectBestDubbedCandidate(
            candidates = listOf(c1, c2),
            dubbedOrder = order,
            preferredProviderId = null
        )
        assertEquals("egydead", best?.providerId)
    }

    @Test
    fun test09_strictMatchingRejectsFalsePositiveSubstrings() {
        val movie = createMovie(id = "1", title = "Batman", originalTitle = "Batman", year = 1989)
        // "Batman Begins" should NOT match a distinct movie simply named "Batman"
        val matches = MediaTitleMatcher.matchesMovie("Batman Begins", movie)
        assertFalse(matches)
    }

    @Test
    fun test10_yearToleranceEnforcedDuringMatching() {
        val movie = createMovie(id = "2", title = "Avatar", year = 2009)

        // Same year: match
        assertTrue(MediaTitleMatcher.matchesMovie("Avatar 2009", movie))
        // 1 year difference: allowed tolerance
        assertTrue(MediaTitleMatcher.matchesMovie("Avatar 2010", movie))
        // 3 years difference: rejected
        assertFalse(MediaTitleMatcher.matchesMovie("Avatar 2022", movie))
    }

    @Test
    fun test11_sequenceNumbersMustMatch() {
        val movie1 = createMovie(id = "1", title = "Frozen", year = 2013)
        val movie2 = createMovie(id = "2", title = "Frozen 2", year = 2019)

        assertFalse(MediaTitleMatcher.matchesMovie("Frozen 2", movie1))
        assertTrue(MediaTitleMatcher.matchesMovie("Frozen 2", movie2))
    }

    @Test
    fun test12_crossLanguageMatchingWithTransliteration() {
        val movie = createMovie(id = "10", title = "Inception", originalTitle = "Inception", year = 2010)
        assertTrue(MediaTitleMatcher.matchesMovie("إنسبشن 2010", movie))
    }

    @Test
    fun test13_seriesAndAnimeMatchingContract() {
        val series = Series(
            id = "77",
            title = "Attack on Titan",
            originalTitle = "Shingeki no Kyojin",
            overview = "Titans attack humanity.",
            posterUrl = "https://img/aot.jpg",
            backdropUrl = "",
            year = 2013,
            rating = 9.0,
            genres = listOf("Animation", "Action")
        )

        assertTrue(MediaTitleMatcher.matchesSeries("Attack on Titan", series))
        assertTrue(MediaTitleMatcher.matchesSeries("Shingeki no Kyojin", series))
        assertFalse(MediaTitleMatcher.matchesSeries("Naruto", series))
    }

    @Test
    fun test14_qFilmScraperRemainsStrictlyUnchanged() {
        val qfilm = QfilmScraper()
        assertTrue(qfilm is BaseSiteScraper)
        assertEquals("qfilm", qfilm.scraperKey)
        assertEquals(1, qfilm.implementationVersion)
        assertTrue(qfilm.supportedContentTypes.isNotEmpty())
        assertTrue(qfilm.supportedCapabilities.isNotEmpty())
    }
}
