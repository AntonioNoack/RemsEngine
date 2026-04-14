package me.anno.tests.graph.visual

import me.anno.engine.OfficialExtensions
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.ReturnNode
import me.anno.graph.visual.StartNode
import me.anno.graph.visual.function.ActionGraphNode
import me.anno.graph.visual.function.ExpressionGraphNode
import me.anno.graph.visual.function.FunctionGraphUtils
import me.anno.graph.visual.function.MacroGraphNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class FunctionGraphNodesTest {

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

    private fun createIdentityCalleeFile(): FileReference {
        val callee = FlowGraph()
        val start = StartNode(listOf("Int", "A"))
        val ret = ReturnNode(listOf("Int", "B"))
        start.outputs[0].connect(ret.inputs[0]) // flow
        start.outputs[1].connect(ret.inputs[1]) // value
        callee.addAll(start, ret)
        val tmp = FileFileRef.createTempFile("callee", "json")
        tmp.writeText(JsonStringWriter.toText(callee, InvalidRef))
        return tmp
    }

    private fun ensureTemplateUpToDate(node: Any) {
        val method = node.javaClass.getDeclaredMethod("ensureTemplateUpToDate")
        method.isAccessible = true
        method.invoke(node)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun actionGraphNodeExecutesCallee() {
        val calleeFile = createIdentityCalleeFile()
        val node = ActionGraphNode()
        node.file = calleeFile
        ensureTemplateUpToDate(node)

        val caller = FlowGraph()
        val start = StartNode(listOf("Int", "X"))
        val ret = ReturnNode(listOf("Int", "Y"))

        // wire flow
        start.outputs[0].connect(node.inputs[0])
        node.outputs[0].connect(ret.inputs[0])
        // wire data
        start.outputs[1].connect(node.inputs[1])
        node.outputs[1].connect(ret.inputs[1])

        caller.addAll(start, node, ret)
        start.setOutput(1, 7)
        caller.execute(start)

        assertEquals(7, ret.getInput(1))
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun macroGraphNodeExecutesCallee() {
        val calleeFile = createIdentityCalleeFile()
        val node = MacroGraphNode()
        node.file = calleeFile
        ensureTemplateUpToDate(node)

        val caller = FlowGraph()
        val start = StartNode(listOf("Int", "X"))
        val ret = ReturnNode(listOf("Int", "Y"))

        // wire flow
        start.outputs[0].connect(node.inputs[0])
        node.outputs[0].connect(ret.inputs[0])
        // wire data
        start.outputs[1].connect(node.inputs[1])
        node.outputs[1].connect(ret.inputs[1])

        caller.addAll(start, node, ret)
        start.setOutput(1, 9)
        caller.execute(start)

        assertEquals(9, ret.getInput(1))
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun expressionGraphNodeExecutesCallee() {
        val calleeFile = createIdentityCalleeFile()
        val node = ExpressionGraphNode()
        node.file = calleeFile
        ensureTemplateUpToDate(node)

        node.setInput(0, 11)
        assertEquals(11, node.calculate())
    }
}

