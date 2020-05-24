package me.anno.maths

fun clamp(x: Int, min: Int, max: Int) = if(x < min) min else if(x < max) x else max
fun clamp(x: Float, min: Float, max: Float) = if(x < min) min else if(x < max) x else max
fun pow(base: Float, power: Float) = StrictMath.pow(base.toDouble(), power.toDouble()).toFloat()