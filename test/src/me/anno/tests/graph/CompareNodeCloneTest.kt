package me.anno.tests.graph

import me.anno.graph.visual.node.NodeLibrary
import me.anno.graph.visual.scalar.CompareMode
import me.anno.graph.visual.scalar.CompareNode
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompareNodeCloneTest {
    @Test
    fun testUpdateName() {
        val node = CompareNode("Type")
        assertEquals("Type Less Than", node.name)
        node.enumType = CompareMode.LESS_OR_EQUALS
        assertEquals("Type Less Or Equals", node.name)
    }

    @Test
    fun testClone() {
        val node = CompareNode("Type")
        val clone = node.clone()
        assertEquals(node.name, clone.name)
    }

    @Test
    fun testNodeLibrary() {
        val flowNodes = NodeLibrary(
            listOf { CompareNode("Double") }
        )
        val joinedNames = flowNodes.allNodes.map { it.first.name }.toString()
        assertTrue('?' !in joinedNames, joinedNames)
    }

    @Test
    fun testSerialization() {
        registerCustomClass(CompareNode())
        val node = CompareNode("Type")
        node.enumType = CompareMode.LESS_OR_EQUALS
        val clone = JsonStringReader.readFirst<CompareNode>(JsonStringWriter.toText(node, InvalidRef), InvalidRef)
        assertEquals(node.toString(), clone.toString())
        assertEquals(node.name, clone.name)
        assertEquals(node.description, clone.description)
    }
}