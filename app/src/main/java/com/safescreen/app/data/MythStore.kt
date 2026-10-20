package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.ClaimResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Mito ou Verdade? history, on the phone only: latest result per claim, rounds played, best round. */
class MythStore(context: Context) {

    private val prefs = context.getSharedPreferences("myth_or_fact", Context.MODE_PRIVATE)

    private val _results = MutableStateFlow(loadResults())
    val results: StateFlow<List<ClaimResult>> = _results.asStateFlow()

    val roundsPlayed: Int get() = prefs.getInt(KEY_ROUNDS, 0)
    val bestRound: Int get() = prefs.getInt(KEY_BEST, 0)

    @Synchronized
    fun record(claimId: String, correct: Boolean) {
        val result = ClaimResult(claimId, correct, System.currentTimeMillis())
        prefs.edit().putString(PREFIX + claimId, "${if (correct) 1 else 0}:${result.timestampMillis}").apply()
        _results.value = _results.value.filterNot { it.claimId == claimId } + result
    }

    @Synchronized
    fun finishRound(score: Int) {
        prefs.edit()
            .putInt(KEY_ROUNDS, roundsPlayed + 1)
            .putInt(KEY_BEST, maxOf(bestRound, score))
            .apply()
    }

    private fun loadResults(): List<ClaimResult> = prefs.all.mapNotNull { (key, value) ->
        if (!key.startsWith(PREFIX)) return@mapNotNull null
        val parts = (value as? String)?.split(':') ?: return@mapNotNull null
        ClaimResult(key.removePrefix(PREFIX), parts[0] == "1", parts.getOrNull(1)?.toLongOrNull() ?: 0L)
    }

    private companion object {
        const val PREFIX = "claim:"
        const val KEY_ROUNDS = "rounds"
        const val KEY_BEST = "best"
    }
}
