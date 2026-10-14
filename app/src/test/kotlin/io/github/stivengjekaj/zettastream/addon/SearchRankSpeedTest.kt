package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankSpeedTest {
    /** Many addons with many results, as on a TV with a long source list. */
    @Test
    fun aLargeSearchMergesQuickly() {
        val answers = (0 until 40).map { a ->
            (0 until 100).map { i -> MetaPreview(if (i % 3 == 0) "tt$i" else "a$a:$i", if (i % 2 == 0) "movie" else "series", "Title number $i of the long list", releaseInfo = "${2000 + i % 20}") }
        }
        val start = System.nanoTime()
        // The search merges again after each answer.
        for (n in 1..answers.size) SearchRank.merge("title", answers.take(n))
        val ms = (System.nanoTime() - start) / 1_000_000
        // The old merge took about 2 s here. The limit leaves room for a slow build server.
        assertTrue("40 merges took $ms ms", ms < 1_000)
    }
}
