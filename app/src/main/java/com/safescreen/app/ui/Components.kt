package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.safescreen.app.R
import com.safescreen.app.core.TaskKind

/** A titled screen with a back action and a scrolling column of content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    actions: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) }
                },
                actions = { actions() },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun SectionCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun Bullet(text: String) {
    Row {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Increments each time the screen resumes, e.g. after returning from Android settings. */
@Composable
fun rememberResumeCount(): Int {
    var count by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        count++
        onPauseOrDispose { }
    }
    return count
}

@Composable
fun taskKindLabel(kind: TaskKind): String = stringResource(
    when (kind) {
        TaskKind.TEST -> R.string.task_kind_test
        TaskKind.HOMEWORK -> R.string.task_kind_homework
        TaskKind.PERMISSION -> R.string.task_kind_permission
        TaskKind.MATERIALS -> R.string.task_kind_materials
        TaskKind.EVENT -> R.string.task_kind_event
    },
)
