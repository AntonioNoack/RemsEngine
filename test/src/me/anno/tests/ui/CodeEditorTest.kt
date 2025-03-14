package me.anno.tests.ui

import me.anno.ui.editor.code.CodeBlock
import me.anno.ui.editor.code.CodeBlockCollapser
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class CodeEditorTest {

    @Test
    fun testBrackets() {
        val brackets = CodeBlockCollapser()
        brackets.close(CodeBlock(1, 2)) // removing one line
        val output = listOf(0, 1, 4, 5, 6, 7)
        val mapped = output.indices.map { brackets.mapLine(it) }
        assertEquals(output, mapped)
    }
}