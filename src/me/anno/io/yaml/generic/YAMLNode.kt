package me.anno.io.yaml.generic

import me.anno.io.yaml.generic.YAMLReader.LIST_KEY
import me.anno.utils.types.Strings
import me.anno.utils.types.Strings.isBlank
import me.anno.utils.types.Strings.writeEscaped

class YAMLNode(
    val key: String, val depth: Int, val value: String? = null,
    var children: List<YAMLNode> = emptyList()
) {

    fun add(node: YAMLNode) {
        val content = children
        if (content.isEmpty()) this.children = arrayListOf(node)
        else (content as MutableList<YAMLNode>).add(node)
    }

    fun packListEntries(): List<YAMLNode> {
        val children = children
        return children.filter { it.key != LIST_KEY } +
                children.filter { it.key == LIST_KEY }.mapNotNull { it.children.firstOrNull() }
    }

    operator fun get(key: String): YAMLNode? {
        return children.firstOrNull { it.key == key }
    }

    fun getBool(): Boolean? = if (value == null) null else value.toIntOrNull() != 0
    fun getBool(name: String): Boolean? = this[name]?.getBool()

    fun getInt(): Int? = value?.toIntOrNull()
    fun getInt(name: String): Int? = this[name]?.getInt()

    fun getFloat(): Float? = value?.toFloatOrNull()
    fun getFloat(name: String): Float? = this[name]?.getFloat()

    fun toString(builder: StringBuilder, printStartTabs: Boolean): StringBuilder {
        val value = value
        if (printStartTabs) {
            for (i in 0 until depth) {
                builder.append(' ')
            }
        }
        val isList = key == LIST_KEY
        if (isList) {
            builder.append("- ")
        } else {
            builder.append(key).append(if (value != null) ": " else ":\n")
        }
        if (value != null) {
            // escape value if necessary
            val needsEscape = value.isEmpty() ||
                    (value.first().isBlank() || value.last().isBlank()) ||
                    value.any { Strings.getEscapeChar(it, '\'') != Strings.DONT_ESCAPE || it == '"' }
            if (needsEscape) {
                builder.append('\'')
                writeEscaped(value, builder, '\'')
                builder.append('\'')
            } else {
                builder.append(value)
            }
            builder.append('\n')
        }
        var startTabs = !isList
        val children = children
        for (index in children.indices) {
            children[index].toString(builder, startTabs)
            startTabs = true
        }
        return builder
    }

    fun toString(prefix: String): String {
        val builder = StringBuilder()
        builder.append(prefix)
        toString(builder, false)
        builder.setLength(builder.length - 1)
        return builder.toString()
    }

    override fun toString(): String {
        return toString("")
    }
}