package fr.perso.nimbus.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** API de kacky.gg (réponses relevées le 1er novembre 2026) : édition en cours et haut du classement. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KackyShareTest {

    private val events = """{"data":[
        {"id":26,"edition":7,"name":"${'$'}o${'$'}i${'$'}aa0Kack${'$'}05ay Re${'$'}09alo${'$'}6a0ad${'$'}aa0ed ${'$'}4f07","eventType":"KR","startTime":"2026-10-30T19:00:00.0000000Z","endTime":"2026-11-29T19:00:00.0000000Z","isActive":true},
        {"id":204,"edition":4,"name":"Xtreme 4","eventType":"KX","startTime":"2026-05-01T16:00:00.0000000Z","endTime":"2026-05-31T16:00:00.0000000Z","isActive":false},
        {"id":11,"edition":6,"name":"KR6","eventType":"KR","startTime":"2025-11-02T18:00:00.0000000Z","endTime":"2025-12-01T18:00:00.0000000Z","isActive":false},
        {"id":99,"edition":0,"name":"Hunting","eventType":"KRHX","startTime":"2020-09-01T18:00:00.0000000Z","endTime":"2030-12-31T23:59:59.0000000Z","isActive":true}
    ],"meta":{"total":4}}"""

    private val board = """{"data":[
        {"rank":2,"playerId":814,"nickname":"LinkMaxTm","customNickname":"${'$'}00d»ғฟ๏« ${'$'}fffĿιиκ.${'$'}f00* ${'$'}m${'$'}bbbLaw","finishCount":19},
        {"rank":1,"playerId":78231,"nickname":"Blev..","customNickname":"${'$'}F00฿${'$'}F30Ł${'$'}F60Ę${'$'}F90Ѷ","finishCount":21},
        {"rank":3,"playerId":65263,"nickname":"SuperBacon53","customNickname":"","finishCount":19},
        {"rank":4,"playerId":60561,"nickname":"BruhHour","customNickname":"x","finishCount":19}
    ],"meta":{"total":1489,"limit":3,"offset":0}}"""

    @Test
    fun `retire la mise en forme des noms Trackmania`() {
        assertEquals("Kacky Reloaded 7", KackyShare.plain("${'$'}o${'$'}i${'$'}aa0Kack${'$'}05ay Re${'$'}09alo${'$'}6a0ad${'$'}aa0ed ${'$'}4f07"))
        assertEquals("»ғฟ๏« Ŀιиκ.* Law", KackyShare.plain("${'$'}00d»ғฟ๏« ${'$'}fffĿιиκ.${'$'}f00* ${'$'}m${'$'}bbbLaw"))
        assertEquals("zeky", KackyShare.plain("${'$'}ff0zeky"))
        assertEquals("a${'$'}b", KackyShare.plain("a${'$'}${'$'}b"))
        assertEquals("site", KackyShare.plain("${'$'}l[https://x.fr]site${'$'}l"))
        assertEquals("fin", KackyShare.plain("fin${'$'}"))
    }

    @Test
    fun `choisit le Kacky Reloaded en cours`() {
        val during = KackyShare.parseEvent(events, Instant.parse("2026-11-01T01:40:00Z"))
        assertNotNull(during)
        assertEquals(26, during!!.id)
        assertEquals(7, during.edition)
        assertEquals("Kacky Reloaded 7", during.name)
        assertEquals("https://kacky.gg/event/26", KackyShare.eventUrl(during))
    }

    @Test
    fun `ni avant le début ni longtemps après la fin`() {
        assertNull(KackyShare.parseEvent(events, Instant.parse("2026-10-30T18:00:00Z")))
        // Juste après la fin : le classement final reste ; douze heures plus tard, non.
        assertNotNull(KackyShare.parseEvent(events, Instant.parse("2026-11-29T20:00:00Z")))
        assertNull(KackyShare.parseEvent(events, Instant.parse("2026-11-30T08:00:00Z")))
    }

    @Test
    fun `lit le podium dans l'ordre des rangs`() {
        val (top, players) = KackyShare.parseTop(board)
        assertEquals(1489, players)
        assertEquals(listOf(1, 2, 3), top.map { it.rank })
        assertEquals(21, top[0].finishes)
        assertEquals("฿ŁĘѶ", top[0].name)
        // Sans pseudo coloré : le pseudo Trackmania.
        assertEquals("SuperBacon53", top[2].name)
        assertTrue(top.size <= KackyShare.TOP)
    }

    @Test
    fun `réponse vide`() {
        assertEquals(emptyList<KackyEntry>(), KackyShare.parseTop("""{"data":[],"meta":{"total":0}}""").first)
        assertNull(KackyShare.parseEvent("""{"data":[]}""", Instant.now()))
    }
}
