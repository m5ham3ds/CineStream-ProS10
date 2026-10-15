package com.example.domain.search

import com.example.domain.models.CoverageStatus
import com.example.domain.models.MediaVariantMode
import com.example.domain.models.Movie
import com.example.domain.models.ProviderSourceCandidate
import com.example.domain.models.Series
import org.junit.Assert.*
import org.junit.Test

class IntelligentDubbedSearchEngineTest {

    // --- SearchTriggerParser Tests (01 to 12) ---

    @Test
    fun test01_detectsCanonicalTriggerWordMudablaj() {
        val res = SearchTriggerParser.parse("الأسد الملك مدبلج")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("الأسد الملك", res.cleanQuery)
    }

    @Test
    fun test02_detectsCanonicalTriggerWordMudablajah() {
        val res = SearchTriggerParser.parse("حكاية لعبة مدبلجة")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("حكاية لعبة", res.cleanQuery)
    }

    @Test
    fun test03_detectsCanonicalTriggerWordMudablajeenAndMudablajoon() {
        val res1 = SearchTriggerParser.parse("ابطال مدبلجين")
        assertTrue(res1.isArabicDubbedIntent)
        assertEquals("ابطال", res1.cleanQuery)

        val res2 = SearchTriggerParser.parse("ابطال مدبلجون")
        assertTrue(res2.isArabicDubbedIntent)
        assertEquals("ابطال", res2.cleanQuery)
    }

    @Test
    fun test04_detectsCanonicalTriggerWordDablajah() {
        val res = SearchTriggerParser.parse("دبلجة المحقق كونان")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("المحقق كونان", res.cleanQuery)
    }

    @Test
    fun test05_detectsEnglishTriggerWordDubbedCaseInsensitive() {
        val resLower = SearchTriggerParser.parse("Finding Nemo dubbed")
        assertTrue(resLower.isArabicDubbedIntent)
        assertEquals("Finding Nemo", resLower.cleanQuery)

        val resUpper = SearchTriggerParser.parse("Toy Story DUBBED")
        assertTrue(resUpper.isArabicDubbedIntent)
        assertEquals("Toy Story", resUpper.cleanQuery)
    }

    @Test
    fun test06_normalizesArabicTashkeelInQuery() {
        val res = SearchTriggerParser.parse("مُدَبْلَجٌ تايتانك")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("تايتانك", res.cleanQuery)
    }

    @Test
    fun test07_normalizesPunctuationAroundTrigger() {
        val resParen = SearchTriggerParser.parse("سبايدرمان (مدبلج)")
        assertTrue(resParen.isArabicDubbedIntent)
        assertEquals("سبايدرمان", resParen.cleanQuery)

        val resDash = SearchTriggerParser.parse("باتمان - مدبلج")
        assertTrue(resDash.isArabicDubbedIntent)
        assertEquals("باتمان", resDash.cleanQuery)
    }

    @Test
    fun test08_enforcesStandaloneWordBoundaries() {
        // A word that merely contains substrings without word boundary must NOT trigger dubbed intent
        val res = SearchTriggerParser.parse("احمد بلا مدب")
        assertFalse(res.isArabicDubbedIntent)
    }

    @Test
    fun test09_extractsCleanTitleCorrectly() {
        val res = SearchTriggerParser.parse("انسبشن مدبلج")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("انسبشن", res.cleanQuery)
        assertTrue(res.hasValidMediaTitle)
    }

    @Test
    fun test10_preservesLegitimateTitleWordsLikeAlMalik() {
        val res = SearchTriggerParser.parse("الملك الاسد مدبلج")
        assertTrue(res.isArabicDubbedIntent)
        assertEquals("الملك الاسد", res.cleanQuery)
        assertTrue(res.cleanQuery.contains("الملك"))
    }

    @Test
    fun test11_identifiesEmptyTitleWhenOnlyTriggerPresent() {
        val resOnly = SearchTriggerParser.parse("مدبلج")
        assertTrue(resOnly.isArabicDubbedIntent)
        assertEquals("", resOnly.cleanQuery)
        assertFalse(resOnly.hasValidMediaTitle)

        val resDubbed = SearchTriggerParser.parse("dubbed")
        assertTrue(resDubbed.isArabicDubbedIntent)
        assertEquals("", resDubbed.cleanQuery)
        assertFalse(resDubbed.hasValidMediaTitle)
    }

