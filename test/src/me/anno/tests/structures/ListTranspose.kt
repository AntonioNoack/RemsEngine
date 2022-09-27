package me.anno.tests.structures

import me.anno.utils.structures.lists.Lists.transpose

fun main() {
    val l0 = arrayListOf(arrayListOf(1, 2, 3), arrayListOf(9, 8, 7))
    println(l0)
    println(l0.transpose())
    println(l0.transpose())
}