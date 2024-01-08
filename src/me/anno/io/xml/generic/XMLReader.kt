package me.anno.io.xml.generic

import me.anno.io.xml.ComparableStringBuilder
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

open class XMLReader {

    fun InputStream.skipSpaces(): Int {
        while (true) {
            when (val char = read()) {
                ' '.code,
                '\t'.code,
                '\r'.code,
                '\n'.code -> {
                }
                -1 -> throw EOFException()
                else -> return char
            }
        }
    }

    var last: Int = 0
    val type0 = ComparableStringBuilder()

    val keyBuilder = ComparableStringBuilder()
    val valueBuilder = ComparableStringBuilder()

    fun InputStream.readTypeUntilSpaceOrEnd(builder: ComparableStringBuilder, last: Int = -1): ComparableStringBuilder {
        builder.clear()
        if (last >= 0) builder.append(last.toChar())
        while (true) {
            when (val char = read()) {
                ' '.code, '\t'.code, '\r'.code, '\n'.code -> {
                    this@XMLReader.last = ' '.code
                    return builder
                }
                '>'.code, '='.code -> {
                    this@XMLReader.last = char
                    return builder
                }
                '/'.code -> {
                    if (builder.isEmpty()) {
                        builder.append(char.toChar())
                    } else {
                        this@XMLReader.last = char
                        return builder
                    }
                }
                -1 -> throw EOFException()
                else -> builder.append(char.toChar())
            }
        }
    }

    fun InputStream.readString(startSymbol: Int, builder: ComparableStringBuilder): ComparableStringBuilder {
        builder.clear()
        while (true) {
            when (val char = read()) {
                '\\'.code -> {
                    when (val second = read()) {
                        '\\'.code -> builder.append('\\')
                        else -> {
                            builder.append('\\')
                            builder.append(second.toChar())
                        }
                    }
                }
                startSymbol -> return builder
                -1 -> throw EOFException()
                else -> builder.append(char.toChar())
            }
        }
    }

    fun InputStream.skipString(startSymbol: Int) {
        while (true) {
            when (read()) {
                '\\'.code -> read()
                startSymbol -> return
                -1 -> throw EOFException()
                else -> {}
            }
        }
    }

    fun min(a: Int, b: Int) = kotlin.math.min(a, b)

    fun InputStream.getStringUntil(end: String): String {
        val size = end.length
        val reversed = end.reversed()
        val buffer = CharArray(size)
        val result = ByteArrayOutputStream(256)
        var length = 0
        search@ while (true) {

            // push the checking buffer forward
            for (i in 1 until min(length + 1, size)) {
                buffer[i] = buffer[i - 1]
            }

            // read the next char
            val here = read()
            if (here == -1) throw EOFException()

            // add it to the result
            result.write(here)
            length++

            // check if the end was reached
            buffer[0] = here.toChar()
            if (length >= size) {

                for ((i, target) in reversed.withIndex()) {
                    if (buffer[i] != target) continue@search
                }

                // yes, it was
                val bytes = result.toByteArray()
                return bytes.decodeToString(0, bytes.size - size)
            }
        }
    }

    fun InputStream.readUntil(end: String) {
        val size = end.length
        val reversed = end.reversed()
        val buffer = CharArray(size)
        var length = 0
        search@ while (true) {

            for (i in 1 until min(length + 1, size)) {
                buffer[i] = buffer[i - 1]
            }

            val here = read()
            if (here == -1) throw EOFException()
            length++

            buffer[0] = here.toChar()
            if (length >= size) {
                for ((i, target) in reversed.withIndex()) {
                    if (buffer[i] != target) continue@search
                }
                break
            }
        }
    }

    fun read(input: InputStream) = read(-1, input)
    fun read(firstChar: Int, input: InputStream): Any? {
        val first = if (firstChar < 0) input.skipSpaces() else firstChar
        if (first == '<'.code) {
            val type = input.readTypeUntilSpaceOrEnd(type0).toString()
            val end = last
            @Suppress("SpellCheckingInspection")
            when {
                type.startsWith("?") -> {
                    // <?xml version="1.0" encoding="utf-8"?>
                    // I don't really care about it
                    // read until ?>
                    // or <?xpacket end="w"?>
                    input.readUntil("?>")
                    return read(-1, input)
                }
                type.startsWith("!--") -> {
                    // search until -->
                    input.readUntil("-->")
                    return read(-1, input)
                }
                type.startsWith("!doctype", true) -> {
                    var ctr = 1
                    while (ctr > 0) {
                        when (input.read()) {
                            '<'.code -> ctr++
                            '>'.code -> ctr--
                        }
                    }
                    return read(-1, input)
                }
                type.startsWith("![cdata[", true) -> {
                    val value = input.getStringUntil("]]>")
                    return value + readString(0, input)
                }
                type.startsWith('/') -> return endElement
            }

            val xmlNode = XMLNode(type)
            // / is the end of an element
            var end2 = end
            if (end == ' '.code) {
                var next = -1
                // read the properties
                propertySearch@ while (true) {
                    // name="value"
                    if (next < 0) next = input.skipSpaces()
                    val propName = input.readTypeUntilSpaceOrEnd(keyBuilder, next)
                    val propEnd = last
                    // ("  '${if(next < 0) "" else next.toChar().toString()}$propName' '${propEnd.toChar()}'")
                    assertEquals(propEnd, '='.code)
                    val start = input.skipSpaces()
                    assert(start, '"', '\'')
                    val value = input.readString(start, valueBuilder)
                    xmlNode[propName.toString()] = value.toString()
                    next = input.skipSpaces()
                    when (next) {
                        '/'.code, '>'.code -> {
                            end2 = next
                            break@propertySearch
                        }
                    }
                }
            }

            when (end2) {
                '/'.code -> {
                    assertEquals(input.read(), '>'.code)
                    return xmlNode
                }
                '>'.code -> {
                    // read the body (all children)
                    var next: Int = -1
                    children@ while (true) {
                        val child = read(next, input)
                        next = -1
                        when (child) {
                            endElement -> return xmlNode
                            is String -> {
                                xmlNode.children.add(child)
                                next = '<'.code
                            }
                            null -> throw RuntimeException()
                            else -> xmlNode.children.add(child)
                        }
                    }
                }
                else -> throw RuntimeException("Unknown end symbol ${end2.toChar()}")
            }
        } else return readString(first, input)
    }

    fun readString(first: Int, input: InputStream): String {
        val str = valueBuilder
        str.clear()
        if (first != 0) str.append(first.toChar())
        while (true) {
            when (val char = input.read()) {
                '\\'.code -> {
                    when (val second = input.read()) {
                        else -> throw RuntimeException("Special character \\${second.toChar()} not yet implemented")
                    }
                }
                '<'.code -> return str.toString()
                -1 -> throw EOFException()
                else -> str.append(char.toChar())
            }
        }
    }

    fun assert(a: Int, b: Char, c: Char) {
        val ac = a.toChar()
        if (ac != b && ac != c) throw IOException("Expected $b or $c, but got ${a.toChar()}")
    }

    fun assertEquals(a: Int, b: Int) {
        if (a != b) throw IOException("$a != $b")
    }

    companion object {
        @JvmStatic
        val endElement = Any()

        @JvmStatic
        val sthElement = Any()
    }
}