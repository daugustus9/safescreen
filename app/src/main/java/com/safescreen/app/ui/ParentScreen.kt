package com.safescreen.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.core.AgeTier
import com.safescreen.app.core.WeeklySummary
import com.safescreen.app.data.PolicyStore
import com.safescreen.app.graph
import com.safescreen.app.system.AlertNotifier
import com.safescreen.app.system.InstalledApp
import com.safescreen.app.system.InstalledApps
import com.safescreen.app.system.UsageReader
import java.time.LocalDate

@Composable
fun ParentScreen(onLock: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    ScreenScaffold(
        title = stringResource(R.string.parent_title),
        onBack = null,
        actions = { TextButton(onClick = onLock) { Text(stringResource(R.string.parent_lock)) } },
    ) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_summary)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_agreement)) })
        }
        if (tab == 0) SummaryTab() else AgreementTab()
    }
}

@Composable
private fun SummaryTab() {
    val context = LocalContext.current
    val graph = context.graph
    val version by graph.events.version.collectAsState()
    val tasks by graph.tasks.tasks.collectAsState()
    val policy by graph.policy.policy.collectAsState()
    val resumes = rememberResumeCount()
    val usage = remember(resumes) { UsageReader.lastDays(context, 7) }
    val summary = remember(version, tasks, policy.ageTier, usage) {
        WeeklySummary.build(graph.events.lastDays(7), tasks, usage, policy.ageTier, LocalDate.now())
    }

    SectionCard(stringResource(R.string.summary_alerts)) {
        if (summary.riskByCategory.isEmpty()) {
            Text(stringResource(R.string.summary_no_alerts))
        } else {
            summary.riskByCategory.forEach { (category, count) ->
                Text("${AlertNotifier.riskLabel(context, category)}: $count", style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(stringResource(R.string.summary_help, summary.helpPresses))
        Text(stringResource(R.string.summary_alerts_note), style = MaterialTheme.typography.bodySmall)
    }
    SectionCard(stringResource(R.string.summary_school)) {
        Text(stringResource(R.string.summary_notices, summary.schoolNotices))
        Text(stringResource(R.string.summary_tasks_done, summary.tasksDone))
        if (summary.upcoming.isNotEmpty()) {
            Text(stringResource(R.string.summary_upcoming), style = MaterialTheme.typography.titleSmall)
            summary.upcoming.take(5).forEach { task ->
                Text("• ${taskKindLabel(task.kind)}: ${task.title}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    SectionCard(stringResource(R.string.summary_focus)) {
        Text(stringResource(R.string.summary_focus_minutes, summary.focusMinutes))
        Text(stringResource(R.string.summary_focus_blocks, summary.focusBlocks))
        Text(stringResource(R.string.summary_cards, summary.cardsShown))
    }
    SectionCard(stringResource(R.string.summary_screen)) {
        if (usage.isEmpty()) {
            Text(stringResource(R.string.summary_usage_missing))
        } else {
            Text(stringResource(R.string.summary_screen_total, hoursMinutes(summary.totalScreenMinutes)))
            if (policy.ageTier.showsAppDetails) {
                summary.topApps.forEach { app ->
                    Text("• ${app.label}: ${hoursMinutes(app.minutes)}", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(stringResource(R.string.summary_screen_hidden), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    Text(stringResource(R.string.summary_relay_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun hoursMinutes(minutes: Int): String =
    stringResource(R.string.summary_hours_minutes, minutes / 60, minutes % 60)

@Composable
private fun AgreementTab() {
    val context = LocalContext.current
    val store = context.graph.policy
    val policy by store.policy.collectAsState()
    var picker by remember { mutableStateOf<AppPicker?>(null) }

    Text(stringResource(R.string.agreement_signatures_note), style = MaterialTheme.typography.bodySmall)

    SectionCard(stringResource(R.string.agreement_tier)) {
        AgeTier.entries.forEach { tier ->
            val (label, description) = when (tier) {
                AgeTier.UNDER_10 -> R.string.tier_under_10 to R.string.tier_desc_under_10
                AgeTier.AGE_10_12 -> R.string.tier_10_12 to R.string.tier_desc_10_12
                AgeTier.TEEN_13_PLUS -> R.string.tier_13_plus to R.string.tier_desc_13_plus
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = policy.ageTier == tier,
                    onClick = {
                        store.updateRules { it.copy(ageTier = tier, cardIntervalMinutes = tier.defaultCardIntervalMinutes) }
                    },
                ),
            ) {
                RadioButton(selected = policy.ageTier == tier, onClick = null)
                Column(Modifier.padding(start = 8.dp)) {
                    Text(stringResource(label), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    SectionCard {
        SwitchRow(stringResource(R.string.agreement_cards), policy.cardsEnabled) { on ->
            store.updateRules { it.copy(cardsEnabled = on) }
        }
        // Local while dragging; the rule (and signatures) change once, on release.
        var interval by remember(policy.cardIntervalMinutes) { mutableFloatStateOf(policy.cardIntervalMinutes.toFloat()) }
        Text(stringResource(R.string.agreement_interval, interval.toInt()))
        Slider(
            value = interval,
            onValueChange = { interval = it },
            valueRange = 10f..90f,
            steps = 15,
            onValueChangeFinished = { store.updateRules { it.copy(cardIntervalMinutes = interval.toInt()) } },
        )
        SwitchRow(stringResource(R.string.agreement_risk), policy.riskDetectionEnabled) { on ->
            store.updateRules { it.copy(riskDetectionEnabled = on) }
        }
        SwitchRow(stringResource(R.string.agreement_encouragement), policy.encouragementEnabled) { on ->
            store.updateRules { it.copy(encouragementEnabled = on) }
        }
    }

    FamilyMessagesCard(store)

    SectionCard(stringResource(R.string.agreement_bedtime)) {
        Stepper(stringResource(R.string.agreement_bed_start, policy.bedtimeStart.toString()),
            onMinus = { store.updateRules { it.copy(bedtimeStart = it.bedtimeStart.minusMinutes(15)) } },
            onPlus = { store.updateRules { it.copy(bedtimeStart = it.bedtimeStart.plusMinutes(15)) } })
        Stepper(stringResource(R.string.agreement_bed_end, policy.bedtimeEnd.toString()),
            onMinus = { store.updateRules { it.copy(bedtimeEnd = it.bedtimeEnd.minusMinutes(15)) } },
            onPlus = { store.updateRules { it.copy(bedtimeEnd = it.bedtimeEnd.plusMinutes(15)) } })
    }

    SectionCard {
        Stepper(stringResource(R.string.agreement_focus_length, policy.focusMinutes),
            onMinus = { store.updateRules { it.copy(focusMinutes = (it.focusMinutes - 5).coerceAtLeast(5)) } },
            onPlus = { store.updateRules { it.copy(focusMinutes = (it.focusMinutes + 5).coerceAtMost(120)) } })
        PickerRow(stringResource(R.string.agreement_focus_apps), policy.focusBlockedPackages.size) {
            picker = AppPicker.FOCUS
        }
        PickerRow(stringResource(R.string.agreement_school_apps), policy.schoolAppPackages.size) {
            picker = AppPicker.SCHOOL
        }
    }

    TrustedAdultCard(store)

    SectionCard {
        if (policy.parentSignedAtMillis != null) {
            Text(stringResource(R.string.agreement_signed_parent), color = MaterialTheme.colorScheme.secondary)
        } else {
            Button(onClick = store::signAsParent) { Text(stringResource(R.string.agreement_sign_parent)) }
        }
        if (policy.childSignedAtMillis != null) {
            Text(stringResource(R.string.agreement_signed_child), color = MaterialTheme.colorScheme.secondary)
        } else {
            OutlinedButton(onClick = store::signAsChild) { Text(stringResource(R.string.agreement_sign_child)) }
        }
    }

    picker?.let { which ->
        val selected = if (which == AppPicker.FOCUS) policy.focusBlockedPackages else policy.schoolAppPackages
        AppPickerDialog(
            title = stringResource(if (which == AppPicker.FOCUS) R.string.agreement_focus_apps else R.string.agreement_school_apps),
            initial = selected,
            onDone = { chosen ->
                if (chosen != selected) {
                    store.updateRules {
                        if (which == AppPicker.FOCUS) it.copy(focusBlockedPackages = chosen) else it.copy(schoolAppPackages = chosen)
                    }
                }
                picker = null
            },
        )
    }
}

private enum class AppPicker { FOCUS, SCHOOL }

@Composable
private fun TrustedAdultCard(store: PolicyStore) {
    val policy by store.policy.collectAsState()
    var name by remember(policy.trustedName) { mutableStateOf(policy.trustedName) }
    var phone by remember(policy.trustedPhone) { mutableStateOf(policy.trustedPhone) }
    SectionCard(stringResource(R.string.agreement_trusted)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text(stringResource(R.string.agreement_trusted_name)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text(stringResource(R.string.agreement_trusted_phone)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            enabled = name != policy.trustedName || phone != policy.trustedPhone,
            onClick = { store.updateRules { it.copy(trustedName = name.trim(), trustedPhone = phone.trim()) } },
        ) { Text(stringResource(R.string.agreement_save_contact)) }
    }
}

/** Visible encouragement the family writes, the open counterpart of the subliminal app's library. */
@Composable
private fun FamilyMessagesCard(store: PolicyStore) {
    val policy by store.policy.collectAsState()
    var draft by remember { mutableStateOf("") }
    SectionCard(stringResource(R.string.agreement_family)) {
        policy.familyMessages.forEachIndexed { index, message ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    store.updateRules { it.copy(familyMessages = it.familyMessages.filterIndexed { i, _ -> i != index }) }
                }) { Text(stringResource(R.string.common_delete)) }
            }
        }
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it.take(MAX_FAMILY_MESSAGE) },
            label = { Text(stringResource(R.string.agreement_family_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            enabled = draft.isNotBlank() && draft.trim() !in policy.familyMessages,
            onClick = {
                val message = draft.trim()
                store.updateRules { it.copy(familyMessages = it.familyMessages + message) }
                draft = ""
            },
        ) { Text(stringResource(R.string.agreement_family_add)) }
        Text(stringResource(R.string.agreement_family_note), style = MaterialTheme.typography.bodySmall)
    }
}

private const val MAX_FAMILY_MESSAGE = 140

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun Stepper(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onMinus) { Text("−") }
        OutlinedButton(onClick = onPlus) { Text("+") }
    }
}

@Composable
private fun PickerRow(label: String, count: Int, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onClick) { Text(stringResource(R.string.agreement_choose, count)) }
    }
}

@Composable
private fun AppPickerDialog(title: String, initial: Set<String>, onDone: (Set<String>) -> Unit) {
    val context = LocalContext.current
    val apps: List<InstalledApp> = remember { InstalledApps.launchable(context) }
    var chosen by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { onDone(initial) },
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                apps.forEach { app ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = app.packageName in chosen,
                            onCheckedChange = { on -> chosen = if (on) chosen + app.packageName else chosen - app.packageName },
                        )
                        Text(app.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDone(chosen) }) { Text(stringResource(R.string.common_done)) } },
    )
}
