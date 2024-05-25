package me.anno.tests.io

import me.anno.engine.history.StringHistory
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringHistoryTest {
    class History : StringHistory() {
        private var curr: String? = null
        override fun apply(prev: String, curr: String) {
            this.curr = curr
        }
    }

    @Test
    fun testSerialization() {
        val s0 = "Hello World"
        val s1 = ", dear visitor"
        val s2 = "Antonio says: "
        val s3 = "Test passed?"
        val sequence = listOf(
            s0, s0 + s1, // append
            s0, s2 + s0, // prepend
            s0, s3, // replace
            s0,
        )
        registerCustomClass(History())
        val sample = History()
        for (string in sequence) {
            sample.put(string)
        }
        assertEquals(sequence, sample.states)
        val asText = JsonStringWriter.toText(sample, InvalidRef)
        val clone = JsonStringReader.readFirst(asText, InvalidRef, History::class)
        assertEquals(sequence, clone.states)
        val asText2 = JsonStringWriter.toText(clone, InvalidRef)
        assertEquals(asText, asText2)
    }
}