package me.anno.ui.editor.code

import me.anno.config.DefaultConfig
import me.anno.config.DefaultConfig.style
import me.anno.config.DefaultStyle.black
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.GFX
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.MouseButton
import me.anno.language.spellcheck.Suggestion
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.pow
import me.anno.studio.history.StringHistory
import me.anno.ui.Panel
import me.anno.ui.base.Font
import me.anno.ui.base.components.Padding
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.code.codemirror.*
import me.anno.ui.input.components.CursorPosition
import me.anno.ui.style.Style
import me.anno.utils.structures.arrays.IntSequence
import me.anno.utils.structures.arrays.LineSequence
import me.anno.utils.structures.lists.Lists.binarySearch
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.joinChars
import kotlin.math.log10
import kotlin.math.max
import kotlin.streams.toList

// todo two new editors/viewers:
//  - hex viewer
//  - optimize for huge files
open class CodeEditor(style: Style) : Panel(style) {

    private val content = LineSequence()

    // todo for actual code, save this history :),
    val history = object : StringHistory() {
        override fun apply(v: String) {
            setText(v, false, true)
        }
    }

    init {
        history.put("")
    }

    var language: Language = LuaLanguage()

    var theme = LanguageThemeLib.Darcula
    var styles = Array(0) { theme.styles[0] }

    var changeListener: (IntSequence) -> Unit = {}

    open fun drawUnderline(x0: Int, x1: Int, y: Int, h: Int, color: Int) {
        drawRect(x0, y + lineHeight - 1, x1 - x0, h, color)
    }

    open fun drawSquiggles(x0: Int, x1: Int, y: Int, h: Int, color: Int) {
        for (x in x0 until x1) {
            drawRect(x, y + wave(x, h), 1, 1, color)
        }
    }

    fun getText(): String {
        return content.toString()
    }

    fun setText(text: CharSequence, updateHistory: Boolean = true, notify: Boolean = true) {
        content.setText(text)
        onChangeText(updateHistory, notify)
    }

    private val variableNames = ArrayList<VariableName>()
    fun recalculateColors() {
        val state = language.getStartState()
        val text = content
        val stream = Stream(text)
        val styles = Array(content.length) { theme.styles[0] }
        this.styles = styles
        var variableNamesIndex = 0
        while (stream.index < text.length) {
            val token = language.getToken(stream, state)
            if (stream.startIndex == stream.index) break
            // color from startIndex to index
            val style = theme.styles[token.ordinal]
            for (index in stream.startIndex until stream.index) {
                styles[index] = style
            }
            when (token) {
                TokenType.VARIABLE, TokenType.VARIABLE2, TokenType.VARIABLE3,
                TokenType.STRING, TokenType.STRING2, TokenType.COMMENT,
                TokenType.PROPERTY, TokenType.TAG, TokenType.ATTRIBUTE -> {
                    var s0 = stream.startIndex
                    var s1 = stream.index - 1
                    // remove quotes from spell check, because there is no reason
                    // to check them
                    if (text[s0] == '\''.code || text[s0] == '"'.code) s0++
                    if (text[s1] == '\''.code || text[s1] == '"'.code) s1--
                    s1++
                    val partial = text.subSequence(s0, s1).toString()
                    if (variableNamesIndex < variableNames.size) {
                        val variable = variableNames[variableNamesIndex]
                        variable.startIndex = s0
                        variable.endIndex = s1
                        variable.text = partial
                    } else {
                        val variable = VariableName(s0, s1, partial)
                        variableNames.add(variable)
                    }
                    variableNamesIndex++
                }
                else -> {}
            }
            stream.startIndex = stream.index
        }
        while (variableNamesIndex > variableNames.size) {
            variableNames.removeAt(variableNames.lastIndex)
        }
        invalidateDrawing()
    }

    var autoWrap = true
    val padding = Padding(4)
    var font = Font("Courier New", 16, false, false) // style.getFont("text", Font("Mono", 16, false, false))
    val fonts = Array(4) {
        font.withBold(it.and(1) > 0)
            .withItalic(it.and(2) > 0)
    }

    val charWidth get() = font.sampleWidth
    val lineHeight get() = font.sampleHeight

    val cursor0 = CursorPosition()
    val cursor1 = CursorPosition()

    private fun getCharsNeededForLineCount(lineCount: Int = content.lineCount): Int {
        return max(log10(lineCount.toFloat()).toInt(), 1) + 2
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        // calculate size... needs max line length & number of lines
        minW = (content.maxLineLength + getCharsNeededForLineCount()) * charWidth + padding.width
        minH = content.lineCount * lineHeight + padding.height
    }

