package me.anno.ui.input

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.input.Input.shiftSlowdown
import me.anno.language.translation.NameDesc
import me.anno.parser.SimpleExpressionParser
import me.anno.ui.Style
import me.anno.ui.input.components.NumberInputComponent
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.roundToLongOr
import me.anno.utils.types.Strings.isBlank2
import org.joml.Vector
import kotlin.math.max
import kotlin.math.pow

// must be open for Rem's Studio
open class FloatInput(
    nameDesc: NameDesc,
    visibilityKey: String,
    type: NumberType = NumberType.FLOAT,
    style: Style,
    inputPanel0: NumberInputComponent? = null
) : NumberInput<Double>(style, nameDesc, visibilityKey, type, inputPanel0) {

    constructor(style: Style) : this(NameDesc.EMPTY, "", NumberType.FLOAT, style)

    final override var value: Double = getValue(type.defaultValue)
    var changeListener: (value: Double) -> Unit = { }

    var allowInfinity = false

    init {
        // to do only override text, if the users presses enter (??)
        setText(value.toString(), false)
        inputPanel.addChangeListener {
            val newValue = parseValue(it)
            value = newValue
            changeListener(newValue)
        }
    }

    constructor(nameDesc: NameDesc, visibilityKey: String, value0: Float, type: NumberType, style: Style) :
            this(nameDesc, visibilityKey, type, style) {
        setValue(value0, false)
    }

    constructor(nameDesc: NameDesc, value0: Float, type: NumberType, style: Style) :
            this(nameDesc, "", type, style) {
        setValue(value0, false)
    }

    constructor(nameDesc: NameDesc, visibilityKey: String, value0: Double, type: NumberType, style: Style) :
            this(nameDesc, visibilityKey, type, style) {
        setValue(value0, -1, false)
    }

    fun parseValue(text: String): Double {
        if (text.isBlank2()) return 0.0
        val trimmed = text.trim().replace(',', '.')
        val newValue = trimmed.toDoubleOrNull() ?: SimpleExpressionParser.parseDouble(trimmed)
        if (newValue == null || !((allowInfinity && !newValue.isNaN()) || newValue.isFinite())) return this.value
        return clamp(newValue)
    }

    fun setValue(v: Int, notify: Boolean): FloatInput {
        setValue(v.toDouble(), -1, notify)
        return this
    }

    fun setValue(v: Long, notify: Boolean): FloatInput {
        setValue(v.toDouble(), -1, notify)
        return this
    }

    fun setValue(v: Float, notify: Boolean): FloatInput {
        setValue(v.toDouble(), -1, notify)
        return this
    }

    // todo prefer the default notation over the scientific one
    // todo especially, if the user input is that way
    // todo match the user input?
    fun stringify(v: Double): String {
        // if it was not in focus, the value may have been from the system, and the user may not prefer it
        // we could also use a setting :)
        /*val userWasScientific = isInFocus && inputPanel.text.run {
            contains("e+", true) || contains("e-", true)
        }*/
        return if (type.defaultValue is Double) v.toString()
        else v.toFloat().toString()
    }

    override fun changeValue(dx: Float, dy: Float) {
        val scale = 20f * shiftSlowdown
        val ws = windowStack
        val size = scale / max(ws.width, ws.height)
        val dx0 = dx * size
        val dy0 = dy * size
        val delta = dx0 - dy0
        // chose between exponential and linear curve, depending on the use-case
        var value = value
        if (type.hasLinear || value == 0.0) value += delta * 0.1 * type.unitScale
        if (type.hasExponential) value *= (if (value < 0) 1.0 / 1.03 else 1.03).pow(delta * if (type.hasLinear) 1.0 else 3.0)
        setValueClamped(value, true)
    }

    fun setValueClamped(value: Double, notify: Boolean) {
        setValue(clamp(value), -1, notify)
    }

    private fun clamp(value: Double): Double {
        val clampFunc = type.clampFunc ?: return value
        val input: Any = when (type.defaultValue) {
            is Boolean -> value >= 0.5
            is Int -> value.roundToIntOr()
            is Long -> value.roundToLongOr()
            is Vector -> value.toFloat()
            else -> AnyToDouble.getDouble(value)
        }
        val clamped = clampFunc(input)
        return AnyToDouble.getDouble(clamped)
    }

    // must be open for Rem's Studio
    open fun getValue(value: Any): Double {
        return AnyToDouble.getDouble(value, 0, 0.0)
    }

    fun setChangeListener(listener: (value: Double) -> Unit): FloatInput {
        changeListener = listener
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(type.defaultValue)
        if (newValue != value) {
            setValue(newValue, -1, true)
        }
    }

    override fun setValue(newValue: Double, mask: Int, notify: Boolean): FloatInput {
        if (newValue != value || !hasValue) {
            hasValue = true
            value = newValue
            setText(stringify(newValue), notify)
            if (notify) changeListener(newValue)
        }
        return this
    }

    fun updateValueMaybe() {
        if (inputPanel.isInFocus) {
            wasInFocus = true
        } else if (wasInFocus) {
            // apply the value, or reset if invalid
            setValueClamped(parseValue(inputPanel.value), true)
            wasInFocus = false
        }
    }

    override fun onEnterKey(x: Float, y: Float) {
        // evaluate the value, and write it back into the text field, e.g., for calculations
        hasValue = false
        setValueClamped(value, true)
    }

    override fun clone(): FloatInput {
        val clone = FloatInput(title, visibilityKey, type, style, inputPanel.clone())
        copyInto(clone)
        return clone
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyIntoExceptChildren(dst)
        if (dst !is FloatInput) return
        // only works without hard references
        dst.changeListener = changeListener
        dst.allowInfinity = allowInfinity
        dst.tooltip = tooltip
        dst.setValue(value, -1, false)
    }
}