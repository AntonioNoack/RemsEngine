package me.anno.tests.graph.visual

import me.anno.graph.visual.scalar.IntMathBinary
import me.anno.graph.visual.scalar.IntMathTernary
import me.anno.graph.visual.scalar.IntMathUnary
import me.anno.graph.visual.scalar.MathI1Node
import me.anno.graph.visual.scalar.MathI2Node
import me.anno.graph.visual.scalar.MathI3Node
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

object IntMathTests {
    @Test
    fun testNegate() {
        val node = MathI1Node().setDataType("Int").setEnumType(IntMathUnary.NEG)
        node.setInput(0, 10)
        node.execute()
        assertEquals(-10, node.getOutput(0))
    }

    @Test
    fun testMul() {
        val node = MathI2Node().setDataType("Int").setEnumType(IntMathBinary.MUL)
        node.setInput(0, -2)
        node.setInput(1, 5)
        node.execute()
        assertEquals(-10, node.getOutput(0))
    }

    @Test
    fun testClamp() {
        val node = MathI3Node().setDataType("Int").setEnumType(IntMathTernary.CLAMP)
        node.setInput(0, -2)
        node.setInput(1, 5)
        node.setInput(2, 10)
        node.execute()
        assertEquals(5, node.getOutput(0))
    }
}