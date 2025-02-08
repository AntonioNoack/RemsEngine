package me.anno.ui.editor.code

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.history.StringHistory
import me.anno.fonts.Codepoints.codepoints
import me.anno.fonts.Font
import me.anno.fonts.keys.TextCacheKey
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawStriped.drawRectStriped
import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.input.ActionManager
import me.anno.input.Input
import me.anno.input.Key
import me.anno.language.spellcheck.Suggestion
import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.min
import me.anno.maths.Maths.pow
import me.anno.ui.Panel
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.components.Padding
import me.anno.ui.editor.code.codemirror.LanguageThemeLib
import me.anno.ui.editor.code.tokenizer.LanguageTokenizer
import me.anno.ui.editor.code.tokenizer.LuaTokenizer
import me.anno.ui.editor.code.tokenizer.Stream
import me.anno.ui.editor.code.tokenizer.TokenType
import me.anno.ui.input.components.CursorPosition
import me.anno.ui.input.components.PureTextInputML.Companion.blinkVisible
import me.anno.ui.input.components.PureTextInputML.Companion.notifyCursorTyped
import me.anno.utils.Color.black
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.arrays.IntSequence
import me.anno.utils.structures.arrays.LineSequence
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.binarySearch
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.joinChars
import kotlin.math.log10
import kotlin.math.max

// todo feedback, what the result of the code is / what compiler errors happened
// todo also add execution button

// todo if on bracket, find+highlight matching bracket
// todo collapsable blocks

// todo for actual code, save this' history :)

// todo line wrapping

// todo placeholder

// todo search & replace
// todo refactoring (rename a variable)

// todo auto-formatting
// done don't snap cursor left when moving up/down lines (except first/last line)

@Suppress("MemberVisibilityCanBePrivate")
open class CodeEditor(style: Style) : Panel(style) {

    private val content = LineSequence()

    val history = object : StringHistory() {
        override fun apply(prev: String, curr: String) {
            setText(curr, false, notify = true)
        }
    }

    var language: LanguageTokenizer = LuaTokenizer()

    var theme = LanguageThemeLib.Twilight
        set(value) {
            if (field != value) {
                field = value
                invalidateDrawing()
            }
        }

    var styles = ByteArray(0)

    var changeListener: (CodeEditor, IntSequence) -> Unit = { _, _ -> }
        private set

    val padding = Padding(4)
    var font = Font("Courier New", 16)
    val fonts = createArrayList(4) {
        font.withBold(it.and(1) > 0)
            .withItalic(it.and(2) > 0)
    }

    val charWidth get() = font.sampleWidth
    val lineHeight get() = font.sampleHeight

    val cursor0 = CursorPosition()
    val cursor1 = CursorPosition()

    val codeBlockCollapser = CodeBlockCollapser()
    val codeBlocks = ArrayList<CodeBlock>()

    fun setOnChangeListener(listener: (CodeEditor, IntSequence) -> Unit) {
        changeListener = listener
    }

    open fun drawUnderline(x0: Int, x1: Int, y: Int, h: Int, color: Int) {
        drawRect(x0, y + lineHeight - 1, x1 - x0, h, color)
    }

    open fun drawSquiggles(x0: Int, x1: Int, y: Int, h: Int, color: Int) {
        drawSquiggles1(x0, x1, y, h, color)
    }

    fun getText(): String {
        return content.toString()
    }

    fun setText(text: CharSequence, updateHistory: Boolean = true, notify: Boolean = true) {
        content.setText(text)
        onChangeText(updateHistory, notify)
    }

