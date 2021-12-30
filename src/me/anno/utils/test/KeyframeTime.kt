package me.anno.utils.test

import me.anno.io.text.TextReader
import me.anno.animation.AnimatedProperty
import me.anno.animation.Interpolation
import me.anno.utils.LOGGER

fun main(){

    val prop = AnimatedProperty.float()
    prop.isAnimated = true
    prop.addKeyframe(1.0, 1f)
    prop.addKeyframe(2.0, 2f)

    prop.keyframes.forEach { it.interpolation = Interpolation.EASE_IN }

    val asString = prop.toString()
    val fromString = TextReader.read(asString, false)
        .filterIsInstance<AnimatedProperty<*>>().first()

    LOGGER.info(asString)
    LOGGER.info(fromString)

}