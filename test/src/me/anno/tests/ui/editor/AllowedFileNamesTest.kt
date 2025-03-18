package me.anno.tests.ui.editor

import me.anno.ui.editor.files.FileNames.toAllowedFilename
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import org.junit.jupiter.api.Test

class AllowedFileNamesTest {
    @Test
    fun testAllowedFileNames() {
        assertEquals(".hidden", ".hidden".toAllowedFilename())
    }

    @Test
    fun testForbiddenFileNames() {
        assertNull("CON.txt".toAllowedFilename())
        assertNull("COM¹".toAllowedFilename())
        assertNull("LPT²".toAllowedFilename())
        assertNull("LPT³".toAllowedFilename())
        assertNull("COM5.exe".toAllowedFilename())
        assertNull("PRN".toAllowedFilename())
        assertNull("/*<?:>*/".toAllowedFilename())
        assertNull("..".toAllowedFilename())
        assertNull(".".toAllowedFilename())
    }
}