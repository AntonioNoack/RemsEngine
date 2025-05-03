package me.anno.io.xml.generic

import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import java.io.Reader

/**
 * Reads an XML file into a String|XMLNode|null|Exception
 *
 * todo transform this class into using a stack explicitly
 * */
open class XMLReader(input: Reader) : XMLReaderBase(input) {

    /**
     * returns String | XMLNode | Marker
     * */
    fun read() = read(-1)
    fun read(firstChar: Int): Any {
        val first = if (firstChar < 0) skipSpaces() else firstChar
        if (first == -1) return endOfReader
        if (first != '<'.code) return readStringUntilNextNode(first)
        val type = readTypeUntilSpaceOrEnd(type0, -1)
        return when {
            type.startsWith("?") -> skipXMLVersion()
            type.startsWith("!--") -> skipComment()
            type.startsWith("!doctype", true) -> skipDocType()
            type.startsWith("![cdata[", true) -> readCData(type)
            type.startsWith('/') -> endElement
            else -> readXMLNode(type.toString())
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun skipXMLVersion(): Any {
        // <?xml version="1.0" encoding="utf-8"?>
        // I don't really care about it
        // read until ?>
        // or <?xpacket end="w"?>
        readUntil("?>")
        return read(-1)
    }

    private fun skipComment(): Any {
        // search until -->
        readUntil("-->")
        return read(-1)
    }

    private fun skipDocType(): Any {
        var depth = 1
        while (depth > 0) {
            when (input.read()) {
                '<'.code -> depth++
                '>'.code -> depth--
            }
        }
        return read(-1)
    }

    private fun readXMLNodeBody(xmlNode: XMLNode): Any {
        // read the body (all children)
        var next: Int = -1
        children@ while (true) {
            val child = read(next)
            next = -1
            when (child) {
                endElement, endOfReader -> return xmlNode
                else -> {
                    xmlNode.children.add(child)
                    if (child is String) next = '<'.code
                }
            }
        }
    }

    private fun readProperties(xmlNode: XMLNode): Int {
        // read the properties
        while (true) {
            // name="value"
            when (val next = skipSpaces()) {
                '/'.code, '>'.code, -1 -> return next
                else -> {
                    val propName = readTypeUntilSpaceOrEnd(keyBuilder, next)
                    val propEnd = last
                    assertEquals(propEnd, '='.code)
                    val start = skipSpaces()
                    assert(start, '"', '\'')
                    val value = readStringUntilQuotes(start)
                    xmlNode[propName.toString()] = value.toString()
                }
            }
        }
    }

    private fun readXMLNode(type: String): Any {
        var end = last
        if (end == -1) return endOfReader

        val xmlNode = XMLNode(type)
        if (end.toChar().isWhitespace()) {
            // read the properties
            end = readProperties(xmlNode)
        }

        when (end) {
            '/'.code -> {
                // / is the end of an element
                assertEquals(input.read(), '>'.code)
                return xmlNode
            }
            '>'.code -> {
                // read the body (all children)
                readXMLNodeBody(xmlNode)
                return xmlNode
            }
            else -> {
                LOGGER.warn("Unknown end symbol ${end.toChar()}")
                return xmlNode
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(XMLReader::class)
    }
}