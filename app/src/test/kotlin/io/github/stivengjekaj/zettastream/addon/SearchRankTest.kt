package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchRankTest {
    private fun m(id: String, name: String, type: String = "series", year: String? = "2020", imdb: String? = null, poster: String? = null) =
        MetaPreview(id, type, name, poster = poster, releaseInfo = year, imdbId = imdb)

    @Test
    fun theSameShowFromThreeAddonsIsOneEntryAtTheTop() {
        val first = listOf(m("tt37376994", "Ted Lasso Does Comic Relief", "movie", "2022"), m("tmdb:1", "Ted Lasso: The Richmond Way"))
        val second = listOf(m("tmdb:97546", "Ted Lasso", imdb = "tt10986410", poster = "https://p.test/a.jpg"))
        val third = listOf(m("tt10986410", "Ted Lasso", year = "2020-2023"), m("x:9", "Ted Lasso", year = "2020"))
        val result = SearchRank.merge("ted lasso", listOf(first, second, third))
        assertEquals("tt10986410", result.first().id)
        assertEquals("https://p.test/a.jpg", result.first().poster)
        assertEquals(1, result.count { it.name == "Ted Lasso" })
        assertEquals(3, result.size)
    }

    @Test
    fun twoShowsWithOneNameButOtherYearsStayApart() {
        val result = SearchRank.merge("dune", listOf(listOf(m("a:1", "Dune", "movie", "1984"), m("b:1", "Dune", "movie", "2021"))))
        assertEquals(2, result.size)
        val noYear = SearchRank.merge("dune", listOf(listOf(m("a:1", "Dune", "movie", null)), listOf(m("b:1", "Dune", "movie", null))))
        assertEquals(2, noYear.size)
    }

    @Test
    fun theBestMatchComesFirst() {
        val result = SearchRank.merge(
            "one piece",
            listOf(listOf(m("1", "Piece of One"), m("2", "One Piece Film: Red", "movie"), m("3", "Someone Piecemeal"), m("4", "One Piece"))),
        )
        assertEquals(listOf("4", "2", "1", "3"), result.map { it.id })
    }

    @Test
    fun accentsCaseAndSignsDoNotMatter() {
        assertEquals("pokemon the movie", SearchRank.normalize("Pokémon: The Movie!"))
        assertEquals(100, SearchRank.score("POKEMON the movie", m("1", "Pokémon: The Movie")))
        assertEquals(100, SearchRank.score("attack on titan", MetaPreview("1", "anime", "Shingeki no Kyojin", aliases = listOf("Attack on Titan"))))
    }
}
