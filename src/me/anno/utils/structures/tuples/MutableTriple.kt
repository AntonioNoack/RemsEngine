package me.anno.utils.structures.tuples

class MutableTriple<A, B, C>(var first: A, var second: B, var third: C) {
    operator fun component1() = first
    operator fun component2() = second
    operator fun component3() = third
}