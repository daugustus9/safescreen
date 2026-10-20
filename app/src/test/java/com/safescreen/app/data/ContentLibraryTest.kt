package com.safescreen.app.data

import com.safescreen.app.core.CardKind
import com.safescreen.app.core.RiskCategory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Checks the published message library itself. */
class ContentLibraryTest {

    private val json = File("src/main/assets/content/cards.json").readText()

    @Test
    fun everyCardHasEnglishAndPortuguese() {
        val array = JSONObject(json).getJSONArray("cards")
        for (i in 0 until array.length()) {
            val card = array.getJSONObject(i)
            for (lang in listOf("en", "pt")) {
                val text = card.getJSONObject(lang)
                assertTrue("${card.getString("id")} $lang title", text.getString("title").isNotBlank())
                assertTrue("${card.getString("id")} $lang body", text.getString("body").isNotBlank())
            }
        }
    }

    @Test
    fun parsesBothLanguagesWithUniqueIds() {
        val en = ContentLibrary.parseCards(json, "en")
        val pt = ContentLibrary.parseCards(json, "pt")
        assertEquals(en.size, pt.size)
        assertEquals(en.size, en.map { it.id }.toSet().size)
        assertTrue(pt.any { it.kind == CardKind.BEDTIME && it.title == "Hora de desacelerar" })
        assertTrue(en.any { it.kind == CardKind.BEDTIME && it.title == "Time to wind down" })
    }

    @Test
    fun everyRiskCategoryHasAHelpCard() {
        val cards = ContentLibrary.parseCards(json, "pt")
        RiskCategory.entries.forEach { category ->
            assertNotNull(category.name, cards.firstOrNull { it.kind == CardKind.HELP && it.riskCategory == category.name })
        }
    }

    @Test
    fun everyAgeHasSafetyAndAdviceCards() {
        val cards = ContentLibrary.parseCards(json, "en")
        for (age in listOf(9, 11, 14)) {
            assertTrue(cards.any { it.kind == CardKind.SAFETY && age in it.minAge..it.maxAge })
            assertTrue(cards.any { it.kind == CardKind.ADVICE && age in it.minAge..it.maxAge })
            assertTrue(cards.count { it.kind == CardKind.ENCOURAGEMENT && age in it.minAge..it.maxAge } >= 10)
        }
    }
}
