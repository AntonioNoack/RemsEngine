package me.anno.utils.structures.sets

/**
 * collects items on multiple threads,
 * and in-parallel all collected items can be processed securely (by a single thread! using two worker threads will lead to this class blocking),
 * and then get discarded (because they're "done" now)
 * */
fun <V> ParallelHashSet(initialCapacity: Int = 16): ParallelCollection<V, HashSet<V>> =
    ParallelCollection(HashSet(initialCapacity), HashSet(initialCapacity))

fun <V> ParallelArrayList(initialCapacity: Int = 16): ParallelCollection<V, ArrayList<V>> =
    ParallelCollection(ArrayList(initialCapacity), ArrayList(initialCapacity))