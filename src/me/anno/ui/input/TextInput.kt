package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.Cursor
import me.anno.io.files.FileReference
import me.anno.language.translation.NameDesc
import me.anno.ui.base.components.Padding
import me.anno.ui.base.groups.PanelContainer
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.Style
import me.anno.ui.input.components.PureTextInputML

@Suppress("unused")
open class TextInput(nameDesc: NameDesc, val visibilityKey: String, enableSpellcheck: Boolean, style: Style) :
    PanelContainer(PureTextInput(style.getChild("deep")), Padding(), style), InputPanel<String>, TextStyleable {

    constructor(style: Style) : this(NameDesc.EMPTY, "", true, style)

    constructor(nameDesc: NameDesc, visibilityKey: String, value: String?, style: Style) :
            this(nameDesc, visibilityKey, true, value, style)

    constructor(nameDesc: NameDesc, visibilityKey: String, enableSuggestions: Boolean, value: String?, style: Style) :
            this(nameDesc, visibilityKey, enableSuggestions, style) {
        setValue(value ?: "", false)
    }

    val base: PureTextInputML = child as PureTextInputML
    private var isSelectedListener: (() -> Unit)? = null

    init {
        tooltip = nameDesc.desc
        base.enableSpellcheck = enableSpellcheck
        base.placeholder = nameDesc.name
        base.backgroundColor = backgroundColor
    }

    override var isEnabled: Boolean
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
    fun deleteBefore(force: Boolean) = base.deleteBefore(force)
    fun deleteAfter(force: Boolean) = base.deleteAfter(force)
    fun ensureCursorBounds() = base.ensureCursorBounds()
    fun addChangeListener(listener: (text: String) -> Unit): TextInput {
        base.addChangeListener(listener)
        return this
    }

    fun setEnterListener(listener: (text: String) -> Unit): TextInput {
        base.setEnterListener(listener)
        return this
    }

    override val value: String get() = base.value

    override fun setValue(newValue: String, mask: Int, notify: Boolean): TextInput {
        base.setText(newValue, notify)
        return this
    }

    override fun onUpdate() {
        super.onUpdate()
        isSelectedListener?.invoke()
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
        get() = base.isInputAllowed && base.isEnabled && isEnabled
        set(value) {
            base.isInputAllowed = value
        }

    override fun getCursor(): Cursor = Cursor.drag

    override fun isKeyInput() = true

    override fun onCopyRequested(x: Float, y: Float): Any? {
        return base.onCopyRequested(x, y)
    }

    override fun clone(): TextInput {
        val clone = TextInput(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as TextInput
        dst.base.placeholder = base.placeholder
        dst.base.backgroundColor = base.backgroundColor
    }
}