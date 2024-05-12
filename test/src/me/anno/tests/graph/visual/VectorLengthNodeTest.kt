package me.anno.tests.graph.visual

import me.anno.graph.visual.node.NodeLibrary.Companion.flowNodes
import me.anno.graph.visual.vector.VectorDistanceNode
import me.anno.graph.visual.vector.VectorLengthMode
import me.anno.graph.visual.vector.VectorLengthNode
import me.anno.graph.visual.vector.vectorTypes
import org.joml.Vector2d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VectorLengthNodeTest {
    @Test
    fun testExistsInList() {
        assertEquals(
            vectorTypes.size * VectorLengthMode.entries.size,
            flowNodes.allNodes.count { it.first is VectorLengthNode })
    }

    @Test
    fun testLength() {
        val node = VectorLengthNode().setDataType("Vector2d").setEnumType(VectorLengthMode.LENGTH_SQUARED)
        node.setInput(0, Vector2d(1.0, 2.0))
        node.compute()
        assertEquals(5.0, node.getOutput(0))
    }

    @Test
    fun testDistance() {
        val node = VectorDistanceNode().setDataType("Vector2d").setEnumType(VectorLengthMode.LENGTH_SQUARED)
        node.setInput(0, Vector2d(1.0, 2.0))
        node.setInput(1, Vector2d(3.0, 6.0))
        node.compute()
        assertEquals(20.0, node.getOutput(0))
    }
}