    private val spellcheckedSections = ArrayList<TextSection>()
    fun recalculateColors() {
        val state = language.getStartState()
        val text = content
        val stream = Stream(text)
        val styles = ByteArray(text.length)
        styles.fill(TokenType.ERROR.ordinal.toByte())
        this.styles = styles
        var variableNamesIndex = 0
        while (stream.index < text.length) {
            val token = language.getToken(stream, state)
            if (stream.startIndex == stream.index) break
            // color from startIndex to index
            val style = token.ordinal.toByte()
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
                    if (variableNamesIndex < spellcheckedSections.size) {
                        val variable = spellcheckedSections[variableNamesIndex]
                        variable.startIndex = s0
                        variable.endIndex = s1
                        variable.text = partial
                    } else {
                        val variable = TextSection(s0, s1, partial)
                        spellcheckedSections.add(variable)
                    }
                    variableNamesIndex++
                }
                else -> {}
            }
            stream.resetToken()
        }
        while (variableNamesIndex > spellcheckedSections.size) {
            spellcheckedSections.removeAt(spellcheckedSections.lastIndex)
        }
        invalidateDrawing()
    }

    fun recalculateBrackets() {
        val brackets = language.brackets.map { pair ->
            pair.first to (pair.first.codepoints() to pair.second.codepoints())
        }
        codeBlocks.clear()
        if (brackets.isEmpty()) return
        val state = language.getStartState()
        val content = content
        val stream = Stream(content)
        val bracketStack = ArrayList<String>()
        val bracketLineStack = IntArrayList(16)
        while (stream.index < content.length) {
            language.getToken(stream, state)
            if (stream.startIndex == stream.index) break // done
            val i0 = stream.startIndex
            val i1 = stream.index
            for ((openingName, pair) in brackets) {
                val (opening, closing) = pair
                if (i1 - i0 == opening.size && content.equals(opening, i0)) {
                    bracketStack.add(openingName)
                    bracketLineStack.add(content.getLineIndexAt(i0))
                    break
                } else if (i1 - i0 == closing.size &&
                    bracketStack.lastOrNull() == openingName &&
                    content.equals(closing, i0)
                ) {
                    bracketStack.removeLast()
                    val startLineIndex = bracketLineStack.removeLast()
                    val lineIndex = content.getLineIndexAt(i0)
                    codeBlocks.add(CodeBlock(startLineIndex, lineIndex - startLineIndex - 1))
                    break
                }
            }
            stream.resetToken()
        }
    }

    private fun getCharsNeededForLineCount(): Int {
        val firstLineNumber = (!firstLineZero).toInt()
        val maxLineNumber = firstLineNumber + content.lineCount - 1
        return max(log10(maxLineNumber.toDouble()).toInt(), 1) + 2
    }

    override fun calculateSize(w: Int, h: Int) {
        super.calculateSize(w, h)
        // calculate size... needs max line length & number of lines
        minW = (content.maxLineLength + getCharsNeededForLineCount()) * charWidth + padding.width
        minH = content.lineCount * lineHeight + padding.height
    }

    override val canDrawOverBorders get() = true

    fun drawLineNumber(yi: Int, lineNumber: Int, cn: Int) {
        var xi = cn - 2
        val textColor = theme.numbersColor
        val background = theme.numbersBGColor
        var remainingNumber = lineNumber
        do {
            val char = '0' + (remainingNumber % 10)
            drawChar(xi, yi, char, textColor, background)
            xi--
            remainingNumber /= 10
        } while (remainingNumber > 0)
    }

    var firstLineZero = false
    var showCursor = false

    override fun onUpdate() {
        val sb = showCursor
        showCursor = isInFocus && blinkVisible
        if (sb != showCursor) invalidateDrawing()
        if (!isInFocus) cursor0.set(cursor1)
    }

    private fun getCharX(xi: Int): Int = this.x + padding.left + xi * charWidth
    private fun getCharY(yi: Int): Int = this.y + padding.top + yi * lineHeight

    private fun getCharXiFloor(x: Int): Int = (x - getCharX(0)) / charWidth
    private fun getCharYiFloor(y: Int): Int = (y - getCharY(0)) / lineHeight

    private fun getCharXiRound(x: Int): Int = getCharXiFloor(x - charWidth.shr(1))

    fun drawChar(
        xi: Int, yi: Int, char: Char,
        textColor: Int, backgroundColor: Int,
        bold: Boolean = false, italic: Boolean = false
    ) {
        val font = fonts[bold.toInt(1) + italic.toInt(2)]
        val x = getCharX(xi)
        val y = getCharY(yi)
        drawRect(x, y, charWidth, lineHeight, backgroundColor or black)
        drawText(
            x + charWidth.shr(1), y, font, TextCacheKey(char.toString(), font),
            textColor, backgroundColor and 0xffffff,
            AxisAlignment.CENTER, AxisAlignment.MIN
        )
    }

    fun drawCharBackground(xi: Int, yi: Int, backgroundColor: Int): Unit =
        drawRect(getCharX(xi), getCharY(yi), charWidth, lineHeight, backgroundColor or black)

    fun drawCharText(
        xi: Int, yi: Int, char: Int,
        textColor: Int, backgroundColor: Int,
        bold: Boolean = false, italic: Boolean = false
    ) {
        val font = fonts[bold.toInt(1) + italic.toInt(2)]
        val text = char.joinChars().toString()
        drawText(
            getCharX(xi) + charWidth.shr(1), getCharY(yi),
            font, TextCacheKey(text, font),
            textColor, backgroundColor and 0xffffff,
            AxisAlignment.CENTER, AxisAlignment.MIN
        )
    }

    val minCursor get() = if (cursor0 < cursor1) cursor0 else cursor1
    val maxCursor get() = if (cursor0 > cursor1) cursor0 else cursor1

    private fun drawLineNumberBackground(cn: Int, y0: Int, y1: Int, lineNumberBGColor: Int) {
        val nlb = this.x
        val nlb2 = cn * charWidth + padding.left - charWidth.shr(1)
        drawRect(nlb, y0, nlb2, y1 - y0, lineNumberBGColor)
        drawRect(nlb + nlb2, y0, 1, y1 - y0, theme.numbersLineColor.withAlpha(1f))
    }

    private fun drawSelectionBackground(cn: Int, x0: Int, x1: Int, selectedBGColor: Int) {
        drawRect(getCharX(cn + minCursor.x), getCharY(minCursor.y), width, lineHeight, selectedBGColor)
        if (minCursor.y + 1 < maxCursor.y) {
            drawRect(
                x0, getCharY(minCursor.y + 1), x1 - x0,
                (maxCursor.y - minCursor.y - 1) * lineHeight,
                selectedBGColor
            )
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {

        var x = getCharX(0)
        val y = getCharY(0)

        val charWidth = charWidth
        val lineHeight = lineHeight
        val minCursor = minCursor
        val maxCursor = maxCursor

        backgroundColor = theme.backgroundColor or black
        drawBackground(x0, y0, x1, y1)

        // draw the selected lines with special color background
        val selectedBGColor = theme.selectedBGColor or black
        val minSY = minCursor.y * lineHeight + y
        val maxSH = (maxCursor.y - minCursor.y + 1) * lineHeight

        // draw space for line numbers
        val cn = getCharsNeededForLineCount()
        val lineNumberBGColor = theme.numbersBGColor or black

        // draw number line background color
        drawLineNumberBackground(cn, y0, y1, lineNumberBGColor)

        val selectedLineBGColor = theme.selectedLineBGColor.withAlpha(1f)
        val bg0 = max(x0, this.x + cn * charWidth + padding.left)
        val bg1 = min(x1, this.x + this.width - padding.right)

        // draw selected line background color
        if (isInFocus && cursor0 == cursor1) {
            drawRect(bg0, minSY, bg1 - bg0, maxSH, selectedLineBGColor)
        }

        val drawnYi = 0

        // draw line numbers
        val firstLineNumber = (!firstLineZero).toInt()

        x += cn * charWidth

        // todo first line of selection background is wider than background... why???

        // draw selection background, which is wider than the text
        if (isInFocus && minCursor.y < maxCursor.y) {
            drawSelectionBackground(cn, bg0, bg1, selectedBGColor)
        }

        val visibleX0 = getCharXiFloor(x0)
        val visibleY0 = getCharYiFloor(y0)
        val visibleX1 = ceilDiv(x1 - x, charWidth)
        val visibleY1 = min(ceilDiv(y1 - y, lineHeight), codeBlockCollapser.countLines(content))

        for (yi in visibleY0 until visibleY1) {
            val lineNumber0 = codeBlockCollapser.mapLine(yi)
            drawLineNumber(yi, firstLineNumber + lineNumber0, cn)
        }

        var varIndex = 0
        codeBlockCollapser.forEachChar(
            visibleX0, visibleY0, visibleX1, visibleY1, content
        ) { charIndex, lineIndex, indexInLine, _ ->
            // draw character background
            val style = theme.styles[styles[charIndex].toInt()]
            val textColor = style.color
            val lineIsSelected = lineIndex in minCursor.y..maxCursor.y && cursor0 == cursor1
            val lineBGColor = if (lineIsSelected) selectedLineBGColor else theme.backgroundColor
            val isSelected = isInFocus && minCursor.contains(maxCursor, indexInLine, lineIndex)
            val background = if (isSelected) selectedBGColor else lineBGColor
            drawCharBackground(cn + indexInLine, drawnYi + lineIndex, background)
            while (varIndex < spellcheckedSections.size &&
                charIndex >= spellcheckedSections[varIndex].endIndex
            ) {
                varIndex++
            }
            val variable = spellcheckedSections.getOrNull(varIndex)
            val isIncorrectlySpelled = variable != null && charIndex in variable
                    && variable.spellcheck()
                ?.any { suggestion -> (charIndex - variable.startIndex) in suggestion } == true
            val xi = getCharX(cn + indexInLine)
            val yi = getCharY(lineIndex)
            if (style.squiggles) {
                drawSquiggles(xi, xi + charWidth, yi + lineHeight * 5 / 6, squigglesHeight, textColor)
            }
            if (isIncorrectlySpelled) {
                drawSquiggles(xi, xi + charWidth, yi + lineHeight * 5 / 6 + 1, squigglesHeight, black or 0xffff55)
            }
            if (style.underlined) {
                drawUnderline(xi, xi + charWidth, yi, underlineThickness, textColor)
            }
        }

        // line after 80 chars
        drawRect(x + recommendedLineLengthLimit * charWidth, y, 1, height - padding.height, selectedLineBGColor)

        codeBlockCollapser.forEachChar(
            visibleX0, visibleY0, visibleX1, visibleY1, content
        ) { charIndex, lineIndex, indexInLine, char ->
            // draw character
            val style = theme.styles[styles[charIndex].toInt()]
            val textColor = style.color
            val lineIsSelected = lineIndex == minCursor.y && cursor0 == cursor1
            val lineBGColor = if (lineIsSelected) selectedLineBGColor else theme.backgroundColor
            val isSelected = isInFocus && minCursor.contains(maxCursor, indexInLine, lineIndex)
            val background = if (isSelected) selectedBGColor else lineBGColor
            drawCharText(cn + indexInLine, drawnYi + lineIndex, char, textColor, background, style.bold, style.italic)
        }

        for (yi in visibleY0 until visibleY1) {
            val lineNumber0 = codeBlockCollapser.mapLine(yi)
            if (codeBlockCollapser.isClosed(lineNumber0)) {
                drawChar(cn - 1, yi, '+', theme.numbersColor, theme.numbersBGColor)
            } else if (codeBlocks.any2 { lineNumber0 == it.start && it.count > 0 }) {
                drawChar(cn - 1, yi, '-', theme.numbersColor, theme.numbersBGColor)
            }
        }

        if (showCursor) {
            val cy = clamp(cursor1.y, 0, content.lineCount - 1)
            val cx = clamp(cursor1.x, 0, content.getLineLength(cy))
            drawRect(// draw selected line background color
                getCharX(cx + cn) - 1, getCharY(cy),
                1, lineHeight, theme.cursorColor
            )
        }
    }

    var underlineThickness = 1
    var squigglesHeight = 3

    var recommendedLineLengthLimit = 80

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
        notifyCursorTyped()
        if (c.x == 0 && c.y > 0) {
            c.set(content.getLineLength(c.y - 1), c.y - 1)
        } else {
            c.set(max(0, c.x - 1), c.y)
        }
    }

    fun up(c: CursorPosition = cursor1) {
        notifyCursorTyped()
        if (c.y <= 0) c.set(0, 0)
        else c.set(c.x, c.y - 1)
    }

    fun down(c: CursorPosition = cursor1) {
        notifyCursorTyped()
        val lc = content.lineCount
        if (c.y + 1 >= lc) c.set(content.getLineLength(lc - 1), lc - 1)
        else c.set(c.x, c.y + 1)
    }

    fun right(c: CursorPosition = cursor1) {
        clampCursor(c)
        notifyCursorTyped()
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
            "Search" -> {
                // todo search things
                // todo option to search in all known (code) files?
            }
            "Replace" -> {
                // todo open UI for replacing things
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    fun deleteSelectionInternal() {
        clampCursors()
        val minCursor = minCursor
        val maxCursor = maxCursor
        val minIndex = content.getIndexAt(codeBlockCollapser.mapLine(minCursor.y), minCursor.x)
        val maxIndex = content.getIndexAt(codeBlockCollapser.mapLine(maxCursor.y), maxCursor.x)
        for (i in maxIndex - 1 downTo minIndex) {
            content.remove(i)
        }
        cursor1.set(minCursor)
        cursor0.set(minCursor)
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        deleteSelectionInternal()
        content.insert(codeBlockCollapser.mapLine(cursor1.y), cursor1.x, data.codepoints())
        for (i in data.indices) right(cursor1)
        cursor0.set(cursor1)
        onChangeText()
    }

    override fun onSelectAll(x: Float, y: Float) {
        cursor0.set(0, 0)
        cursor1.set(content.getLineLength(content.lineCount - 1), content.lineCount - 1)
        notifyCursorTyped()
        invalidateDrawing()
    }

    override fun onEmpty(x: Float, y: Float) {
        if (cursor0 != cursor1 && content.length > 0) {
            deleteSelectionInternal()
            onChangeText()
        }
    }

    var isInputAllowed = true

    override fun onCharTyped(x: Float, y: Float, codepoint: Int) {
        if (isInputAllowed && !Input.skipCharTyped(codepoint)) {
            val suggestion = lastSuggestion
            val variable = lastVariable
            if (codepoint == '\t'.code && variable != null && suggestion?.improvements?.isNotEmpty() == true) {
                applySuggestion(variable, suggestion, suggestion.improvements.first())
                lastVariable = null
                lastSuggestion = null
            } else {
                deleteSelectionInternal()
                content.insert(codeBlockCollapser.mapLine(cursor1.y), cursor1.x, codepoint)
                right(cursor1)
                cursor0.set(cursor1)
                onChangeText()
            }
        } else super.onCharTyped(x, y, codepoint)
    }

    fun clampCursors() {
        clampCursor(cursor0)
        clampCursor(cursor1)
    }

    fun clampCursor(cursor: CursorPosition) {
        val y = clamp(cursor.y, 0, codeBlockCollapser.countLines(content) - 1)
        cursor.set(clamp(cursor.x, 0, content.getLineLength(codeBlockCollapser.mapLine(y))), y)
    }

    override fun onBackSpaceKey(x: Float, y: Float) {
        clampCursors()
        if (cursor0 == cursor1) {
            if (cursor0.x > 0 || cursor0.y > 0) {
                left(cursor1)
                content.remove(codeBlockCollapser.mapLine(cursor1.y), cursor1.x)
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
        recalculateBrackets()
        invalidateLayout()
        notifyCursorTyped()
        if (updateHistory) history.put(content.toString())
        if (notify) changeListener(this, content)
    }

    override fun onDeleteKey(x: Float, y: Float) {
        clampCursors()
        if (cursor0 == cursor1) {
            val numVisLines = codeBlockCollapser.countLines(content)
            if (cursor0.y < numVisLines - 1 ||
                cursor0.x < content.getLineLength(codeBlockCollapser.mapLine(numVisLines - 1))
            ) content.remove(codeBlockCollapser.mapLine(cursor1.y), cursor1.x)
            else return // cursor is at the end
        } else {
            deleteSelectionInternal()
            if (cursor0 < cursor1) cursor1.set(cursor0)
        }
        onChangeText()
    }

    override fun onKeyDown(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            moveCursor(cursor0, x, y)
        } else super.onKeyDown(x, y, key)
    }

    override fun onKeyUp(x: Float, y: Float, key: Key) {
        if (key == Key.BUTTON_LEFT) {
            moveCursor(cursor1, x, y)
        } else super.onKeyUp(x, y, key)
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) {
            moveCursor(cursor1, x, y)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        if (button == Key.BUTTON_LEFT) {
            val xi = getCharXiFloor(x.toInt()) - getCharsNeededForLineCount()
            val yi = codeBlockCollapser.mapLine(getCharYiFloor(y.toInt()))
            if (xi == -1) {
                if (codeBlockCollapser.isClosed(yi)) {
                    codeBlockCollapser.open(yi)
                    invalidateDrawing()
                    return
                } else {
                    val bracket = codeBlocks
                        .filter { it.start == yi && it.count > 0 }
                        .maxByOrNull { it.count }
                    if (bracket != null) {
                        codeBlockCollapser.close(bracket)
                        invalidateDrawing()
                        return
                    }
                }
            }
        }
        super.onMouseClicked(x, y, button, long)
    }

    fun moveCursor(cursor: CursorPosition, x: Float, y: Float) {
        val ox = cursor.x
        val oy = cursor.y
        cursor.x = getCharXiRound(x.toInt()) - getCharsNeededForLineCount()
        cursor.y = getCharYiFloor(y.toInt())
        clampCursor(cursor)
        if (cursor.x != ox || cursor.y != oy) {
            notifyCursorTyped()
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
    private var lastVariable: TextSection? = null
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

    private fun applySuggestion(textSection: TextSection, suggestion: Suggestion, choice: String) {
        val codepoints = choice.codepoints()
        val si = textSection.startIndex
        val charIndex = si + suggestion.start
        // remove incorrect letters
        for (i in suggestion.end - suggestion.start - 1 downTo 0) {
            content.remove(charIndex + i)
        }
        // insert corrected letters
        for (i in codepoints.indices.reversed()) {
            content.insert(charIndex, codepoints[i])
        }
        onChangeText()
    }

    override fun getTooltipText(x: Float, y: Float): String? {
        val xi = getCharXiRound(x.toInt()) - getCharsNeededForLineCount()
        val yi = getCharYiFloor(y.toInt())
        // find if there is a variable
        if (yi in 0 until content.lineCount && xi in 0 until content.getLineLength(yi)) {
            // find which char is here
            val charIndex = content.getIndexAt(yi, xi)
            // binary search...
            var variableIndex = spellcheckedSections.binarySearch { e -> e.startIndex.compareTo(charIndex) }
            if (variableIndex < 0) variableIndex = -2 - variableIndex
            if (variableIndex in spellcheckedSections.indices) {
                val variable = spellcheckedSections[variableIndex]
                if (charIndex in variable) {
                    // check if there is a correction for it
                    val suggestions = variable.spellcheck()
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
            // return TokenType.values2[styles[charIndex].toInt()].name
        }
        lastSuggestion = null
        return super.getTooltipText(x, y)
    }

    // accept tabs
    override fun isKeyInput(): Boolean = true
    override fun acceptsChar(char: Int): Boolean = true

    override fun clone(): Panel {
        val clone = CodeEditor(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is CodeEditor) return
        dst.language = language
        // todo more to copy?
    }

    companion object {

        @JvmStatic
        fun drawSquiggles1(x0: Int, x1: Int, y: Int, h: Int, color: Int) {
            when (h) {
                1 -> drawRect(x0, y, x1 - x0, h, color)
                2 -> {
                    drawRectStriped(x0, y + 0, x1 - x0, 1, 0, 2, color)
                    drawRectStriped(x0, y + 1, x1 - x0, 1, 1, 2, color)
                }
                3 -> {
                    drawRectStriped(x0, y + 0, x1 - x0, 1, 0, 4, color)
                    drawRectStriped(x0, y + 1, x1 - x0, 1, 1, 2, color)
                    drawRectStriped(x0, y + 2, x1 - x0, 1, 2, 4, color)
                }
                else -> {
                    // optimized shader for this instead of that many draw calls?
                    for (x in x0 until x1) {
                        drawRect(x, y + wave(x, h), 1, 1, color)
                    }
                }
            }
        }

        @JvmStatic
        fun wave(x: Int, h: Int): Int {
            val period = 2 * h - 2
            val p = x % period
            return if (p < h) p else period - h
        }

        @JvmStatic
        fun registerActions() {
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
            ActionManager.register("CodeEditor.r.t.c", "Replace")
            ActionManager.register("CodeEditor.f.t.c", "Search")
        }
    }
}