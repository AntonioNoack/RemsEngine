package me.anno.graph.visual.scalar

import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

enum class BooleanMathsTernary(
    val id: Int, val glsl: String,
    val compute: Int
) {
    AND(0, "a && b && c", 0b1000_0000),
    OR(1, "a || b || c", 0b1111_1110),
    XOR(2, "a ^^ b ^^ c", 0b1001_0110),
    XNOR(3, "!(a ^^ b ^^ c)", 0b0110_1001),
    NAND(4, "!(a && b && c)", 0b0111_1111),
    NOR(5, "!(a || b || c)", 0b0000_0001);

    fun compute(a: Boolean, b: Boolean, c: Boolean): Boolean {
        val index = a.toInt(4) + b.toInt(2) + c.toInt()
        return compute.hasFlag(1 shl index)
    }
}