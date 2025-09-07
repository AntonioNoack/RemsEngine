package me.anno.tests.io.yaml

import me.anno.io.yaml.generic.YAMLReader
import me.anno.io.yaml.generic.YAMLReaderV2
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import org.junit.jupiter.api.Test

class YAMLListTests {
    @Test
    fun testYAMLLists() {
        val text = """
            items:
              - a: 1
                b: 2
              - a: 5
                b: 6
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val yaml = YAMLReaderV2.parseYAML(text.reader().buffered(), false) as Map<String, Any>

        @Suppress("UNCHECKED_CAST")
        val items = assertNotNull(yaml["items"]) as List<Map<*, *>>
        val asList = items
        assertEquals(2, asList.size)

        val item0 = asList[0]
        val item1 = asList[1]

        assertEquals("1", item0["a"])
        assertEquals("2", item0["b"])

        assertEquals("5", item1["a"])
        assertEquals("6", item1["b"])
    }
}