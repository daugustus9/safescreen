package com.safescreen.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.safescreen.app.R
import com.safescreen.app.core.DailyChallenge
import com.safescreen.app.core.DailyHint
import com.safescreen.app.core.Hint
import com.safescreen.app.core.Sharing
import com.safescreen.app.graph
import com.safescreen.app.system.Share
import java.time.LocalDate

/** Home-screen section: today's Dica do Dia, with a button to pass it on. */
@Composable
fun HintOfTheDay(age: Int) {
    val graph = LocalContext.current.graph
    val today = LocalDate.now()
    val hint = DailyHint.forDate(today, graph.content.hints, age) ?: return
    SectionCard(stringResource(R.string.hint_today, DailyChallenge.number(today))) {
        HintBody(hint, graph.content.hintCategories[hint.category], graph.content.hintTitlePrefix)
    }
}

@Composable
fun HintBody(hint: Hint, category: String?, titlePrefix: String) {
    val context = LocalContext.current
    if (category != null) {
        Text(category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
    Text(hint.title, style = MaterialTheme.typography.titleMedium)
    Text(hint.body, style = MaterialTheme.typography.bodyMedium)
    if (hint.perspective) {
        Text(stringResource(R.string.hint_perspective), style = MaterialTheme.typography.bodySmall)
    }
    TextButton(onClick = { Share.send(context, Sharing.card(hint.toCard(titlePrefix), Share.texts(context))) }) {
        Text(stringResource(R.string.share_card_button))
    }
}
