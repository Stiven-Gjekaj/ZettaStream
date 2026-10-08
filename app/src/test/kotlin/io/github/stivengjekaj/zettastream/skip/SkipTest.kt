package io.github.stivengjekaj.zettastream.skip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkipTest {
    private val ranges = listOf(
        SkipRange(SkipKind.Opening, 60_000, 150_000),
        SkipRange(SkipKind.Ending, 1_284_000, 1_377_000),
    )

    @Test
    fun insideARangeTheButtonGoesToItsEnd() {
        assertEquals(SkipAction.SeekTo(150_000, SkipKind.Opening), Skip.action(ranges, 90_000))
        assertEquals(SkipAction.SeekTo(1_377_000, SkipKind.Ending), Skip.action(ranges, 1_300_000))
    }

    @Test
    fun aShortColdOpenStillSkipsTheOpening() {
        assertEquals(SkipAction.SeekTo(150_000, SkipKind.Opening), Skip.action(ranges, 5_000))
    }

    @Test
    fun withNoRangeTheButtonJumps() {
        assertEquals(SkipAction.Jump(Skip.FALLBACK), Skip.action(ranges, 600_000))
        assertEquals(SkipAction.Jump(Skip.FALLBACK), Skip.action(emptyList(), 0))
    }

    @Test
    fun theCurrentRangeIsTheOneThatHoldsThePosition() {
        assertEquals(SkipKind.Opening, Skip.current(ranges, 61_000)?.kind)
        assertNull(Skip.current(ranges, 150_000))
    }

    @Test
    fun readsTheMalIdFromKitsuMappings() {
        val json = """{"data":[{"attributes":{"externalSite":"thetvdb/series","externalId":"79481"}},
            {"attributes":{"externalSite":"myanimelist/anime","externalId":"1535"}}]}"""
        assertEquals(1535, Skip.malIdFromKitsu(json))
        assertNull(Skip.malIdFromKitsu("""{"data":[]}"""))
        assertNull(Skip.malIdFromKitsu("not json"))
    }

    @Test
    fun readsAnAniSkipAnswer() {
        val json = """{"found":true,"results":[
            {"interval":{"startTime":1284,"endTime":1377},"skipType":"ed"},
            {"interval":{"startTime":1.039,"endTime":91.039},"skipType":"op"},
            {"interval":{"startTime":5,"endTime":5},"skipType":"recap"}]}"""
        assertEquals(
            listOf(SkipRange(SkipKind.Opening, 1_039, 91_039), SkipRange(SkipKind.Ending, 1_284_000, 1_377_000)),
            Skip.parseAniSkip(json),
        )
        assertEquals(emptyList<SkipRange>(), Skip.parseAniSkip("""{"found":false,"results":[]}"""))
    }

    @Test
    fun readsTheKitsuEpisodeFromAVideoId() {
        assertEquals(1376 to 5, Skip.kitsuEpisode("kitsu:1376:5"))
        assertNull(Skip.kitsuEpisode("tt0903747:1:2"))
    }
}
