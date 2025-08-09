package me.anno.maths.optimization

fun interface DoubleTargetFunction {
    fun eval(params: DoubleArray): Double
}