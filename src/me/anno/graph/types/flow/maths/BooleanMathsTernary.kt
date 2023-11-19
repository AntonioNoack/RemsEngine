package me.anno.graph.types.flow.maths

enum class BooleanMathsTernary(
    val id: Int, val glsl: String,
    val compute: (a: Boolean, b: Boolean, c: Boolean) -> Boolean
) {
    AND(0, "a && b && c", { a, b, c -> a && b && c }),
    OR(1, "a || b || c", { a, b, c -> a || b || c }),
    XOR(2, "a ^^ b ^^ c", { a, b, c -> (a != b) != c }),
    XNOR(3, "!(a ^^ b ^^ c)", { a, b, c -> (a != b) == c }),
    NAND(4, "!(a && b && c)", { a, b, c -> !(a && b && c) }),
    NOR(5, "!(a || b || c)", { a, b, c -> !(a || b || c) });
}