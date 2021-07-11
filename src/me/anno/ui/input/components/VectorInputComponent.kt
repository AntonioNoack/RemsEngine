package me.anno.ui.input.components

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.VectorInput
import me.anno.ui.input.VectorIntInput
import me.anno.ui.style.Style
import me.anno.utils.types.Floats.anyToDouble

fun VectorInputComponent(
    title: String, type: Type, owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int,
    owner: VectorInput,
    style: Style
): FloatInput {
    val base = FloatInput(style, title, type, owningProperty, indexInProperty)
    base.setChangeListener {
        owner.changeListener(
            owner.compX.lastValue.anyToDouble(),
            owner.compY.lastValue.anyToDouble(),
            owner.compZ?.lastValue?.anyToDouble() ?: 0.0,
            owner.compW?.lastValue?.anyToDouble() ?: 0.0
        )
    }
    return base
}

fun VectorInputIntComponent(
    title: String, type: Type, owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int,
    owner: VectorIntInput,
    style: Style
): IntInput {
    val base = IntInput(style, title, type, owningProperty, indexInProperty)
    base.setChangeListener {
        owner.changeListener(
            owner.compX.lastValue.toInt(),
            owner.compY.lastValue.toInt(),
            owner.compZ?.lastValue?.toInt() ?: 0,
            owner.compW?.lastValue?.toInt() ?: 0
        )
    }
    return base
}