package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.utils.OS.documents
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TestRoot {
    @Test
    fun testRootIsFileFileRoot() {
        assertSame(FileRootRef, findRoot(documents))
    }

    private fun findRoot(file: FileReference): FileReference {
        val parent = file.getParent()
        return if (parent == InvalidRef) file
        else findRoot(parent)
    }
}