package me.anno.graph.visual

import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.compiler.GraphCompiler
import me.anno.graph.visual.render.compiler.GLSLFlowNode
import kotlin.math.max

open class ReturnNode(returnValues: List<String> = emptyList(), name: String = "Return") :
    FlowGraphNode(name, inputBase + returnValues, emptyList()), GLSLFlowNode {

    constructor(name: String) : this(emptyList(), name)

    val values = ArrayList<Any?>(max(4, returnValues.size ushr 1))
    override fun execute(): NodeOutput? {
        values.clear()
        for (i in 1 until inputs.size) {
            values.add(getInput(1))
        }
        throw ReturnThrowable(this) // escape from loops and such
    }

    class ReturnThrowable(val node: ReturnNode) : Throwable()

    override fun buildCode(g: GraphCompiler, depth: Int): Boolean {
        g.handleReturnNode(this)
        return false
    }

    companion object {
        @JvmStatic
        private val inputBase = listOf("Flow", "Return")
    }
}