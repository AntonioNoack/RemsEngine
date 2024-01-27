package me.anno.graph

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.graph.render.Texture
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.ComputeNode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.Color.black2
import me.anno.utils.Color.black3
import me.anno.utils.Color.black4
import me.anno.utils.Color.white2
import me.anno.utils.Color.white3
import me.anno.utils.Color.white4
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class NodeInput : NodeConnector {

    constructor() : super(false)
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

    fun validate(value: Any?, graph: FlowGraph) {
        lastValidId = graph.validId
        currValue = value
    }

    fun getValue(): Any? {
        val graph = node?.graph as? FlowGraph
        if (graph != null && graph.validId == lastValidId) return currValue
        val src = others.firstOrNull()
        val srcNode = src?.node
        if (srcNode is ComputeNode) {
            srcNode.compute()
        }
        if (src != null) currValue = src.currValue
        // ensure the value is matching
        // todo cast if required
        // todo warn if failed
        if (graph != null) lastValidId = graph.validId
        val value = currValue
        currValue = when (type) {
            // ensures that the function gets the correct type
            "Int", "Integer" -> AnyToInt.getInt(value, 0, 0)
            "Long" -> AnyToLong.getLong(value, 0, 0)
            "Float" -> AnyToFloat.getFloat(value, 0, 0f)
            "Double" -> AnyToDouble.getDouble(value, 0, 0.0)
            "Vector2f" -> when (value) {
                true -> white2
                false -> black2
                is Int -> Vector2f(value.toFloat())
                is Float -> Vector2f(value)
                is Vector2f -> value
                is Vector3f -> Vector2f(value.x, value.y)
                is Vector4f -> Vector2f(value.x, value.y)
                else -> Vector2f(0f)
            }
            "Vector3f" -> when (value) {
                true -> white3
                false -> black3
                is Int -> Vector3f(value.toFloat())
                is Float -> Vector3f(value)
                is Vector2f -> Vector3f(value.x, value.y, 0f)
                is Vector3f -> value
                is Vector4f -> Vector3f(value.x, value.y, value.z)
                else -> Vector3f(0f)
            }
            "Vector4f" -> when (value) {
                true -> white4
                false -> black4
                is Int -> Vector4f(value.toFloat())
                is Float -> Vector4f(value)
                is Vector2f -> Vector4f(value.x, value.y, 0f, 0f)
                is Vector3f -> Vector4f(value.x, value.y, value.z, 0f)
                is Vector4f -> value
                else -> Vector4f(0f)
            }
            "String" -> value?.toString() ?: ""
            "Any?", "", "?" -> value
            "Boolean", "Bool" -> when (val v = value) {
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
            "Texture" -> when (value) {
                is Float -> Texture(Vector4f(value, value, value, 1f))
                is Vector2f -> Texture(Vector4f(value, 0f, 1f))
                is Vector3f -> Texture(Vector4f(value, 1f))
                is Vector4f -> Texture(value)
                is Texture -> value
                is ITexture2D -> Texture(value)
                else -> null
            }.run { if (this != null && isDestroyed) null else this }
            "ITexture2D" -> when (value) {
                is Texture -> value.tex
                is ITexture2D -> value
                else -> null
            }.run { if (this is Texture2D && isDestroyed) null else this }
            "Texture2D" -> when (value) {
                is Texture -> value.tex as? Texture2D
                is Texture2D -> value
                else -> null
            }.run { if (this != null && isDestroyed) null else this }
            "Texture3D" -> value as? Texture3D
            "File", "FileReference" -> value as? FileReference ?: InvalidRef
            "IntArray" -> value as? IntArray
            "LongArray" -> value as? LongArray
            "FloatArray" -> value as? FloatArray
            "DoubleArray" -> value as? DoubleArray
            else -> {
                if (type.startsWith("List<")) {
                    value as? List<*> ?: emptyList<Any>()
                } else if (type.startsWith("Enum<")) {
                    value as? Enum<*>
                } else throw NotImplementedError("type $type needs to be implemented")
            }
        }
        return currValue
    }
}