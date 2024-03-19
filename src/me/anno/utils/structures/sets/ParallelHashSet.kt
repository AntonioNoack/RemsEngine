package me.anno.utils.structures.sets

/**
 * collects items on multiple threads,
 * and in-parallel all collected items can be processed securely (by a single thread! using two worker threads will lead to this class blocking),
 * and then get discarded (because they're "done" now)
 * */
class ParallelHashSet<V>(initialCapacity: Int = 16) {

    private var addable = HashSet<V>(initialCapacity)
    private var removable = HashSet<V>(initialCapacity)

    val size get() = addable.size

    fun add(element: V) {
        val addable = addable
        synchronized(addable) {
            addable.add(element)
        }
    }

    private fun swapGetRemovable(): HashSet<V> {
        return synchronized(this) {
            val tmp = addable
            addable = removable
            removable = tmp
            tmp
        }
    }

    fun process2x(first: (V) -> Unit, second: (V) -> Unit) {
        val removable = swapGetRemovable()
        synchronized(removable) {
            for (entry in removable) {
                first(entry)
            }
            for (entry in removable) {
                second(entry)
            }
            removable.clear()
        }
    }

    // inline?
    fun process(callback: (V) -> Unit) {
        val removable = swapGetRemovable()
        synchronized(removable) {
            for (entry in removable) {
                callback(entry)
            }
            removable.clear()
        }
    }
}