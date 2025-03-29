package me.anno.ui.input.components

import me.anno.fonts.Codepoints.codepoints
import me.anno.input.Key
import me.anno.utils.Color.mixARGB

class PureTextInputLine(val self: PureTextInputML) : CorrectingTextPanel(self.style) {

    override val effectiveTextColor: Int
        get() = if (self.isInputAllowed && isEnabled) super.effectiveTextColor else
            mixARGB(textColor, backgroundColor, 0.5f)

    override val isShowingPlaceholder: Boolean
        get() = self.value.isEmpty()

    override fun onCharTyped2(x: Float, y: Float, key: Int) = self.onCharTyped(x, y, key)
    override fun onEnterKey2(x: Float, y: Float) = self.onEnterKey(x, y)
    override fun onKeyDown(x: Float, y: Float, key: Key) = self.onKeyDown(x, y, key)
    override fun onKeyUp(x: Float, y: Float, key: Key) = self.onKeyUp(x, y, key)

    override fun setCursor(position: Int) {
        // set cursor after replacement
        if (self.cursor1 != self.cursor2 || self.cursor1.x != position || self.cursor1.y != indexInParent) {
            self.cursor1.set(position, indexInParent)
            self.cursor2.set(self.cursor1)
        }
    }

    override fun updateChars(notify: Boolean) {
        // replace chars in main string...
        // convert text back to lines
        self.lines[indexInParent] = text.codepoints().toMutableList()
        self.update(true)
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return self.onCopyRequested(x, y)
    }

    override fun isKeyInput(): Boolean {
        return self.isKeyInput()
    }

    override fun acceptsChar(char: Int): Boolean {
        return self.acceptsChar(char)
    }

    override val className: String get() = "PureTextInputLine"
}