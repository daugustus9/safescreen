package com.safescreen.app.core

import org.json.JSONObject
import kotlin.random.Random

/** TRUE = verdade, FALSE = mito, DEPENDS = honestly depends, or is disputed. */
enum class Verdict { TRUE, FALSE, DEPENDS }

data class Claim(
    val id: String,
    val verdict: Verdict,
    val minAge: Int,
    /** The Dica do Dia it teaches, if any. */
    val hintId: String?,
    val text: String,
    val explanation: String,
)

/** The child's latest answer to a claim. */
data class ClaimResult(val claimId: String, val correct: Boolean, val timestampMillis: Long)

/**
 * "Mito ou Verdade?": rounds of five claims about safety, health, the digital world and
 * more. No timer and no ranking; new claims come first, then the ones the child missed.
 */
object MythOrFact {

    const val ROUND = 5

    fun parse(json: String, language: String): List<Claim> {
        val array = JSONObject(json).getJSONArray("claims")
        return (0 until array.length()).map { i ->
            val c = array.getJSONObject(i)
            val text = c.optJSONObject(language) ?: c.getJSONObject("en")
            Claim(
                id = c.getString("id"),
                verdict = Verdict.valueOf(c.getString("verdict")),
                minAge = c.optInt("minAge", 6),
                hintId = c.optString("hint").ifEmpty { null },
                text = text.getString("claim"),
                explanation = text.getString("explanation"),
            )
        }
    }

    /**
     * Next round: claims never answered first (in random order), then claims last answered
     * wrong (oldest first), then the rest, least recently seen first.
     */
    fun round(claims: List<Claim>, results: List<ClaimResult>, age: Int, random: Random = Random.Default): List<Claim> {
        val eligible = claims.filter { age >= it.minAge }
        val latest = results.groupBy { it.claimId }.mapValues { (_, list) -> list.maxBy { it.timestampMillis } }
        val fresh = eligible.filter { it.id !in latest }.shuffled(random)
        val missed = eligible.filter { latest[it.id]?.correct == false }.sortedBy { latest.getValue(it.id).timestampMillis }
        val known = eligible.filter { latest[it.id]?.correct == true }.sortedBy { latest.getValue(it.id).timestampMillis }
        return (fresh + missed + known).take(ROUND)
    }

    /** Result line for sharing: squares only, never the claims or answers. */
    fun squares(correct: List<Boolean>): String = correct.joinToString("") { if (it) "🟩" else "🟥" }
}
