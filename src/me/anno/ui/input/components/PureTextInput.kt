package me.anno.ui.input.components

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.fonts.Codepoints.codepoints
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isLeftDown
import me.anno.input.Key
import me.anno.maths.Maths.clamp
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.scrolling.ScrollPanelX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Strings.getIndexFromText
import me.anno.utils.types.Strings.joinChars

open class PureTextInput(style: Style) :
    ScrollPanelX(Panel(style), Padding(0), style),
    InputPanel<String>, TextStyleable {

    companion object {

        var lastChangeTime = 0L
        val blinkVisible get() = !((Time.nanoTime - lastChangeTime) ushr 29).hasFlag(1L)

        fun notifyCursorTyped() {
            lastChangeTime = Time.nanoTime
        }
    }

    val content = PureTextInputLine(this)

    init {
        child = content
    }

    val cursor1 = CursorPosition1D(0)
    val cursor2 = CursorPosition1D(0)

    var placeholder = ""
        set(value) {
            field = value
            if (text.isEmpty()) update(false)
        }

    private var text = ""

    val lineLimit: Int get() = 1
    var lengthLimit = Int.MAX_VALUE // todo respect this property

    init {
        content.focusTextColor = style.getColor("textColorFocused", -1)
    }

    var focusTextColor: Int
        get() = content.focusTextColor
        set(value) {
            content.focusTextColor = value
        }

    var enableSpellcheck
        get() = content.enableSpellcheck
        set(value) {
            content.enableSpellcheck = value
        }

    private var enterListener: ((text: String) -> Unit)? = null
    private var resetListener: (() -> String?)? = null

    override fun requestFocus(exclusive: Boolean) {
        super.requestFocus(exclusive)
        notifyCursorTyped()
    }

    fun setEnterListener(listener: (text: String) -> Unit): PureTextInput {
        enterListener = listener
        return this
    }

    fun setResetListener(listener: () -> String?): PureTextInput {
        resetListener = listener
        return this
    }

    fun addChangeListener(listener: (text: String) -> Unit): PureTextInput {
        changeListeners += listener
        return this
    }

    override var textColor
        get() = content.textColor
        set(value) {
            content.textColor = value
        }

    override var textSize: Float
        get() = content.textSize
        set(value) {
            content.textSize = value
        }

    override var isBold: Boolean
        get() = content.isBold
        set(value) {
            content.isBold = value
        }

    override var isItalic: Boolean
        get() = content.isItalic
        set(value) {
            content.isItalic = value
        }

    override var isInputAllowed = true

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            super.isEnabled = value
            content.isEnabled = value
        }

    override val value: String
        get() = lastText

    override fun setValue(newValue: String, mask: Int, notify: Boolean): PureTextInput {
        setText(newValue, notify)
        return this
    }

    @NotSerializedProperty
    private var lastText = ""

    private val changeListeners = ArrayList<(text: String) -> Unit>()

    var line: MutableList<Int> = mutableListOf()

    private val endCursor get() = CursorPosition1D(line.size)
    private val joinedText get() = line.joinChars().toString()

    val styleSample get() = content

    private fun updateLines() {
        val needsPlaceholder = text.isEmpty()
        val chars = line
        val content = content
        content.text = if (needsPlaceholder) placeholder else chars.joinChars().toString()
        content.textColor = content.textColor.withAlpha(if (needsPlaceholder) 70 else 255)
        content.disableFocusColors()
    }

    override fun calculateSize(w: Int, h: Int) {
        loadTexturesSync.push(true)
        super.calculateSize(w, h)
        loadTexturesSync.pop()
    }

    override val canDrawOverBorders: Boolean
        get() = true

    private val showBars get() = isAnyChildInFocus && blinkVisible
    private var loadTextSync = false
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(loadTextSync)
        super.draw(x0, y0, x1, y1)
        val font = content.font
        val textSize = font.sizeInt
        val textColor = content.textColor or black
        val isReallyInFocus = isAnyChildInFocus
        if (isReallyInFocus && (showBars || cursor1 != cursor2)) {
            ensureCursorBounds()
            val padding = textSize / 4
            val panel1 = content as TextPanel
            val line1 = line
            val cursor1Text = line1.joinChars(0, cursor1.x)
            val cursorX1 = if (cursor1.x == 0) 0 else getTextSizeX(font, cursor1Text) - 1
            if (cursor1 != cursor2) {
                val panel2 = content as TextPanel
                val line2 = line
                val cursor2Text = line2.joinChars(0, cursor2.x)
                val cursorX2 = if (cursor2.x == 0) 0 else getTextSizeX(font, cursor2Text) - 1
                val minCursorX = kotlin.math.min(cursorX1, cursorX2)
                val maxCursorX = kotlin.math.max(cursorX1, cursorX2)

                // draw box in same line
                drawRect(
                    panel2.x + minCursorX + panel2.padding.left,
                    panel2.y + padding,
                    maxCursorX - minCursorX,
                    panel2.height - 2 * padding,
                    textColor and 0x3fffffff
                ) // marker

                if (showBars) drawRect(
                    panel2.x + cursorX2 + panel2.padding.left - 1,
                    panel2.y + padding,
                    2,
                    panel2.height - 2 * padding,
                    textColor
                )
                // cursor 1
            }
            if (showBars) drawRect(
                panel1.x + cursorX1 + panel1.padding.left - 1,
                panel1.y + padding,
                2,
                panel1.height - 2 * padding,
                textColor
            )
            // cursor 2
        }

        loadTexturesSync.pop()
    }

    fun <V : Comparable<V>> min(a: V, b: V): V = if (a < b) a else b
    fun <V : Comparable<V>> max(a: V, b: V): V = if (a > b) a else b

    fun setCursorToEnd() {
        cursor1.set(endCursor)
        cursor2.set(cursor1)
    }

    fun setText(text: String, notify: Boolean) {
        if (text == this.text) return
        this.text = text
        content.text = text
        line = text.codepoints().toMutableList()
        update(notify)
    }

    private fun updateText(notify: Boolean) {
        val previousText = lastText
        text = joinedText
        if (text != previousText) {
            lastText = text
            if (notify) {
                for (changeListener in changeListeners) {
                    changeListener(text)
                }
            }
        }
    }

    fun deleteSelection(): Boolean {
        ensureCursorBounds()
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        if (min == max) return false
        // delete in between
        line.subList(min.x, max.x).clear()
        cursor1.set(min)
        cursor2.set(min)
        updateText(true)
        return true
    }

    fun addKey(codePoint: Int) {
        insert(codePoint, true)
    }

    fun insert(insertion: CharSequence) {
        if (insertion.isNotEmpty()) {
            notifyCursorTyped()
            for (cp in insertion.codepoints()) {
                // break if length limit reached
                if (insert(cp, false)) break
            }
            update(true)
        }
    }

    fun insert(char: Int) {
        insert(char.joinChars())
    }

    fun update(notify: Boolean) {
        updateText(notify)
        updateLines()
    }

    /**
     * returns if the length limit was reached
     * */
    fun insert(insertion: Int, notify: Boolean): Boolean {
        notifyCursorTyped()
        deleteSelection()
        // cancel, if length limit reached
        if (line.size >= lengthLimit) {
            return true
        }
        when (insertion) {
            '\n'.code -> {
                // ^^ -> meh for char-input...
                insert('\\'.code, notify)
                insert('n'.code, notify)
            }
            '\r'.code -> {
            } // ignore, because it's useless ^^
            else -> {
                // just insert the key :)
                line.add(cursor1.x, insertion)
                cursor1.set(cursor1.x + 1)
                cursor2.set(cursor1)
            }
        }
        ensureCursorBounds()
        if (notify) {
            update(true)
        }
        return false
    }

    fun deleteBefore(force: Boolean) {
        if (lastDelete != Time.frameTimeNanos || force) {
            lastDelete = Time.frameTimeNanos
            notifyCursorTyped()
            if (!deleteSelection() && cursor1.x > 0) {
                // remove a char
                line.removeAt(cursor1.x - 1)
                cursor1.set(cursor1.x - 1)
                cursor2.set(cursor1)
            }
            ensureCursorBounds()
            update(true)
        }
    }

    fun deleteAfter(force: Boolean) {
        if (lastDelete != Time.frameTimeNanos || force) {
            lastDelete = Time.frameTimeNanos
            if (cursor1 != cursor2) {
                deleteSelection()
                update(true)
            } else {
                val oldCursor = cursor1.hashCode()
                moveRight()
                if (oldCursor != cursor1.hashCode()) {
                    deleteBefore(true)
                }
            }
        }
    }

    fun ensureCursorBounds() {
        cursor1.set(ensureCursorBounds(cursor1))
        cursor2.set(ensureCursorBounds(cursor2))
    }

    private fun ensureCursorBounds(cursor0: CursorPosition1D): CursorPosition1D {
        var cursor = cursor0
        val line = line
        if (cursor.x !in 0..line.size) {
            cursor = CursorPosition1D(clamp(cursor.x, 0, line.size))
        }
        return cursor
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (isInputAllowed && !Input.skipCharTyped(codepoint)) {
            notifyCursorTyped()
            addKey(codepoint)
        } else super.onCharTyped(x, y, codepoint)
    }

    private fun moveRight() {
        val isSelectingText = Input.isShiftDown
        val oldCursor = if (isSelectingText) cursor2 else max(cursor1, cursor2)
        val newCursor = if (!isSelectingText && cursor1 != cursor2) {
            // remove current selection first
            oldCursor
        } else {
            // we can move right
            CursorPosition1D(min(oldCursor.x + 1, line.size))
        }
        if (isSelectingText) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        notifyCursorTyped()
    }

    private fun moveLeft() {
        val isSelecting = Input.isShiftDown
        val oldCursor = if (isSelecting) cursor2 else min(cursor1, cursor2)
        val newCursor = if (!isSelecting && cursor1 != cursor2) {
            // remove current selection first
            oldCursor
        } else {
            // move left
            CursorPosition1D(max(oldCursor.x - 1, 0))
        }
        if (isSelecting) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        notifyCursorTyped()
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if (cursor1 == cursor2) return text
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        return line.joinChars(min.x, max.x).toString()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        insert(data)
        dragged = null
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (!isHovered || dragged != null) return super.onMouseMoved(x, y, dx, dy)
        if (!isControlDown && isLeftDown) {
            val localX = x - (content.x + padding.left)
            val indexX = getIndexFromText(line, localX, styleSample)
            cursor2.set(indexX)
            ensureCursorBounds()
            super.onMouseMoved(x, y, 0f, dy) // dx was consumed, kinda
        } else super.onMouseMoved(x, y, dx, dy)
    }

    fun getCursor(x: Float, withOffset: Boolean): CursorPosition1D {
        var ix = x
        if (withOffset) ix -= content.x + padding.left
        val indexX = getIndexFromText(line, ix, styleSample)
        return CursorPosition1D(indexX)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if ((!isHovered) || key != Key.BUTTON_LEFT) {
            super.onKeyDown(x, y, key)
        } else {
            if (isControlDown) {
                selectAll()
            } else {
                // find the correct location for the cursor
                cursor1.set(getCursor(x, true))
                cursor2.set(cursor1)
                ensureCursorBounds()
            }
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        selectAll()
    }

    override fun onSelectAll(x: Float, y: Float) {
        selectAll()
    }

    private fun selectAll() {
        cursor1.set(0)
        cursor2.set(endCursor)
        ensureCursorBounds()
    }

    override fun onEmpty(x: Float, y: Float) {
        if (isInputAllowed) {
            if (isNothingSelected()) {
                resetToDefault()
            } else {
                // empty selection
                deleteSelection()
                update(true)
            }
        } else super.onEmpty(x, y)
    }

    fun isNothingSelected(): Boolean {
        return cursor1 == cursor2
    }

    fun resetToDefault() {
        val newText = resetListener?.invoke() ?: ""
        setText(newText, true)
        setCursorToEnd()
    }

    private fun clearText() {
        setText("", true)
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DeleteAfter" -> if (isInputAllowed) deleteAfter(false)
            "DeleteBefore" -> if (isInputAllowed) deleteBefore(false)
            "DeleteSelection" -> if (isInputAllowed) deleteSelection()
            "MoveLeft" -> moveLeft()
            "MoveRight" -> moveRight()
            "Clear" -> if (isInputAllowed) clearText()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun getCursor() = Cursor.editText
    override fun isKeyInput() = true

    init {
        update(false)
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        if (!isInputAllowed) return
        deleteBefore(false)
    }

    private var lastDelete = 0L

    override fun onEnterKey(x: Float, y: Float) {
        val listener = enterListener
        if (listener != null) listener.invoke(text)
        else super.onEnterKey(x, y)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        if (!isInputAllowed) return
        deleteAfter(false)
    }

    override fun clone(): PureTextInput {
        val clone = PureTextInput(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is PureTextInput) return
        dst.setValue(text, false)
        dst.cursor1.set(cursor1)
        dst.cursor2.set(cursor2)
        dst.placeholder = placeholder
        dst.focusTextColor = focusTextColor
    }
}