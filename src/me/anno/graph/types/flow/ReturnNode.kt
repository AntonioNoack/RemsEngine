package me.anno.graph.types.flow

import me.anno.graph.NodeOutput
import kotlin.math.max

open class ReturnNode(returnValues: List<String> = emptyList(), name: String = "Return") :
    FlowGraphNode(name, flow + returnValues, emptyList()) {

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
        private val flow = listOf("Flow", "Return")
    }

}