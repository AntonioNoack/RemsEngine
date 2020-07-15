package me.anno.io.json

import java.lang.RuntimeException

class JsonValue(val value: Any?): JsonNode(){
    override fun get(key: String): JsonNode? {
        throw RuntimeException("Not supported! This is a value node!")
    }
    override fun toString() = value.toString()
}