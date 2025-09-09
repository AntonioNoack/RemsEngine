package me.anno.io.xml.generic

import me.anno.io.generic.GenericWriterImpl
import me.anno.utils.assertions.assertEquals
import java.io.Reader

/**
 * Reads an XML file without create objects
 * */
interface XMLScanner {

    /**
     * Called, when an XML node is entered; return whether you want to process/enter that node.
     * */
    fun onStart(depth: Int, type: CharSequence): Boolean

    /**
     * Called, when an XML node is exited
     * */
    fun onEnd(depth: Int, type: CharSequence)

    /**
     * Called on each XML attribute
     * */
    fun onAttribute(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence)

    /**
     * Called on each XML string inside an element
     * */
    fun onContent(depth: Int, type: CharSequence, value: CharSequence)

    private class XMLScannerImpl(val self: XMLScanner) : GenericWriterImpl() {

        private var depthI = -1
        private val tags = ArrayList<String>()

        override fun beginObject(tag: CharSequence?): Boolean {
            super.beginObject(tag)
            tags.add(tag.toString())
            return self.onStart(++depthI, tags.last())
        }

        override fun endObject() {
            super.endObject()
            self.onEnd(depthI--, tags.removeLast())
        }

        override fun beginArray(): Boolean {
            super.beginArray()
            tags.add("")
            return true
        }

        override fun endArray() {
            super.endArray()
            assertEquals("", tags.removeLast())
        }

        private val attributes = ArrayList<CharSequence>()
        override fun attr(tag: CharSequence): Boolean {
            super.attr(tag)
            attributes.add(tag)
            return true
        }

        override fun write(value: CharSequence, isString: Boolean) {
            super.write(value, isString)
            if (tags.last() == "") {
                // in content
                self.onContent(depthI, tags[tags.size - 2], value)
            } else {
                // in attributes
                self.onAttribute(depthI, tags.last(), attributes.removeLast(), value)
            }
        }
    }

    fun scan(reader: Reader) {
        XMLReader(reader)
            .read(XMLScannerImpl(this))
    }
}