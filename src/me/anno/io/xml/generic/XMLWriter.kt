package me.anno.io.xml.generic

object XMLWriter {

    fun write(xml: XMLNode, indentation: String? = " ", closeEmptyTypes: Boolean = false): String {
        val builder = StringBuilder(64)
        builder.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
        write(xml, builder, 0, indentation, closeEmptyTypes)
        return builder.toString()
    }

    fun tabs(builder: StringBuilder, depth: Int, indentation: String?) {
        indentation ?: return
        for (i in 0 until depth) builder.append(indentation)
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
            // todo escape string value
            builder.append(' ').append(k).append("=\"")
                .append(v).append('"')
        }
        if (closeEmptyTypes && node.children.isEmpty()) {
            builder.append("/>")
        } else {
            builder.append(">\n")
            for (child in node.children) {
                if (child is XMLNode) {
                    write(child, builder, depth + 1, indentation, closeEmptyTypes)
                } else {
                    // todo escape this
                    tabs(builder, depth + 1, indentation)
                    builder.append(child.toString()).append('\n')
                }
            }
            tabs(builder, depth, indentation)
            builder.append("</").append(node.type).append('>')
        }
        if (depth > 0) builder.append('\n')
    }
}