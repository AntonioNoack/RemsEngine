package me.anno.tests.graph.visual

import me.anno.graph.visual.vector.SeparateVectorNode
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeparateVectorNodeTest {
    @Test
    fun testReturnInt() {
        val node = SeparateVectorNode().setDataType("Vector2i")
        node.setInput(0, Vector2i(1, 2))
        node.compute()
        assertEquals(1, node.getOutput(0))
        assertEquals(2, node.getOutput(1))
    }

    @Test
    fun testReturnFloat() {
        val node = SeparateVectorNode().setDataType("Vector2f")
        node.setInput(0, Vector2f(1f, 2f))
        node.compute()
        assertEquals(1f, node.getOutput(0))
        assertEquals(2f, node.getOutput(1))
    }

    @Test
    fun testReturnDouble() {
        val node = SeparateVectorNode().setDataType("Vector2d")
        node.setInput(0, Vector2d(1.0, 2.0))
        node.compute()
        assertEquals(1.0, node.getOutput(0))
        assertEquals(2.0, node.getOutput(1))
    }
}