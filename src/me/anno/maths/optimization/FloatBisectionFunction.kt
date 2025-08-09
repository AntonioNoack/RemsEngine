package me.anno.maths.optimization

fun interface FloatBisectionFunction {
    /**
     * returns whether x shall be bigger
     * */
    fun calc(x: Float): Boolean
}