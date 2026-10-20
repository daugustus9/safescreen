package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.Answer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/** The child's answers to study questions, one JSON line each, kept on the phone only. */
class AnswerStore(context: Context) {

    private val file = File(context.filesDir, "answers.jsonl")
    private val _answers = MutableStateFlow(load())
    val answers: StateFlow<List<Answer>> = _answers.asStateFlow()

    @Synchronized
    fun add(answer: Answer) {
        val line = JSONObject()
            .put("q", answer.questionId)
            .put("c", answer.chosen)
            .put("ok", answer.correct)
            .put("t", answer.timestampMillis)
            .toString()
        file.appendText(line + "\n")
        _answers.value = _answers.value + answer
    }

    private fun load(): List<Answer> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            runCatching {
                val json = JSONObject(line)
                Answer(json.getString("q"), json.getString("c"), json.getBoolean("ok"), json.getLong("t"))
            }.getOrNull()
        }
    }
}
