package me.anno.tests.utils

import me.anno.io.files.Reference.getReference
import me.anno.io.files.WebRef
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test

class WebRefTest {
    @Suppress("PrivatePropertyName")
    private val LOGGER = LogManager.getLogger(WebRefTest::class)

    private fun getWebRef(path: String): WebRef {
        return getReference(path).resolved() as WebRef
    }

    @Test
    fun testGoogleURL() {
        val file = getWebRef("https://www.google.com/search?q=search&oq=search#hash")
        assertTrue(file.exists)
        LOGGER.info(file.lastModified)
        LOGGER.info(file.length())
        // LOGGER.info(WebRef.getHeaders(file.toUri().toURL(), 1000L, false))
        assertEquals(200, file.responseCode)
    }

    @Test
    fun testURLParsing() {
        val file = getWebRef("https://www.google.com/search?q=search&oq=search#hash")
        assertEquals("https://www.google.com/search", file.path)
        assertEquals(mapOf("q" to "search", "oq" to "search"), file.arguments)
        assertEquals("hash", file.hashbang)
    }
}