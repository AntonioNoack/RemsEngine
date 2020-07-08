package me.anno.ui.input.components

import me.anno.config.DefaultStyle.black
import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.ui.base.TextPanel
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelList
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.scrolling.ScrollPanelXY
import me.anno.ui.style.Style
import me.anno.utils.clamp
import me.anno.utils.getIndexFromText
import me.anno.utils.getLineWidth
import me.anno.utils.joinChars
import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.streams.toList

class PureTextInputML(style: Style): ScrollPanelXY(Padding(0), style){

    // todo hide space for scroll panel, if not used/not in focus?

    var cursor1 = CursorPosition(0,0)
    var cursor2 = CursorPosition(0,0)

    var placeholder = ""
        set(value) {
            field = value
            if(text.isEmpty()) update()
        }

    private var text = ""
    private val lines: ArrayList<MutableList<Int>> = arrayListOf(mutableListOf())
    private val lastValidCursor get() = CursorPosition(lines.size-1, lines.last().size)
    private val joinedText get() = lines.joinToString("\n"){ list -> list.joinChars() }
    private val styleSample get() = actualChildren[0] as TextPanel
    private val actualChildren = (content as PanelListY).children
    private val scrollbarStartY get() = if(minW > w) actualChildren.last().run { y + h - 3 } else y+h

    private var lastChangeTime = 0L
    private val wasJustChanged get() = abs(GFX.lastTime-lastChangeTime) < 200_000_000

    class CursorPosition(val x: Int, val y: Int): Comparable<CursorPosition> {
        override fun hashCode(): Int = x+y*65536
        override fun compareTo(other: CursorPosition): Int = hashCode().compareTo(other.hashCode())
        override fun equals(other: Any?): Boolean {
            return other is CursorPosition && other.x == x && other.y == y
        }
    }

