package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.LocalTime

class StudyTest {

    private val questions = QuestionBank.parseQuestions(File("src/main/assets/questions/enem_2023_mt.json").readText())
    private val skills = QuestionBank.parseSkills(File("src/main/assets/questions/enem_matriz_mt.json").readText())
    private val byNumber = questions.associateBy { it.number }

    private fun answer(number: Int, correct: Boolean, at: Long): Answer {
        val q = byNumber.getValue(number)
        val chosen = if (correct) q.answer else q.options.first { it.first != q.answer }.first
        return Answer(q.id, chosen, correct, at)
    }

    @Test
    fun bankIsCompleteAndConsistent() {
        assertEquals(16, questions.size)
        assertEquals(30, skills.size)
        val skillKeys = skills.map { it.key }.toSet()
        questions.forEach { q ->
            assertEquals("${q.number} options", listOf("A", "B", "C", "D", "E"), q.options.map { it.first })
            assertTrue("${q.number} answer", q.answer in q.options.map { it.first })
            assertTrue("${q.number} skill", skillKey(q.area, q.skill) in skillKeys)
            assertTrue("${q.number} explanation", q.explanation.length > 20)
            assertTrue("${q.number} id", q.id == "enem-2023-mt-${q.number}")
        }
        assertEquals("ENEM 2023, 2º dia, caderno 5 amarelo, questão 136", byNumber.getValue(136).source)
    }

    @Test
    fun startsWithTheEasiestUnansweredQuestion() {
        // Question 173 has the lowest INEP difficulty (b = 0.14), then 169 (0.74).
        assertEquals(173, QuestionPicker.next(questions, emptyList(), age = 14)!!.number)
        assertEquals(169, QuestionPicker.next(questions, listOf(answer(173, true, 1)), age = 14)!!.number)
    }

    @Test
    fun onlyForTheRightAge() {
        assertNull(QuestionPicker.next(questions, emptyList(), age = 11))
    }

    @Test
    fun afterEverythingRetriesOldestMistakeFirst() {
        val allRight = questions.mapIndexed { i, q -> answer(q.number, true, i.toLong()) }
        assertNull(QuestionPicker.next(questions, allRight, age = 14))

        val twoWrong = allRight.map {
            when (it.questionId) {
                byNumber.getValue(150).id -> answer(150, false, 100)
                byNumber.getValue(141).id -> answer(141, false, 50)
                else -> it
            }
        }
        assertEquals(141, QuestionPicker.next(questions, twoWrong, age = 14)!!.number)
        // Answering 141 right later takes it off the list.
        assertEquals(150, QuestionPicker.next(questions, twoWrong + answer(141, true, 200), age = 14)!!.number)
    }

    @Test
    fun focusNeedsEnoughAnswers() {
        val report = FocusAnalysis.analyze(questions, listOf(answer(173, false, 1), answer(169, false, 2)))
        assertFalse(report.enough)
        assertEquals(2, report.answered)
    }

    @Test
    fun focusWeighsMistakesOnEasyQuestionsMore() {
        val answers = listOf(
            answer(173, false, 1), // H17, b = 0.14: an easy miss
            answer(166, false, 2), // H30, b = 2.92: a hard miss
            answer(169, true, 3), // H30, right
            answer(171, true, 4), // H19, right
            answer(161, true, 5), // H19, right
        )
        val report = FocusAnalysis.analyze(questions, answers)
        assertTrue(report.enough)
        assertEquals(listOf("MT:H17", "MT:H30"), report.focus.map { it.skill })
        assertEquals(SkillFocus("MT:H30", answered = 2, correct = 1, priority = 0.5), report.focus[1])
    }

    @Test
    fun onlyTheLatestAnswerToEachQuestionCounts() {
        val answers = listOf(answer(173, false, 1), answer(173, true, 2)) +
            listOf(169, 171, 161, 136).mapIndexed { i, n -> answer(n, true, 10L + i) }
        val report = FocusAnalysis.analyze(questions, answers)
        assertEquals(5, report.answered)
        assertTrue(report.focus.isEmpty())
    }

    @Test
    fun studyInviteShowsOnceADayButNotAtBedtime() {
        val invite = Card("study-invite", CardKind.STUDY, "Questão do ENEM de hoje", "…")
        val scheduler = CardScheduler(listOf(Card("safety-a", CardKind.SAFETY, "S", "s"))) { task, _ ->
            Card("reminder-${task.id}", CardKind.SCHOOL, "R", task.title)
        }
        fun state(now: LocalDateTime, shown: Set<String> = emptySet()) = SchedulerState(
            now = now, tier = AgeTier.TEEN_13_PLUS, intervalMinutes = 30,
            bedtimeStart = LocalTime.of(21, 30), bedtimeEnd = LocalTime.of(7, 0),
            lastCardAt = null, recentCardIds = emptyList(), tasks = emptyList(),
            shownReminderKeys = shown, studyInvite = invite,
        )
        val afternoon = LocalDateTime.of(2026, 10, 20, 16, 0)
        val first = scheduler.next(state(afternoon))!!
        assertEquals(CardKind.STUDY, first.card.kind)
        assertEquals("study:2026-10-20", first.reminderKey)
        assertEquals(CardKind.SAFETY, scheduler.next(state(afternoon, setOf("study:2026-10-20")))!!.card.kind)
        // At bedtime only bedtime cards are shown (none in this test library), never the invitation.
        assertNull(scheduler.next(state(LocalDateTime.of(2026, 10, 20, 22, 0))))
    }

    @Test
    fun missWeightRange() {
        assertEquals(2.0, FocusAnalysis.missWeight(-0.3), 1e-9)
        assertEquals(0.5, FocusAnalysis.missWeight(3.0), 1e-9)
        assertEquals(1.0, FocusAnalysis.missWeight(1.2), 1e-9)
    }
}
