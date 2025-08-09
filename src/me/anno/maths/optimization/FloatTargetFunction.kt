package me.anno.maths.optimization

fun interface FloatTargetFunction {
    fun eval(params: FloatArray): Float
}