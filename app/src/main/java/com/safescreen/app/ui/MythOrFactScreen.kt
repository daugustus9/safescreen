package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.Claim
import com.safescreen.app.core.MythOrFact
import com.safescreen.app.core.Verdict
import com.safescreen.app.graph
import com.safescreen.app.system.Share

/**
 * Mito ou Verdade?: five claims per round, answered Verdade, Mito or Depende, each followed
 * by its explanation. No timer, no ranking; the result can be shared as squares only.
 */
@Composable
fun MythOrFactScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val age = graph.policy.current.ageTier.representativeAge

    var roundNumber by remember { mutableIntStateOf(0) }
    val round = remember(roundNumber) { MythOrFact.round(graph.content.claims, graph.myth.results.value, age) }
    var index by remember(roundNumber) { mutableIntStateOf(0) }
    var chosen by remember(roundNumber) { mutableStateOf<Verdict?>(null) }
    val outcomes = remember(roundNumber) { mutableStateListOf<Boolean>() }

    ScreenScaffold(title = stringResource(R.string.myth_title), onBack = onBack) {
        val claim = round.getOrNull(index)
        if (claim == null) {
            RoundResult(outcomes, onPlayAgain = { roundNumber++ }, onBack = onBack)
            return@ScreenScaffold
        }
        Text(stringResource(R.string.myth_header, index + 1, round.size), style = MaterialTheme.typography.titleSmall)
        val combo = outcomes.takeLastWhile { it }.size
        if (combo >= 2) Text(stringResource(R.string.myth_combo, combo), style = MaterialTheme.typography.titleSmall)

        SectionCard {
            Text("“${claim.text}”", style = MaterialTheme.typography.headlineSmall)
        }

        val answer = chosen
        if (answer == null) {
            Verdict.entries.forEach { verdict ->
                Button(
                    onClick = {
                        chosen = verdict
                        val correct = verdict == claim.verdict
                        outcomes += correct
                        graph.myth.record(claim.id, correct)
                        if (outcomes.size == round.size) graph.myth.finishRound(outcomes.count { it })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = verdictColor(verdict)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) { Text(verdictLabel(verdict), style = MaterialTheme.typography.titleMedium) }
            }
        } else {
            Feedback(claim, answer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { index++; chosen = null }) {
                    Text(stringResource(if (index + 1 < round.size) R.string.daily_next else R.string.daily_see_result))
                }
                val shareText = stringResource(R.string.myth_share_claim, claim.text) + "\n\n" + stringResource(R.string.share_footer)
                OutlinedButton(onClick = { Share.send(context, shareText) }) {
                    Text(stringResource(R.string.share_question_button))
                }
            }
        }
    }
}

@Composable
private fun Feedback(claim: Claim, answer: Verdict) {
    val graph = LocalContext.current.graph
    val right = answer == claim.verdict
    SectionCard {
        Text(
            if (right) stringResource(R.string.myth_right) else stringResource(R.string.myth_wrong, verdictLabel(claim.verdict)),
            style = MaterialTheme.typography.titleMedium,
            color = if (right) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
        Text(claim.explanation, style = MaterialTheme.typography.bodyMedium)
        val hint = claim.hintId?.let { id -> graph.content.hints.firstOrNull { it.id == id } }
        if (hint != null) {
            Text(stringResource(R.string.myth_learn_more, hint.title), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RoundResult(outcomes: List<Boolean>, onPlayAgain: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val score = outcomes.count { it }
    val squares = MythOrFact.squares(outcomes)
    val shareText = stringResource(R.string.myth_share_result, squares, score, outcomes.size)

    SectionCard {
        Text(squares, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.daily_score, score, outcomes.size), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.myth_best, graph.myth.bestRound, MythOrFact.ROUND), style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onPlayAgain) { Text(stringResource(R.string.myth_play_again)) }
        OutlinedButton(onClick = { Share.send(context, shareText) }) { Text(stringResource(R.string.daily_share)) }
    }
    Text(stringResource(R.string.daily_share_note), style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
}

@Composable
private fun verdictLabel(verdict: Verdict): String = stringResource(
    when (verdict) {
        Verdict.TRUE -> R.string.verdict_true
        Verdict.FALSE -> R.string.verdict_false
        Verdict.DEPENDS -> R.string.verdict_depends
    },
)

private fun verdictColor(verdict: Verdict): Color = when (verdict) {
    Verdict.TRUE -> Color(0xFF2E7D32)
    Verdict.FALSE -> Color(0xFFB3261E)
    Verdict.DEPENDS -> Color(0xFF9C4F00)
}
