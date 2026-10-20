package com.safescreen.app.core

/**
 * Text the child can choose to share: an exam question to challenge a friend, or a safety,
 * advice or encouragement card. Every message names SafeScreen, which is how the app spreads.
 * Nothing personal ever goes out: no answers chosen, no family messages, no school tasks.
 */
object Sharing {

    /** Wording that depends on the device language. */
    data class Texts(
        val questionIntro: String,
        val questionOutro: String,
        /** e.g. "Fonte: %s (INEP)." */
        val questionSource: String,
        val safetyIntro: String,
        val adviceIntro: String,
        val encouragementIntro: String,
        val footer: String,
    )

    /** Library cards only: help, school, bedtime, focus and family messages stay private. */
    fun isShareable(card: Card): Boolean =
        card.kind in setOf(CardKind.SAFETY, CardKind.ADVICE, CardKind.ENCOURAGEMENT) && !card.id.startsWith("family-")

    /** The question and its alternatives, never the answer or the explanation. */
    fun question(question: Question, texts: Texts): String = buildString {
        appendLine(texts.questionIntro)
        appendLine()
        appendLine(question.stem)
        appendLine()
        question.options.forEach { (letter, option) -> appendLine("$letter) $option") }
        appendLine()
        appendLine(texts.questionOutro)
        appendLine(texts.questionSource.format(question.source))
        appendLine()
        append(texts.footer)
    }

    fun card(card: Card, texts: Texts): String {
        val intro = when (card.kind) {
            CardKind.SAFETY -> texts.safetyIntro
            CardKind.ADVICE -> texts.adviceIntro
            else -> texts.encouragementIntro
        }
        // *bold* is WhatsApp's formatting; elsewhere it reads as plain emphasis.
        return "$intro\n\n*${card.title}*\n${card.body}\n\n${texts.footer}"
    }
}
