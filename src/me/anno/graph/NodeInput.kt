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
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class NodeInput : NodeConnector {

    companion object {

        private val LOGGER = LogManager.getLogger(NodeInput::class)

        // ensure the value is matching
        //  - cast if required
        //  - warn if failed?
        private val typeValidators = HashMap<String, (Any?) -> Any?>()
        fun registerType(name: String, validator: (Any?) -> Any?) {
            typeValidators[name] = validator
        }

        init {
            registerType("Int") { AnyToInt.getInt(it, 0, 0) }
            registerType("Long") { AnyToLong.getLong(it, 0, 0L) }
            registerType("Float") { AnyToFloat.getFloat(it, 0, 0f) }
            registerType("Double") { AnyToDouble.getDouble(it, 0, 0.0) }
            registerType("Vector2f") { v ->
                when (v) {
                    true -> white2
                    false -> black2
                    is Int -> Vector2f(v.toFloat())
                    is Float -> Vector2f(v)
                    is Vector2f -> v
                    is Vector3f -> Vector2f(v.x, v.y)
                    is Vector4f -> Vector2f(v.x, v.y)
                    else -> Vector2f(0f)
                }
            }
            registerType("Vector3f") { v ->
                when (v) {
                    true -> white3
                    false -> black3
                    is Int -> Vector3f(v.toFloat())
                    is Float -> Vector3f(v)
                    is Vector2f -> Vector3f(v.x, v.y, 0f)
                    is Vector3f -> v
                    is Vector4f -> Vector3f(v.x, v.y, v.z)
                    else -> Vector3f(0f)
                }
            }
            registerType("Vector4f") { v ->
                when (v) {
                    true -> white4
                    false -> black4
                    is Int -> Vector4f(v.toFloat())
                    is Float -> Vector4f(v)
                    is Vector2f -> Vector4f(v.x, v.y, 0f, 0f)
                    is Vector3f -> Vector4f(v.x, v.y, v.z, 0f)
                    is Vector4f -> v
                    else -> Vector4f(0f)
                }
            }
            registerType("Texture") { v ->
                when (v) {
                    is Float -> Texture(Vector4f(v, v, v, 1f))
                    is Vector2f -> Texture(Vector4f(v, 0f, 1f))
                    is Vector3f -> Texture(Vector4f(v, 1f))
                    is Vector4f -> Texture(v)
                    is Texture -> v
                    is ITexture2D -> Texture(v)
                    else -> null
                }.run { if (this != null && isDestroyed) null else this }
            }
            registerType("ITexture2D") { v ->
                when (v) {
                    is Texture -> v.tex
                    is ITexture2D -> v
                    else -> null
                }.run { if (this is Texture2D && isDestroyed) null else this }
            }
            registerType("Texture2D") { v ->
                when (v) {
                    is Texture -> v.tex as? Texture2D
                    is Texture2D -> v
                    else -> null
                }.run { if (this != null && isDestroyed) null else this }
            }
            registerType("String") { it?.toString() ?: "" }
            registerType("Texture3D") { it as? Texture3D }
            registerType("File", ::anyToFile)
            registerType("FileReference", ::anyToFile)
            registerType("Bool", ::anyToBool)
            registerType("Boolean", ::anyToBool)
            registerType("IntArray") { it as? IntArray }
            registerType("LongArray") { it as? LongArray }
            registerType("FloatArray") { it as? FloatArray }
            registerType("DoubleArray") { it as? DoubleArray }
            registerType("Any?", ::identity)
            registerType("?", ::identity)
            registerType("", ::identity)
        }

        private fun identity(v: Any?): Any? = v

        private fun anyToFile(v: Any?): FileReference {
            return v as? FileReference ?: InvalidRef
        }

        private fun anyToBool(v: Any?): Boolean {
            return when (v) {
                is Int -> v != 0
                is Long -> v != 0L
                is Float -> v.isFinite() && v != 0f
                is Double -> v.isFinite() && v != 0.0
                is String -> v.isNotEmpty()
                is Boolean -> v
                else -> v != null
            }
        }
    }

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
        if (graph != null) lastValidId = graph.validId
        var value = currValue
        // validate type
        val validator = typeValidators[type]
        value = when {
            validator != null -> validator(value)
            type.startsWith("List<") -> value as? List<*> ?: emptyList<Any?>()
            type.startsWith("Enum<") -> value as? Enum<*>
            else -> {
                LOGGER.warn("Type $type needs a validator")
                value
            }
        }
        currValue = value
        return value
    }
}