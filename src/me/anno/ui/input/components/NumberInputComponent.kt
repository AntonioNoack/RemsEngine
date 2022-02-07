package me.anno.ui.input.components

import me.anno.ui.base.Visibility
import me.anno.ui.input.InputVisibility
import me.anno.ui.style.Style

open class NumberInputComponent(
    val visibilityKey: String,
    style: Style
) : PureTextInput(style.getChild("deep")) {

    init {
        setResetListener { "0.0" }
    }

    override val enableSpellcheck: Boolean
        get() = false

    override var visibility: Visibility
        get() = InputVisibility[visibilityKey]
        set(_) {}

    override fun acceptsChar(char: Int): Boolean {
        return when (char.toChar()) {
            '\t', '\n' -> false
            else -> true
        }
    }

    override fun clone(): NumberInputComponent {
        val clone = NumberInputComponent(visibilityKey, style)
        copy(clone)
        return clone
    }

}