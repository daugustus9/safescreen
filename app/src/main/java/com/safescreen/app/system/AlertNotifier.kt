package com.safescreen.app.system

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safescreen.app.MainActivity
import com.safescreen.app.R

/**
 * Real-time alerts (T5): S1 risk categories and S5 help presses only. In the prototype the
 * alert is a notification on this phone, which the child also sees (no hidden mode).
 * The production app sends it end-to-end encrypted to the parent's phone instead.
 */
object AlertNotifier {

    private const val CHANNEL = "parent_alerts"

    fun riskDetected(context: Context, category: String) = post(
        context,
        id = category.hashCode(),
        title = context.getString(R.string.alert_risk_title),
        text = context.getString(R.string.alert_risk_text, riskLabel(context, category)),
    )

    fun helpPressed(context: Context) = post(
        context,
        id = 2001,
        title = context.getString(R.string.alert_help_title),
        text = context.getString(R.string.alert_help_text),
    )

    fun riskLabel(context: Context, category: String): String = context.getString(
        when (category) {
            "SELF_HARM" -> R.string.risk_self_harm
            "DANGEROUS_CHALLENGE" -> R.string.risk_challenge
            "DRUGS" -> R.string.risk_drugs
            "WEAPONS" -> R.string.risk_weapons
            else -> R.string.risk_other
        },
    )

    @SuppressLint("MissingPermission") // Checked through Permissions.isGranted.
    private fun post(context: Context, id: Int, title: String, text: String) {
        if (!Permissions.isGranted(context, Access.NOTIFICATIONS)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
