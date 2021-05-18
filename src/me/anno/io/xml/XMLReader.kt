package me.anno.io.xml

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.util.*

object XMLReader {

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

    fun InputStream.readTypeUntilSpaceOrEnd(): Pair<String, Int> {
        var name = ""
        while (true) {
            when (val char = read()) {
                ' '.code, '\t'.code, '\r'.code, '\n'.code -> return name to ' '.code
                '>'.code, '='.code -> return name to char
                -1 -> throw EOFException()
                else -> name += char.toChar()
            }
        }
    }

    fun InputStream.readString(startSymbol: Int): String {
        val str = StringBuilder(20)
        while (true) {
            when (val char = read()) {
                '\\'.code -> {
                    str.append(
                        when (val second = read()) {
                            '\\'.code -> "\\"
                            /*'U'.toInt(), 'u'.toInt() -> {
                                val str2 = "${read().toChar()}${read().toChar()}${read().toChar()}${read().toChar()}"
                                val value = str2.toIntOrNull(16) ?: {
                                    LOGGER.warn("JSON String \\$second$str2 could not be parsed")
                                    32
                                }()
                                Character.toChars(value).joinToString("")
                            }*/
                            else -> {
                                str.append('\\')
                                second.toChar()
                                // throw RuntimeException("Special character \\${second.toChar()} not yet implemented")
                            }
                        }
                    )
                }
                startSymbol -> return str.toString()
                -1 -> throw EOFException()
                else -> str.append(char.toChar())
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
                return String(bytes, 0, bytes.size - size)

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

    fun parse(input: InputStream) = parse(null, input)
    fun parse(firstChar: Int?, input: InputStream): Any? {
        val first = firstChar ?: input.skipSpaces()
        if (first == '<'.code) {
            val (name, end) = input.readTypeUntilSpaceOrEnd()
            val lowerCaseName = name.lowercase(Locale.getDefault())
            when {
                name.startsWith("?") -> {
                    // <?xml version="1.0" encoding="utf-8"?>
                    // I don't really care about it
                    // read until ?>
                    // or <?xpacket end="w"?>
                    input.readUntil("?>")
                    return parse(null, input)
                }
                name.startsWith("!--") -> {
                    // search until -->
                    input.readUntil("-->")
                    return parse(null, input)
                }
                lowerCaseName.startsWith("!doctype") -> {
                    var ctr = 1
                    while (ctr > 0) {
                        when (input.read()) {
                            '<'.code -> ctr++
                            '>'.code -> ctr--
                        }
                    }
                    return parse(null, input)
                }
                lowerCaseName.startsWith("![cdata[") -> {
                    val value = input.getStringUntil("]]>")
                    return value + readString(0, input)
                }
                name.startsWith('/') -> return endElement
            }

            val xmlElement = XMLElement(name)
            // / is the end of an element
            var end2 = end
            if (end == ' '.code) {
                var next = -1
                // read the properties
                propertySearch@ while (true) {
                    // name="value"
                    if (next < 0) next = input.skipSpaces()
                    val (propName, propEnd) = input.readTypeUntilSpaceOrEnd()
                    // ("  '${if(next < 0) "" else next.toChar().toString()}$propName' '${propEnd.toChar()}'")
                    assert(propEnd, '=')
                    val start = input.skipSpaces()
                    assert(start, '"', '\'')
                    val value = input.readString(start)
                    xmlElement[if (next < 0) propName else "${next.toChar()}$propName"] = value
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
                    assert(input.read(), '>')
                    return xmlElement
                }
                '>'.code -> {
                    // read the body (all children)
                    var next: Int? = null
                    children@ while (true) {
                        val child = parse(next, input)
                        next = null
                        when (child) {
                            endElement -> return xmlElement
                            is String -> {
                                xmlElement.children.add(child)
                                next = '<'.code
                            }
                            null -> throw RuntimeException()
                            else -> xmlElement.children.add(child)
                        }
                    }
                }
                else -> throw RuntimeException("Unknown end symbol ${end2.toChar()}")
            }
        } else return readString(first, input)
    }

    fun readString(first: Int, input: InputStream): String {
        val str = StringBuilder(20)
        if(first != 0) str.append(first.toChar())
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

    fun assert(a: Int, b: Char) {
        if (a.toChar() != b) throw RuntimeException("Expected $b, but got ${a.toChar()}")
    }

    fun assert(a: Int, b: Char, c: Char) {
        val ac = a.toChar()
        if (ac != b && ac != c) throw RuntimeException("Expected $b, but got ${a.toChar()}")
    }

    fun assert(a: Int, b: Int) {
        if (a != b) throw RuntimeException("Expected ${b.toChar()}, but got ${a.toChar()}")
    }

    val endElement = Any()

}