    override val canDrawOverBorders: Boolean = true

    fun drawLineNumber(yi: Int, i: Int, cn: Int) {
        var xi = cn - 2
        val textColor = theme.numbersColor
        val background = theme.numbersBGColor
        var ii = i
        do {
            val char = '0' + (ii % 10)
            drawChar(xi, yi, char, textColor, background)
            xi--
            ii /= 10
        } while (ii > 0)
    }

    var firstLineZero = false
    var showCursor = false

    private var lastChangeTime = 0L

    var blinkingIntervalNanos = 500_000_000L

    override fun tickUpdate() {
        // performance test
        val blinkVisible = ((GFX.gameTime - lastChangeTime) / blinkingIntervalNanos).and(1L) == 0L
        val sb = showCursor
        showCursor = isInFocus && blinkVisible
        if (sb != showCursor) invalidateDrawing()
    }

    fun drawChar(
        xi: Int, yi: Int, char: Char,
        textColor: Int, backgroundColor: Int,
        bold: Boolean = false, italic: Boolean = false
    ) {
        val font = fonts[bold.toInt(1) + italic.toInt(2)]
        val text = char.toString()
        val key = TextCacheKey(text, font)
        val background = backgroundColor
        val x = this.x + padding.left + xi * charWidth
        val y = this.y + padding.top + yi * lineHeight
        val tw = DrawTexts.getTextSizeX(font, text, -1, -1)
        drawRect(x, y, charWidth, lineHeight, background or black)
        drawText(x - (charWidth - tw) / 2, y, font, key, textColor, background and 0xffffff)
    }

    fun drawCharBackground(
        xi: Int, yi: Int, backgroundColor: Int
    ) {
        val background = backgroundColor
        val x = this.x + padding.left + xi * charWidth
        val y = this.y + padding.top + yi * lineHeight
        drawRect(x, y, charWidth, lineHeight, background or black)
    }

    fun drawCharText(
        xi: Int, yi: Int, char: Int,
        textColor: Int, backgroundColor: Int,
        bold: Boolean = false, italic: Boolean = false
    ) {
        val font = fonts[bold.toInt(1) + italic.toInt(2)]
        val text = char.joinChars().toString()
        val key = TextCacheKey(text, font)
        val background = backgroundColor
        val x = this.x + padding.left + xi * charWidth
        val y = this.y + padding.top + yi * lineHeight
        val tw = DrawTexts.getTextSizeX(font, text, -1, -1)
        drawText(x + (charWidth - tw) / 2, y, font, key, textColor, background and 0xffffff)
    }

    val minCursor get() = if (cursor0 < cursor1) cursor0 else cursor1
    val maxCursor get() = if (cursor0 > cursor1) cursor0 else cursor1

