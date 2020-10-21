package me.anno.ui.input.components

import me.anno.ui.base.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.utils.isDownKey
import me.anno.utils.isUpKey

class EnumValuePanel(title: String, private val owner: EnumInput, style: Style) :
    TextPanel(title, style.getChild("italic")) {

    override fun acceptsChar(char: Int) = char.isUpKey() || char.isDownKey()
    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (key.isDownKey()) owner.moveDown(1)
        else if (key.isUpKey()) owner.moveDown(-1)
    }

    override fun isKeyInput() = true

}