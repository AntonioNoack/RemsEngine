package me.anno.ui.input.components

import me.anno.Time
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.getTextSizeX
import me.anno.input.Input
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isLeftDown
import me.anno.input.Key
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.maths.Maths.clamp
import me.anno.engine.EngineBase.Companion.dragged
import me.anno.ui.Style
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.InputPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.lists.Lists.firstOrNull2
import me.anno.utils.types.Strings.getIndexFromText
import me.anno.utils.types.Strings.getLineWidth
import me.anno.utils.types.Strings.joinChars
import kotlin.math.abs
import kotlin.streams.toList

// todo hovering over spell correction can reset the cursor (WTF), fix that
open class PureTextInputML(style: Style) :
    ScrollPanelXY(Padding(0), style),
    InputPanel<String>,
    TextStyleable {

    private val cursor1 = CursorPosition(0, 0)
    private val cursor2 = CursorPosition(0, 0)

    var placeholder = ""
        set(value) {
            field = value
            if (text.isEmpty()) update(false)
        }

    private var text = ""
        set(value) {
            if (field != value) {
                field = value
                invalidateLayout()
            }
        }

    var lineLimit = Int.MAX_VALUE

    // todo update all children, when this is overridden
    var focusTextColor = style.getColor("textColorFocused", -1)

    var enableSpellcheck = true
        set(value) {
            if (field != value) {
                field = value
                val children = actualChildren
                for (i in children.indices) {
                    (children[i] as? CorrectingTextPanel ?: continue).enableSpellcheck = value
                }
            }
        }

    private var enterListener: ((text: String) -> Unit)? = null
    private var resetListener: (() -> String?)? = null

    fun setEnterListener(listener: (text: String) -> Unit): PureTextInputML {
        enterListener = listener
        return this
    }

    fun setResetListener(listener: () -> String?): PureTextInputML {
        resetListener = listener
        return this
    }

    fun addChangeListener(listener: (text: String) -> Unit): PureTextInputML {
        changeListeners += listener
        return this
    }

    override var textColor
        get() = styleSample.textColor
        set(value) {
            val children = actualChildren
            for (i in children.indices) {
                (children[i] as TextStyleable).textColor = value
            }
        }

    override var textSize: Float
        get() = styleSample.textSize
        set(value) {
            val children = actualChildren
            for (i in children.indices) {
                (children[i] as TextStyleable).textSize = value
            }
        }

    override var isBold: Boolean
        get() = styleSample.isBold
        set(value) {
            for (p in actualChildren) {
                (p as TextStyleable).isBold = value
            }
        }

    override var isItalic: Boolean
        get() = styleSample.isItalic
        set(value) {
            val children = actualChildren
            for (i in children.indices) {
                (children[i] as TextStyleable).isItalic = value
            }
        }

    override var isInputAllowed = true
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            super.isEnabled = value
            val children = actualChildren
            for (i in children.indices) {
                children[i].isEnabled = value
            }
            invalidateDrawing()
        }

    override val value: String
        get() = lastText

    override fun setValue(newValue: String, mask: Int, notify: Boolean): PureTextInputML {
        setText(newValue, notify)
        return this
    }

    @NotSerializedProperty
    private var lastText = ""

    @NotSerializedProperty
    private var lastChangeTime = 0L
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    private val changeListeners = ArrayList<(text: String) -> Unit>()

    private val lines: ArrayList<MutableList<Int>> = arrayListOf(mutableListOf())
    private val endCursor get() = CursorPosition(lines.last().size, lines.lastIndex)
    private val joinedText get() = lines.joinToString("\n") { list -> list.joinChars() }
    private val actualChildren = (child as PanelListY).children
    private val scrollbarStartY get() = if (minW > width) actualChildren.last().run { y + height - 3 } else y + height
    private val wasJustChanged get() = abs(Time.nanoTime - lastChangeTime) < 200_000_000
    val styleSample get() = actualChildren[0] as TextPanel

    private fun updateLines() {
        val needsPlaceholder = text.isEmpty()
        val children = actualChildren
        if (lines.isEmpty()) {// at least one line always needs to be available
            lines.add(mutableListOf())
        }
        while (lines.size < children.size) {// remove no longer needed TextInput panels
            children.removeAt(children.lastIndex)
        }
        val content = content as PanelListY
        while (lines.size > children.size) {// add new TextInput panels
            val panel = object : CorrectingTextPanel(style) {

                override val className: String get() = "PureTextInputML/CorrectingTextInput"

                override val effectiveTextColor: Int
                    get() = if (isInputAllowed && isEnabled) super.effectiveTextColor else
                        mixARGB(textColor, backgroundColor, 0.5f)

                override val isShowingPlaceholder: Boolean
                    get() = this@PureTextInputML.text.isEmpty()

                override fun onCharTyped2(x: Float, y: Float, key: Int) = this@PureTextInputML.onCharTyped(x, y, key)
                override fun onEnterKey2(x: Float, y: Float) = this@PureTextInputML.onEnterKey(x, y)
                override fun onKeyDown(x: Float, y: Float, key: Key) = this@PureTextInputML.onKeyDown(x, y, key)
                override fun onKeyUp(x: Float, y: Float, key: Key) = this@PureTextInputML.onKeyUp(x, y, key)

                override fun setCursor(position: Int) {
                    // set cursor after replacement
                    if (cursor1 != cursor2 || cursor1.x != position || cursor1.y != indexInParent) {
                        cursor1.set(position, indexInParent)
                        cursor2.set(cursor1)
                        this@PureTextInputML.invalidateDrawing()
                    }
                }

                override fun updateChars(notify: Boolean) {
                    // replace chars in main string...
                    // convert text back to lines
                    lines[indexInParent] = text.codePoints().toList().toMutableList()
                    this@PureTextInputML.update(true)
                }

                override fun onCopyRequested(x: Float, y: Float): Any? {
                    return this@PureTextInputML.onCopyRequested(x, y)
                }
            }
            panel.enableSpellcheck = enableSpellcheck
            content.add(panel)
        }
        for ((index, chars) in lines.withIndex()) {
            val panel = children[index] as TextPanel
            panel.text = if (needsPlaceholder) placeholder else chars.joinChars().toString()
            panel.textColor = panel.textColor.withAlpha(if (needsPlaceholder) 70 else 255)
            panel.disableFocusColors()
        }
        invalidateLayout()
    }

    private var lastClick = 0L
    override fun onUpdate() {
        super.onUpdate()
        val blinkVisible = (((Time.nanoTime - lastClick) / 500_000_000L) % 2L == 0L)
        val isInFocus = isAnyChildInFocus
        val oldShowBars = showBars
        showBars = isInFocus && (blinkVisible || wasJustChanged)
        if (showBars != oldShowBars) invalidateDrawing()
    }

    override fun calculateSize(w: Int, h: Int) {
        loadTexturesSync.push(true)
        super.calculateSize(w, h)
        loadTexturesSync.pop()
    }

    override val canDrawOverBorders: Boolean
        get() = true

    private var showBars = false
    private var loadTextSync = false
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(loadTextSync)
        super.onDraw(x0, y0, x1, y1)
        val children = actualChildren
        val examplePanel = children.firstOrNull2() as? TextPanel ?: return
        val font = examplePanel.font
        val textSize = font.sizeInt
        val textColor = examplePanel.textColor or black
        val isReallyInFocus = isAnyChildInFocus
        if (isReallyInFocus && (showBars || cursor1 != cursor2)) {
            ensureCursorBounds()
            val padding = textSize / 4
            val panel1 = children[cursor1.y] as TextPanel
            val line1 = lines[cursor1.y]
            val cursor1Text = line1.joinChars(0, cursor1.x)
            val cursorX1 = if (cursor1.x == 0) 0 else getTextSizeX(font, cursor1Text, -1, -1, false) - 1
            if (cursor1 != cursor2) {
                val panel2 = children[cursor2.y] as TextPanel
                val line2 = lines[cursor2.y]
                val cursor2Text = line2.joinChars(0, cursor2.x)
                val cursorX2 = if (cursor2.x == 0) 0 else getTextSizeX(font, cursor2Text, -1, -1, false) - 1
                val minCursor = min(cursor1, cursor2)
                val maxCursor = max(cursor1, cursor2)
                val minPanel = children[minCursor.y] as TextPanel
                val maxPanel = children[maxCursor.y] as TextPanel
                val minCursorX = kotlin.math.min(cursorX1, cursorX2)
                val maxCursorX = kotlin.math.max(cursorX1, cursorX2)
                if (minCursor.y == maxCursor.y) {
                    // draw box in same line
                    drawRect(
                        panel2.x + minCursorX + panel2.padding.left,
                        panel2.y + padding,
                        maxCursorX - minCursorX,
                        panel2.height - 2 * padding,
                        textColor and 0x3fffffff
                    ) // marker
                } else {

                    // draw end of first line
                    drawRect(
                        x + panel2.padding.left + minCursorX,
                        minPanel.y + padding,
                        width - panel2.padding.width - minCursorX,
                        minPanel.height - padding,
                        textColor and 0x3fffffff
                    )

                    // draw in between lines
                    if (minCursor.y + 1 < maxCursor.y) {
                        drawRect(
                            x + panel2.padding.left,
                            minPanel.y + minPanel.height,
                            width - panel2.padding.width,
                            maxPanel.y - minPanel.y - minPanel.height,
                            textColor and 0x3fffffff
                        )
                    }

                    // draw start of last line
                    val endX = maxPanel.x + maxCursorX
                    drawRect(
                        x + panel2.padding.left,
                        maxPanel.y,
                        endX - x,
                        maxPanel.height - padding,
                        textColor and 0x3fffffff
                    )
                }
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

    //fun String.toLines() = split('\n')
    //    .map { line -> line.codePoints().toList().toMutableList() }

    fun setText(text: String, notify: Boolean) {
        if (text == this.text) return
        this.text = text
        val textLines = text.split('\n')
        for (i in lines.lastIndex downTo textLines.size - 1) {
            lines.removeAt(i)
        }
        for (lineIndex in textLines.indices) {
            val line = textLines[lineIndex]
            if (lines.size <= lineIndex) lines += line.codePoints().toList().toMutableList()
            else {
                val lineList = lines[lineIndex]
                lineList.clear()
                lineList.addAll(line.codePoints().toList())
            }
        }
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
        if (min.y < max.y) {
            // delete back, in between, and end
            val line0 = lines[min.y]
            lines[min.y] = line0.subList(0, min.x)
            val line1 = lines[max.y]
            lines[max.y] = line1.subList(max.x, line1.size)
            for (i in max.y - 1 downTo min.y + 1) {
                lines.removeAt(i)
            }
        } else {
            // delete in between
            val line = lines[min.y]
            lines[min.y] = (line.subList(0, min.x) + line.subList(max.x, line.size)).toMutableList()
        }
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
            lastChangeTime = Time.nanoTime
            for (cp in insertion.codePoints()) {
                insert(cp, false)
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

    private fun findStartingWhitespace(line: List<Int>): List<Int> {
        var i = 0
        while (i < line.size && when (line[i]) {
                ' '.code, '\t'.code -> true
                else -> false
            }
        ) {
            i++
        }
        return line.subList(0, i)
    }

    fun insert(insertion: Int, notify: Boolean) {
        lastChangeTime = Time.nanoTime
        deleteSelection()
        when (insertion) {
            '\n'.code -> {
                // split the line here
                if (lines.size + 1 < lineLimit) {
                    val line0 = lines[cursor1.y]
                    val line1 = line0.subList(cursor1.x, line0.size)
                    lines[cursor1.y] = line0.subList(0, cursor1.x)
                    lines.add(
                        cursor1.y + 1,
                        (findStartingWhitespace(line0) // help with spaces at the start
                                + line1).toMutableList()
                    )
                    cursor1.set(0, cursor1.y + 1)
                    cursor2.set(cursor1)
                } else {// ^^
                    insert('\\'.code, notify)
                    insert('n'.code, notify)
                }
            }
            '\r'.code -> {
            } // ignore, because it's useless ^^
            else -> {
                // just insert the key :)
                val line = lines[cursor1.y]
                line.add(cursor1.x, insertion)
                cursor1.set(cursor1.x + 1, cursor1.y)
                cursor2.set(cursor1)
            }
        }
        ensureCursorBounds()
        if (notify) {
            update(true)
        }
    }

    fun deleteBefore() {
        lastChangeTime = Time.nanoTime
        if (!deleteSelection() && cursor1.x + cursor1.y > 0) {
            if (cursor1.x == 0) {
                // join lines
                val line0 = lines[cursor1.y - 1]
                val joint = (line0 + lines[cursor1.y]).toMutableList()
                lines[cursor1.y - 1] = joint
                lines.removeAt(cursor1.y)
                cursor1.set(line0.size, cursor1.y - 1)
            } else {
                // remove a char
                lines[cursor1.y].removeAt(cursor1.x - 1)
                cursor1.set(cursor1.x - 1, cursor1.y)
            }
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        update(true)
    }

    fun deleteAfter() {
        if (lastDelete != Time.nanoTime) {
            lastDelete = Time.nanoTime
            if (cursor1 != cursor2) {
                deleteSelection()
                update(true)
            } else {
                moveRight()
                deleteBefore()
            }
        }
    }

    fun ensureCursorBounds() {
        cursor1.set(ensureCursorBounds(cursor1))
        cursor2.set(ensureCursorBounds(cursor2))
    }

    private fun ensureCursorBounds(cursor0: CursorPosition): CursorPosition {
        var cursor = cursor0
        if (cursor.y !in 0 until lines.size) {
            cursor = CursorPosition(cursor.x, clamp(cursor.y, 0, lines.lastIndex))
            // LOGGER.info("changed y from ${cursor0.y} to ${cursor.y}, because ${cursor.y} !in 0 until ${lines.size}")
        }
        val line = lines[cursor.y]
        if (cursor.x !in 0..line.size) {
            cursor = CursorPosition(clamp(cursor.x, 0, line.size), cursor.y)
        }
        return cursor
    }

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (!isInputAllowed) return
        lastChangeTime = Time.nanoTime
        addKey(codepoint)
    }

    private fun moveRight() {
        val isSelectingText = Input.isShiftDown
        val oldCursor = if (isSelectingText) cursor2 else max(cursor1, cursor2)
        val currentLine = lines[oldCursor.y]
        val newCursor = when {
            !isSelectingText && cursor1 != cursor2 -> {
                // remove current selection first
                oldCursor
            }
            oldCursor.x < currentLine.size -> {
                // we can move right
                CursorPosition(oldCursor.x + 1, oldCursor.y)
            }
            else -> {
                // we need to move down
                CursorPosition(0, min(oldCursor.y + 1, lines.lastIndex))
            }
        }
        if (isSelectingText) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        lastChangeTime = Time.nanoTime
    }

    private fun moveLeft() {
        val isSelecting = Input.isShiftDown
        val oldCursor = if (isSelecting) cursor2 else min(cursor1, cursor2)
        val newCursor = when {
            !isSelecting && cursor1 != cursor2 -> {
                // remove current selection first
                oldCursor
            }
            oldCursor.x > 0 -> {
                // we can move left
                CursorPosition(oldCursor.x - 1, oldCursor.y)
            }
            else -> {
                // we need to move down, if possible
                CursorPosition(0, max(oldCursor.y - 1, 0))
            }
        }
        if (isSelecting) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        lastChangeTime = Time.nanoTime
    }

    private fun moveUp() {
        val useC2 = Input.isShiftDown
        val oldCursor = if (useC2) cursor2 else cursor1
        val newCursor = if (oldCursor.y > 0) {
            getCursor(getLineWidth(lines[oldCursor.y], oldCursor.x, styleSample), oldCursor.y - 1, false)
        } else return // cannot move up
        if (useC2) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        lastChangeTime = Time.nanoTime
    }

    private fun moveDown() {
        val useC2 = Input.isShiftDown
        val oldCursor = if (useC2) cursor2 else cursor1
        val newCursor = if (oldCursor.y < lines.lastIndex) {
            getCursor(getLineWidth(lines[oldCursor.y], oldCursor.x, styleSample), oldCursor.y + 1, false)
        } else return // cannot move down
        if (useC2) {
            cursor2.set(newCursor)
        } else {
            cursor1.set(newCursor)
            cursor2.set(cursor1)
        }
        ensureCursorBounds()
        lastChangeTime = Time.nanoTime
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if (cursor1 == cursor2) return text
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        if (min.y == max.y) {
            return lines[min.y].joinChars(min.x, max.x).toString()
        }
        val answer = StringBuilder((max.y - min.y + 1))
        val line0 = lines[min.y]
        val line1 = lines[max.y]
        answer.append(line0.joinChars(min.x, line0.size))
        for (lineY in min.y + 1 until max.y) {
            answer.append(lines[lineY].joinChars())
        }
        answer.append(line1.joinChars(0, max.x))
        return answer.toString()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        insert(data)
        dragged = null
    }

    private fun getLineIndex(y: Float): Int {
        // find the correct line index
        val yInt = y.toInt()
        var index = actualChildren.binarySearch { it.y.compareTo(yInt) }
        if (index < 0) index = -2 - index
        return clamp(index, 0, lines.lastIndex)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (!isHovered || y >= scrollbarStartY || dragged != null) return super.onMouseMoved(x, y, dx, dy)
        val indexY = getLineIndex(y)

        invalidateDrawing()

        if (!isControlDown && isLeftDown) {
            val localX = x - ((content as PanelList).children.first().x + padding.left)
            val line = lines[indexY]
            val indexX = getIndexFromText(line, localX, styleSample)
            cursor2.set(indexX, indexY)
            ensureCursorBounds()
            super.onMouseMoved(x, y, 0f, dy) // dx was consumed, kinda
        } else super.onMouseMoved(x, y, dx, dy)
    }

    fun getCursor(x: Float, indexY: Int, withOffset: Boolean): CursorPosition {
        val line = lines[indexY]
        var ix = x
        if (withOffset) ix -= (content as PanelList).children.first().x + padding.left
        val indexX = getIndexFromText(line, ix, styleSample)
        return CursorPosition(indexX, indexY)
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        lastClick = Time.nanoTime
        if ((!isHovered || y >= scrollbarStartY) || key != Key.BUTTON_LEFT) {
            super.onKeyDown(x, y, key)
        } else {
            if (isControlDown) {
                selectAll()
            } else {
                // find the correct location for the cursor
                cursor1.set(getCursor(x, getLineIndex(y), true))
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
        cursor1.set(0, 0)
        cursor2.set(endCursor)
        ensureCursorBounds()
    }

    override fun onEmpty(x: Float, y: Float) {
        if (!isInputAllowed) return
        if (isNothingSelected()) {
            resetToDefault()
        } else {
            // empty selection
            deleteSelection()
            update(true)
        }
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
            "DeleteAfter" -> if (isInputAllowed) deleteAfter()
            "DeleteBefore" -> if (isInputAllowed) likeBackspaceKey()
            "DeleteSelection" -> if (isInputAllowed) deleteSelection()
            "MoveLeft" -> moveLeft()
            "MoveRight" -> moveRight()
            "MoveUp" -> moveUp()
            "MoveDown" -> moveDown()
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
        likeBackspaceKey()
    }

    private var lastDelete = 0L
    private fun likeBackspaceKey() {
        if (lastDelete != Time.lastTimeNanos) {
            lastDelete = Time.lastTimeNanos
            deleteBefore()
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        if (isInputAllowed && lines.size + 1 < lineLimit) {
            insert('\n'.code, true)
            invalidateDrawing()
        } else {
            val listener = enterListener
            if (listener != null) listener.invoke(text)
            else super.onEnterKey(x, y)
        }
    }

    override fun onDeleteKey(x: Float, y: Float) {
        if (!isInputAllowed) return
        deleteAfter()
    }

    override fun clone(): PureTextInputML {
        val clone = PureTextInputML(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as PureTextInputML
        dst.text = text
        dst.cursor1.set(cursor1)
        dst.cursor2.set(cursor2)
        dst.lineLimit = lineLimit
        dst.placeholder = placeholder
        dst.focusTextColor = focusTextColor
    }

    override val className: String get() = "PureTextInputML"
}