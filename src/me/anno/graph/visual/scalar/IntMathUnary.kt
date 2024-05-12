package me.anno.graph.visual.scalar

import kotlin.math.abs
import kotlin.math.sqrt

enum class IntMathUnary(
    val id: Int,
    val glsl: String
) {

    ABS(0, "abs(a)"),
    NEG(1, "-a"),
    NOT(2, "~a"), // = -x-1
    SQRT(3, "sqrt(a)"),

    ;

    fun long(a: Long): Long {
        return when (this) {
            ABS -> abs(a)
            NEG -> -a
            NOT -> a.inv()
            SQRT -> sqrt(a.toDouble()).toLong()
        }
    }

    fun int(a: Int): Int {
        return long(a.toLong()).toInt()
    }
}