package me.anno.ui.input

import me.anno.animation.Type
import me.anno.gpu.Cursor
import me.anno.input.Input.isLeftDown
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.base.text.TextStyleable
import me.anno.ui.input.components.NumberInputComponent
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.style.Style
import me.anno.utils.types.Strings.isBlank2

abstract class NumberInput<BaseType>(
    style: Style,
    title: String,
    val visibilityKey: String,
    val type: Type = Type.FLOAT,
    inputPanel0: NumberInputComponent?
) : PanelListY(style), InputPanel<BaseType>, TextStyleable {

    var hasValue = false

    val inputPanel = inputPanel0 ?: NumberInputComponent(visibilityKey, style)

    var titleView = if (title.isBlank2()) null else TitlePanel(title, this, style)
    var isSelectedListener: (() -> Unit)? = null

    var title = title
        set(value) {
            field = value
            if (titleView == null && !value.isBlank2()) {
                val titleView = TitlePanel(title, this, style)
                this.titleView = titleView
                add(0, titleView)
            }
            titleView?.text = value
        }

    override var textSize: Float
        get() = inputPanel.textSize
        set(value) {
            titleView?.textSize = value
            inputPanel.textSize = value
            invalidateLayout()
        }

    override var textColor: Int
        get() = inputPanel.textColor
        set(value) {
            titleView?.textColor = value
            inputPanel.textColor = value
            invalidateLayout()
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

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView?.isInFocus == true || isInFocus || inputPanel.isInFocus
        if (focused1) {
            isSelectedListener?.invoke()
        }
        /*if(RemsStudio.hideUnusedProperties){
            val focused2 = focused1 || (owningProperty != null && owningProperty == selectedProperty)
            inputPanel.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }*/
        super.onDraw(x0, y0, x1, y1)
        when (this) {
            is IntInput -> updateValueMaybe()
            is FloatInput -> updateValueMaybe()
            else -> throw RuntimeException()
        }
    }

    fun setPlaceholder(placeholder: String) {
        inputPanel.placeholder = placeholder
    }

    init {
        val titleView = titleView
        if (titleView != null) {
            add(titleView)
            titleView.enableHoverColor = true
            titleView.disableFocusColors()
            titleView.addLeftClickListener { InputVisibility.toggle(visibilityKey, this) }
        }
        add(inputPanel)
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
        inputPanel.hide()
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
        // super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown) {
            changeValue(dx, dy)
        } else super.onMouseMoved(x, y, dx, dy)
    }

    abstract fun changeValue(dx: Float, dy: Float)

    fun setText(newText: String, notify: Boolean) {
        val oldText = inputPanel.text
        if (oldText == newText) return
        inputPanel.setText(newText, notify)
        if (oldText.length != newText.length) {
            inputPanel.setCursorToEnd()
        }
    }

    override fun getCursor(): Long = Cursor.drag

}