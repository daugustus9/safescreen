package com.safescreen.app.core

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class SchedulerState(
    val now: LocalDateTime,
    val tier: AgeTier,
    val intervalMinutes: Int,
    val bedtimeStart: LocalTime,
    val bedtimeEnd: LocalTime,
    val lastCardAt: LocalDateTime?,
    /** Card ids shown recently, most recent first. */
    val recentCardIds: List<String>,
    val tasks: List<SchoolTask>,
    /** Reminder keys already shown, so each reminder appears once. */
    val shownReminderKeys: Set<String>,
)

data class ScheduledCard(val card: Card, val reminderKey: String? = null)

/**
 * Picks the next card for the message engine shared with the subliminal-message and
 * human-upgrade apps. Priority: school reminders, then bedtime, then a rotation of
 * safety and advice cards fitting the child's age. Every card is visible and readable.
 */
class CardScheduler(
    private val cards: List<Card>,
    /** Builds the localized reminder card for a task; `tomorrow` is false for "today". */
    private val reminderCard: (task: SchoolTask, tomorrow: Boolean) -> Card,
) {

    fun next(state: SchedulerState): ScheduledCard? {
        if (!isDue(state)) return null
        schoolReminder(state)?.let { return it }
        if (inBedtime(state.now.toLocalTime(), state.bedtimeStart, state.bedtimeEnd)) {
            return rotate(cards.filter { it.kind == CardKind.BEDTIME }, state.recentCardIds)
                ?.let { ScheduledCard(it) }
        }
        val age = state.tier.representativeAge
        val eligible = cards.filter {
            (it.kind == CardKind.SAFETY || it.kind == CardKind.ADVICE) && age in it.minAge..it.maxAge
        }
        // Alternate safety and advice so the child doesn't get three warnings in a row.
        val lastKind = state.recentCardIds.firstOrNull()?.let { id -> cards.firstOrNull { it.id == id }?.kind }
        val preferred = if (lastKind == CardKind.SAFETY) CardKind.ADVICE else CardKind.SAFETY
        val pick = rotate(eligible.filter { it.kind == preferred }, state.recentCardIds)
            ?: rotate(eligible, state.recentCardIds)
        return pick?.let { ScheduledCard(it) }
    }

    private fun isDue(state: SchedulerState): Boolean {
        val last = state.lastCardAt ?: return true
        return Duration.between(last, state.now).toMinutes() >= state.intervalMinutes
    }

    /** The evening before (from 17:00) and the morning of (06:00 to 12:00) a due date. */
    private fun schoolReminder(state: SchedulerState): ScheduledCard? {
        val today = state.now.toLocalDate()
        val hour = state.now.hour
        val pending = state.tasks.filter { !it.done && it.dueDate != null }.sortedBy { it.dueDate!!.toEpochDay() }
        for (task in pending) {
            val (tomorrow, slot) = when {
                task.dueDate == today.plusDays(1) && hour >= 17 -> true to "eve"
                task.dueDate == today && hour in 6..11 -> false to "morning"
                else -> continue
            }
            val key = "${task.id}:${task.dueDate}:$slot"
            if (key !in state.shownReminderKeys) return ScheduledCard(reminderCard(task, tomorrow), key)
        }
        return null
    }

    /** First candidate not shown recently; if all were, the one shown longest ago. */
    private fun rotate(candidates: List<Card>, recent: List<String>): Card? {
        if (candidates.isEmpty()) return null
        candidates.firstOrNull { it.id !in recent }?.let { return it }
        return candidates.maxByOrNull { recent.indexOf(it.id) }
    }

    companion object {
        fun inBedtime(time: LocalTime, start: LocalTime, end: LocalTime): Boolean =
            if (start <= end) time >= start && time < end
            else time >= start || time < end
    }
}
