package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankSpeedTest {
    private fun answers(addons: Int) = (0 until addons).map { a ->
        (0 until 100).map { i -> MetaPreview("a$a:$i", if (i % 2 == 0) "movie" else "series", "Title $a number $i of the long list", releaseInfo = "${2000 + i % 20}") }
    }

    /** The shortest of a few runs, so that a pause of the machine does not count. */
    private fun time(input: List<List<MetaPreview>>): Long {
        repeat(3) { SearchRank.merge("title", input) }
        return (0 until 5).minOf {
            val start = System.nanoTime()
            SearchRank.merge("title", input)
            System.nanoTime() - start
        }
    }

    /**
     * Four times the results take about four times the time. The old merge compared each
     * result with each other result, so it took about sixteen times the time, and it froze the
     * app with a long source list. A ratio does not depend on the speed of the machine.
     */
    @Test
    fun theTimeGrowsWithTheResultsAndNotWithTheirSquare() {
        val small = time(answers(10))
        val large = time(answers(40))
        val ratio = large.toDouble() / small
        assertTrue("4 times the results took $ratio times the time", ratio < 9)
    }
}
