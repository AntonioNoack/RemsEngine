package me.anno.ecs.components.mesh.unique

interface UMRIterator<Mesh> {
    fun push(start: Int, endExcl: Int)
    fun getRange(entry: Mesh): IntRange
    fun filter(entry: Mesh): Boolean

    /**
     * efficiently iterates over all ranges, only calling the minimum amount of times;
     * sortedByRanges must be sorted as the name suggests, or the result won't be optimal
     * */
    fun iterateRanges(sortedByRanges: List<Mesh>): Long {
        var currStart = 0
        var currEnd = 0
        var counter = 0L
        for (i in sortedByRanges.indices) {
            val entry = sortedByRanges[i]
            if (filter(entry)) {
                val range = getRange(entry)
                if (range.first != currEnd) {
                    if (currEnd > currStart) {
                        push(currStart, currEnd)
                        counter += (currEnd - currStart)
                    }
                    currStart = range.first
                }
                currEnd = range.last + 1
            }
        }
        if (currEnd > currStart) {
            push(currStart, currEnd)
            counter += (currEnd - currStart)
        }
        return counter
    }
}