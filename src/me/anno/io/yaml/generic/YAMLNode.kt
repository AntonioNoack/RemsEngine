package me.anno.io.yaml.generic

import me.anno.io.yaml.generic.YAMLReader.listKey

class YAMLNode(
    val key: String,
    val depth: Int,
    val value: String? = null,
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

    fun getBool(): Boolean? = if (value == null) null else value.toIntOrNull() != 0
    fun getBool(name: String): Boolean? = this[name]?.getBool()

    fun getInt(): Int? = value?.toIntOrNull()
    fun getInt(name: String): Int? = this[name]?.getInt()

    fun getFloat(): Float? = value?.toFloatOrNull()
    fun getFloat(name: String): Float? = this[name]?.getFloat()

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
            builder.append(": ").append(value).append('\n')
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