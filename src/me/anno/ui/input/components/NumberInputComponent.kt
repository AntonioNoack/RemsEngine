package me.anno.ui.input.components

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

    override var isVisible: Boolean
        get() = InputVisibility[visibilityKey]
        set(_) {}

    override fun acceptsChar(char: Int): Boolean {
        return when (char.toChar()) {
            '\t', '\n' -> false
            else -> true
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        // don't call parent, as this would prevent dragging a value
        uiParent?.onMouseMoved(x, y, dx, dy)
    }

    override fun clone(): NumberInputComponent {
        val clone = NumberInputComponent(visibilityKey, style)
        copy(clone)
        return clone
    }

}