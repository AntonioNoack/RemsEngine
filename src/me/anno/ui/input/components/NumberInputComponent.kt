package me.anno.ui.input.components

import me.anno.ecs.annotations.Docs
import me.anno.ui.Style
import me.anno.ui.input.InputVisibility

open class NumberInputComponent(val visibilityKey: String, style: Style) :
    PureTextInputML(style.getChild("deep")) {

    init {
        setResetListener { "0.0" }
        enableSpellcheck = false
        lineLimit = 1
    }

    @Docs("Used in Rem's Studio")
    @Suppress("unused")
    var indexInProperty = 0

    override var isVisible: Boolean
        get() = InputVisibility[visibilityKey] // can be null in constructor
        set(_) {}

    override fun acceptsChar(char: Int): Boolean {
        return when (char.toChar()) {
            '\t', '\n' -> false
            else -> true
        }
    }

    override fun clone(): NumberInputComponent {
        val clone = NumberInputComponent(visibilityKey, style)
        copyInto(clone)
        return clone
    }
}