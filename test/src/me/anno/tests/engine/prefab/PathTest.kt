package me.anno.tests.engine.prefab

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertSame
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class PathTest {

    private fun createPath(names: List<String>, indices: List<Int>, types: List<Char>): Path = Path(
        if (names.size > 1) createPath(
            names.subList(0, names.lastIndex),
            indices.subList(0, names.lastIndex),
            types.subList(0, types.lastIndex)
        ) else ROOT_PATH, names.last(), indices.last(), types.last()
    )

    val p1 = createPath(listOf("1"), listOf(1), listOf('a'))
    val p2 = createPath(listOf("2"), listOf(2), listOf('b'))
    val p3 = createPath(listOf("3"), listOf(3), listOf('c'))
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
        assertEquals(p, ROOT_PATH)
        assertEquals(p12, p123.parent)
    }

    @Test
    fun testNotEqual() {
        if (p !== ROOT_PATH) throw RuntimeException()

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
        assertEquals(createPath(listOf("2", "3"), listOf(2, 3), listOf('b', 'c')), p23)
        assertEquals(createPath(listOf("1", "2", "3"), listOf(1, 2, 3), listOf('a', 'b', 'c')), p123)
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

        val ab = createPath(listOf("a", "b"), listOf(0, 1), listOf('x', 'x'))
        val abc = createPath(listOf("a", "b", "c"), listOf(0, 1, 2), listOf('x', 'x', 'x'))
        val bcd = createPath(listOf("b", "c", "d"), listOf(1, 2, 3), listOf('x', 'x', 'x'))

        assertSame(ROOT_PATH, abc.getRestIfStartsWith(abc, 0))
        assertTrue(null === abc.getRestIfStartsWith(ab, 0))
        assertTrue(null === abc.getRestIfStartsWith(abc, 1))
        assertEquals(Path(ROOT_PATH, "d", 3, 'x'), abc.getRestIfStartsWith(bcd, 1))
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
        if (sample.prefabPath != ROOT_PATH) throw RuntimeException()
        val c1 = prefab.add(ROOT_PATH, 'e', "Entity", "C1")
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