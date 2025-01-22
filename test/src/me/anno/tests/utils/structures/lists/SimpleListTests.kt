package me.anno.tests.utils.structures.lists

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.SimpleList
import me.anno.utils.structures.lists.SimpleListIterator
import org.junit.jupiter.api.Test

class SimpleListTests {

    private class SimpleListImpl<V>(val elements: List<V>) : SimpleList<V>() {
        override val size: Int
            get() = elements.size

        override fun get(index: Int): V {
            return elements[index]
        }
    }

    @Test
    fun testToString() {
        val list = SimpleListImpl(listOf(1, 2, 3, 4, 5, 6))
        assertEquals("[1, 2, 3, 4, 5, 6]", list.toString())
    }

    @Test
    fun testSubList() {
        val list = SimpleListImpl(listOf(1, 2, 3, 4, 5, 6))
        assertEquals(6, list.size)
        assertEquals(listOf(1, 2, 3), list.subList(0, 3))
        assertEquals(listOf(4, 5, 6), list.subList(3, 6))
    }

    @Test
    fun testIndexOf() {
        val list = SimpleListImpl(listOf(1, 2, 3, 4, 1, 6))
        assertEquals(0, list.indexOf(1))
        assertEquals(4, list.lastIndexOf(1))
    }

    @Test
    fun testSimpleListIterator() {
        val list = SimpleListImpl(listOf(1, 2, 3, 4))
        val iter = list.listIterator()
        assertIs(SimpleListIterator::class, iter)
        assertFalse(iter.hasPrevious())
        assertTrue(iter.hasNext())
        assertEquals(-1, iter.previousIndex())
        assertEquals(0, iter.nextIndex())
        assertEquals(1, iter.next())
        assertTrue(iter.hasPrevious())
        assertTrue(iter.hasNext())
        assertEquals(2, iter.next())
        assertEquals(3, iter.next())
        assertTrue(iter.hasPrevious())
        assertTrue(iter.hasNext())
        assertEquals(4, iter.next())
        assertTrue(iter.hasPrevious())
        assertFalse(iter.hasNext())
    }
}