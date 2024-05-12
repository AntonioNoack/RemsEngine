package me.anno.graph.visual

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeInput

// visual coding language
// we enter the graph somewhere, do processing, and then exit some time
open class FlowGraph : Graph() {

    var validId = 0
    var lastInvalidation = 0L

    val localVariables = HashMap<String, Any?>()

    fun requestId(): Int {
        val v = ++validId
        // must be non-negative, negative is invalid
        return if (v < 0) {
            validId = 0
            invalidate()
            return 0
        } else v
    }

    fun invalidate() {
        for (ni in nodes.indices) {
            nodes[ni].invalidateState()
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
}