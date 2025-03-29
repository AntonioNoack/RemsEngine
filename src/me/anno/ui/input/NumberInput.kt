package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.Input.isLeftDown
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.NumberInputComponent
import me.anno.ui.input.components.TitlePanel
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

abstract class NumberInput<BaseType>(
    style: Style,
    nameDesc: NameDesc,
    val visibilityKey: String,
    val type: NumberType = NumberType.FLOAT,
    inputPanel0: NumberInputComponent?
) : PanelListY(style), InputPanel<BaseType>, TextStyleable {

    companion object {
        private val LOGGER = LogManager.getLogger(NumberInput::class)
    }

    var hasValue = false

    val inputPanel = inputPanel0 ?: NumberInputComponent(visibilityKey, style)

    var titleView = if (nameDesc.name.isBlank2()) null else TitlePanel(nameDesc, this, style)
    var isSelectedListener: (() -> Unit)? = null

    var title = nameDesc
        set(value) {
            field = value
            if (titleView == null && !value.name.isBlank2()) {
                val titleView = TitlePanel(title, this, style)
                this.titleView = titleView
                add(0, titleView)
            }
            titleView?.text = value.name
            tooltip = value.desc
        }

    override var isEnabled: Boolean
        get() = super.isEnabled
        set(value) {
            super.isEnabled = value
            inputPanel.isEnabled = value
        }

    override var isInputAllowed
        get() = inputPanel.isInputAllowed
        set(value) {
            inputPanel.isInputAllowed = value
        }

    override var textSize: Float
        get() = inputPanel.textSize
        set(value) {
            titleView?.textSize = value
            inputPanel.textSize = value
        }

    override var textColor: Int
        get() = inputPanel.textColor
        set(value) {
            titleView?.textColor = value
            inputPanel.textColor = value
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

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView?.isInFocus == true || isInFocus || inputPanel.isInFocus
        if (focused1) {
            isSelectedListener?.invoke()
        }
        super.draw(x0, y0, x1, y1)
        when (this) {
            is IntInput -> updateValueMaybe()
            is FloatInput -> updateValueMaybe()
            else -> LOGGER.warn("Unknown child type")
        }
    }

    fun setPlaceholder(placeholder: String): NumberInput<BaseType> {
        inputPanel.placeholder = placeholder
        return this
    }

    init {
        val titleView = titleView
        if (titleView != null) {
            add(titleView)
            titleView.enableHoverColor = true
            titleView.disableFocusColors()
            titleView.addLeftClickListener {
                InputVisibility.toggle(visibilityKey)
            }
        }
        add(inputPanel)
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = nameDesc.name
        inputPanel.hide()
        tooltip = nameDesc.desc
    }

    fun setIsSelectedListener(listener: () -> Unit): NumberInput<BaseType> {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> String): NumberInput<BaseType> {
        inputPanel.setResetListener(listener)
        return this
    }

    private val mouseIsDown get() = isAnyChildInFocus && isLeftDown

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (mouseIsDown && isInputAllowed && InputVisibility[visibilityKey]) {
            changeValue(dx, dy)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    override fun wantsMouseTeleport(): Boolean = true

    abstract fun changeValue(dx: Float, dy: Float)

    fun setText(newText: String, notify: Boolean) {
        val oldText = inputPanel.value
        if (oldText == newText) return
        inputPanel.setText(newText, notify)
        if (oldText.length != newText.length) {
            inputPanel.setCursorToEnd()
        }
    }

    override fun getCursor(): Cursor = Cursor.drag
}