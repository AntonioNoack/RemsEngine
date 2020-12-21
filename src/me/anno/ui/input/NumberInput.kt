package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.studio.rems.RemsStudio
import me.anno.studio.rems.RemsStudio.selectedProperty
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.NumberInputComponent
import me.anno.ui.input.components.TitlePanel
import me.anno.ui.style.Style

abstract class NumberInput(
    style: Style, title: String,
    val type: Type = Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
) : PanelListY(style) {

    fun noTitle(): NumberInput {
        titleView.hide()
        inputPanel.show()
        return this
    }

    var hasValue = false

    val titleView = TitlePanel(title, this, style)

    val inputPanel = NumberInputComponent(owningProperty, indexInProperty, this, style)

    var wasInFocus = false

    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titleView.isInFocus || inputPanel.isInFocus
        if (focused1) isSelectedListener?.invoke()
        if(RemsStudio.hideUnusedProperties){
            val focused2 = focused1 || (owningProperty != null && owningProperty == selectedProperty)
            inputPanel.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        }
        super.onDraw(x0, y0, x1, y1)
        when(this){
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
        titleView.setSimpleClickListener { inputPanel.toggleVisibility() }
        this += inputPanel
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
        inputPanel.hide()
    }

    var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): NumberInput {
        isSelectedListener = listener
        return this
    }

    override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    var mouseIsDown = false
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

    override fun getCursor(): Long = Cursor.drag

}