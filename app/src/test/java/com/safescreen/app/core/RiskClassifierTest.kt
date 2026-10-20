package com.safescreen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/** Runs the published rules file, so a bad edit to the rules fails the build. */
class RiskClassifierTest {

    private val classifier = RiskClassifier(
        RiskClassifier.parseRules(File("src/main/assets/rules/risk_rules.json").readText()),
    )

    private fun category(text: String) = classifier.classify(text)?.category

    @Test
    fun detectsSelfHarmInPortugueseWithOrWithoutAccents() {
        assertEquals(RiskCategory.SELF_HARM, category("eu não quero mais viver"))
        assertEquals(RiskCategory.SELF_HARM, category("EU NAO QUERO MAIS VIVER"))
        assertEquals(RiskCategory.SELF_HARM, category("como me cortar sem ninguém ver"))
    }

    @Test
    fun detectsSelfHarmInEnglish() {
        assertEquals(RiskCategory.SELF_HARM, category("I want to die"))
        assertEquals(RiskCategory.SELF_HARM, category("ways to hurt myself"))
    }

    @Test
    fun detectsDangerousChallenges() {
        assertEquals(RiskCategory.DANGEROUS_CHALLENGE, category("desafio do desmaio tiktok"))
        assertEquals(RiskCategory.DANGEROUS_CHALLENGE, category("blackout challenge"))
        assertEquals(RiskCategory.DANGEROUS_CHALLENGE, category("vídeo quebra-crânio"))
    }

    @Test
    fun detectsDrugsAndWeapons() {
        assertEquals(RiskCategory.DRUGS, category("onde comprar lança-perfume"))
        assertEquals(RiskCategory.WEAPONS, category("como fazer uma bomba caseira"))
    }

    @Test
    fun picksTheMostSevereCategoryWhenSeveralMatch() {
        // Challenge (severity 2) and self-harm (severity 3) in one search.
        assertEquals(RiskCategory.SELF_HARM, category("blackout challenge quero morrer"))
    }

    @Test
    fun ignoresOrdinaryText() {
        assertNull(category("matemática para amanhã"))
        assertNull(category("challenge accepted: 100 push-ups"))
        assertNull(category("vou matar a saudade da vovó"))
        assertNull(category("ok"))
    }
}
