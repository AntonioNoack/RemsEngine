package me.anno.utils.structures.lists

import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.heap.Heap
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

@Suppress("unused")
object Lists {

    /**
     * allocation-free any()
     * */
    @JvmStatic
    inline fun <V> List<V>.any2(test: (V) -> Boolean): Boolean {
        for (index in indices) {
            if (test(this[index])) return true
        }
        return false
    }

    /**
     * allocation-free any()
     * */
    @JvmStatic
    inline fun <V> List<V>.all2(test: (V) -> Boolean): Boolean {
        for (index in indices) {
            if (!test(this[index])) return false
        }
        return true
    }

    /**
     * allocation-free any()
     * */
    @JvmStatic
    inline fun <V> List<V>.none2(test: (V) -> Boolean): Boolean {
        for (index in indices) {
            if (test(this[index])) return false
        }
        return true
    }

    /**
     * allocation-free count()
     * */
    @JvmStatic
    inline fun <V> List<V>.count2(test: (V) -> Boolean): Int {
        var sum = 0
        for (index in indices) {
            if (test(this[index])) sum++
        }
        return sum
    }

    /**
     * allocation-free firstOrNull()
     * */
    @JvmStatic
    inline fun <V> List<V>.firstOrNull2(test: (V) -> Boolean): V? {
        for (index in indices) {
            val element = this[index]
            if (test(element)) return element
        }
        return null
    }

    /**
     * allocation-free first()
     * */
    @JvmStatic
    inline fun <V : Any> List<V>.first2(test: (V) -> Boolean): V {
        return firstOrNull2(test) ?: throw NoSuchElementException()
    }

    /**
     * non-inline firstInstanceOrNull()
     * */
    @JvmStatic
    fun <V : Any> List<*>.firstInstanceOrNull2(clazz: KClass<V>): V? {
        for (i in indices) {
            val v = get(i)
            if (clazz.isInstance(v)) {
                @Suppress("UNCHECKED_CAST")
                return v as V
            }
        }
        return null
    }

    /**
     * allocation-free lastOrNull()
     * */
    @JvmStatic
    inline fun <V> List<V>.lastOrNull2(test: (V) -> Boolean): V? {
        for (index in lastIndex downTo 0) {
            val element = this[index]
            if (test(element)) return element
        }
        return null
    }

    /**
     * allocation-free last()
     * */
    @JvmStatic
    inline fun <V : Any> List<V>.last2(test: (V) -> Boolean): V {
        return lastOrNull2(test) ?: throw NoSuchElementException()
    }

    @JvmStatic
    inline fun <A, B> List<A>.mapFirstNotNull(run: (A) -> B): B? {
        for (index in indices) {
            val mapped = run(this[index])
            if (mapped != null) return mapped
        }
        return null
    }