    @Test
    fun test12_normalizesWhitespace() {
        val normalized = SearchTriggerParser.normalizeWhitespace("   فيلم    هاري   بوتر   ")
        assertEquals("فيلم هاري بوتر", normalized)
    }

    // --- DubbedSearchRankingEngine Tests (13 to 25) ---

    private fun createMovie(
        id: String,
        title: String,
        hasDubbed: Boolean = false,
        sourceCandidates: List<ProviderSourceCandidate> = emptyList()
    ): Movie {
        val candidates = if (sourceCandidates.isNotEmpty()) {
            sourceCandidates
        } else if (hasDubbed) {
            listOf(
                ProviderSourceCandidate(
                    providerId = "egydead",
                    scraperKey = "egydead",
                    title = "$title مدبلج",
                    sourcePageUrl = "https://egydead.link/m/$id",
                    status = CoverageStatus.FOUND,
                    mode = MediaVariantMode.DUBBED
                )
            )
        } else {
            emptyList()
        }
        return Movie(
            id = id,
            title = title,
            originalTitle = title,
            overview = "",
            posterUrl = "",
            backdropUrl = "",
            year = 2020,
            rating = 8.0,
            genres = emptyList(),
            runtime = 120,
            sourceCandidates = candidates
        )
    }

    private fun createSeries(
        id: String,
        title: String,
        hasDubbed: Boolean = false,
        sourceCandidates: List<ProviderSourceCandidate> = emptyList()
    ): Series {
        val candidates = if (sourceCandidates.isNotEmpty()) {
            sourceCandidates
        } else if (hasDubbed) {
            listOf(
                ProviderSourceCandidate(
                    providerId = "witanime",
                    scraperKey = "witanime",
                    title = "$title مدبلج",
                    sourcePageUrl = "https://witanime.link/s/$id",
                    status = CoverageStatus.FOUND,
                    mode = MediaVariantMode.DUBBED
                )
            )
        } else {
            emptyList()
        }
        return Series(
            id = id,
            title = title,
            originalTitle = title,
            overview = "",
            posterUrl = "",
            backdropUrl = "",
            year = 2020,
            rating = 8.0,
            genres = emptyList(),
            sourceCandidates = candidates
        )
    }

