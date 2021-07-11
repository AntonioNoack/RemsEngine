package me.anno.ui.input

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.NumberInputComponent
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.style.Style

abstract class NumberInput(
    style: Style,
    val title: String,
    val type: Type = Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
) : PanelListY(style) {

    fun noTitle(): NumberInput {
        titleView.hide()
        inputPanel.show()
        InputVisibility.show(title, null)
        return this
    }

    var hasValue = false
    var mouseIsDown = false

    val titleView = TitlePanel(title, this, style)
    var isSelectedListener: (() -> Unit)? = null

    val inputPanel = object : NumberInputComponent(
        owningProperty, indexInProperty, this@NumberInput, style
    ) {
        override val needsSuggestions: Boolean get() = false
        override var visibility: Visibility
            get() = InputVisibility[title]
            set(_) {}
        override fun onEnterKey(x: Float, y: Float) {
            this@NumberInput.onEnterKey(x, y)
        }
    }

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || inputPanel.isInFocus
        if (focused1) isSelectedListener?.invoke()
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
        this += titleView
        titleView.enableHoverColor = true
        titleView.focusTextColor = titleView.textColor
        titleView.setSimpleClickListener { InputVisibility.toggle(title, this) }
        this += inputPanel
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
        inputPanel.hide()
    }

    fun setIsSelectedListener(listener: () -> Unit): NumberInput {
        isSelectedListener = listener
        return this
    }

    fun setResetListener(listener: () -> String): NumberInput {
        inputPanel.setResetListener(listener)
        return this
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if (mouseIsDown) {
            changeValue(dx, dy)
        }
    }

    abstract fun changeValue(dx: Float, dy: Float)

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    fun setText(newText: String){
        val oldText = inputPanel.text
        inputPanel.text = newText
        inputPanel.updateChars(false)
        if(oldText.length != newText.length){
            inputPanel.setCursorToEnd()
        }
    }

    override fun getCursor(): Long = Cursor.drag

}