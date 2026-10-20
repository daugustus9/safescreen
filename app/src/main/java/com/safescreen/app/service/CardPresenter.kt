package com.safescreen.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.safescreen.app.MainActivity
import com.safescreen.app.R
import com.safescreen.app.core.Card
import com.safescreen.app.core.CardKind

/**
 * Draws one card at the top of the screen, over whatever app is open. Unlike the
 * subliminal-message app's flashes, cards stay until read: the child dismisses them, and
 * only non-urgent cards hide on their own after [AUTO_HIDE_MS].
 *
 * Shared by the card service and the accessibility service, which run in the same process.
 * The held view is built from the application context only, so no activity can leak.
 */
@SuppressLint("StaticFieldLeak")
object CardPresenter {

    private const val AUTO_HIDE_MS = 25_000L
    private val main = Handler(Looper.getMainLooper())
    private var current: View? = null
    private val autoHide = Runnable { hide() }

    fun show(context: Context, card: Card) = main.post {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return@post
        hide()
        val view = buildView(app, card)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: the app underneath keeps the keyboard. Touchable: the card's
            // buttons work, and the rest of the screen is outside this small window.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            y = dp(app, 48)
        }
        val windowManager = app.getSystemService(WindowManager::class.java)
        runCatching { windowManager.addView(view, params) }.onSuccess {
            current = view
            if (card.kind != CardKind.HELP) main.postDelayed(autoHide, AUTO_HIDE_MS)
        }
    }

    fun hide() {
        main.removeCallbacks(autoHide)
        val view = current ?: return
        current = null
        runCatching { view.context.getSystemService(WindowManager::class.java).removeView(view) }
    }

    private fun buildView(context: Context, card: Card): View {
        val accent = when (card.kind) {
            CardKind.HELP, CardKind.SAFETY -> Color.parseColor("#B3261E")
            CardKind.SCHOOL -> Color.parseColor("#1E5BB8")
            CardKind.BEDTIME, CardKind.FOCUS -> Color.parseColor("#4A3F8C")
            CardKind.ADVICE -> Color.parseColor("#2E7D32")
        }
        val title = TextView(context).apply {
            text = card.title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
        }
        val body = TextView(context).apply {
            text = card.body
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, dp(context, 6), 0, dp(context, 8))
        }
        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            if (card.kind == CardKind.HELP) {
                addView(textButton(context, R.string.card_get_help) {
                    hide()
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(MainActivity.EXTRA_OPEN_HELP, true),
                    )
                })
            }
            addView(textButton(context, R.string.card_ok) { hide() })
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 16), dp(context, 12), dp(context, 8))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 16).toFloat()
                setColor(accent)
            }
            addView(title)
            addView(body)
            addView(buttons)
        }.let { inner ->
            // Side margins so the card reads as a card, not a banner.
            LinearLayout(context).apply {
                setPadding(dp(context, 12), 0, dp(context, 12), 0)
                addView(inner, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ))
            }
        }
    }

    private fun textButton(context: Context, label: Int, onClick: () -> Unit) =
        Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            text = context.getString(label)
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }

    private fun dp(context: Context, value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}
