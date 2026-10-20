package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SchoolNoticeParserTest {

    // A Monday.
    private val today = LocalDate.of(2026, 10, 19)

    private fun parse(text: String, title: String? = null) = SchoolNoticeParser.parse(title, text, today)

    @Test
    fun readsATestTomorrow() {
        val notice = parse("Prova de matemática amanhã, capítulos 3 e 4", title = "Agenda 7º B")!!
        assertEquals(TaskKind.TEST, notice.kind)
        assertEquals(today.plusDays(1), notice.dueDate)
        assertEquals("Agenda 7º B: Prova de matemática amanhã, capítulos 3 e 4", notice.title)
    }

    @Test
    fun permissionSlipWinsOverTheEventItIsFor() {
        val notice = parse("Assinar a autorização para o passeio ao museu até sexta-feira")!!
        assertEquals(TaskKind.PERMISSION, notice.kind)
        assertEquals(LocalDate.of(2026, 10, 23), notice.dueDate)
    }

    @Test
    fun readsNumericDates() {
        assertEquals(LocalDate.of(2026, 10, 25), parse("Trabalho de ciências para 25/10")!!.dueDate)
        assertEquals(LocalDate.of(2027, 3, 2), parse("Simulado em 02/03")!!.dueDate) // past date → next year
        assertEquals(LocalDate.of(2026, 11, 5), parse("Feira de ciências 05/11/2026")!!.dueDate)
    }

    @Test
    fun readsDayOfMonthAndWeekdays() {
        assertEquals(LocalDate.of(2026, 10, 30), parse("Reunião de pais dia 30")!!.dueDate)
        assertEquals(LocalDate.of(2026, 11, 10), parse("Prova dia 10")!!.dueDate)
        // "Monday" said on a Monday means next week.
        assertEquals(LocalDate.of(2026, 10, 26), parse("Homework due Monday")!!.dueDate)
    }

    @Test
    fun readsEnglishNotices() {
        val notice = parse("Bring your art materials tomorrow")!!
        assertEquals(TaskKind.MATERIALS, notice.kind)
        assertEquals(today.plusDays(1), notice.dueDate)
    }

    @Test
    fun keepsNoticesWithoutADate() {
        val notice = parse("Nova tarefa de português publicada")!!
        assertEquals(TaskKind.HOMEWORK, notice.kind)
        assertNull(notice.dueDate)
    }

    @Test
    fun ignoresNoticesThatAreNotTasks() {
        assertNull(parse("Bom dia, famílias! Lembrem-se de beber água."))
        assertNull(parse("", title = null))
    }
}
