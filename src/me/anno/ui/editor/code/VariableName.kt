package me.anno.ui.editor.code

import me.anno.language.spellcheck.Spellchecking

class VariableName(var startIndex: Int, var endIndex: Int, var text: String) {
    override fun toString(): String = "[$startIndex until $endIndex]('$text')"
    operator fun contains(index: Int) = index in startIndex until endIndex
    fun check() = Spellchecking.check(text, false, this)
     //   ?.filter { it.start > 1 || !it.clearMessage.startsWith("This sentence does not start with") }
}