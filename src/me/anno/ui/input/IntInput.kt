package me.anno.ui.input

import me.anno.gpu.GFX
import me.anno.input.Input.isShiftDown
import me.anno.objects.animation.AnimatedProperty
import me.anno.parser.SimpleExpressionParser
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
            is Int -> setValue(value)
            is Long -> setValue(value)
            else -> throw RuntimeException("Unknown type $value for ${getClassName()}")
        }
    }

    constructor(title: String, value0: Int, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Long, type: AnimatedProperty.Type, style: Style): this(style, title, type, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Int, style: Style): this(style, title, AnimatedProperty.Type.FLOAT, null, 0){
        setValue(value0)
    }

    constructor(title: String, value0: Long, style: Style): this(style, title, AnimatedProperty.Type.DOUBLE, null, 0){
        setValue(value0)
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

    fun setValue(v: Int) = setValue(v.toLong())

    override fun stringify(v: Long): String = v.toString()

    var savedDelta = 0f
    override fun changeValue(dx: Float, dy: Float) {
        val scale = 1f
        val size = scale * (if(isShiftDown) 4f else 20f) / max(GFX.width,GFX.height)
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
            is Int -> setValue(clamped)
            is Long -> setValue(clamped)
            else -> throw RuntimeException("Unknown type $clamped for ${getClassName()}")
        }
    }

    fun setValueClamped(value: Long){
        when(val clamped = type.clamp(
            when(type.defaultValue){
                is Float -> value.toFloat()
                is Double -> value.toDouble()
                is Int -> value.toInt()
                is Long -> value
                else -> throw RuntimeException("Unknown type ${type.defaultValue}")
            }
        )){
            is Float -> setValue(clamped.roundToLong())
            is Double -> setValue(clamped.roundToLong())
            is Int -> setValue(clamped)
            is Long -> setValue(clamped)
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
            '+'.toInt() -> setValueClamped(lastValue+1)
            '-'.toInt() -> setValueClamped(lastValue-1)
            else -> super.onCharTyped(x, y, key)
        }
    }

    override fun getClassName() = "FloatInput"

}