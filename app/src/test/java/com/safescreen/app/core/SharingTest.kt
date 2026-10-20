package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SharingTest {

    private val texts = Sharing.Texts(
        questionIntro = "Você acerta esta questão do ENEM? 🤔",
        questionOutro = "Me responde com a letra!",
        questionSource = "Fonte: %s (INEP).",
        safetyIntro = "⚠️ Olha que importante:",
        adviceIntro = "💡 Uma dica para a vida:",
        encouragementIntro = "✨ Pra você:",
        footer = "Enviado pelo SafeScreen.",
    )
    private val question = QuestionBank.parseQuestions(File("src/main/assets/questions/enem_2023_mt.json").readText())
        .first { it.number == 173 }

    @Test
    fun sharedQuestionHasEveryOptionAndTheSourceButNeverTheAnswer() {
        val text = Sharing.question(question, texts)
        question.options.forEach { (letter, option) -> assertTrue(text.contains("$letter) $option")) }
        assertTrue(text.contains("Fonte: ENEM 2023, 2º dia, caderno 5 amarelo, questão 173 (INEP)."))
        assertTrue(text.endsWith("Enviado pelo SafeScreen."))
        assertFalse("no explanation", text.contains(question.explanation))
        assertFalse("no answer key", text.contains("Resposta") || text.contains("Gabarito"))
    }

    @Test
    fun onlyLibraryCardsAreShareable() {
        assertTrue(Sharing.isShareable(Card("safety-blackout", CardKind.SAFETY, "T", "B")))
        assertTrue(Sharing.isShareable(Card("advice-no", CardKind.ADVICE, "T", "B")))
        assertTrue(Sharing.isShareable(Card("enc-learn", CardKind.ENCOURAGEMENT, "T", "B")))
        // Private or personal: family messages, school reminders, help, bedtime, focus, the study invite.
        assertFalse(Sharing.isShareable(Card("family-123", CardKind.ENCOURAGEMENT, "Da sua família", "B")))
        listOf(CardKind.SCHOOL, CardKind.HELP, CardKind.BEDTIME, CardKind.FOCUS, CardKind.STUDY).forEach {
            assertFalse(it.name, Sharing.isShareable(Card("x", it, "T", "B")))
        }
    }

    @Test
    fun sharedCardNamesTheKindAndTheApp() {
        val text = Sharing.card(Card("safety-scam", CardKind.SAFETY, "Skin grátis é armadilha", "Jogos nunca pedem a sua senha."), texts)
        assertEquals(
            "⚠️ Olha que importante:\n\n*Skin grátis é armadilha*\nJogos nunca pedem a sua senha.\n\nEnviado pelo SafeScreen.",
            text,
        )
    }
}
