package me.anno.tests.utils

import me.anno.engine.history.StringHistory
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test


/**
 * a test for StringHistories compression capabilities
 * */
class StringHistorySerializationTest {

    class TestHistory : StringHistory() {
        override fun apply(prev: String, curr: String) {}
    }

    @Test
    fun testSerialization() {
        Saveable.registerCustomClass(TestHistory())

        val entries = ArrayList<String>()
        entries.add("hallo")
        entries.add("hello")
        entries.add("hello world")
        entries.add("hell")
        entries.add("hello world, you")
        entries.add("kiss the world")
        entries.add("this is the world")
        entries.add("that was le world")

        val hist1 = TestHistory()
        for (v in entries) hist1.put(v)

        val str1 = JsonStringWriter.toText(hist1, InvalidRef)
        val hist2 = JsonStringReader.readFirst(str1, InvalidRef, TestHistory::class)
        val str2 = JsonStringWriter.toText(hist2, InvalidRef)
        assertEquals(str1, str2)

        assertEquals(entries.last(), hist1.currentState)
        assertEquals(entries.last(), hist2.currentState)

        for ((i, v) in entries.withIndex().reversed()) {
            assertEquals(v, hist1.currentState)
            assertEquals(v, hist2.currentState)
            assertEquals(i > 0, hist1.undo())
            assertEquals(i > 0, hist2.undo())
        }

        assertEquals(entries.first(), hist1.currentState)
        assertEquals(entries.first(), hist2.currentState)
    }
}