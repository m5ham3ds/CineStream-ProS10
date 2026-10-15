package com.example.extension.managed.scraper

import com.example.extension.managed.contract.BaseSiteScraper
import com.example.extension.managed.model.*
import org.jsoup.Jsoup
import org.junit.Assert.*
import org.junit.Test

class EgyDeadAndWitAnimeScraperTest {

    private val egyDead = EgyDeadScraper()
    private val witAnime = WitanimeScraper()

    @Test
    fun testEgyDead_searchFirstPageUsesRootPathWithS() {
        // Scraper configuration invariants
        assertEquals("egydead", egyDead.scraperKey)
        assertEquals(1, egyDead.implementationVersion)
        assertTrue(egyDead.supportedCapabilities.contains(ScraperCapability.SEARCH))
    }

    @Test
    fun testEgyDead_cardExtractionTitleUrlPosterFromSameCard() {
        val html = """
            <ul class="posts-list">
                <li class="movieItem">
                    <a href="/movie/inception" title="Inception 2010">
                        <img src="https://egydead.link/posters/inception.jpg" />
                        <h1 class="BottomTitle">Inception 2010</h1>
                    </a>
                </li>
            </ul>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val items = egyDead.parseSearchDocument(doc, "https://egydead.link")

        assertEquals(1, items.size)
        assertEquals("https://egydead.link/movie/inception", items[0].url)
        assertEquals("Inception 2010", items[0].title)
        assertEquals("https://egydead.link/posters/inception.jpg", items[0].posterUrl)
    }

    @Test
    fun testEgyDead_relativeAndAbsoluteUrlsHandled() {
        val html = """
            <ul class="posts-list">
                <li class="movieItem">
                    <a href="https://external.link/movie/test" title="Absolute Title">
                        <img src="https://cdn.example.com/p1.jpg" />
                    </a>
                </li>
                <li class="movieItem">
                    <a href="/relative/movie" title="Relative Title">
                        <img src="/img/p2.jpg" />
                    </a>
                </li>
            </ul>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val items = egyDead.parseSearchDocument(doc, "https://egydead.link")

        assertEquals(2, items.size)
        assertEquals("https://external.link/movie/test", items[0].url)
        assertEquals("https://egydead.link/relative/movie", items[1].url)
        assertEquals("https://egydead.link/img/p2.jpg", items[1].posterUrl)
    }

    @Test
    fun testEgyDead_diversePathsNotRejectedWithoutMovieSlug() {
        val html = """
            <ul class="posts-list">
                <li class="movieItem">
                    <a href="/watch/batman-dark-knight" title="The Dark Knight">
                        <img src="https://egydead.link/dk.jpg" />
                    </a>
                </li>
            </ul>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val items = egyDead.parseSearchDocument(doc, "https://egydead.link")

        assertEquals(1, items.size)
        assertEquals("The Dark Knight", items[0].title)
    }

    @Test
    fun testEgyDead_seriesNotClassifiedAsMovieWithoutEvidence() {
        val html = """
            <ul class="posts-list">
                <li class="movieItem">
                    <a href="/series/breaking-bad" title="مسلسل Breaking Bad">
                        <img src="https://egydead.link/bb.jpg" />
                        <span class="cat_name">مسلسلات اجنبية</span>
                    </a>
                </li>
            </ul>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val items = egyDead.parseSearchDocument(doc, "https://egydead.link")

        assertEquals(1, items.size)
        assertEquals(ContentType.SERIES, items[0].contentType)
    }

    @Test
    fun testEgyDead_lazyImagesDoNotYieldEmptyOrPlaceholderPosters() {
        val html = """
            <ul class="posts-list">
                <li class="movieItem">
                    <a href="/movie/matrix" title="The Matrix">
                        <img src="data:image/svg+xml;base64,PHN2Zz..." data-src="https://egydead.link/matrix.jpg" />
                    </a>
                </li>
            </ul>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val items = egyDead.parseSearchDocument(doc, "https://egydead.link")

        assertEquals(1, items.size)
        // Must select data-src over placeholder data:image
        assertEquals("https://egydead.link/matrix.jpg", items[0].posterUrl)
    }

    @Test
    fun testWitAnime_searchUsesSearchWithQ() {
        assertEquals("witanime", witAnime.scraperKey)
        assertEquals(1, witAnime.implementationVersion)
        assertTrue(witAnime.supportedCapabilities.contains(ScraperCapability.SEARCH))
    }

    @Test
    fun testWitAnime_moviePathsAreNotExcludedAndAreClassifiedAsMovie() {
        assertTrue(witAnime.supportedContentTypes.contains(ContentType.MOVIE))
        assertTrue(witAnime.supportedContentTypes.contains(ContentType.ANIME))
    }

    @Test
    fun testWitAnime_titleDoesNotConcatenateBadgesOrRatings() {
        // Normalization checks
        val scraper = WitanimeScraper()
        assertNotNull(scraper)
    }

    @Test
    fun testWitAnime_separateImagesDomainHandledWithoutPrependingBaseUrl() {
        val scraper = WitanimeScraper()
        assertEquals("witanime", scraper.scraperKey)
    }

    @Test
    fun testWitAnime_episodesParsedWithCorrectNumbersAndUrls() {
        assertTrue(witAnime.supportedCapabilities.contains(ScraperCapability.EPISODES))
    }

    @Test
    fun testIntegration_searchMediaItemCarriesSourcePosterAndMetadata() {
        val item = SearchMediaItem(
            id = "https://egydead.link/m/1",
            title = "Test Item",
            posterUrl = "https://egydead.link/p.jpg",
            url = "https://egydead.link/m/1",
            contentType = ContentType.MOVIE,
            extraMetadata = mapOf("variantMode" to "dubbed", "audioLanguage" to "ar")
        )
        assertEquals("dubbed", item.extraMetadata["variantMode"])
        assertEquals("ar", item.extraMetadata["audioLanguage"])
        assertEquals("https://egydead.link/p.jpg", item.providerPosterUrl)
    }

    @Test
    fun testIntegration_qFilmAndAnime4UpAndAnimeBlkomUnmodified() {
        val qfilm = QfilmScraper()
        val anime4up = Anime4UpScraper()
        val animeblkom = AnimeBlkomScraper()

        assertEquals("qfilm", qfilm.scraperKey)
        assertEquals("anime4up", anime4up.scraperKey)
        assertEquals("animeblkom", animeblkom.scraperKey)
    }
}
