package me.anno.graph.visual

import me.anno.graph.visual.node.NodeOutput
import kotlin.math.max

open class ReturnNode(returnValues: List<String> = emptyList(), name: String = "Return") :
    FlowGraphNode(name, inputBase + returnValues, emptyList()) {

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

    companion object {
        @JvmStatic
        private val inputBase = listOf("Flow", "Return")
    }
}