package me.anno.utils.search

object BinarySearch {

    inline fun binarySearch(length: Int, compare: (Int) -> Int): Int {

        var min = 0
        var max = length - 1

        while (max >= min) {
            val mid = (min + max).ushr(1)
            val cmp = compare(mid)
            if (cmp == 0) return mid
            if (cmp < 0) {
                // right
                min = mid + 1
            } else {
                // left
                max = mid - 1
            }
        }
        return -1 - min
    }

}
/*
fun main() {

    val list = arrayListOf(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f)

    val toAdd = listOf(-10f, 0.5f, 4f, 7f, 11f)
    for (v in toAdd) {
        val insertIndex = binarySearch(list.size) { list[it].compareTo(v) }
        if (insertIndex >= 0) {
            println("$v was found")
        } else {
            list.add(-1 - insertIndex, v)
        }
    }

    println("isSorted? ${list.sorted() == list}")
    println(list)

}*/