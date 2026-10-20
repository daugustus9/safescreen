package com.safescreen.app.core

import java.time.LocalDate

/**
 * Plain models shared by the logic in this package. Nothing here touches Android, so the
 * classifier, parser, scheduler and summary can all be unit-tested on the JVM.
 */

/** Oversight presets (T6). The child can see which tier is active and what it shares. */
enum class AgeTier(
    /** Age used to pick age-appropriate cards. */
    val representativeAge: Int,
    val defaultCardIntervalMinutes: Int,
    /** Whether the parent summary lists minutes per app, or only totals. */
    val showsAppDetails: Boolean,
) {
    UNDER_10(representativeAge = 9, defaultCardIntervalMinutes = 20, showsAppDetails = true),
    AGE_10_12(representativeAge = 11, defaultCardIntervalMinutes = 30, showsAppDetails = false),
    TEEN_13_PLUS(representativeAge = 14, defaultCardIntervalMinutes = 45, showsAppDetails = false),
}

/**
 * ADVICE gives life guidance (W1); ENCOURAGEMENT is short positive messages for
 * self-view, habits, kindness and citizenship, including ones the family writes.
 * STUDY is the once-a-day invitation to answer a question, which opens in the app.
 * Every kind is shown visibly, long enough to read; nothing is subliminal.
 */
enum class CardKind { SAFETY, ADVICE, ENCOURAGEMENT, SCHOOL, HELP, BEDTIME, FOCUS, STUDY }

/** One on-screen card. Text is already in the device language when it reaches here. */
data class Card(
    val id: String,
    val kind: CardKind,
    val title: String,
    val body: String,
    val minAge: Int = 0,
    val maxAge: Int = 99,
    /** For HELP cards: the [RiskCategory] name they answer. */
    val riskCategory: String? = null,
)

enum class TaskKind { TEST, HOMEWORK, PERMISSION, MATERIALS, EVENT }

/** A school task, from a school-app notification (C2) or typed in by hand. */
data class SchoolTask(
    val id: String,
    val kind: TaskKind,
    val title: String,
    val dueDate: LocalDate?,
    /** Package of the school app it came from, or null when typed in by hand. */
    val sourceApp: String?,
    val done: Boolean = false,
    val createdAtMillis: Long,
)

enum class EventType {
    RISK_DETECTED,
    HELP_PRESSED,
    CARD_SHOWN,
    FOCUS_COMPLETED,
    FOCUS_BLOCKED,
    SCHOOL_NOTICE,
    TASK_DONE,
    QUESTION_ANSWERED,
}

/**
 * What the app records. Deliberately no free text: a risk event keeps the category
 * ("SELF_HARM"), never what the child typed.
 */
data class Event(
    val timestampMillis: Long,
    val type: EventType,
    val category: String? = null,
    val value: Int = 0,
)

data class AppUsage(val label: String, val minutes: Int)
