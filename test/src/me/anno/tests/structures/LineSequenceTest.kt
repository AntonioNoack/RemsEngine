package me.anno.tests.structures

import me.anno.utils.structures.arrays.LineSequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LineSequenceTest {
    @Test
    fun testInsertAndRemove() {
        val ls = LineSequence()
        val text0 = "this is great\nisn't it?"
        ls.setText(text0)
        assertEquals(text0, ls.toString())
        ls[0, 0] = 'T'.code
        ls[1, 0] = 'I'.code
        assertEquals("This is great\nIsn't it?", ls.toString())
        ls.insert(1, "isn't it".length, '!'.code)
        assertEquals("This is great\nIsn't it!?", ls.toString())
        ls.remove(1, "isn't it!".length)
        assertEquals("This is great\nIsn't it!", ls.toString())
        ls.remove(1, 2)
        ls.remove(1, 2)
        ls.remove(1, 2)
        assertEquals("This is great\nIs it!", ls.toString())
    }
}