package me.anno.graph.visual

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.visual.node.Node
import me.anno.graph.visual.node.NodeConnector
import me.anno.graph.visual.node.NodeOutput
import me.anno.graph.visual.render.Texture
import me.anno.graph.visual.render.Texture.Companion.mask
import me.anno.graph.visual.render.Texture.Companion.texOrNull
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import org.apache.logging.log4j.LogManager
import org.joml.Vector4f

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

    // collection of a few helper methods
    fun getFileInput(i: Int): FileReference = getInput(i) as? FileReference ?: InvalidRef
    fun getBoolInput(i: Int): Boolean = getInput(i) == true
    fun getIntInput(i: Int): Int = AnyToInt.getInt(getInput(i), 0)
    fun getLongInput(i: Int): Long = AnyToLong.getLong(getInput(i), 0)
    fun getFloatInput(i: Int): Float = AnyToFloat.getFloat(getInput(i), 0f)
    fun getDoubleInput(i: Int): Double = AnyToDouble.getDouble(getInput(i), 0.0)
    fun getTextureInput(i: Int): ITexture2D? = (getInput(i) as? Texture).texOrNull
    fun getTextureInput(i: Int, defaultValue: ITexture2D): ITexture2D = getTextureInput(i) ?: defaultValue
    fun getTextureInputMask(i: Int): Vector4f = (getInput(i) as? Texture).mask

    fun getNodeOutput(index: Int): NodeOutput = outputs[index]

    override fun supportsMultipleInputs(con: NodeConnector): Boolean = con.type == "Flow"
    override fun supportsMultipleOutputs(con: NodeConnector): Boolean = con.type != "Flow"

    companion object {
        val beforeName = "Before"
        val afterName = "After"
    }
}