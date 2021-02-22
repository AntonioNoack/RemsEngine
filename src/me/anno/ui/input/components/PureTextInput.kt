package me.anno.ui.input.components

import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.GFXx2D.drawRect
import me.anno.gpu.GFXx2D.getTextSize
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.MouseButton
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.structures.tuples.Quad
import me.anno.utils.types.Strings.getIndexFromText
import me.anno.utils.types.Strings.joinChars
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

open class PureTextInput(style: Style) : CorrectingTextInput(style.getChild("edit")) {

    val characters = ArrayList<Int>()

    init {
        instantTextLoading = true
    }

    fun setCursorToEnd() {
        cursor1 = characters.size
        cursor2 = cursor1
    }

    override fun updateChars(notify: Boolean) {
        characters.clear()
        characters.addAll(text.codePoints().toList())
        if (notify) changeListener(text)
    }

    fun updateText(notify: Boolean) {
        text = characters.joinChars()
        if (notify) changeListener(text)
    }

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isBlank()) placeholder else text
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        super.calculateSize(w, h)
        val (w2, h2) = getTextSize(font, text, widthLimit)
        minW = max(1, w2 + padding.width)
        minH = max(1, h2 + padding.height)
        minW2 = minW
        minH2 = minH
        if (inst) loadTexturesSync.pop()
    }

    var cursor1 = 0
    var cursor2 = 0

    var placeholderColor = style.getColor("placeholderColor", textColor and 0x7fffffff)
    var placeholder = ""

    fun deleteSelection(): Boolean {
        synchronized(this) {
            val min = min(cursor1, cursor2)
            val max = max(cursor1, cursor2)
            if (max != min) {
                for (i in max - 1 downTo min) {
                    characters.removeAt(i)
                }
                updateText(true)
                cursor1 = min
                cursor2 = min
            }
            return max > min
        }
    }

    var lastChange = 0L
    val wasJustChanged get() = abs(GFX.gameTime - lastChange) < 200_000_000

    fun calculateOffset(required: Int, cursor: Int) {
        // center the cursor, 1/3 of the width, if possible;
        // clamp left/right
        drawingOffset = if(isDragging) 0 else -clamp(cursor - w / 3, 0, max(0, required - w))
    }

    var showBars = false
    override fun getVisualState(): Any? = Quad(
        super.getVisualState(),
        Pair(showBars, drawingOffset),
        cursor1,
        cursor2
    )

    override fun tickUpdate() {
        super.tickUpdate()
        val blinkVisible = ((GFX.gameTime / 500_000_000L) % 2L == 0L)
        showBars = isInFocus && (blinkVisible || wasJustChanged)
    }

    override val isShowingPlaceholder: Boolean
        get() = text.isBlank()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(true)
        drawBackground()
        val x = x + padding.left
        val y = y + padding.top
        val usePlaceholder = text.isBlank()
        val textColor = if (usePlaceholder) placeholderColor else effectiveTextColor
        val drawnText = if (usePlaceholder) placeholder else text
        val wh = drawText(drawingOffset, 0, drawnText, textColor)
        if (isInFocus && (showBars || cursor1 != cursor2)) {
            ensureCursorBounds()
            val textSize = font.sizeInt
            val padding = textSize / 4
            // to do cache sizes... (low priority, because it has to be in focus for this calculation, so this calculation is rather rare)
            val cursorX1 =
                if (cursor1 == 0) -1 else getTextSize(font, characters.subList(0, cursor1).joinChars(), -1).first - 1
            if (cursor1 != cursor2) {
                val cursorX2 = if (cursor2 == 0) -1 else getTextSize(
                    font,
                    characters.subList(0, cursor2).joinChars(),
                    -1
                ).first - 1
                val min = min(cursorX1, cursorX2)
                val max = max(cursorX1, cursorX2)
                drawRect(
                    x + min + drawingOffset,
                    y + padding,
                    max - min,
                    h - 2 * padding,
                    textColor and 0x3fffffff
                ) // marker
                if (showBars) drawRect(
                    x + cursorX2 + drawingOffset,
                    y + padding,
                    2,
                    h - 2 * padding,
                    textColor
                ) // cursor 1
                calculateOffset(wh.first, cursorX2)
            } else {
                calculateOffset(wh.first, cursorX1)
            }
            if (showBars) drawRect(x + cursorX1 + drawingOffset, y + padding, 2, h - 2 * padding, textColor) // cursor 2
        }
        drawSuggestionLines()
        loadTexturesSync.pop()
    }

    fun addKey(codePoint: Int) = insert(codePoint)

    fun insert(insertion: String) {
        if (insertion.isNotEmpty()) {
            lastChange = GFX.gameTime
            deleteSelection()
            insertion.codePoints().forEach {
                characters.add(cursor1++, it)
            }
            cursor2 = cursor1
            updateText(true)
            ensureCursorBounds()
        }
    }

    fun insert(insertion: Int, updateText: Boolean = true) {
        lastChange = GFX.gameTime
        deleteSelection()
        characters.add(cursor1, insertion)
        if (updateText) updateText(true)
        cursor1++
        cursor2++
        ensureCursorBounds()
    }

    fun deleteBefore() {
        lastChange = GFX.gameTime
        if (!deleteSelection() && cursor1 > 0) {
            characters.removeAt(cursor1 - 1)
            updateText(true)
            cursor1--
            cursor2--
        }
        ensureCursorBounds()
    }

    fun deleteAfter() {
        lastChange = GFX.gameTime
        if (!deleteSelection() && cursor1 < characters.size) {
            characters.removeAt(cursor1)
            updateText(true)
        }
        ensureCursorBounds()
    }

    fun ensureCursorBounds() {
        val maxLength = characters.size
        cursor1 = clamp(cursor1, 0, maxLength)
        cursor2 = clamp(cursor2, 0, maxLength)
    }

    fun moveRight() {
        lastChange = GFX.gameTime
        if (isShiftDown) {
            cursor2++
        } else {
            if (cursor2 != cursor1) {
                cursor1 = cursor2
            } else {
                cursor1++
                cursor2 = cursor1
            }
        }
        ensureCursorBounds()
    }

    fun moveLeft() {
        lastChange = GFX.gameTime
        if (isShiftDown) {
            cursor2--
        } else {
            cursor1--
            cursor2 = cursor1
        }
        ensureCursorBounds()
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        deleteBefore()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        deleteAfter()
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if (cursor1 == cursor2) return text
        return characters.subList(min(cursor1, cursor2), max(cursor1, cursor2)).joinChars()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        insert(data)
    }

    private var changeListener: (text: String) -> Unit = { _ -> }
    fun setChangeListener(listener: (String) -> Unit): PureTextInput {
        changeListener = listener
        return this
    }

    private var enterListener: ((text: String) -> Unit)? = null
    fun setEnterListener(listener: (String) -> Unit): PureTextInput {
        enterListener = listener
        return this
    }

    override fun setCursor(position: Int) {
        cursor1 = position
        cursor2 = position
        ensureCursorBounds()
    }

    override fun onCharTyped2(x: Float, y: Float, key: Int) {
        lastChange = GFX.gameTime
        addKey(key)
    }

    override fun onEnterKey2(x: Float, y: Float) {
        enterListener?.invoke(text) ?: parent?.onEnterKey(x, y)
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        lastChange = GFX.gameTime
        if (isControlDown) {
            selectAll()
        } else {
            // find the correct location for the cursor
            val localX = x - (this.x + padding.left + drawingOffset)
            cursor1 = getIndexFromText(characters, localX, font)
            cursor2 = cursor1
        }
    }

    fun selectAll() {
        cursor1 = 0
        cursor2 = characters.size
    }

    var isDragging = false
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (!isControlDown) {
            if (0 in mouseKeysDown) {
                val localX = x - (this.x + padding.left + drawingOffset)
                cursor2 = getIndexFromText(characters, localX, font)
                isDragging = true
            } else isDragging = false
        } else isDragging = false
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        cursor1 = 0
        cursor2 = characters.size
    }

    override fun onSelectAll(x: Float, y: Float) {
        cursor1 = 0
        cursor2 = characters.size
    }

    override fun onEmpty(x: Float, y: Float) {
        if (text != "") {
            if (cursor1 == cursor2) {
                clear()
            } else deleteSelection()
        }
    }

    fun clear() {
        lastChange = GFX.gameTime
        text = ""
        characters.clear()
        cursor1 = 0
        cursor2 = 0
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "DeleteAfter" -> deleteAfter()
            "DeleteBefore" -> deleteBefore()
            "DeleteSelection" -> deleteSelection()
            "MoveLeft" -> moveLeft()
            "MoveRight" -> moveRight()
            "Clear" -> clear()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun acceptsChar(char: Int): Boolean {
        return when (char.toChar()) {
            '\n' -> false
            else -> true
        }
    }

    override var enableHoverColor: Boolean
        get() = text.isNotEmpty()
        set(_) {}

    override fun getClassName() = "PureTextInput"

}