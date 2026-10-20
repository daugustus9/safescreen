package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.ParsedNotice
import com.safescreen.app.core.SchoolTask
import com.safescreen.app.core.TaskKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** School tasks from school-app notifications (C2) or typed in by the child. */
class SchoolTaskStore(context: Context) {

    private val file = File(context.filesDir, "tasks.json")
    private val _tasks = MutableStateFlow(load())
    val tasks: StateFlow<List<SchoolTask>> = _tasks.asStateFlow()

    /** Adds the task unless the same notice was already turned into one. */
    @Synchronized
    fun addFromNotice(notice: ParsedNotice, sourceApp: String?): Boolean {
        val duplicate = _tasks.value.any { it.title == notice.title && it.dueDate == notice.dueDate }
        if (duplicate) return false
        save(
            _tasks.value + SchoolTask(
                id = UUID.randomUUID().toString(),
                kind = notice.kind,
                title = notice.title,
                dueDate = notice.dueDate,
                sourceApp = sourceApp,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
        return true
    }

    @Synchronized
    fun setDone(id: String, done: Boolean) =
        save(_tasks.value.map { if (it.id == id) it.copy(done = done) else it })

    @Synchronized
    fun delete(id: String) = save(_tasks.value.filterNot { it.id == id })

    private fun save(tasks: List<SchoolTask>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject()
                    .put("id", task.id)
                    .put("kind", task.kind.name)
                    .put("title", task.title)
                    .put("due", task.dueDate?.toString() ?: "")
                    .put("source", task.sourceApp ?: "")
                    .put("done", task.done)
                    .put("created", task.createdAtMillis),
            )
        }
        file.writeText(array.toString())
        _tasks.value = tasks
    }

    private fun load(): List<SchoolTask> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i ->
                val json = array.getJSONObject(i)
                SchoolTask(
                    id = json.getString("id"),
                    kind = TaskKind.valueOf(json.getString("kind")),
                    title = json.getString("title"),
                    dueDate = json.optString("due").ifEmpty { null }?.let(LocalDate::parse),
                    sourceApp = json.optString("source").ifEmpty { null },
                    done = json.optBoolean("done", false),
                    createdAtMillis = json.optLong("created", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }
}
