package me.anno.tests.structures

import me.anno.utils.structures.arrays.ExpandingIntArray

fun main() {
    val list = ExpandingIntArray(16)
    list.add(1)
    list.add(2)
    list.add(3)
    println(list) // 1,2,3
    list.removeAt(1)
    println(list) // 1,3
    list.add(0, 5)
    println(list) // 5,1,3
    list.removeBetween(0, 1)
    println(list) // 1,3
}