package com.safescreen.app.system

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.safescreen.app.R
import com.safescreen.app.core.Sharing

/** Opens Android's share sheet; the child picks where the text goes, if anywhere. */
object Share {

    fun send(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser))
        // Cards are drawn by a service, which has no activity to open the sheet from.
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun texts(context: Context) = Sharing.Texts(
        questionIntro = context.getString(R.string.share_question_intro),
        questionOutro = context.getString(R.string.share_question_outro),
        questionSource = context.getString(R.string.share_question_source),
        safetyIntro = context.getString(R.string.share_intro_safety),
        adviceIntro = context.getString(R.string.share_intro_advice),
        encouragementIntro = context.getString(R.string.share_intro_encouragement),
        footer = context.getString(R.string.share_footer),
    )
}
