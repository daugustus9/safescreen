package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.EventType
import com.safescreen.app.core.QuestionPicker
import com.safescreen.app.core.SchoolTask
import com.safescreen.app.graph
import com.safescreen.app.service.CardOverlayService
import com.safescreen.app.system.Access
import com.safescreen.app.system.InstalledApps
import com.safescreen.app.system.Permissions
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChildHomeScreen(
    onOpenSetup: () -> Unit,
    onHelp: () -> Unit,
    onTasks: () -> Unit,
    onTransparency: () -> Unit,
    onMessages: () -> Unit,
    onQuestion: () -> Unit,
    onParent: () -> Unit,
) {
    val context = LocalContext.current
    val graph = context.graph
    val policy by graph.policy.policy.collectAsState()
    val running by CardOverlayService.running.collectAsState()
    val focus by graph.focus.state.collectAsState()
    val tasks by graph.tasks.tasks.collectAsState()
    val answers by graph.answers.answers.collectAsState()
    val resumes = rememberResumeCount()
    val allGranted = remember(resumes) { Access.entries.all { Permissions.isGranted(context, it) } }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(focus) {
        nowMillis = System.currentTimeMillis()
        while (focus.isActive(nowMillis)) {
            delay(15_000)
            nowMillis = System.currentTimeMillis()
        }
        graph.focus.takeCompletedMinutes()?.let { graph.events.add(EventType.FOCUS_COMPLETED, value = it) }
    }

    ScreenScaffold(title = stringResource(R.string.app_name), onBack = null) {
        SectionCard {
            Text(
                stringResource(if (running) R.string.home_on else R.string.home_off),
                style = MaterialTheme.typography.titleLarge,
            )
            if (!allGranted) Text(stringResource(R.string.home_setup_needed), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (running) {
                    OutlinedButton(onClick = { CardOverlayService.stop(context) }) { Text(stringResource(R.string.home_stop)) }
                } else {
                    Button(onClick = { CardOverlayService.start(context) }) { Text(stringResource(R.string.home_start)) }
                }
                OutlinedButton(onClick = onOpenSetup) { Text(stringResource(R.string.home_open_setup)) }
            }
        }

        Button(
            onClick = onHelp,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text(stringResource(R.string.home_help_button), style = MaterialTheme.typography.titleLarge) }

        SectionCard(stringResource(R.string.home_focus_title)) {
            val endsAt = focus.endsAtMillis
            if (endsAt != null && focus.isActive(nowMillis)) {
                val minutesLeft = ((endsAt - nowMillis) / 60_000L + 1).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.home_focus_left, minutesLeft),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { graph.focus.stop() }) { Text(stringResource(R.string.home_focus_stop)) }
                }
            } else {
                Button(onClick = { graph.focus.start(policy.focusMinutes) }) {
                    Text(stringResource(R.string.home_focus_start, policy.focusMinutes))
                }
            }
        }

        SectionCard(stringResource(R.string.home_tasks_title)) {
            val pending = tasks.filter { !it.done }
                .sortedBy { it.dueDate?.toEpochDay() ?: Long.MAX_VALUE }
                .take(3)
            if (pending.isEmpty()) {
                Text(stringResource(R.string.home_tasks_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                pending.forEach { task ->
                    TaskRow(task, onDone = {
                        graph.tasks.setDone(task.id, true)
                        graph.events.add(EventType.TASK_DONE)
                    })
                }
            }
            TextButton(onClick = onTasks) { Text(stringResource(R.string.home_tasks_all)) }
        }

        val age = policy.ageTier.representativeAge
        val studyQuestions = graph.content.questions.filter { age >= it.minAge }
        if (policy.studyEnabled && studyQuestions.isNotEmpty()) {
            SectionCard(stringResource(R.string.home_study_title)) {
                val latest = QuestionPicker.latestAnswers(answers)
                val today = LocalDate.now()
                val doneToday = answers.any {
                    Instant.ofEpochMilli(it.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today
                }
                Text(
                    stringResource(
                        R.string.home_study_progress,
                        studyQuestions.count { it.id in latest },
                        studyQuestions.size,
                        latest.values.count { it.correct },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (doneToday) {
                    Text(stringResource(R.string.home_study_done_today), color = MaterialTheme.colorScheme.secondary)
                }
                Button(onClick = onQuestion) {
                    Text(stringResource(if (doneToday) R.string.home_study_more else R.string.home_study_start))
                }
            }
        }

        SectionCard {
            Text(
                stringResource(R.string.home_agreement, policy.bedtimeStart.toString(), policy.cardIntervalMinutes),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!policy.fullySigned) {
                Text(stringResource(R.string.home_agreement_unsigned), style = MaterialTheme.typography.bodySmall)
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTransparency) { Text(stringResource(R.string.home_what_it_sees)) }
            OutlinedButton(onClick = onMessages) { Text(stringResource(R.string.home_all_messages)) }
            OutlinedButton(onClick = onParent) { Text(stringResource(R.string.home_parent_area)) }
            OutlinedButton(onClick = { CardOverlayService.showNow(context) }) { Text(stringResource(R.string.home_show_card)) }
        }
    }
}

@Composable
fun TaskRow(task: SchoolTask, onDone: () -> Unit, trailing: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val dueFormat = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()) }
    val source = remember(task.sourceApp) { task.sourceApp?.let { InstalledApps.label(context, it) ?: it } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = task.done, onCheckedChange = { if (it) onDone() })
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.bodyMedium)
            val due = task.dueDate?.let { stringResource(R.string.tasks_due, dueFormat.format(it)) }
            val from = source?.let { stringResource(R.string.tasks_from_app, it) }
            Text(
                listOfNotNull(taskKindLabel(task.kind), due, from).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        trailing()
    }
}
