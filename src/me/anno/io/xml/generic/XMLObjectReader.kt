package me.anno.io.xml.generic

import me.anno.io.generic.GenericWriter
import me.anno.io.xml.generic.XMLReader.Companion.CHILDREN_NAME
import me.anno.utils.structures.lists.Lists.pop

/**
 * Uses this on a XMLReader to read it as an XMLNode.
 * */
open class XMLObjectReader : GenericWriter {

    private val stack = ArrayList<Any>()
    private val attrStack = ArrayList<String>()

    var result: Any? = null

    fun writeImpl(value: Any?) {
        @Suppress("UNCHECKED_CAST")
        when (val prev = stack.lastOrNull()) {
            is ArrayList<*> -> (prev as ArrayList<Any?>).add(value)
            is XMLNode -> {
                val attr = attrStack.removeLast()
                if (attr == CHILDREN_NAME) return
                prev[attr] = value.toString()
            }
            else -> result = value
        }
    }

    override fun beginObject(tag: CharSequence?): Boolean {
        val value = XMLNode(tag.toString())
        stack.add(value)
        return true
    }

    override fun endObject() {
        writeImpl(stack.pop())
    }

    override fun beginArray(): Boolean {
        stack.add((stack.last() as XMLNode).children)
        return true
    }

    override fun endArray() {
        stack.pop()
    }

    override fun attr(tag: CharSequence): Boolean {
        attrStack.add(tag.toString())
        return true
    }

    override fun write(value: CharSequence, isString: Boolean) {
        writeImpl(value.toString())
    }
}