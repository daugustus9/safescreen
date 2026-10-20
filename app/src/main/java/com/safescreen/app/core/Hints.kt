package com.safescreen.app.core

import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** One "Dica do Dia": an important fact or tip, adapted from the Human Upgrade library. */
data class Hint(
    val id: String,
    /** SAFETY or ADVICE: sets the card's color and its sharing intro. */
    val kind: CardKind,
    val category: String,
    /** A view (e.g. Stoicism), not a proven fact; the app says so. */
    val perspective: Boolean,
    val minAge: Int,
    val title: String,
    val body: String,
) {
    /** The on-screen card, titled so it reads as the day's hint wherever it's seen or shared. */
    fun toCard(titlePrefix: String): Card = Card(id, kind, "$titlePrefix$title", body, minAge = minAge)
}

object DailyHint {

    private val STEPS = listOf(7L, 11L, 13L, 17L, 19L, 23L)

    /**
     * The hint for a date: chosen only from the date, among hints suited to the age, so
     * children of the same age see the same hint and can talk about it. Every suited hint
     * comes once before any repeats.
     */
    fun forDate(date: LocalDate, hints: List<Hint>, age: Int): Hint? {
        val eligible = hints.filter { age >= it.minAge }.sortedBy { it.id }
        if (eligible.isEmpty()) return null
        val n = eligible.size.toLong()
        val step = STEPS.firstOrNull { gcd(it, n) == 1L } ?: 1L
        val day = ChronoUnit.DAYS.between(DailyChallenge.FIRST_DAY, date)
        return eligible[Math.floorMod(day * step, n).toInt()]
    }

    fun parse(json: String, language: String): Pair<List<Hint>, Map<String, String>> {
        val root = JSONObject(json)
        val categoriesJson = root.getJSONObject("categories")
        val categories = categoriesJson.keys().asSequence().associateWith { key ->
            val names = categoriesJson.getJSONObject(key)
            names.optString(language).ifEmpty { names.getString("en") }
        }
        val array = root.getJSONArray("hints")
        val hints = (0 until array.length()).map { i ->
            val h = array.getJSONObject(i)
            val text = h.optJSONObject(language) ?: h.getJSONObject("en")
            Hint(
                id = h.getString("id"),
                kind = CardKind.valueOf(h.getString("kind")),
                category = h.getString("category"),
                perspective = h.optString("type") == "perspective",
                minAge = h.optInt("minAge", 6),
                title = text.getString("title"),
                body = text.getString("body"),
            )
        }
        return hints to categories
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
}
