package me.anno.utils.structures.sets

/**
 * collects items on multiple threads,
 * and in-parallel all collected items can be processed securely (by a single thread! using two worker threads will lead to this class blocking),
 * and then get discarded (because they're "done" now)
 * */
class ParallelCollection<V, C : MutableCollection<V>>(
    private var inputList: C,
    private var outputList: C
) {

    val size get() = inputList.size

    fun add(element: V) {
        val addable = inputList
        synchronized(addable) {
            addable.add(element)
        }
    }

    fun addAll(elements: Collection<V>) {
        val addable = inputList
        synchronized(addable) {
            addable.addAll(elements)
        }
    }

    fun clear() {
        synchronized(this) {
            synchronized(inputList) {
                inputList.clear()
            }
            synchronized(outputList) {
                outputList.clear()
            }
        }
    }

    private fun swapGetRemovable(): C {
        return synchronized(this) {
            val tmp = inputList
            inputList = outputList
            outputList = tmp
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