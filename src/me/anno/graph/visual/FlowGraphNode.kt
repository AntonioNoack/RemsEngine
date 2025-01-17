package me.anno.graph.visual

import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeOutput
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import org.apache.logging.log4j.LogManager

/**
 * Executable node, that redirects execution to the next node, or finishes (null return value)
 * */
abstract class FlowGraphNode(name: String, inputs: List<String>, outputs: List<String>) :
    Node(name, inputs, outputs) {

    /**
     * Return, which node shall be executed next;
     * Returns null, if execution is finished
     * */
    abstract fun execute(): NodeOutput?

    open fun continueExecution(state: Saveable?): NodeOutput? {
        LOGGER.warn("If you use the graph-stack, you must implement execute(state)")
        return null
    }

    fun requestNextExecution(state: Saveable?) {
        (graph as? FlowGraph)?.push(this, state)
    }

    // collection of a few helper methods
    fun getFileInput(i: Int): FileReference = getInput(i) as? FileReference ?: InvalidRef
    fun getBoolInput(i: Int): Boolean = getInput(i) == true
    fun getIntInput(i: Int): Int = AnyToInt.getInt(getInput(i), 0)
    fun getLongInput(i: Int): Long = AnyToLong.getLong(getInput(i), 0)
    fun getFloatInput(i: Int): Float = AnyToFloat.getFloat(getInput(i), 0f)
    fun getDoubleInput(i: Int): Double = AnyToDouble.getDouble(getInput(i), 0.0)

    override fun supportsMultipleInputs(con: NodeConnector): Boolean = con.type == "Flow"
    override fun supportsMultipleOutputs(con: NodeConnector): Boolean = con.type != "Flow"

    companion object {
        private val LOGGER = LogManager.getLogger(FlowGraphNode::class)
        val beforeName = "Before"
        val afterName = "After"
    }
}