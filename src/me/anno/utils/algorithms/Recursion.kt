package me.anno.utils.algorithms

import me.anno.utils.assertions.assertEquals
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

    /**
     * processes all items in a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    @Suppress("unused")
    fun <V, W> processRecursivePairs(
        initialFirst: V, initialSecond: W,
        process: (first: V, second: W, remaining: ArrayList<Any?>) -> Unit
    ) {
        findRecursivePairs(initialFirst, initialSecond) { first, second, remaining ->
            process(first, second, remaining)
            null
        }
    }

    fun <V> processRecursiveSet(initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> Unit) {
        findRecursiveSet(initial) { item, remaining ->
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

    fun <V> collectRecursiveSet(
        initial: Collection<V>,
        process: (item: V, remaining: ArrayList<V>) -> Unit
    ): HashSet<V> {
        val result = HashSet<V>()
        findRecursiveSet(initial) { item, remaining ->
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
    fun <V> anyRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> Boolean): Boolean {
        return findRecursive(initial) { item, remaining ->
            if (process(item, remaining)) Unit else null
        } == Unit
    }

    /**
     * finds whether an item fulfills a condition within a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    fun <V, W> anyRecursivePairs(
        initialFirst: V, initialSecond: W,
        process: (first: V, second: W, remaining: ArrayList<Any?>) -> Boolean
    ): Boolean {
        return findRecursivePairs(initialFirst, initialSecond) { first, second, remaining ->
            if (process(first, second, remaining)) Unit else null
        } == Unit
    }

    @Suppress("unused")
    fun <V> anyRecursiveSet(
        initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> Boolean
    ): Boolean {
        return findRecursiveSet(initial) { item, remaining ->
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
    fun <V, R : Any> findRecursive(initial: V, process: (item: V, remaining: ArrayList<V>) -> R?): R? {
        val remaining = getContainer<V>()
        val startIndex = remaining.size
        remaining.add(initial)
        return findRecursiveRun(remaining, startIndex, process)
    }

    /**
     * finds first non-null value in a tree; must not have loops;
     * process() must add any to-be-processed children to the list
     * */
    fun <V, W, R : Any> findRecursivePairs(
        initialFirst: V, initialSecond: W,
        process: (first: V, second: W, remaining: ArrayList<Any?>) -> R?
    ): R? {
        val remaining = getContainer<Any?>()
        val startIndex = remaining.size
        remaining.add(initialFirst)
        remaining.add(initialSecond)
        return findRecursiveRunPairs(remaining, startIndex, process)
    }

    fun <V, R : Any> findRecursiveSet(initial: Collection<V>, process: (item: V, remaining: ArrayList<V>) -> R?): R? {
        val remaining = getContainer<V>()
        val startIndex = remaining.size
        remaining.addAll(initial)
        return findRecursiveRun(remaining, startIndex, process)
    }

    fun <V, R : Any> findRecursiveRun(
        remaining: ArrayList<V>, startIndex: Int,
        process: (item: V, remaining: ArrayList<V>) -> R?
    ): R? {
        var maxSize = startIndex
        try {
            var result: R? = null
            while (result == null && remaining.size > startIndex) {
                val entry = remaining.removeLast()
                result = process(entry, remaining)
                maxSize = max(maxSize, remaining.size)
            }
            if (maxSize > startIndex + TRIM_SIZE) {
                remaining.trimToSize()
            }
            return result
        } finally {
            clearList(remaining, startIndex, maxSize)
        }
    }

    fun <V, W, R : Any> findRecursiveRunPairs(
        remaining: ArrayList<Any?>, startIndex: Int,
        process: (first: V, second: W, remaining: ArrayList<Any?>) -> R?
    ): R? {
        var maxSize = startIndex
        try {
            var result: R? = null
            @Suppress("UNCHECKED_CAST")
            while (result == null && remaining.size > startIndex) {
                val second = remaining.removeLast() as W
                val first = remaining.removeLast() as V
                result = process(first, second, remaining)
                maxSize = max(maxSize, remaining.size)
            }
            return result
        } finally {
            clearList(remaining, startIndex, maxSize)
        }
    }

    private fun <V> clearList(remaining: ArrayList<V>, startIndex: Int, maxSize: Int) {
        if (remaining.size > startIndex + 16) { // fast clear, one dynamic allocation
            remaining.subList(startIndex, remaining.size).clear()
        } else { // normal clear
            while (remaining.size > startIndex) {
                remaining.removeLast()
            }
        }
        if (maxSize > startIndex + TRIM_SIZE) {
            remaining.trimToSize()
        }
        assertEquals(startIndex, remaining.size)
    }

    private const val TRIM_SIZE = 500
}