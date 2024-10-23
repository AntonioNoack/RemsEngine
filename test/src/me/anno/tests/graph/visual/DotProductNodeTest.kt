package me.anno.tests.graph.visual

import me.anno.graph.visual.vector.DotProductNode
import me.anno.utils.assertions.assertEquals
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test

class DotProductNodeTest {
    @Test
    fun testVector2i() {
        val node = DotProductNode()
        node.setDataType("Vector2i")
        node.setInputs(listOf(Vector2i(1, 2), Vector2i(3, 4)))
        node.compute()
        assertEquals(11L, node.getOutput(0))
    }

    @Test
    fun testVector2f() {
        val node = DotProductNode()
        node.setDataType("Vector2f")
        node.setInputs(listOf(Vector2f(1f, 2f), Vector2f(3f, 4f)))
        node.compute()
        assertEquals(11f, node.getOutput(0))
    }

    @Test
    fun testVector2d() {
        val node = DotProductNode()
        node.setDataType("Vector2d")
        node.setInputs(listOf(Vector2d(1.0, 2.0), Vector2d(3.0, 4.0)))
        node.compute()
        assertEquals(11.0, node.getOutput(0))
    }

    @Test
    fun testVector3i() {
        val node = DotProductNode()
        node.setDataType("Vector3i")
        node.setInputs(listOf(Vector3i(1, 2, 3), Vector3i(3, 4, 5)))
        node.compute()
        assertEquals(26L, node.getOutput(0))
    }

    @Test
    fun testVector3f() {
        val node = DotProductNode()
        node.setDataType("Vector3f")
        node.setInputs(listOf(Vector3f(1f, 2f, 3f), Vector3f(3f, 4f, 5f)))
        node.compute()
        assertEquals(26f, node.getOutput(0))
    }

    @Test
    fun testVector3d() {
        val node = DotProductNode()
        node.setDataType("Vector3d")
        node.setInputs(listOf(Vector3d(1.0, 2.0, 3.0), Vector3d(3.0, 4.0, 5.0)))
        node.compute()
        assertEquals(26.0, node.getOutput(0))
    }

    @Test
    fun testVector4i() {
        val node = DotProductNode()
        node.setDataType("Vector4i")
        node.setInputs(listOf(Vector4i(1, 2, 3, 4), Vector4i(3, 4, 5, 6)))
        node.compute()
        assertEquals(50L, node.getOutput(0))
    }

    @Test
    fun testVector4f() {
        val node = DotProductNode()
        node.setDataType("Vector4f")
        node.setInputs(listOf(Vector4f(1f, 2f, 3f, 4f), Vector4f(3f, 4f, 5f, 6f)))
        node.compute()
        assertEquals(50f, node.getOutput(0))
    }

    @Test
    fun testVector4d() {
        val node = DotProductNode()
        node.setDataType("Vector4d")
        node.setInputs(listOf(Vector4d(1.0, 2.0, 3.0, 4.0), Vector4d(3.0, 4.0, 5.0, 6.0)))
        node.compute()
        assertEquals(50.0, node.getOutput(0))
    }
}