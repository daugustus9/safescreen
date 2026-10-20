package com.safescreen.app.core

import org.json.JSONObject

/** One multiple-choice question from the study bank (ENEM now; OBMEP and BNCC-based later). */
data class Question(
    val id: String,
    val exam: String,
    val number: Int,
    val area: String,
    val areaName: String,
    /** Skill code from the exam's reference matrix, e.g. "H16". */
    val skill: String,
    /** Official difficulty (TRI parameter b); higher is harder. */
    val difficulty: Double,
    /**
     * Upper bounds of the easier and medium thirds of this area's exam. TRI scales differ by
     * area (b = 0.9 is hard in Humanas, easy in Matemática), so labels are per area.
     */
    val difficultyCuts: Pair<Double, Double>,
    val minAge: Int,
    val stem: String,
    /** Letter to text, in order. */
    val options: List<Pair<String, String>>,
    val answer: String,
    val explanation: String,
    val adaptation: String?,
    /** Human-readable source, e.g. "ENEM 2023, 2º dia, caderno 5 amarelo, questão 136". */
    val source: String,
)

/** A skill from an exam's reference matrix. Codes repeat across areas, so [key] includes the area. */
data class Skill(val area: String, val code: String, val description: String, val tip: String) {
    val key: String get() = skillKey(area, code)
}

fun skillKey(area: String, code: String) = "$area:$code"

data class Answer(val questionId: String, val chosen: String, val correct: Boolean, val timestampMillis: Long)

enum class Difficulty { EASIER, MEDIUM, HARDER }

object QuestionBank {

    /** Which third of its own area's exam the question falls in. */
    fun difficultyOf(question: Question): Difficulty = when {
        question.difficulty < question.difficultyCuts.first -> Difficulty.EASIER
        question.difficulty <= question.difficultyCuts.second -> Difficulty.MEDIUM
        else -> Difficulty.HARDER
    }

    fun parseQuestions(json: String): List<Question> {
        val root = JSONObject(json)
        val exam = root.getString("exam")
        val area = root.getString("area")
        val areaName = root.getString("areaName")
        val booklet = root.optString("booklet")
        val minAge = root.optInt("minAge", 13)
        val cuts = root.getJSONArray("difficultyCuts").let { it.getDouble(0) to it.getDouble(1) }
        val array = root.getJSONArray("questions")
        return (0 until array.length()).map { i ->
            val q = array.getJSONObject(i)
            val number = q.getInt("number")
            val options = q.getJSONObject("options")
            Question(
                id = "${exam.lowercase().replace(' ', '-')}-$area-$number".lowercase(),
                exam = exam,
                number = number,
                area = area,
                areaName = areaName,
                skill = q.getString("skill"),
                difficulty = q.getDouble("difficulty"),
                difficultyCuts = cuts,
                minAge = q.optInt("minAge", minAge),
                stem = q.getString("stem"),
                options = options.keys().asSequence().sorted().map { it to options.getString(it) }.toList(),
                answer = q.getString("answer"),
                explanation = q.getString("explanation"),
                adaptation = q.optString("adaptation").ifEmpty { null },
                source = listOf(exam, booklet, "questão $number").filter { it.isNotEmpty() }.joinToString(", "),
            )
        }
    }

    fun parseSkills(json: String): List<Skill> {
        val root = JSONObject(json)
        val area = root.getString("area")
        val array = root.getJSONArray("skills")
        return (0 until array.length()).map { i ->
            val s = array.getJSONObject(i)
            Skill(area, s.getString("code"), s.getString("description"), s.getString("tip"))
        }
    }
}

/**
 * Chooses the next question. Unanswered ones come first, from the area the child has
 * practiced least (so all subjects get turns), easiest first within that area so the
 * child builds confidence before the hard ones. After that, questions last answered
 * wrong come back, oldest mistake first (spaced practice).
 */
object QuestionPicker {

    fun next(questions: List<Question>, answers: List<Answer>, age: Int): Question? {
        val eligible = questions.filter { age >= it.minAge }
        val latest = latestAnswers(answers)
        val unanswered = eligible.filter { it.id !in latest }
        if (unanswered.isNotEmpty()) {
            val answeredPerArea = eligible.filter { it.id in latest }.groupingBy { it.area }.eachCount()
            val areaOrder = eligible.map { it.area }.distinct()
            val area = unanswered.map { it.area }.distinct()
                .minWith(compareBy<String> { answeredPerArea[it] ?: 0 }.thenBy { areaOrder.indexOf(it) })
            return unanswered.filter { it.area == area }.minBy { it.difficulty }
        }
        return eligible
            .filter { latest[it.id]?.correct == false }
            .minByOrNull { latest.getValue(it.id).timestampMillis }
    }

    fun latestAnswers(answers: List<Answer>): Map<String, Answer> =
        answers.groupBy { it.questionId }.mapValues { (_, list) -> list.maxBy { it.timestampMillis } }
}

/** [skill] is a [skillKey], e.g. "MT:H16". */
data class SkillFocus(val skill: String, val answered: Int, val correct: Int, val priority: Double)

data class FocusReport(
    val answered: Int,
    val correct: Int,
    /** False until there are enough answers for a suggestion to mean anything. */
    val enough: Boolean,
    /** Skills to review, most important first; empty when nothing stands out. */
    val focus: List<SkillFocus>,
)

/**
 * "Onde focar": which skills need work, from the latest answer to each question. A wrong
 * answer on an easy question weighs more than one on a hard question, using INEP's own
 * difficulty values. It suggests what to review; it does not predict an ENEM score.
 */
object FocusAnalysis {

    const val MIN_ANSWERS = 5
    private const val TOP = 3

    fun analyze(questions: List<Question>, answers: List<Answer>): FocusReport {
        val byId = questions.associateBy { it.id }
        val latest = QuestionPicker.latestAnswers(answers).values.filter { it.questionId in byId }
        val focus = latest
            .groupBy { byId.getValue(it.questionId).let { q -> skillKey(q.area, q.skill) } }
            .map { (skill, list) ->
                SkillFocus(
                    skill = skill,
                    answered = list.size,
                    correct = list.count { it.correct },
                    priority = list.filterNot { it.correct }.sumOf { missWeight(byId.getValue(it.questionId).difficulty) },
                )
            }
            .filter { it.priority > 0 }
            .sortedWith(compareByDescending<SkillFocus> { it.priority }.thenBy { it.skill })
            .take(TOP)
        return FocusReport(
            answered = latest.size,
            correct = latest.count { it.correct },
            enough = latest.size >= MIN_ANSWERS,
            focus = focus,
        )
    }

    /** 2.0 for the easiest questions down to 0.5 for the hardest. */
    fun missWeight(difficulty: Double): Double = (2.2 - difficulty).coerceIn(0.5, 2.0)
}
