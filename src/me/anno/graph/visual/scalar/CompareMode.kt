package me.anno.graph.visual.scalar

enum class CompareMode(val id: Int, val glsl: String) {
    LESS_THAN(0, "<"),
    LESS_OR_EQUALS(1, "<="),
    EQUALS(2, "=="),
    GREATER_THAN(3, ">"),
    GREATER_OR_EQUALS(4, ">="),
    NOT_EQUALS(5, "!="),
    IDENTICAL(6, "=="),
    NOT_IDENTICAL(7, "!=")
}