package org.recast4j

class FloatPair(val first: Float, val second: Float) {
    operator fun component1() = first
    operator fun component2() = second
}