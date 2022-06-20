package me.anno.graph.types

import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.flow.FlowGraphNode
import me.anno.graph.types.flow.actions.PrintNode
import me.anno.graph.types.flow.control.ForNode
import me.anno.graph.types.flow.local.GetLocalVariableNode
import me.anno.graph.types.flow.local.SetLocalVariableNode
import me.anno.graph.types.flow.maths.FloatMathsBinary
import me.anno.graph.types.flow.maths.IntMathType
import me.anno.graph.types.flow.maths.MathD2Node
import me.anno.graph.types.flow.maths.MathL2Node
import org.apache.logging.log4j.LogManager

// visual coding language
// we enter the graph somewhere, do processing, and then exit some time
open class FlowGraph : Graph() {

    // todo probably nodes should be able to compute their values
    // todo maybe we need to evaluate nodes every time: internal values may have changed

    var validId = 0

    // todo define them local variables...
    var localVariables = HashMap<String, Any?>()

    fun requestId(): Int {
        val v = ++validId
        // must be non-negative, negative is invalid
        return if (v < 0) {
            validId = 0
            invalidate()
            return 0
        } else v
    }

    // shouldn't really be required...
    fun resetLocalVariables() {
        localVariables.clear()
    }

    fun invalidate() {
        for (node in nodes) {
            for (input in node.inputs ?: continue) {
                input.lastValidId = -1
            }
        }
    }

    // returns the last node, which was executed
    fun execute(startNode: Node): Node {
        // LOGGER.debug("Execute ${startNode.className}")
        startNode as FlowGraphNode
        val nextNodes = startNode.execute(this)?.others?.mapNotNull { it.node } ?: return startNode
        // LOGGER.debug("${startNode.className} -> ${nextNodes.map { it.className }}")
        var lastNode = startNode
        for (nodeI in nextNodes) {
            if (nodeI == nextNodes) penalty()
            lastNode = execute(nodeI)
        }
        return lastNode
    }

    fun penalty() {
        Thread.sleep(5000) // penalty for this infinity loop
    }

    fun executeConnectors(inputs: List<NodeConnector>) {
        for (input in inputs) {
            execute(input.node ?: continue)
        }
    }

    fun executeNodes(inputs: List<Node>) {
        for (input in inputs) {
            execute(input)
        }
    }

    fun computeNode(input: Node): List<Any?> {
        val node = execute(input)
        return node.outputs!!.map { it.value }
    }

    fun computeNode1(input: Node): Any? {
        val node = execute(input)
        return node.outputs!!.firstOrNull()?.value
    }

    fun getValue(input: NodeInput): Any? {
        return input.castGetValue(this, validId)
    }

    override val className = "FlowGraph"

    companion object {

        private val LOGGER = LogManager.getLogger(FlowGraph::class)

        fun testCalculation(): FlowGraph {
            val g = FlowGraph()
            val n0 = MathD2Node(FloatMathsBinary.ADD)
            val n1 = MathD2Node(FloatMathsBinary.DIV)
            n0.connectTo(n1, 0)
            n0.setInputs(listOf(1.0, 2.0))
            n1.setInput(1, 2.0)
            LOGGER.info(g.computeNode1(n1))
            return g
        }

        fun testLoopPrint(): FlowGraph {
            val g = FlowGraph()
            val forNode = ForNode()
            forNode.setInputs(listOf(null, 0L, 5L, 1L))
            val printNode = PrintNode()
            forNode.connectTo(0, printNode, 0)
            forNode.connectTo(1, printNode, 1)
            val endNode = PrintNode()
            endNode.setInputs(listOf(null, "Done"))
            forNode.connectTo(2, endNode, 0)
            g.execute(forNode)
            return g
        }

        fun testLocalVariables(): FlowGraph {
            // calculate factorial
            val g = FlowGraph()
            val initNode = SetLocalVariableNode("var", 1)
            val forNode = ForNode()
            forNode.setInputs(listOf(null, 1L, 5L, 1L))
            initNode.connectTo(forNode)
            val mulNode = MathL2Node(IntMathType.MUL)
            val getNode = GetLocalVariableNode("var")
            forNode.connectTo(1, mulNode, 0)
            getNode.connectTo(0, mulNode, 1)
            val setNode = SetLocalVariableNode("var", null)
            forNode.connectTo(setNode)
            mulNode.connectTo(0, setNode, 2)
            g.execute(initNode)
            g.requestId()
            g.addAll(
                listOf(
                    initNode,
                    forNode,
                    mulNode,
                    getNode,
                    setNode
                )
            )
            LOGGER.info(g.localVariables["var"])
            return g
        }

        @JvmStatic
        fun main(args: Array<String>) {
            testCalculation()
            testLoopPrint()
            testLocalVariables()
        }
    }

}