package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.studio.RemsStudio
import me.anno.studio.RemsStudio.lastT
import me.anno.studio.RemsStudio.onSmallChange
import me.anno.studio.Studio
import me.anno.studio.Studio.editorTime
import me.anno.studio.history.History
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style
import me.anno.utils.get

abstract class NumberInput<Type>(
    style: Style, title: String,
    val type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
) : PanelListY(style) {

    var hasValue = false
    var lastValue: Type = getValue(type.defaultValue)
    var changeListener = { value: Type -> }

    val titlePanel = object : TextPanel(title, style) {
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
            if(lastTime != editorTime && owningProperty != null && owningProperty.isAnimated){
                lastTime = editorTime
                setValue(owningProperty[editorTime]!![indexInProperty] as Type, false)
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
                    if (it != null) Studio.selectedInspectable = it
                    else {
                        text = stringify(lastValue)
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
        val focused1 = titlePanel.isInFocus || inputPanel.isInFocus
        if (focused1) isSelectedListener?.invoke()
        val focused2 = focused1 || (owningProperty != null && owningProperty == Studio.selectedProperty)
        inputPanel.visibility = if (focused2) Visibility.VISIBLE else Visibility.GONE
        super.onDraw(x0, y0, x1, y1)
        updateValueMaybe()
    }

    fun updateValueMaybe() {
        if (inputPanel.isInFocus) {
            wasInFocus = true
        } else if (wasInFocus) {
            // apply the value, or reset if invalid
            val value = parseValue(inputPanel.text) ?: lastValue
            setValue(value, true)
            wasInFocus = false
        }
    }

    fun setPlaceholder(placeholder: String) {
        inputPanel.placeholder = placeholder
    }

    abstract fun parseValue(text: String): Type?

    init {
        this += titlePanel
        this += inputPanel.setChangeListener {
            val newValue = parseValue(it)
            if (newValue != null) {
                lastValue = newValue
                changeListener(newValue)
            }
        }
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
    }

    fun setValue(v: Type, notify: Boolean) {
        if (v != lastValue || !hasValue) {
            hasValue = true
            lastValue = v
            if(notify) changeListener(v)
            inputPanel.text = stringify(v)
            inputPanel.updateChars(false)
        }
    }

    abstract fun stringify(v: Type): String

    fun setChangeListener(listener: (value: Type) -> Unit): NumberInput<Type> {
        changeListener = listener
        return this
    }

    var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): NumberInput<Type> {
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

    abstract fun getValue(value: Any): Type

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(owningProperty?.defaultValue ?: type.defaultValue)
        if(newValue != lastValue){
            setValue(newValue, true)
            onSmallChange("empty")
        }
    }

    override fun getCursor(): Long = Cursor.drag

}