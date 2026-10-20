package com.safescreen.app.core

import java.text.Normalizer

/** Lowercase, accent-free, single-spaced text, so rules match "Amanhã" and "amanha" alike. */
object TextNormalizer {
    private val diacritics = Regex("\\p{Mn}+")
    private val spaces = Regex("\\s+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase()
            .replace(spaces, " ")
            .trim()
}
