package me.anno.io.xml

import me.anno.utils.types.Strings

object XMLWriter {

    fun write(xml: XMLNode, indentation: String? = " ", closeEmptyTypes: Boolean = false): String {
        val builder = StringBuilder(64)
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        write(xml, builder, 0, indentation, closeEmptyTypes)
        return builder.toString()
    }

    fun write(
        xml: XMLNode,
        builder: StringBuilder,
        depth: Int,
        indentation: String? = " ",
        closeEmptyTypes: Boolean = false
    ) {
        if (indentation != null) newLine(builder, depth, indentation)
        builder.append("<").append(xml.type)
        for ((k, v) in xml.attributes) {
            builder.append(' ').append(k).append("=\"")
            Strings.writeEscaped(v, builder)
            builder.append("\"")
        }
        if (xml.children.isEmpty() && closeEmptyTypes) {
            builder.append("/>")
        } else {
            builder.append(">")
            if (indentation != null && !(xml.children.size <= 1 && xml.children.all { it is String })) {
                newLine(builder, depth, indentation)
            }
            for (child in xml.children) {
                if (child is XMLNode)
                    write(child, builder, depth + 1, indentation, closeEmptyTypes)
                else builder.append(child.toString())
            }
            builder.append("</").append(xml.type).append(">")
        }
    }

    fun newLine(builder: StringBuilder, depth: Int, indentation: String?) {
        builder.append('\n')
        for (i in 0 until depth) builder.append(indentation)
    }

}