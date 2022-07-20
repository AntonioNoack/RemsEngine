package me.anno.tests

import me.anno.utils.search.BinarySearch.binarySearch

fun main() {

    val list = arrayListOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)
    for (v in listOf(-10f, 0.5f, 4f, 7f, 11f)) {
        val insertIndex = binarySearch(list.size) { list[it].compareTo(v) }
        if (insertIndex >= 0) {
            println("$v was found")
        } else {
            list.add(-1 - insertIndex, v)
        }
    }

    println("isSorted? ${list.sorted() == list}")
    println(list)

}