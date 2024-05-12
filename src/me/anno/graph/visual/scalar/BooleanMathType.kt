package me.anno.graph.visual.scalar

import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

enum class BooleanMathType(
    val id: Int,
    val glsl2d: String, val compute2d: Int,
    val glsl3d: String, val compute3d: Int,
) { // todo dynamic number of inputs?
    AND(0, "a && b", 0b1000, "a && b && c", 0b1000_0000),
    OR(1, "a || b", 0b1110, "a || b || c", 0b1111_1110),
    XOR(2, "a != b", 0b0110, "a ^^ b ^^ c", 0b1001_0110),
    XNOR(3, "a == b", 0b1001, "!(a ^^ b ^^ c)", 0b0110_1001),
    NAND(4, "!(a && b)", 0b0111, "!(a && b && c)", 0b0111_1111),
    NOR(5, "!(a || b)", 0b0001, "!(a || b || c)", 0b0000_0001);

    fun compute2d(a: Boolean, b: Boolean): Boolean {
        val index = a.toInt(2) + b.toInt()
        return compute2d.hasFlag(1 shl index)
    }

    fun compute3d(a: Boolean, b: Boolean, c: Boolean): Boolean {
        val index = a.toInt(4) + b.toInt(2) + c.toInt()
        return compute3d.hasFlag(1 shl index)
    }
}