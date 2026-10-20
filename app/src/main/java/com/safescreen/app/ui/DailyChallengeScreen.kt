package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.Answer
import com.safescreen.app.core.DailyChallenge
import com.safescreen.app.core.Question
import com.safescreen.app.core.Streak
import com.safescreen.app.graph
import java.time.LocalDate

/**
 * Desafio do Dia: three questions, the same for every teen today. At the end the child can
 * share a card with colored squares only (no answers, nothing personal) with friends or family.
 */
@Composable
fun DailyChallengeScreen(onBack: () -> Unit, onAlbum: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val policy by graph.policy.policy.collectAsState()
    val allResults by graph.daily.results.collectAsState()
    val date = remember { LocalDate.now() }
    val number = DailyChallenge.number(date)
    val questions = remember { DailyChallenge.questionsFor(date, graph.content.questions) }
    val done = allResults[date].orEmpty()

    // The question on screen stays until the child taps Next, so the feedback can be read.
    var shown by remember { mutableStateOf(questions.firstOrNull { q -> done.none { it.first == q.id } }) }
    var selected by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<Answer?>(null) }

    ScreenScaffold(title = stringResource(R.string.daily_title), onBack = onBack) {
        val question = shown
        when {
            policy.ageTier.representativeAge < 13 || questions.isEmpty() ->
                Text(stringResource(R.string.study_not_for_age), style = MaterialTheme.typography.bodyLarge)
            question == null -> DailyResult(number, questions, done, graph.daily.doneDays(), date, onAlbum)
            else -> {
                Text(
                    stringResource(R.string.daily_header, number, questions.indexOf(question) + 1, questions.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                QuestionBody(question, selected, result) { if (result == null) selected = it }
                val answered = result
                if (answered == null) {
                    Button(
                        enabled = selected != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val chosen = selected ?: return@Button
                            val answer = recordAnswer(graph, question, chosen)
                            graph.daily.record(date, question.id, answer.correct)
                            result = answer
                        },
                    ) { Text(stringResource(R.string.card_answer)) }
                } else {
                    Feedback(question, answered)
                    val next = questions.firstOrNull { q -> graph.daily.results.value[date].orEmpty().none { it.first == q.id } }
                    Button(onClick = {
                        shown = next
                        selected = null
                        result = null
                    }) { Text(stringResource(if (next == null) R.string.daily_see_result else R.string.daily_next)) }
                }
            }
        }
    }
}

@Composable
private fun DailyResult(
    number: Int,
    questions: List<Question>,
    done: List<Pair<String, Boolean>>,
    doneDays: Set<LocalDate>,
    today: LocalDate,
    onAlbum: () -> Unit,
) {
    val context = LocalContext.current
    val results = questions.map { q -> done.firstOrNull { it.first == q.id }?.second == true }
    val squares = DailyChallenge.squares(results)
    val score = results.count { it }
    val streak = Streak.current(doneDays, today)

    val header = stringResource(R.string.daily_share_header, number)
    val lines = questions.mapIndexed { i, q -> "${squares[i]} ${q.areaName}" }
    val scoreLine = stringResource(R.string.daily_share_score, score, questions.size) +
        if (streak >= 2) " · " + stringResource(R.string.daily_share_streak, streak) else ""
    val shareText = (listOf(header) + lines + scoreLine + "#DesafioDoDia").joinToString("\n")
    val chooser = stringResource(R.string.share_chooser)

    SectionCard(stringResource(R.string.daily_share_header, number)) {
        lines.forEach { Text(it, style = MaterialTheme.typography.titleMedium) }
        Text(stringResource(R.string.daily_score, score, questions.size), style = MaterialTheme.typography.titleLarge)
        if (streak >= 1) {
            Text(pluralStringResource(R.plurals.daily_streak, streak, streak), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.daily_streak_note), style = MaterialTheme.typography.bodySmall)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { shareText(context, shareText, chooser) }) { Text(stringResource(R.string.daily_share)) }
        OutlinedButton(onClick = onAlbum) { Text(stringResource(R.string.daily_open_album)) }
    }
    Text(stringResource(R.string.daily_share_note), style = MaterialTheme.typography.bodySmall)
    Text(stringResource(R.string.daily_come_back, number + 1), style = MaterialTheme.typography.bodyMedium)
}
