package me.anno.utils.structures

import me.anno.utils.hpc.threadLocal
import kotlin.math.max

/**
 * recursion can be expensive (because more is stacked than necessary),
 *      and some languages like Zig don't even support it ->
 * often, recursion only needs an arraylist as a remaining-items-stack ->
 * cache that stack on a threadLocal value to avoid recurring allocations
 * */
object Recursion {

    private val recursive = threadLocal { ArrayList<Any?>() }

    /**
     * processes all items in a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    fun <V> processRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> Unit) {
        findRecursive(initial) { item, remaining ->
            process(item, remaining)
            null
        }
    }

    fun <V> processRecursive2(initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> Unit) {
        findRecursive2(initial) { item, remaining ->
            process(item, remaining)
            null
        }
    }

    /**
     * processes each item in a graph only once; returns the set of all processed/reached items;
     * process() must add any to-be-processed children to the list
     * */
    fun <V> collectRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> Unit): HashSet<V> {
        val result = HashSet<V>()
        findRecursive(initial) { item, remaining ->
            if (result.add(item)) {
                process(item, remaining)
            }
            null
        }
        return result
    }

    fun <V> collectRecursive2(initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> Unit): HashSet<V> {
        val result = HashSet<V>()
        findRecursive2(initial) { item, remaining ->
            if (result.add(item)) {
                process(item, remaining)
            }
            null
        }
        return result
    }

    /**
     * finds whether an item fulfills a condition within a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    fun <V : Any> anyRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> Boolean): Boolean {
        return findRecursive(initial) { item, remaining ->
            if (process(item, remaining)) Unit else null
        } == Unit
    }

    fun <V : Any> anyRecursive(
        initial: Collection<V>,
        process: (item: V, remaining: ArrayList<V>) -> Boolean
    ): Boolean {
        return findRecursive2(initial) { item, remaining ->
            if (process(item, remaining)) Unit else null
        } == Unit
    }

    private fun <V> getContainer(): ArrayList<V> {
        @Suppress("UNCHECKED_CAST")
        return recursive.get() as ArrayList<V>
    }

    /**
     * finds first non-null value in a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    fun <V, W : Any> findRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> W?): W? {
        val remaining = getContainer<V>()
        val startIndex = remaining.size
        remaining.add(initial)
        return findRecursiveRun(remaining, startIndex, process)
    }

    fun <V, W : Any> findRecursive2(initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> W?): W? {
        val remaining = getContainer<V>()
        val startIndex = remaining.size
        remaining.addAll(initial)
        return findRecursiveRun(remaining, startIndex, process)
    }

    fun <V, W : Any> findRecursiveRun(
        remaining: ArrayList<V>, startIndex: Int,
        process: (item: V, remaining: ArrayList<V>) -> W?
    ): W? {
        var maxSize = startIndex
        var result: W? = null
        while (result == null && remaining.size > startIndex) {
            val entry = remaining.removeLast()
            result = process(entry, remaining)
            maxSize = max(maxSize, remaining.size)
        }
        if (maxSize > startIndex + 500) {
            remaining.trimToSize()
        }
        return result
    }
}