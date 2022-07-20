package me.anno.utils.search

object BinarySearch {

    /**
     * performs a binary search;
     * @length number of values within the collection
     * @param compare compare element at this index with the searched element; return 0 if found
     * @return index of element or -1-insertIndex if not found
     * */
    inline fun binarySearch(length: Int, compare: (index: Int) -> Int): Int {

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