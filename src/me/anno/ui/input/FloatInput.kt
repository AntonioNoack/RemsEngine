package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.Input.isShiftDown
import me.anno.utils.pow
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser
import me.anno.studio.Studio.shiftSlowdown
import me.anno.ui.style.Style
import me.anno.utils.get
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.RuntimeException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

open class FloatInput(
    style: Style, title: String,
    type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int
): NumberInput<Double>(style, title, type, owningProperty, indexInProperty) {

    constructor(title: String, owningProperty: AnimatedProperty<*>, indexInProperty: Int, time: Double, style: Style): this(style, title, owningProperty.type, owningProperty, indexInProperty){
        when(val value = owningProperty[time]){
            is Float -> setValue(value, false)
            is Double -> setValue(value, false)
            else -> throw RuntimeException("Unknown type $value for ${javaClass.simpleName}")
        }
    }

    constructor(title: String, value0: Float, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0, false)
    }

    constructor(title: String, value0: Double, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0, false)
    }

    var allowInfinity = false

    override fun parseValue(text: String): Double? {
        val trimmed = text.trim()
        val newValue = if(trimmed.isEmpty()) 0.0 else trimmed.toDoubleOrNull() ?: SimpleExpressionParser.parseDouble(trimmed)
        if(newValue == null || !((allowInfinity && !newValue.isNaN()) || newValue.isFinite())) return null
        return newValue
    }

    fun setValue(v: Int, notify: Boolean) = setValue(v.toDouble(), notify)
    fun setValue(v: Long, notify: Boolean) = setValue(v.toDouble(), notify)
    fun setValue(v: Float, notify: Boolean) = setValue(v.toDouble(), notify)

    override fun stringify(v: Double): String =
        if(type.defaultValue is Double) v.toString()
        else v.toFloat().toString()

    override fun changeValue(dx: Float, dy: Float) {
        val scale = 20f * shiftSlowdown
        val size = scale / max(GFX.width,GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
        // chose between exponential and linear curve, depending on the use-case
        var value = lastValue
        if(type.hasLinear) value += delta * 0.1f * type.unitScale
        if(type.hasExponential) value *= pow(if(lastValue < 0) 1f / 1.03f else 1.03f, delta * if(type.hasLinear) 1f else 3f)
        setValueClamped(value, true)
    }

    fun setValueClamped(value: Double, notify: Boolean){
        if(type.minValue == null && type.maxValue == null){
            setValue(value, notify)
        } else
        when(val clamped = type.clamp(
            when(type.defaultValue){
                is Float -> value.toFloat()
                is Double -> value
                is Int -> value.roundToInt()
                is Long -> value.roundToLong()
                is Vector2f -> value[indexInProperty]
                is Vector3f -> value[indexInProperty]
                is Vector4f -> value[indexInProperty]
                is Quaternionf -> value[indexInProperty]
                else -> throw RuntimeException("Unknown type ${type.defaultValue}")
            }
        )){
            is Float -> setValue(clamped, notify)
            is Double -> setValue(clamped, notify)
            is Int -> setValue(clamped, notify)
            is Long -> setValue(clamped, notify)
            else -> throw RuntimeException("Unknown type $clamped for ${getClassName()}")
        }
    }

    override fun getValue(value: Any): Double {
        return when(value){
            is Float -> value.toDouble()
            is Double -> value
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Vector2f, is Vector3f, is Vector4f,
            is Quaternionf -> value[indexInProperty].toDouble()
            else -> throw RuntimeException("Unknown type $value for ${getClassName()}")
        }
    }

    override fun getClassName() = "FloatInput"

}