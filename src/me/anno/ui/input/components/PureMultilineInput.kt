package me.anno.ui.input.components

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import me.anno.utils.clamp
import kotlin.math.abs
import kotlin.streams.toList

class PureMultilineInput(style: Style): PanelListY(style){

    class CursorPosition(val x: Int, val y: Int): Comparable<CursorPosition> {
        override fun hashCode(): Int = x+y*65536
        override fun compareTo(other: CursorPosition): Int = hashCode().compareTo(other.hashCode())
        override fun equals(other: Any?): Boolean {
            return other is CursorPosition && other.x == x && other.y == y
        }
    }

    fun <V: Comparable<V>> min(a: V, b: V): V = if(a < b) a else b
    fun <V: Comparable<V>> max(a: V, b: V): V = if(a > b) a else b

    var cursor1 = CursorPosition(0,0)
    var cursor2 = CursorPosition(0,0)

    val cursorEnd get() = CursorPosition(lines.size-1, lines.last().size)

    val lines: ArrayList<MutableList<Int>> = arrayListOf(mutableListOf())

    fun setCursorToEnd(){
        cursor1 = cursorEnd
        cursor2 = cursor1
    }

    private var text = ""
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
        changeListener(text)
    }

    val jointText get() = lines.joinToString("\n"){
        list -> list.joinToString(""){ String(Character.toChars(it)) }
    }

    fun updateText(){
        text = jointText
        changeListener(text)
    }

    fun deleteSelection(): Boolean {
        val min = min(cursor1, cursor2)
        val max = max(cursor1, cursor2)
        TODO()
        /*for(i in max-1 downTo min){
            characters.removeAt(i)
        }
        updateText()
        return max > min*/
    }

    var drawingOffset = 0
    var lastMove = 0L

    val wasJustChanged get() = abs(GFX.lastTime-lastMove) < 200_000_000

    fun calculateOffset(required: Int, cursor: Int){
        // center the cursor, 1/3 of the width, if possible;
        // clamp left/right
        drawingOffset = -clamp(cursor - w / 3, 0, max(0, required - w))
    }

    fun addKey(codePoint: Int) = insert(codePoint)

    fun insert(insertion: String){
        lastMove = GFX.lastTime
        insertion.codePoints().forEach {
            insert(it)
        }
    }

    fun insert(insertion: Int){
        lastMove = GFX.lastTime
        deleteSelection()
        TODO()
        /*if(cursor1 < characters.size){
            characters.add(cursor1, insertion)
        } else {
            characters.add(insertion)
        }
        updateText()
        cursor1++
        cursor2++
        ensureCursorBounds()*/
    }

    fun deleteBefore(){
        TODO()
        /*lastMove = GFX.lastTime
        if(!deleteSelection() && cursor1 > 0){
            characters.removeAt(cursor1-1)
            updateText()
            cursor1--
            cursor2--
        }
        ensureCursorBounds()*/
    }

    fun deleteAfter(){
        TODO()
        /*lastMove = GFX.lastTime
        if(!deleteSelection() && cursor1 < characters.size){
            characters.removeAt(cursor1)
            updateText()
        }
        ensureCursorBounds()*/
    }

    fun ensureCursorBounds(){
        TODO()
        // cursor1 = clamp(cursor1, 0, characters.size)
        // cursor2 = clamp(cursor2, 0, characters.size)
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        lastMove = GFX.lastTime
        addKey(key)
    }

    fun moveRight(){
        TODO()
        /*lastMove = GFX.lastTime
        if(Input.isShiftDown){
            cursor2++
        } else {
            if(cursor2 != cursor1){
                cursor1 = cursor2
            } else {
                cursor1++
                cursor2 = cursor1
            }
        }
        ensureCursorBounds()*/
    }

    fun moveLeft(){
        TODO()
        /*lastMove = GFX.lastTime
        if(Input.isShiftDown){
            cursor2--
        } else {
            cursor1--
            cursor2 = cursor1
        }
        ensureCursorBounds()*/
    }

    fun moveUp(){
        TODO()
    }

    fun moveDown(){
        TODO()
    }

    override fun onBackKey(x: Float, y: Float) {
        deleteBefore()
    }

    override fun onDeleteKey(x: Float, y: Float) {
        deleteAfter()
    }

    override fun onCopyRequested(x: Float, y: Float): String? {
        TODO()
        //return characters.subList(min(cursor1, cursor2), max(cursor1, cursor2)).joinToString(""){ String(Character.toChars(it)) }
    }

    override fun onPaste(x: Float, y: Float, data: String, type: String) {
        insert(data)
    }

    var changeListener: (text: String) -> Unit = {
            _ ->
    }

    override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
        // todo find the correct location for the cursor
        lastMove = GFX.lastTime
        if(Input.isControlDown){
            cursor1 = CursorPosition(0,0)
            cursor2 = cursorEnd
        } else {
            cursor1 = cursorEnd
            cursor2 = cursor1
        }
        super.onMouseClicked(x, y, button, long)
    }

    override fun onDoubleClick(x: Float, y: Float, button: Int) {
        selectAll()
    }

    override fun onSelectAll(x: Float, y: Float) {
        selectAll()
    }

    fun selectAll(){
        cursor1 = CursorPosition(0,0)
        cursor2 = cursorEnd
    }

    override fun onEmpty(x: Float, y: Float) {
        deleteSelection()
    }

    fun clearText(){
        setText("")
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
            "DeleteAfter" -> deleteAfter()
            "DeleteBefore" -> deleteBefore()
            "DeleteSelection" -> deleteSelection()
            "MoveLeft" -> moveLeft()
            "MoveRight" -> moveRight()
            "Clear" -> clearText()
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun getCursor() = Cursor.editText
    override fun isKeyInput() = true
    override fun getClassName() = "PureTextInput"

}