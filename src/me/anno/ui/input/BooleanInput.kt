package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.Checkbox
import me.anno.utils.types.Strings.isBlank2

/**
 * Checkbox with title
 * */
class BooleanInput(
    nameDesc: NameDesc,
    startValue: Boolean,
    defaultValue: Boolean,
    style: Style
) : PanelListX(style), InputPanel<Boolean>, TextStyleable {

    constructor(style: Style) : this(NameDesc.EMPTY, false, false, style)

    private val titleView = if (nameDesc.name.isBlank2()) null else TextPanel("${nameDesc.name}:", style)
    private val checkView = Checkbox(startValue, defaultValue, style.getSize("fontSize", 10), style)

    init {
        if (titleView != null) {
            this += titleView
            titleView.enableHoverColor = true
            titleView.padding.right = 5
            titleView.disableFocusColors()
        }
        tooltip = nameDesc.desc
        this += checkView
    }

    override var value: Boolean
        get() = checkView.value
        set(value) {
            checkView.value = value
        }

    override var isInputAllowed
        get() = checkView.isInputAllowed
        set(value) {
            if (titleView != null) {
                titleView.setTextAlpha(if (value) 1f else 0.5f)
                titleView.enableHoverColor = value
            }
            checkView.isInputAllowed = value
            invalidateDrawing()
        }

    override var textColor: Int
        get() = titleView?.textColor ?: 0
        set(value) {
            titleView?.textColor = value
        }

    override var textSize: Float
        get() = titleView?.textSize ?: 0f
        set(value) {
            titleView?.textSize = value
        }

    override var isBold: Boolean
        get() = titleView?.isBold == true
        set(value) {
            titleView?.isBold = value
        }

    override var isItalic: Boolean
        get() = titleView?.isItalic == true
        set(value) {
            titleView?.isItalic = value
        }

    fun setChangeListener(listener: (value: Boolean) -> Unit): BooleanInput {
        checkView.setChangeListener(listener)
        return this
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
        if (isInFocus) isSelectedListener?.invoke()
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): BooleanInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> Boolean?): BooleanInput {
        checkView.setResetListener(listener)
        return this
    }

    override fun setValue(newValue: Boolean, mask: Int, notify: Boolean): BooleanInput {
        checkView.setValue(newValue, notify)
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        if (isInputAllowed) checkView.onEmpty(x, y)
        else super.onEmpty(x, y)
    }

    override fun onCopyRequested(x: Float, y: Float) = checkView.value.toString()

    override fun clone(): BooleanInput {
        val clone = BooleanInput(style)
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is BooleanInput) return
        dst.titleView?.text = titleView?.text ?: ""
        // only works, if there is no references
        dst.isSelectedListener = isSelectedListener
        dst.value = value
    }
}