package me.anno.ui.input.components

import me.anno.ui.style.Style

open class PureTextInput(style: Style) : PureTextInputML(style.getChild("edit")) {

    init {
        lineLimit = 1
    }

    override val className get() = "PureTextInput"

}