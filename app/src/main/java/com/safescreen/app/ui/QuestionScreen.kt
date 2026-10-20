package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.Answer
import com.safescreen.app.core.Difficulty
import com.safescreen.app.core.EventType
import com.safescreen.app.core.Question
import com.safescreen.app.core.QuestionBank
import com.safescreen.app.core.QuestionPicker
import com.safescreen.app.core.skillKey
import com.safescreen.app.graph

/**
 * One real exam question at a time, opened by the child (never forced). After answering,
 * the child sees the right answer, how to solve it, the skill it tests and its source.
 */
@Composable
fun QuestionScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val policy by graph.policy.policy.collectAsState()
    val answers by graph.answers.answers.collectAsState()
    val age = policy.ageTier.representativeAge
    val questions = graph.content.questions

    // Picked once per question, so recording an answer doesn't swap the question away.
    var current by remember { mutableStateOf(QuestionPicker.next(questions, answers, age)) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Answer?>(null) }

    ScreenScaffold(title = stringResource(R.string.study_title), onBack = onBack) {
        val question = current
        when {
            questions.none { age >= it.minAge } ->
                Text(stringResource(R.string.study_not_for_age), style = MaterialTheme.typography.bodyLarge)
            question == null ->
                Text(stringResource(R.string.study_done), style = MaterialTheme.typography.bodyLarge)
            else -> {
                QuestionBody(question, selected, result) { if (result == null) selected = it }
                val answered = result
                if (answered == null) {
                    Button(
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val chosen = selected ?: return@Button
                            val answer = Answer(question.id, chosen, chosen == question.answer, System.currentTimeMillis())
                            graph.answers.add(answer)
                            graph.events.add(
                                EventType.QUESTION_ANSWERED,
                                category = skillKey(question.area, question.skill),
                                value = if (answer.correct) 1 else 0,
                            )
                            result = answer
                        },
                    ) { Text(stringResource(R.string.card_answer)) }
                } else {
                    Feedback(question, answered)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            current = QuestionPicker.next(questions, graph.answers.answers.value, age)
                            selected = null
                            result = null
                        }) { Text(stringResource(R.string.study_next)) }
                        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionBody(question: Question, selected: String?, result: Answer?, onSelect: (String) -> Unit) {
    val difficulty = stringResource(
        when (QuestionBank.difficultyOf(question)) {
            Difficulty.EASIER -> R.string.difficulty_easier
            Difficulty.MEDIUM -> R.string.difficulty_medium
            Difficulty.HARDER -> R.string.difficulty_harder
        },
    )
    Text(
        stringResource(R.string.study_header, question.exam, question.number, question.areaName),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(stringResource(R.string.study_difficulty, difficulty), style = MaterialTheme.typography.bodySmall)
    Text(question.stem, style = MaterialTheme.typography.bodyLarge)

    question.options.forEach { (letter, text) ->
        val colors = when {
            result != null && letter == question.answer ->
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            result != null && letter == result.chosen ->
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            letter == selected ->
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            else -> CardDefaults.cardColors()
        }
        Card(
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = letter == selected, enabled = result == null, onClick = { onSelect(letter) }),
        ) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(letter, fontWeight = FontWeight.Bold)
                Text(text)
            }
        }
    }
}

@Composable
private fun Feedback(question: Question, answer: Answer) {
    val graph = LocalContext.current.graph
    val skill = graph.content.skills[skillKey(question.area, question.skill)]
    SectionCard {
        Text(
            if (answer.correct) stringResource(R.string.study_correct)
            else stringResource(R.string.study_wrong, question.answer),
            style = MaterialTheme.typography.titleMedium,
            color = if (answer.correct) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
    SectionCard(stringResource(R.string.study_explanation)) {
        Text(question.explanation, style = MaterialTheme.typography.bodyMedium)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (skill != null) {
                Text(stringResource(R.string.study_skill, skill.code, skill.description), style = MaterialTheme.typography.bodySmall)
            }
            Text(stringResource(R.string.study_source, question.source), style = MaterialTheme.typography.bodySmall)
            question.adaptation?.let { Text(stringResource(R.string.study_adaptation, it), style = MaterialTheme.typography.bodySmall) }
            Text(stringResource(R.string.study_pending_review), style = MaterialTheme.typography.bodySmall)
        }
    }
}
