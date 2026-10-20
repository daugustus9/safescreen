package com.safescreen.app.core

import org.json.JSONObject

/** S1 categories. Grooming (S2) needs a trained model and comes in v2. */
enum class RiskCategory { SELF_HARM, DANGEROUS_CHALLENGE, DRUGS, WEAPONS }

data class RiskRule(val category: RiskCategory, val severity: Int, val pattern: Regex)

data class Detection(val category: RiskCategory, val severity: Int)

/**
 * On-device risk detection (S1). Rules are public regexes written against normalized
 * text (see [TextNormalizer]) so anyone can audit exactly what triggers an alert.
 * The text itself is never stored or sent anywhere; only the returned category is.
 */
class RiskClassifier(private val rules: List<RiskRule>) {

    fun classify(text: String): Detection? {
        val normalized = TextNormalizer.normalize(text)
        if (normalized.length < MIN_LENGTH) return null
        return rules
            .filter { it.pattern.containsMatchIn(normalized) }
            .maxByOrNull { it.severity }
            ?.let { Detection(it.category, it.severity) }
    }

    companion object {
        private const val MIN_LENGTH = 4

        /**
         * Parses the rules file:
         * `{"rules":[{"category":"SELF_HARM","severity":3,"patterns":["\\bquero morrer\\b"]}]}`.
         * Unknown categories are skipped so an older app can read a newer rules file.
         */
        fun parseRules(json: String): List<RiskRule> {
            val array = JSONObject(json).getJSONArray("rules")
            val rules = mutableListOf<RiskRule>()
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                val category = runCatching { RiskCategory.valueOf(entry.getString("category")) }
                    .getOrNull() ?: continue
                val severity = entry.optInt("severity", 1)
                val patterns = entry.getJSONArray("patterns")
                for (j in 0 until patterns.length()) {
                    rules += RiskRule(category, severity, Regex(patterns.getString(j)))
                }
            }
            return rules
        }
    }
}
