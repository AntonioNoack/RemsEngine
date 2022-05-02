package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.io.files.FileReference
import me.anno.ui.base.Visibility
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style

@Suppress("unused")
open class TextInput(
    title: String,
    val visibilityKey: String,
    val enableSpellcheck: Boolean, style: Style
) : PanelContainer(
    object : PureTextInput(style) {
        override val enableSpellcheck = enableSpellcheck
    }, Padding(), style
), InputPanel<String>, TextStyleable {

    constructor(style: Style) : this("", "", true, style)

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
        set(_) {}

    fun setCursorToEnd() = base.setCursorToEnd()

    fun setPlaceholder(text: String): TextInput {
        base.placeholder = text
        return this
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

    override val lastValue: String get() = base.text

    override fun setValue(value: String, notify: Boolean): TextInput {
        base.setText(value, notify)
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

    override fun requestFocus(exclusive: Boolean) {
        base.requestFocus(exclusive)
    }

    override var isInputAllowed: Boolean
        get() = base.isInputAllowed
        set(value) {
            base.isInputAllowed = value
        }

    override fun getCursor(): Long = Cursor.drag

    override fun isKeyInput() = true

    override fun clone(): TextInput {
        val clone = TextInput(style)
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as TextInput
        clone.base.placeholder = base.placeholder
        clone.base.backgroundColor = base.backgroundColor
    }

    override val className get() = "TextInput"

}