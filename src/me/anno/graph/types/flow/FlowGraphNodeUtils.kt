package me.anno.graph.types.flow

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

/**
 * if you feel like your node-classes look dirty with "as X", use this ;)
 * */
object FlowGraphNodeUtils {
    @JvmStatic
    fun FlowGraphNode.getFileInput(i: Int): FileReference = getInput(i) as? FileReference ?: InvalidRef

    @JvmStatic
    fun FlowGraphNode.getBoolInput(i: Int): Boolean = getInput(i) == true

    @JvmStatic
    fun FlowGraphNode.getIntInput(i: Int): Int = getInput(i) as Int

    @JvmStatic
    fun FlowGraphNode.getFloatInput(i: Int): Float = getInput(i) as Float
}