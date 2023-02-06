package me.anno.io.xml

import java.io.EOFException
import java.io.InputStream
import java.util.*

class XMLScanner : XMLReader() {

    val types = ArrayList<ComparableStringBuilder>()
    var typeI = 0

    init {
        types.add(type0)
    }

    fun parse(
        input: InputStream,
        onStart: (type: CharSequence) -> Boolean,
        onEnd: (type: CharSequence) -> Unit,
        onProperty: (type: CharSequence, key: CharSequence, value: CharSequence) -> Unit,
    ) = parse(-1, input, onStart, onEnd, onProperty)

    fun parse(
        firstChar: Int, input: InputStream,
        onStart: (type: CharSequence) -> Boolean,
        onEnd: (type: CharSequence) -> Unit,
        onProperty: (type: CharSequence, key: CharSequence, value: CharSequence) -> Unit,
    ): Any? {
        val first = if(firstChar < 0) input.skipSpaces() else firstChar
        if (first == '<'.code) {
            if (types.size <= typeI) types.add(ComparableStringBuilder())
            val type = input.readTypeUntilSpaceOrEnd(types[typeI])
            val end = last
            @Suppress("SpellCheckingInspection")
            when {
                type.startsWith("?") -> {
                    // <?xml version="1.0" encoding="utf-8"?>
                    // I don't really care about it
                    // read until ?>
                    // or <?xpacket end="w"?>
                    input.readUntil("?>")
                    return parse(-1, input, onStart, onEnd, onProperty)
                }
                type.startsWith("!--") -> {
                    // search until -->
                    input.readUntil("-->")
                    return parse(-1, input, onStart, onEnd, onProperty)
                }
                type.startsWith("!doctype", true) -> {
                    var ctr = 1
                    while (ctr > 0) {
                        when (input.read()) {
                            '<'.code -> ctr++
                            '>'.code -> ctr--
                        }
                    }
                    return parse(-1, input, onStart, onEnd, onProperty)
                }
                type.startsWith("![cdata[", true) -> {
                    val value = input.getStringUntil("]]>")
                    return value + readString(0, input)
                }
                type.startsWith('/') -> return endElement
            }

            if (!onStart(type)) {
                // this node can be ignored
                var depth = 1
                while (true) {
                    when (input.read()) {
                        '>'.code -> {
                            depth--
                            if (depth == 0) {
                                return Unit
                            }
                        }
                        '<'.code -> depth++
                        '"'.code -> input.skipString('"'.code)
                        '\''.code -> input.skipString('\''.code)
                        -1 -> throw EOFException()
                    }
                }
            }

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
                    assert(propEnd, '=')
                    val start = input.skipSpaces()
                    assert(start, '"', '\'')
                    val value = input.readString(start, valueBuilder)
                    onProperty(type, propName, value)
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
                    onEnd(type)
                    return Unit
                }
                '>'.code -> {
                    // read the body (all children)
                    typeI++
                    var next: Int = -1
                    while (true) {
                        val child = parse(next, input, onStart, onEnd, onProperty)
                        next = -1
                        when (child) {
                            endElement -> {
                                typeI--
                                onEnd(type)
                                return Unit
                            }
                            is String -> {
                                onProperty(type, "", child)
                                next = '<'.code
                            }
                            null -> throw RuntimeException()
                            else -> {} // do nothing
                        }
                    }
                }
                else -> throw RuntimeException("Unknown end symbol ${end2.toChar()}")
            }
        } else return readString(first, input)
    }

}