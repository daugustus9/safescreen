package com.safescreen.app.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class MythOrFactTest {

    private val json = File("src/main/assets/content/myth_or_fact.json").readText()
    private val claims = MythOrFact.parse(json, "pt")
    private val hintIds = DailyHint.parse(File("src/main/assets/content/hints.json").readText(), "pt").first.map { it.id }.toSet()

    @Test
    fun claimsAreCompleteAndLinkedToRealHints() {
        assertEquals(38, claims.size)
        assertEquals(claims.size, claims.map { it.id }.toSet().size)
        val raw = JSONObject(json).getJSONArray("claims")
        for (i in 0 until raw.length()) {
            val c = raw.getJSONObject(i)
            for (lang in listOf("pt", "en")) {
                assertTrue("${c.getString("id")} $lang", c.getJSONObject(lang).getString("explanation").length > 30)
            }
        }
        claims.mapNotNull { it.hintId }.forEach { assertTrue("hint $it", it in hintIds) }
        // All three answers are used, so "Depende" is a real option, not a trick.
        assertEquals(mapOf(Verdict.FALSE to 25, Verdict.TRUE to 9, Verdict.DEPENDS to 4), claims.groupingBy { it.verdict }.eachCount())
    }

    @Test
    fun roundHasFiveClaimsSuitedToTheAge() {
        val round = MythOrFact.round(claims, emptyList(), age = 9, random = Random(1))
        assertEquals(5, round.size)
        assertTrue(round.all { it.minAge <= 9 })
        assertEquals(5, round.map { it.id }.toSet().size)
    }

    @Test
    fun newClaimsFirstThenMissedOnes() {
        val age = 14
        val eligible = claims.filter { age >= it.minAge }
        // Everything answered right except two misses.
        val results = eligible.mapIndexed { i, c -> ClaimResult(c.id, correct = true, timestampMillis = i.toLong()) }
            .map {
                when (it.claimId) {
                    "mf-vaccines" -> ClaimResult(it.claimId, false, 100)
                    "mf-flood" -> ClaimResult(it.claimId, false, 50)
                    else -> it
                }
            }
        val round = MythOrFact.round(claims, results, age, Random(2))
        assertEquals(listOf("mf-flood", "mf-vaccines"), round.take(2).map { it.id })

        // With nothing answered yet, every claim in the first rounds is new.
        val fresh = MythOrFact.round(claims, results.filter { it.claimId == "mf-flood" }, age, Random(3))
        assertTrue(fresh.none { it.id == "mf-flood" })
    }

    @Test
    fun sharedResultIsSquaresOnly() {
        assertEquals("🟩🟥🟩🟩🟥", MythOrFact.squares(listOf(true, false, true, true, false)))
    }
}
