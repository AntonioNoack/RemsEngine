package me.anno.graph.visual.node

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture3D
import me.anno.graph.visual.ComputeNode
import me.anno.graph.visual.FlowGraph
import me.anno.graph.visual.render.Texture
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.types.AnyToBool
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToFloat
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong
import me.anno.utils.types.AnyToVector
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
            registerType("Int", AnyToInt::getInt)
            registerType("Long", AnyToLong::getLong)
            registerType("Float", AnyToFloat::getFloat)
            registerType("Double", AnyToDouble::getDouble)
            registerType("Vector2f", AnyToVector::getVector2f)
            registerType("Vector3f", AnyToVector::getVector3f)
            registerType("Vector4f", AnyToVector::getVector4f)
            registerType("Vector2d", AnyToVector::getVector2d)
            registerType("Vector3d", AnyToVector::getVector3d)
            registerType("Vector4d", AnyToVector::getVector4d)
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
                }?.createdOrNull()
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
            registerType("File", Companion::anyToFile)
            registerType("FileReference", Companion::anyToFile)
            registerType("Bool", AnyToBool::anyToBool)
            registerType("Boolean", AnyToBool::anyToBool)
            registerType("IntArray") { it as? IntArray }
            registerType("LongArray") { it as? LongArray }
            registerType("FloatArray") { it as? FloatArray }
            registerType("DoubleArray") { it as? DoubleArray }
            registerType("Any?", Companion::identity)
            registerType("?", Companion::identity)
            registerType("", Companion::identity)
        }

        private fun identity(v: Any?): Any? = v

        private fun anyToFile(v: Any?): FileReference {
            return v as? FileReference ?: InvalidRef
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