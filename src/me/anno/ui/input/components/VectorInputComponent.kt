package me.anno.ui.input.components

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.ui.input.FloatInput
import me.anno.ui.input.VectorInput
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.anyToDouble

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
                owner.compX.lastValue.anyToDouble(),
                owner.compY.lastValue.anyToDouble(),
                owner.compZ?.lastValue?.anyToDouble() ?: 0.0,
                owner.compW?.lastValue?.anyToDouble() ?: 0.0
            )
        }
    }

}