package com.safescreen.app

import android.app.Application
import android.content.Context
import com.safescreen.app.core.CardScheduler
import com.safescreen.app.data.AnswerStore
import com.safescreen.app.data.CardHistoryStore
import com.safescreen.app.data.ContentLibrary
import com.safescreen.app.data.EventLog
import com.safescreen.app.data.FocusStore
import com.safescreen.app.data.ParentPin
import com.safescreen.app.data.PolicyStore
import com.safescreen.app.data.SchoolTaskStore

/** One instance of each store, shared by the UI and the three services (same process). */
class AppGraph(context: Context) {
    val content = ContentLibrary(context)
    val policy = PolicyStore(context)
    val events = EventLog(context)
    val tasks = SchoolTaskStore(context)
    val focus = FocusStore(context)
    val cardHistory = CardHistoryStore(context)
    val parentPin = ParentPin(context)
    val answers = AnswerStore(context)
    val scheduler by lazy { CardScheduler(content.cards, content::reminderCard) }
}

class SafeScreenApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

val Context.graph: AppGraph get() = (applicationContext as SafeScreenApp).graph
