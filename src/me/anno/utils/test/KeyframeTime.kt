package me.anno.utils.test

import me.anno.io.text.TextReader
import me.anno.objects.animation.AnimatedProperty
import me.anno.objects.animation.Interpolation

fun main(){

    val prop = AnimatedProperty.float()
    prop.isAnimated = true
    prop.addKeyframe(1.0, 1f)
    prop.addKeyframe(2.0, 2f)

    prop.keyframes.forEach { it.interpolation = Interpolation.EASE_IN }

    val asString = prop.toString()
    val fromString = TextReader.fromText(asString).filterIsInstance<AnimatedProperty<*>>().first()

    println(asString)
    println(fromString)

}