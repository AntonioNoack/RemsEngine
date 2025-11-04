package me.anno.io.xml.generic

import me.anno.io.generic.GenericWriterImpl
import me.anno.io.xml.ComparableStringBuilder
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
        private val tags = ArrayList<CharSequence>()
        private val attributes = ArrayList<CharSequence>()

        // to reduce unnecessary memory allocations when scanning large files
        private val builders = ArrayList<ComparableStringBuilder>()

        private fun newBuilder(content: CharSequence): CharSequence {
            val dst = builders.removeLastOrNull() ?: ComparableStringBuilder()
            dst.clear()
            dst.append(content)
            return dst
        }

        private fun recycleBuilder(builder: CharSequence) {
            if (builder is ComparableStringBuilder) {
                builder.clear()
                builders.add(builder)
            }
        }

        override fun beginObject(tag: CharSequence?): Boolean {
            super.beginObject(tag)
            tags.add(newBuilder(tag ?: "null"))
            return self.onStart(++depthI, tags.last())
        }

        override fun endObject() {
            super.endObject()
            val lastTag = tags.removeLast()
            self.onEnd(depthI--, lastTag)
            recycleBuilder(lastTag)
        }

        override fun beginArray(): Boolean {
            super.beginArray()
            tags.add("")
            return true
        }

        override fun endArray() {
            super.endArray()
            val lastTag = tags.removeLast()
            assertEquals("", lastTag)
            recycleBuilder(lastTag)
        }

        override fun attr(tag: CharSequence): Boolean {
            super.attr(tag)
            attributes.add(newBuilder(tag))
            return true
        }

        override fun write(value: CharSequence, isString: Boolean) {
            super.write(value, isString)
            if (tags.last() == "") {
                // in content
                self.onContent(depthI, tags[tags.size - 2], value)
            } else {
                // in attributes
                val lastAttr = attributes.removeLast()
                self.onAttribute(depthI, tags.last(), lastAttr, value)
                recycleBuilder(lastAttr)
            }
        }
    }

    fun scan(reader: Reader) {
        XMLReader(reader)
            .read(XMLScannerImpl(this))
    }
}