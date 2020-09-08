package me.anno.ui.input.components

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.gpu.GFX.loadTexturesSync
import me.anno.input.Input.isControlDown
import me.anno.input.Input.isShiftDown
import me.anno.input.Input.mouseKeysDown
import me.anno.input.MouseButton
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.utils.clamp
import me.anno.ui.base.TextPanel
import me.anno.ui.style.Style
import me.anno.utils.getIndexFromText
import me.anno.utils.joinChars
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.streams.toList

open class PureTextInput(style: Style): TextPanel("", style.getChild("edit")) {

    val characters = ArrayList<Int>()

    init {
        instantTextLoading = true
    }

    fun setCursorToEnd(){
        cursor1 = characters.size
        cursor2 = cursor1
    }

    fun updateChars(){
        characters.clear()
        characters.addAll(text.codePoints().toList())
        changeListener(text)
    }

    fun updateText(){
        text = characters.joinChars()
        changeListener(text)
    }

    var cursor1 = 0
    var cursor2 = 0

    var placeholderColor = style.getColor("placeholderColor", textColor and 0x7fffffff)
    var placeholder = ""

    fun deleteSelection(): Boolean {
        synchronized(this){
            val min = min(cursor1, cursor2)
            val max = max(cursor1, cursor2)
            for(i in max-1 downTo min){
                characters.removeAt(i)
            }
            updateText()
            cursor1 = min
            cursor2 = min
            onSmallChange("text-delete-selection")
            return max > min
        }
    }

    var drawingOffset = 0
    var lastMove = 0L

    val wasJustChanged get() = abs(GFX.lastTime-lastMove) < 200_000_000

    fun calculateOffset(required: Int, cursor: Int){
        // center the cursor, 1/3 of the width, if possible;
        // clamp left/right
        drawingOffset = -clamp(cursor - w / 3, 0, max(0, required - w))
    }

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        loadTexturesSync.push(true)
        drawBackground()
        val x = x + padding.left
        val y = y + padding.top
        val usePlaceholder = text.isEmpty()
        val textColor = if(usePlaceholder) placeholderColor else effectiveTextColor
        val drawnText = if(usePlaceholder) placeholder else text
        val wh = drawText(drawingOffset, 0, drawnText, textColor)
        val blinkVisible = ((System.nanoTime() / 500_000_000L) % 2L == 0L)
        val showBars = blinkVisible || wasJustChanged
        if(isInFocus && (showBars || cursor1 != cursor2)){
            ensureCursorBounds()
            val padding = textSize/4
            val cursorX1 = if(cursor1 == 0) -1 else GFX.getTextSize(fontName, textSize, isBold, isItalic, text.substring(0, cursor1), -1).first-1
            if(cursor1 != cursor2){
                val cursorX2 = if(cursor2 == 0) -1 else GFX.getTextSize(fontName, textSize, isBold, isItalic, text.substring(0, cursor2), -1).first-1
                val min = min(cursorX1, cursorX2)
                val max = max(cursorX1, cursorX2)
                GFX.drawRect(x+min+drawingOffset, y+padding, max-min, h-2*padding, textColor and 0x3fffffff) // marker
                if(showBars) GFX.drawRect(x+cursorX2+drawingOffset, y+padding, 2, h-2*padding, textColor) // cursor 1
                calculateOffset(wh.first, cursorX2)
            } else {
                calculateOffset(wh.first, cursorX1)
            }
            if(showBars) GFX.drawRect(x+cursorX1+drawingOffset, y+padding, 2, h-2*padding, textColor) // cursor 2
        }
        loadTexturesSync.pop()
    }

    fun addKey(codePoint: Int) = insert(codePoint)

    fun insert(insertion: String){
        lastMove = GFX.lastTime
        insertion.codePoints().forEach {
            insert(it, false)
        }
        updateText()
        onSmallChange("text-insert")
    }

    fun insert(insertion: Int, updateText: Boolean = true){
        lastMove = GFX.lastTime
        deleteSelection()
        characters.add(cursor1, insertion)
        if(updateText) updateText()
        cursor1++
        cursor2++
        ensureCursorBounds()
        onSmallChange("text-insert2")
    }

    fun deleteBefore(){
        lastMove = GFX.lastTime
        if(!deleteSelection() && cursor1 > 0){
            characters.removeAt(cursor1-1)
            updateText()
            cursor1--
            cursor2--
        }
        ensureCursorBounds()
        onSmallChange("text-delete-before")
    }

    fun deleteAfter(){
        lastMove = GFX.lastTime
        if(!deleteSelection() && cursor1 < characters.size){
            characters.removeAt(cursor1)
            updateText()
        }
        ensureCursorBounds()
        onSmallChange("text-delete-after")
    }

    fun ensureCursorBounds(){
        val maxLength = min(characters.size, text.length) // text may not be updated???
        cursor1 = clamp(cursor1, 0, maxLength)
        cursor2 = clamp(cursor2, 0, maxLength)
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        lastMove = GFX.lastTime
        addKey(key)
    }

    fun moveRight(){
        lastMove = GFX.lastTime
        if(isShiftDown){
            cursor2++
        } else {
            if(cursor2 != cursor1){
                cursor1 = cursor2
            } else {
                cursor1++
                cursor2 = cursor1
            }
        }
        ensureCursorBounds()
    }

    fun moveLeft(){
        lastMove = GFX.lastTime
        if(isShiftDown){
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
        if(cursor1 == cursor2) return text
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

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        lastMove = GFX.lastTime
        if(isControlDown){
            selectAll()
        } else {
            // find the correct location for the cursor
            val localX = x - (this.x + padding.left + drawingOffset)
            cursor1 = getIndexFromText(characters, localX, fontName, textSize, isBold, isItalic)
            cursor2 = cursor1
        }
    }

    fun selectAll(){
        cursor1 = 0
        cursor2 = characters.size
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if(!isControlDown){
            if(0 in mouseKeysDown){
                val localX = x - (this.x + padding.left + drawingOffset)
                cursor2 = getIndexFromText(characters, localX, fontName, textSize, isBold, isItalic)
            }
        }
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
        if(text != ""){
            if(cursor1 == cursor2){
                clear()
            } else deleteSelection()
        }
    }

    fun clear(){
        lastMove = GFX.lastTime
        text = ""
        characters.clear()
        cursor1 = 0
        cursor2 = 0
        onSmallChange("text-clear")
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when(action){
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

    override val enableHoverColor: Boolean
        get() = text.isNotEmpty()

    override fun getCursor() = Cursor.editText
    override fun isKeyInput() = true
    override fun getClassName() = "PureTextInput"

}