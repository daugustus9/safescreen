package com.safescreen.app.data

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** What the card engine needs to remember between runs. */
class CardHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("card_history", Context.MODE_PRIVATE)

    val lastCardAt: LocalDateTime?
        get() = prefs.getLong(KEY_LAST, 0L).takeIf { it > 0 }
            ?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()) }

    /** Most recent first. */
    val recentIds: List<String>
        get() = prefs.getString(KEY_RECENT, "")!!.split(',').filter { it.isNotEmpty() }

    val reminderKeys: Set<String>
        get() = prefs.getStringSet(KEY_REMINDERS, emptySet())!!.toSet()

    @Synchronized
    fun recordShown(cardId: String, reminderKey: String?) {
        val recent = (listOf(cardId) + recentIds.filterNot { it == cardId }).take(MAX_RECENT)
        val reminders = if (reminderKey == null) reminderKeys else (reminderKeys + reminderKey).toList().takeLast(MAX_REMINDERS).toSet()
        prefs.edit()
            .putLong(KEY_LAST, System.currentTimeMillis())
            .putString(KEY_RECENT, recent.joinToString(","))
            .putStringSet(KEY_REMINDERS, reminders)
            .apply()
    }

    private companion object {
        const val KEY_LAST = "last_card_at"
        const val KEY_RECENT = "recent_ids"
        const val KEY_REMINDERS = "reminder_keys"
        const val MAX_RECENT = 12
        const val MAX_REMINDERS = 100
    }
}

data class FocusState(val endsAtMillis: Long?, val minutes: Int) {
    fun isActive(nowMillis: Long = System.currentTimeMillis()) = endsAtMillis != null && nowMillis < endsAtMillis
}

/** Focus timer (P3). The accessibility service pauses blocked apps while it runs. */
class FocusStore(context: Context) {

    private val prefs = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    fun start(minutes: Int) {
        prefs.edit()
            .putLong(KEY_ENDS, System.currentTimeMillis() + minutes * 60_000L)
            .putInt(KEY_MINUTES, minutes)
            .apply()
        _state.value = load()
    }

    fun stop() {
        prefs.edit().remove(KEY_ENDS).apply()
        _state.value = load()
    }

    /** Returns the session length once, after a session runs to its end; null otherwise. */
    @Synchronized
    fun takeCompletedMinutes(nowMillis: Long = System.currentTimeMillis()): Int? {
        val current = _state.value
        val ends = current.endsAtMillis ?: return null
        if (nowMillis < ends) return null
        stop()
        return current.minutes
    }

    private fun load() = FocusState(
        endsAtMillis = prefs.getLong(KEY_ENDS, 0L).takeIf { it > 0 },
        minutes = prefs.getInt(KEY_MINUTES, 0),
    )

    private companion object {
        const val KEY_ENDS = "ends_at"
        const val KEY_MINUTES = "minutes"
    }
}

/**
 * Parent-area PIN, stored as a salted SHA-256 hash. Enough to keep a child out of the
 * settings in a prototype; the real app moves parent controls to the parent's own phone.
 */
class ParentPin(context: Context) {

    private val prefs = context.getSharedPreferences("parent_pin", Context.MODE_PRIVATE)

    val isSet: Boolean get() = prefs.contains(KEY_HASH)

    fun set(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, hash(salt, pin))
            .apply()
    }

    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        return MessageDigest.isEqual(
            hash(salt, pin).toByteArray(),
            prefs.getString(KEY_HASH, "")!!.toByteArray(),
        )
    }

    private fun hash(salt: ByteArray, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(salt)
            update(pin.toByteArray())
        }.digest()
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    private companion object {
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
    }
}
