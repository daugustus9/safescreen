package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.DailyChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** The child's Desafio do Dia results, by date: which questions, and right or wrong. On the phone only. */
class DailyStore(context: Context) {

    private val prefs = context.getSharedPreferences("daily_challenge", Context.MODE_PRIVATE)

    private val _results = MutableStateFlow(load())
    /** Date to (question id, right) in the order answered. */
    val results: StateFlow<Map<LocalDate, List<Pair<String, Boolean>>>> = _results.asStateFlow()

    fun isDone(date: LocalDate): Boolean = (_results.value[date]?.size ?: 0) >= DailyChallenge.SIZE

    fun doneDays(): Set<LocalDate> = _results.value.filterValues { it.size >= DailyChallenge.SIZE }.keys

    @Synchronized
    fun record(date: LocalDate, questionId: String, correct: Boolean) {
        val today = _results.value[date].orEmpty()
        if (today.any { it.first == questionId }) return
        val updated = today + (questionId to correct)
        prefs.edit()
            .putString(date.toString(), updated.joinToString(",") { "${it.first}:${if (it.second) 1 else 0}" })
            .apply()
        _results.value = _results.value + (date to updated)
    }

    private fun load(): Map<LocalDate, List<Pair<String, Boolean>>> =
        prefs.all.mapNotNull { (key, value) ->
            val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
            val entries = (value as? String).orEmpty().split(',').filter { it.contains(':') }.map {
                val (id, ok) = it.split(':')
                id to (ok == "1")
            }
            date to entries
        }.toMap()
}
