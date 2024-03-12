package me.anno.utils.search

object BinarySearch {

    fun interface IndexComparator {
        fun compare(index: Int): Int
    }

    /**
     * performs a binary search;
     * @length number of values within the collection
     * @param comparator compare element at this index with the searched element; return 0 if found
     * @return index of element or -1-insertIndex if not found
     * */
    @JvmStatic
    fun binarySearch(length: Int, comparator: IndexComparator): Int =
        binarySearch(0, length - 1, comparator)

    /**
     * performs a binary search;
     * @param comparator compare element at this index with the searched element; return 0 if found
     * @return index of element or -1-insertIndex if not found
     * */
    @JvmStatic
    fun binarySearch(minIndex: Int, maxIndex: Int, comparator: IndexComparator): Int {

        var min = minIndex
        var max = maxIndex

        while (max >= min) {
            val mid = (min + max).ushr(1)
            val cmp = comparator.compare(mid)
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