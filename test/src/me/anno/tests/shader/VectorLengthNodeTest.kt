package me.anno.tests.shader

import me.anno.graph.visual.node.NodeLibrary.Companion.flowNodes
import me.anno.graph.visual.vector.VectorLengthNode
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class VectorLengthNodeTest {
    @Test
    fun testExistsInList() {
        assertEquals(3 * 3, flowNodes.allNodes.count { it.first is VectorLengthNode })
    }
}