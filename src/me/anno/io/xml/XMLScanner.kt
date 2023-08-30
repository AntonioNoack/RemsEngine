package me.anno.io.xml

import java.io.EOFException
import java.io.InputStream
import java.util.*

/**
 * Reads an XML file without create objects
 * */
class XMLScanner : XMLReader() {

    val types = ArrayList<ComparableStringBuilder>()
    var typeI = 0

    init {
        types.add(type0)
    }

    /**
     * Called, when an XML node is entered
     * */
    fun interface OnStart {
        /**
         * @return whether the node shall be traversed; if not, OnEnd.handle() isn't called.
         * */
        fun handle(type: CharSequence): Boolean
    }

    /**
     * Called, when an XML node is exited
     * */
    fun interface OnEnd {
        fun handle(type: CharSequence)
    }

    /**
     * Called on each XML attribute
     * */
    fun interface OnAttribute {
        fun handle(type: CharSequence, key: CharSequence, value: CharSequence)
    }

    fun parse(input: InputStream, onStart: OnStart, onEnd: OnEnd, onAttribute: OnAttribute): Any? {
        return parse(-1, input, onStart, onEnd, onAttribute)
    }

    fun parse(firstChar: Int, input: InputStream, onStart: OnStart, onEnd: OnEnd, onAttribute: OnAttribute): Any? {
        val first = if (firstChar < 0) input.skipSpaces() else firstChar
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
                    return parse(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("!--") -> {
                    // search until -->
                    input.readUntil("-->")
                    return parse(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("!doctype", true) -> {
                    var ctr = 1
                    while (ctr > 0) {
                        when (input.read()) {
                            '<'.code -> ctr++
                            '>'.code -> ctr--
                        }
                    }
                    return parse(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("![cdata[", true) -> {
                    val value = input.getStringUntil("]]>")
                    return value + readString(0, input)
                }
                type.startsWith('/') -> return endElement
            }

            if (!onStart.handle(type)) {
                // this node can be ignored
                var depth = 1
                while (true) {
                    when (input.read()) {
                        '>'.code -> {
                            depth--
                            if (depth == 0) {
                                return sthElement
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
                    onAttribute.handle(type, propName, value)
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
                    onEnd.handle(type)
                    return sthElement
                }
                '>'.code -> {
                    // read the body (all children)
                    typeI++
                    var next: Int = -1
                    while (true) {
                        val child = parse(next, input, onStart, onEnd, onAttribute)
                        next = -1
                        when (child) {
                            endElement -> {
                                typeI--
                                onEnd.handle(type)
                                return sthElement
                            }
                            is String -> {
                                onAttribute.handle(type, "", child)
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