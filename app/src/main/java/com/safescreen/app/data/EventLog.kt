package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.Event
import com.safescreen.app.core.EventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The on-device event store: one JSON line per event, kept [RETENTION_DAYS] days.
 * Events hold a type and category only, never what the child typed or saw.
 */
class EventLog(context: Context) {

    private val file = File(context.filesDir, "events.jsonl")

    /** Bumps on every write so screens know to rebuild the summary. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    init {
        prune()
    }

    @Synchronized
    fun add(type: EventType, category: String? = null, value: Int = 0) {
        val line = JSONObject()
            .put("t", System.currentTimeMillis())
            .put("type", type.name)
            .apply { if (category != null) put("cat", category) }
            .put("v", value)
            .toString()
        file.appendText(line + "\n")
        _version.value += 1
    }

    @Synchronized
    fun readSince(sinceMillis: Long): List<Event> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull(::parse).filter { it.timestampMillis >= sinceMillis }
    }

    fun lastDays(days: Int): List<Event> =
        readSince(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong()))

    @Synchronized
    private fun prune() {
        if (!file.exists()) return
        val keep = readSince(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RETENTION_DAYS))
        file.writeText(
            keep.joinToString("") { event ->
                JSONObject()
                    .put("t", event.timestampMillis)
                    .put("type", event.type.name)
                    .apply { if (event.category != null) put("cat", event.category) }
                    .put("v", event.value)
                    .toString() + "\n"
            },
        )
    }

    private fun parse(line: String): Event? = runCatching {
        val json = JSONObject(line)
        Event(
            timestampMillis = json.getLong("t"),
            type = EventType.valueOf(json.getString("type")),
            category = json.optString("cat").ifEmpty { null },
            value = json.optInt("v", 0),
        )
    }.getOrNull()

    private companion object {
        const val RETENTION_DAYS = 30L
    }
}
