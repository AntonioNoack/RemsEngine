package me.anno.tests.io.files

import me.anno.io.files.Reference.sanitizePath
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class SanitizePathTest {
    @Test
    fun testRemovesSingleDots() {
        assertEquals("b", sanitizePath("./b"))
    }

    @Test
    fun testReplacesDoubleDots() {
        assertEquals("b", sanitizePath("a/../b/c/.."))
    }

    @Test
    fun testRemovesSlashAtEnd() {
        assertEquals("a/b/c", sanitizePath("a/b/c/"))
    }

    @Test
    fun testReplacesBackwardsSlashes() {
        assertEquals("a/b/c", sanitizePath("a\\b/c\\"))
    }

    @Test
    fun testRemovesDoubleSlashes() {
        assertEquals("a/b/c", sanitizePath("//a//b///c//"))
    }

    @Test
    fun testKeepsProtocol() {
        assertEquals("tmp://a/b/c", sanitizePath("tmp://a//b//c/"))
    }

    @Test
    fun testKeepsDriveLetterSlash() {
        assertEquals("C:/", sanitizePath("C:/"))
    }
}