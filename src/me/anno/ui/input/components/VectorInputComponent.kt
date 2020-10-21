package me.anno.ui.input.components

import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.ui.input.FloatInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.anyToFloat
import me.anno.utils.get

class VectorInputComponent(
    title: String, type: Type, owningProperty: AnimatedProperty<*>?, val i: Int,
    owner: VectorInput,
    style: Style
) :
    FloatInput(style, title, type, owningProperty, i) {
    override fun onEmpty(x: Float, y: Float) {
        val defaultValue = type.defaultValue
        this.setValue(defaultValue[i], true)
    }

    init {
        setChangeListener {
            owner.changeListener(
                owner.compX.lastValue.anyToFloat(),
                owner.compY.lastValue.anyToFloat(),
                owner.compZ?.lastValue?.anyToFloat() ?: 0f,
                owner.compW?.lastValue?.anyToFloat() ?: 0f
            )
        }
    }

}