package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.studio.Studio
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style

abstract class NumberInput<Type>(
    style: Style, title: String,
    val type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
): PanelListY(style) {

    var lastValue: Type = getValue(type.defaultValue)
    var changeListener = { value: Type -> }

    val titlePanel = object: TextPanel(title, style){
        override fun onMouseDown(x: Float, y: Float, button: Int) { this@NumberInput.onMouseDown(x,y,button) }
        override fun onMouseUp(x: Float, y: Float, button: Int) { this@NumberInput.onMouseUp(x,y,button) }
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) { this@NumberInput.onMouseMoved(x,y,dx,dy) }
    }

    val inputPanel = object: PureTextInput(style.getChild("deep")){
        val driver get() = owningProperty?.drivers?.get(indexInProperty)
        val hasDriver get() = driver != null
        override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
            val driver = driver
            val driverName = driver?.getDisplayName()
            if(driver != null && text != driverName){
                text = driverName!!
                updateChars()
            }
            super.draw(x0, y0, x1, y1)
        }
        override fun onMouseDown(x: Float, y: Float, button: Int) {
            if(!hasDriver){
                super.onMouseDown(x, y, button)
                this@NumberInput.onMouseDown(x,y,button)
            }
        }
        override fun onMouseUp(x: Float, y: Float, button: Int) {
            if(!hasDriver) this@NumberInput.onMouseUp(x,y,button)
        }
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            if(!hasDriver) this@NumberInput.onMouseMoved(x,y,dx,dy)
        }
        override fun onMouseClicked(x: Float, y: Float, button: Int, long: Boolean) {
            if(owningProperty != null && (button != 0 || long)){
                val oldDriver = owningProperty.drivers[indexInProperty]
                AnimationDriver.openDriverSelectionMenu(x.toInt(), y.toInt(), oldDriver){
                    owningProperty.drivers[indexInProperty] = it
                    if(it != null) Studio.selectedInspectable = it
                }
                return
            }
            super.onMouseClicked(x, y, button, long)
        }

        override fun onEmpty(x: Float, y: Float) {
            this@NumberInput.onEmpty(x,y)
        }
    }

    var wasInFocus = false

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        val focused1 = titlePanel.isInFocus || inputPanel.isInFocus
        if(focused1) isSelectedListener?.invoke()
        val focused2 = focused1 || (owningProperty != null && owningProperty == Studio.selectedProperty)
        inputPanel.visibility = if(focused2) Visibility.VISIBLE else Visibility.GONE
        super.draw(x0, y0, x1, y1)
        updateValueMaybe()
    }

    fun updateValueMaybe(){
        if(inputPanel.isInFocus){
            wasInFocus = true
        } else if(wasInFocus){
            // apply the value, or reset if invalid
            val value = parseValue(inputPanel.text) ?: lastValue
            setValue(value)
            wasInFocus = false
        }
    }

    fun setPlaceholder(placeholder: String){
        inputPanel.placeholder = placeholder
    }

    abstract fun parseValue(text: String): Type?

    init {
        this += titlePanel
        this += inputPanel.setChangeListener {
            val newValue = parseValue(it)
            if(newValue != null){
                lastValue = newValue
                changeListener(newValue)
            }
        }
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
    }

    fun setValue(v: Type){
        if(v != lastValue){
            lastValue = v
            changeListener(v)
            inputPanel.text = stringify(v)
            inputPanel.updateChars()
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

    override fun onMouseDown(x: Float, y: Float, button: Int) {
        super.onMouseDown(x, y, button)
        mouseIsDown = true
    }

    var mouseIsDown = false
    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        super.onMouseMoved(x, y, dx, dy)
        if(mouseIsDown){
            changeValue(dx, dy)
        }
    }

    abstract fun changeValue(dx: Float, dy: Float)

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    abstract fun getValue(value: Any): Type

    override fun onEmpty(x: Float, y: Float) {
        setValue(getValue(type.defaultValue))
    }

    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true

}