package me.anno.ui.input.components

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.getTextSize
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeX
import me.anno.gpu.drawing.GFXx2D.getSizeY
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.MouseButton
import me.anno.io.serialization.NotSerializedProperty
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Strings.getIndexFromText
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.joinChars
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

open class PureTextInput(style: Style) : CorrectingTextInput(style.getChild("edit")) {

    private val characters = IntArrayList(16)

    init {
        instantTextLoading = true
    }

    private var changeListeners = ArrayList<(text: String) -> Unit>()
    private var resetListener: () -> String? = { null }
    private var enterListener: ((text: String) -> Unit)? = null

    var showBars = false
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    fun setCursorToEnd() {
        cursor1 = characters.size
        cursor2 = cursor1
        invalidateLayout()
        invalidateDrawing()
    }

    private fun onChange(text: String) {
        for (listener in changeListeners) {
            listener(text)
        }
    }

    override fun updateChars(notify: Boolean) {
        characters.clear()
        characters.addAll(text.codePoints().toList())
        if (notify) onChange(text)
        invalidateLayout()
    }

    fun updateText(notify: Boolean) {
        text = characters.joinChars().toString()
        if (notify) onChange(text)
        invalidateLayout()
        invalidateDrawing()
    }

    override fun calculateSize(w: Int, h: Int) {
        val text = if (text.isBlank2()) placeholder else text
        val inst = instantTextLoading
        if (inst) loadTexturesSync.push(true)
        super.calculateSize(w, h)
        val size = getTextSize(font, text, widthLimit, heightLimit)
        val w2 = getSizeX(size)
        val h2 = getSizeY(size)
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
            val min = max(min(cursor1, cursor2), 0)
            val max = min(max(cursor1, cursor2), characters.size)
            if (max != min) {
                for (i in max - 1 downTo min) {
                    characters.removeAt(i)
                }
                updateText(true)
                cursor1 = min
                cursor2 = min
                invalidateDrawing()
            }
            return max > min
        }
    }

    @NotSerializedProperty
    private var lastChange = 0L

    val blinkVisible get() = (((GFX.gameTime - lastChange) / 500_000_000L).and(1) == 0L)
    val wasJustChanged get() = GFX.gameTime - lastChange < 200_000_000

    fun calculateOffset(required: Int, cursor: Int) {
        // center the cursor, 1/3 of the width, if possible;
        // clamp left/right
        drawingOffset = if (isDragging) 0 else -clamp(cursor - w / 3, 0, max(0, required - w))
    }

    override fun tickUpdate() {
        val lastShowBars = showBars
        showBars = isInFocus && blinkVisible
        if (showBars != lastShowBars) invalidateDrawing()
    }

    override val isShowingPlaceholder: Boolean
        get() = text.isBlank2()

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(true)
        drawBackground()
        val x = x + padding.left
        val y = y + padding.top
        val usePlaceholder = text.isBlank2()
        val textColor = if (usePlaceholder) placeholderColor else effectiveTextColor
        val drawnText = if (usePlaceholder) placeholder else text
        val wh = drawText(drawingOffset, 0, drawnText, textColor)
        if (isInFocus && (showBars || isSomethingSelected())) {
            ensureCursorBounds()
            val textSize = font.sizeInt
            val padding = textSize / 4
            // to do cache sizes... (low priority, because it has to be in focus for this calculation, so this calculation is rather rare)
            val cursorX1 =
                if (cursor1 == 0) -1
                else getTextSizeX(font, characters.joinChars(0, cursor1), -1, -1) - 1
            if (cursor1 != cursor2) {
                val cursorX2 = if (cursor2 == 0) -1
                else getTextSizeX(font, characters.joinChars(0, cursor2), -1, -1) - 1
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
                calculateOffset(getSizeX(wh), cursorX2)
            } else {
                calculateOffset(getSizeX(wh), cursorX1)
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
            for (it in insertion.codePoints()) {
                characters.add(cursor1++, it)
            }
            cursor2 = cursor1
            updateText(true)
            ensureCursorBounds()
            invalidateDrawing()
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
        invalidateDrawing()
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
        invalidateDrawing()
    }

    fun deleteAfter() {
        lastChange = GFX.gameTime
        if (!deleteSelection() && cursor1 < characters.size) {
            characters.removeAt(cursor1)
            updateText(true)
        }
        ensureCursorBounds()
        invalidateDrawing()
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
        invalidateDrawing()
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
        invalidateDrawing()
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        deleteBefore()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        deleteAfter()
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if (isNothingSelected() || isEverythingSelected()) return text
        return characters.joinChars(min(cursor1, cursor2), max(cursor1, cursor2)).toString()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        insert(data)
    }

    fun addChangeListener(listener: (String) -> Unit): PureTextInput {
        changeListeners += listener
        return this
    }

    fun setResetListener(listener: () -> String?): PureTextInput {
        resetListener = listener
        return this
    }

    fun setEnterListener(listener: (String) -> Unit): PureTextInput {
        enterListener = listener
        return this
    }

    override fun setCursor(position: Int) {
        cursor1 = position
        cursor2 = position
        ensureCursorBounds()
        invalidateDrawing()
    }

    override fun onCharTyped2(x: Float, y: Float, key: Int) {
        lastChange = GFX.gameTime
        addKey(key)
    }

    override fun onEnterKey2(x: Float, y: Float) {
        enterListener?.invoke(text) ?: uiParent?.onEnterKey(x, y)
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
        invalidateDrawing()
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
        invalidateDrawing()
    }

    override fun onDoubleClick(x: Float, y: Float, button: MouseButton) {
        selectAll()
        invalidateDrawing()
    }

    override fun onSelectAll(x: Float, y: Float) {
        selectAll()
    }

    fun isNothingSelected() = cursor1 == cursor2
    fun isEverythingSelected() = cursor1 == 0 && cursor2 == characters.size
    fun isSomethingSelected() = cursor1 != cursor2

    override fun onEmpty(x: Float, y: Float) {
        if (isNothingSelected() || isEverythingSelected()) {
            clear()
        } else {
            deleteSelection()
        }
    }

    fun clear() {
        lastChange = GFX.gameTime
        text = resetListener() ?: ""
        updateChars(false)
        cursor1 = characters.size
        cursor2 = cursor1
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

    override fun clone(): PureTextInput {
        val clone = PureTextInput(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as PureTextInput
        clone.showBars = showBars
        clone.cursor1 = cursor1
        clone.cursor2 = cursor2
        clone.placeholder = placeholder
        clone.placeholderColor = placeholderColor
    }

    override val className get() = "PureTextInput"

}