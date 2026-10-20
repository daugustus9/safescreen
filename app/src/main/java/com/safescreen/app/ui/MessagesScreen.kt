package com.safescreen.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.safescreen.app.R
import com.safescreen.app.core.Card
import com.safescreen.app.core.CardKind
import com.safescreen.app.core.Sharing
import com.safescreen.app.graph
import com.safescreen.app.system.Share

/**
 * Every message the app can ever put on screen, readable by the child and the parent:
 * the in-app copy of the public message library, plus the family's own messages.
 */
@Composable
fun MessagesScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val policy by graph.policy.policy.collectAsState()
    val byKind = graph.content.cards.groupBy { it.kind }

    ScreenScaffold(title = stringResource(R.string.messages_title), onBack = onBack) {
        Text(stringResource(R.string.messages_intro), style = MaterialTheme.typography.bodyLarge)

        SectionCard(stringResource(R.string.kind_family)) {
            if (policy.familyMessages.isEmpty()) {
                Text(stringResource(R.string.messages_family_empty), style = MaterialTheme.typography.bodyMedium)
            }
            policy.familyMessages.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        listOf(
            CardKind.ENCOURAGEMENT to R.string.kind_encouragement,
            CardKind.ADVICE to R.string.kind_advice,
            CardKind.SAFETY to R.string.kind_safety,
            CardKind.BEDTIME to R.string.kind_bedtime,
            CardKind.FOCUS to R.string.kind_focus,
            CardKind.HELP to R.string.kind_help,
        ).forEach { (kind, label) ->
            SectionCard(stringResource(label)) {
                byKind[kind].orEmpty().forEachIndexed { index, card ->
                    if (index > 0) HorizontalDivider()
                    MessageItem(card)
                }
            }
        }
        SectionCard(stringResource(R.string.kind_hints)) {
            Text(stringResource(R.string.messages_hints_note), style = MaterialTheme.typography.bodySmall)
            graph.content.hints.forEach { hint ->
                HorizontalDivider()
                HintBody(hint, graph.content.hintCategories[hint.category], graph.content.hintTitlePrefix)
            }
        }
        SectionCard(stringResource(R.string.kind_study)) {
            val invite = graph.content.studyInviteCard()
            MessageItem(invite)
            Text(stringResource(R.string.messages_study_note), style = MaterialTheme.typography.bodySmall)
        }
        SectionCard(stringResource(R.string.kind_school)) {
            Text(stringResource(R.string.messages_school_note), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageItem(card: Card) {
    val context = LocalContext.current
    Column {
        Text(card.title, style = MaterialTheme.typography.titleSmall)
        Text(card.body, style = MaterialTheme.typography.bodyMedium)
        if (Sharing.isShareable(card)) {
            TextButton(onClick = { Share.send(context, Sharing.card(card, Share.texts(context))) }) {
                Text(stringResource(R.string.share_card_button))
            }
        }
    }
}
