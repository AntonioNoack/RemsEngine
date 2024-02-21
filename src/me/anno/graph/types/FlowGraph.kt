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
import me.anno.graph.types.flow.maths.IntMathsBinary
import me.anno.graph.types.flow.maths.MathD2Node
import me.anno.graph.types.flow.maths.MathL2Node
import me.anno.utils.Color.black
import org.apache.logging.log4j.LogManager

// visual coding language
// we enter the graph somewhere, do processing, and then exit some time
open class FlowGraph : Graph() {

    // todo probably nodes should be able to compute their values
    // todo maybe we need to evaluate nodes every time: internal values may have changed

    var validId = 0
    var lastInvalidation = 0L

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
        for (ni in nodes.indices) {
            val node = nodes[ni]
            val inputs = node.inputs
            for (ii in inputs.indices) {
                inputs[ii].lastValidId = -1
            }
        }
    }

    /**
     * returns the last node, which was executed
     * */
    fun execute(startNode0: Node): Node {
        var currentNode = startNode0
        while (true) {
            currentNode as FlowGraphNode
            val exec = currentNode.execute()
            val nextNodes = exec?.others ?: return currentNode
            val firstNext = nextNodes.firstOrNull()?.node
            if (nextNodes.size == 1 && firstNext != null) {
                // non-recursive path to keep the stack trace flatter
                currentNode = firstNext
            } else {
                // recursion needed
                var lastNode = currentNode
                for (i in nextNodes.indices) {
                    val nodeX = nextNodes[i].node ?: continue
                    lastNode = execute(nodeX)
                }
                return lastNode
            }
        }
    }

    fun executeConnectors(inputs: List<NodeConnector>) {
        for (i in inputs.indices) {
            val node = inputs[i].node ?: continue
            execute(node)
        }
    }

    fun executeNodes(inputs: List<Node>) {
        for (i in inputs.indices) {
            execute(inputs[i])
        }
    }

    fun computeNode(input: Node): List<Any?> {
        val node = execute(input)
        return node.outputs.map { it.currValue }
    }

    fun computeNode1(input: Node): Any? {
        val node = execute(input)
        return node.outputs.firstOrNull()?.currValue
    }

    fun getValue(input: NodeInput): Any? {
        return input.getValue()
    }

    override val className: String get() = "FlowGraph"

    companion object {

        @JvmStatic
        private val LOGGER = LogManager.getLogger(FlowGraph::class)

        @JvmStatic
        fun testCalculation(): FlowGraph {
            val g = FlowGraph()
            val n0 = MathD2Node().apply { type = FloatMathsBinary.ADD }
            val n1 = MathD2Node().apply { type = FloatMathsBinary.DIV }
            n0.connectTo(n1, 0)
            n0.setInputs(listOf(1.0, 2.0))
            n1.setInput(1, 2.0)
            g.addAll(n0, n1)
            LOGGER.info(g.computeNode1(n1))
            return g
        }

        @JvmStatic
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
            g.addAll(forNode, printNode, endNode)
            g.execute(forNode)
            return g
        }

        @JvmStatic
        fun testLocalVariables(): FlowGraph {
            // calculate factorial
            val g = FlowGraph()
            val initNode = SetLocalVariableNode("var", 1)
            initNode.color = black or 0x112233
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
            LOGGER.info(g.localVariables["var"])
            return g
        }
    }
}