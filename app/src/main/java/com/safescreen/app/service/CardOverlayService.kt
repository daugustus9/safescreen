package com.safescreen.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safescreen.app.MainActivity
import com.safescreen.app.R
import com.safescreen.app.core.CardKind
import com.safescreen.app.core.EventType
import com.safescreen.app.core.ROTATION_ORDER
import com.safescreen.app.core.ScheduledCard
import com.safescreen.app.core.SchedulerState
import com.safescreen.app.graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.safescreen.app.core.DailyChallenge
import com.safescreen.app.core.DailyHint
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Foreground service running the message engine: every [TICK_MS] it asks the
 * [com.safescreen.app.core.CardScheduler] whether a card is due and shows it. Its
 * persistent notification is the visible "SafeScreen is on" indicator.
 */
class CardOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW_NOW -> scope.launch { tick(force = true) }
        }
        if (!_running.value) {
            startForegroundWithNotification()
            _running.value = true
            scope.launch {
                while (isActive) {
                    tick(force = false)
                    delay(TICK_MS)
                }
            }
        }
        return START_STICKY
    }

    private fun tick(force: Boolean) {
        val graph = applicationContext.graph
        graph.focus.takeCompletedMinutes()?.let { graph.events.add(EventType.FOCUS_COMPLETED, value = it) }

        val policy = graph.policy.current
        if (!policy.cardsEnabled && !force) return
        val today = LocalDate.now()
        val challengeWaiting = policy.ageTier.representativeAge >= 13 &&
            DailyChallenge.questionsFor(today, graph.content.questions).isNotEmpty() &&
            !graph.daily.isDone(today)
        val state = SchedulerState(
            now = LocalDateTime.now(),
            tier = policy.ageTier,
            intervalMinutes = policy.cardIntervalMinutes,
            bedtimeStart = policy.bedtimeStart,
            bedtimeEnd = policy.bedtimeEnd,
            lastCardAt = if (force) null else graph.cardHistory.lastCardAt,
            recentCardIds = graph.cardHistory.recentIds,
            tasks = graph.tasks.tasks.value,
            shownReminderKeys = graph.cardHistory.reminderKeys,
            rotationKinds = if (policy.encouragementEnabled) ROTATION_ORDER.toSet() else ROTATION_ORDER.toSet() - CardKind.ENCOURAGEMENT,
            familyCards = if (policy.encouragementEnabled) graph.content.familyCards(policy.familyMessages) else emptyList(),
            studyInvite = if (policy.studyEnabled && challengeWaiting) graph.content.studyInviteCard() else null,
            dailyHint = DailyHint.forDate(today, graph.content.hints, policy.ageTier.representativeAge)
                ?.toCard(graph.content.hintTitlePrefix),
        )
        graph.scheduler.next(state)?.let(::present)
    }

    private fun present(scheduled: ScheduledCard) {
        val graph = applicationContext.graph
        CardPresenter.show(this, scheduled.card)
        graph.cardHistory.recordShown(scheduled.card.id, scheduled.reminderKey)
        graph.events.add(EventType.CARD_SHOWN, category = scheduled.card.kind.name)
    }

    private fun startForegroundWithNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.channel_status), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TRANSPARENCY, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.status_notification_title))
            .setContentText(getString(R.string.status_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        _running.value = false
        CardPresenter.hide()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "safescreen_status"
        private const val NOTIFICATION_ID = 1001
        private const val TICK_MS = 30_000L
        private const val ACTION_STOP = "com.safescreen.app.action.STOP"
        private const val ACTION_SHOW_NOW = "com.safescreen.app.action.SHOW_NOW"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, CardOverlayService::class.java))

        fun stop(context: Context) {
            context.startService(Intent(context, CardOverlayService::class.java).setAction(ACTION_STOP))
        }

        /** Shows the next card right away, ignoring the interval (for trying the app out). */
        fun showNow(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, CardOverlayService::class.java).setAction(ACTION_SHOW_NOW),
        )
    }
}
