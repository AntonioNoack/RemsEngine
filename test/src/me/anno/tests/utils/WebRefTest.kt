package me.anno.tests.utils

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.WebRef
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebRefTest {
    @Suppress("PrivatePropertyName")
    private val LOGGER = LogManager.getLogger(WebRefTest::class)

    @Test
    fun testGoogleURL() {
        val file = getReference("https://www.google.com/search?q=search&oq=search#hash") as WebRef
        assertTrue(file.exists)
        LOGGER.info(file.lastModified)
        LOGGER.info(file.length())
        LOGGER.info(WebRef.getHeaders(file.toUri().toURL(), 1000L, false))
        assertEquals(200, file.responseCode)
    }

    @Test
    fun testURLParsing() {
        val file = getReference("https://www.google.com/search?q=search&oq=search#hash") as WebRef
        assertEquals("https://www.google.com/search", file.path)
        assertEquals(mapOf("q" to "search", "oq" to "search"), file.arguments)
        assertEquals("hash", file.hashbang)
    }
}