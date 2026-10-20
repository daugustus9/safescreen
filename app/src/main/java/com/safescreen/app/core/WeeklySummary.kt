package com.safescreen.app.core

import java.time.LocalDate

/**
 * The parent's weekly summary (T1 + C3): counts and categories, never content. What it
 * includes depends on the age tier, and the child can see the same summary.
 */
data class WeeklySummary(
    val riskByCategory: Map<String, Int>,
    val helpPresses: Int,
    val cardsShown: Int,
    val focusMinutes: Int,
    val focusBlocks: Int,
    val schoolNotices: Int,
    val tasksDone: Int,
    /** Pending tasks due in the next 7 days (or undated), soonest first. */
    val upcoming: List<SchoolTask>,
    val totalScreenMinutes: Int,
    /** Empty unless the tier allows per-app detail. */
    val topApps: List<AppUsage>,
) {
    companion object {
        private const val TOP_APPS = 5

        fun build(
            events: List<Event>,
            tasks: List<SchoolTask>,
            usage: List<AppUsage>,
            tier: AgeTier,
            today: LocalDate,
        ): WeeklySummary {
            fun count(type: EventType) = events.count { it.type == type }
            val horizon = today.plusDays(7)
            return WeeklySummary(
                riskByCategory = events
                    .filter { it.type == EventType.RISK_DETECTED && it.category != null }
                    .groupingBy { it.category!! }
                    .eachCount(),
                helpPresses = count(EventType.HELP_PRESSED),
                cardsShown = count(EventType.CARD_SHOWN),
                focusMinutes = events.filter { it.type == EventType.FOCUS_COMPLETED }.sumOf { it.value },
                focusBlocks = count(EventType.FOCUS_BLOCKED),
                schoolNotices = count(EventType.SCHOOL_NOTICE),
                tasksDone = count(EventType.TASK_DONE),
                upcoming = tasks
                    .filter { !it.done && (it.dueDate == null || !it.dueDate.isAfter(horizon)) }
                    .sortedBy { it.dueDate?.toEpochDay() ?: Long.MAX_VALUE },
                totalScreenMinutes = usage.sumOf { it.minutes },
                topApps = if (tier.showsAppDetails) {
                    usage.sortedByDescending { it.minutes }.take(TOP_APPS)
                } else {
                    emptyList()
                },
            )
        }
    }
}
