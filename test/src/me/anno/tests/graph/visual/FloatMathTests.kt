package me.anno.tests.graph.visual

import me.anno.graph.visual.scalar.FloatMathBinary
import me.anno.graph.visual.scalar.FloatMathTernary
import me.anno.graph.visual.scalar.FloatMathUnary
import me.anno.graph.visual.scalar.MathF1Node
import me.anno.graph.visual.scalar.MathF2Node
import me.anno.graph.visual.scalar.MathF3Node
import me.anno.maths.Maths.mix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

object FloatMathTests {
    @Test
    fun testNegate() {
        val node = MathF1Node().setDataType("Float").setEnumType(FloatMathUnary.NEG)
        node.setInput(0, 1f)
        node.execute()
        assertEquals(-1f, node.getOutput(0))
    }

    @Test
    fun testMul() {
        val node = MathF2Node().setDataType("Float").setEnumType(FloatMathBinary.MUL)
        node.setInput(0, -2f)
        node.setInput(1, 5f)
        node.execute()
        assertEquals(-10f, node.getOutput(0))
    }

    @Test
    fun testMix() {
        val node = MathF3Node().setDataType("Float").setEnumType(FloatMathTernary.MIX)
        node.setInput(0, -2f)
        node.setInput(1, 5f)
        node.setInput(2, 0.25f)
        node.execute()
        assertEquals(mix(-2f, 5f, 0.25f), node.getOutput(0))
    }
}