package me.anno.tests.graph

import me.anno.engine.OfficialExtensions
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.function.FunctionGraphUtils
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class FunctionGraphUtilsTest {

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun signatureKeepsFlowPins() {
        val g = FlowGraph()
        g.addAll(
            StartNode(listOf("Int", "A")),
            ReturnNode(listOf("Float", "B")),
        )

        val sig = FunctionGraphUtils.getSignature(g)
        assertEquals(listOf("Flow" to "Start", "Int" to "A"), sig.args)
        assertEquals(listOf("Flow" to "Return", "Float" to "B"), sig.returns)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun rebuildKeepsConnectionsWhenTypeChanges() {
        val a = object : Node("A", emptyList(), emptyList()) {}
        val b = object : Node("B", emptyList(), emptyList()) {}

        // old input/output ports
        val in0 = NodeInput("Int", "x", a, false)
        val out0 = NodeOutput("Float", "y", b, false)
        a.inputs.add(in0)
        b.outputs.add(out0)
        out0.connect(in0)

        // change input signature to Float, should keep connection
        FunctionGraphUtils.rebuildInputs(a, listOf("Float" to "x"))

        assertEquals(1, a.inputs.size)
        assertEquals("Float", a.inputs[0].type)
        assertTrue(out0 in a.inputs[0].others)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun rebuildDisconnectsWhenFlowNessChanges() {
        val a = object : Node("A", emptyList(), emptyList()) {}
        val b = object : Node("B", emptyList(), emptyList()) {}

        val in0 = NodeInput("Int", "x", a, false)
        val out0 = NodeOutput("Float", "y", b, false)
        a.inputs.add(in0)
        b.outputs.add(out0)
        out0.connect(in0)

        // change to Flow input => should disconnect
        FunctionGraphUtils.rebuildInputs(a, listOf("Flow" to "Start"))

        assertEquals("Flow", a.inputs[0].type)
        assertEquals(0, a.inputs[0].others.size)
        assertTrue(in0 !in out0.others)
    }
}

