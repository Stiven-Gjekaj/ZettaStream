package io.github.stivengjekaj.zettastream.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarsTest {
    @Test
    fun readsAniListCharactersAndLeavesOutTheDefaultPicture() {
        val text = """{"data":{"Page":{"characters":[
            {"name":{"full":"Satoru Gojou"},"image":{"large":"https://s4.anilist.co/c/large/b1.png"}},
            {"name":{"full":"Nobody"},"image":{"large":"https://s4.anilist.co/c/large/default.jpg"}},
            {"name":{"full":"No picture"},"image":{"large":null}}]}}}"""
        assertEquals(listOf(Avatar("Satoru Gojou", "https://s4.anilist.co/c/large/b1.png")), Avatars.Parse.anilist(text))
        assertEquals(emptyList<Avatar>(), Avatars.Parse.anilist("""{"errors":[{"message":"Too Many Requests"}]}"""))
    }

    @Test
    fun aCastUsesTheCharacterPictureOrElseTheActor() {
        val text = """[
            {"person":{"name":"Bryan Cranston","image":{"medium":"https://t.test/p1.jpg"}},"character":{"name":"Walter White","image":{"medium":"https://t.test/c1.jpg"}}},
            {"person":{"name":"Aaron Paul","image":{"medium":"https://t.test/p2.jpg"}},"character":{"name":"Jesse Pinkman","image":null}},
            {"person":{"name":"X","image":null},"character":{"name":"Y","image":null}}]"""
        assertEquals(
            listOf(Avatar("Walter White", "https://t.test/c1.jpg"), Avatar("Jesse Pinkman", "https://t.test/p2.jpg")),
            Avatars.Parse.tvmazeCast(text),
        )
    }

    @Test
    fun readsPeopleFromASearch() {
        val text = """[{"score":1,"person":{"name":"Morgan Freeman","image":{"medium":"https://t.test/m.jpg"}}},{"score":0.5,"person":{"name":"Other","image":null}}]"""
        assertEquals(listOf(Avatar("Morgan Freeman", "https://t.test/m.jpg")), Avatars.Parse.tvmazePeople(text))
    }
}
