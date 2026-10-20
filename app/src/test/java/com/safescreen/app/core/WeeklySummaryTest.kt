package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklySummaryTest {

    private val today = LocalDate.of(2026, 10, 19)
    private val events = listOf(
        Event(1, EventType.RISK_DETECTED, "SELF_HARM", 3),
        Event(2, EventType.RISK_DETECTED, "DANGEROUS_CHALLENGE", 2),
        Event(3, EventType.RISK_DETECTED, "DANGEROUS_CHALLENGE", 2),
        Event(4, EventType.HELP_PRESSED),
        Event(5, EventType.FOCUS_COMPLETED, value = 25),
        Event(6, EventType.FOCUS_COMPLETED, value = 25),
        Event(7, EventType.CARD_SHOWN, "SAFETY"),
        Event(8, EventType.SCHOOL_NOTICE, "TEST"),
        Event(9, EventType.TASK_DONE),
    )
    private val usage = listOf(AppUsage("YouTube", 300), AppUsage("Classroom", 40), AppUsage("Game", 120))
    private val tasks = listOf(
        SchoolTask("late", TaskKind.EVENT, "Trip", today.plusDays(20), null, createdAtMillis = 0),
        SchoolTask("soon", TaskKind.TEST, "Math", today.plusDays(1), null, createdAtMillis = 0),
        SchoolTask("undated", TaskKind.HOMEWORK, "Essay", null, null, createdAtMillis = 0),
        SchoolTask("done", TaskKind.HOMEWORK, "Done", today, null, done = true, createdAtMillis = 0),
    )

    @Test
    fun countsCategoriesNotContent() {
        val summary = WeeklySummary.build(events, tasks, usage, AgeTier.UNDER_10, today)
        assertEquals(mapOf("SELF_HARM" to 1, "DANGEROUS_CHALLENGE" to 2), summary.riskByCategory)
        assertEquals(1, summary.helpPresses)
        assertEquals(50, summary.focusMinutes)
        assertEquals(1, summary.cardsShown)
        assertEquals(1, summary.schoolNotices)
        assertEquals(1, summary.tasksDone)
        assertEquals(460, summary.totalScreenMinutes)
    }

    @Test
    fun upcomingIsPendingWithinAWeekSoonestFirst() {
        val summary = WeeklySummary.build(events, tasks, usage, AgeTier.UNDER_10, today)
        assertEquals(listOf("soon", "undated"), summary.upcoming.map { it.id })
    }

    @Test
    fun perAppDetailOnlyForTheYoungestTier() {
        assertEquals(
            listOf("YouTube", "Game", "Classroom"),
            WeeklySummary.build(events, tasks, usage, AgeTier.UNDER_10, today).topApps.map { it.label },
        )
        assertTrue(WeeklySummary.build(events, tasks, usage, AgeTier.AGE_10_12, today).topApps.isEmpty())
        assertTrue(WeeklySummary.build(events, tasks, usage, AgeTier.TEEN_13_PLUS, today).topApps.isEmpty())
    }
}
