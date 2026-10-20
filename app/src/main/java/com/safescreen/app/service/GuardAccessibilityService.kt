package com.safescreen.app.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.safescreen.app.core.Detection
import com.safescreen.app.core.EventType
import com.safescreen.app.graph
import com.safescreen.app.system.AlertNotifier

/**
 * Risk detection (S1) and focus mode (P3).
 *
 * Reads only two things: text the child types (checked on the device, then discarded;
 * password fields are skipped) and which app comes to the front. It never reads screen
 * content, never stores text, and has no network access.
 */
class GuardAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingText: String = ""
    private val lastAlertAt = mutableMapOf<String, Long>()
    private val lastBlockAt = mutableMapOf<String, Long>()

    // Checks text once typing pauses, not on every keystroke.
    private val checkTyped = Runnable {
        val text = pendingText
        pendingText = ""
        graph.content.classifier.classify(text)?.let(::onDetection)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> onTextChanged(event)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onAppInFront(pkg)
        }
    }

    private fun onTextChanged(event: AccessibilityEvent) {
        if (event.isPassword || !graph.policy.current.riskDetectionEnabled) return
        pendingText = event.text.joinToString(" ")
        handler.removeCallbacks(checkTyped)
        handler.postDelayed(checkTyped, TYPING_PAUSE_MS)
    }

    private fun onDetection(detection: Detection) {
        val category = detection.category.name
        val now = System.currentTimeMillis()
        if (now - (lastAlertAt[category] ?: 0L) < ALERT_COOLDOWN_MS) return
        lastAlertAt[category] = now

        graph.events.add(EventType.RISK_DETECTED, category = category, value = detection.severity)
        graph.content.helpCardFor(category)?.let { CardPresenter.show(this, it) }
        AlertNotifier.riskDetected(this, category)
    }

    private fun onAppInFront(pkg: String) {
        val graph = graph
        if (!graph.focus.state.value.isActive()) return
        if (pkg !in graph.policy.current.focusBlockedPackages) return
        performGlobalAction(GLOBAL_ACTION_HOME)
        graph.content.focusCard()?.let { CardPresenter.show(this, it) }
        val now = System.currentTimeMillis()
        if (now - (lastBlockAt[pkg] ?: 0L) > BLOCK_LOG_COOLDOWN_MS) {
            lastBlockAt[pkg] = now
            graph.events.add(EventType.FOCUS_BLOCKED)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(checkTyped)
        super.onDestroy()
    }

    private companion object {
        const val TYPING_PAUSE_MS = 1_200L
        const val ALERT_COOLDOWN_MS = 10 * 60_000L
        const val BLOCK_LOG_COOLDOWN_MS = 60_000L
    }
}
