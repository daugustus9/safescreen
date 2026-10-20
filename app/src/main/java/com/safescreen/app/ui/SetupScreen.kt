package com.safescreen.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safescreen.app.R
import com.safescreen.app.system.Access
import com.safescreen.app.system.Permissions

@Composable
fun SetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    var granted by remember { mutableIntStateOf(0) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted++
    }

    ScreenScaffold(title = stringResource(R.string.setup_title), onBack = onBack) {
        Text(stringResource(R.string.setup_intro), style = MaterialTheme.typography.bodyLarge)
        // Re-checked after each return from Android settings.
        key(resumes, granted) { Access.entries.forEach { access ->
            val (title, description) = when (access) {
                Access.OVERLAY -> R.string.access_overlay_title to R.string.access_overlay_desc
                Access.ACCESSIBILITY -> R.string.access_accessibility_title to R.string.access_accessibility_desc
                Access.NOTIFICATION_LISTENER -> R.string.access_listener_title to R.string.access_listener_desc
                Access.USAGE -> R.string.access_usage_title to R.string.access_usage_desc
                Access.NOTIFICATIONS -> R.string.access_notifications_title to R.string.access_notifications_desc
            }
            val isOn = Permissions.isGranted(context, access)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
                    }
                    if (isOn) {
                        AssistChip(onClick = {}, label = { Text(stringResource(R.string.access_granted)) })
                    } else {
                        OutlinedButton(onClick = {
                            val intent = Permissions.settingsIntent(context, access)
                            if (intent != null) {
                                context.startActivity(intent)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) { Text(stringResource(R.string.access_grant)) }
                    }
                }
            }
        } }
        Text(
            stringResource(R.string.setup_restricted_note),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
