package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.ui.style.Style
import me.anno.ui.Keys.isDownKey
import me.anno.ui.Keys.isUpKey

class EnumValuePanel(title: String, private var owner: EnumInput, style: Style) :
    TextPanel(title, style.getChild("italic")) {

    constructor(base: EnumValuePanel) : this(base.text, base.owner, base.style)

    override fun acceptsChar(char: Int) = char.isUpKey() || char.isDownKey()
    override fun onKeyTyped(x: Float, y: Float, key: Int) {
        if (key.isDownKey()) owner.moveDown(1)
        else if (key.isUpKey()) owner.moveDown(-1)
    }

    override fun isKeyInput() = true

    override fun clone() = EnumValuePanel(this)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as EnumValuePanel
        clone.text = text
        clone.owner = getInClone(owner, clone) ?: owner
    }

}