    // todo line after 80 chars

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {

        var x = this.x + padding.left
        val y = this.y + padding.top

        val charWidth = charWidth
        val lineHeight = lineHeight
        val minCursor = minCursor
        val maxCursor = maxCursor

        backgroundColor = theme.backgroundColor or black
        drawBackground(x0, y0, x1, y1)

        // draw the selected lines with special color background
        val selectedBGColor = (if (minCursor.y == maxCursor.y && minCursor.x + 1 >= maxCursor.x)
            theme.selectedSingleColor else theme.selectedMultipleColor) or black
        val minSY = minCursor.y * lineHeight + y
        val maxSH = (maxCursor.y - minCursor.y + 1) * lineHeight

        // draw space for line numbers
        val cn = getCharsNeededForLineCount()
        val lineNumberBGColor = theme.numbersBGColor or black

        // draw number line background color
        drawRect(
            x - padding.left, y - padding.top,
            cn * charWidth + padding.left - charWidth / 2, h,
            lineNumberBGColor
        )

        val selectedLineBGColor = theme.selectedLineBGColor or black
        val bg0 = max(x0, this.x + cn * charWidth + padding.left)
        val bg1 = min(x1, this.x + this.w - padding.right)

        // draw selected line background color
        if (cursor0 == cursor1) {
            drawRect(
                bg0, minSY, bg1 - bg0, maxSH,
                selectedLineBGColor
            )
        }

        val drawnYi = 0

        // draw line numbers
        val lineNumber = if (firstLineZero) 0 else 1

        x += cn * charWidth

        // draw selection background, which is wider than the text
        if (minCursor.y < maxCursor.y) {
            drawRect(x + (cn + minCursor.x), y + minCursor.y * lineHeight, w, lineHeight, selectedBGColor)
            if (minCursor.y + 1 < maxCursor.y) {
                drawRect(
                    bg0, y + (minCursor.y + 1) * lineHeight, bg1 - bg0,
                    (maxCursor.y - minCursor.y - 1) * lineHeight,
                    selectedBGColor
                )
            }
        }

        val vx0 = (x0 - x) / charWidth
        val vy0 = (y0 - y) / lineHeight
        val vx1 = ceilDiv(x1 - x, charWidth)
        val vy1 = ceilDiv(y1 - y, lineHeight)

        for (yi in vy0 until vy1) {
            drawLineNumber(yi, yi + lineNumber, cn)
        }

        var varIndex = 0
        content.forEachChar(vx0, vy0, vx1, vy1) { charIndex, lineIndex, indexInLine, _ ->
            // draw character
            val style = styles[charIndex]
            val textColor = style.color
            val lineIsSelected = lineIndex in minCursor.y..maxCursor.y && cursor0 == cursor1
            val lineBGColor = if (lineIsSelected) selectedLineBGColor else theme.backgroundColor
            val isSelected = minCursor.contains(maxCursor, indexInLine, lineIndex)
            val background = if (isSelected) selectedBGColor else lineBGColor
            drawCharBackground(cn + indexInLine, drawnYi + lineIndex, background)
            while (varIndex < variableNames.size &&
                charIndex >= variableNames[varIndex].endIndex
            ) {
                varIndex++
            }
            val variable = variableNames.getOrNull(varIndex)
            val isIncorrectlySpelled = variable != null && charIndex in variable
                    && variable.check()?.any { suggestion -> (charIndex - variable.startIndex) in suggestion } == true
            val xi = x + indexInLine * charWidth
            val yi = y + lineIndex * lineHeight
            if (style.squiggles || isIncorrectlySpelled) {
                // todo get text baseline for alignment...
                // todo which thickness?
                // todo which color?
                // todo why not working???
                drawSquiggles(xi, xi + charWidth, yi + lineHeight * 5 / 6, 3, textColor)
            }
            if (style.underlined) {
                // todo which thickness?
                drawUnderline(xi, xi + charWidth, yi, 1, textColor)
            }
        }

        content.forEachChar(vx0, vy0, vx1, vy1) { charIndex, lineIndex, indexInLine, char ->
            // draw character
            val style = styles[charIndex]
            val textColor = style.color
            val lineIsSelected = lineIndex in minCursor.y..maxCursor.y && cursor0 == cursor1
            val lineBGColor = if (lineIsSelected) selectedLineBGColor else theme.backgroundColor
            val isSelected = minCursor.contains(maxCursor, indexInLine, lineIndex)
            val background = if (isSelected) selectedBGColor else lineBGColor
            drawCharText(cn + indexInLine, drawnYi + lineIndex, char, textColor, background, style.bold, style.italic)
        }

        if (showCursor) {
            val cy = clamp(cursor1.y, 0, content.lineCount - 1)
            val cx = clamp(cursor1.x, 0, content.getLineLength(cy))
            drawRect(// draw selected line background color
                this.x + padding.left + (cx + cn) * charWidth - 1,
                this.y + padding.top + cy * lineHeight,
                1, lineHeight, theme.cursorColor
            )
        }

    }

