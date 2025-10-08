package me.anno.ui.input.components

import me.anno.fonts.Codepoints.codepoints
import me.anno.input.Key
import me.anno.ui.Panel
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.mixARGB

class PureTextInputLine(val owner: Panel) : CorrectingTextPanel(owner.style) {

    override val effectiveTextColor: Int
        get() = if ((owner as InputPanel<*>).isInputAllowed && isEnabled) super.effectiveTextColor else
            mixARGB(textColor, backgroundColor, 0.5f)

    @Suppress("UNCHECKED_CAST")
    override val isShowingPlaceholder: Boolean
        get() = (owner as InputPanel<String>).value.isEmpty()

    override fun onCharTyped2(x: Float, y: Float, key: Int) = owner.onCharTyped(x, y, key)
    override fun onEnterKey2(x: Float, y: Float) = owner.onEnterKey(x, y)
    override fun onKeyDown(x: Float, y: Float, key: Key) = owner.onKeyDown(x, y, key)
    override fun onKeyUp(x: Float, y: Float, key: Key) = owner.onKeyUp(x, y, key)

    override fun setCursor(position: Int) {
        // set cursor after replacement
        when (owner) {
            is PureTextInput -> {
                if (owner.cursor1 != owner.cursor2 || owner.cursor1.x != position) {
                    owner.cursor1.set(position)
                    owner.cursor2.set(owner.cursor1)
                }
            }
            is PureTextInputML -> {
                if (owner.cursor1 != owner.cursor2 || owner.cursor1.x != position || owner.cursor1.y != indexInParent) {
                    owner.cursor1.set(position, indexInParent)
                    owner.cursor2.set(owner.cursor1)
                }
            }
        }
    }

    override fun updateChars(notify: Boolean) {
        when (owner) {
            is PureTextInput -> {
                // replace chars in main string...
                // convert text back to lines
                owner.line = text.codepoints().toMutableList()
                owner.update(true)
            }
            is PureTextInputML -> {
                // replace chars in main string...
                // convert text back to lines
                owner.lines[indexInParent] = text.codepoints().toMutableList()
                owner.update(true)
            }
        }
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return owner.onCopyRequested(x, y)
    }

    override fun isKeyInput(): Boolean {
        return owner.isKeyInput()
    }

    override fun acceptsChar(char: Int): Boolean {
        return owner.acceptsChar(char)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        return when (action) {
            "MoveUp", "MoveDown" -> owner
            else -> uiParent ?: return false
        }.onGotAction(x, y, dx, dy, action, isContinuous)
    }

    override val className: String get() = "PureTextInputLine"
}