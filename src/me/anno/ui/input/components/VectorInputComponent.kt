package me.anno.ui.input.components

import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Type
import me.anno.ui.input.FloatInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.Floats.anyToFloat

class VectorInputComponent(
    title: String, type: Type, owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int,
    owner: VectorInput,
    style: Style
) :
    FloatInput(style, title, type, owningProperty, indexInProperty) {

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