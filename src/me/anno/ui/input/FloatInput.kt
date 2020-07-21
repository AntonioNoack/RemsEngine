package me.anno.ui.input

import me.anno.gpu.Cursor
import me.anno.gpu.GFX
import me.anno.input.Input.isShiftDown
import me.anno.utils.pow
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.parser.SimpleExpressionParser
import me.anno.studio.Studio
import me.anno.ui.base.TextPanel
import me.anno.ui.base.Visibility
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.components.PureTextInput
import me.anno.ui.style.Style
import me.anno.utils.get
import kotlin.math.max

class FloatInput(
    style: Style, title: String,
    val type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    val owningProperty: AnimatedProperty<*>?,
    val indexInProperty: Int
): PanelListY(style) {

    constructor(title: String, owningProperty: AnimatedProperty<*>, indexInProperty: Int, time: Double, style: Style): this(style, title, owningProperty.type, owningProperty, indexInProperty){
        val value = owningProperty[time]
        if(value is Double) setValue(value)
        else setValue(value as Float)
    }

    constructor(title: String, value0: Float, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Double, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Float, style: Style): this(style, title, AnimatedProperty.Type.FLOAT, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Double, style: Style): this(style, title, AnimatedProperty.Type.DOUBLE, null, 0){
        setValue(value0)
    }

    val titlePanel = object: TextPanel(title, style){
        override fun onMouseDown(x: Float, y: Float, button: Int) { this@FloatInput.onMouseDown(x,y,button) }
        override fun onMouseUp(x: Float, y: Float, button: Int) { this@FloatInput.onMouseUp(x,y,button) }
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) { this@FloatInput.onMouseMoved(x,y,dx,dy) }
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
                this@FloatInput.onMouseDown(x,y,button)
            }
        }
        override fun onMouseUp(x: Float, y: Float, button: Int) {
            if(!hasDriver) this@FloatInput.onMouseUp(x,y,button)
        }
        override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
            if(!hasDriver) this@FloatInput.onMouseMoved(x,y,dx,dy)
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
    }

    var lastValue = 0.0
    var allowInfinity = false
    var wasInFocus = false

    var changeListener = { value: Double -> }

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
            val value = isValid(inputPanel.text) ?: lastValue
            setValue(value)
            wasInFocus = false
        }
    }

    fun setPlaceholder(placeholder: String){
        inputPanel.placeholder = placeholder
    }

    fun isValid(text: String): Double? {
        val trimmed = text.trim()
        val newValue = if(trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull() ?: SimpleExpressionParser.parseDouble(trimmed)
        if(newValue == null || !((allowInfinity && !newValue.isNaN()) || newValue.isFinite())) return null
        return newValue
    }

    init {
        this += titlePanel
        this += inputPanel.setChangeListener {
            val newValue = isValid(it)
            if(newValue != null){
                lastValue = newValue
                changeListener(newValue)
            }
        }
        setValue(0f)
        inputPanel.setCursorToEnd()
        inputPanel.placeholder = title
    }

    fun setValue(v: Float) = setValue(v.toDouble())
    fun setValue(v: Double){
        if(v != lastValue){
            lastValue = v
            changeListener(v)
            inputPanel.text = stringify(v)
            inputPanel.updateChars()
        }
    }

    fun stringify(v: Double): String {
        val vInt = v.toInt()
        return if(vInt.toDouble() == v) "$vInt"
        else "${v.toFloat()}"
    }

    fun setChangeListener(listener: (value: Double) -> Unit): FloatInput {
        changeListener = listener
        return this
    }

    private var isSelectedListener: (() -> Unit)? = null
    fun setIsSelectedListener(listener: () -> Unit): FloatInput {
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
            val scale = if(type == AnimatedProperty.Type.ROT_YXZ) 10f else 1f
            val size = scale * (if(isShiftDown) 4f else 20f) / max(GFX.width,GFX.height)
            val dx0 = dx*size
            val dy0 = dy*size
            val delta = dx0-dy0
            // chose between exponential and linear curve, depending on the use-case
            var value = lastValue
            if(type.hasLinear) value += delta * 0.1f
            if(type.hasExponential) value *= pow(if(lastValue < 0) 1f / 1.03f else 1.03f, delta * if(type.hasLinear) 1f else 3f)
            setValue(value)
        }
    }

    override fun onMouseUp(x: Float, y: Float, button: Int) {
        super.onMouseUp(x, y, button)
        mouseIsDown = false
    }

    override fun onEmpty(x: Float, y: Float) {
        val defaultValue = type.defaultValue
        val defaultDouble = if(defaultValue is Double) defaultValue else (defaultValue as Float).toDouble()
        setValue(defaultDouble)
    }

    override fun getCursor(): Long = Cursor.drag
    override fun isKeyInput() = true
    override fun getClassName() = "FloatInput"

}