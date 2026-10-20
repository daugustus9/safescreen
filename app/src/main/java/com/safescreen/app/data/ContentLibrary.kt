package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.Card
import com.safescreen.app.core.CardKind
import com.safescreen.app.core.Question
import com.safescreen.app.core.QuestionBank
import com.safescreen.app.core.RiskClassifier
import com.safescreen.app.core.SchoolTask
import com.safescreen.app.core.Skill
import org.json.JSONObject
import java.util.Locale

data class Helpline(val name: String, val number: String, val description: String)

/**
 * Loads the public message library, helplines and risk rules from assets. These files
 * are the "published message library" from the trust model: every card the app can show
 * is readable there in plain text.
 */
class ContentLibrary(private val context: Context) {

    /** Portuguese is the app's main language; English only on English-language phones. */
    val language: String = if (Locale.getDefault().language == "en") "en" else "pt"

    val cards: List<Card> by lazy { parseCards(readAsset("content/cards.json"), language) }

    val classifier: RiskClassifier by lazy {
        RiskClassifier(RiskClassifier.parseRules(readAsset("rules/risk_rules.json")))
    }

    /** Study questions: every bank file in assets/questions (reference matrices excluded). */
    val questions: List<Question> by lazy {
        questionFiles(matrix = false).flatMap { QuestionBank.parseQuestions(readAsset("questions/$it")) }
    }

    /** Skills by [Skill.key] ("MT:H16"): official description and a study tip. */
    val skills: Map<String, Skill> by lazy {
        questionFiles(matrix = true)
            .flatMap { QuestionBank.parseSkills(readAsset("questions/$it")) }
            .associateBy { it.key }
    }

    private fun questionFiles(matrix: Boolean): List<String> =
        context.assets.list("questions").orEmpty()
            .filter { it.endsWith(".json") && it.contains("_matriz_") == matrix }
            .sorted()

    fun studyInviteCard(): Card = if (language == "pt") {
        Card("study-invite", CardKind.STUDY, "Questão do ENEM de hoje", "Uma questão real do ENEM, cerca de 2 minutos. Toque em Responder quando puder.")
    } else {
        Card("study-invite", CardKind.STUDY, "Today's ENEM question", "A real ENEM question, about 2 minutes. Tap Answer when you can.")
    }

    fun card(id: String): Card? = cards.firstOrNull { it.id == id }

    fun helpCardFor(riskCategory: String): Card? =
        cards.firstOrNull { it.kind == CardKind.HELP && it.riskCategory == riskCategory }

    fun focusCard(): Card? = cards.firstOrNull { it.kind == CardKind.FOCUS }

    fun reminderCard(task: SchoolTask, tomorrow: Boolean): Card {
        val pt = language == "pt"
        val title = when {
            tomorrow && pt -> "Amanhã na escola"
            tomorrow -> "Tomorrow at school"
            pt -> "Hoje na escola"
            else -> "Today at school"
        }
        return Card(id = "reminder-${task.id}", kind = CardKind.SCHOOL, title = title, body = task.title)
    }

    /** Family-written encouragement as cards. Ids follow the text, so edits rotate as new cards. */
    fun familyCards(messages: List<String>): List<Card> {
        val title = if (language == "pt") "Da sua família" else "From your family"
        return messages.map { text ->
            Card(id = "family-${text.hashCode().toUInt()}", kind = CardKind.ENCOURAGEMENT, title = title, body = text)
        }
    }

    fun helplines(): List<Helpline> {
        val root = JSONObject(readAsset("content/helplines.json"))
        val country = Locale.getDefault().country
        val list = root.optJSONArray(country) ?: root.getJSONArray("default")
        return (0 until list.length()).map { i ->
            val entry = list.getJSONObject(i)
            val text = entry.optJSONObject(language) ?: entry.getJSONObject("en")
            Helpline(text.getString("name"), entry.getString("number"), text.getString("description"))
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        fun parseCards(json: String, language: String): List<Card> {
            val array = JSONObject(json).getJSONArray("cards")
            return (0 until array.length()).mapNotNull { i ->
                val entry = array.getJSONObject(i)
                val kind = runCatching { CardKind.valueOf(entry.getString("kind")) }.getOrNull()
                    ?: return@mapNotNull null
                val text = entry.optJSONObject(language) ?: entry.getJSONObject("en")
                Card(
                    id = entry.getString("id"),
                    kind = kind,
                    title = text.getString("title"),
                    body = text.getString("body"),
                    minAge = entry.optInt("minAge", 0),
                    maxAge = entry.optInt("maxAge", 99),
                    riskCategory = entry.optString("riskCategory").ifEmpty { null },
                )
            }
        }
    }
}
