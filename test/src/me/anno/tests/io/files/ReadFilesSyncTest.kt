package me.anno.tests.io.files

import me.anno.Engine
import me.anno.io.files.FileFileRef
import me.anno.utils.structures.Iterators.toList
import me.anno.utils.types.Strings.splitLines
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class ReadFilesSyncTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testReadLinesSync() {
        Engine.cancelShutdown()
        val file = FileFileRef.createTempFile("lines", "txt")

        val text = "hey\ndu\nwas machst\ndu so?\n\n"
        file.writeText(text)
        assertEquals(text, file.readTextSync())

        val readLines = file.readLinesSync(1024).toList()
        assertEquals(text.splitLines(), readLines)
        file.delete()
    }

    @Test
    fun testSplitLines() {
        val lines = listOf(
            "hey",
            "du",
            "was machst",
            "du so?",
            "",
            ""
        )
        assertEquals(lines, lines.joinToString("\n").splitLines()) // Linux
        assertEquals(lines, lines.joinToString("\r\n").splitLines()) // Windows
        assertEquals(lines, lines.joinToString("\r").splitLines()) // MacOS
    }
}