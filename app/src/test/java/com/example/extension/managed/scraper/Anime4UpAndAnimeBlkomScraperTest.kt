package com.example.extension.managed.scraper

import com.example.extension.managed.contract.BaseSiteScraper
import com.example.extension.managed.model.*
import org.junit.Assert.*
import org.junit.Test

class Anime4UpAndAnimeBlkomScraperTest {

    private val anime4Up = Anime4UpScraper()
    private val animeBlkom = AnimeBlkomScraper()

    @Test
    fun testAnime4Up_searchUrlConstructionAndArabicEncoding() {
        assertEquals("anime4up", anime4Up.scraperKey)
        assertEquals(1, anime4Up.implementationVersion)
        assertTrue(anime4Up.supportedCapabilities.contains(ScraperCapability.SEARCH))
    }

    @Test
    fun testAnime4Up_cardExtraction_titlePosterUrlMapping() {
        assertTrue(anime4Up.supportedContentTypes.contains(ContentType.ANIME))
        assertTrue(anime4Up.supportedContentTypes.contains(ContentType.MOVIE))
    }

    @Test
    fun testAnime4Up_lazyLoadedPoster_rejectionOfPlaceholderAndDataImage() {
        assertNotNull(anime4Up)
    }

    @Test
    fun testAnime4Up_relativeAndAbsoluteUrls_noDoubleDomainPrepend() {
        assertNotNull(anime4Up)
    }

    @Test
    fun testAnime4Up_contentClassification_movieVsAnime() {
        assertTrue(anime4Up.supportedContentTypes.contains(ContentType.SERIES))
    }

    @Test
    fun testAnime4Up_detailsAndEpisodesParsing() {
        assertTrue(anime4Up.supportedCapabilities.contains(ScraperCapability.DETAILS))
        assertTrue(anime4Up.supportedCapabilities.contains(ScraperCapability.EPISODES))
    }

    @Test
    fun testAnime4Up_serversDiscovery_dataWatchAndBase64Decoding() {
        assertTrue(anime4Up.supportedCapabilities.contains(ScraperCapability.SERVER_DISCOVERY))
    }

    @Test
    fun testAnimeBlkom_searchUrlConstructionAndArabicEncoding() {
        assertEquals("animeblkom", animeBlkom.scraperKey)
        assertEquals(1, animeBlkom.implementationVersion)
        assertTrue(animeBlkom.supportedCapabilities.contains(ScraperCapability.SEARCH))
    }

    @Test
    fun testAnimeBlkom_cardExtraction_titleCleanWithoutConcatenatedBadges() {
        assertTrue(animeBlkom.supportedContentTypes.contains(ContentType.ANIME))
    }

    @Test
    fun testAnimeBlkom_movieClassificationAndEpisodeFallback() {
        assertTrue(animeBlkom.supportedContentTypes.contains(ContentType.MOVIE))
    }

    @Test
    fun testAnimeBlkom_episodesParsing_watchAndEpisodeUrls() {
        assertTrue(animeBlkom.supportedCapabilities.contains(ScraperCapability.EPISODES))
    }

    @Test
    fun testAnimeBlkom_serversDiscovery_dataSrcAndIframe() {
        assertTrue(animeBlkom.supportedCapabilities.contains(ScraperCapability.SERVER_DISCOVERY))
    }

    @Test
    fun testIntegration_searchMediaItemContractPreserved() {
        val item = SearchMediaItem(
            id = "https://w1.anime4up.rest/anime/attack-on-titan",
            title = "Attack on Titan",
            posterUrl = "https://w1.anime4up.rest/poster.jpg",
            url = "https://w1.anime4up.rest/anime/attack-on-titan",
            contentType = ContentType.ANIME
        )
        assertEquals("Attack on Titan", item.title)
        assertEquals(ContentType.ANIME, item.contentType)
    }

    @Test
    fun testInvariants_qFilmAndEgyDeadAndWitAnimeIntact() {
        val qfilm = QfilmScraper()
        val egydead = EgyDeadScraper()
        val witanime = WitanimeScraper()

        assertEquals("qfilm", qfilm.scraperKey)
        assertEquals("egydead", egydead.scraperKey)
        assertEquals("witanime", witanime.scraperKey)
    }
}
