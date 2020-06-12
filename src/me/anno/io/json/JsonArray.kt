package me.anno.io.json

import java.lang.RuntimeException

class JsonArray: JsonNode(){
    val content = ArrayList<Any>()
    override fun isValueNode() = false
    fun add(any: Any) = content.add(any)
    override fun get(key: String): JsonNode? {
        throw RuntimeException("Not supported, this is an array node!")
    }
    override fun toString() = content.toString()
}