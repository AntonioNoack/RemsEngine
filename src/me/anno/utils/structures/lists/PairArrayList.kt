package me.anno.utils.structures.lists

import me.anno.utils.structures.tuples.MutablePair
import org.apache.logging.log4j.LogManager
import kotlin.math.max

@Suppress("unused")
class PairArrayList<A, B>(capacity: Int = 16) : Iterable<MutablePair<A, B>> {

    var array = arrayOfNulls<Any>(max(capacity * 2, 2))
    var elementSize = 0

    val size get() = elementSize shr 1

    fun clear() {
        elementSize = 0
        array.fill(null)
    }

    @Suppress("unchecked_cast")
    fun getA(index: Int) = array[index * 2] as A

    @Suppress("unchecked_cast")
    fun getB(index: Int) = array[index * 2 + 1] as B

    inline fun <V> iterate(run: (a: A, b: B) -> V?): V? {
        var index = 0
        for (i in 0 until size) {
            @Suppress("unchecked_cast")
            val v = run(array[index++] as A, array[index++] as B)
            if (v != null) return v
        }
        return null
    }

    @Suppress("unchecked_cast")
    inline fun forEachA(run: (a: A) -> Unit) {
        for (i in 0 until size) {
            run(array[i * 2] as A)
        }
    }

    @Suppress("unchecked_cast")
    inline fun forEachB(run: (b: B) -> Unit) {
        for (i in 0 until size) {
            run(array[i * 2 + 1] as B)
        }
    }

    fun add(a: A, b: B) {
        var elementSize = elementSize
        var array = array
        if (elementSize + 2 >= array.size) {
            val newArray = array.copyOf(array.size * 2)
            this.array = newArray
            array = newArray
        }
        array[elementSize++] = a
        array[elementSize++] = b
        this.elementSize = elementSize
    }

    fun byA(a: A): B? {
        var i = 0
        val array = array
        val size = elementSize
        while (i < size) {
            @Suppress("unchecked_cast")
            if (array[i] == a) return array[i + 1] as B
            i += 2
        }
        return null
    }

    fun byB(b: B): A? {
        var i = 1
        val array = array
        val size = elementSize
        while (i < size) {
            @Suppress("unchecked_cast")
            if (array[i] == b) return array[i - 1] as A
            i += 2
        }
        return null
    }

    fun removeAt(elementIndex: Int) {
        val array = array
        val size = elementSize
        if (size > elementIndex + 1) {
            // we can use the last one
            array[elementIndex + 1] = array[size - 1]
            array[elementIndex] = array[size - 2]
            elementSize -= 2
        } else {
            // we can just remove the two last
            elementSize -= 2
            // for the garbage collector
            array[elementIndex + 1] = null
            array[elementIndex] = null
        }
    }

    fun removeByA(a: A): Boolean {
        var i = 0
        val array = array
        val size = elementSize
        while (i < size) {
            if (array[i] == a) {
                removeAt(i)
                return true
            }
            i += 2
        }
        return false
    }

    fun removeByB(b: B): Boolean {
        var i = 1
        val array = array
        val size = elementSize
        while (i < size) {
            if (array[i] == b) {
                removeAt(i)
                return true
            }
            i += 2
        }
        return false
    }

    fun remove(a: A, b: B): Boolean {
        var i = 0
        val array = array
        val size = elementSize
        while (i < size) {
            if (array[i] == a && array[i + 1] == b) {
                removeAt(i)
                return true
            }
            i += 2
        }
        return false
    }

    /**
     * @return whether an element was added
     * */
    fun replaceOrAddMap(a: A, b: B): Boolean {
        var i = 0
        val array = array
        val size = elementSize
        while (i < size) {
            if (array[i] == a) {
                array[i + 1] = b
                return false
            }
            i += 2
        }
        add(a, b)
        return true
    }

    fun replaceBs(run: (a: A, b: B) -> B): Int {
        var changed = 0
        var i = 0
        val array = array
        val size = elementSize
        @Suppress("unchecked_cast")
        while (i < size) {
            val oldValue = array[i + 1] as B
            val newValue = run(array[i] as A, oldValue)
            if (newValue !== oldValue) {
                array[i + 1] = newValue
                changed++
            }
            i += 2
        }
        return changed
    }

    override fun iterator(): Iterator<MutablePair<A, B>> {
        return object : Iterator<MutablePair<A, B>> {
            @Suppress("unchecked_cast")
            val pair = MutablePair(null as A, null as B)
            var index = 0
            override fun hasNext(): Boolean = index < elementSize

            @Suppress("unchecked_cast")
            override fun next(): MutablePair<A, B> {
                pair.first = array[index++] as A
                pair.second = array[index++] as B
                return pair
            }
        }
    }

    /**
     * @return how many elements were removed
     * */
    fun removeIf(run: (a: A, b: B) -> Boolean): Int {
        var result = 0
        var i = 0
        while (i < elementSize) {
            @Suppress("unchecked_cast")
            if (run(array[i] as A, array[i + 1] as B)) {
                removeAt(i)
                result++
            } else i += 2 // this else is correct!
        }
        return result
    }

    override fun toString(): String {
        return (0 until size)
            .joinToString { "(${array[it * 2]}, ${array[it * 2 + 1]})" }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PairArrayList::class)
    }

}