package me.anno.graph.visual

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.visual.control.RecursiveFlowGraphNode
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
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

    private val nodeStack = PairArrayList<FlowGraphNode, Saveable?>()

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
     * returns the last node, which was executed;
     * returns a ReturnNode | NewStateNode, if that is found
     * */
    fun execute(startNode0: FlowGraphNode): Node {
        val depth = nodeStack.size
        push(startNode0, null)
        return executeNodesUntilDepth(depth) ?: startNode0
    }

    private fun executeNodesUntilDepth(depth: Int): Node? {
        var lastNode: Node? = null
        while (nodeStack.size > depth) {

            val node = nodeStack.lastFirst()
            val state = nodeStack.lastSecond()
            nodeStack.removeLast()

            val expectedStackSize = nodeStack.size

            lastNode = node
            val nodeOutput = if (node is RecursiveFlowGraphNode<*> && state != null) {
                node.continueExecutionUnsafe(state)
            } else node.execute()
            if (node is EarlyExitNode) break

            if (nodeStack.size != expectedStackSize) {
                // invalidation is only necessary, if things were pushed
                invalidate() // invalidate graph in O(1)
            }

            val nextNodes = nodeOutput?.others
            if (nextNodes != null) enqueue(nextNodes)
        }
        clearStack(depth)
        return lastNode
    }

    private fun clearStack(depth: Int) {
        nodeStack.elementSize = depth * 2
    }

    private fun enqueue(nextNodes: List<NodeConnector>) {
        for (ni in nextNodes.indices.reversed()) {
            val nextNode = nextNodes[ni].node as? FlowGraphNode ?: continue
            push(nextNode, null)
        }
    }

    fun executeNodes(nextNodes: List<NodeConnector>): Node? {
        val depth = nodeStack.size
        enqueue(nextNodes)
        return executeNodesUntilDepth(depth)
    }

    override fun getValidTypesForChild(child: PrefabSaveable) = "n"
    override fun getChildListByType(type: Char) = nodes
    override fun getChildListNiceName(type: Char): String = "Nodes"
}