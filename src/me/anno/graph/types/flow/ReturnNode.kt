package me.anno.graph.types.flow

import me.anno.graph.NodeOutput
import me.anno.graph.types.FlowGraph
import kotlin.math.max

open class ReturnNode(returnValues: List<String> = emptyList()) :
    FlowGraphNode("Return", flow + returnValues, emptyList()) {

    val values = ArrayList<Any?>(max(4, returnValues.size ushr 1))
    override fun execute(graph: FlowGraph): NodeOutput? {
        values.clear()
        for (i in 1 until inputs!!.size) {
            values.add(getInput(graph, 1))
        }
        throw ReturnException(this) // escape from loops and such
    }

    class ReturnException(val node: ReturnNode): RuntimeException()

    companion object {
        private val flow = listOf("Flow", "Return")
    }

}