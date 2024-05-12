package me.anno.tests.graph.visual

import me.anno.graph.visual.scalar.FloatMathBinary
import me.anno.graph.visual.scalar.FloatMathTernary
import me.anno.graph.visual.scalar.FloatMathUnary
import me.anno.graph.visual.vector.CrossProductNode
import me.anno.graph.visual.vector.MathF1XNode
import me.anno.graph.visual.vector.MathF2XNode
import me.anno.graph.visual.vector.MathF3XNode
import me.anno.graph.visual.vector.NormalizeNode
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2d
import org.joml.Vector3d
import org.junit.jupiter.api.Test

object VectorMathTests {
    @Test
    fun testCross3d() {
        val node = CrossProductNode().setDataType("Vector3d")
        node.setInput(0, Vector3d(1.0, 0.0, 0.0))
        node.setInput(1, Vector3d(0.0, 1.0, 0.0))
        node.execute()
        assertEquals(Vector3d(0.0, 0.0, 1.0), node.getOutput(0))
    }

    @Test
    fun testCross2d() {
        val node = CrossProductNode().setDataType("Vector2d")
        node.setInput(0, Vector2d(1.0, 0.0))
        node.setInput(1, Vector2d(0.0, 1.0))
        node.execute()
        assertEquals(1.0, node.getOutput(0))
    }

    @Test
    fun testNormalize() {
        val node = NormalizeNode().setDataType("Vector3d")
        node.setInput(0, Vector3d(0.0, 3.0, 4.0))
        node.execute()
        assertTrue(Vector3d(0.0, 3.0 / 5.0, 4.0 / 5.0).equals(node.getOutput(0) as Vector3d, 1e-3))
    }

    @Test
    fun testF1X() {
        val node = MathF1XNode().setDataType("Vector3d").setEnumType(FloatMathUnary.NEG)
        node.setInput(0, Vector3d(1.0, -3.0, 5.0))
        node.execute()
        assertEquals(Vector3d(-1.0, 3.0, -5.0), node.getOutput(0))
    }

    @Test
    fun testF2X() {
        val node = MathF2XNode().setDataType("Vector3d").setEnumType(FloatMathBinary.SUB)
        node.setInput(0, Vector3d(1.0, -3.0, 5.0))
        node.setInput(1, Vector3d(2.0, 2.0, 3.0))
        node.execute()
        assertEquals(Vector3d(-1.0, -5.0, 2.0), node.getOutput(0))
    }

    @Test
    fun testF3X() {
        val node = MathF3XNode().setDataType("Vector3d").setEnumType(FloatMathTernary.MIX)
        node.setInput(0, Vector3d(1.0, -3.0, 5.0))
        node.setInput(1, Vector3d(2.0, 2.0, 3.0))
        node.setInput(2, Vector3d(1.0, 0.0, 0.5))
        node.execute()
        assertEquals(Vector3d(2.0, -3.0, 4.0), node.getOutput(0))
    }
}