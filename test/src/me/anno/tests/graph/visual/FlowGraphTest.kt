package me.anno.tests.graph.visual

import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.FlowGraphNode
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.control.ForNode
import me.anno.graph.visual.local.GetLocalVariableNode
import me.anno.graph.visual.local.SetLocalVariableNode
import me.anno.graph.visual.scalar.FloatMathBinary
import me.anno.graph.visual.scalar.IntMathBinary
import me.anno.graph.visual.scalar.MathF2Node
import me.anno.graph.visual.scalar.MathI2Node
import me.anno.maths.Maths.factorial
import me.anno.utils.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

object FlowGraphTest {

    @Test
    fun testSimpleCalculation() {
        val g = FlowGraph()
        val n0 = MathF2Node().setDataType("Double").setEnumType(FloatMathBinary.ADD)
        val n1 = MathF2Node().setDataType("Double").setEnumType(FloatMathBinary.DIV)
        n0.connectTo(n1, 0)
        n0.setInputs(listOf(1.0, 2.0))
        n1.setInput(1, 2.0)
        g.addAll(n0, n1)
        assertEquals(1.5, g.execute(n1).getOutput(0))
    }

    class RecordingPrintNode(val recording: ArrayList<String>) :
        ActionNode("Print", listOf("Any?", "Printed", "Any?", "Printed"), emptyList()) {
        override fun executeAction() {
            recording.add(getInput(1).toString())
            recording.add(getInput(2)?.toString() ?: return)
        }
    }

    @Test
    fun testLoop() {
        testLoop(
            listOf(null, 0L, 5L, 1L),
            listOf("0", "1", "2", "3", "4", "Done")
        )
    }

    @Test
    fun testLoopEndInclusive() {
        testLoop(
            listOf(null, 0L, 5L, 1L, true),
            listOf("0", "1", "2", "3", "4", "5", "Done")
        )
    }

    @Test
    fun testLoopReverse() {
        testLoop(
            listOf(null, 5L, 0L, -1L),
            listOf("5", "4", "3", "2", "1", "Done")
        )
    }

    @Test
    fun testLoopReverseEndIncl() {
        testLoop(
            listOf(null, 5L, 0L, -1L, true),
            listOf("5", "4", "3", "2", "1", "0", "Done")
        )
    }

    @Test
    fun testLoop2D() {
        testLoop2D(
            listOf(null, 0L, 2L, 1L),
            listOf(null, 0L, 3L, 1L),
            listOf(
                "0", "0",
                "0", "1",
                "0", "2",
                "1", "0",
                "1", "1",
                "1", "2",
                "Done"
            ),
        )
    }

    fun testLoop(inputs: List<Any?>, expectedOutputs: List<String>) {
        val recording = ArrayList<String>()
        val g = FlowGraph()
        val forNode = ForNode()
        forNode.setInputs(inputs)
        val printNode = RecordingPrintNode(recording)
        forNode.connectTo(0, printNode, 0)
        forNode.connectTo(1, printNode, 1)
        val endNode = RecordingPrintNode(recording)
        endNode.setInputs(listOf(null, "Done"))
        forNode.connectTo(2, endNode, 0)
        g.addAll(forNode, printNode, endNode)
        g.execute(forNode)
        assertEquals(expectedOutputs, recording)
    }

    fun testLoop2D(inputs0: List<Any?>, inputs1: List<Any?>, expectedOutputs: List<String>) {
        val recording = ArrayList<String>()
        val g = FlowGraph()
        val forNode0 = ForNode()
        val forNode1 = ForNode()
        forNode0.setInputs(inputs0)
        forNode1.setInputs(inputs1)
        val printNode = RecordingPrintNode(recording)
        forNode0.connectTo(0, forNode1, 0)
        forNode0.connectTo(1, printNode, 1)
        forNode1.connectTo(0, printNode, 0)
        forNode1.connectTo(1, printNode, 2)
        val endNode = RecordingPrintNode(recording)
        endNode.setInputs(listOf(null, "Done"))
        forNode0.connectTo(2, endNode, 0)
        g.addAll(forNode0, forNode1, printNode, endNode)
        g.execute(forNode0)
        assertEquals(expectedOutputs, recording)
    }

    @Test
    fun testLocalVariablesByCalculatingFactorial() {
        val (g, initNode) = createFactorialGraph(4)
        g.execute(initNode)
        g.invalidate()
        assertEquals(4L.factorial(), g.localVariables["var"])
    }

    @Test
    fun testIsNonRecursive() {
        // a recursive function hopefully would crash from a stackoverflow
        val (g, initNode) = createFactorialGraph(100_000)
        g.execute(initNode)
        assertEquals(100_000L.factorial(), g.localVariables["var"])
    }

    fun createFactorialGraph(n: Int): Pair<FlowGraph, FlowGraphNode> {
        val g = FlowGraph()
        val initNode = SetLocalVariableNode("var", 1)
        initNode.color = Color.black or 0x112233
        val forNode = ForNode()
        forNode.setInputs(listOf(null, 1L, 1L + n, 1L)) // flow, start, end, step
        initNode.connectTo(forNode)
        val mulNode = MathI2Node().setDataType("Long").setEnumType(IntMathBinary.MUL)
        val getNode = GetLocalVariableNode("var", "?")
        forNode.connectTo(1, mulNode, 0)
        getNode.connectTo(0, mulNode, 1)
        val setNode = SetLocalVariableNode("var", null)
        forNode.connectTo(setNode)
        mulNode.connectTo(0, setNode, 2)
        g.addAll(initNode, forNode, mulNode, getNode, setNode)
        return Pair(g, initNode)
    }
}