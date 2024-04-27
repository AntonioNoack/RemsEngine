package me.anno.tests.graph

import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.FloatMathsBinary
import me.anno.graph.types.flow.maths.IntMathsBinary
import me.anno.graph.types.flow.maths.MathD2Node
import me.anno.graph.types.flow.maths.MathL2Node
import me.anno.utils.Color
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

object FlowGraphTest {

    @Test
    fun testSimpleCalculation() {
        val g = FlowGraph()
        val n0 = MathD2Node().apply { type = FloatMathsBinary.ADD }
        val n1 = MathD2Node().apply { type = FloatMathsBinary.DIV }
        n0.connectTo(n1, 0)
        n0.setInputs(listOf(1.0, 2.0))
        n1.setInput(1, 2.0)
        g.addAll(n0, n1)
        assertEquals(1.5, g.executeThenGetOutput(n1))
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
        val g = FlowGraph()
        val initNode = SetLocalVariableNode("var", 1)
        initNode.color = Color.black or 0x112233
        val forNode = ForNode()
        forNode.setInputs(listOf(null, 1L, 5L, 1L))
        initNode.connectTo(forNode)
        val mulNode = MathL2Node()
        mulNode.type = IntMathsBinary.MUL
        val getNode = GetLocalVariableNode("var", "?")
        forNode.connectTo(1, mulNode, 0)
        getNode.connectTo(0, mulNode, 1)
        val setNode = SetLocalVariableNode("var", null)
        forNode.connectTo(setNode)
        mulNode.connectTo(0, setNode, 2)
        g.addAll(
            initNode,
            forNode,
            mulNode,
            getNode,
            setNode
        )
        g.execute(initNode)
        g.requestId()
        assertEquals(24L, g.localVariables["var"])
    }
}