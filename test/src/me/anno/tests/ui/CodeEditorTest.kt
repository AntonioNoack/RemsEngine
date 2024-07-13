package me.anno.tests.ui

import me.anno.ui.editor.code.CodeBlockCollapser
import me.anno.ui.editor.code.CodeBlock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CodeEditorTest {

    @Test
    fun testBrackets() {
        val brackets = CodeBlockCollapser()
        brackets.close(CodeBlock(1, 2)) // removing one line
        val output = listOf(0, 1, 3, 4, 5, 6)
        val mapped = output.indices.map { brackets.mapLine(it) }
        assertEquals(output, mapped)
    }
}