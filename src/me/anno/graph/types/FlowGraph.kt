package me.anno.graph.types

import me.anno.graph.Graph
import me.anno.graph.Node
import me.anno.graph.NodeConnector
import me.anno.graph.NodeInput
import me.anno.graph.types.flow.FlowGraphNode

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

    fun executeThenGetOutputs(input: Node): List<Any?> {
        val node = execute(input)
        return node.outputs.map { it.currValue }
    }

    fun executeThenGetOutput(input: Node): Any? {
        val node = execute(input)
        return node.outputs.firstOrNull()?.currValue
    }

    fun getValue(input: NodeInput): Any? {
        return input.getValue()
    }

    override val className: String get() = "FlowGraph"
}