    @JvmStatic
    fun List<Double>.median(default: Double): Double {
        return run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    @JvmStatic
    fun Iterable<Double>.median(default: Double): Double {
        return toList().run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    @JvmStatic
    fun List<Float>.median(default: Float): Float {
        return run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    @JvmStatic
    fun Iterable<Float>.median(default: Float): Float {
        return toList().run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    @JvmStatic
    fun <V> MutableList<V>.pop(): V? {
        if (isEmpty()) return null
        val last = last()
        removeAt(lastIndex)
        return last
    }

    @JvmStatic
    fun List<Double>.accumulate(): List<Double> {
        val accumulator = ArrayList<Double>()
        var sum = 0.0
        for (value in this) {
            sum += value
            accumulator += sum
        }
        return accumulator
    }

    @JvmStatic
    fun <V> List<V>.getOrPrevious(index: Int) = if (index > 0) this[index - 1] else this.getOrNull(0)

    @JvmStatic
    fun <V> MutableList<V>.swap(i: Int, j: Int) {
        val t = this[i]
        this[i] = this[j]
        this[j] = t
    }

    /**
     * splits the list such that the elements fulfilling the condition come first, and the rest second;
     * returns the index of the first element failing the condition; or length if none
     * */
    @JvmStatic
    fun <V> MutableList<V>.partition1(condition: (V) -> Boolean): Int {
        return partition1(0, size, condition)
    }

    /**
     * splits the list such that the elements fulfilling the condition come first, and the rest second;
     * returns the index of the first element failing the condition; or length if none
     * */
    @JvmStatic
    fun <V> MutableList<V>.partition1(start: Int, end: Int, condition: (V) -> Boolean): Int {

        var i = start
        var j = end - 1

        if (i == j) return i
        while (true) {
            // while front is fine, progress front
            while (i < j && condition(this[i])) i++
            // while back is fine, progress back
            while (i < j && !condition(this[j])) j--
            // if nothing works, swap i and j
            if (i < j) swap(i, j)
            else return i
        }
    }

    /**
     * searches for an element using a comparing function;
     * list must be sorted; return -1-insertIndex, if no element is found with compare(it) = 0
     * */
    @JvmStatic
    fun <V> List<V>.binarySearch(compare: (V) -> Int): Int {
        return binarySearch(0, size, compare)
    }

    /**
     * searches for an element using a comparing function;
     * list must be sorted; return -1-insertIndex, if no element is found with compare(it) = 0
     * */
    @JvmStatic
    fun <V> List<V>.binarySearch(fromIndex: Int = 0, toIndex: Int = size, compare: (V) -> Int): Int {

        // if the list is reversed, this will still work
        val sign = if (size > 1 && compare(get(0)) > compare(get(1))) -1 else +1

        var low = fromIndex
        var high = toIndex - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = get(mid)
            val cmp = compare(midVal) * sign
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid // key found
            }
        }
        return -(low + 1)  // key not found
    }

    @JvmStatic
    fun <V> List<V>.indexOf2(v: V, i0: Int, minus1: Boolean): Int {
        for (i in i0 until size) {
            if (this[i] == v) return i
        }
        return if (minus1) -1 else size
    }

    @JvmStatic
    fun <V> List<List<V>?>.flatten(): ArrayList<V> {
        val list = ArrayList<V>(sumOf { it?.size ?: 0 })
        for (i in indices) {
            val child = this[i] ?: continue
            list.addAll(child)
        }
        return list
    }

    @JvmStatic
    fun <X, Y : Comparable<Y>> List<X>.smallestKElementsBy(k: Int, getValue: (X) -> Y): List<X> {
        return if (size <= k) this
        else {
            val comp2 = { a: X, b: X -> getValue(a).compareTo(getValue(b)) }
            this.smallestKElements(k, comp2)
        }
    }

    @JvmStatic
    fun <X, Y : Comparable<Y>> Iterator<X>.smallestKElementsBy(k: Int, getValue: (X) -> Y): List<X> {
        val comp2 = { a: X, b: X -> getValue(a).compareTo(getValue(b)) }
        return this.smallestKElements(k, comp2)
    }

    @JvmStatic
    fun <X> Iterator<X>.smallestKElements(k: Int, comparator: Comparator<X>): List<X> {
        val topK = ArrayList<X>(k)
        for (j in 0 until k) {
            if (hasNext()) {
                topK.add(next())
            } else return topK
        }
        topK.sortWith(comparator)
        var lastBest = topK.last()
        while (hasNext()) {
            val element = next()
            if (comparator.compare(element, lastBest) < 0) {
                var index = topK.binarySearch(element, comparator)
                if (index < 0) index = -1 - index // insert index
                for (l in k - 1 downTo index + 1) {
                    topK[l] = topK[l - 1]
                }
                topK[index] = element
                lastBest = topK.last()
            }
        }
        return topK
    }

    @JvmStatic
    fun <X> List<X>.smallestKElements(k: Int, comparator: Comparator<X>): List<X> {
        if (size <= k) {
            return this
        } else {
            val topK = ArrayList(subList(0, k))
            topK.sortWith(comparator)
            var lastBest = topK.last()
            for (j in k until size) {
                val element = this[j]
                if (comparator.compare(element, lastBest) < 0) {
                    var index = topK.binarySearch(element, comparator)
                    if (index < 0) index = -1 - index // insert index
                    for (l in k - 1 downTo index + 1) {
                        topK[l] = topK[l - 1]
                    }
                    topK[index] = element
                    lastBest = topK.last()
                }
            }
            return topK
        }
    }

    @JvmStatic
    fun <X, Y : Comparable<Y>> List<X>.largestKElementsBy(k: Int, comparator: (X) -> Y): List<X> {
        return if (size <= k) {
            this
        } else {
            val comp2 = { a: X, b: X -> comparator(a).compareTo(comparator(b)) }
            this.largestKElements(k, comp2)
        }
    }

    @JvmStatic
    fun <X> List<X>.largestKElements(k: Int, comparator: Comparator<X>): List<X> {
        return if (size <= k) {
            this
        } else {
            smallestKElements(k) { a, b -> -comparator.compare(a, b) }
        }
    }

    @JvmStatic
    fun <X> List<X>.buildMaxHeap(comparator: Comparator<X>): ArrayList<X> {
        val list = ArrayList(this)
        Heap.buildMaxHeap(list, comparator)
        return list
    }

    @JvmStatic
    fun <X> List<X>.buildMinHeap(comparator: Comparator<X>): ArrayList<X> {
        val list = ArrayList(this)
        Heap.buildMinHeap(list, comparator)
        return list
    }

    @JvmStatic
    inline fun <reified V> List<V>.extractMin(k: Int, comparator: Comparator<V>): List<V> {
        if (k >= size) return this
        val topK = ArrayList<V>(k)
        for (i in 0 until k) {
            topK.add(this[i])
        }
        topK.sortWith(comparator)
        var lastBest = topK.last()
        for (j in k until size) {
            val element = this[j]
            if (comparator.compare(element, lastBest) < 0) {
                var index = topK.binarySearch { comparator.compare(it, element) }
                if (index < 0) index = -1 - index // insert index
                for (l in k - 1 downTo index + 1) {
                    topK[l] = topK[l - 1]
                }
                topK[index] = element
                lastBest = topK.last()
            }
        }
        return topK
    }

    /**
     * creates an ArrayList with size elements generated from a generator function
     * */
    @JvmStatic
    fun <V> createArrayList(size: Int, createElement: (index: Int) -> V): ArrayList<V> {
        val result = ArrayList<V>(size)
        for (i in 0 until size) {
            result.add(createElement(i))
        }
        return result
    }

    /**
     * creates an ArrayList with size elements all repeated
     * */
    @JvmStatic
    fun <V> createArrayList(size: Int, element: V): ArrayList<V> {
        val result = ArrayList<V>(size)
        for (i in 0 until size) {
            result.add(element)
        }
        return result
    }

    /**
     * creates an immutable List with size elements all repeated
     * */
    @JvmStatic
    fun <V> createList(size: Int, element: V): List<V> {
        return RepeatingList(size, element)
    }

    @JvmStatic
    fun <V> arrayListOfNulls(size: Int): ArrayList<V?> {
        return createArrayList(size, null)
    }

    @JvmStatic
    @Suppress("unchecked_cast")
    fun <V> List<List<V>>.transposed(): List<List<V>> {
        val m = size
        val n = maxOfOrNull { it.size } ?: return emptyList()
        return createArrayList(n) { i ->
            createArrayList(m) { j ->
                this[j].getOrNull(i) as V // could be null, V may be nullable,
                // so indeed this could introduce errors, when not all lists are long enough
            }
        }
    }

    @JvmStatic
    fun <V> MutableList<ArrayList<V>>.transpose(): MutableList<ArrayList<V>> {
        // to do function, which is in-place-transpose?
        val m = size
        val n = maxOfOrNull { it.size } ?: return this
        // make pseudo square: enough for both formats
        // ensure size
        for (i in m until n) {
            add(ArrayList())
        }
        // in all sub-lists, ensure size
        for (i in 0 until n) {
            val listI = this[i]
            for (j in listI.size until m) {
                @Suppress("unchecked_cast")
                listI.add(null as V)
            }
        }
        // transpose
        for (i in 1 until max(m, n)) {
            @Suppress("unchecked_cast")
            for (j in 0 until i) {
                val thisI = this[i]
                val thisJ = this[j]
                // this[j][i] = this[i].set(j, this[j][i]) ^^
                val t = thisJ.getOrNull(i) as V
                if (i < thisJ.size) thisJ[i] = thisI.getOrNull(j) as V
                thisI[j] = t
            }
        }
        // remove all unnecessary lists
        for (i in size - 1 downTo n) {
            removeAt(i)
        }
        // in all sub-lists, remove the unnecessary items
        for (i in 0 until n) {
            val listI = this[i]
            for (j in listI.size - 1 downTo m) {
                listI.removeAt(j)
            }
        }
        return this
    }

    @JvmStatic
    inline fun <reified Type> Iterable<*>.firstInstanceOrNull() =
        firstOrNull { it is Type } as? Type

    @JvmStatic
    inline fun <reified Type> Iterable<*>.firstInstance() =
        first { it is Type } as Type

    @JvmStatic
    fun <V> Collection<V>.sortedByTopology(getDependencies: (V) -> Collection<V>?): List<V> =
        toMutableList().sortByTopology(getDependencies)

    @JvmStatic
    fun <V> Collection<V>.sortedByParent(getParent: (V) -> V?): List<V> =
        toMutableList().sortByParent(getParent)

    /**
     * returns an order such that elements without dependencies come first,
     * and elements with dependencies come after their dependencies;
     * https://en.wikipedia.org/wiki/Topological_sorting
     *
     * @throws IllegalArgumentException if there is cyclic dependencies
     * */
    @JvmStatic
    fun <V> MutableList<V>.sortByTopology(getDependencies: (V) -> Collection<V>?): List<V> {

        val noPermanentMark = toHashSet()
        val temporaryMark = HashSet<V>(min(64, size))

        clear()

        fun visit(node: V) {
            if (node !in noPermanentMark) return
            if (node in temporaryMark) throw IllegalArgumentException("Found cyclic dependency by $temporaryMark")
            temporaryMark.add(node)
            val dependencies = getDependencies(node)
            if (!dependencies.isNullOrEmpty()) {
                for (dep in dependencies) {
                    visit(dep)
                }
            }
            temporaryMark.remove(node)
            noPermanentMark.remove(node)
            this.add(node)
        }

        while (noPermanentMark.isNotEmpty()) {
            visit(noPermanentMark.first())
        }

        return this
    }

    fun <V> MutableList<V>.sortByParent(getParent: (V) -> V?): List<V> {

        val noPermanentMark = toHashSet()
        val temporaryMark = HashSet<V>(min(64, size))

        clear()

        fun visit(node: V) {
            if (node !in noPermanentMark) return
            if (node in temporaryMark) throw IllegalArgumentException("Found cyclic dependency by $temporaryMark")
            temporaryMark.add(node)
            val parent = getParent(node)
            if (parent != null) {
                visit(parent)
            }
            temporaryMark.remove(node)
            noPermanentMark.remove(node)
            this.add(node)
        }

        while (noPermanentMark.isNotEmpty()) {
            visit(noPermanentMark.first())
        }

        return this
    }

    @JvmStatic
    fun <V> List<List<V>>.flattenWithSeparator(separator: V): List<V> {
        val result = ArrayList<V>(size + sumOf { it.size })
        if (isNotEmpty()) {
            result.addAll(first())
        }
        for (i in 1 until size) {
            result.add(separator)
            result.addAll(this[i])
        }
        return result
    }

    @JvmStatic
    fun <V> List<V>.iff(condition: Boolean): List<V> {
        return if (condition) this else emptyList()
    }

    @JvmStatic
    fun <V : Any> Any?.castToList(clazz: KClass<V>): List<V> {
        return if (this is List<*>) {
            if (all2 { clazz.isInstance(it) }) {
                @Suppress("UNCHECKED_CAST")
                this as List<V>
            } else filterIsInstance2(clazz)
        } else emptyList()
    }

    @JvmStatic
    fun <V : Any> V?.wrap(): List<V> {
        return if (this == null) emptyList()
        else listOf(this)
    }

    @JvmStatic
    fun <V> MutableList<V>.sortedAdd(instance: V, comparator: Comparator<V>, insertIfEquals: Boolean) {
        var index = binarySearch(instance, comparator)
        if (index < 0) index = -1 - index
        else if (!insertIfEquals && this[index] == instance) return
        add(index, instance)
    }

    @JvmStatic
    fun <V : Comparable<V>> MutableList<V>.sortedAdd(instance: V, insertIfEquals: Boolean) {
        sortedAdd(instance, { a, b -> a.compareTo(b) }, insertIfEquals)
    }
}