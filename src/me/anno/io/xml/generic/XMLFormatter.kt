package me.anno.io.xml.generic

import me.anno.io.ISaveable
import me.anno.io.xml.saveable.XMLStringWriter
import me.anno.studio.StudioBase
import me.anno.utils.types.Strings

object XMLFormatter {

    fun format(v: Any?, indentation: String = "  ", lineBreakLength: Int = 50): String {
        return when (v) {
            is String -> format(XMLReader().read(v.byteInputStream()))
            is XMLNode -> format(v, indentation, lineBreakLength)
            is ISaveable -> format(XMLStringWriter.toText(v, StudioBase.workspace))
            else -> throw IllegalArgumentException()
        }
    }

    fun format(v: XMLNode, indentation: String, lineBreakLength: Int = 50): String {
        val builder = StringBuilder()
        append(builder, v, indentation, lineBreakLength, -1)
        builder.setLength(builder.length - 1) // remove last line break
        return builder.toString()
    }

    private fun nextLine(builder: StringBuilder, indentation: String, depth: Int) {
        for (i in 0..depth) {
            builder.append(indentation)
        }
    }

    fun append(builder: StringBuilder, v: XMLNode, indentation: String, lineBreakLength: Int, depth: Int) {
        var lineStart = builder.length
        nextLine(builder, indentation, depth)
        builder.append('<').append(v.type)
        for ((key, value) in v.attributes) {
            // if line is too long, add line break
            if (builder.length - lineStart > lineBreakLength) {
                builder.append('\n')
                nextLine(builder, indentation, depth + 1)
                lineStart = builder.length
            }
            // add attribute
            builder.append(' ').append(key).append("=\"")
            Strings.writeEscaped(value, builder)
            builder.append("\"")
        }
        if (v.children.isNotEmpty()) {
            builder.append(">\n")
            for (child in v.children) {
                when (child) {
                    is String -> builder.append(child) // todo indent text?
                    is XMLNode -> append(builder, child, indentation, lineBreakLength, depth + 1)
                    else -> throw NotImplementedError()
                }
            }
            nextLine(builder, indentation, depth)
            builder.append("</").append(v.type).append(">\n")
        } else {
            builder.append("/>\n")
        }
    }
}