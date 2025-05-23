package me.anno.io.xml.generic

import me.anno.io.xml.ComparableStringBuilder
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import java.io.Reader

/**
 * Reads an XML file without create objects
 *
 * todo transform this class into using a stack explicitly
 * */
abstract class XMLScanner(reader: Reader) : XMLReaderBase(reader) {

    private val types = ArrayList<ComparableStringBuilder>()
    private var typeI = 0

    init {
        types.add(type0)
    }

    /**
     * Called, when an XML node is entered; return whether you want to process/enter that node.
     * */
    abstract fun onStart(depth: Int, type: CharSequence): Boolean

    /**
     * Called, when an XML node is exited
     * */
    abstract fun onEnd(depth: Int, type: CharSequence)

    /**
     * Called on each XML attribute
     * */
    abstract fun onAttribute(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence)

    /**
     * Called on each XML string inside an element
     * */
    abstract fun onContent(depth: Int, type: CharSequence, value: CharSequence)

    fun scan(): Any {
        return scan(-1, 0)
    }

    private fun scan(firstChar: Int, depth: Int): Any {
        val first = if (firstChar < 0) skipSpaces() else firstChar
        if (first == -1) return endOfReader
        if (first != '<'.code) return readStringUntilNextNode(first)
        if (types.size <= typeI) types.add(ComparableStringBuilder())
        val type = readTypeUntilSpaceOrEnd(types[typeI], -1)
        return when {
            type.startsWith("?") -> skipScanXMLVersion(depth)
            type.startsWith("!--") -> {
                // search until -->
                readUntil("-->")
                scan(-1, depth)
            }
            type.startsWith("!doctype", true) -> skipScanDocType(depth)
            type.startsWith("![cdata[", true) -> readCData(type)
            type.startsWith('/') -> endElement
            else -> scanXMLNode(depth, type)
        }
    }

    private fun scanXMLAttributes(depth: Int, type: CharSequence): Int {
        var next = -1
        while (true) {
            // name="value"
            if (next < 0) next = skipSpaces()
            val propName = readTypeUntilSpaceOrEnd(keyBuilder, next)
            val propEnd = last
            assertEquals(propEnd, '='.code)
            val start = skipSpaces()
            assert(start, '"', '\'')
            val value = readStringUntilQuotes(start)
            onAttribute(depth, type, propName, value)
            next = skipSpaces()
            when (next) {
                '/'.code,
                '>'.code,
                -1 -> return next
            }
        }
    }

    /**
     * read the body (all children)
     * */
    private fun scanXMLBody(depth: Int, type: CharSequence): Any {
        typeI++
        var next: Int = -1
        while (true) {
            val child = scan(next, depth + 1)
            next = -1
            when (child) {
                is String -> {
                    onContent(depth, type, child)
                    next = '<'.code
                }
                sthElement -> {} // do nothing
                endElement, endOfReader -> {
                    typeI--
                    onEnd(depth, type)
                    return if (child == endElement) sthElement else endOfReader
                }
                else -> throw IllegalStateException("Unknown child $child")
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun skipScanXMLVersion(depth: Int): Any {
        // <?xml version="1.0" encoding="utf-8"?>
        // I don't really care about it
        // read until ?>
        // or <?xpacket end="w"?>
        readUntil("?>")
        return scan(-1, depth)
    }

    private fun skipScanDocType(depth: Int): Any {
        var ctr = 1
        while (ctr > 0) {
            when (input.read()) {
                '<'.code -> ctr++
                '>'.code -> ctr--
            }
        }
        return scan(-1, depth)
    }

    private fun skipXMLNode(): Any {
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
                '"'.code -> skipString('"'.code)
                '\''.code -> skipString('\''.code)
                -1 -> return RuntimeException("Unexpected end")
            }
        }
    }

    private fun scanXMLNode(depth: Int, type: CharSequence): Any {
        var end = last
        if (end == -1) return endOfReader
        if (!onStart(depth, type)) {
            // this node can be ignored
            return skipXMLNode()
        }

        // / is the end of an element
        if (end.toChar().isWhitespace()) {
            end = scanXMLAttributes(depth, type)
        }

        when (end) {
            '/'.code -> {
                assertEquals(input.read(), '>'.code)
                onEnd(depth, type)
                return sthElement
            }
            '>'.code -> return scanXMLBody(depth, type)
            else -> {
                LOGGER.warn("Unknown end symbol '${end.toChar()}', $end")
                onEnd(depth, type)
                return endOfReader
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(XMLScanner::class)
    }
}