package me.anno.graph.visual

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong

/**
 * if you feel like your node-classes look dirty with "as X", use this ;)
 * */
object FlowGraphNodeUtils {
    @JvmStatic
    fun FlowGraphNode.getFileInput(i: Int): FileReference = getInput(i) as? FileReference ?: InvalidRef

    @JvmStatic
    fun FlowGraphNode.getBoolInput(i: Int): Boolean = getInput(i) == true

    @JvmStatic
    fun FlowGraphNode.getIntInput(i: Int): Int = AnyToInt.getInt(getInput(i), 0)

    @JvmStatic
    fun FlowGraphNode.getLongInput(i: Int): Long = AnyToLong.getLong(getInput(i), 0)

    @JvmStatic
    fun FlowGraphNode.getFloatInput(i: Int): Float = AnyToFloat.getFloat(getInput(i), 0f)

    @JvmStatic
    fun FlowGraphNode.getDoubleInput(i: Int): Double = AnyToDouble.getDouble(getInput(i), 0.0)
}