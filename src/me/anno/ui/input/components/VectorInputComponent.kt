package me.anno.ui.input.components

import me.anno.animation.AnimatedProperty
import me.anno.animation.Type
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.FloatVectorInput
import me.anno.ui.input.IntVectorInput
import me.anno.ui.style.Style

fun VectorInputComponent(
    title: String, visibilityKey: String,
    type: Type, owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int,
    owner: FloatVectorInput,
    style: Style
): FloatInput {
    val base = FloatInput(style, title, visibilityKey, type, owningProperty, indexInProperty)
    base.setChangeListener { owner.onChange() }
    return base
}

fun VectorInputIntComponent(
    title: String, visibilityKey: String,
    type: Type, owningProperty: AnimatedProperty<*>?,
    indexInProperty: Int,
    owner: IntVectorInput,
    style: Style
): IntInput {
    val base = IntInput(style, title, visibilityKey, type, owningProperty, indexInProperty)
    base.setChangeListener { owner.onChange() }
    return base
}