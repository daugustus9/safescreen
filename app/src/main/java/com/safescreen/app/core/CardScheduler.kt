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
    /** Kinds in the everyday rotation; the family agreement can switch encouragement off. */
    val rotationKinds: Set<CardKind> = ROTATION_ORDER.toSet(),
    /** Encouragement messages the family wrote, already turned into cards. */
    val familyCards: List<Card> = emptyList(),
    /** Invitation to today's question, when the child has one waiting; shown once a day. */
    val studyInvite: Card? = null,
)

/** Everyday cards take turns in this order, so warnings never come three in a row. */
val ROTATION_ORDER = listOf(CardKind.SAFETY, CardKind.ADVICE, CardKind.ENCOURAGEMENT)

data class ScheduledCard(val card: Card, val reminderKey: String? = null)

/**
 * Picks the next card for the message engine shared with the subliminal-message and
 * human-upgrade apps. Priority: school reminders, then bedtime, then today's study
 * invitation (once a day), then a rotation of
 * safety, advice and encouragement cards fitting the child's age. Every card is visible
 * and readable.
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
        state.studyInvite?.let { invite ->
            val key = "study:${state.now.toLocalDate()}"
            if (key !in state.shownReminderKeys) return ScheduledCard(invite, key)
        }
        val age = state.tier.representativeAge
        val all = cards + state.familyCards
        val eligible = all.filter { it.kind in state.rotationKinds && age in it.minAge..it.maxAge }
        val order = ROTATION_ORDER.filter { it in state.rotationKinds }
        if (order.isEmpty()) return null

        // The kind after the last one shown goes first; kinds with no card are skipped.
        val lastKind = state.recentCardIds.firstOrNull()?.let { id -> all.firstOrNull { it.id == id }?.kind }
        val start = (order.indexOf(lastKind) + 1) % order.size
        for (step in order.indices) {
            val kind = order[(start + step) % order.size]
            rotate(eligible.filter { it.kind == kind }, state.recentCardIds)?.let { return ScheduledCard(it) }
        }
        return null
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
