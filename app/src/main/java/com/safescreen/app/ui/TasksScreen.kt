package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.EventType
import com.safescreen.app.core.ParsedNotice
import com.safescreen.app.core.SchoolNoticeParser
import com.safescreen.app.core.TaskKind
import com.safescreen.app.graph
import java.time.LocalDate

private enum class DueChoice(val label: Int, val days: Long?) {
    NONE(R.string.due_none, null),
    TODAY(R.string.due_today, 0),
    TOMORROW(R.string.due_tomorrow, 1),
    TWO_DAYS(R.string.due_2days, 2),
    WEEK(R.string.due_week, 7),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val tasks by graph.tasks.tasks.collectAsState()
    var text by remember { mutableStateOf("") }
    var due by remember { mutableStateOf(DueChoice.NONE) }

    ScreenScaffold(title = stringResource(R.string.tasks_title), onBack = onBack) {
        SectionCard {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.tasks_add_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DueChoice.entries.forEach { choice ->
                    FilterChip(
                        selected = due == choice,
                        onClick = { due = choice },
                        label = { Text(stringResource(choice.label)) },
                    )
                }
            }
            Button(
                enabled = text.isNotBlank(),
                onClick = {
                    // A pasted school notice is parsed like a notification; a chosen date wins.
                    val today = LocalDate.now()
                    val parsed = SchoolNoticeParser.parse(null, text, today)
                    val notice = ParsedNotice(
                        kind = parsed?.kind ?: TaskKind.HOMEWORK,
                        title = text.trim().take(140),
                        dueDate = due.days?.let(today::plusDays) ?: parsed?.dueDate,
                    )
                    if (graph.tasks.addFromNotice(notice, sourceApp = null)) {
                        text = ""
                        due = DueChoice.NONE
                    }
                },
            ) { Text(stringResource(R.string.tasks_add)) }
        }

        val sorted = tasks.sortedWith(compareBy({ it.done }, { it.dueDate?.toEpochDay() ?: Long.MAX_VALUE }))
        if (sorted.isEmpty()) {
            Text(stringResource(R.string.tasks_empty), style = MaterialTheme.typography.bodyMedium)
        }
        sorted.forEach { task ->
            TaskRow(
                task = task,
                onDone = {
                    graph.tasks.setDone(task.id, true)
                    graph.events.add(EventType.TASK_DONE)
                },
                trailing = {
                    TextButton(onClick = { graph.tasks.delete(task.id) }) { Text(stringResource(R.string.tasks_delete)) }
                },
            )
        }
    }
}
