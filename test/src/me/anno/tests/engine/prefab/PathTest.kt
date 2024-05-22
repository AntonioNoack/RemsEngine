package me.anno.tests.engine.prefab

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PathTest {

    val p1 = Path(listOf("1"), intArrayOf(1), charArrayOf('a'))
    val p2 = Path(listOf("2"), intArrayOf(2), charArrayOf('b'))
    val p3 = Path(listOf("3"), intArrayOf(3), charArrayOf('c'))
    val p23 = p2 + p3
    val p12 = p1 + p2
    val p123 = p12 + p3
    val p = p1.parent!!

    @Test
    fun testSize() {
        assertEquals(3, p123.size)
        assertEquals(2, p12.size)
        assertEquals(1, p1.size)
        assertEquals(0, p.size)
    }

    @Test
    fun testParents() {
        assertEquals(p1, p12.parent)
        assertEquals(p, p1.parent)
        assertEquals(p, Path.ROOT_PATH)
        assertEquals(p12, p123.parent)
    }

    @Test
    fun testNotEqual() {
        if (p !== Path.ROOT_PATH) throw RuntimeException()

        assertNotEquals(p, p1)
        assertNotEquals(p, p12)
        assertNotEquals(p, p123)
        assertNotEquals(p1, p12)
        assertNotEquals(p1, p123)
        assertNotEquals(p12, p123)
    }

    @Test
    fun testToString() {
        assertEquals("a1,1/b2,2/c3,3", p123.toString())
        assertEquals("a1,1/b2,2", p12.toString())
        assertEquals("a1,1", p1.toString())
        assertEquals("", p.toString())
    }

    @Test
    fun subList() {
        assertEquals(p123, p123.subList(0))
        assertEquals(p23, p123.subList(1))
        assertEquals(p3, p123.subList(2))
        assertEquals(p, p123.subList(3))
    }

    @Test
    fun testConcat() {
        val p12123 = p12 + p123
        assertEquals(Path(listOf("2", "3"), intArrayOf(2, 3), charArrayOf('b', 'c')), p23)
        assertEquals(Path(listOf("1", "2", "3"), intArrayOf(1, 2, 3), charArrayOf('a', 'b', 'c')), p123)
        assertEquals("a1,1/b2,2/a1,1/b2,2/c3,3", p12123.toString())
    }

    @Test
    fun testFromString() {
        val groundTruth = p123.toString()
        val copy = Path.fromString(groundTruth)
        val copied = copy.toString()
        assertEquals(groundTruth, copied)
        assertEquals(p123, copy)
    }

    @Test
    fun testStartsWith() {
        assertTrue(p123.startsWith(p))
        assertTrue(p123.startsWith(p1))
        assertTrue(p123.startsWith(p12))
        assertTrue(p123.startsWith(p123))
        assertFalse(p.startsWith(p123))
        assertFalse(p1.startsWith(p123))
        assertFalse(p12.startsWith(p123))
    }

    @Test
    fun testRestIfStartsWith() {

        val ab = Path(listOf("a", "b"), intArrayOf(0, 1), charArrayOf('x', 'x'))
        val abc = Path(listOf("a", "b", "c"), intArrayOf(0, 1, 2), charArrayOf('x', 'x', 'x'))
        val bcd = Path(listOf("b", "c", "d"), intArrayOf(1, 2, 3), charArrayOf('x', 'x', 'x'))

        assertSame(Path.ROOT_PATH, abc.getRestIfStartsWith(abc, 0))
        assertTrue(null === abc.getRestIfStartsWith(ab, 0))
        assertTrue(null === abc.getRestIfStartsWith(abc, 1))
        assertEquals(Path(Path.ROOT_PATH, "d", 3, 'x'), abc.getRestIfStartsWith(bcd, 1))
    }

    @Test
    fun testJson() {
        registerCustomClass(Path())
        val cloned = JsonStringReader.read(JsonStringWriter.toText(p123, InvalidRef), InvalidRef, false).first()
        assertEquals(p123, cloned)
    }

    fun main() {

        val prefab = Prefab("Entity")
        val sample = prefab.getSampleInstance()
        if (sample.prefabPath != Path.ROOT_PATH) throw RuntimeException()
        val c1 = prefab.add(Path.ROOT_PATH, 'e', "Entity", "C1")
        /*val c2 = */prefab.add(c1, 'e', "Entity", "C2")
        // val c3 = prefab.add(c2, 'e', "Entity", "C3")

        val adds = prefab.adds

        for ((_, addsI) in adds) {
            for (add in addsI) {
                val x0 = JsonStringWriter.toText(add, InvalidRef)
                val x1 = JsonStringReader.read(x0, InvalidRef, false)[0] as CAdd
                val x2 = JsonStringWriter.toText(x1, InvalidRef)
                if (x0 != x2) {
                    println(JsonFormatter.format(x0))
                    println(JsonFormatter.format(x2))
                    throw RuntimeException()
                }
            }
        }

        val json = JsonStringWriter.toText(prefab, InvalidRef)
        val prefabClone = JsonStringReader.read(json, InvalidRef, false)[0] as Prefab

        println(prefab.adds)

        println(JsonFormatter.format(json))

        println(prefabClone.adds)
        val json2 = JsonStringWriter.toText(prefabClone, InvalidRef)
        println(JsonFormatter.format(json2))
        if (json != json2) throw RuntimeException()
    }
}