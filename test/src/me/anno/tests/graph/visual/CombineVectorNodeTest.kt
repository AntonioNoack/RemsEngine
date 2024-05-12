package me.anno.tests.graph.visual

import me.anno.graph.visual.vector.CombineVectorNode
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CombineVectorNodeTest {
    @Test
    fun testReturnInt() {
        val node = CombineVectorNode().setDataType("Vector2i")
        node.setInputs(listOf(1, 2))
        node.compute()
        assertEquals(Vector2i(1, 2), node.getOutput(0))
    }

    @Test
    fun testReturnFloat() {
        val node = CombineVectorNode().setDataType("Vector2f")
        node.setInputs(listOf(1f, 2f))
        node.compute()
        assertEquals(Vector2f(1f, 2f), node.getOutput(0))
    }

    @Test
    fun testReturnDouble() {
        val node = CombineVectorNode().setDataType("Vector2d")
        node.setInputs(listOf(1.0, 2.0))
        node.compute()
        assertEquals(Vector2d(1.0, 2.0), node.getOutput(0))
    }
}