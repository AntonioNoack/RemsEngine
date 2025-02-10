package me.anno.tests.io.files

import me.anno.Engine
import me.anno.io.files.FileFileRef
import me.anno.utils.structures.Iterators.toList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class ReadFilesSyncBroken {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testReadLinesSync() {
        Engine.cancelShutdown()
        val file = FileFileRef.createTempFile("lines", "txt")
        val lines = "hey\ndu\nwas machst\ndu so?\n\n"
        file.writeText(lines)
        val readLines = file.readLinesSync(1024).toList()
        assertEquals(lines.split('\n'), readLines)
        file.delete()
    }
}