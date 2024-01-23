package me.anno.ui.input.components

import me.anno.ui.Style

fun PureTextInput(style: Style): PureTextInputML {
    val panel = PureTextInputML(style)
    panel.lineLimit = 1
    return panel
}