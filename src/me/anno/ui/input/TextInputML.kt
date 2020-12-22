package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.input.components.PureTextInputML
import me.anno.ui.style.Style
import java.io.File

// todo line numbers? :)
open class TextInputML(title: String, style: Style): PanelContainer(
    PureTextInputML(
        style.getChild("deep")
    ), Padding(2), style.getChild("deep")) {

    constructor(title: String, style: Style, v0: String): this(title, style){
        setText(v0, false)
    }

    val base = child as PureTextInputML

    init {
        base.placeholder = title
        base.backgroundColor = backgroundColor
    }

    fun setCursorToEnd() = base.setCursorToEnd()

    fun setPlaceholder(text: String): TextInputML {
        base.placeholder = text
        return this
    }

    fun deleteKeys() = base.deleteSelection()
    fun addKey(codePoint: Int) = base.addKey(codePoint)
    fun insert(insertion: String) = base.insert(insertion)
    fun insert(insertion: Int) = base.insert(insertion, true)
    fun deleteBefore() = base.deleteBefore()
    fun deleteAfter() = base.deleteAfter()
    fun ensureCursorBounds() = base.ensureCursorBounds()
    fun setChangeListener(listener: (text: String) -> Unit): TextInputML {
        base.changeListener = listener
        return this
    }

    fun setText(text: String, notify: Boolean): TextInputML {
        base.setText(text, notify)
        /*base.text = text
        updateChars()*/
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if(base.isInFocus){
            isSelectedListener?.invoke()
        }
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): TextInputML {
        isSelectedListener = listener
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<File>) {
        val keyFile = files.firstOrNull() ?: return
        setText(keyFile.toString(), true)
    }

    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true

}