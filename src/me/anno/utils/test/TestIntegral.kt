package me.anno.utils.test

import me.anno.objects.animation.AnimatedProperty

fun main(){

    val line = AnimatedProperty.float()
    line.isAnimated = true
    line.addKeyframe(0.0, 1f)
    line.addKeyframe(1.0, 2f)

    for(i in 0 until 11){
        val time = i/10.0
        println(line.getIntegral<Float>(time, false))
    }

    val ap = AnimatedProperty.float()
    ap.isAnimated = true
    ap.addKeyframe(0.0, 0.0)
    ap.addKeyframe(10.0, 10.0)

    for(i in 1 .. 50){
        val target = i.toDouble()
        val time = ap.findIntegralX<Float>(0.0, 10.0, target)
        println("$target: $time")
    }

}