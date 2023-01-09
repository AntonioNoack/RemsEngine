package me.anno.graph

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.render.Texture
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ValueNode
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import org.joml.Vector4f

class NodeInput : NodeConnector {

    constructor(isCustom: Boolean) : super(isCustom)
    constructor(type: String, isCustom: Boolean) : super(type, isCustom)
    constructor(type: String, node: Node, isCustom: Boolean) : super(type, node, isCustom)
    constructor(type: String, name: String, node: Node, isCustom: Boolean) : super(type, name, node, isCustom)

    // ofc not thread-safe
    // for multi-thread-operation, copy the graphs
    var lastValidId = -1

    override fun invalidate() {
        super.invalidate()
        lastValidId = -1
    }

    fun castGetValue(graph: FlowGraph, validId: Int): Any? {
        if (validId == lastValidId) return value
        val src = others.firstOrNull()
        val srcNode = src?.node
        if (srcNode is ValueNode) {
            srcNode.compute(graph)
        }
        if (src != null) value = src.value
        // ensure the value is matching
        // todo cast if required
        // todo warn if failed
        lastValidId = validId
        value = when (type) {
            // ensures that the function gets the correct type
            "Int", "Integer" -> AnyToInt.getInt(value, 0, 0)
            "Long" -> AnyToLong.getLong(value, 0, 0)
            "Float" -> AnyToFloat.getFloat(value, 0, 0f)
            "Double" -> AnyToDouble.getDouble(value, 0, 0.0)
            "String" -> value.toString()
            "Any?", "", "?" -> value
            "Boolean" -> when (val v = value) {
                is Collection<*> -> v.isNotEmpty()
                is Array<*> -> v.isNotEmpty()
                is ByteArray -> v.isNotEmpty()
                is ShortArray -> v.isNotEmpty()
                is IntArray -> v.isNotEmpty()
                is LongArray -> v.isNotEmpty()
                is FloatArray -> v.isNotEmpty()
                is DoubleArray -> v.isNotEmpty()
                is Int -> v != 0
                is Long -> v != 0L
                is Float -> v.isFinite() && v != 0f
                is Double -> v.isFinite() && v != 0.0
                is String -> v.isNotEmpty()
                is Boolean -> v
                else -> v != null
            }
            "Texture" -> when (val v = value) {
                is ITexture2D -> Texture(v)
                is Vector4f -> Texture(v)
                is Int -> Texture(v)
                is Float -> Texture(Vector4f(v, v, v, 1f))
                is Texture -> v
                else -> null
            }
            else -> TODO("type $type")
        }
        return value
    }

    override val className get() = "NodeInput"

}