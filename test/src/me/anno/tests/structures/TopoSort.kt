package me.anno.tests.structures

import me.anno.utils.structures.lists.Lists.sortedByTopology

fun main() {
    fun okTest() {
        val elements = listOf(0, 1, 2, 3, 4).shuffled()
        val dependencies = hashMapOf(// full sort
            1 to listOf(0),
            2 to listOf(1),
            3 to listOf(2),
            4 to listOf(3)
        )
        println(elements.sortedByTopology { dependencies[it] })
    }

    fun cyclicTest() {
        val elements = listOf(0, 1, 2, 3, 4).shuffled()
        val dependencies = hashMapOf(// full sort
            0 to listOf(4),
            1 to listOf(0),
            2 to listOf(1),
            3 to listOf(2),
            4 to listOf(3)
        )
        try {
            println(elements.sortedByTopology { dependencies[it] })
            println("incorrect!, didn't find cycle!")
        } catch (e: IllegalArgumentException) {
            println("correctly found error:\n  ${e.message}")
        }
    }
    okTest()
    cyclicTest()
}