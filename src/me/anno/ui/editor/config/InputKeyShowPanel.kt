package me.anno.ui.editor.config

import me.anno.input.Input
import me.anno.input.Key
import me.anno.input.Key.Companion.modifierKeys
import me.anno.input.KeyCombination
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.text.TextPanel

/**
 * Panel, which accepts any input, and shows the current key value for ActionManager
 * */
class InputKeyShowPanel(val title: NameDesc, style: Style) : TextPanel(title, style) {

    private val prefix = "${title.name}: "

    override fun onUpdate() {
        super.onUpdate()

        val keys = Input.keysDown.keys.toList()
        val key = keys.firstOrNull { it !in modifierKeys }
            ?: keys.firstOrNull()
            ?: return
        val name = KeyCombination.keyMapping.reverse[key]
        var suffix = (if (Input.isShiftDown) "s" else "") +
                (if (Input.isControlDown) "c" else "") +
                (if (Input.isAltDown) "a" else "")
        suffix = if (suffix.isNotEmpty()) ".t.$suffix" else ".t"
        text =
            if (name != null) "$prefix${key.name}, '$name$suffix'"
            else "$prefix${key.name}"
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
    }

    override fun onKeyTyped(x: Float, y: Float, key: Key) {
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
    }

    override fun acceptsChar(char: Int): Boolean = true
}