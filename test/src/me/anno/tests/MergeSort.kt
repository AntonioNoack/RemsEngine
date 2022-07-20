package me.anno.tests

import me.anno.utils.sorting.MergeSort

fun main() {
    val list = arrayOf(0, 1, 2, 3, 4, 5, 6)
    list.shuffle()
    println(list.joinToString())
    MergeSort.mergeSort(list, 1, list.size) { a, b -> a.compareTo(b) }
    println(list.joinToString())
}