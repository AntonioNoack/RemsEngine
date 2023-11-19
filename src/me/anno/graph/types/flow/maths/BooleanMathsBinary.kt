package me.anno.graph.types.flow.maths

enum class BooleanMathsBinary(
    val id: Int, val glsl: String,
    val compute: (a: Boolean, b: Boolean) -> Boolean
) { // todo dynamic number of inputs?
    AND(0, "a && b", { a, b -> a && b }),
    OR(1, "a || b", { a, b -> a || b }),
    XOR(2, "a != b", { a, b -> a != b }),
    XNOR(3, "a == b", { a, b -> a == b }),
    NAND(4, "!(a && b)", { a, b -> !(a && b) }),
    NOR(5, "!(a || b)", { a, b -> !(a || b) });
}