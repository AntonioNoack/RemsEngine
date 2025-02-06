package org.recast4j

class IntPair(val first: Int, val second: Int) {
    operator fun component1() = first
    operator fun component2() = second
}