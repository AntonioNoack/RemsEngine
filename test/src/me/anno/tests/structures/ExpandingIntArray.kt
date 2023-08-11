package me.anno.tests.structures

import me.anno.utils.structures.arrays.ExpandingIntArray
import org.junit.jupiter.api.Assertions

fun main() {
    val list = ExpandingIntArray(16)
    list.add(1)
    list.add(2)
    list.add(3)
    Assertions.assertEquals(list.toString(), "[1,2,3]")
    list.removeAt(1)
    Assertions.assertEquals(list.toString(), "[1,3]")
    list.add(0, 5)
    Assertions.assertEquals(list.toString(), "[5,1,3]")
    list.removeBetween(0, 1)
    Assertions.assertEquals(list.toString(), "[1,3]")
}