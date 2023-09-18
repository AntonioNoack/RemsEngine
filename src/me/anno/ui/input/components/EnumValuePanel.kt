package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Key
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput
import me.anno.ui.Style

class EnumValuePanel(title: String, private var owner: EnumInput, style: Style) :
    TextPanel(title, style.getChild("italic")) {

    constructor(base: EnumValuePanel) : this(base.text, base.owner, base.style)

    override fun acceptsChar(char: Int): Boolean {
        val asKey = Key.byId(char)
        return asKey.isUpKey() || asKey.isDownKey()
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        if (key.isDownKey()) owner.moveDown(1)
        else if (key.isUpKey()) owner.moveDown(-1)
    }

    override fun isKeyInput() = true

    override fun clone() = EnumValuePanel(this)

    override fun getCursor(): Long? {
        return if (owner.isInputAllowed) super.getCursor()
        else null
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as EnumValuePanel
        dst.text = text
        dst.owner = getInClone(owner, dst) ?: owner
    }
}