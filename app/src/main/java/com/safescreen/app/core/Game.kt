package com.safescreen.app.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * "Desafio do Dia", in the style of Wordle: everyone gets the same three questions each day,
 * chosen only from the date, so friends can compare without any server. Sharing sends a grid
 * of colored squares, never the answers or anything personal.
 */
object DailyChallenge {

    /** Day #1 of the challenge. */
    val FIRST_DAY: LocalDate = LocalDate.of(2026, 10, 1)
    const val SIZE = 3
    private val STEPS = listOf(11L, 13L, 17L, 19L, 23L, 29L, 31L, 37L)

    fun number(date: LocalDate): Int = ChronoUnit.DAYS.between(FIRST_DAY, date).toInt() + 1

    /**
     * The day's questions: one from each of three areas, the fourth area resting in turn.
     * Only questions open to every teen (min age 13) are used, so the set is the same for all.
     */
    fun questionsFor(date: LocalDate, bank: List<Question>): List<Question> {
        val pool = bank.filter { it.minAge <= 13 }.groupBy { it.area }.toSortedMap()
        if (pool.isEmpty()) return emptyList()
        val areas = pool.keys.toList()
        val m = areas.size.toLong()
        val day = ChronoUnit.DAYS.between(FIRST_DAY, date)
        val resting = if (areas.size > SIZE) Math.floorMod(day, m).toInt() else -1
        return areas.withIndex().filter { it.index != resting }.take(SIZE).map { (a, area) ->
            val questions = pool.getValue(area).sortedBy { it.number }
            val n = questions.size.toLong()
            // Count this area's own appearances (it rests one day in m), and step through its
            // questions by a number coprime with n: every question comes once before any repeats.
            val restsBefore = if (resting < 0) 0L else Math.floorDiv(day - a - 1, m) + 1
            val appearance = day - restsBefore
            val step = STEPS.firstOrNull { gcd(it, n) == 1L } ?: 1L
            questions[Math.floorMod(appearance * step + a * 7L, n).toInt()]
        }
    }

    /** 🟩 for right, 🟥 for wrong, in question order. */
    fun squares(results: List<Boolean>): List<String> = results.map { if (it) "🟩" else "🟥" }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
}

/**
 * Days in a row with the challenge done. Forgiving on purpose: one missed day per week
 * (Monday to Sunday) doesn't break it, so a streak never becomes a source of anxiety.
 * Today counts only once it's done; until then the streak runs to yesterday.
 */
object Streak {

    fun current(doneDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in doneDays) today else today.minusDays(1)
        var count = 0
        val weeksWithDayOff = mutableSetOf<LocalDate>()
        while (true) {
            if (day in doneDays) {
                count++
                day = day.minusDays(1)
                continue
            }
            val week = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            if (day.minusDays(1) in doneDays && week !in weeksWithDayOff) {
                weeksWithDayOff += week
                day = day.minusDays(1)
                continue
            }
            return count
        }
    }
}

enum class StickerState { EARNED, SHINY, AVAILABLE, COMING_SOON }

data class Sticker(val skill: Skill, val state: StickerState)

/**
 * "Álbum de Habilidades", in the style of a sticker album: one sticker per ENEM skill.
 * A right answer earns it for good; a right answer on a question from the hardest third of
 * its area makes it shiny. Skills with no question in the bank yet are "coming soon".
 */
object Album {

    fun stickers(skills: List<Skill>, questions: List<Question>, answers: List<Answer>): List<Sticker> {
        val byId = questions.associateBy { it.id }
        val rightAnswers = answers.filter { it.correct }.mapNotNull { byId[it.questionId] }
        val earned = rightAnswers.map { skillKey(it.area, it.skill) }.toSet()
        val shiny = rightAnswers
            .filter { QuestionBank.difficultyOf(it) == Difficulty.HARDER }
            .map { skillKey(it.area, it.skill) }.toSet()
        val available = questions.map { skillKey(it.area, it.skill) }.toSet()
        return skills.map { skill ->
            Sticker(
                skill,
                when (skill.key) {
                    in shiny -> StickerState.SHINY
                    in earned -> StickerState.EARNED
                    in available -> StickerState.AVAILABLE
                    else -> StickerState.COMING_SOON
                },
            )
        }
    }

    /** Next question to practice one skill: unanswered first (easiest), then past mistakes, then any. */
    fun practiceQuestion(skillKey: String, questions: List<Question>, answers: List<Answer>, age: Int): Question? {
        val inSkill = questions.filter { skillKey(it.area, it.skill) == skillKey && age >= it.minAge }
        return QuestionPicker.next(inSkill, answers, age) ?: inSkill.minByOrNull { it.difficulty }
    }
}
