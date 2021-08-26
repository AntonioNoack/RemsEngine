package me.anno.ui.input

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.gpu.GFX
import me.anno.parser.SimpleExpressionParser
import me.anno.studio.StudioBase.Companion.shiftSlowdown
import me.anno.ui.style.Style
import me.anno.utils.maths.Maths.pow
import org.joml.Vector2i
import org.joml.Vector3i
import org.joml.Vector4i
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToLong

open class IntInput(
    style: Style,
    title: String,
    visibilityKey: String,
    type: Type = Type.FLOAT,
    owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int
) : NumberInput(style, title, visibilityKey, type, owningProperty, indexInProperty) {

    var lastValue: Long = getValue(type.defaultValue)
    var changeListener: (value: Long) -> Unit = { }

    init {
        inputPanel.setChangeListener {
            val newValue = parseValue(it)
            if (newValue != null) {
                lastValue = newValue
                changeListener(newValue)
            }
        }
    }

    constructor(
        title: String, visibilityKey: String,
        owningProperty: AnimatedProperty<*>, indexInProperty: Int, time: Double, style: Style
    ) : this(style, title, visibilityKey, owningProperty.type, owningProperty, indexInProperty) {
        when (val value = owningProperty[time]) {
            is Int -> setValue(value, false)
            is Long -> setValue(value, false)
            else -> throw RuntimeException("Unknown type $value for ${javaClass.simpleName}")
        }
    }

    constructor(title: String, visibilityKey: String, value0: Int, type: Type, style: Style) :
            this(style, title, visibilityKey, type, null, 0) {
        setValue(value0, false)
    }

    constructor(title: String, visibilityKey: String, value0: Long, type: Type, style: Style) :
            this(style, title, visibilityKey, type, null, 0) {
        setValue(value0, false)
    }

    constructor(title: String, visibilityKey: String, value0: Int, style: Style) :
            this(style, title, visibilityKey, Type.FLOAT, null, 0) {
        setValue(value0, false)
    }

    constructor(title: String, visibilityKey: String, value0: Long, style: Style) :
            this(style, title, visibilityKey, Type.DOUBLE, null, 0) {
        setValue(value0, false)
    }

    fun parseValue(text: String): Long? {
        try {
            val trimmed = text.trim()
            return if (trimmed.isEmpty()) 0L
            else trimmed.toLongOrNull() ?: SimpleExpressionParser.parseDouble(trimmed)?.roundToLong()
        } catch (e: Exception) {
        }
        return 0L
    }

    fun setValue(v: Int, notify: Boolean) = setValue(v.toLong(), notify)

    fun stringify(v: Long): String = v.toString()

    var savedDelta = 0f
    override fun changeValue(dx: Float, dy: Float) {
        val scale = 20f
        val size = scale * shiftSlowdown / max(GFX.width, GFX.height)
        val dx0 = dx * size
        val dy0 = dy * size
        val delta = dx0 - dy0
        // chose between exponential and linear curve, depending on the use-case
        savedDelta += delta * 0.5f * type.unitScale
        val actualDelta = round(savedDelta)
        savedDelta -= actualDelta
        var value = lastValue
        if (type.hasLinear) value += actualDelta.toLong()
        if (type.hasExponential) value = (value * pow(
            if (lastValue < 0) 1f / 1.03f else 1.03f,
            delta * if (type.hasLinear) 1f else 3f
        )).roundToLong()
        when (val clamped = type.clamp(if (type.defaultValue is Int) value.toInt() else value)) {
            is Byte -> setValue(clamped.toLong(), true)
            is Short -> setValue(clamped.toLong(), true)
            is Int -> setValue(clamped.toLong(), true)
            is Long -> setValue(clamped, true)
            else -> throw RuntimeException("Unknown type ${clamped::class} for ${javaClass.simpleName}")
        }
    }

    fun setValueClamped(value: Long, notify: Boolean) {
        when (val clamped = type.clamp(
            when (type.defaultValue) {
                is Float -> value.toFloat()
                is Double -> value.toDouble()
                is Byte -> value.toInt()
                is Short -> value.toInt()
                is Char -> value.toInt()
                is Int -> value.toInt()
                is Long -> value
                else -> throw RuntimeException("Unknown type ${type.defaultValue}")
            }
        )) {
            is Float -> setValue(clamped.roundToLong(), notify)
            is Double -> setValue(clamped.roundToLong(), notify)
            is Int -> setValue(clamped.toLong(), notify)
            is Long -> setValue(clamped, notify)
            else -> throw RuntimeException("Unknown type $clamped for ${javaClass.simpleName}")
        }
    }

    fun getValue(value: Any): Long {
        return when (value) {
            is Boolean -> if (value) 1L else 0L
            is Byte -> value.toLong()
            is Short -> value.toLong()
            is Char -> value.code.toLong()
            is Int -> value.toLong()
            is Long -> value
            is Float -> value.toLong()
            is Double -> value.toLong()
            is Vector2i -> value[indexInProperty].toLong()
            is Vector3i -> value[indexInProperty].toLong()
            is Vector4i -> value[indexInProperty].toLong()
            else -> throw RuntimeException("Unknown type $value for ${javaClass.simpleName}")
        }
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        when (key) {
            '+'.code -> setValueClamped(lastValue + 1, true)
            '-'.code -> setValueClamped(lastValue - 1, true)
            else -> super.onCharTyped(x, y, key)
        }
    }

    fun setChangeListener(listener: (value: Long) -> Unit): NumberInput {
        changeListener = listener
        return this
    }

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(owningProperty?.defaultValue ?: type.defaultValue)
        if (newValue != lastValue) {
            setValue(newValue, true)
        }
    }

    fun setValue(v: Long, notify: Boolean) {
        if (v != lastValue || !hasValue) {
            hasValue = true
            lastValue = v
            if (notify) changeListener(v)
            setText(stringify(v))
        }
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

    override fun onEnterKey(x: Float, y: Float) {
        // evaluate the value, and write it back into the text field, e.g. for calculations
        hasValue = false
        setValue(lastValue, true)
    }

}