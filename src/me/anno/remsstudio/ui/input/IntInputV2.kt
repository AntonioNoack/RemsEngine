package me.anno.remsstudio.ui.input

import me.anno.animation.Type
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.remsstudio.ui.input.components.NumberInputComponentV2
import me.anno.ui.input.IntInput
import me.anno.ui.style.Style
import org.joml.Vector2i
import org.joml.Vector3i
import org.joml.Vector4i

class IntInputV2(
    style: Style, title: String, visibilityKey: String,
    type: Type, private val owningProperty: AnimatedProperty<*>,
) : IntInput(style, title, visibilityKey, type, NumberInputComponentV2(owningProperty, visibilityKey, style)) {

    constructor(
        title: String, visibilityKey: String,
        owningProperty: AnimatedProperty<*>, time: Double, style: Style
    ) : this(style, title, visibilityKey, owningProperty.type, owningProperty) {
        when (val value = owningProperty[time]) {
            is Int -> setValue(value, false)
            is Long -> setValue(value, false)
            else -> throw RuntimeException("Unknown type $value for ${javaClass.simpleName}")
        }
    }

    private val indexInProperty get() = indexInParent

    override fun getValue(value: Any): Long {
        return when (value) {
            is Vector2i -> value[indexInProperty].toLong()
            is Vector3i -> value[indexInProperty].toLong()
            is Vector4i -> value[indexInProperty].toLong()
            else -> super.getValue(value)
        }
    }

    override fun onEmpty(x: Float, y: Float) {
        val newValue = getValue(owningProperty.defaultValue ?: type.defaultValue)
        if (newValue != lastValue) {
            setValue(newValue, true)
        }
    }

    override fun clone(): IntInputV2 {
        val clone = IntInputV2(style, title, visibilityKey, type, owningProperty)
        copy(clone)
        return clone
    }

}