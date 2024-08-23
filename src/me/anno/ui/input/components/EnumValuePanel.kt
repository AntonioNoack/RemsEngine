package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel
import me.anno.ui.input.EnumInput

class EnumValuePanel(nameDesc: NameDesc, private var owner: EnumInput, style: Style) :
    TextPanel(nameDesc, style.getChild("italic")) {

    override fun acceptsChar(char: Int): Boolean {
        val asKey = Key.byId(char)
        return asKey.isUpKey() || asKey.isDownKey()
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
        if (key.isDownKey()) owner.moveDown(1)
        else if (key.isUpKey()) owner.moveDown(-1)
    }

    override fun isKeyInput() = true

    override fun clone(): EnumValuePanel {
        val clone = EnumValuePanel(NameDesc(text, tooltip, ""), owner, style)
        copyInto(clone)
        return clone
    }

    override fun getCursor(): Cursor? {
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