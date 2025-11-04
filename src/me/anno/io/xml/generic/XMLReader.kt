package me.anno.io.xml.generic

import me.anno.io.generic.GenericReader
import me.anno.io.generic.GenericWriter
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import java.io.Reader

/**
 * Reads an XML file into a String|XMLNode|null|Exception
 * */
open class XMLReader(input: Reader) : XMLReaderBase(input), GenericReader {

    override fun read(writer: GenericWriter) {
        readImpl(writer)
    }

    /**
     * returns XMLNode or null
     * */
    fun readXMLNode(): XMLNode? {
        val reader = XMLObjectReader()
        readImpl(reader)
        return reader.result as? XMLNode
    }

    private fun readImpl(writer: GenericWriter): Any? {
        while (true) {
            val first = skipSpaces()
            if (first == -1) return endOfReader
            if (first != '<'.code) {
                val value = readStringUntilNextNode(first)
                writer.write(value, isString = true)
                return null
            }
            val type = readTypeUntilSpaceOrEnd(type0, -1)
            when {
                type.startsWith("?") -> skipXMLVersion()
                type.startsWith("!--") -> skipComment()
                type.startsWith("!doctype", true) -> skipDocType()
                type.startsWith("![cdata[", true) -> {
                    writer.write(readCData(type), isString = true)
                    return null
                }
                type.startsWith('/') -> {
                    assertEquals('>'.code, readChar())
                    return endElement
                }
                else -> {
                    readXMLNode(writer, type)
                    return null
                }
            }
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun skipXMLVersion() {
        // <?xml version="1.0" encoding="utf-8"?>
        // I don't really care about it
        // read until ?>
        // or <?xpacket end="w"?>
        readUntil("?>")
    }

    private fun skipComment() {
        // search until -->
        readUntil("-->")
    }

    private fun skipDocType() {
        var depth = 1
        while (depth > 0) {
            val char = readChar()
            when (char) {
                '<'.code -> depth++
                '>'.code -> depth--
                -1 -> return
            }
        }
    }

    private fun readXMLNodeBody(writer: GenericWriter) {
        // read the body (all children)
        while (true) {
            val end = readImpl(writer)
            if (end != null) break
        }
    }

    private fun readProperties(writer: GenericWriter): Int {
        // read the properties
        while (true) {
            // name="value"
            when (val next = skipSpaces()) {
                '/'.code, '>'.code, -1 -> return next
                else -> {
                    val propName = readTypeUntilSpaceOrEnd(keyBuilder, next)
                    assertEquals('='.code, readChar())
                    val start = skipSpaces()
                    assert(start, '"', '\'')
                    val value = readStringUntilQuotes(start)

                    writer.attr(propName)
                    writer.write(value, true)
                }
            }
        }
    }

    private fun readXMLNode(writer: GenericWriter, type: CharSequence): Any? {
        var end = readChar()
        if (end == -1) return endOfReader

        writer.beginObject(type)

        if (end.toChar().isWhitespace()) { // <a href...
            // read the properties
            end = readProperties(writer)
        }

        when (end) {
            '/'.code -> {
                // / is the end of an element
                assertEquals('>'.code, readChar())
            }
            '>'.code -> {
                // read the body (all children)
                writer.attr(CHILDREN_NAME)
                writer.beginArray()
                readXMLNodeBody(writer)
                writer.endArray()
            }
            else -> {
                LOGGER.warn("Unknown end symbol ${end.toChar()}")
            }
        }
        writer.endObject()
        return null
    }

    companion object {
        private val LOGGER = LogManager.getLogger(XMLReader::class)
        const val CHILDREN_NAME = "!"
    }
}