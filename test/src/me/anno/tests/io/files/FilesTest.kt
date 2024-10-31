package me.anno.tests.io.files

import me.anno.utils.OS.documents
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class FilesTest {

    // whatever folder
    private val base get() = documents

    @Test
    fun testRelativePathTo() {
        val path1 = base.getChild("a/b/c/d")
        val path2 = base.getChild("a/x/y/z")
        assertNull(path2.relativePathTo(path1, 0))
        assertNull(path2.relativePathTo(path1, 1))
        assertNull(path2.relativePathTo(path1, 2))
        assertEquals("../../../x/y/z", path2.relativePathTo(path1, 3))
    }

    @Test
    fun testRelativePathTo2() {
        val path1 = base.getChild("a/b")
        val path2 = base.getChild("a/b/c/d")
        assertEquals("c/d", path2.relativePathTo(path1, 0))
        assertEquals(null, path1.relativePathTo(path2, 1))
        assertEquals("../..", path1.relativePathTo(path2, 2))
    }

    @Test
    fun testRelativePathToEmpty() {
        val path1 = base.getChild("a/b/c")
        val path2 = base.getChild("a/b/c")
        assertEquals("", path2.relativePathTo(path1, 0))
    }

    @Test
    fun testSiblingWithExtension() {
        val path1 = base.getChild("a/b/name.png")
        val expected = base.getChild("a/b/name.jpg")
        assertEquals(expected, path1.getSiblingWithExtension("jpg"))
    }

    @Test
    fun testSibling() {
        val path1 = base.getChild("a/b/name.png")
        val expected = base.getChild("a/b/newName.jpg")
        assertEquals(expected, path1.getSibling("newName.jpg"))
    }

    @Test
    fun testIsSubFolderOf() {
        assertTrue(base.getChild("a/b/c").isSubFolderOf(base.getChild("a/b")))
        assertTrue(base.getChild("a/b/c").isSubFolderOf(base.getChild("a")))
        assertTrue(base.getChild("a/b/c").isSubFolderOf(base))
        assertFalse(base.getChild("a/b/c").isSubFolderOf(base.getChild("a/c")))
        assertFalse(base.getChild("a/b/c").isSubFolderOf(base.getChild("a/b/c")))
        assertFalse(base.getChild("a/b/c").isSubFolderOf(base.getChild("a/b/d")))
    }
}