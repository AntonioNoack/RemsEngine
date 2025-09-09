package me.anno.io.generic

import me.anno.utils.structures.lists.Lists.pop

/**
 * Uses this on a XMLReader/JSONReader/YAMLReader to read it as an object/array.
 * The result(ing) object will be a Map or List of Maps, Lists, Strings, null, true and false.
 * Map keys will always be strings.
 * */
open class ObjectReader : GenericWriter {

    companion object {
        const val TAG_NAME = ""
    }

    private val stack = ArrayList<Any>()
    private val attrStack = ArrayList<String>()
    var result: Any? = null

    fun writeImpl(value: Any?) {
        @Suppress("UNCHECKED_CAST")
        when (val prev = stack.lastOrNull()) {
            is ArrayList<*> -> (prev as ArrayList<Any?>).add(value)
            is HashMap<*, *> -> (prev as HashMap<String, Any?>)[attrStack.removeLast()] = value
            else -> result = value
        }
    }

    override fun beginObject(tag: CharSequence?): Boolean {
        val value = LinkedHashMap<String, Any?>()
        if (tag != null) value[TAG_NAME] = tag.toString()
        stack.add(value)
        return true
    }

    override fun endObject() {
        writeImpl(stack.pop())
    }

    override fun beginArray(): Boolean {
        val value = ArrayList<Any?>()
        stack.add(value)
        return true
    }

    override fun endArray() {
        writeImpl(stack.pop())
    }

    override fun attr(tag: CharSequence): Boolean {
        attrStack.add(tag.toString())
        return true
    }

    override fun write(value: CharSequence, isString: Boolean) {
        if (isString) {
            writeImpl(value.toString())
        } else {
            when (value) {
                "null" -> writeImpl(null)
                "true" -> writeImpl(true)
                "false" -> writeImpl(false)
                else -> writeImpl(value.toString())
            }
        }
    }
}