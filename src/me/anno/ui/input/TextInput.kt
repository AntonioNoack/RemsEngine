package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.io.files.FileReference
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style

open class TextInput(
    title: String,
    val visibilityKey: String,
    val enableSpellcheck: Boolean, style: Style
) : PanelContainer(
    object : PureTextInput(style) {
        override val enableSpellcheck = enableSpellcheck
    }, Padding(), style
), TextStyleable {

    constructor(title: String, visibilityKey: String, v0: String?, style: Style) :
            this(title, visibilityKey, true, v0, style)

    constructor(title: String, visibilityKey: String, enableSuggestions: Boolean, v0: String?, style: Style) :
            this(title, visibilityKey, enableSuggestions, style) {
        setValue(v0 ?: "", false)
    }

    val base = child as PureTextInput
    private var isSelectedListener: (() -> Unit)? = null

    init {
        base.placeholder = title
        base.backgroundColor = backgroundColor
    }

    override var visibility: Visibility
        get() = InputVisibility[visibilityKey]
        set(value) {}

    fun setCursorToEnd() = base.setCursorToEnd()
    fun updateChars(notify: Boolean) = base.updateChars(notify)
    fun updateText(notify: Boolean) = base.updateText(notify)

    fun setPlaceholder(text: String): TextInput {
        base.placeholder = text
        return this
    }

    override fun setBold(bold: Boolean) {
        base.setBold(bold)
    }

    override fun setItalic(italic: Boolean) {
        base.setItalic(italic)
    }

    fun deleteKeys() = base.deleteSelection()
    fun addKey(codePoint: Int) = base.addKey(codePoint)
    fun insert(insertion: String) = base.insert(insertion)
    fun insert(insertion: Int) = base.insert(insertion)
    fun deleteBefore() = base.deleteBefore()
    fun deleteAfter() = base.deleteAfter()
    fun ensureCursorBounds() = base.ensureCursorBounds()
    fun addChangeListener(listener: (text: String) -> Unit): TextInput {
        base.addChangeListener(listener)
        return this
    }

    fun setEnterListener(listener: (text: String) -> Unit): TextInput {
        base.setEnterListener(listener)
        return this
    }

    val text get() = base.text

    fun setValue(text: String, notify: Boolean): TextInput {
        base.text = text
        updateChars(notify)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (base.isInFocus) {
            isSelectedListener?.invoke()
        }
    }

    fun setIsSelectedListener(listener: () -> Unit): TextInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> String?): TextInput {
        base.setResetListener(listener)
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val keyFile = files.firstOrNull() ?: return
        setValue(keyFile.toString(), true)
    }

    override fun requestFocus() {
        base.requestFocus()
    }

    override val className get() = "TextInput"
    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true


}