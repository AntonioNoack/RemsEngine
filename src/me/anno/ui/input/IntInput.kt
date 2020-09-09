package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.Input.isShiftDown
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser
import me.anno.studio.Studio.shiftSlowdown
import me.anno.ui.style.Style
import me.anno.utils.pow
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToLong

open class IntInput(
    style: Style, title: String,
    type: AnimatedProperty.Type = AnimatedProperty.Type.FLOAT,
    owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int
): NumberInput<Long>(style, title, type, owningProperty, indexInProperty) {

    constructor(title: String, owningProperty: AnimatedProperty<*>, indexInProperty: Int, time: Double, style: Style): this(style, title, owningProperty.type, owningProperty, indexInProperty){
        when(val value = owningProperty[time]){
            is Int -> setValue(value, false)
            is Long -> setValue(value, false)
            else -> throw RuntimeException("Unknown type $value for ${getClassName()}")
        }
    }

    constructor(title: String, value0: Int, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0, false)
    }

    constructor(title: String, value0: Long, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0, false)
    }

    constructor(title: String, value0: Int, style: Style): this(style, title, AnimatedProperty.Type.FLOAT, null, 0){
        setValue(value0, false)
    }

    constructor(title: String, value0: Long, style: Style): this(style, title, AnimatedProperty.Type.DOUBLE, null, 0){
        setValue(value0, false)
    }

    override fun parseValue(text: String): Long? {
        try {
            val trimmed = text.trim()
            return if(trimmed.isEmpty()) 0L
            else trimmed.toLongOrNull() ?:
            SimpleExpressionParser.parseDouble(trimmed)?.roundToLong()
        } catch (e: Exception){ }
        return 0L
    }

    fun setValue(v: Int, notify: Boolean) = setValue(v.toLong(), notify)

    override fun stringify(v: Long): String = v.toString()

    var savedDelta = 0f
    override fun changeValue(dx: Float, dy: Float) {
        val scale = 20f
        val size = scale * shiftSlowdown / max(GFX.width,GFX.height)
        val dx0 = dx*size
        val dy0 = dy*size
        val delta = dx0-dy0
        // chose between exponential and linear curve, depending on the use-case
        savedDelta += delta * 0.5f * type.unitScale
        val actualDelta = round(savedDelta)
        savedDelta -= actualDelta
        var value = lastValue
        if(type.hasLinear) value += actualDelta.toLong()
        if(type.hasExponential) value = (value * pow(if(lastValue < 0) 1f / 1.03f else 1.03f, delta * if(type.hasLinear) 1f else 3f)).roundToLong()
        when(val clamped = type.clamp(if(type.defaultValue is Int) value.toInt() else value)){
            is Int -> setValue(clamped, true)
            is Long -> setValue(clamped, true)
            else -> throw RuntimeException("Unknown type $clamped for ${getClassName()}")
        }
    }

    fun setValueClamped(value: Long, notify: Boolean){
        when(val clamped = type.clamp(
            when(type.defaultValue){
                is Float -> value.toFloat()
                is Double -> value.toDouble()
                is Int -> value.toInt()
                is Long -> value
                else -> throw RuntimeException("Unknown type ${type.defaultValue}")
            }
        )){
            is Float -> setValue(clamped.roundToLong(), notify)
            is Double -> setValue(clamped.roundToLong(), notify)
            is Int -> setValue(clamped, notify)
            is Long -> setValue(clamped, notify)
            else -> throw RuntimeException("Unknown type $clamped for ${getClassName()}")
        }
    }

    override fun getValue(value: Any): Long {
        return when(value){
            is Int -> value.toLong()
            is Long -> value
            else -> throw RuntimeException("Unknown type $value for ${getClassName()}")
        }
    }

    override fun onCharTyped(x: Float, y: Float, key: Int) {
        when(key){
            '+'.toInt() -> setValueClamped(lastValue+1, true)
            '-'.toInt() -> setValueClamped(lastValue-1, true)
            else -> super.onCharTyped(x, y, key)
        }
    }

    override fun getClassName() = "FloatInput"

}