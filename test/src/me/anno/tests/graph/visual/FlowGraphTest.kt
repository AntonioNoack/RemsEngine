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

    @Test
    fun testLoopByPseudoPrinting() {
        val recording = ArrayList<String>()

        class RecordingPrintNode : ActionNode("Print", listOf("Any?", "Printed"), emptyList()) {
            override fun executeAction() {
                recording.add(inputs[1].getValue().toString())
            }
        }

        val g = FlowGraph()
        val forNode = ForNode()
        forNode.setInputs(listOf(null, 0L, 5L, 1L))
        val printNode = RecordingPrintNode()
        forNode.connectTo(0, printNode, 0)
        forNode.connectTo(1, printNode, 1)
        val endNode = RecordingPrintNode()
        endNode.setInputs(listOf(null, "Done"))
        forNode.connectTo(2, endNode, 0)
        g.addAll(forNode, printNode, endNode)
        g.execute(forNode)
        assertEquals(listOf("0", "1", "2", "3", "4", "Done"), recording)
    }

    @Test
    fun testLocalVariablesByCalculatingFactorial() {
        val (g, initNode) = createFactorialGraph(4)
        g.execute(initNode)
        g.requestId()
        assertEquals(4L.factorial(), g.localVariables["var"])
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