    fun updateLines(){
        val needsPlaceholder = text.isEmpty()
        val children = actualChildren
        if(lines.isEmpty()){
            lines.add(mutableListOf())
        }
        while(lines.size < children.size){
            children.removeAt(children.lastIndex)
        }
        val content = content as PanelList
        while(lines.size > children.size){
            val panel = object: TextPanel("", style){
                // override fun onBackKey(x: Float, y: Float) = this@PureTextInputML.onBackKey(x, y)
                // override fun onCharTyped(x: Float, y: Float, key: Int) = this@PureTextInputML.onCharTyped(x, y, key)
                // override fun onEnterKey(x: Float, y: Float) = this@PureTextInputML.onEnterKey(x, y)
                override fun isKeyInput() = true
                override fun onMouseDown(x: Float, y: Float, button: Int) = this@PureTextInputML.onMouseDown(x, indexInParent, button)
                override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) = this@PureTextInputML.onMouseMoved(x, y, dx, dy)
                override fun onCopyRequested(x: Float, y: Float): String? {
                    return this@PureTextInputML.onCopyRequested(x, y)
                }
            }
            children.add(panel)
            panel.parent = content
        }
        lines.forEachIndexed { index, chars ->
            val panel = children[index] as TextPanel
            panel.text = if(needsPlaceholder) placeholder else chars.joinChars()
            panel.textColor = (panel.textColor and 0xffffff) or (if(needsPlaceholder) 70 else 255).shl(24)
            panel.focusTextColor = panel.textColor
        }
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        val blinkVisible = ((System.nanoTime() / 500_000_000L) % 2L == 0L)
        val showBars = blinkVisible || wasJustChanged
        val children = actualChildren
        val examplePanel = children.firstOrNull() as? TextPanel ?: return
        val fontName = examplePanel.fontName
        val textSize = examplePanel.textSize
        val isBold = examplePanel.isBold
        val isItalic = examplePanel.isItalic
        val textColor = examplePanel.textColor or black
        val isReallyInFocus = isInFocus || children.count { it.isInFocus } > 0
        if(isReallyInFocus && (showBars || cursor1 != cursor2)){
            ensureCursorBounds()
            val padding = textSize/4
            val panel1 = children[cursor1.y]
            val line1 = lines[cursor1.y]
            val cursor1Text = line1.subList(0, cursor1.x).joinChars()
            val cursorX1 = if(cursor1.x == 0) -1 else GFX.getTextSize(fontName, textSize, isBold, isItalic, cursor1Text).first-1
            if(cursor1 != cursor2){
                val panel2 = children[cursor2.y]
                val line2 = lines[cursor2.y]
                val cursor2Text = line2.subList(0, cursor2.x).joinChars()
                val cursorX2 = if(cursor2.x == 0) -1 else GFX.getTextSize(fontName, textSize, isBold, isItalic, cursor2Text).first-1
                val minCursor = min(cursor1, cursor2)
                val maxCursor = max(cursor1, cursor2)
                val minPanel = children[minCursor.y] as TextPanel
                val maxPanel = children[maxCursor.y] as TextPanel
                val minCursorX = kotlin.math.min(cursorX1, cursorX2)
                val maxCursorX = kotlin.math.max(cursorX1, cursorX2)
                if(minCursor.y == maxCursor.y){
                    // draw box in same line
                    GFX.drawRect(panel2.x+minCursorX, panel2.y+padding, maxCursorX-minCursorX, panel2.h-2*padding, textColor and 0x3fffffff) // marker
                } else {

                    // todo not working when covered???

                    // draw end of first line
                    GFX.drawRect(minPanel.x+minCursorX, minPanel.y+padding, (w-padding*2)-minCursorX,minPanel.h-padding, textColor and 0x3fffffff)

                    // draw in between lines
                    if(minCursor.y+1 < maxCursor.y){
                        GFX.drawRect(x, minPanel.y+minPanel.h, w, maxPanel.y-minPanel.y-2*minPanel.h, textColor and 0x3fffffff)
                    }

                    // draw start of last line
                    val endX = maxPanel.x+maxCursorX
                    GFX.drawRect(x, maxPanel.y, endX-x,maxPanel.h-padding, textColor and 0x3fffffff)

                }
                if(showBars) GFX.drawRect(panel2.x+cursorX2, panel2.y+padding, 2, panel2.h-2*padding, textColor) // cursor 1
            }
            if(showBars) GFX.drawRect(panel1.x+cursorX1, panel1.y+padding, 2, panel1.h-2*padding, textColor) // cursor 2
        }
    }

    fun <V: Comparable<V>> min(a: V, b: V): V = if(a < b) a else b
    fun <V: Comparable<V>> max(a: V, b: V): V = if(a > b) a else b

    fun setCursorToEnd(){
        cursor1 = lastValidCursor
        cursor2 = cursor1
    }

    fun String.toLines() = split('\n')
            .map { line -> line.codePoints().toList().toMutableList() }

    fun setText(text: String){
        if(text == this.text) return
        this.text = text
        val textLines = text.split('\n')
        for(i in lines.lastIndex downTo textLines.size-1){
            lines.removeAt(i)
        }
        textLines.forEachIndexed { lineIndex, line ->
            if(lines.size <= lineIndex) lines += line.codePoints().toList().toMutableList()
            else {
                val lineList = lines[lineIndex]
                lineList.clear()
                lineList.addAll(line.codePoints().toList())
            }
        }
        update()
    }

    fun updateText(){
        text = joinedText
        changeListener(text)
    }

    fun deleteSelection(): Boolean {
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        if(min == max) return false
        if(min.y < max.y){
            // delete back, in between, and end
            val line0 = lines[min.y]
            lines[min.y] = line0.subList(0, min.x)
            val line1 = lines[max.y]
            lines[max.y] = line1.subList(max.x, line1.size)
            for(i in max.y-1 downTo min.y+1){
                lines.removeAt(i)
            }
        } else {
            // delete in between
            val line = lines[min.y]
            lines[min.y] = (line.subList(0, min.x) + line.subList(max.x, line.size)).toMutableList()
        }
        return true
    }

    fun addKey(codePoint: Int) = insert(codePoint, true)

    fun insert(insertion: String){
        lastChangeTime = GFX.lastTime
        insertion.codePoints().forEach {
            insert(it, false)
        }
        update()
    }

    fun update(){
        updateText()
        updateLines()
    }

    fun findStartingWhitespace(line: List<Int>): List<Int> {
        var i = 0
        while(i < line.size && when(line[i]){
                ' '.toInt(), '\t'.toInt() -> true
                else -> false
            }){ i++ }
        return line.subList(0, i)
    }

    fun insert(insertion: Int, update: Boolean){
        lastChangeTime = GFX.lastTime
        deleteSelection()
        when(insertion){
            '\n'.toInt() -> {
                // split the line here
                val line0 = lines[cursor1.y]
                val line1 = line0.subList(cursor1.x, line0.size)
                lines[cursor1.y] = line0.subList(0, cursor1.x)
                lines.add(cursor1.y+1,
                    (findStartingWhitespace(line0) // help with spaces at the start
                            + line1).toMutableList())
                cursor1 = CursorPosition(0, cursor1.y+1)
                cursor2 = cursor1
            }
            '\r'.toInt() -> {} // ignore, because it's useless ^^
            else -> {
                // just insert the key :)
                val line = lines[cursor1.y]
                line.add(cursor1.x, insertion)
                cursor1 = CursorPosition(cursor1.x+1, cursor1.y)
                cursor2 = cursor1
            }
        }
        ensureCursorBounds()
        if(update) update()
    }

    fun deleteBefore(){
        lastChangeTime = GFX.lastTime
        if(!deleteSelection() && cursor1.x + cursor1.y > 0){
            if(cursor1.x == 0){
                // join lines
                val line0 = lines[cursor1.y-1]
                val joint = (line0 + lines[cursor1.y]).toMutableList()
                lines[cursor1.y-1] = joint
                lines.removeAt(cursor1.y)
                cursor1 = CursorPosition(line0.size, cursor1.y-1)
            } else {
                // remove a char
                lines[cursor1.y].removeAt(cursor1.x-1)
                cursor1 = CursorPosition(cursor1.x-1, cursor1.y)
            }
            update()
            cursor2 = cursor1
        }
        ensureCursorBounds()
    }

    fun deleteAfter(){
        moveRight()
        deleteBefore()
    }

    fun ensureCursorBounds(){
        cursor1 = ensureCursorBounds(cursor1)
        cursor2 = ensureCursorBounds(cursor2)
    }

    fun ensureCursorBounds(cursor0: CursorPosition): CursorPosition {
        var cursor = cursor0
        if(cursor.y !in 0 until lines.size){
            cursor = CursorPosition(cursor.x, clamp(cursor.y, 0, lines.lastIndex))
        }
        val line = lines[cursor.y]
        if(cursor.x !in 0 .. line.size){
            cursor = CursorPosition(clamp(cursor.x, 0, line.size), cursor.y)
        }
        return cursor
    }


    override fun onCharTyped(x: Float, y: Float, key: Int) {
        lastChangeTime = GFX.lastTime
        addKey(key)
    }

    fun moveRight(){
        val useC2 = Input.isShiftDown
        val oldCursor = if(useC2) cursor2 else cursor1
        val currentLine = lines[oldCursor.y]
        val newCursor = if(oldCursor.x < currentLine.size){
            // we can move right
            CursorPosition(oldCursor.x+1, oldCursor.y)
        } else {
            // we need to move down
            CursorPosition(0, min(oldCursor.y+1, lines.lastIndex))
        }
        if(useC2){
            cursor2 = newCursor
        } else {
            cursor1 = newCursor
            cursor2 = cursor1
        }
        ensureCursorBounds()
        lastChangeTime = GFX.lastTime
    }

    fun moveLeft(){
        val useC2 = Input.isShiftDown
        val oldCursor = if(useC2) cursor2 else cursor1
        val newCursor = if(oldCursor.x > 0){
            // we can move left
            CursorPosition(oldCursor.x-1, oldCursor.y)
        } else {
            // we need to move down
            CursorPosition(0, max(0, oldCursor.y-1))
        }
        if(useC2){
            cursor2 = newCursor
        } else {
            cursor1 = newCursor
            cursor2 = cursor1
        }
        ensureCursorBounds()
        lastChangeTime = GFX.lastTime
    }

    fun moveUp(){
        val useC2 = Input.isShiftDown
        val oldCursor = if(useC2) cursor2 else cursor1
        val newCursor = if(oldCursor.y > 0){
            getCursor(getLineWidth(lines[oldCursor.y], oldCursor.x, styleSample),  oldCursor.y-1, false)
        } else return // cannot move up
        if(useC2){
            cursor2 = newCursor
        } else {
            cursor1 = newCursor
            cursor2 = cursor1
        }
        ensureCursorBounds()
        lastChangeTime = GFX.lastTime
    }

    fun moveDown(){
        val useC2 = Input.isShiftDown
        val oldCursor = if(useC2) cursor2 else cursor1
        val newCursor = if(oldCursor.y < lines.lastIndex){
            getCursor(getLineWidth(lines[oldCursor.y], oldCursor.x, styleSample),  oldCursor.y+1, false)
        } else return // cannot move down
        if(useC2){
            cursor2 = newCursor
        } else {
            cursor1 = newCursor
            cursor2 = cursor1
        }
        ensureCursorBounds()
        lastChangeTime = GFX.lastTime
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        if(cursor1 == cursor2) return text
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        if(cursor1.y == cursor2.y){
            return lines[cursor1.y].subList(min.x, max.x).joinChars()
        }
        val answer = StringBuilder((max.y-min.y+1))
        val line0 = lines[min.y]
        val line1 = lines[max.y]
        answer.append(line0.subList(min.x, line0.size).joinChars())
        for(lineY in min.y + 1 until max.y){
            answer.append(lines[lineY].joinChars())
        }
        answer.append(line1.subList(0, max.x).joinChars())
        return answer.toString()
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) { insert(data) }

    var changeListener: (text: String) -> Unit = { _ -> }

    fun getLineIndex(y: Float): Int {
        // find the correct line index
        val yInt = y.toInt()
        var index = actualChildren.binarySearch { it.y.compareTo(yInt) }
        if(index < 0) index = -2-index
        return clamp(index, 0, lines.lastIndex)
    }

    fun onMouseMoved(x: Float, indexY: Int){
        if(!Input.isControlDown){
            if(0 in Input.mouseKeysDown){
                val localX = x - (this.x + padding.left)
                val line = lines[indexY]
                val indexX = getIndexFromText(line, localX, styleSample)
                cursor2 = CursorPosition(indexX, indexY)
                ensureCursorBounds()
            }
        }
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(y >= scrollbarStartY) return super.onMouseMoved(x, y, dx, dy)
        onMouseMoved(x, getLineIndex(y))
    }

    fun getCursor(x: Float, indexY: Int, withOffset: Boolean): CursorPosition {
        val line = lines[indexY]
        var ix = x
        if(withOffset) ix -= this.x+padding.left
        val indexX = getIndexFromText(line, ix, styleSample)
        return CursorPosition(indexX, indexY)
    }

    fun onMouseDown(x: Float, indexY: Int, button: Int){
        if(Input.isControlDown){
            cursor1 = CursorPosition(0,0)
            cursor2 = lastValidCursor
        } else {
            // find the correct location for the cursor
            cursor1 = getCursor(x, indexY, true)
            cursor2 = cursor1
            ensureCursorBounds()
        }
    }

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        if(y >= scrollbarStartY) return super.onMouseDown(x,y,button)
        onMouseDown(x, getLineIndex(y), button)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Int) { selectAll() }
    override fun onSelectAll(x: Float, y: Float) { selectAll() }

    fun selectAll(){
        cursor1 = CursorPosition(0,0)
        cursor2 = lastValidCursor
    }

    override fun onEmpty(x: Float, y: Float) {
        deleteSelection()
        update()
    }

    fun clearText(){ setText("") }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "DeleteAfter" -> deleteAfter()
            "DeleteBefore" -> deleteBefore()
            "DeleteSelection" -> deleteSelection()
            "MoveLeft" -> moveLeft()
            "MoveRight" -> moveRight()
            "MoveUp" -> moveUp()
            "MoveDown" -> moveDown()
            "Clear" -> clearText()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onEnterKey(x: Float, y: Float) { insert('\n'.toInt(), true) }

    override fun getCursor() = Cursor.editText
    override fun isKeyInput() = true
    override fun getClassName() = "PureTextInputML"

    init { update() }

}