package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.io.files.FileReference
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.PureTextInputML
import me.anno.ui.style.Style

@Suppress("unused")
open class TextInputML(title: String, style: Style) : PanelContainer(
    PureTextInputML(
        style.getChild("deep")
    ), Padding(2), style.getChild("deep")
), InputPanel<String>, TextStyleable {

    constructor(style: Style) : this("", style)

    constructor(title: String, v0: String, style: Style) : this(title, style) {
        base.setText(v0, false)
    }

    val base = child as PureTextInputML
    val text get() = base.lastValue

    init {
        base.placeholder = title
        base.backgroundColor = backgroundColor
    }

    override var textSize: Float
        get() = base.textSize
        set(value) {
            base.textSize = value
        }

    override var textColor: Int
        get() = base.textColor
        set(value) {
            base.textColor = value
        }

    override var isBold: Boolean
        get() = base.isBold
        set(value) {
            base.isBold = value
        }

    override var isItalic: Boolean
        get() = base.isItalic
        set(value) {
            base.isItalic = value
        }

    override var isInputAllowed: Boolean
        get() = base.isInputAllowed
        set(value) {
            base.isInputAllowed = value
        }

    fun setCursorToEnd() = base.setCursorToEnd()

    fun setPlaceholder(text: String): TextInputML {
        base.placeholder = text
        return this
    }

    override val lastValue: String get() = base.lastValue
    override fun setValue(value: String, notify: Boolean): TextInputML {
        base.setText(value, notify)
        return this
    }

    fun deleteKeys() = base.deleteSelection()
    fun addKey(codePoint: Int) = base.addKey(codePoint)
    fun insert(insertion: String) = base.insert(insertion)
    fun insert(insertion: Int) = base.insert(insertion, true)
    fun deleteBefore() = base.deleteBefore()
    fun deleteAfter() = base.deleteAfter()
    fun ensureCursorBounds() = base.ensureCursorBounds()
    fun addChangeListener(listener: (text: String) -> Unit): TextInputML {
        base.addChangeListener(listener)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (isAnyChildInFocus) {
            isSelectedListener?.invoke()
        }
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): TextInputML {
        isSelectedListener = listener
        return this
    }

    override fun onPasteFiles(x: Float, y: Float, files: List<FileReference>) {
        val keyFile = files.firstOrNull() ?: return
        setValue(keyFile.toString(), true)
    }

    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true

    override fun clone(): TextInputML {
        val clone = TextInputML(base.placeholder, text, style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextInputML
        // ...
    }

    override val className: String get() = "TextInputML"

}