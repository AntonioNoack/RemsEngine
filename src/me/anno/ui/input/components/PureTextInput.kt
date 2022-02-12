package me.anno.ui.input.components

import me.anno.ui.style.Style

open class PureTextInput(style: Style) : PureTextInputML(style.getChild("edit")) {

    init {
        lineLimit = 1
    }

    override fun clone(): PureTextInput {
        val clone = PureTextInput(style)
        copy(clone)
        return clone
    }

    override val className get() = "PureTextInput"

}