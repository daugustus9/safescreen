package com.safescreen.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.safescreen.app.core.EventType
import com.safescreen.app.core.SchoolNoticeParser
import com.safescreen.app.graph
import java.time.LocalDate

/**
 * C2: turns notifications from the school apps the family picked (Agenda Edu, Escola
 * Paraná, ClassApp...) into tasks and reminders. Notifications from every other app are
 * ignored without being read.
 */
class SchoolNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val graph = graph
        if (sbn.packageName !in graph.policy.current.schoolAppPackages) return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val notice = SchoolNoticeParser.parse(title, text, LocalDate.now()) ?: return
        if (graph.tasks.addFromNotice(notice, sbn.packageName)) {
            graph.events.add(EventType.SCHOOL_NOTICE, category = notice.kind.name)
        }
    }
}
