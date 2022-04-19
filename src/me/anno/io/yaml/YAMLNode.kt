package me.anno.io.yaml

import me.anno.io.yaml.YAMLReader.listKey
import me.anno.io.yaml.YAMLReader.parseYAMLxJSON
import me.anno.utils.Color.rgba
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt

class YAMLNode(
    var key: String,
    var depth: Int,
    var value: String? = null,
    var children: List<YAMLNode>? = null
) {

    init {
        val value = value
        if (value != null && value.endsWith("}") && !value.startsWith("{"))
            RuntimeException("Incorrect YAML Value '$value'").printStackTrace()
    }

    fun add(node: YAMLNode) {
        val content = children
        if (content == null) this.children = arrayListOf(node)
        else (content as MutableList<YAMLNode>).add(node)
    }

    fun packListEntries(): YAMLNode {
        val children = children
        return if (children == null) this
        else {
            val newChildren = children.filter { it.key != listKey } +
                    children.filter { it.key == listKey }.mapNotNull { it.children?.firstOrNull() }
            YAMLNode(key, depth, value, newChildren)
        }

    }

    operator fun get(key: String): YAMLNode? {
        return children?.firstOrNull { it.key == key }
    }

    fun getBool(): Boolean? = if (value == null) null else value?.toIntOrNull() != 0
    fun getBool(name: String): Boolean? = this[name]?.getBool()

    fun getInt(): Int? = value?.toIntOrNull()
    fun getInt(name: String): Int? = this[name]?.getInt()

    fun getFloat(): Float? = value?.toFloatOrNull()
    fun getFloat(name: String): Float? = this[name]?.getFloat()

    fun getColorAsInt(name: String): Int? = this[name]?.getColorAsInt()
    fun getColorAsInt(): Int? {
        val str = value ?: return null
        var r = 255
        var g = 255
        var b = 255
        var a = 255
        parseYAMLxJSON(str) { key, value ->
            val asFloat = value.toFloatOrNull()
            if (asFloat != null) {
                val asInt = (asFloat * 255).roundToInt()
                when (key) {
                    "r", "x" -> r = asInt
                    "g", "y" -> g = asInt
                    "b", "z" -> b = asInt
                    "a", "w" -> a = asInt
                }
            }
        }
        return rgba(r, g, b, a)
    }

    fun getColorAsVector4f(name: String): Vector4f? = this[name]?.getColorAsVector4f()
    fun getColorAsVector4f(): Vector4f? {
        val str = value ?: return null
        var r = 1f
        var g = 1f
        var b = 1f
        var a = 1f
        parseYAMLxJSON(str) { key, value ->
            val asFloat = value.toFloatOrNull()
            if (asFloat != null) {
                when (key) {
                    "r", "x" -> r = asFloat
                    "g", "y" -> g = asFloat
                    "b", "z" -> b = asFloat
                    "a", "w" -> a = asFloat
                }
            }
        }
        return Vector4f(r, g, b, a)
    }

    fun getColorAsVector3f(name: String): Vector3f? = this[name]?.getColorAsVector3f()
    fun getColorAsVector3f(): Vector3f? {
        val str = value ?: return null
        var r = 1f
        var g = 1f
        var b = 1f
        parseYAMLxJSON(str) { key, value ->
            val asFloat = value.toFloatOrNull()
            if (asFloat != null) {
                when (key) {
                    "r", "x" -> r = asFloat
                    "g", "y" -> g = asFloat
                    "b", "z" -> b = asFloat
                }
            }
        }
        return Vector3f(r, g, b)
    }

    fun getVector3d(significance: Double): Vector3d? {
        val str = value ?: return null
        var x = 0.0
        var y = 0.0
        var z = 0.0
        parseYAMLxJSON(str) { key, value ->
            val parsed = value.toDoubleOrNull()
            if (parsed != null) {
                when (key) {
                    "r", "x" -> x = parsed
                    "g", "y" -> y = parsed
                    "b", "z" -> z = parsed
                }
            }
        }
        return if (abs(x) > significance || abs(y) > significance || abs(z) > significance) {
            Vector3d(x, y, z)
        } else null
    }

    fun getVector3dScale(significance: Double): Vector3d? {
        val str = value ?: return null
        var x = 1.0
        var y = 1.0
        var z = 1.0
        parseYAMLxJSON(str) { key, value ->
            val parsed = value.toDoubleOrNull()
            if (parsed != null) {
                when (key) {
                    "r", "x" -> x = parsed
                    "g", "y" -> y = parsed
                    "b", "z" -> z = parsed
                }
            }
        }
        return if (abs(x - 1) + abs(y - 1) + abs(z - 1) > significance) {
            Vector3d(x, y, z)
        } else null
    }

    fun getQuaternion(significance: Double): Quaterniond? {
        val str = value ?: return null
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var w = 1.0
        parseYAMLxJSON(str) { key, value ->
            val parsed = value.toDoubleOrNull()
            if (parsed != null) {
                when (key) {
                    "r", "x" -> x = parsed
                    "g", "y" -> y = parsed
                    "b", "z" -> z = parsed
                    "w" -> w = parsed
                }
            }
        }
        return if (abs(x) + abs(y) + abs(z) + abs(w - 1.0) > significance) {
            Quaterniond(x, y, z, w)
        } else null
    }

    fun toString(builder: StringBuilder): StringBuilder {
        val value = value
        builder.ensureCapacity(
            builder.length + depth + key.length +
                    (if (value != null) 3 + value.length else 2) +
                    (children?.size ?: 0) * (depth + 3)
        )
        for (i in 0 until depth) builder.append(' ')
        builder.append(key)
        if (value != null) {
            builder.append(": ")
            builder.append(value)
            builder.append('\n')
        } else {
            builder.append(":\n")
        }
        val children = children
        if (children != null) {
            for (index in children.indices) {
                children[index].toString(builder)
            }
        }
        return builder
    }

    override fun toString(): String {
        return toString(StringBuilder()).toString()
    }

}