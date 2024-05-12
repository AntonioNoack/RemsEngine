package me.anno.tests.graph.visual

import me.anno.graph.visual.scalar.BooleanMathType
import me.anno.graph.visual.scalar.MathB2Node
import me.anno.graph.visual.scalar.MathB3Node
import me.anno.graph.visual.scalar.NotNode
import me.anno.utils.types.Booleans.hasFlag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

object BooleanMathTests {
    @Test
    fun testNegate() {
        val node = NotNode()
        node.setInput(0, true)
        node.execute()
        assertEquals(false, node.getOutput(0))
        node.setInput(0, false)
        node.execute()
        assertEquals(true, node.getOutput(0))
    }

    @Test
    fun testAnd() {
        val node = MathB2Node().setEnumType(BooleanMathType.AND)
        for (i in 0 until 4) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.execute()
            assertEquals(i == 3, node.getOutput(0))
        }
    }

    @Test
    fun testOr() {
        val node = MathB2Node().setEnumType(BooleanMathType.OR)
        for (i in 0 until 4) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.execute()
            assertEquals(i > 0, node.getOutput(0))
        }
    }

    @Test
    fun testXor() {
        val node = MathB2Node().setEnumType(BooleanMathType.XOR)
        for (i in 0 until 4) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.execute()
            assertEquals(i == 1 || i == 2, node.getOutput(0))
        }
    }

    @Test
    fun testAnd3() {
        val node = MathB3Node().setEnumType(BooleanMathType.AND)
        for (i in 0 until 8) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.setInput(2, i.hasFlag(4))
            node.execute()
            assertEquals(i == 7, node.getOutput(0))
        }
    }

    @Test
    fun testOr3() {
        val node = MathB3Node().setEnumType(BooleanMathType.OR)
        for (i in 0 until 8) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.setInput(2, i.hasFlag(4))
            node.execute()
            assertEquals(i > 0, node.getOutput(0))
        }
    }

    @Test
    fun testXor3() {
        val node = MathB3Node().setEnumType(BooleanMathType.XOR)
        for (i in 0 until 8) {
            node.setInput(0, i.hasFlag(1))
            node.setInput(1, i.hasFlag(2))
            node.setInput(2, i.hasFlag(4))
            node.execute()
            assertEquals(i.hasFlag(1).xor(i.hasFlag(2)).xor(i.hasFlag(4)), node.getOutput(0))
        }
    }
}