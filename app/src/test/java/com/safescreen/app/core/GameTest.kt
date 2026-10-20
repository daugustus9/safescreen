package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class GameTest {

    private val areas = listOf("ch", "cn", "lc", "mt")
    private val questions = areas.flatMap {
        QuestionBank.parseQuestions(File("src/main/assets/questions/enem_2023_$it.json").readText())
    }
    private val skills = areas.flatMap {
        QuestionBank.parseSkills(File("src/main/assets/questions/enem_matriz_$it.json").readText())
    }
    private val oct20 = LocalDate.of(2026, 10, 20)

    @Test
    fun dailyChallengeIsTheSameForEveryoneOnADay() {
        val first = DailyChallenge.questionsFor(oct20, questions)
        assertEquals(first, DailyChallenge.questionsFor(oct20, questions.shuffled()))
        assertEquals(3, first.size)
        assertEquals("three different areas", 3, first.map { it.area }.toSet().size)
        assertEquals(20, DailyChallenge.number(oct20))
    }

    @Test
    fun dailyChallengeSkipsTextsForOlderTeensAndRotatesAreas() {
        val days = (0L until 120L).map { oct20.plusDays(it) }
        val picked = days.flatMap { DailyChallenge.questionsFor(it, questions) }
        assertTrue(picked.all { it.minAge <= 13 })
        // Each area rests one day in four.
        assertEquals(setOf(90), picked.groupingBy { it.area }.eachCount().values.toSet())
    }

    @Test
    fun noQuestionRepeatsBeforeTheAreaRunsOut() {
        // Each area appears 3 days in 4; its first n appearances use all n of its questions.
        for (area in listOf("CH", "CN", "LC", "MT")) {
            val pool = questions.count { it.area == area && it.minAge <= 13 }
            val appearances = (0L until 200L).map { oct20.plusDays(it) }
                .mapNotNull { day -> DailyChallenge.questionsFor(day, questions).firstOrNull { it.area == area } }
            assertEquals(area, pool, appearances.take(pool).map { it.id }.toSet().size)
        }
    }

    @Test
    fun sharedSquaresHoldNoAnswers() {
        assertEquals(listOf("🟩", "🟥", "🟩"), DailyChallenge.squares(listOf(true, false, true)))
    }

    @Test
    fun streakCountsDaysInARow() {
        val days = (0L..4L).map { oct20.minusDays(it) }.toSet() // Oct 16 to 20
        assertEquals(5, Streak.current(days, oct20))
        // Today not done yet: the streak still stands, counted to yesterday.
        assertEquals(4, Streak.current(days - oct20, oct20))
    }

    @Test
    fun streakForgivesOneDayOffPerWeek() {
        // Tuesday Oct 20; Monday Oct 19 missed: forgiven.
        val oneGap = setOf(oct20, oct20.minusDays(2), oct20.minusDays(3))
        assertEquals(3, Streak.current(oneGap, oct20))
        // Two days off in the same week (Sat Oct 24: missed Fri 23 and Wed 21): stops at the second.
        val oct24 = LocalDate.of(2026, 10, 24)
        val twoGaps = setOf(oct24, oct24.minusDays(2), oct24.minusDays(4))
        assertEquals(2, Streak.current(twoGaps, oct24))
        // Days off in different weeks are each forgiven (Mon Oct 19 and Sat Oct 17).
        assertEquals(3, Streak.current(setOf(oct20, oct20.minusDays(2), oct20.minusDays(4)), oct20))
        // Two days in a row missed: not a day off, the streak is over.
        assertEquals(0, Streak.current(setOf(oct20.minusDays(3)), oct20))
    }

    @Test
    fun albumStickersEarnedShinyAvailableAndComingSoon() {
        val mt173 = questions.first { it.id == "enem-2023-mt-173" } // H17, easy
        val mt166 = questions.first { it.id == "enem-2023-mt-166" } // H30, hard
        val mt136 = questions.first { it.id == "enem-2023-mt-136" } // H16, answered wrong
        val answers = listOf(
            Answer(mt173.id, mt173.answer, true, 1),
            Answer(mt166.id, mt166.answer, true, 2),
            Answer(mt136.id, "A", false, 3),
        )
        val stickers = Album.stickers(skills, questions, answers).associateBy { it.skill.key }
        assertEquals(120, stickers.size)
        assertEquals(StickerState.EARNED, stickers.getValue("MT:H17").state)
        assertEquals(StickerState.SHINY, stickers.getValue("MT:H30").state)
        assertEquals(StickerState.AVAILABLE, stickers.getValue("MT:H16").state)
        assertEquals(StickerState.COMING_SOON, stickers.getValue("MT:H1").state) // no H1 question yet
    }

    @Test
    fun practicingASkillPicksFromThatSkillOnly() {
        val q = Album.practiceQuestion("MT:H30", questions, emptyList(), age = 14)!!
        assertEquals("H30", q.skill)
        assertEquals("enem-2023-mt-169", q.id) // the easier of the two H30 questions
    }
}
