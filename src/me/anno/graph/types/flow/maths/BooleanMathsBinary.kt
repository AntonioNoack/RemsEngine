package me.anno.graph.types.flow.maths

import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.toInt

enum class BooleanMathsBinary(
    val id: Int, val glsl: String,
    val compute: Int
) { // todo dynamic number of inputs?
    AND(0, "a && b", 0b1000),
    OR(1, "a || b", 0b1110),
    XOR(2, "a != b", 0b0110),
    XNOR(3, "a == b", 0b1001),
    NAND(4, "!(a && b)", 0b0111),
    NOR(5, "!(a || b)", 0b0001);

    fun compute(a: Boolean, b: Boolean): Boolean {
        val index = a.toInt(2) + b.toInt()
        return compute.hasFlag(1 shl index)
    }
}