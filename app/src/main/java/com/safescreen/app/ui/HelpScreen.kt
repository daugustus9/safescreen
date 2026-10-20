package com.safescreen.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.graph

/** S5. Opens the dialer only; the child always decides whether to place the call. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val policy by graph.policy.policy.collectAsState()
    val helplines = remember { graph.content.helplines() }

    ScreenScaffold(title = stringResource(R.string.help_title), onBack = onBack) {
        Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.titleMedium)
        if (policy.trustedPhone.isNotBlank()) {
            Button(
                onClick = { dial(context, policy.trustedPhone) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text(
                    stringResource(R.string.help_trusted, policy.trustedName.ifBlank { policy.trustedPhone }),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else {
            Text(stringResource(R.string.help_no_trusted), style = MaterialTheme.typography.bodyMedium)
        }
        SectionCard(stringResource(R.string.help_lines)) {
            helplines.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${line.name} · ${line.number}", style = MaterialTheme.typography.titleSmall)
                        Text(line.description, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { dial(context, line.number) }) { Text(stringResource(R.string.help_call)) }
                }
            }
        }
    }
}

private fun dial(context: Context, number: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
}
