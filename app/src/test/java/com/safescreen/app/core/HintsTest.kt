package com.safescreen.app.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HintsTest {

    private val json = File("src/main/assets/content/hints.json").readText()
    private val parsed = DailyHint.parse(json, "pt")
    private val hints = parsed.first
    private val categories = parsed.second
    private val oct20 = LocalDate.of(2026, 10, 20)

    @Test
    fun libraryIsCompleteInBothLanguages() {
        val raw = JSONObject(json).getJSONArray("hints")
        assertEquals(50, raw.length())
        for (i in 0 until raw.length()) {
            val h = raw.getJSONObject(i)
            for (lang in listOf("pt", "en")) {
                assertTrue("${h.getString("id")} $lang", h.getJSONObject(lang).getString("body").length > 30)
            }
            assertTrue(h.getString("source") == "safescreen" || h.getString("source").startsWith("human-upgrade:"))
        }
        assertEquals(hints.size, hints.map { it.id }.toSet().size)
        hints.forEach { assertTrue("${it.id} category", it.category in categories) }
        assertTrue(hints.all { it.kind == CardKind.SAFETY || it.kind == CardKind.ADVICE })
        assertEquals(setOf("hint-viral-challenges", "hint-golden-rule", "hint-stoicism", "hint-ubuntu"), hints.filter { it.perspective }.map { it.id }.toSet())
    }

    @Test
    fun dailyHintIsTheSameForTheSameAgeAndRespectsAge() {
        assertEquals(DailyHint.forDate(oct20, hints, 14), DailyHint.forDate(oct20, hints.shuffled(), 14))
        val days = (0L until 100L).map { oct20.plusDays(it) }
        assertTrue(days.all { DailyHint.forDate(it, hints, 6)!!.minAge <= 6 })
        // The suicide-prevention hint is for 13 and up only.
        assertTrue(days.none { DailyHint.forDate(it, hints, 11)!!.id == "hint-ask-directly" })
    }

    @Test
    fun everySuitedHintComesBeforeAnyRepeats() {
        for (age in listOf(6, 9, 11, 14)) {
            val suited = hints.count { age >= it.minAge }
            val shown = (0L until suited.toLong()).map { DailyHint.forDate(oct20.plusDays(it), hints, age)!!.id }
            assertEquals("age $age", suited, shown.toSet().size)
        }
    }

    @Test
    fun noHintsForNobody() {
        assertNull(DailyHint.forDate(oct20, emptyList(), 14))
    }

    @Test
    fun hintCardShowsOnceADayAndIsShareable() {
        val hint = DailyHint.forDate(oct20, hints, 14)!!.toCard("Dica do Dia: ")
        assertTrue(hint.title.startsWith("Dica do Dia: "))
        assertTrue(Sharing.isShareable(hint))
        val scheduler = CardScheduler(listOf(Card("safety-a", CardKind.SAFETY, "S", "s"))) { task, _ ->
            Card("reminder-${task.id}", CardKind.SCHOOL, "R", task.title)
        }
        fun state(shown: Set<String>) = SchedulerState(
            now = LocalDateTime.of(oct20, LocalTime.of(16, 0)), tier = AgeTier.TEEN_13_PLUS, intervalMinutes = 30,
            bedtimeStart = LocalTime.of(21, 30), bedtimeEnd = LocalTime.of(7, 0), lastCardAt = null,
            recentCardIds = emptyList(), tasks = emptyList(), shownReminderKeys = shown, dailyHint = hint,
        )
        val first = scheduler.next(state(emptySet()))!!
        assertEquals(hint.id, first.card.id)
        assertEquals("hint:2026-10-20", first.reminderKey)
        assertEquals("safety-a", scheduler.next(state(setOf("hint:2026-10-20")))!!.card.id)
    }
}
