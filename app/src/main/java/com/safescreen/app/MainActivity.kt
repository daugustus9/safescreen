package com.safescreen.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.safescreen.app.core.EventType
import com.safescreen.app.system.AlertNotifier
import com.safescreen.app.ui.AlbumScreen
import com.safescreen.app.ui.ChildHomeScreen
import com.safescreen.app.ui.DailyChallengeScreen
import com.safescreen.app.ui.HelpScreen
import com.safescreen.app.ui.MessagesScreen
import com.safescreen.app.ui.MythOrFactScreen
import com.safescreen.app.ui.ParentGateScreen
import com.safescreen.app.ui.ParentScreen
import com.safescreen.app.ui.QuestionScreen
import com.safescreen.app.ui.SafeScreenTheme
import com.safescreen.app.ui.SetupScreen
import com.safescreen.app.ui.TasksScreen
import com.safescreen.app.ui.TransparencyScreen

class MainActivity : ComponentActivity() {

    /** A screen asked for from outside: a help card's button or the status notification. */
    private val externalRequest = mutableStateOf<Screen?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        setContent {
            SafeScreenTheme { AppRoot(externalRequest) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        when {
            intent?.getBooleanExtra(EXTRA_OPEN_HELP, false) == true -> externalRequest.value = Screen.HELP
            intent?.getBooleanExtra(EXTRA_OPEN_TRANSPARENCY, false) == true -> externalRequest.value = Screen.TRANSPARENCY
            intent?.getBooleanExtra(EXTRA_OPEN_DAILY, false) == true -> externalRequest.value = Screen.DAILY
        }
    }

    companion object {
        const val EXTRA_OPEN_HELP = "open_help"
        const val EXTRA_OPEN_TRANSPARENCY = "open_transparency"
        const val EXTRA_OPEN_DAILY = "open_daily"
    }
}

enum class Screen { TRANSPARENCY, SETUP, HOME, HELP, TASKS, MESSAGES, QUESTION, DAILY, ALBUM, MYTH, PARENT_GATE, PARENT }

@Composable
private fun AppRoot(externalRequest: MutableState<Screen?>) {
    val context = LocalContext.current
    val graph = context.graph
    val firstRun = !graph.policy.current.transparencySeen
    var screen by rememberSaveable { mutableStateOf(if (firstRun) Screen.TRANSPARENCY else Screen.HOME) }
    // The skill being practiced from the album (e.g. "MT:H16"), or null for regular practice.
    var practiceSkill by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(externalRequest.value) {
        externalRequest.value?.let { screen = it }
        externalRequest.value = null
    }
    BackHandler(enabled = screen != Screen.HOME && !(screen == Screen.TRANSPARENCY && firstRun)) {
        screen = if (screen == Screen.QUESTION && practiceSkill != null) Screen.ALBUM else Screen.HOME
    }
    val home = { screen = Screen.HOME }

    when (screen) {
        Screen.TRANSPARENCY -> TransparencyScreen(firstRun = firstRun) {
            if (firstRun) {
                graph.policy.markTransparencySeen()
                screen = Screen.SETUP
            } else {
                home()
            }
        }
        Screen.SETUP -> SetupScreen(onBack = home)
        Screen.HOME -> ChildHomeScreen(
            onOpenSetup = { screen = Screen.SETUP },
            onHelp = {
                // S5: the one action that always alerts the parent, whatever the age tier.
                graph.events.add(EventType.HELP_PRESSED)
                AlertNotifier.helpPressed(context)
                screen = Screen.HELP
            },
            onTasks = { screen = Screen.TASKS },
            onTransparency = { screen = Screen.TRANSPARENCY },
            onMessages = { screen = Screen.MESSAGES },
            onQuestion = { practiceSkill = null; screen = Screen.QUESTION },
            onDaily = { screen = Screen.DAILY },
            onAlbum = { screen = Screen.ALBUM },
            onMyth = { screen = Screen.MYTH },
            onParent = { screen = Screen.PARENT_GATE },
        )
        Screen.HELP -> HelpScreen(onBack = home)
        Screen.TASKS -> TasksScreen(onBack = home)
        Screen.MESSAGES -> MessagesScreen(onBack = home)
        Screen.QUESTION -> QuestionScreen(skill = practiceSkill, onBack = {
            screen = if (practiceSkill != null) Screen.ALBUM else Screen.HOME
        })
        Screen.MYTH -> MythOrFactScreen(onBack = home)
        Screen.DAILY -> DailyChallengeScreen(onBack = home, onAlbum = { screen = Screen.ALBUM })
        Screen.ALBUM -> AlbumScreen(onBack = home, onPractice = { skill ->
            practiceSkill = skill
            screen = Screen.QUESTION
        })
        Screen.PARENT_GATE -> ParentGateScreen(onUnlocked = { screen = Screen.PARENT }, onBack = home)
        Screen.PARENT -> ParentScreen(onLock = home)
    }
}
