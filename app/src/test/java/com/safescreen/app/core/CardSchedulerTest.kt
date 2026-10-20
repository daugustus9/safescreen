package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CardSchedulerTest {

    private val cards = listOf(
        Card("safety-a", CardKind.SAFETY, "S", "s", minAge = 6, maxAge = 17),
        Card("safety-teen", CardKind.SAFETY, "S", "s", minAge = 13, maxAge = 17),
        Card("advice-a", CardKind.ADVICE, "A", "a"),
        Card("advice-b", CardKind.ADVICE, "A", "a"),
        Card("bedtime-1", CardKind.BEDTIME, "B", "b"),
    )
    private val scheduler = CardScheduler(cards) { task, tomorrow ->
        Card("reminder-${task.id}", CardKind.SCHOOL, if (tomorrow) "Tomorrow" else "Today", task.title)
    }

    private val monday3pm = LocalDateTime.of(2026, 10, 19, 15, 0)

    private fun state(
        now: LocalDateTime = monday3pm,
        lastCardAt: LocalDateTime? = null,
        recent: List<String> = emptyList(),
        tasks: List<SchoolTask> = emptyList(),
        reminders: Set<String> = emptySet(),
        tier: AgeTier = AgeTier.AGE_10_12,
    ) = SchedulerState(
        now = now, tier = tier, intervalMinutes = 30,
        bedtimeStart = LocalTime.of(21, 30), bedtimeEnd = LocalTime.of(7, 0),
        lastCardAt = lastCardAt, recentCardIds = recent, tasks = tasks, shownReminderKeys = reminders,
    )

    private fun task(due: LocalDate?, done: Boolean = false) =
        SchoolTask("t1", TaskKind.TEST, "Math test", due, sourceApp = null, done = done, createdAtMillis = 0)

    @Test
    fun waitsForTheAgreedInterval() {
        assertNull(scheduler.next(state(lastCardAt = monday3pm.minusMinutes(10))))
        assertEquals("safety-a", scheduler.next(state(lastCardAt = monday3pm.minusMinutes(30)))!!.card.id)
    }

    @Test
    fun remindsTheEveningBeforeAndOnlyOnce() {
        val evening = LocalDateTime.of(2026, 10, 19, 18, 0)
        val tomorrowTest = task(LocalDate.of(2026, 10, 20))

        val first = scheduler.next(state(now = evening, tasks = listOf(tomorrowTest)))!!
        assertEquals("Tomorrow", first.card.title)
        assertEquals("t1:2026-10-20:eve", first.reminderKey)

        val again = scheduler.next(state(now = evening, tasks = listOf(tomorrowTest), reminders = setOf(first.reminderKey!!)))!!
        assertNotEquals(CardKind.SCHOOL, again.card.kind)
    }

    @Test
    fun remindsOnTheMorningOfButNotForDoneTasks() {
        val morning = LocalDateTime.of(2026, 10, 20, 7, 30)
        assertEquals("Today", scheduler.next(state(now = morning, tasks = listOf(task(LocalDate.of(2026, 10, 20)))))!!.card.title)
        assertEquals(CardKind.SAFETY, scheduler.next(state(now = morning, tasks = listOf(task(LocalDate.of(2026, 10, 20), done = true))))!!.card.kind)
    }

    @Test
    fun showsBedtimeCardsAcrossMidnight() {
        assertEquals(CardKind.BEDTIME, scheduler.next(state(now = LocalDateTime.of(2026, 10, 19, 22, 0)))!!.card.kind)
        assertEquals(CardKind.BEDTIME, scheduler.next(state(now = LocalDateTime.of(2026, 10, 20, 6, 30)))!!.card.kind)
    }

    @Test
    fun alternatesSafetyAndAdviceAndRespectsAge() {
        assertEquals("advice-a", scheduler.next(state(recent = listOf("safety-a")))!!.card.id)
        assertEquals("safety-a", scheduler.next(state(recent = listOf("advice-a")))!!.card.id)
        // Only teens get the teen card; an 11-year-old who saw safety-a recently gets the oldest instead.
        val tween = scheduler.next(state(recent = listOf("advice-b", "advice-a", "safety-a")))!!
        assertEquals("safety-a", tween.card.id)
        val teen = scheduler.next(state(recent = listOf("advice-b", "advice-a", "safety-a"), tier = AgeTier.TEEN_13_PLUS))!!
        assertEquals("safety-teen", teen.card.id)
    }

    @Test
    fun bedtimeWindowWithoutMidnight() {
        assert(CardScheduler.inBedtime(LocalTime.of(14, 0), LocalTime.of(13, 0), LocalTime.of(15, 0)))
        assert(!CardScheduler.inBedtime(LocalTime.of(15, 0), LocalTime.of(13, 0), LocalTime.of(15, 0)))
    }
}
