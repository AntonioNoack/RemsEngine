package me.anno.io.xml.generic

import me.anno.fonts.Codepoints.codepoints

object XMLWriter {

    fun write(xml: XMLNode): String {
        return write(xml, " ", false)
    }

    fun write(xml: XMLNode, indentation: String?, closeEmptyTypes: Boolean): String {
        val builder = StringBuilder(64)
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        if (indentation != null) builder.append("\n")
        write(xml, builder, if (indentation == null) Int.MIN_VALUE else 0, indentation, closeEmptyTypes)
        return builder.toString()
    }

    fun tabs(builder: StringBuilder, depth: Int, indentation: String?) {
        if (depth <= 0 || indentation == null) return
        for (i in 0 until depth) builder.append(indentation)
    }

    fun StringBuilder.appendStringEscaped(str: String): StringBuilder {
        for (cx in str.codepoints()) {
            if (cx in 32 until 128) {
                when (val c = cx.toChar()) {
                    in 'A'..'Z', in 'a'..'z',
                    in '0'..'9', in " .:,;-_!§$%/()=?[]{}" -> append(c)
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    '&' -> append("&amp;")
                    else -> append("&#").append(c.code).append(';')
                }
            } else {
                append("&#").append(cx).append(';')
            }
        }
        return this
    }

    fun StringBuilder.appendXMLEscaped(str: String): StringBuilder {
        for (c in str) {
            when (c) {
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '\n' -> append("<br/>")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                '&' -> append("&amp;")
                else -> append(c)
            }
        }
        return this
    }

    fun escapeXML(str: String): String {
        val extra = str.count { it in "<>\n\"'&" }
        if (extra == 0) return str
        val builder = StringBuilder(str.length + extra * 3)
        builder.appendXMLEscaped(str)
        return builder.toString()
    }

    fun write(
        node: XMLNode,
        builder: StringBuilder,
        depth: Int,
        indentation: String?,
        closeEmptyTypes: Boolean
    ) {
        tabs(builder, depth, indentation)
        builder.append('<').append(node.type)
        for ((k, v) in node.attributes) {
            // escape string value
            builder.append(' ').append(k).append("=\"")
                .appendStringEscaped(v).append('"')
        }
        if (closeEmptyTypes && node.children.isEmpty()) {
            builder.append("/>")
        } else {
            builder.append('>')
            if (indentation != null) builder.append('\n')
            for (child in node.children) {
                if (child is XMLNode) {
                    write(child, builder, depth + 1, indentation, closeEmptyTypes)
                } else {
                    tabs(builder, depth + 1, indentation)
                    builder.appendXMLEscaped(child.toString())
                    if (indentation != null) builder.append('\n')
                }
            }
            tabs(builder, depth, indentation)
            builder.append("</").append(node.type).append('>')
        }
        if (depth > 0) builder.append('\n')
    }
}