package me.anno.graph.visual

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeInput
import me.anno.io.saveable.Saveable
import me.anno.utils.structures.lists.PairArrayList

/**
 * visual coding language;
 * we enter the graph somewhere, do processing, and then exit some time
 * */
open class FlowGraph : Graph() {

    var validId = 0
    var lastInvalidation = 0L

    val localVariables = HashMap<String, Any?>()

    val nodeStack = PairArrayList<FlowGraphNode, Saveable?>()

    fun push(node: FlowGraphNode, state: Saveable?) {
        nodeStack.add(node, state)
    }

    fun invalidate(): Int {
        val v = ++validId
        // must be non-negative, negative is invalid
        return if (v < 0) {
            validId = 0
            invalidateSlow()
            return 0
        } else v
    }

    private fun invalidateSlow() {
        for (ni in nodes.indices) {
            nodes[ni].invalidateState()
        }
    }

    /**
     * returns the last node, which was executed
     * */
    fun execute(startNode0: FlowGraphNode): Node {
        val nodeStackPtr = nodeStack.size
        var lastNode = executeLinearlyUntilDone(startNode0)
        while (nodeStack.size > nodeStackPtr) {
            val node = nodeStack.lastFirst()
            val state = nodeStack.lastSecond()
            nodeStack.removeLast(true)
            invalidate() // invalidate graph in O(1)
            val lastNodeI = node.continueExecution(state)
            lastNode = if (lastNodeI != null) {
                executeNodes(lastNodeI.others) ?: node
            } else node
        }
        return lastNode
    }

    private fun executeLinearlyUntilDone(startNode0: Node): Node {
        var currentNode = startNode0
        while (currentNode is FlowGraphNode) {
            val exec = currentNode.execute() ?: return currentNode
            val nextNodes = exec.others
            val firstNext = nextNodes.firstOrNull()?.node
            currentNode = if (nextNodes.size == 1 && firstNext != null) {
                // non-recursive path to keep the stack trace flatter
                firstNext
            } else {
                // recursion needed
                executeNodes(nextNodes)
                    ?: return currentNode
            }
        }
        return currentNode
    }

    fun executeNodes(nextNodes: List<NodeConnector>): Node? {
        var lastNode: Node? = null
        for (i in nextNodes.indices) {
            val nodeX = nextNodes[i].node as? FlowGraphNode ?: continue
            lastNode = execute(nodeX)
        }
        return lastNode
    }

    fun getValue(input: NodeInput): Any? {
        return input.getValue()
    }
}