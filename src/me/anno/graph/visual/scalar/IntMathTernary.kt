package me.anno.graph.visual.scalar

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.median

enum class IntMathTernary(
    val id: Int,
    val glsl: String
) {

    CLAMP(0, "clamp(a,b,c)"),
    MEDIAN(1, "max(min(a,b),min(max(a,b),c))"),
    ADD(10, "a+b+c"),
    MUL(12, "a*b*c"),
    MUL_ADD(13, "a*b+c"),

    ;

    fun long(a: Long, b: Long, c: Long): Long {
        return when (this) {
            CLAMP -> clamp(a, b, c)
            MEDIAN -> median(a, b, c)
            ADD -> a + b + c
            MUL -> a * b * c
            MUL_ADD -> a * b + c
        }
    }

    fun int(a: Int, b: Int, c: Int): Int {
        return long(a.toLong(), b.toLong(), c.toLong()).toInt()
    }
}