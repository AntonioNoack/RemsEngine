package me.anno.tests.graph.visual

import me.anno.graph.visual.vector.RotateF2Node
import me.anno.graph.visual.vector.RotateF3XNode
import me.anno.graph.visual.vector.RotateF3YNode
import me.anno.graph.visual.vector.RotateF3ZNode
import me.anno.utils.assertions.assertEquals
import org.joml.Vector2f
import org.joml.Vector3f
import org.junit.jupiter.api.Test

class RotateNodeTest {
    @Test
    fun testRotateF2() {
        val node = RotateF2Node()
        node.setInputs(listOf(Vector2f(1f, 2f), Vector2f(2f, 5f), 5f))
        node.compute()
        val baseline = Vector2f(1f, 2f)
        val center = Vector2f(2f, 5f)
        baseline.sub(center).rotate(5f).add(center)
        assertEquals(baseline, node.getOutput(0))
    }

    @Test
    fun testRotateF3X() {
        val node = RotateF3XNode()
        node.setInputs(listOf(Vector3f(1f, 2f, 3f), Vector3f(2f, 5f, -3f), 5f))
        node.compute()
        val baseline = Vector3f(1f, 2f, 3f)
        val center = Vector3f(2f, 5f, -3f)
        baseline.sub(center).rotateX(5f).add(center)
        assertEquals(baseline, node.getOutput(0))
    }

    @Test
    fun testRotateF3Y() {
        val node = RotateF3YNode()
        node.setInputs(listOf(Vector3f(1f, 2f, 3f), Vector3f(2f, 5f, -3f), 5f))
        node.compute()
        val baseline = Vector3f(1f, 2f, 3f)
        val center = Vector3f(2f, 5f, -3f)
        baseline.sub(center).rotateY(5f).add(center)
        assertEquals(baseline, node.getOutput(0))
    }

    @Test
    fun testRotateF3Z() {
        val node = RotateF3ZNode()
        node.setInputs(listOf(Vector3f(1f, 2f, 3f), Vector3f(2f, 5f, -3f), 5f))
        node.compute()
        val baseline = Vector3f(1f, 2f, 3f)
        val center = Vector3f(2f, 5f, -3f)
        baseline.sub(center).rotateZ(5f).add(center)
        assertEquals(baseline, node.getOutput(0))
    }
}