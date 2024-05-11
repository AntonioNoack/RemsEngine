package me.anno.tests.io

import me.anno.engine.history.StringHistory
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
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
        val sequence = listOf("Hallo", "Hello World", "Hello World! 😁", "Hoi?", "aloha", "aloha", "aloha", "Hai")
        registerCustomClass(History())
        val sample = History()
        for (string in sequence) {
            sample.put(string)
        }
        assertEquals(sequence, sample.states)
        val asText = JsonStringWriter.toText(sample, InvalidRef)
        val clone = JsonStringReader.readFirst<History>(asText, InvalidRef)
        assertEquals(sequence, clone.states)
        val asText2 = JsonStringWriter.toText(clone, InvalidRef)
        assertEquals(asText, asText2)
    }
}