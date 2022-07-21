package me.anno.tests

import me.anno.maths.Maths.factorial
import me.anno.maths.Permutations

fun main() {
    val all = HashSet<List<Int>>()
    var ctr = 0
    val base = listOf(1, 2, 3, 4, 5)
    Permutations.generatePermutations(base) {
        ctr++
        println(it)
        if (!all.add(it.toList()))
            throw RuntimeException("Double entry $it")
    }
    if (ctr != base.size.factorial()) {
        throw RuntimeException("Incorrect number of permutations")
    }
}