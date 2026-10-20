package com.safescreen.app.core

import java.time.DayOfWeek
import java.time.LocalDate

data class ParsedNotice(val kind: TaskKind, val title: String, val dueDate: LocalDate?)

/**
 * Turns a school-app notification (Agenda Edu, Escola Paraná, ClassApp...) into a task
 * (C2). Works on wording, not on any one app's format, so no partnership is needed.
 * Portuguese and English.
 */
object SchoolNoticeParser {

    private const val MAX_TITLE = 140

    // Checked in order: a notice about signing a permission slip for a trip is a
    // PERMISSION, not an EVENT.
    private val kindRules: List<Pair<TaskKind, Regex>> = listOf(
        TaskKind.PERMISSION to Regex("\\b(autorizacao|autorizar|assinar|assinatura|permission slip|consent form|sign the)\\b"),
        TaskKind.TEST to Regex("\\b(prova|provas|avaliacao|simulado|teste|test|exam|quiz)\\b"),
        TaskKind.HOMEWORK to Regex("\\b(tarefa|tarefas|licao|dever de casa|para casa|homework|assignment|trabalho)\\b"),
        TaskKind.MATERIALS to Regex("\\b(trazer|levar|material|materiais|bring)\\b"),
        TaskKind.EVENT to Regex("\\b(passeio|excursao|reuniao|festa|feira|apresentacao|field trip|meeting|fair|event)\\b"),
    )

    private val weekdays: Map<String, DayOfWeek> = mapOf(
        "segunda" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY,
        "terca" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY,
        "quarta" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
        "quinta" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY,
        "sexta" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY,
        "sabado" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY,
    )
    private val weekdayPattern = Regex("\\b(${weekdays.keys.joinToString("|")})\\b")
    private val numericDate = Regex("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b")
    private val dayOfMonth = Regex("\\bdia (\\d{1,2})\\b")

    fun parse(title: String?, text: String?, today: LocalDate): ParsedNotice? {
        val original = listOfNotNull(title?.trim(), text?.trim())
            .filter { it.isNotEmpty() }
            .joinToString(": ")
        if (original.isEmpty()) return null
        val normalized = TextNormalizer.normalize(original)
        val kind = kindRules.firstOrNull { (_, regex) -> regex.containsMatchIn(normalized) }?.first
            ?: return null
        return ParsedNotice(kind, original.take(MAX_TITLE), extractDate(normalized, today))
    }

    fun extractDate(normalized: String, today: LocalDate): LocalDate? {
        when {
            "depois de amanha" in normalized || "day after tomorrow" in normalized -> return today.plusDays(2)
            Regex("\\b(amanha|tomorrow)\\b").containsMatchIn(normalized) -> return today.plusDays(1)
            Regex("\\b(hoje|today|tonight)\\b").containsMatchIn(normalized) -> return today
        }
        numericDate.find(normalized)?.let { match ->
            val day = match.groupValues[1].toInt()
            val month = match.groupValues[2].toInt()
            val yearText = match.groupValues[3]
            val explicitYear = when (yearText.length) {
                2 -> 2000 + yearText.toInt()
                4 -> yearText.toInt()
                else -> null
            }
            val date = runCatching { LocalDate.of(explicitYear ?: today.year, month, day) }.getOrNull()
            if (date != null) {
                return if (explicitYear == null && date.isBefore(today)) date.plusYears(1) else date
            }
        }
        dayOfMonth.find(normalized)?.let { match ->
            val day = match.groupValues[1].toInt()
            val thisMonth = runCatching { today.withDayOfMonth(day) }.getOrNull()
            if (thisMonth != null && !thisMonth.isBefore(today)) return thisMonth
            return runCatching { today.plusMonths(1).withDayOfMonth(day) }.getOrNull()
        }
        weekdayPattern.find(normalized)?.let { match ->
            val target = weekdays.getValue(match.groupValues[1])
            var date = today.plusDays(1)
            while (date.dayOfWeek != target) date = date.plusDays(1)
            return date
        }
        return null
    }
}
