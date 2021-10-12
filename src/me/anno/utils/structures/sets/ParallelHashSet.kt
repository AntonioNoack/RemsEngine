package me.anno.utils.structures.sets

class ParallelHashSet<V>(initialCapacity: Int = 16) {

    private var addable = HashSet<V>(initialCapacity)
    private var removable = HashSet<V>(initialCapacity)

    fun add(element: V) {
        val addable = addable
        synchronized(addable) {
            addable.add(element)
        }
    }

    // inline?
    fun process2x(callback: (V, first: Boolean) -> Unit) {
        val removable = synchronized(this) {
            val tmp = addable
            addable = removable
            removable = tmp
            tmp
        }
        synchronized(removable) {
            for (entry in removable) {
                callback(entry, true)
            }
            for (entry in removable) {
                callback(entry, false)
            }
            removable.clear()
        }
    }

    // inline?
    fun process(callback: (V) -> Unit) {
        val removable = synchronized(this) {
            val tmp = addable
            addable = removable
            removable = tmp
            tmp
        }
        synchronized(removable) {
            for (entry in removable) {
                callback(entry)
            }
            removable.clear()
        }
    }

}