    @Test
    fun test13_rankMoviesPrioritizesConfirmedDubbedMatchingQuery() {
        val lionKingSub = createMovie("1", "The Lion King", hasDubbed = false)
        val lionKingDub = createMovie("2", "The Lion King", hasDubbed = true)
        val otherDub = createMovie("3", "Aladdin", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankMoviesForDubbedIntent(
            listOf(lionKingSub, otherDub, lionKingDub),
            cleanQuery = "The Lion King"
        )

        // Tier 1 (Lion King with dubbed source) must be first
        assertEquals("2", ranked.first().id)
    }

    @Test
    fun test14_rankMoviesPrioritizesStandardMatchWhenNoDubbedAvailable() {
        val interstellarSub = createMovie("10", "Interstellar", hasDubbed = false)
        val unrelatedDub = createMovie("20", "Toy Story", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankMoviesForDubbedIntent(
            listOf(unrelatedDub, interstellarSub),
            cleanQuery = "Interstellar"
        )

        // Interstellar (Tier 2) must come first before unrelated dubbed
        assertEquals("10", ranked.first().id)
    }

    @Test
    fun test15_rankMoviesNeverDropsRequestedWorkWhenNoDubbed() {
        val movie = createMovie("100", "Memento", hasDubbed = false)
        val ranked = DubbedSearchRankingEngine.rankMoviesForDubbedIntent(
            listOf(movie),
            cleanQuery = "Memento"
        )
        assertEquals(1, ranked.size)
        assertEquals("100", ranked[0].id)
    }

    @Test
    fun test16_rankMoviesIncludesRelevantDubbedSuggestions() {
        val nemoSub = createMovie("1", "Finding Nemo", hasDubbed = false)
        val doryDub = createMovie("2", "Finding Dory", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankMoviesForDubbedIntent(
            listOf(nemoSub, doryDub),
            cleanQuery = "Finding Nemo"
        )

        // Nemo (Tier 2 primary standard) first, then Dory (Tier 3 relevant dubbed alternative)
        assertEquals("1", ranked[0].id)
        assertEquals("2", ranked[1].id)
    }

    @Test
    fun test17_rankMoviesExcludesUnrelatedDubbedItems() {
        val inception = createMovie("1", "Inception", hasDubbed = false)
        val frozenDub = createMovie("99", "Frozen", hasDubbed = true) // 0 token overlap

        val ranked = DubbedSearchRankingEngine.rankMoviesForDubbedIntent(
            listOf(frozenDub, inception),
            cleanQuery = "Inception"
        )

        // Frozen has 0 relevance to Inception, must NOT be suggested as alternative
        assertEquals(1, ranked.size)
        assertEquals("1", ranked[0].id)
    }

    @Test
    fun test18_rankSeriesPrioritizesConfirmedDubbedMatchingQuery() {
        val conanSub = createSeries("1", "Detective Conan", hasDubbed = false)
        val conanDub = createSeries("2", "Detective Conan", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankSeriesForDubbedIntent(
            listOf(conanSub, conanDub),
            cleanQuery = "Detective Conan"
        )

        assertEquals("2", ranked.first().id)
    }

    @Test
    fun test19_rankSeriesPrioritizesStandardMatchWhenNoDubbedAvailable() {
        val breakingBad = createSeries("1", "Breaking Bad", hasDubbed = false)
        val narutoDub = createSeries("2", "Naruto", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankSeriesForDubbedIntent(
            listOf(narutoDub, breakingBad),
            cleanQuery = "Breaking Bad"
        )

        // Breaking Bad (primary standard) must be first
        assertEquals("1", ranked.first().id)
    }

    @Test
    fun test20_rankSeriesNeverDropsRequestedSeriesWhenNoDubbed() {
        val series = createSeries("5", "Chernobyl", hasDubbed = false)
        val ranked = DubbedSearchRankingEngine.rankSeriesForDubbedIntent(
            listOf(series),
            cleanQuery = "Chernobyl"
        )
        assertEquals(1, ranked.size)
        assertEquals("5", ranked[0].id)
    }

    @Test
    fun test21_rankSeriesIncludesRelevantDubbedSuggestions() {
        val narutoOriginal = createSeries("1", "Naruto", hasDubbed = false)
        val narutoShippudenDub = createSeries("2", "Naruto Shippuden", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankSeriesForDubbedIntent(
            listOf(narutoOriginal, narutoShippudenDub),
            cleanQuery = "Naruto"
        )

        assertEquals("1", ranked[0].id)
        assertEquals("2", ranked[1].id)
    }

    @Test
    fun test22_rankSeriesExcludesUnrelatedDubbedSeries() {
        val series = createSeries("1", "The Wire", hasDubbed = false)
        val onePieceDub = createSeries("99", "One Piece", hasDubbed = true)

        val ranked = DubbedSearchRankingEngine.rankSeriesForDubbedIntent(
            listOf(onePieceDub, series),
            cleanQuery = "The Wire"
        )

        assertEquals(1, ranked.size)
        assertEquals("1", ranked[0].id)
    }

    @Test
    fun test23_rankDropdownMaintainsTierHierarchy() {
        val movieDub = createMovie("1", "Batman", hasDubbed = true)
        val seriesSub = createSeries("2", "Batman", hasDubbed = false)

        val dropdown = DubbedSearchRankingEngine.rankDropdownResults(
            movies = listOf(movieDub),
            series = listOf(seriesSub),
            cleanQuery = "Batman",
            isDubbedIntent = true
        )

        assertEquals(2, dropdown.size)
        // Movie with confirmed dubbed source is Tier 1 (rank 0)
        assertEquals(true, dropdown[0].second) // isMovie = true
    }

    @Test
    fun test24_normalSearchWithoutDubbedIntentPreservesExactOrder() {
        val m1 = createMovie("1", "Avatar", hasDubbed = false)
        val m2 = createMovie("2", "Avatar 2", hasDubbed = true)

        val dropdown = DubbedSearchRankingEngine.rankDropdownResults(
            movies = listOf(m1, m2),
            series = emptyList(),
            cleanQuery = "Avatar",
            isDubbedIntent = false
        )

        // Exact original order preserved without shifting m2 to the front
        assertEquals("1", (dropdown[0].first as Movie).id)
        assertEquals("2", (dropdown[1].first as Movie).id)
    }

    @Test
    fun test25_relevanceCalculationIgnoresStopWords() {
        // Queries sharing only stop words like "the" or "ال" must receive 0.0 relevance
        val score = DubbedSearchRankingEngine.calculateRelevanceScore(
            cleanQuery = "The Matrix",
            title = "The Godfather"
        )
        assertEquals(0.0, score, 0.001)
    }
}
