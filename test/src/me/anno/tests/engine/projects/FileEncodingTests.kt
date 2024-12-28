package me.anno.tests.engine.projects

import me.anno.engine.projects.FileEncoding
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class FileEncodingTests {
    @Test
    fun testIsPretty() {
        assertTrue(FileEncoding.PRETTY_JSON.isPretty)
        assertTrue(FileEncoding.PRETTY_XML.isPretty)
        assertTrue(FileEncoding.YAML.isPretty)
        assertFalse(FileEncoding.COMPACT_JSON.isPretty)
        assertFalse(FileEncoding.COMPACT_XML.isPretty)
        assertFalse(FileEncoding.BINARY.isPretty)
    }

    @Test
    fun testExtension() {
        assertEquals("json", FileEncoding.PRETTY_JSON.extension)
        assertEquals("json", FileEncoding.COMPACT_JSON.extension)
        assertEquals("xml", FileEncoding.PRETTY_XML.extension)
        assertEquals("xml", FileEncoding.COMPACT_XML.extension)
        assertEquals("yaml", FileEncoding.YAML.extension)
        assertEquals("rem", FileEncoding.BINARY.extension)
    }
}