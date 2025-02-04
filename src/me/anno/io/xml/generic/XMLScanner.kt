package me.anno.io.xml.generic

import me.anno.io.xml.ComparableStringBuilder
import me.anno.utils.assertions.assertEquals
import java.io.Reader

/**
 * Reads an XML file without create objects
 * */
open class XMLScanner : XMLReader() {

    private val types = ArrayList<ComparableStringBuilder>()
    private var typeI = 0

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
        fun handle(depth: Int, type: CharSequence): Boolean
    }

    /**
     * Called, when an XML node is exited
     * */
    fun interface OnEnd {
        fun handle(depth: Int, type: CharSequence)
    }

    /**
     * Called on each XML attribute
     * */
    fun interface OnAttribute {
        fun handle(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence)
    }

    fun scan(input: Reader, onStart: OnStart, onEnd: OnEnd, onAttribute: OnAttribute): Any? {
        return scan(-1, input, onStart, onEnd, onAttribute)
    }

    fun scan(
        firstChar: Int, input: Reader, onStart: OnStart, onEnd: OnEnd, onAttribute: OnAttribute,
        depth: Int = 0
    ): Any? {
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
                    return scan(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("!--") -> {
                    // search until -->
                    input.readUntil("-->")
                    return scan(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("!doctype", true) -> {
                    var ctr = 1
                    while (ctr > 0) {
                        when (input.read()) {
                            '<'.code -> ctr++
                            '>'.code -> ctr--
                        }
                    }
                    return scan(-1, input, onStart, onEnd, onAttribute)
                }
                type.startsWith("![cdata[", true) -> {
                    val value = input.getStringUntil("]]>")
                    return value + readString(0, input)
                }
                type.startsWith('/') -> return endElement
            }

            if (!onStart.handle(depth, type)) {
                // this node can be ignored
                var depthI = 1
                while (true) {
                    when (input.read()) {
                        '>'.code -> {
                            depthI--
                            if (depthI == 0) {
                                return sthElement
                            }
                        }
                        '<'.code -> depthI++
                        '"'.code -> input.skipString('"'.code)
                        '\''.code -> input.skipString('\''.code)
                        -1 -> return RuntimeException("Unexpected end")
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
                    assertEquals(propEnd, '='.code)
                    val start = input.skipSpaces()
                    assert(start, '"', '\'')
                    val value = input.readString(start, valueBuilder)
                    onAttribute.handle(depth, type, propName, value)
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
                    onEnd.handle(depth, type)
                    return sthElement
                }
                '>'.code -> {
                    // read the body (all children)
                    typeI++
                    var next: Int = -1
                    while (true) {
                        val child = scan(next, input, onStart, onEnd, onAttribute, depth + 1)
                        next = -1
                        when (child) {
                            endElement -> {
                                typeI--
                                onEnd.handle(depth, type)
                                return sthElement
                            }
                            is String -> {
                                onAttribute.handle(depth, type, "", child)
                                next = '<'.code
                            }
                            is Exception -> return child
                            null -> return RuntimeException("Unexpected child type")
                            else -> {} // do nothing
                        }
                    }
                }
                else -> return RuntimeException("Unknown end symbol ${end2.toChar()}")
            }
        } else return readString(first, input)
    }
}