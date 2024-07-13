package me.anno.ui.editor.code

import me.anno.language.spellcheck.Spellchecking

class TextSection(var startIndex: Int, var endIndex: Int, var text: CharSequence) {
    override fun toString(): String = "[$startIndex until $endIndex]('$text')"
    operator fun contains(index: Int) = index in startIndex until endIndex
    fun spellcheck() = Spellchecking.check(text, false)
}