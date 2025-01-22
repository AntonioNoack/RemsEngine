package me.anno.tests.utils

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.utils.assertions.assertEquals
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.files.Files.findNextName
import org.junit.jupiter.api.Test

class FindNextNameTests {

    private fun createFolder(fileNames: String): FileReference {
        val tmp = InnerTmpTextFile("")
        val folder = InnerFolder(tmp)
        for (fileName in fileNames.split(',')) {
            folder.createChild(fileName, null)
        }
        return folder
    }

    @Test
    fun testFindNextFileNameInBetween() {
        // if any number is present, the next one is chosen
        val folder = createFolder("a.txt,a-1.txt,a-2.txt,a-003.txt,b.txt,c-3.txt")
        assertEquals("c-4.txt", findNextFileName(folder, "c", "txt", 1, '-'))
    }

    @Test
    fun testFindNextFileNameContinue() {
        val folder = createFolder("a.txt,a-1.txt,a-2.txt,a-003.txt,b.txt,c-3.txt")
        assertEquals("a-4.txt", findNextFileName(folder, "a", "txt", 1, '-'))
        assertEquals("a-04.txt", findNextFileName(folder, "a", "txt", 2, '-'))
        assertEquals("a-004.txt", findNextFileName(folder, "a", "txt", 3, '-'))
    }

    @Test
    fun testFindNextFileNameByFile() {
        val folder = createFolder("a.txt,a-1.txt,a-2.txt,a-003.txt,b.txt,c-3.txt")
        val sibling = folder.getChild("a.txt")
        assertEquals("a-4.txt", findNextFileName(sibling, 1, '-'))
        assertEquals("a-04.txt", findNextFileName(sibling, 2, '-'))
        assertEquals("a-004.txt", findNextFileName(sibling, 3, '-'))
    }

    @Test
    fun testFindNextNameNew() {
        assertEquals("a-1", findNextName("a", '-'))
        assertEquals("a-0", findNextName("a", '-', 0))
        assertEquals("a-005", findNextName("a", '-', 5, 3))
    }

    @Test
    fun testFindNextNameContinue() {
        // if any number is present, starting-number is ignored
        assertEquals("a-002", findNextName("a-1", '-', 5, 3))
        assertEquals("a-12", findNextName("a-11", '-', 5, 1))
    }
}