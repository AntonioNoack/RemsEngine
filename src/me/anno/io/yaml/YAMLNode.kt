package me.anno.io.yaml

import me.anno.io.yaml.YAMLReader.listKey
import me.anno.io.yaml.YAMLReader.parseYAMLxJSON
import me.anno.utils.Color.rgba
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.roundToInt

class YAMLNode(
    var key: String,
    var depth: Int,
    var value: String? = null,
    var children: List<YAMLNode>? = null
) {

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