    override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
        if (Input.isControlDown) {
            val newSize = font.size * pow(1.05f, -dx + dy)
            font = font.withSize(newSize)
            for (i in fonts.indices) {
                fonts[i] = fonts[i].withSize(newSize)
            }
            invalidateLayout()
        } else super.onMouseWheel(x, y, dx, dy, byMouse)
    }

    fun left(c: CursorPosition = cursor1) {
        clampCursor(c)
        lastChangeTime = GFX.gameTime
        if (c.x == 0 && c.y > 0) {
            c.set(content.getLineLength(c.y - 1), c.y - 1)
        } else {
            c.set(max(0, c.x - 1), c.y)
        }
    }

    fun up(c: CursorPosition = cursor1) {
        clampCursor(c)
        lastChangeTime = GFX.gameTime
        c.set(c.x, max(c.y - 1, 0))
    }

    fun down(c: CursorPosition = cursor1) {
        clampCursor(c)
        lastChangeTime = GFX.gameTime
        c.set(c.x, min(c.y + 1, content.lineCount - 1))
    }

    fun right(c: CursorPosition = cursor1) {
        clampCursor(c)
        lastChangeTime = GFX.gameTime
        if (c.x >= content.getLineLength(c.y) && c.y + 1 < content.lineCount) {
            // set to next line
            c.set(0, c.y + 1)
        } else {
            c.set(min(c.x + 1, content.getLineLength(c.y)), c.y)
        }
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "Up" -> {
                up()
                cursor0.set(cursor1)
                invalidateDrawing()
            }
            "Up-2" -> {
                up()
                invalidateDrawing()
            }
            "Down" -> {
                down()
                cursor0.set(cursor1)
                invalidateDrawing()
            }
            "Down-2" -> {
                down()
                invalidateDrawing()
            }
            "Left" -> {
                left()
                cursor0.set(cursor1)
                invalidateDrawing()
            }
            "Left-2" -> {
                left()
                invalidateDrawing()
            }
            "Right" -> {
                right()
                cursor0.set(cursor1)
                invalidateDrawing()
            }
            "Right-2" -> {
                right()
                invalidateDrawing()
            }
            "Redo" -> history.redo()
            "Undo" -> history.undo()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun deleteSelectionInternal() {
        clampCursors()
        val minCursor = minCursor
        val maxCursor = maxCursor
        val minIndex = content.getIndexAt(minCursor.y, minCursor.x)
        val maxIndex = content.getIndexAt(maxCursor.y, maxCursor.x)
        for (i in maxIndex - 1 downTo minIndex) {
            content.remove(i)
        }
        cursor1.set(minCursor)
        cursor0.set(minCursor)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        deleteSelectionInternal()
        content.insert(cursor1.y, cursor1.x, data.codePoints())
        for (i in data.indices) right(cursor1)
        cursor0.set(cursor1)
        onChangeText()
    }

    override fun onSelectAll(x: Float, y: Float) {
        cursor0.set(0, 0)
        cursor1.set(content.getLineLength(content.lineCount - 1), content.lineCount - 1)
        lastChangeTime = GFX.gameTime
        invalidateDrawing()
    }

    override fun onEmpty(x: Float, y: Float) {
        if (cursor0 != cursor1 && content.length > 0) {
            deleteSelectionInternal()
            onChangeText()
        }
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        val suggestion = lastSuggestion
        val variable = lastVariable
        if (key == '\t'.code && variable != null && suggestion?.improvements?.isNotEmpty() == true) {
            applySuggestion(variable, suggestion, suggestion.improvements[0])
            lastVariable = null
            lastSuggestion = null
        } else {
            deleteSelectionInternal()
            content.insert(cursor1.y, cursor1.x, key)
            right(cursor1)
            cursor0.set(cursor1)
            onChangeText()
        }
    }

    fun clampCursors() {
        clampCursor(cursor0)
        clampCursor(cursor1)
    }

    fun clampCursor(cursor: CursorPosition) {
        val y = clamp(cursor.y, 0, content.lineCount - 1)
        cursor.set(clamp(cursor.x, 0, content.getLineLength(y)), y)
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        clampCursors()
        if (cursor0 == cursor1) {
            if (cursor0.x > 0 || cursor0.y > 0) {
                left(cursor1)
                content.remove(cursor1.y, cursor1.x)
                cursor0.set(cursor1)
            } else return // cursor is at start
        } else {
            deleteSelectionInternal()
            if (cursor0 < cursor1) cursor1.set(cursor0)
        }
        onChangeText()
    }

    private fun onChangeText(updateHistory: Boolean = true, notify: Boolean = true) {
        recalculateColors()
        invalidateLayout()
        lastChangeTime = GFX.gameTime
        if (updateHistory) history.put(content.toString())
        if (notify) changeListener(content)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        clampCursors()
        if (cursor0 == cursor1) {
            if (cursor0.y < content.lineCount - 1 ||
                cursor0.x < content.getLineLength(content.lineCount - 1)
            ) content.remove(cursor1.y, cursor1.x)
            else return // cursor is at the end
        } else {
            deleteSelectionInternal()
            if (cursor0 < cursor1) cursor1.set(cursor0)
        }
        onChangeText()
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            moveCursor(cursor0, x, y)
        } else super.onMouseDown(x, y, button)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            moveCursor(cursor1, x, y)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        if (button.isLeft) {
            moveCursor(cursor1, x, y)
        } else super.onMouseUp(x, y, button)
    }

    fun moveCursor(cursor: CursorPosition, x: Float, y: Float) {
        val cn = getCharsNeededForLineCount()
        val ox = cursor.x
        val oy = cursor.y
        cursor.x = ((x - (this.x + padding.left)) / charWidth).toInt() - cn
        cursor.y = ((y - (this.y + padding.top)) / lineHeight).toInt()
        clampCursor(cursor)
        if (cursor.x != ox || cursor.y != oy) {
            lastChangeTime = GFX.gameTime
            invalidateDrawing()
        }
    }

    override fun onCopyRequested(x: Float, y: Float): Any? {
        clampCursors()
        val minCursor = minCursor
        val maxCursor = maxCursor
        return content.subSequence(
            content.getIndexAt(minCursor.y, minCursor.x),
            content.getIndexAt(maxCursor.y, maxCursor.x)
        )
    }

    // todo tab to apply correction & give hint about it
    private var lastVariable: VariableName? = null
    private var lastSuggestion: Suggestion? = null

    override fun onEnterKey(x: Float, y: Float) {
        val suggestion = lastSuggestion
        val variable = lastVariable
        if (suggestion != null && variable != null && suggestion.improvements.isNotEmpty()) {
            applySuggestion(variable, suggestion, suggestion.improvements[0])
            lastVariable = null
            lastSuggestion = null
        } else {
            onCharTyped(x, y, '\n'.code)
        }
    }

    private fun applySuggestion(variableName: VariableName, suggestion: Suggestion, choice: String) {
        // todo test the example: "Eine Löwenfuß" -> "Ein Löwenfußß"?
        val cp = choice.codePoints().toList()
        val si = variableName.startIndex
        val charIndex = si + suggestion.start
        // remove incorrect letters
        for (i in suggestion.end - suggestion.start - 1 downTo 0) {
            content.remove(charIndex + i)
        }
        // insert corrected letters
        for (c in cp.reversed()) {
            content.insert(charIndex, c)
        }
        onChangeText()
    }

    override fun getTooltipText(x: Float, y: Float): String? {
        val xi = ((x - (this.x + padding.left)) / charWidth).toInt() - getCharsNeededForLineCount()
        val yi = ((y - (this.y + padding.top)) / lineHeight).toInt()
        // find if there is a variable
        if (yi in 0 until content.lineCount && xi in 0 until content.getLineLength(yi)) {
            // find which char is here
            val charIndex = content.getIndexAt(yi, xi)
            // binary search...
            var variableIndex = variableNames.binarySearch { e -> e.startIndex.compareTo(charIndex) }
            if (variableIndex < 0) variableIndex = -2 - variableIndex
            if (variableIndex in variableNames.indices) {
                val variable = variableNames[variableIndex]
                if (charIndex in variable) {
                    // check if there is a correction for it
                    val suggestions = variable.check()
                    if (suggestions != null) {
                        val localIndex = charIndex - variable.startIndex
                        val s = suggestions.firstOrNull { s -> localIndex in s }
                        if (s != null) {
                            lastVariable = variable
                            lastSuggestion = s
                            return if (s.improvements.isEmpty()) s.clearMessage else s.clearMessage + "\n" +
                                    "Suggestions: " + s.improvements.withIndex()
                                .joinToString { (index, s) -> if (index == 0) "$s <Tab>" else s }
                        }
                    }
                }
            }
        }
        lastSuggestion = null
        return null
    }

    // accept tabs
    override fun isKeyInput(): Boolean = true
    override fun acceptsChar(char: Int): Boolean = true

    override val className: String = "CodeEditor"

    companion object {

        fun wave(x: Int, h: Int): Int {
            val period = 2 * h - 2
            val p = x % period
            return if (p < h) p else period - h
        }

        @JvmStatic
        fun main(args: Array<String>) {
            testUI {
                DefaultConfig["debug.ui.enableVsync"] = false
                ActionManager.register("CodeEditor.upArrow.t", "Up")
                ActionManager.register("CodeEditor.downArrow.t", "Down")
                ActionManager.register("CodeEditor.leftArrow.t", "Left")
                ActionManager.register("CodeEditor.rightArrow.t", "Right")
                ActionManager.register("CodeEditor.upArrow.t.s", "Up-2")
                ActionManager.register("CodeEditor.downArrow.t.s", "Down-2")
                ActionManager.register("CodeEditor.leftArrow.t.s", "Left-2")
                ActionManager.register("CodeEditor.rightArrow.t.s", "Right-2")
                ActionManager.register("CodeEditor.z.t.c", "Undo")
                ActionManager.register("CodeEditor.z.t.cs", "Redo")
                ActionManager.register("CodeEditor.y.t.c", "Undo")
                ActionManager.register("CodeEditor.y.t.cs", "Redo")
                CodeEditor(style).apply {
                    setText(
                        "" +
                                "if cnt == 1 and state == 0 then\n" +
                                "  print('this is cool')\n" +
                                "end\n" +
                                " 4\n 5\n 6\n 7\n 8\n 9\n10\n"
                    )
                }
            }
        }
    }

}