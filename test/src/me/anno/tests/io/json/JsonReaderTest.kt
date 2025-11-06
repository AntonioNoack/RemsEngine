package me.anno.tests.io.json

import me.anno.io.json.generic.JsonReader
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.EOFException

class JsonReaderTest {

    @Test
    fun testReadSimpleObject() {
        val json = """{ "name": "Alice", "age": 30 }"""
        val reader = JsonReader(json)

        val result = reader.readObject()
        assertEquals("Alice", result["name"])
        assertEquals("30", result["age"])
    }

    @Test
    fun testReadNestedObject() {
        val json = """{
            "user": {
                "name": "Bob",
                "details": { "age": 25, "country": "USA" }
            }
        }"""
        val reader = JsonReader(json)

        val result = reader.readObject()
        val user = result["user"] as Map<*, *>
        val details = user["details"] as Map<*, *>

        assertEquals("Bob", user["name"])
        assertEquals("25", details["age"])
        assertEquals("USA", details["country"])
    }

    @Test
    fun testReadArray() {
        val json = """[1, 2, 3, 4, 5]"""
        val result = JsonReader(json).readArray()
        assertEquals(listOf("1", "2", "3", "4", "5"), result)
    }

    @Test
    fun testReadNestedArray() {
        val json = """[[1, 2], [3, 4], [5]]"""
        val result = JsonReader(json).readArray()
        assertEquals(listOf(listOf("1", "2"), listOf("3", "4"), listOf("5")), result)
    }

    @Test
    fun testReadPrimitiveValue() {
        val json = """"Hello""""
        val result = JsonReader(json).read()
        assertEquals("Hello", result)
    }

    @Test
    fun testReadEmptyObject() {
        val json = "{}"
        val result = JsonReader(json).readObject()
        assertTrue(result.isEmpty())
    }

    @Test
    fun testReadEmptyArray() {
        val json = "[]"
        val result = JsonReader(json).readArray()
        assertTrue(result.isEmpty())
    }

    @Test
    fun testReadObjectWithComments() {
        val json = """
            {
                // this is a line comment
                "name": "Charlie", /* inline comment */
                "age": 40 // another comment
            }
        """
        val result = JsonReader(json).readObject()
        assertEquals("Charlie", result["name"])
        assertEquals("40", result["age"])
    }

    @Test
    fun testReadArrayWithComments() {
        val json = """
            [
                1, // first number
                2, /* inline comment */
                3
            ]
        """
        val result = JsonReader(json).readArray()
        assertEquals(listOf("1", "2", "3"), result)
    }

    @Test
    fun testMalformedJsonThrowsException() {
        val json = """{ "name": "Invalid", """
        assertThrows<EOFException> { JsonReader(json).readObject() }
    }

    @Test
    fun testReadMixedArray() {
        val json = """[ "text", 123, true, null, { "key": "value" } ]"""
        val reader = JsonReader(json)

        val result = reader.readArray()
        assertEquals("text", result[0])
        assertEquals("123", result[1])
        assertEquals(true, result[2])
        assertNull(result[3])
        assertEquals(mapOf("key" to "value"), result[4])
    }

    @Test
    fun testReadTopLevelValue() {
        val json = "42"
        val result = JsonReader(json).read()
        assertEquals("42", result)
    }
}
