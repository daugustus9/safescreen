package com.safescreen.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.safescreen.app.R

/**
 * The child-facing "what this app sees" screen. Shown before anything is switched on, and
 * reachable any time from the home screen and the persistent notification.
 */
@Composable
fun TransparencyScreen(firstRun: Boolean, onContinue: () -> Unit) {
    ScreenScaffold(
        title = stringResource(R.string.transparency_title),
        onBack = if (firstRun) null else onContinue,
    ) {
        Text(stringResource(R.string.transparency_intro), style = MaterialTheme.typography.bodyLarge)
        SectionCard(stringResource(R.string.transparency_checks_title)) {
            Bullet(stringResource(R.string.transparency_checks_1))
            Bullet(stringResource(R.string.transparency_checks_2))
            Bullet(stringResource(R.string.transparency_checks_3))
            Bullet(stringResource(R.string.transparency_checks_4))
        }
        SectionCard(stringResource(R.string.transparency_parents_title)) {
            Bullet(stringResource(R.string.transparency_parents_1))
            Bullet(stringResource(R.string.transparency_parents_2))
            Bullet(stringResource(R.string.transparency_parents_3))
        }
        SectionCard(stringResource(R.string.transparency_never_title)) {
            Bullet(stringResource(R.string.transparency_never_1))
            Bullet(stringResource(R.string.transparency_never_2))
        }
        Text(stringResource(R.string.transparency_offline), style = MaterialTheme.typography.titleSmall)
        if (firstRun) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.transparency_continue))
            }
        }
    }
}
