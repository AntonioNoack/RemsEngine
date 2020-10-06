package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.editorTime
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.RemsStudio.selectedInspectable
import me.anno.studio.RemsStudio.selectedProperty
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style
import me.anno.utils.get

abstract class NumberInput(
    style: Style, title: String,
    val type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
) : PanelListY(style) {

    var hasValue = false

    val titleView = object : TextPanel(title, style) {
        override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
            this@NumberInput.onMouseDown(x, y, button)
        }

        override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
            this@NumberInput.onMouseUp(x, y, button)
        }

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            this@NumberInput.onMouseMoved(x, y, dx, dy)
        }
    }

    val inputPanel = object : PureTextInput(style.getChild("deep")) {

        val driver get() = owningProperty?.drivers?.get(indexInProperty)
        val hasDriver get() = driver != null
        var lastTime = editorTime

        override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
            val editorTime = editorTime
            if(lastTime != editorTime && owningProperty != null && owningProperty.isAnimated){
                lastTime = editorTime
                val value = owningProperty[editorTime]!![indexInProperty]
                when(this@NumberInput){
                    is IntInput -> setValue(value.toLong(), false)
                    is FloatInput -> setValue(value.toDouble(), false)
                    else -> throw RuntimeException()
                }
            }
            val driver = driver
            if (driver != null) {
                val driverName = driver.getDisplayName()
                if (text != driverName) {
                    text = driverName
                    updateChars(false)
                }
            }
            super.onDraw(x0, y0, x1, y1)
        }

        override fun onMouseDown(x: Float, y: Float, button: MouseButton) {
            if (!hasDriver) {
                super.onMouseDown(x, y, button)
                this@NumberInput.onMouseDown(x, y, button)
            }
        }

        override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
            if (!hasDriver) this@NumberInput.onMouseUp(x, y, button)
        }

        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            if (!hasDriver) this@NumberInput.onMouseMoved(x, y, dx, dy)
        }

        override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
            if (owningProperty != null && (!button.isLeft || long)) {
                val oldDriver = owningProperty.drivers[indexInProperty]
                AnimationDriver.openDriverSelectionMenu(oldDriver) {
                    owningProperty.drivers[indexInProperty] = it
                    if (it != null) selectedInspectable = it
                    else {
                        text = when(this@NumberInput){
                            is IntInput -> stringify(lastValue)
                            is FloatInput -> stringify(lastValue)
                            else -> throw RuntimeException()
                        }
                    }
                    onSmallChange("number-set-driver")
                }
                return
            }
            super.onMouseClicked(x, y, button, long)
        }

        override fun onEmpty(x: Float, y: Float) {
            if (hasDriver) {
                owningProperty?.drivers?.set(indexInProperty, null)
                this@NumberInput.onEmpty(x, y)
            } else this@NumberInput.onEmpty(x, y)
        }

        override fun acceptsChar(char: Int): Boolean {
            return when(char.toChar()){
                '\t', '\n' -> false
                else -> true
            }
        }

    }

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
            onSmallChange("number-drag")
        }
    }

    abstract fun changeValue(dx: Float, dy: Float)

    override fun onMouseUp(x: Float, y: Float, button: MouseButton) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun getCursor(): Long = Cursor.drag

}