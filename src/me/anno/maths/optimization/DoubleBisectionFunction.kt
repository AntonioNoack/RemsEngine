package me.anno.maths.optimization

fun interface DoubleBisectionFunction {
    /**
     * returns whether x shall be bigger
     * */
    fun calc(x: Double): Boolean
}
