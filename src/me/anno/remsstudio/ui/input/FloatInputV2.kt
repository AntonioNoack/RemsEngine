package me.anno.remsstudio.ui.input

import me.anno.animation.Type
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.ui.input.components.NumberInputComponentV2
import me.anno.ui.input.FloatInput
import me.anno.ui.style.Style
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import org.joml.*

class FloatInputV2(
    style: Style, title: String, visibilityKey: String,
    type: Type,
    private val owningProperty: AnimatedProperty<*>
) : FloatInput(
    style,
    title,
    visibilityKey,
    type,
    NumberInputComponentV2(owningProperty, visibilityKey, style)
) {

    private val indexInProperty get() = indexInParent

    constructor(
        title: String,
        visibilityKey: String,
        owningProperty: AnimatedProperty<*>,
        time: Double,
        style: Style
    ) : this(style, title, visibilityKey, owningProperty.type, owningProperty) {
        when (val value = owningProperty[time]) {
            is Float -> setValue(value, false)
            is Double -> setValue(value, false)
            else -> throw RuntimeException("Unknown type $value for ${javaClass.simpleName}")
        }
    }

    override fun getValue(value: Any): Double {
        return when (value) {
            is Vector2fc, is Vector3fc, is Vector4fc,
            is Quaternionf -> AnyToFloat.getFloat(value, indexInProperty).toDouble()
            is Vector2dc, is Vector3dc, is Vector4dc,
            is Quaterniond -> AnyToDouble.getDouble(value, indexInProperty)
            else -> super.getValue(value)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(owningProperty.defaultValue ?: type.defaultValue)
        if (newValue != lastValue) {
            setValue(newValue, true)
        }
    }

    override fun clone(): FloatInputV2 {
        val clone = FloatInputV2(style, title, visibilityKey, type, owningProperty)
        copy(clone)
        return clone
    }

}