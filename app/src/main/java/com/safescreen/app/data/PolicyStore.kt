package com.safescreen.app.data

import android.content.Context
import com.safescreen.app.core.AgeTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.time.LocalTime

/** The family agreement (T2) plus a few device flags. */
data class Policy(
    val ageTier: AgeTier = AgeTier.AGE_10_12,
    val cardsEnabled: Boolean = true,
    val cardIntervalMinutes: Int = AgeTier.AGE_10_12.defaultCardIntervalMinutes,
    val bedtimeStart: LocalTime = LocalTime.of(21, 30),
    val bedtimeEnd: LocalTime = LocalTime.of(7, 0),
    val focusMinutes: Int = 25,
    val focusBlockedPackages: Set<String> = emptySet(),
    val schoolAppPackages: Set<String> = emptySet(),
    val riskDetectionEnabled: Boolean = true,
    val encouragementEnabled: Boolean = true,
    /** Study questions (ENEM for 13+ now); answers stay on the phone. */
    val studyEnabled: Boolean = true,
    /** Encouragement messages written by the family; the child can read them all. */
    val familyMessages: List<String> = emptyList(),
    val trustedName: String = "",
    val trustedPhone: String = "",
    val parentSignedAtMillis: Long? = null,
    val childSignedAtMillis: Long? = null,
    val transparencySeen: Boolean = false,
) {
    val fullySigned: Boolean get() = parentSignedAtMillis != null && childSignedAtMillis != null
}

class PolicyStore(context: Context) {

    private val prefs = context.getSharedPreferences("policy", Context.MODE_PRIVATE)
    private val _policy = MutableStateFlow(load())
    val policy: StateFlow<Policy> = _policy.asStateFlow()

    val current: Policy get() = _policy.value

    /** Any rule change asks both parent and child to sign the agreement again. */
    fun updateRules(transform: (Policy) -> Policy) = save(
        transform(current).copy(parentSignedAtMillis = null, childSignedAtMillis = null),
    )

    fun signAsParent() = save(current.copy(parentSignedAtMillis = System.currentTimeMillis()))

    fun signAsChild() = save(current.copy(childSignedAtMillis = System.currentTimeMillis()))

    fun markTransparencySeen() = save(current.copy(transparencySeen = true))

    @Synchronized
    private fun save(policy: Policy) {
        prefs.edit()
            .putString(KEY_TIER, policy.ageTier.name)
            .putBoolean(KEY_CARDS, policy.cardsEnabled)
            .putInt(KEY_INTERVAL, policy.cardIntervalMinutes)
            .putString(KEY_BED_START, policy.bedtimeStart.toString())
            .putString(KEY_BED_END, policy.bedtimeEnd.toString())
            .putInt(KEY_FOCUS, policy.focusMinutes)
            .putStringSet(KEY_BLOCKED, policy.focusBlockedPackages)
            .putStringSet(KEY_SCHOOL, policy.schoolAppPackages)
            .putBoolean(KEY_RISK, policy.riskDetectionEnabled)
            .putBoolean(KEY_ENCOURAGEMENT, policy.encouragementEnabled)
            .putBoolean(KEY_STUDY, policy.studyEnabled)
            .putString(KEY_FAMILY, JSONArray(policy.familyMessages).toString())
            .putString(KEY_TRUSTED_NAME, policy.trustedName)
            .putString(KEY_TRUSTED_PHONE, policy.trustedPhone)
            .putLong(KEY_PARENT_SIGNED, policy.parentSignedAtMillis ?: 0L)
            .putLong(KEY_CHILD_SIGNED, policy.childSignedAtMillis ?: 0L)
            .putBoolean(KEY_TRANSPARENCY, policy.transparencySeen)
            .apply()
        _policy.value = policy
    }

    private fun load(): Policy {
        val defaults = Policy()
        return Policy(
            ageTier = prefs.getString(KEY_TIER, null)
                ?.let { runCatching { AgeTier.valueOf(it) }.getOrNull() } ?: defaults.ageTier,
            cardsEnabled = prefs.getBoolean(KEY_CARDS, defaults.cardsEnabled),
            cardIntervalMinutes = prefs.getInt(KEY_INTERVAL, defaults.cardIntervalMinutes),
            bedtimeStart = prefs.getString(KEY_BED_START, null)?.let(LocalTime::parse) ?: defaults.bedtimeStart,
            bedtimeEnd = prefs.getString(KEY_BED_END, null)?.let(LocalTime::parse) ?: defaults.bedtimeEnd,
            focusMinutes = prefs.getInt(KEY_FOCUS, defaults.focusMinutes),
            focusBlockedPackages = prefs.getStringSet(KEY_BLOCKED, null)?.toSet() ?: emptySet(),
            schoolAppPackages = prefs.getStringSet(KEY_SCHOOL, null)?.toSet() ?: emptySet(),
            riskDetectionEnabled = prefs.getBoolean(KEY_RISK, defaults.riskDetectionEnabled),
            encouragementEnabled = prefs.getBoolean(KEY_ENCOURAGEMENT, defaults.encouragementEnabled),
            studyEnabled = prefs.getBoolean(KEY_STUDY, defaults.studyEnabled),
            familyMessages = prefs.getString(KEY_FAMILY, null)
                ?.let { json -> JSONArray(json).let { array -> (0 until array.length()).map(array::getString) } }
                ?: emptyList(),
            trustedName = prefs.getString(KEY_TRUSTED_NAME, "") ?: "",
            trustedPhone = prefs.getString(KEY_TRUSTED_PHONE, "") ?: "",
            parentSignedAtMillis = prefs.getLong(KEY_PARENT_SIGNED, 0L).takeIf { it > 0 },
            childSignedAtMillis = prefs.getLong(KEY_CHILD_SIGNED, 0L).takeIf { it > 0 },
            transparencySeen = prefs.getBoolean(KEY_TRANSPARENCY, false),
        )
    }

    private companion object {
        const val KEY_TIER = "age_tier"
        const val KEY_CARDS = "cards_enabled"
        const val KEY_INTERVAL = "card_interval"
        const val KEY_BED_START = "bedtime_start"
        const val KEY_BED_END = "bedtime_end"
        const val KEY_FOCUS = "focus_minutes"
        const val KEY_BLOCKED = "focus_blocked"
        const val KEY_SCHOOL = "school_apps"
        const val KEY_RISK = "risk_detection"
        const val KEY_ENCOURAGEMENT = "encouragement"
        const val KEY_STUDY = "study"
        const val KEY_FAMILY = "family_messages"
        const val KEY_TRUSTED_NAME = "trusted_name"
        const val KEY_TRUSTED_PHONE = "trusted_phone"
        const val KEY_PARENT_SIGNED = "parent_signed"
        const val KEY_CHILD_SIGNED = "child_signed"
        const val KEY_TRANSPARENCY = "transparency_seen"
    }
}
