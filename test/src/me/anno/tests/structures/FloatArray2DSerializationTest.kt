package me.anno.tests.structures

import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class FloatArray2DSerializationTest {
    @Test
    fun testSerialization() {
        val writer = JsonStringWriter(InvalidRef)
        writer.writeFloatArray2D("x", List(5) { FloatArray(5) { if (it < 3) it.toFloat() else 0f } })
        assertEquals("\"f[][]:x\":[5,[5,0,1,2],[5,0,1,2],[5,0,1,2],[5,0,1,2],[5,0,1,2]]", writer.toString())

        val reader = JsonStringReader(writer.toString(), InvalidRef)
        var wasCalled = false
        reader.readProperty(object : Saveable() {
            override fun setProperty(name: String, value: Any?) {
                val values = value as List<*>
                assertEquals("x", name)
                assertEquals("[[0,1,2,0,0],[0,1,2,0,0],[0,1,2,0,0],[0,1,2,0,0],[0,1,2,0,0]]",
                    values.joinToString(",", "[", "]") { fa ->
                        fa as FloatArray
                        fa.joinToString(",", "[", "]") { it.toInt().toString() }
                    })
                wasCalled = true
            }

            override fun isDefaultValue(): Boolean = false
            override val approxSize get() = -1
        })
        assertTrue(wasCalled)
    }
}
